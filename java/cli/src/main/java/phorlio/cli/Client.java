package phorlio.cli;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import phorlio.Constants;
import phorlio.ServiceState;

public class Client {

  /**
   * Main entry point.
   */
  public static void main(final String[] args) throws Exception {
    // Clumsy demo code for the school assignment. Must be removed ASAP.
    final var home = new InetSocketAddress(InetAddress.getByName("192.168.0.110"), Constants.PORT_CLIENT);
    final var server = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), Constants.PORT_SERVER);
    try (final var client = new phorlio.Client(List.of(home))) {
      client.serversProperty().set(List.of(server));
      client.start().subscribe();
      client.announce();
      client
          .stateProperty()
          .getStream()
          .filter(it -> it == ServiceState.CLOSED)
          .firstElement()
          .blockingGet();
    }
  }
}