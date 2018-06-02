package phorlio;

import io.netty.channel.socket.DatagramPacket;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.EventObject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import rxbeans.MutableProperty;
import rxbeans.Property;
import rxbeans.StandardObject;
import rxbeans.StandardProperty;

/**
 * PCP client.
 */
public class Client extends StandardObject implements AutoCloseable {

  public class ServerRestartedEvent extends EventObject {

    private final InetAddress ipAddress;

    public ServerRestartedEvent(final InetAddress ipAddress) {
      super(Client.this);
      this.ipAddress = ipAddress;
    }

    public InetAddress getIpAddress() {
      return ipAddress;
    }
  }

  private final MutableProperty<ServiceState> state = new StandardProperty<>(ServiceState.CREATED);
  private final MutableProperty<List<InetSocketAddress>> servers = new StandardProperty<>(
      List.of()
  );
  private final Set<UdpTransceiver> transceivers = new LinkedHashSet<>();
  private final Logger logger = Logger.getAnonymousLogger();
  private final FlowableProcessor<Response> inboundResponseStream = PublishProcessor
      .<Response>create()
      .toSerialized();
  private long epoch;

  private static boolean isDefaultNetworkInterface(final NetworkInterface it) {
    try {
      return it.isUp() && !it.isVirtual() && !it.isLoopback();
    } catch (SocketException err) {
      throw new RuntimeException(err);
    }
  }

  private static NetworkInterface findDefaultNetworkInterface() {
    return Observable
        .fromIterable(() -> {
          try {
            return NetworkInterface.getNetworkInterfaces().asIterator();
          } catch (SocketException err) {
            throw new RuntimeException(err);
          }
        })
        .filter(Client::isDefaultNetworkInterface)
        .firstOrError()
        .blockingGet();
  }

  private void handlePacket(final DatagramPacket packet) {
    throw new UnsupportedOperationException();
  }

  private void cleanUpFailedTransceivers() throws SocketException {
    final Predicate<UdpTransceiver> predicate = it -> {
      return it.stateProperty().get() != ServiceState.RUNNING;
    };
    transceivers.removeIf(predicate);
    if (transceivers.isEmpty()) {
      throw new SocketException("Failed to open sockets on every single home interface.");
    }
  }

  private void logFailedTransceiver(final Throwable err) {
    logger.log(Level.SEVERE, "Failed to open a socket", err);
  }

  private void logSuccessfulInitialization() {
    final var msg = new StringBuilder();
    msg.append("Opened sockets:").append(System.lineSeparator());
    transceivers
        .stream()
        .map(it -> it.getLocalSocketAddress().toString())
        .sorted()
        .forEachOrdered(it -> msg.append("  ").append(it).append(System.lineSeparator()));
    logger.info(msg.toString());
  }

  /**
   * Default constructor.
   * @param homes Manually specifies the {@link NetworkInterface} to use PCP. Use an empty
   *        {@link Collection} in order to choose a default one.
   * @throws IllegalStateException When fail to find a default home interface is found.
   */
  public Client(final Collection<InetSocketAddress> homes) {
    final var normalizedHomes = new LinkedHashSet<InetSocketAddress>();
    if (homes.isEmpty()) {
      findDefaultNetworkInterface()
          .getInterfaceAddresses()
          .forEach(it -> normalizedHomes.add(new InetSocketAddress(
              it.getAddress(),
              Constants.PORT_CLIENT
          )));
      if (normalizedHomes.isEmpty()) {
        throw new IllegalStateException("Could not find a suitable home interface.");
      }
    } else {
      normalizedHomes.addAll(homes);
    }
    for (final var addr : normalizedHomes) {
      if (addr.getAddress() instanceof Inet4Address) {
        transceivers.add(new NettyTransceiver(
            addr,
            List.of(new InetSocketAddress(Constants.MULTICAST_IPV4, Constants.PORT_CLIENT))
        ));
      } else if (addr.getAddress() instanceof Inet6Address) {
        transceivers.add(new NettyTransceiver(
            addr,
            List.of(new InetSocketAddress(Constants.MULTICAST_IPV6, Constants.PORT_CLIENT))
        ));
      } else {
        logger.info("An IP address of unsupported version is used: " + addr.toString());
      }
    }
  }

  /**
   * Starts the initialization.
   */
  public Completable start() {
    return state.getAndDo(state -> {
      switch (state) {
        case CLOSED:
          throw new IllegalStateException();
        case RUNNING:
          return Completable.complete();
        case INITIALIZING:
          return stateProperty()
              .getStream()
              .filter(it -> it == ServiceState.RUNNING)
              .firstOrError()
              .ignoreElement();
        default: break;
      }
      return Observable
          .fromIterable(transceivers)
          .flatMapCompletable(it -> it.start().doOnError(this::logFailedTransceiver))
          .andThen(Completable.fromAction(() -> {
            cleanUpFailedTransceivers();
            this.state.change(ServiceState.RUNNING);
            logSuccessfulInitialization();
          }))
          .doFinally(() -> {
            if (stateProperty().get() != ServiceState.RUNNING) {
              close();
            }
          });
    });
  }

  /**
   * Current state. This property can be used as a lock for operations that modifies this class.
   */
  public Property<ServiceState> stateProperty() {
    return state;
  }

  /**
   * Default PCP servers for PCP requests. Sending a request without specifying any PCP servers will
   * result an {@link IllegalArgumentException}.
   */
  public MutableProperty<List<InetSocketAddress>> serversProperty() {
    return servers;
  }

  @Override
  public void close() {
    state.change(ServiceState.CLOSED);
    transceivers.forEach(UdpTransceiver::close);
  }
}