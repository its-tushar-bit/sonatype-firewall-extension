/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import org.keycloak.testsuite.KeycloakServer;

// Encapsulates a closable KeycloakServer started on a random port
// https://issues.sonatype.org/browse/CLM-13230
public class KeycloakTestServer
    implements AutoCloseable
{
  private final KeycloakServer keycloakServer = new KeycloakServer();

  private String url;

  public KeycloakTestServer() {
    // Find an available port
    int port;
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      port = serverSocket.getLocalPort();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    try {
      keycloakServer.getConfig().setPort(port);
      keycloakServer.start();
      url = String.format("http://localhost:%d/auth/", port);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public String getUrl() {
    return url;
  }

  @Override
  public void close() {
    keycloakServer.stop();
  }
}
