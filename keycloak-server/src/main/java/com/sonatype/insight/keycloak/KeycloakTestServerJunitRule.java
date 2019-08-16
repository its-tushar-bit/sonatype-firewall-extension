/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import org.junit.rules.ExternalResource;

public class KeycloakTestServerJunitRule
    extends ExternalResource
{
  private static KeycloakTestServer keycloakTestServer = new KeycloakTestServer();

  public static KeycloakTestServer getServer() {
    return keycloakTestServer;
  }

  @Override
  protected void before() {
    keycloakTestServer.start();
  }

  @Override
  protected void after() {
    keycloakTestServer.start();
    keycloakTestServer.clean();
  }
}
