/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.common.test.SlowTest;

import com.github.javafaker.Faker;
import com.github.javafaker.Internet;
import com.github.javafaker.Name;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static jakarta.ws.rs.client.ClientBuilder.newClient;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category(SlowTest.class)
public class KeycloakServerUtilTest
{
  @ClassRule
  public static KeycloakServerRule rule = new KeycloakServerRule();

  private KeycloakServerUtil keycloak = rule.getKeycloakServerUtil();

  // There is a bug in keycloak that causes some tests to fail randomly,
  // so we add a retry for all tests in case they fail.
  @Rule
  public TestRetryRule retryRule = new TestRetryRule(2);

  private final Faker faker = new Faker();

  @After
  public void after() {
    rule.clean();
  }

  @Test
  public void testKeycloakServerUtil() throws Exception {
    Response response = newClient().target(keycloak.getBaseUrl()).request().get();
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(keycloak.getMasterRealm().getAccessTokenLifespan())
        .isEqualTo(KeycloakServerUtil.ADMIN_TOKEN_LIFESPAN_IN_SECONDS);
    response.close();
  }

  @Test
  public void testGetToken_admin() throws Exception {
    assertThat(keycloak.getToken(KeycloakServer.DEFAULT_USERNAME, KeycloakServer.DEFAULT_PASSWORD)).isNotBlank();
  }

  @Test
  public void testGetToken_userDoesNotExist() {
    assertThatThrownBy(() -> keycloak.getToken("", "")).isInstanceOf(NotAuthorizedException.class);
  }

  @Test
  public void testCreateClient() {
    // Initial clients in Keycloak:
    // account, admin-cli, broken, master-realm, security-admin-console
    int builtInClientsCount = keycloak.getClients().length;

    ClientRepresentation clientRepresentation = new ClientRepresentation();
    clientRepresentation.setClientId("a-new-client");
    clientRepresentation.setProtocol("saml");

    keycloak.createClient(clientRepresentation);
    assertThat(keycloak.getClients()).hasSize(builtInClientsCount + 1);
  }

  @Test
  public void testCreateClientRepresentation() throws Exception {
    String metadata = new String(readAllBytes(Paths.get("src/test/resources/service_provider_metadata.xml")), UTF_8);

    ClientRepresentation client = keycloak.createClientRepresentation(metadata);

    assertThat(client.getClientId()).isEqualTo("http://localhost:8081/securityRealm/finishLogin");
    assertThat(client.getProtocol()).isEqualTo("saml");
  }

  @Test
  public void testClean() {
    String username = faker.name().username();
    String email = faker.internet().emailAddress();
    String clientId = faker.funnyName().name();
    String groupName = faker.funnyName().name();

    int builtInClientsCount = keycloak.getClients().length;
    assertThat(keycloak.getUsers()).extracting(UserRepresentation::getUsername)
        .containsExactly(KeycloakServer.DEFAULT_USERNAME);

    ClientRepresentation clientRepresentation = new ClientRepresentation();
    clientRepresentation.setClientId(clientId);
    clientRepresentation.setProtocol("saml");

    UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setUsername(username);
    userRepresentation.setEmail(email);
    userRepresentation.setEnabled(true);

    keycloak.createClient(clientRepresentation);
    keycloak.createUser(userRepresentation);
    keycloak.createGroup(groupName);

    assertThat(keycloak.getClients()).hasSize(builtInClientsCount + 1);
    assertThat(keycloak.getUsers()).hasSize(2);
    assertThat(keycloak.getGroups()).hasSize(1);

    keycloak.clean();

    assertThat(keycloak.getClients()).hasSize(builtInClientsCount);
    assertThat(keycloak.getUsers()).extracting(UserRepresentation::getUsername)
        .containsExactly(KeycloakServer.DEFAULT_USERNAME);
  }

  @Test
  public void testCreateClient_Duplicate() {
    String clientName = faker.funnyName().name();

    ClientRepresentation clientRepresentation = new ClientRepresentation();
    clientRepresentation.setClientId(clientName);
    clientRepresentation.setProtocol("saml");

    keycloak.createClient(clientRepresentation);
    assertThatThrownBy(() -> keycloak.createClient(clientRepresentation)).isInstanceOf(RuntimeException.class)
        .hasMessage("Client creation failed with status code: 409 for clientId:" + clientName);
  }

  @Test
  public void testCreateUser_UsingUserRepresentation() throws Exception {
    String username = faker.name().username();
    String password = faker.internet().password();
    String email = faker.internet().emailAddress();

    Map<String, List<String>> userAttributes = new HashMap<>();
    userAttributes.put("key-01", asList("key-01-val01", "key-01-val02"));
    userAttributes.put("key-02", asList("key-02-val01", "key-02-val02"));

    String userId = createUser(username, password, email, userAttributes);

    assertThat(userId).isNotNull();

    // Use getUserById to get full user details including attributes (Keycloak 26+ doesn't include
    // attributes in list response even with briefRepresentation=false)
    UserRepresentation user = keycloak.getUserById(userId);
    assertThat(user.getEmail()).isEqualTo(email);
    assertThat(user.isEnabled()).isTrue();
    assertThat(user.getAttributes()).isEqualTo(userAttributes);

    // This asserts the user is enabled and the password is working
    assertThat(keycloak.getToken(username, password)).isNotNull();
  }

  @Test
  public void testCreateUser_UsingUserRepresentationDuplicate() {
    String username = faker.name().username();
    String password = faker.internet().password();
    String email = faker.internet().emailAddress();

    Map<String, List<String>> userAttributes = new HashMap<>();
    userAttributes.put("key-01", asList("key-01-val01", "key-01-val02"));

    createUser(username, password, email, userAttributes);
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> createUser(username, password, email, userAttributes))
        .withMessageStartingWith("User creation failed with status code: 409 for username:" + username);
  }

  @Test
  public void testCreateUser_UsingUserAttributes() throws Exception {
    Name name = faker.name();
    Internet internet = faker.internet();

    String firstName = name.firstName();
    String lastName = name.lastName();
    String username = name.username();
    String email = internet.emailAddress();
    String password = internet.password();

    String id = keycloak.createUser(firstName, lastName, username, email, password, new HashMap<>());
    assertThat(id).isNotNull();

    UserRepresentation user =
        stream(keycloak.getUsers()).filter(u -> u.getUsername().equals(username)).findFirst().get();
    assertThat(user.getEmail()).isEqualTo(email);
    assertThat(user.getFirstName()).isEqualTo(firstName);
    assertThat(user.getLastName()).isEqualTo(lastName);
    assertThat(user.isEnabled()).isTrue();
    assertThat(user.getAttributes()).isNull();

    // This asserts the user is enabled and the password is working
    assertThat(keycloak.getToken(username, password)).isNotNull();
  }

  @Test
  public void testCreateUser_UsingUserAttributesDuplicate() {
    Name name = faker.name();
    Internet internet = faker.internet();

    String firstName = name.firstName();
    String lastName = name.lastName();
    String username = name.username();
    String email = internet.emailAddress();
    String password = internet.password();

    keycloak.createUser(firstName, lastName, username, email, password, new HashMap<>());

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> keycloak.createUser(firstName, lastName, username, email, password, new HashMap<>()))
        .withMessageStartingWith("User creation failed with status code: 409 for username:" + username);
  }

  @Test
  public void testUpdateUser() {
    Name name = faker.name();
    Internet internet = faker.internet();

    String username = name.username();
    String email = internet.emailAddress();
    String password = internet.password();

    UserRepresentation user = new UserRepresentation();
    user.setUsername(username);
    user.setEmail(email);
    user.setEnabled(true);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(password);
    credential.setTemporary(false);
    user.setCredentials(Collections.singletonList(credential));

    Map<String, List<String>> userAttributes = new HashMap<>();
    userAttributes.put("foo", Collections.singletonList("bar"));
    user.setAttributes(userAttributes);

    String userId = keycloak.createUser(user);
    // Use getUserById to get full user details including attributes (Keycloak 26+)
    user = keycloak.getUserById(userId);
    assertThat(user.getAttributes().get("foo")).isEqualTo(Collections.singletonList("bar"));

    user.getAttributes().get("foo").set(0, "baz");
    keycloak.updateUser(user);

    user = keycloak.getUserById(userId);
    assertThat(user.getAttributes().get("foo")).isEqualTo(Collections.singletonList("baz"));
  }

  @Test
  public void testUpdateUser_UserDoesNotExist() {
    UserRepresentation user = new UserRepresentation();
    user.setId("no-such-user");
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> keycloak.updateUser(user))
        .withMessage("User update failed with status code: 404");
  }

  @Test
  public void testLogoutUser() throws Exception {
    Name name = faker.name();
    Internet internet = faker.internet();

    String firstName = name.firstName();
    String lastName = name.lastName();
    String username = name.username();
    String email = internet.emailAddress();
    String password = internet.password();

    String id = keycloak.createUser(firstName, lastName, username, email, password, new HashMap<>());
    keycloak.getToken(username, password); // Creates a session
    assertThat(keycloak.getSessionsOfUser(id)).hasSize(1);
    keycloak.logoutUser(id);
    assertThat(keycloak.getSessionsOfUser(id)).isEmpty();
  }

  @Test
  public void testGetSamlMetadataXml() {
    String samlMetadata = keycloak.getSamlMetadataXml();
    assertThat(samlMetadata).contains("IDPSSODescriptor");
  }

  @Test
  public void testCreateGroups() {
    assertThat(keycloak.getGroups()).hasSize(0);
    keycloak.createGroup(faker.funnyName().name());
    assertThat(keycloak.getGroups()).hasSize(1);
  }

  @Test
  public void testAssignUserToGroup() {
    Name name = faker.name();
    Internet internet = faker.internet();

    String username = name.username();
    String email = internet.emailAddress();

    UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setUsername(username);
    userRepresentation.setEmail(email);
    userRepresentation.setEnabled(true);

    keycloak.assignUserToGroup(keycloak.createUser(userRepresentation), keycloak.createGroup(faker.funnyName().name()));

    // Keycloak does not have a proper API for fetching users of a group or groups of a user.
    // This test only verifies no exceptions are thrown when keycloak.assignUserToGroup(user, group) is called.
    // The group assignment is implicitly tested in SamlTest where we check from IQ Server user is assigned.
  }

  /**
   * @return The userId of the created user in Keycloak
   */
  private String createUser(String username, String password, String email, Map<String, List<String>> userAttributes) {
    UserRepresentation user = new UserRepresentation();
    user.setUsername(username);
    user.setEmail(email);
    user.setEnabled(true);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(password);
    credential.setTemporary(false);
    user.setCredentials(Collections.singletonList(credential));

    user.setAttributes(userAttributes);

    return keycloak.createUser(user);
  }
}
