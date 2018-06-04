package phorlio;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class IpAddressUtils {

  public static byte[] toIpv4Mapped(final InetAddress address) {
    final var bytes = ByteBuffer.allocate(128 / 8);
    if (address instanceof Inet4Address) {
      bytes.put(new byte[6]);
      bytes.putShort((short) 0xFFFF);
    }
    bytes.put(address.getAddress());
    return bytes.array();
  }
}