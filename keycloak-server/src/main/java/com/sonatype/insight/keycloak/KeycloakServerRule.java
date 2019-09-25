/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import org.junit.rules.ExternalResource;

public class KeycloakServerRule
    extends ExternalResource
{
  private KeycloakServerUtil keycloakServerUtil;

  private KeycloakServer keycloakServer;

  public KeycloakServerUtil getServerUtil() {
    return keycloakServerUtil;
  }

  @Override
  public void before() {
    keycloakServer = new KeycloakServer();
    keycloakServerUtil = new KeycloakServerUtil(keycloakServer.getUrl());
  }

  @Override
  public void after() {
    if (keycloakServer != null) {
      keycloakServer.close();
      keycloakServerUtil = null;
    }
  }
}
