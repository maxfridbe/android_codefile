package com.TechHurts.CodeFile;

import java.net.*;

/** Endpoint policy independent of Android so it can be tested on the JVM. */
public final class ApiEndpoint {
  private ApiEndpoint() {}

  public static URL parse(String value) throws Exception {
    URI uri = new URI(value.trim());
    String scheme = uri.getScheme();
    if (!"https".equals(scheme) && !"http".equals(scheme))
      throw new IllegalArgumentException("Use an http:// or https:// chat completions URL");
    if (uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getFragment() != null
        || uri.getQuery() != null)
      throw new IllegalArgumentException(
          "Use a complete endpoint without credentials, query, or fragment");
    if (!uri.getPath().endsWith("/chat/completions"))
      throw new IllegalArgumentException("Endpoint must end in /chat/completions");
    if ("http".equals(scheme)) {
      // Require a literal LAN/loopback address, avoiding DNS rebinding and accidental public HTTP.
      String host = uri.getHost().replace("[", "").replace("]", "");
      if (!host.equals("localhost") && !host.matches("[0-9.]+") && !host.contains(":"))
        throw new IllegalArgumentException("For local HTTP, use localhost or a private IP address");
      for (InetAddress address : InetAddress.getAllByName(host))
        if (!isLocal(address))
          throw new IllegalArgumentException(
              "Public API endpoints require HTTPS; HTTP is supported on local networks only");
    }
    return uri.toURL();
  }

  static boolean isLocal(InetAddress address) {
    byte[] bytes = address.getAddress();
    return address.isLoopbackAddress()
        || address.isSiteLocalAddress()
        || address.isLinkLocalAddress()
        || (bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc);
  }
}
