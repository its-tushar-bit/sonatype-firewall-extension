/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.docker.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;

public final class DockerUtils
{
  private DockerUtils() {
    throw new UnsupportedOperationException();
  }

  public static void assumeSupported() {
    if (Boolean.getBoolean("docker.optional")) {
      throw new RuntimeException("Docker unavailable.");
    }
  }

  public static String applyRegistry(String image) {
    String registry = System.getProperty("docker.registry", "");
    return (registry.isEmpty() ? "" : registry + '/') + image;
  }

  public static String getHostname(String hostname, int port) {
    if ("localhost".equalsIgnoreCase(hostname) || "127.0.0.1".equals(hostname)) {
      return findHostIpAddress(port);
    }
    else {
      return hostname;
    }
  }

  public static String findHostIpAddress(int port) {
    try {
      for (Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces(); netInterfaces
          .hasMoreElements();)
      {
        NetworkInterface netInterface = netInterfaces.nextElement();
        try {
          if (netInterface.isUp() && !netInterface.isLoopback()) {
            for (Enumeration<InetAddress> addresses = netInterface.getInetAddresses(); addresses.hasMoreElements();) {
              InetAddress address = addresses.nextElement();
              try {
                if (!address.isLoopbackAddress()) {
                  testHttpConnection(address.getHostAddress(), port);
                  return address.getHostAddress();
                }
              }
              catch (IOException ignored) {
                // try the next address
              }
            }
          }
        }
        catch (IOException ignored) {
          // try the next interface
        }
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    throw new IllegalStateException("Could not determine IP address");
  }

  public static void testHttpConnection(String hostname, int port) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL("http://" + hostname + ":" + port).openConnection();
    connection.setConnectTimeout(1000);
    connection.setReadTimeout(1000);
    try (InputStream ignored = connection.getInputStream()) {
      // Ignored
    }
    finally {
      connection.disconnect();
    }
  }
}
