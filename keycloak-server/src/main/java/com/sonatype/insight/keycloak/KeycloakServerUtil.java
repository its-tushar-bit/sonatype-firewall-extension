/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.keycloak;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;

// Exposes utility methods per https://www.keycloak.org/docs-api/6.0/rest-api/ for a running Keycloak on a given url.
// For using in tests, please see KeycloakTestUtilTest for reference which is responsible for
// ignoring tests when -Dkeycloak.optional=true and stopping server / container gracefully
// Call #clean whenever you need to reset the server to original state.
public class KeycloakServerUtil
{
  static final Integer ADMIN_TOKEN_LIFESPAN_IN_SECONDS = 600;

  private final String url;

  private final String adminToken;

  private final Set<String> createdClientIds = new HashSet<>();

  private final Set<String> createdUserIds = new HashSet<>();

  private final Set<String> createdGroupIds = new HashSet<>();

  public KeycloakServerUtil(String url) {
    this.url = url;
    RealmRepresentation realmRepresentation = getMasterRealm();
    realmRepresentation.setAccessTokenLifespan(ADMIN_TOKEN_LIFESPAN_IN_SECONDS);
    ClientBuilder.newClient().target(url).path("admin/realms/master").request()
        .header("Authorization", "Bearer " + getToken(KeycloakServer.USERNAME, KeycloakServer.PASSWORD))
        .put(Entity.entity(realmRepresentation, MediaType.APPLICATION_JSON_TYPE));
    adminToken = getToken(KeycloakServer.USERNAME, KeycloakServer.PASSWORD);
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
        .header("Authorization", "Bearer " + adminToken)
        .post(Entity.entity(client, MediaType.APPLICATION_JSON));

    if (response.getStatus() == Status.CREATED.getStatusCode()) {
      createdClientIds.add(
          Arrays.stream(getClients()).filter(c -> c.getClientId().equals(client.getClientId())).findAny().get()
              .getId());
    }
    else {
      throw new RuntimeException("Client creation failed.");
    }
  }

  /**
   * @see <a href="https://www.keycloak.org/docs-api/6.0/rest-api/#_convertclientdescription">Convert Client</a>
   * @see <a href="https://www.keycloak.org/docs-api/6.0/rest-api/#_clientrepresentation">Client Representation</a>
   */
  public ClientRepresentation createClientRepresentation(String xmlMetadata) {
    return ClientBuilder.newClient().target(url).path("admin/realms/master/client-description-converter").request()
        .header("Authorization", "Bearer " + adminToken)
        .post(Entity.xml(xmlMetadata), ClientRepresentation.class);
  }

  /**
   * @return Returns the id assigned to the created user.
   * @see <a href="https://www.keycloak.org/docs-api/6.0/rest-api/#_users_resource">Users Resource</a>
   * @see <a href="https://www.keycloak.org/docs-api/6.0/rest-api/#_userrepresentation">User Representation</a>
   */
  public String createUser(UserRepresentation user) {
    Response response = ClientBuilder.newClient().target(url).path("admin/realms/master/users").request()
        .header("Authorization", "Bearer " + adminToken)
        .post(Entity.entity(user, MediaType.APPLICATION_JSON));

    if (response.getStatus() == Status.CREATED.getStatusCode()) {
      String id =
          Arrays.stream(getUsers()).filter(u -> u.getUsername().equals(user.getUsername())).findAny().get().getId();
      createdUserIds.add(id);
      return id;
    }
    else {
      throw new RuntimeException("User creation failed with status code: " + response.getStatus());
    }
  }

  /**
   * Convenience method to create a user from parameters directly
   *
   * @return Returns the id assigned to the created user.
   */
  public String createUser(
      String firstName,
      String lastName,
      String username,
      String email,
      String password,
      Map<String, List<String>> attributes)
  {
    UserRepresentation user = new UserRepresentation();
    user.setUsername(username);
    user.setEmail(email);
    user.setEnabled(true);
    user.setFirstName(firstName);
    user.setLastName(lastName);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(password);
    credential.setTemporary(false);
    user.setCredentials(Arrays.asList(credential));

    user.setAttributes(attributes);

    return createUser(user);
  }

  public String createGroup(String groupName) {
    GroupRepresentation groupRepresentation = new GroupRepresentation();
    groupRepresentation.setName(groupName);
    groupRepresentation.setPath(groupName);

    Response response = ClientBuilder.newClient().target(url).path("admin/realms/master/groups").request()
        .header("Authorization", "Bearer " + adminToken)
        .post(Entity.entity(groupRepresentation, MediaType.APPLICATION_JSON));

    if (response.getStatus() == Status.CREATED.getStatusCode()) {
      String id = Arrays.stream(getGroups()).filter(g -> g.getName().equals(groupName)).findAny().get().getId();
      createdGroupIds.add(id);
      return id;
    }
    else {
      throw new RuntimeException("Group creation failed.");
    }
  }

  public void assignUserToGroup(String userId, String groupId) {
    Response response =
        ClientBuilder.newClient().target(url).path("admin/realms/master/users").path(userId).path("groups")
            .path(groupId).request()
            .header("Authorization", "Bearer " + adminToken)
            // Keycloak API for assigning users to groups is exposed for PUT but does not care about body
            // Jersey does not like null in PUT hence we are sending some empty json here
            .put(Entity.json("{}"));

    if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {
      throw new RuntimeException("User could not be assigned to group: " + response.getStatus());
    }
  }

  /**
   * @see <a href="https://www.keycloak.org/docs-api/6.0/rest-api/#_users_resource">Users Resource</a>
   * @see <a href="https://www.keycloak.org/docs-api/6.0/rest-api/#_userrepresentation">User Representation</a>
   */
  public void updateUser(UserRepresentation user) {
    Response response =
        ClientBuilder.newClient().target(url).path("admin/realms/master/users").path(user.getId()).request()
            .header("Authorization", "Bearer " + adminToken)
            .put(Entity.entity(user, MediaType.APPLICATION_JSON));

    if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {
      throw new RuntimeException("User update failed with status code: " + response.getStatus());
    }
  }

  /**
   * Remove all user sessions associated with the user
   * Also send notification to all clients that have an admin URL to invalidate the sessions for the particular user
   *
   * @see <a href="https://www.keycloak.org/docs-api/6.0/rest-api/#_users_resource">Users Resource</a>
   * @see <a href="https://www.keycloak.org/docs-api/6.0/rest-api/#_userrepresentation">User Representation</a>
   */
  public void logoutUser(String userId) {
    ClientBuilder.newClient().target(url).path("admin/realms/master/users").path(userId).path("logout").request()
        .header("Authorization", "Bearer " + adminToken).post(null);
  }

  public String getSamlMetadataXml() {
    return ClientBuilder.newClient().target(url).path("realms/master/protocol/saml/descriptor").request()
        .get(String.class);
  }

  public String getUrl() {
    return url;
  }

  public void clean() {
    for (String clientId : createdClientIds) {
      Response response =
          ClientBuilder.newClient().target(url).path("admin/realms/master/clients").path(clientId).request()
              .header("Authorization", "Bearer " + adminToken).delete();
      if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {
        throw new IllegalStateException("Client clean failed with Status Code: " + response.getStatus());
      }
    }
    createdClientIds.clear();

    for (String userId : createdUserIds) {
      Response response =
          ClientBuilder.newClient().target(url).path("admin/realms/master/users").path(userId).request()
              .header("Authorization", "Bearer " + adminToken).delete();
      if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {
        throw new IllegalStateException("User clean failed with Status Code: " + response.getStatus());
      }
    }
    createdUserIds.clear();

    for (String groupId : createdGroupIds) {
      Response response =
          ClientBuilder.newClient().target(url).path("admin/realms/master/groups").path(groupId).request()
              .header("Authorization", "Bearer " + adminToken).delete();
      if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {
        throw new IllegalStateException("Group clean failed with Status Code: " + response.getStatus());
      }
    }
    createdGroupIds.clear();
  }

  ClientRepresentation[] getClients() {
    return ClientBuilder.newClient().target(url).path("admin/realms/master/clients").request()
        .header("Authorization", "Bearer " + adminToken).get(ClientRepresentation[].class);
  }

  UserRepresentation[] getUsers() {
    return ClientBuilder.newClient().target(url).path("admin/realms/master/users").request()
        .header("Authorization", "Bearer " + adminToken).get(UserRepresentation[].class);
  }

  GroupRepresentation[] getGroups() {
    return ClientBuilder.newClient().target(url).path("admin/realms/master/groups").request()
        .header("Authorization", "Bearer " + adminToken).get(GroupRepresentation[].class);
  }

  UserSessionRepresentation[] getSessionsOfUser(String userId) {
    return ClientBuilder.newClient().target(url).path("admin/realms/master/users").path(userId).path("sessions")
        .request()
        .header("Authorization", "Bearer " + adminToken).get(UserSessionRepresentation[].class);
  }

  RealmRepresentation getMasterRealm() {
    return ClientBuilder.newClient().target(url).path("admin/realms/master").request()
        .header("Authorization", "Bearer " + getToken(KeycloakServer.USERNAME, KeycloakServer.PASSWORD))
        .get(RealmRepresentation.class);
  }
}
