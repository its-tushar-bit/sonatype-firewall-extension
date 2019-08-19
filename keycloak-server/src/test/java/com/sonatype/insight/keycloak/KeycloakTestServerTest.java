/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import java.net.ConnectException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;

import org.junit.Rule;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
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
    assertThat(keycloak.getUsers()).extracting(UserRepresentation::getUsername).containsExactly("admin");

    ClientRepresentation clientRepresentation = new ClientRepresentation();
    clientRepresentation.setClientId("a-new-client");
    clientRepresentation.setProtocol("saml");

    UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setUsername("john.doe");
    userRepresentation.setEmail("example@example.com");
    userRepresentation.setEnabled(true);

    keycloak.createClient(clientRepresentation);
    keycloak.createUser(userRepresentation);

    assertThat(keycloak.getClients()).hasSize(6);
    assertThat(keycloak.getUsers()).hasSize(2);

    keycloak.clean();

    assertThat(keycloak.getClients()).hasSize(5);
    assertThat(keycloak.getUsers()).extracting(UserRepresentation::getUsername).containsExactly("admin");
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

  @Test
  public void testCreateUser_UsingUserRepresentation() {
    UserRepresentation user = new UserRepresentation();
    user.setUsername("john.doe");
    user.setEmail("example@example.com");
    user.setEnabled(true);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue("password");
    credential.setTemporary(false);
    user.setCredentials(asList(credential));

    Map<String, List<String>> userAttributes = new HashMap<>();
    userAttributes.put("key-01", asList("key-01-val01", "key-01-val02"));
    userAttributes.put("key-02", asList("key-02-val01", "key-02-val02"));
    user.setAttributes(userAttributes);

    String userId = keycloak.createUser(user);
    assertThat(userId).isNotNull();

    user = stream(keycloak.getUsers()).filter(u -> u.getUsername().equals("john.doe")).findFirst().get();
    assertThat(user.getEmail()).isEqualTo("example@example.com");
    assertThat(user.isEnabled()).isTrue();
    assertThat(userAttributes).isEqualTo(user.getAttributes());

    // This asserts the user is enabled and the password is working
    assertThat(keycloak.getToken("john.doe", "password")).isNotNull();
  }

  @Test
  public void testCreateUser_UsingUserRepresentationDuplicate() {
    testCreateUser_UsingUserRepresentation();
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(this::testCreateUser_UsingUserRepresentation)
        .withMessage("User creation failed with status code: 409");
  }

  @Test
  public void testCreateUser_UsingUserAttributes() {
    String id = keycloak.createUser("john", "doe", "joanne.doe", "joanne@example.com", "pw", new HashMap<>());
    assertThat(id).isNotNull();

    UserRepresentation user =
        stream(keycloak.getUsers()).filter(u -> u.getUsername().equals("joanne.doe")).findFirst().get();
    assertThat(user.getEmail()).isEqualTo("joanne@example.com");
    assertThat(user.getFirstName()).isEqualTo("john");
    assertThat(user.getLastName()).isEqualTo("doe");
    assertThat(user.isEnabled()).isTrue();
    assertThat(user.getAttributes()).isNull();

    // This asserts the user is enabled and the password is working
    assertThat(keycloak.getToken("joanne.doe", "pw")).isNotNull();
  }

  @Test
  public void testCreateUser_UsingUserAttributesDuplicate() {
    testCreateUser_UsingUserAttributes();
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(this::testCreateUser_UsingUserAttributes)
        .withMessage("User creation failed with status code: 409");
  }

  @Test
  public void testUpdateUser() {
    UserRepresentation user = new UserRepresentation();
    user.setUsername("john.doe");
    user.setEmail("example@example.com");
    user.setEnabled(true);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue("password");
    credential.setTemporary(false);
    user.setCredentials(asList(credential));

    Map<String, List<String>> userAttributes = new HashMap<>();
    userAttributes.put("foo", asList("bar"));
    user.setAttributes(userAttributes);

    keycloak.createUser(user);
    user = stream(keycloak.getUsers()).filter(u -> u.getUsername().equals("john.doe")).findFirst().get();
    assertThat(user.getAttributes().get("foo")).isEqualTo(Arrays.asList("bar"));

    user.getAttributes().get("foo").set(0, "baz");
    keycloak.updateUser(user);

    user = stream(keycloak.getUsers()).filter(u -> u.getUsername().equals("john.doe")).findFirst().get();
    assertThat(user.getAttributes().get("foo")).isEqualTo(Arrays.asList("baz"));
  }

  @Test
  public void testUpdateUser_UserDoesNotExist() {
    UserRepresentation user = new UserRepresentation();
    user.setId("no-such-user");
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> keycloak.updateUser(user))
        .withMessage("User update failed with status code: 404");
  }

  @Test
  public void testLogoutUser() {
    String id = keycloak.createUser("john", "doe", "john.doe", "", "password", new HashMap<>());
    keycloak.getToken("john.doe", "password"); // Creates a session for john.doe
    assertThat(keycloak.getSessionsOfUser(id)).hasSize(1);
    keycloak.logoutUser(id);
    assertThat(keycloak.getSessionsOfUser(id)).isEmpty();
  }
}
