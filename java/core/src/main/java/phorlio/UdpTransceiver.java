package phorlio;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.annotations.SchedulerSupport;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Set;
import rxbeans.MutableProperty;
import rxbeans.Property;
import rxbeans.StandardProperty;

@SchedulerSupport(SchedulerSupport.NONE)
public abstract class UdpTransceiver implements AutoCloseable {

  protected final MutableProperty<ServiceState> state = new StandardProperty<>(
      ServiceState.CREATED
  );
  protected final FlowableProcessor<DatagramPacket> inboundPacketStream = PublishProcessor
      .<DatagramPacket>create()
      .toSerialized();
  protected final Set<InetAddress> multicastGroups;
  protected final InetSocketAddress suggestedLocalSocketAddress;

  public UdpTransceiver(final InetSocketAddress local, final Collection<InetAddress> multicast) {
    suggestedLocalSocketAddress = local;
    multicastGroups = Set.copyOf(multicast);
  }

  public abstract Completable send(final DatagramPacket packet);

  public abstract Completable start();

  public abstract InetSocketAddress getLocalSocketAddress();

  public Flowable<DatagramPacket> getInboundPacketStream() {
    return inboundPacketStream;
  }

  public Property<ServiceState> stateProperty() {
    return state;
  }

  @Override
  public void close() {
    state.change(ServiceState.CLOSED);
  }
}