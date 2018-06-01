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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import rxbeans.MutableProperty;
import rxbeans.Property;
import rxbeans.StandardProperty;

/**
 * PCP client.
 */
public class Client implements AutoCloseable {

  private final MutableProperty<ServiceState> state = new StandardProperty<>(ServiceState.CREATED);
  private final MutableProperty<List<InetSocketAddress>> servers = new StandardProperty<>(
      List.of()
  );
  private final Map<Inet4Address, UdpTransceiver> transceivers4 = new LinkedHashMap<>();
  private final Map<Inet6Address, UdpTransceiver> transceivers6 = new LinkedHashMap<>();
  private final Logger logger = Logger.getAnonymousLogger();
  private final FlowableProcessor<Response> inboundResponseStream = PublishProcessor
      .<Response>create()
      .toSerialized();

  private void handlePacket(final DatagramPacket packet) {
    throw new UnsupportedOperationException();
  }

  private void cleanUpFailedTransceivers() throws SocketException {
    final Predicate<UdpTransceiver> predicate = it -> {
      return it.stateProperty().get() != ServiceState.RUNNING;
    };
    transceivers4.values().removeIf(predicate);
    transceivers6.values().removeIf(predicate);
    if (transceivers4.isEmpty() && transceivers6.isEmpty()) {
      throw new SocketException("Failed to open sockets on every single home interface.");
    }
  }

  private void logFailedTransceivers(final Throwable err) {
    logger.log(Level.SEVERE, "Failed to open a socket", err);
  }

  /**
   * Default constructor.
   * @param homes Manually specifies the local address and port to use PCP. Use an empty
   *              {@link Collection} in order to use the default ones chosen by the system.
   */
  public Client(final Collection<InetSocketAddress> homes) {
    final var normalizedHomes = new LinkedHashSet<InetSocketAddress>();
    if (homes.isEmpty()) {
      try {
        normalizedHomes.add(new InetSocketAddress(
            InetAddress.getByName("0.0.0.0"),
            Constants.PORT_CLIENT
        ));
        normalizedHomes.add(new InetSocketAddress(
            InetAddress.getByName("::"),
            Constants.PORT_CLIENT
        ));
      } catch (UnknownHostException err) {
        throw new RuntimeException(err);
      }
    } else {
      normalizedHomes.addAll(homes);
    }
    for (final var addr : normalizedHomes) {
      if (addr.getAddress() instanceof Inet4Address) {
        transceivers4.put(
            (Inet4Address) addr.getAddress(),
            new NettyTransceiver(addr, List.of(Constants.MULTICAST_IPV4))
        );
      } else if (addr.getAddress() instanceof Inet6Address) {
        transceivers6.put(
            (Inet6Address) addr.getAddress(),
            new NettyTransceiver(addr, List.of(Constants.MULTICAST_IPV6))
        );
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
          .fromArray(transceivers4.values(), transceivers6.values())
          .flatMap(Observable::fromIterable)
          .flatMapCompletable(
              it -> it.start().doOnError(this::logFailedTransceivers).onErrorComplete()
          )
          .doOnComplete(this::cleanUpFailedTransceivers)
          .doOnComplete(() -> this.state.change(ServiceState.RUNNING))
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
    transceivers4.values().forEach(UdpTransceiver::close);
    transceivers6.values().forEach(UdpTransceiver::close);
  }
}