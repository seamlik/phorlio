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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
  private final Map<InetAddress, Integer> epoch = new HashMap<>();

  private void handlePacket(final DatagramPacket packet) {
    final Response response;
    try {
      response = Response.fromBytes(packet.content().array());
      inboundResponseStream.onNext(response);
    } catch (Exception err) {
      logger.log(Level.SEVERE, "Failed to parse a response.", err);
      return;
    }

    synchronized (epoch) {
      final var serverIp = packet.sender().getAddress();
      final var currentEpoch = epoch.getOrDefault(serverIp, 0);
      if (response.getEpoch() < currentEpoch) {
        triggerEvent(new ServerRestartedEvent(serverIp));
      }
      epoch.put(serverIp, response.getEpoch());
    }
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
   * @param homes Home {@link NetworkInterface} to use PCP. {@link Request}s will be sent from all
   *        homes simultaneously, and UDP sockets will be opened on all homes.
   * @throws IllegalArgumentException If no home {@link NetworkInterface} is specified..
   */
  public Client(final Collection<InetSocketAddress> homes) {
    if (homes.isEmpty()) {
      throw new IllegalArgumentException("Must specify at least one home interface.");
    }
    for (final var addr : homes) {
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