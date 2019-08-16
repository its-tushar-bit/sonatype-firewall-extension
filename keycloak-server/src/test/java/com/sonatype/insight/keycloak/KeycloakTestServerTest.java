/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import java.net.ConnectException;
import java.net.URL;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;

import org.junit.Rule;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;

import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KeycloakTestServerTest
{
  @Rule
  public KeycloakTestServerJunitRule tempKeycloakServer = new KeycloakTestServerJunitRule();

  private KeycloakTestServer keycloak = KeycloakTestServerJunitRule.getServer();

  @Test
  public void testKeycloakTestServer() {
    try (Response response = newClient().target(keycloak.getUrl()).request().get()) {
      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    }
  }

  @Test
  public void testStopStart() {
    keycloak.stop();
    assertThatExceptionOfType(ConnectException.class).isThrownBy(() -> new URL(keycloak.getUrl()).openStream());

    keycloak.start();
    try (Response response = newClient().target(keycloak.getUrl()).request().get()) {
      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    }
  }

  @Test
  public void testStart_startRunning() {
    keycloak.start();
  }

  @Test
  public void testStop_stopAlreadyStopped() {
    keycloak.stop();
    keycloak.stop();
  }

  @Test
  public void testGetToken_admin() {
    assertThat(keycloak.getToken("admin", "admin")).hasSize(950);
  }

  @Test
  public void testGetToken_userDoesNotExist() {
    assertThatThrownBy(() -> keycloak.getToken("", "")).isInstanceOf(NotAuthorizedException.class);
  }

  @Test
  public void testCreateClient() {
    // Initial clients in Keycloak:
    // account, admin-cli, broken, master-realm, security-admin-console
    assertThat(keycloak.getClients()).hasSize(5);

    ClientRepresentation clientRepresentation = new ClientRepresentation();
    clientRepresentation.setClientId("a-new-client");
    clientRepresentation.setProtocol("saml");

    keycloak.createClient(clientRepresentation);
    assertThat(keycloak.getClients()).hasSize(6);
  }

  @Test
  public void testClean() {
    assertThat(keycloak.getClients()).hasSize(5);

    ClientRepresentation clientRepresentation = new ClientRepresentation();
    clientRepresentation.setClientId("a-new-client");
    clientRepresentation.setProtocol("saml");

    keycloak.createClient(clientRepresentation);
    assertThat(keycloak.getClients()).hasSize(6);

    keycloak.clean();
    assertThat(keycloak.getClients()).hasSize(5);
  }

  @Test
  public void testCreateClient_Duplicate() {
    ClientRepresentation clientRepresentation = new ClientRepresentation();
    clientRepresentation.setClientId("a-new-client");
    clientRepresentation.setProtocol("saml");

    keycloak.createClient(clientRepresentation);
    assertThatThrownBy(() -> keycloak.createClient(clientRepresentation)).isInstanceOf(RuntimeException.class)
        .hasMessage("Client creation failed.");
  }
}
