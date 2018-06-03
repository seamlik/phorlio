package phorlio;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * Utilities for working with {@link NetworkInterface}s.
 */
public final class NetworkInterfaceUtils {

  private static boolean isDefaultNetworkInterface(final NetworkInterface it) {
    try {
      return it.isUp() && !it.isVirtual() && !it.isLoopback();
    } catch (SocketException err) {
      return false;
    }
  }

  public static Maybe<NetworkInterface> findDefaultNetworkInterface() {
    return Observable
        .fromIterable(() -> {
          try {
            return NetworkInterface.getNetworkInterfaces().asIterator();
          } catch (SocketException err) {
            throw new RuntimeException(err);
          }
        })
        .filter(NetworkInterfaceUtils::isDefaultNetworkInterface)
        .firstElement();
  }

  private NetworkInterfaceUtils() {}
}