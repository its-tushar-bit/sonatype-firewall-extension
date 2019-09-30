/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KeycloakServerTest
{
  @Test
  public void testKeycloakServer() {
    try (KeycloakServer keycloakServer = new KeycloakServer()) {
      assertThat(keycloakServer.getHostname()).isNotNull().isNotEqualToIgnoringCase("localhost");
      assertThat(keycloakServer.getPort()).isNotNull();
    }
  }
}
