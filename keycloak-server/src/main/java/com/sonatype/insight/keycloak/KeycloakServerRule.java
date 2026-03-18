/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

public class KeycloakServerRule
    extends ExternalResource
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Network network;

  private KeycloakServer keycloakServer;

  private KeycloakServerUtil keycloakServerUtil;

  private Exception lastCleanException;

  public KeycloakServerRule() {
    this(null);
  }

  public KeycloakServerRule(Network network) {
    this.network = network;
  }

  @Override
  public void before() throws InterruptedException {
    keycloakServer = new KeycloakServer(network);
    keycloakServerUtil = new KeycloakServerUtil();
    keycloakServerUtil.init(keycloakServer.getBaseUrl());
    // Enable unmanaged user attributes (required for Keycloak 24+ where User Profile is enabled by default)
    keycloakServerUtil.enableUnmanagedAttributes();
  }

  public KeycloakServerUtil getKeycloakServerUtil() {
    return keycloakServerUtil;
  }

  @Override
  public void after() {
    if (keycloakServer != null) {
      keycloakServer.close();
      keycloakServerUtil = null;
    }
  }

  private void resetKeycloakServer() throws InterruptedException {
    keycloakServer.close();
    keycloakServer = new KeycloakServer(network);
    keycloakServerUtil.init(keycloakServer.getBaseUrl());
  }

  /**
   * Resets the test keycloak server to original state.
   *
   * WARNING: The test keycloak server is restarted if the cleanup fails (there's a bug in keycloak).
   */
  public void clean() {
    if (lastCleanException != null) {
      log.error("Previous KeycloakServerUtil.clean() failed: {}", lastCleanException.getMessage(), lastCleanException);
      lastCleanException = null;
    }

    try {
      keycloakServerUtil.clean();
    }
    catch (RuntimeException e) {
      // There is a bug in keycloak that causes the cleanup to fail randomly, which results in flaky tests.
      // Retrying the operations doesn't seem to help, so we restart the keycloak server in case of errors.
      // This is much faster then starting a new keycloak server for each test.
      try {
        e.printStackTrace();
        log.error("KeycloakServerUtil.clean() failed: {}", e.getMessage(), e);
        lastCleanException = e;
        resetKeycloakServer();
      }
      catch (InterruptedException e1) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Keycloak server reset was interrupted.", e1);
      }
    }
  }
}
