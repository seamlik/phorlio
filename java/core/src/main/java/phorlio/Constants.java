package phorlio;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class Constants {

  private Constants() {}

  public static final int PORT_SERVER = 5351;
  public static final int PORT_CLIENT = 5350;
  public static final short VERSION = 2;
  public static final InetAddress MULTICAST_IPV4;
  public static final InetAddress MULTICAST_IPV6;
  public static final Inet4Address SERVER_IPV4;
  public static final Inet6Address SERVER_IPV6;

  static {
    try {
      MULTICAST_IPV4 = InetAddress.getByName("224.0.0.1");
      MULTICAST_IPV6 = InetAddress.getByName("ff02::1");
      SERVER_IPV4 = (Inet4Address) InetAddress.getByName("192.0.0.9");
      SERVER_IPV6 = (Inet6Address) InetAddress.getByName("2001:1::1");
    } catch (UnknownHostException ex) {
      throw new RuntimeException(ex);
    }
  }
}