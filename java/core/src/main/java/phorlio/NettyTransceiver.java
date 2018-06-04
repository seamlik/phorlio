package phorlio;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.NetUtil;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class NettyTransceiver extends UdpTransceiver {

  private final EventLoopGroup nettyLoopers = new NioEventLoopGroup();
  private @MonotonicNonNull DatagramChannel channel;

  private void readPacket(final io.netty.channel.socket.DatagramPacket packet) {
    final var bytes = new byte[packet.content().readableBytes()];
    packet.content().readBytes(bytes);
    inboundPacketStream.onNext(new DatagramPacket(bytes, bytes.length, packet.sender()));
  }

  public NettyTransceiver(final InetSocketAddress local,
                          final Collection<InetSocketAddress> multicast) {
    super(local, multicast);
  }

  @Override
  public Completable start() {
    return state.getAndDo(state -> {
      switch (state) {
        case CLOSED: throw new IllegalStateException();
        case RUNNING: return Completable.complete();
        case INITIALIZING: return stateProperty()
            .getStream()
            .filter(it -> it == ServiceState.RUNNING)
            .firstOrError()
            .ignoreElement();
        default: break;
      }
      this.state.change(ServiceState.INITIALIZING);
      final var bootstrap = new Bootstrap();
      bootstrap.group(nettyLoopers);
      bootstrap.channel(NioDatagramChannel.class);
      bootstrap.option(ChannelOption.SO_REUSEADDR, true);
      bootstrap.handler(new SimpleChannelInboundHandler<io.netty.channel.socket.DatagramPacket>() {
        @Override
        protected void channelRead0(final ChannelHandlerContext ctx,
                                    final io.netty.channel.socket.DatagramPacket msg) {
          readPacket(msg);
        }
      });
      return Completable
          .fromAction(() -> channel = (DatagramChannel) bootstrap
              .bind(suggestedLocalSocketAddress)
              .sync()
              .channel()
          )
          .andThen(Observable
              .fromIterable(multicastGroups)
              .flatMapCompletable(
                  it -> Completable.fromFuture(channel.joinGroup(it, NetUtil.LOOPBACK_IF))
              )
          )
          .andThen(Completable.fromAction(() -> this.state.change(ServiceState.RUNNING)))
          .doFinally(() -> {
            if (stateProperty().get() != ServiceState.RUNNING) {
              close();
            }
          });
    });
  }

  @Override
  public Completable send(final DatagramPacket packet) {
    return state.getAndDo(state -> {
      if (state != ServiceState.RUNNING) {
        throw new IllegalStateException();
      }
      final var buf = channel.alloc().buffer(packet.getLength());
      buf.writeBytes(packet.getData(), packet.getOffset(), packet.getLength());
      final var nettyPacket = new io.netty.channel.socket.DatagramPacket(
          buf,
          new InetSocketAddress(packet.getAddress(), packet.getPort())
      );
      return Completable.fromFuture(channel.writeAndFlush(nettyPacket));
    });
  }

  @Override
  public InetSocketAddress getLocalSocketAddress() {
    return state.getAndDo(state -> {
      if (state == ServiceState.RUNNING || state == ServiceState.CLOSED) {
        return channel.localAddress();
      } else {
        throw new IllegalStateException("Transceiver must have been initialized.");
      }
    });
  }

  @Override
  public void close() {
    super.close();
    nettyLoopers.shutdownGracefully();
  }
}