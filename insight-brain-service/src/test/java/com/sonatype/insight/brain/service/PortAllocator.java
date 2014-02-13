/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @since 1.9.1
 */
public class PortAllocator
{
  public static int findFreePort(final int defaultPort) {
    int port = defaultPort;
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(0);
      port = socket.getLocalPort();
    }
    catch (final IOException e) {
      e.printStackTrace();
    }
    finally {
      if (socket != null) {
        try {
          socket.close();
        }
        catch (final IOException e) {
          e.printStackTrace();
        }
      }
    }
    return port;
  }
}
