/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import javax.ws.rs.core.Response;

import org.junit.Test;

import static javax.ws.rs.client.ClientBuilder.newClient;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;

public class KeycloakTestServerTest
{
  @Test
  public void testKeycloakTestServer() {
    try (KeycloakTestServer keycloak = new KeycloakTestServer();
         Response response = newClient().target(keycloak.getUrl()).request().get()) {
      assertThat(response.getStatus()).isEqualTo(SC_OK);
    }
  }
}
