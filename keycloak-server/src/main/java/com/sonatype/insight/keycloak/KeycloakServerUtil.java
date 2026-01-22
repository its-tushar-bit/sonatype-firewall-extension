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

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Exposes utility methods for a running Keycloak on a given url.
// For using in tests, please see KeycloakTestUtilTest for reference which is responsible for
// ignoring tests when -Dkeycloak.optional=true and stopping server / container gracefully
public class KeycloakServerUtil
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  static final Integer ADMIN_TOKEN_LIFESPAN_IN_SECONDS = 600;

  private String url;

  private String adminToken;

  private Set<String> createdClientIds;

  private Set<String> createdUserIds;

  private Set<String> createdGroupIds;

  KeycloakServerUtil() {
  }

  void init(String url) throws InterruptedException {
    this.url = url;
    RealmRepresentation realmRepresentation = getMasterRealm();
    realmRepresentation.setAccessTokenLifespan(ADMIN_TOKEN_LIFESPAN_IN_SECONDS);
    ClientBuilder.newClient().target(url).path("admin/realms/master").request()
        .header("Authorization", "Bearer " + getToken(KeycloakServer.DEFAULT_USERNAME, KeycloakServer.DEFAULT_PASSWORD))
        .put(Entity.entity(realmRepresentation, MediaType.APPLICATION_JSON_TYPE));
    adminToken = getToken(KeycloakServer.DEFAULT_USERNAME, KeycloakServer.DEFAULT_PASSWORD);

    createdClientIds = new HashSet<>();
    createdUserIds = new HashSet<>();
    createdGroupIds = new HashSet<>();
  }

  /**
   * @return Bearer token for the passed in user
   * @throws InterruptedException
   */
  public String getToken(String username, String password) throws InterruptedException {
    for (int i = 5;; i--) {
      try {
        return ClientBuilder.newClient().target(url).path("realms/master/protocol/openid-connect/token")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(new Form().param("username", username).param("password", password)
                .param("client_id", "admin-cli").param("grant_type", "password"),
                MediaType.APPLICATION_FORM_URLENCODED_TYPE), Token.class).accessToken;
      }
      catch (NotAuthorizedException e) {
        if (i <= 0) {
          throw e;
        }
        log.warn("Failed to get keycloak token for user {}. Will retry. Error: {}", username, e.getMessage(), e);
        Thread.sleep(1000);
      }
    }
  }

  public void createClient(ClientRepresentation client) {
    log.info("KeycloakServerUtil.createClient() start clientId:{}", client.getClientId());

    Response response = ClientBuilder.newClient().target(url).path("admin/realms/master/clients").request()
        .header("Authorization", "Bearer " + adminToken)
        .post(Entity.entity(client, MediaType.APPLICATION_JSON));

    if (response.getStatus() == Status.CREATED.getStatusCode()) {
      String newId =
          Arrays.stream(getClients()).filter(c -> c.getClientId().equals(client.getClientId())).findAny().get().getId();
      createdClientIds.add(newId);

      log.info("KeycloakServerUtil.createClient() end clientId:{}, new id:{}", client.getClientId(), newId);
    }
    else {
      log.error("KeycloakServerUtil.createClient() end clientId:{} failed", client.getClientId());
      throw new RuntimeException(
          "Client creation failed with status code: " + response.getStatus() + " for clientId:" + client.getClientId());
    }
  }

  public ClientRepresentation createClientRepresentation(String xmlMetadata) {
    return ClientBuilder.newClient().target(url).path("admin/realms/master/client-description-converter").request()
        .header("Authorization", "Bearer " + adminToken)
        .post(Entity.xml(xmlMetadata), ClientRepresentation.class);
  }

  /**
   * @return Returns the id assigned to the created user.
   */
  public String createUser(UserRepresentation user) {
    log.info("KeycloakServerUtil.createUser() start username:{}", user.getUsername());

    Response response = ClientBuilder.newClient().target(url).path("admin/realms/master/users").request()
        .header("Authorization", "Bearer " + adminToken)
        .post(Entity.entity(user, MediaType.APPLICATION_JSON));

    if (response.getStatus() == Status.CREATED.getStatusCode()) {
      String newId =
          Arrays.stream(getUsers()).filter(u -> u.getUsername().equals(user.getUsername())).findAny().get().getId();
      createdUserIds.add(newId);

      log.info("KeycloakServerUtil.createUser() end username:{}, new id:{}", user.getUsername(), newId);

      return newId;
    }
    else {
      String responseBody = response.readEntity(String.class);
      log.error("KeycloakServerUtil.createUser() end username:{} failed, response: {}", user.getUsername(),
          responseBody);
      throw new RuntimeException(
          "User creation failed with status code: " + response.getStatus() + " for username:" + user.getUsername() +
          ", response: " + responseBody);
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
    log.info("KeycloakServerUtil.createGroup() start groupName:{}", groupName);

    GroupRepresentation groupRepresentation = new GroupRepresentation();
    groupRepresentation.setName(groupName);
    // Note: Do NOT set path - Keycloak 23+ computes it automatically as "/<groupName>"

    Response response = ClientBuilder.newClient().target(url).path("admin/realms/master/groups").request()
        .header("Authorization", "Bearer " + adminToken)
        .post(Entity.entity(groupRepresentation, MediaType.APPLICATION_JSON));

    if (response.getStatus() == Status.CREATED.getStatusCode()) {
      String newId = Arrays.stream(getGroups()).filter(g -> g.getName().equals(groupName)).findAny().get().getId();
      createdGroupIds.add(newId);

      log.info("KeycloakServerUtil.createGroup() end groupName:{}, new id:{}", groupName, newId);

      return newId;
    }
    else {
      log.error("KeycloakServerUtil.createGroup() end groupName:{} failed", groupName);
      throw new RuntimeException(
          "Group creation failed with status code: " + response.getStatus() + " for groupName:" + groupName);
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
   */
  public void logoutUser(String userId) {
    ClientBuilder.newClient().target(url).path("admin/realms/master/users").path(userId).path("logout").request()
        .header("Authorization", "Bearer " + adminToken).post(null);
  }

  public String getSamlMetadataXml() {
    return ClientBuilder.newClient().target(url).path("realms/master/protocol/saml/descriptor").request()
        .get(String.class);
  }

  public String getBaseUrl() {
    return url;
  }

  /**
   * Maps SAML attribute firstName to users first name. Maps SAML attribute lastName to users last name. Maps SAML
   * attribute groups to users groups.
   *
   * @return All the mappings created in a list.
   */
  public static List<ProtocolMapperRepresentation> protocolMappers() {
    ProtocolMapperRepresentation firstNameMapping = new ProtocolMapperRepresentation();
    firstNameMapping.setName("firstName");
    firstNameMapping.setProtocol("saml");
    firstNameMapping.setProtocolMapper("saml-user-property-mapper");
    firstNameMapping.getConfig().put("attribute.nameformat", "Basic");
    firstNameMapping.getConfig().put("user.attribute", "firstName");
    firstNameMapping.getConfig().put("friendly.name", "firstName");
    firstNameMapping.getConfig().put("attribute.name", "firstName");

    ProtocolMapperRepresentation lastNameMapping = new ProtocolMapperRepresentation();
    lastNameMapping.setName("lastName");
    lastNameMapping.setProtocol("saml");
    lastNameMapping.setProtocolMapper("saml-user-property-mapper");
    lastNameMapping.getConfig().put("attribute.nameformat", "Basic");
    lastNameMapping.getConfig().put("user.attribute", "lastName");
    lastNameMapping.getConfig().put("friendly.name", "lastName");
    lastNameMapping.getConfig().put("attribute.name", "lastName");

    ProtocolMapperRepresentation groupsMapping = new ProtocolMapperRepresentation();
    groupsMapping.setName("groups");
    groupsMapping.setProtocol("saml");
    groupsMapping.setProtocolMapper("saml-group-membership-mapper");
    groupsMapping.getConfig().put("attribute.nameformat", "Basic");
    groupsMapping.getConfig().put("name", "Group List");
    groupsMapping.getConfig().put("friendly.name", "Groups");
    groupsMapping.getConfig().put("attribute.name", "groups");

    return Arrays.asList(firstNameMapping, lastNameMapping, groupsMapping);
  }

  /**
   * Cleanup the test keycloak server.
   */
  void clean() {
    log.info("KeycloakServerUtil.clean() start");

    for (String clientId : createdClientIds) {
      log.info("KeycloakServerUtil.clean() deleting clientId:{}", clientId);
      Response response = ClientBuilder.newClient().target(url).path("admin/realms/master/clients").path(clientId)
          .request().header("Authorization", "Bearer " + adminToken).delete();
      if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {
        throw new IllegalStateException(
            "Client clean failed with Status Code: " + response.getStatus() + " for clientId:" + clientId);
      }
      log.info("KeycloakServerUtil.clean() deleted clientId:{}", clientId);
    }
    log.info("KeycloakServerUtil.clean() deleted {} clientIds", createdClientIds.size());
    createdClientIds.clear();

    for (String userId : createdUserIds) {
      log.info("KeycloakServerUtil.clean() deleting userId:{}", userId);
      Response response = ClientBuilder.newClient().target(url).path("admin/realms/master/users").path(userId).request()
          .header("Authorization", "Bearer " + adminToken).delete();
      if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {
        throw new IllegalStateException(
            "User clean failed with Status Code: " + response.getStatus() + " for userId:" + userId);
      }
      log.info("KeycloakServerUtil.clean() deleted userId:{}", userId);
    }
    log.info("KeycloakServerUtil.clean() deleted {} userIds", createdUserIds.size());
    createdUserIds.clear();

    for (String groupId : createdGroupIds) {
      log.info("KeycloakServerUtil.clean() deleting groupId:{}", groupId);
      Response response = ClientBuilder.newClient().target(url).path("admin/realms/master/groups").path(groupId)
          .request().header("Authorization", "Bearer " + adminToken).delete();
      if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {
        throw new IllegalStateException(
            "Group clean failed with Status Code: " + response.getStatus() + " for groupId:" + groupId);
      }
      log.info("KeycloakServerUtil.clean() deleted groupId:{}", groupId);
    }
    log.info("KeycloakServerUtil.clean() deleted {} groupIds", createdGroupIds.size());
    createdGroupIds.clear();

    log.info("KeycloakServerUtil.clean() end");
  }

  ClientRepresentation[] getClients() {
    return ClientBuilder.newClient().target(url).path("admin/realms/master/clients").request()
        .header("Authorization", "Bearer " + adminToken).get(ClientRepresentation[].class);
  }

  UserRepresentation[] getUsers() {
    // briefRepresentation=false is required for Keycloak 26+ to include user attributes in the response
    return ClientBuilder.newClient().target(url).path("admin/realms/master/users")
        .queryParam("briefRepresentation", "false")
        .request()
        .header("Authorization", "Bearer " + adminToken).get(UserRepresentation[].class);
  }

  UserRepresentation getUserById(String userId) {
    // Getting a user by ID returns full user details including attributes
    return ClientBuilder.newClient().target(url).path("admin/realms/master/users").path(userId)
        .request()
        .header("Authorization", "Bearer " + adminToken).get(UserRepresentation.class);
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

  RealmRepresentation getMasterRealm() throws InterruptedException {
    return ClientBuilder.newClient().target(url).path("admin/realms/master").request()
        .header("Authorization", "Bearer " + getToken(KeycloakServer.DEFAULT_USERNAME, KeycloakServer.DEFAULT_PASSWORD))
        .get(RealmRepresentation.class);
  }

  /**
   * Enable unmanaged attributes in the User Profile configuration.
   * Required for Keycloak 24+ where User Profile is enabled by default and only managed attributes are returned.
   */
  public void enableUnmanagedAttributes() {
    log.info("KeycloakServerUtil.enableUnmanagedAttributes() - configuring realm to allow unmanaged user attributes");

    // The User Profile configuration needs to have unmanagedAttributePolicy set to ENABLED
    // This is done via the realm's user profile endpoint
    // Note: firstName, lastName, email are optional (no "required" field) for test compatibility
    @SuppressWarnings("checkstyle:LineLength")
    String userProfileConfig = """
        {
          "attributes": [
            {"name": "username", "displayName": "${username}", "validations": {"length": {"min": 3, "max": 255}}, "permissions": {"view": ["admin", "user"], "edit": ["admin", "user"]}, "multivalued": false},
            {"name": "email", "displayName": "${email}", "validations": {"email": {}, "length": {"max": 255}}, "permissions": {"view": ["admin", "user"], "edit": ["admin", "user"]}, "multivalued": false},
            {"name": "firstName", "displayName": "${firstName}", "validations": {"length": {"max": 255}}, "permissions": {"view": ["admin", "user"], "edit": ["admin", "user"]}, "multivalued": false},
            {"name": "lastName", "displayName": "${lastName}", "validations": {"length": {"max": 255}}, "permissions": {"view": ["admin", "user"], "edit": ["admin", "user"]}, "multivalued": false}
          ],
          "groups": [{"name": "user-metadata", "displayHeader": "User metadata", "displayDescription": "Attributes, which refer to user metadata"}],
          "unmanagedAttributePolicy": "ENABLED"
        }
        """;

    Response response = ClientBuilder.newClient().target(url).path("admin/realms/master/users/profile").request()
        .header("Authorization", "Bearer " + adminToken)
        .put(Entity.entity(userProfileConfig, MediaType.APPLICATION_JSON));

    if (response.getStatus() != Status.OK.getStatusCode()) {
      String responseBody = response.readEntity(String.class);
      log.error("Failed to enable unmanaged attributes: {} - {}", response.getStatus(), responseBody);
      throw new RuntimeException(
          "Failed to enable unmanaged attributes: " + response.getStatus() + " - " + responseBody);
    }
    log.info("KeycloakServerUtil.enableUnmanagedAttributes() - unmanaged attributes enabled successfully");
  }
}
