package phorlio;

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * PCP request.
 */
public class Request extends Packet {

  private final InetAddress clientIpAddress;

  /**
   * Default constructor.
   */
  public Request(final byte version,
                 final Opcode opcode,
                 final int lifetime,
                 final InetAddress clientIpAddress) {
    super(version, opcode, lifetime);
    this.clientIpAddress = clientIpAddress;
  }

  public InetAddress getClientIpAddress() {
    return clientIpAddress;
  }

  @Override
  public byte[] toBytes() {
    final var bytes = ByteBuffer.allocate(getLength());
    bytes.put(Constants.VERSION);
    bytes.put(getOpcode().getValue());
    bytes.put(new byte[2]);
    bytes.putInt((int) getLifetime());
    bytes.put(IpAddressUtils.toIpv4Mapped(clientIpAddress));
    // TODO: Support Opcode data and options
    return bytes.array();
  }

  @Override
  public int getLength() {
    return 32 * 6;
    // TODO: Support Opcode data and options
  }
}
