/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import java.net.ServerSocket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.KeycloakServer;

// Encapsulates a closable KeycloakServer started on a random port
// and exposes utility methods per https://www.keycloak.org/docs-api/6.0/rest-api/
// For using in tests, please see KeycloakTestServerTest for reference
// https://issues.sonatype.org/browse/CLM-13230
public class KeycloakTestServer
{
  private KeycloakServer keycloakServer;

  private boolean running = false;

  private String url;

  private String adminBearerToken;

  private final Set<String> createdClientIds = new HashSet<>();

  public void start() {
    if (running) {
      return;
    }

    try {
      if (keycloakServer == null) {
        int port;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
          port = serverSocket.getLocalPort();
        }
        keycloakServer = new KeycloakServer();
        keycloakServer.getConfig().setPort(port);
        url = String.format("http://localhost:%d/auth/", port);
      }
      keycloakServer.start();
      adminBearerToken = getToken("admin", "admin");
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }

    running = true;
  }

  public void stop() {
    if (running) {
      keycloakServer.stop();
      running = false;
    }
  }

  /**
   * @return Bearer token for the passed in user
   */
  public String getToken(String username, String password) {
    return ClientBuilder.newClient().target(url).path("realms/master/protocol/openid-connect/token")
        .request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(
            new Form().param("username", username).param("password", password).param("client_id", "admin-cli")
                .param("grant_type", "password"), MediaType.APPLICATION_FORM_URLENCODED_TYPE), Token.class).accessToken;
  }

  /**
   * @see <a href="https://www.keycloak.org/docs-api/6.0/rest-api/#_clients_resource">Client Resource</a>
   * @see <a href="https://www.keycloak.org/docs-api/6.0/rest-api/#_clientrepresentation">Client Representation</a>
   */
  public void createClient(ClientRepresentation client) {
    Response response = ClientBuilder.newClient().target(url).path("admin/realms/master/clients").request()
        .header("Authorization", "Bearer " + adminBearerToken)
        .buildPost(Entity.entity(client, MediaType.APPLICATION_JSON)).invoke();

    if (response.getStatus() == Status.CREATED.getStatusCode()) {
      createdClientIds.add(
          Arrays.stream(getClients()).filter(c -> c.getClientId().equals(client.getClientId())).findAny().get()
              .getId());
    }
    else {
      throw new RuntimeException("Client creation failed.");
    }
  }

  public String getUrl() {
    return url;
  }

  public void clean() {
    for (String clientId : createdClientIds) {
      ClientBuilder.newClient().target(url).path("admin/realms/master/clients").path(clientId).request()
          .header("Authorization", "Bearer " + adminBearerToken).buildDelete().invoke();
    }
  }

  ClientRepresentation[] getClients() {
    return ClientBuilder.newClient().target(url).path("admin/realms/master/clients").request()
        .header("Authorization", "Bearer " + adminBearerToken).buildGet().invoke()
        .readEntity(ClientRepresentation[].class);
  }
}
