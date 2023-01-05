/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import org.junit.rules.ExternalResource;
import org.testcontainers.containers.Network;

public class KeycloakServerRule
    extends ExternalResource
{
  private final Network network;

  private KeycloakServer keycloakServer;

  private KeycloakServerUtil keycloakServerUtil;

  public KeycloakServerRule() {
    this(null);
  }

  public KeycloakServerRule(Network network) {
    this.network = network;
  }

  @Override
  public void before() throws InterruptedException {
    keycloakServer = new KeycloakServer(network);
    keycloakServerUtil = new KeycloakServerUtil(keycloakServer.getBaseUrl());
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
}
