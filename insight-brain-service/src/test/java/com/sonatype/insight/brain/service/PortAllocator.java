/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @since 1.9.1
 */
public class PortAllocator
{
  /**
   * Given we close the socket to get the ephemeral port, it's possible for another call to {@link #findFreePort(int)}
   * to yield the same port if the previously found free port has not been used just yet. To avoid such unintented port
   * reuse, we keep a little history of ports that have been delivered and manually exclude them.
   */
  @SuppressWarnings("serial")
  private static final Map<Integer, Object> usedPorts = new LinkedHashMap<Integer, Object>()
  {
    @Override
    protected boolean removeEldestEntry(Map.Entry<Integer, Object> eldest) {
      return size() >= 10;
    }
  };

  public static int findFreePort(final int defaultPort) {
    int port = defaultPort;
    for (int i = 0; i < 10; i++) {
      try (ServerSocket socket = new ServerSocket(0)) {
        port = socket.getLocalPort();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      if (usedPorts.put(port, "") == null) {
        break;
      }
    }
    return port;
  }
}
