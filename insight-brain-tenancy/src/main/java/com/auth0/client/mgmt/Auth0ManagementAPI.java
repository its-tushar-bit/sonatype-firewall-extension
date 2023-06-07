/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.auth0.client.mgmt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.auth0.client.mgmt.filter.ConnectionFilter;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.Connection;
import com.auth0.json.mgmt.client.Addon;
import com.auth0.json.mgmt.client.Addons;
import com.auth0.json.mgmt.client.Client;
import com.auth0.json.mgmt.tickets.PasswordChangeTicket;
import com.auth0.json.mgmt.users.Identity;
import com.auth0.json.mgmt.users.User;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;

public class Auth0ManagementAPI
    extends ManagementAPI
{
  public static final String AUTH0_APP_TYPE = "regular_web";

  // The %s will be replaced with the given URL. The URL must contain the trailing / at the end
  public static final String SAML_ENDPOINT_TEMPLATE = "%ssaml";

  public static final String AUTH0_CONNECTION_STRATEGY = "auth0";

  public static final int TTL_SECONDS = 432000;

  public static final String IS_INVITED_FLAG = "isInvited";

  private final String apiToken;

  public Auth0ManagementAPI(final String domain, final String apiToken) {
    super(domain, apiToken);
    this.apiToken = apiToken;
  }

  @Override
  public OkHttpClient getClient() {
    return super.getClient();
  }

  public Client createOrUpdateTenant(
      final String name,
      final String tenantUrl,
      final String tenantDescription,
      final String logoUrl,
      final String clientId)
  {
    validateClientData(name, tenantUrl, tenantDescription, logoUrl);
    Client client = newClient(name, tenantUrl, tenantDescription, logoUrl);
    return createOrUpdateClient(client, clientId);
  }

  private void validateClientData(
      final String tenantName,
      final String tenantUrl,
      final String tenantDescription,
      final String logoUrl)
  {
    if (StringUtils.isBlank(tenantName) || StringUtils.containsAny(tenantName, "<", ">")) {
      throw new IllegalArgumentException("Tenant name cannot be blank or invalid characters <,>");
    }

    if (StringUtils.isBlank(tenantUrl)) {
      throw new IllegalArgumentException("Tenant URL cannot be blank");
    }

    if (StringUtils.length(tenantDescription) > 140) {
      throw new IllegalArgumentException("Tenant description should be less that 140 characters");
    }

    validateUrl(tenantUrl, "Tenant URL must be a valid URL");

    validateUrl(logoUrl, "Tenant logo URL must be a valid URL");
  }

  private static void validateUrl(final String url, final String errorMessage) {
    if (StringUtils.isNotBlank(url)) {
      try {
        new URL(url);
      }
      catch (MalformedURLException e) {
        throw new IllegalArgumentException(errorMessage, e);
      }
    }
  }

  private Client newClient(
      final String name,
      final String tenantUrl,
      final String tenantDescription,
      final String logoUrl)
  {
    Client client = new Client(name);
    client.setAppType(AUTH0_APP_TYPE);
    client.setDescription(tenantDescription);
    client.setCallbacks(Collections.singletonList(getTenantSamlEndpoint(tenantUrl)));
    client.setLogoUri(logoUrl);
    client.setInitiateLoginUri(tenantUrl);
    client.setAllowedLogoutUrls(Collections.singletonList(tenantUrl));
    client.setCrossOriginAuth(false);
    client.setGrantTypes(Arrays.asList("authorization_code", "implicit"));
    Addon samlpAddOn = new Addon();
    Addons addOns = new Addons(null, null, null, null);
    addOns.setAdditionalAddon("samlp", samlpAddOn);
    client.setAddons(addOns);
    return client;
  }

  private String getTenantSamlEndpoint(final String tenantUrl) {
    return String.format(SAML_ENDPOINT_TEMPLATE, tenantUrl);
  }

  private Client createOrUpdateClient(Client client, String clientId) {
    try {
      if (StringUtils.isNotBlank(clientId)) {
        return clients().update(clientId, client).execute();
      }

      return clients().create(client).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  public File getSamlMetaDataFile(String auth0ClientId) {
    String samlMetadata = getSamlMetaData(auth0ClientId);

    if (samlMetadata != null) {
      try {
        Path tempFile = Files.createTempFile(auth0ClientId + "-", "-samlmetadata.xml");
        Files.write(tempFile, samlMetadata.getBytes(StandardCharsets.UTF_8));
        return tempFile.toFile();
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return null;
  }

  public String getSamlMetaData(String auth0ClientId) {
    String url = getBaseUrl()
        .newBuilder()
        .addPathSegments("samlp/metadata/")
        .addPathSegment(auth0ClientId)
        .build()
        .toString();

    return downloadSamlMetadata(url);
  }

  String downloadSamlMetadata(String url) {
    Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + apiToken).build();
    try (Response response = getClient().newCall(request).execute()) {
      ResponseBody body = response.body();
      return body != null ? body.string() : null;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Connection createOrUpdateConnection(String name, List<String> clientIds) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("Connection name cannot be blank");
    }

    if (clientIds == null || clientIds.isEmpty()) {
      throw new IllegalArgumentException("Client ids cannot be empty or null");
    }

    for (String clientId : clientIds) {
      if (StringUtils.isBlank(clientId)) {
        throw new IllegalArgumentException("Client id cannot be be blank");
      }
    }

    try {
      Connection existing = findConnectionByName(name);

      if (existing != null) {
        return existing;
      }

      Connection connection = newConnection(name, clientIds);

      return connections().create(connection).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Connection findConnectionByName(String name) throws Auth0Exception {
    ConnectionFilter filter = new ConnectionFilter()
        .withFields("name,id,enabled_clients", true)
        .withName(name.toLowerCase());

    return connections().listAll(filter).execute().getItems().stream().findFirst().orElse(null);
  }

  private Connection newConnection(String name, List<String> clientIds) {
    Connection connection = new Connection(name.toLowerCase(), AUTH0_CONNECTION_STRATEGY);
    connection.setEnabledClients(clientIds);

    return connection;
  }

  public User createOrGetUser(String email, String firstName, String lastName, String connectionName) {
    if (StringUtils.isBlank(email)) {
      throw new IllegalArgumentException("Email cannot be blank");
    }

    if (StringUtils.isBlank(connectionName)) {
      throw new IllegalArgumentException("Connection name cannot be blank");
    }

    try {
      User existing = findUserByEmail(email, connectionName);

      if (existing != null) {
        return existing;
      }

      User user = newUser(email, firstName, lastName, connectionName);

      return users().create(user).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void deleteUserByEmail(String email, String connectionName) {
    if (StringUtils.isBlank(email)) {
      throw new IllegalArgumentException("Email cannot be blank");
    }

    if (StringUtils.isBlank(connectionName)) {
      throw new IllegalArgumentException("Connection name cannot be blank");
    }

    try {
      User user = findUserByEmail(email, connectionName);

      if (user != null) {
        users().delete(user.getId()).execute();
      }
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void deleteUserByEmailFromConnection(String email, String connectionId) {
    if (StringUtils.isBlank(email)) {
      throw new IllegalArgumentException("Email cannot be blank");
    }

    if (StringUtils.isBlank(connectionId)) {
      throw new IllegalArgumentException("Connection id cannot be blank");
    }

    try {
      connections().deleteUser(connectionId, email).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean userExists(String email, String connectionName) throws Auth0Exception {
    return findUserByEmail(email, connectionName) != null;

  }

  private User findUserByEmail(String email, String connectionName) throws Auth0Exception {
    UserFilter filter = new UserFilter().withFields("email,user_id,identities", true);
    return users().listByEmail(email, filter).execute().stream()
        .filter(user -> user.getIdentities().stream().map(Identity::getConnection)
            .anyMatch(connectionName::equals)).findFirst().orElse(null);
  }

  private User newUser(String email, String firstName, String lastName, String connectionName) {
    User user = new User(connectionName);
    user.setEmail(email);
    user.setGivenName(firstName);
    user.setFamilyName(lastName);
    user.setPassword(UUID.randomUUID().toString().toCharArray());
    user.setEmailVerified(true);

    Map<String, Object> userMetadata = new HashMap<>();
    userMetadata.put(IS_INVITED_FLAG, true);
    user.setUserMetadata(userMetadata);

    return user;
  }

  public PasswordChangeTicket createPasswordChangeTicket(
      String email,
      String connectionId,
      String clientId)
  {
    if (StringUtils.isBlank(email)) {
      throw new IllegalArgumentException("Email cannot be blank");
    }

    if (StringUtils.isBlank(connectionId)) {
      throw new IllegalArgumentException("Connection id cannot be blank");
    }

    if (StringUtils.isBlank(clientId)) {
      throw new IllegalArgumentException("Client id cannot be blank");
    }

    try {
      PasswordChangeTicket passwordChangeTicket = new PasswordChangeTicket(email, connectionId);
      passwordChangeTicket.setMarkEmailAsVerified(true);
      passwordChangeTicket.setClientId(clientId);
      passwordChangeTicket.setTTLSeconds(TTL_SECONDS);

      return tickets().requestPasswordChange(passwordChangeTicket).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }
}
