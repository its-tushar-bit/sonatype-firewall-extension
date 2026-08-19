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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.auth0.client.mgmt.filter.ConnectionFilter;
import com.auth0.client.mgmt.filter.PageFilter;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.Connection;
import com.auth0.json.mgmt.client.Addon;
import com.auth0.json.mgmt.client.Addons;
import com.auth0.json.mgmt.client.Client;
import com.auth0.json.mgmt.organizations.Branding;
import com.auth0.json.mgmt.organizations.EnabledConnection;
import com.auth0.json.mgmt.organizations.EnabledConnectionsPage;
import com.auth0.json.mgmt.organizations.Member;
import com.auth0.json.mgmt.organizations.Members;
import com.auth0.json.mgmt.organizations.Organization;
import com.auth0.json.mgmt.tickets.PasswordChangeTicket;
import com.auth0.json.mgmt.users.Identity;
import com.auth0.json.mgmt.users.User;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Auth0ManagementAPI
    extends ManagementAPI
{
  private static final Logger log = LoggerFactory.getLogger(Auth0ManagementAPI.class);

  public static final String AUTH0_APP_TYPE = "regular_web";

  // The %s will be replaced with the given URL. The URL must contain the trailing / at the end
  public static final String SAML_ENDPOINT_TEMPLATE = "%ssaml";

  public static final String AUTH0_CONNECTION_STRATEGY = "auth0";

  public static final String GOOGLE_APPS_CONNECTION_STRATEGY = "google-apps";

  public static final int TTL_SECONDS = 432000;

  public static final String IS_INVITED_FLAG = "isInvited";

  public static final String DISABLE_SIGNUP_OPTION = "disable_signup";

  public static final String PASSWORD_POLICY_OPTION = "passwordPolicy";

  public static final String PASSWORD_HISTORY_OPTION = "password_history";

  public static final String PASSWORD_NO_PERSONAL_INFO_OPTION = "password_no_personal_info";

  public static final String PASSWORD_DICTIONARY_OPTION = "password_dictionary";

  public static final int PASSWORD_HISTORY_SIZE = 4;

  // Auth0 predefined password strength policy. Check details in:
  // https://auth0.com/docs/authenticate/database-connections/password-strength#password-policies
  public static final String PASSWORD_POLICY = "good";

  public static final String RSA_SHA_256 = "rsa-sha256";

  public static final String SHA_256 = "sha256";

  public static final String INVALID_TENANT_NAME = "Tenant name cannot be blank or invalid characters <,>";

  public static final String INVALID_BLANK_TENANT_URL = "Tenant URL cannot be blank";

  public static final String INVALID_TENANT_DESCRIPTION = "Tenant description should be less that 140 characters";

  public static final String INVALID_TENANT_URL = "Tenant URL must be a valid URL";

  public static final String INVALID_LOGO_URL = "Tenant logo URL must be a valid URL";

  public static final String INVALID_CLIENT_ID = "Client id cannot be blank";

  public static final String INVALID_CONNECTION_ID = "Connection id cannot be blank";

  public static final String INVALID_CLIENT_IDS = "Client ids cannot be empty or null";

  public static final String INVALID_CONNECTION_NAME = "Connection name cannot be blank";

  public static final String INVALID_EMAIL = "Email cannot be blank";

  public static final String INVALID_ORGANIZATION_NAME = "Organization name cannot be blank";

  public static final String INVALID_ORGANIZATION_DISPLAY_NAME = "Organization display name cannot be blank";

  public static final String INVALID_ORGANIZATION_CONNECTIONS =
      "You must enable at least one connection for the organization";

  public static final String INVALID_ORGANIZATION_ID = "Organization id cannot be blank";

  public static final String INVALID_USER_IDS_LIST = "User Ids list cannot be empty or null";

  public static final String INVALID_USER_ID = "The user id cannot be blank";

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
      throw new IllegalArgumentException(INVALID_TENANT_NAME);
    }

    if (StringUtils.isBlank(tenantUrl)) {
      throw new IllegalArgumentException(INVALID_BLANK_TENANT_URL);
    }

    if (StringUtils.length(tenantDescription) > 140) {
      throw new IllegalArgumentException(INVALID_TENANT_DESCRIPTION);
    }

    validateUrl(tenantUrl, INVALID_TENANT_URL);

    validateUrl(logoUrl, INVALID_LOGO_URL);
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
    samlpAddOn.setProperty("signatureAlgorithm", RSA_SHA_256);
    samlpAddOn.setProperty("digestAlgorithm", SHA_256);
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

  public Client getClientById(final String clientId) {
    if (StringUtils.isBlank(clientId)) {
      throw new IllegalArgumentException(INVALID_CLIENT_ID);
    }

    try {
      return clients().get(clientId, null).execute();
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
        Files.writeString(tempFile, samlMetadata);
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

  public Connection updateAndGetConnectionById(String connectionId, List<String> clientIds) {
    if (StringUtils.isBlank(connectionId)) {
      throw new IllegalArgumentException(INVALID_CONNECTION_ID);
    }

    if (clientIds == null || clientIds.isEmpty()) {
      throw new IllegalArgumentException(INVALID_CLIENT_IDS);
    }

    for (String clientId : clientIds) {
      if (StringUtils.isBlank(clientId)) {
        throw new IllegalArgumentException("Client id cannot be be blank");
      }
    }

    try {
      Connection existingConnection = getConnectionById(connectionId);

      // Ensuring we can add new items to the returned list
      List<String> allClientIds = new ArrayList<>(existingConnection.getEnabledClients());
      allClientIds.addAll(clientIds);
      existingConnection.setEnabledClients(allClientIds);

      // Preparing patch request with only the enabled clients
      Connection update = new Connection();
      update.setEnabledClients(allClientIds);

      return connections().update(connectionId, update).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Connection createOrGetConnectionByName(String name, List<String> clientIds) {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException(INVALID_CONNECTION_NAME);
    }

    if (clientIds == null || clientIds.isEmpty()) {
      throw new IllegalArgumentException(INVALID_CLIENT_IDS);
    }

    for (String clientId : clientIds) {
      if (StringUtils.isBlank(clientId)) {
        throw new IllegalArgumentException("Client id cannot be be blank");
      }
    }

    try {
      Connection existing = getConnectionByName(name);

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

  public Connection getConnectionById(String connectionId) {
    if (StringUtils.isBlank(connectionId)) {
      throw new IllegalArgumentException(INVALID_CONNECTION_ID);
    }

    try {
      return connections().get(connectionId, null).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Connection getConnectionByName(String name) throws Auth0Exception {
    ConnectionFilter filter = new ConnectionFilter()
        .withFields("name,id,enabled_clients", true)
        .withName(name.toLowerCase());

    return connections().listAll(filter).execute().getItems().stream().findFirst().orElse(null);
  }

  private Connection newConnection(String name, List<String> clientIds) {
    Connection connection = new Connection(name.toLowerCase(), AUTH0_CONNECTION_STRATEGY);
    connection.setEnabledClients(clientIds);

    Map<String, Object> optionsMap = new HashMap<>();
    optionsMap.put(DISABLE_SIGNUP_OPTION, true);
    optionsMap.put(PASSWORD_POLICY_OPTION, PASSWORD_POLICY);

    Map<String, Object> passwordHistory = new HashMap<>();
    passwordHistory.put("enable", true);
    passwordHistory.put("size", PASSWORD_HISTORY_SIZE);
    optionsMap.put(PASSWORD_HISTORY_OPTION, passwordHistory);

    Map<String, Object> passwordPersonalInfo = new HashMap<>();
    passwordPersonalInfo.put("enable", true);
    optionsMap.put(PASSWORD_NO_PERSONAL_INFO_OPTION, passwordPersonalInfo);

    Map<String, Object> passwordDictionary = new HashMap<>();
    passwordDictionary.put("enable", true);
    optionsMap.put(PASSWORD_DICTIONARY_OPTION, passwordDictionary);

    connection.setOptions(optionsMap);

    return connection;
  }

  public User createOrGetUser(String email, String firstName, String lastName, String connectionName) {
    if (StringUtils.isBlank(email)) {
      throw new IllegalArgumentException(INVALID_EMAIL);
    }

    if (StringUtils.isBlank(connectionName)) {
      throw new IllegalArgumentException(INVALID_CONNECTION_NAME);
    }

    try {
      User existing = getUserByEmail(email, connectionName);

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
      throw new IllegalArgumentException(INVALID_EMAIL);
    }

    if (StringUtils.isBlank(connectionName)) {
      throw new IllegalArgumentException(INVALID_CONNECTION_NAME);
    }

    try {
      User user = getUserByEmail(email, connectionName);

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
      throw new IllegalArgumentException(INVALID_EMAIL);
    }

    if (StringUtils.isBlank(connectionId)) {
      throw new IllegalArgumentException(INVALID_CONNECTION_ID);
    }

    try {
      connections().deleteUser(connectionId, email).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean userExists(String email, String connectionName) {
    return getUserByEmail(email, connectionName) != null;
  }

  public User getUserByEmail(String email, String connectionName) {
    if (StringUtils.isBlank(email)) {
      throw new IllegalArgumentException(INVALID_EMAIL);
    }

    if (StringUtils.isBlank(connectionName)) {
      throw new IllegalArgumentException(INVALID_CONNECTION_NAME);
    }

    try {
      UserFilter filter = new UserFilter().withFields("email,user_id,identities,user_metadata", true);
      return users().listByEmail(email, filter)
          .execute()
          .stream()
          .filter(user -> user.getIdentities()
              .stream()
              .map(Identity::getConnection)
              .anyMatch(connectionName::equals))
          .findFirst()
          .orElse(null);
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  private User newUser(String email, String firstName, String lastName, String connectionName) {
    User user = new User(connectionName);
    user.setEmail(email);
    user.setGivenName(firstName);
    user.setFamilyName(lastName);
    user.setPassword(generateCommonLangPassword());
    user.setEmailVerified(true);

    Map<String, Object> userMetadata = new HashMap<>();
    userMetadata.put(IS_INVITED_FLAG, true);
    user.setUserMetadata(userMetadata);

    return user;
  }

  private char[] generateCommonLangPassword() {
    RandomStringUtils randomStringUtils = RandomStringUtils.secure();
    String upperCaseLetters = randomStringUtils.next(3, 65, 90, true, true);
    String lowerCaseLetters = randomStringUtils.next(3, 97, 122, true, true);
    String numbers = randomStringUtils.nextNumeric(3);
    String specialChar = randomStringUtils.next(3, 33, 47, false, false);
    String totalChars = randomStringUtils.nextAlphanumeric(3);
    String combinedChars = upperCaseLetters.concat(lowerCaseLetters)
        .concat(numbers)
        .concat(specialChar)
        .concat(totalChars);
    List<Character> pwdChars = combinedChars.chars()
        .mapToObj(c -> (char) c)
        .collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(pwdChars);
    return pwdChars.stream()
        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
        .toString()
        .toCharArray();
  }

  public PasswordChangeTicket createPasswordChangeTicket(
      String email,
      String connectionId,
      String clientId)
  {
    if (StringUtils.isBlank(email)) {
      throw new IllegalArgumentException(INVALID_EMAIL);
    }

    if (StringUtils.isBlank(connectionId)) {
      throw new IllegalArgumentException(INVALID_CONNECTION_ID);
    }

    if (StringUtils.isBlank(clientId)) {
      throw new IllegalArgumentException(INVALID_CLIENT_ID);
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

  public Organization createOrUpdateOrganization(
      String name,
      String displayName,
      String logoUrl,
      List<EnabledConnection> connectionsToEnable)
  {
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException(INVALID_ORGANIZATION_NAME);
    }

    if (StringUtils.isBlank(displayName)) {
      throw new IllegalArgumentException(INVALID_ORGANIZATION_DISPLAY_NAME);
    }

    if (connectionsToEnable == null || connectionsToEnable.isEmpty()) {
      throw new IllegalArgumentException(INVALID_ORGANIZATION_CONNECTIONS);
    }

    try {
      Organization existing = getOrganizationByName(name);

      Organization organization = newOrganization(name, displayName, logoUrl);

      if (existing != null) {
        return updateOrganization(connectionsToEnable, existing.getId(), organization);
      }

      return createOrganization(connectionsToEnable, organization);
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Organization newOrganization(
      String name,
      String displayName,
      String logoUrl)
  {
    Organization organization = new Organization(name);

    // Update display name
    organization.setDisplayName(displayName);

    // Update logo URL
    Branding branding = new Branding();
    branding.setLogoUrl(logoUrl);
    organization.setBranding(branding);

    return organization;
  }

  private Organization updateOrganization(
      final List<EnabledConnection> connectionsToEnable,
      final String orgId,
      final Organization organization) throws Auth0Exception
  {
    updateOrganizationConnections(connectionsToEnable, orgId);
    return organizations().update(orgId, organization).execute();
  }

  private Organization createOrganization(
      final List<EnabledConnection> connectionsToEnable,
      final Organization organization) throws Auth0Exception
  {
    organization.setEnabledConnections(connectionsToEnable);
    return organizations().create(organization).execute();
  }

  private void updateOrganizationConnections(
      final List<EnabledConnection> connectionsToEnable,
      final String orgId) throws Auth0Exception
  {
    List<EnabledConnection> enabledConnections = getEnabledConnections(orgId);

    for (EnabledConnection connectionToEnable : connectionsToEnable) {
      if (shouldBeAdded(connectionToEnable, enabledConnections)) {
        organizations().addConnection(orgId, connectionToEnable).execute();
      }
      else {
        EnabledConnection dataToUpdate = new EnabledConnection();
        dataToUpdate.setAssignMembershipOnLogin(connectionToEnable.isAssignMembershipOnLogin());
        organizations().updateConnection(orgId, connectionToEnable.getConnectionId(), dataToUpdate)
            .execute();
      }
    }
  }

  private List<EnabledConnection> getEnabledConnections(String orgId) throws Auth0Exception {
    EnabledConnectionsPage enabledConnectionsPage = organizations().getConnections(orgId, null).execute();
    List<EnabledConnection> enabledConnections = enabledConnectionsPage.getItems();

    if (enabledConnections != null && !enabledConnections.isEmpty()) {
      return enabledConnectionsPage.getItems();
    }

    return Collections.emptyList();
  }

  private boolean shouldBeAdded(EnabledConnection connectionToEnable, List<EnabledConnection> enabledConnections) {
    if (enabledConnections == null || enabledConnections.isEmpty()) {
      return true;
    }

    return enabledConnections.stream()
        .noneMatch(
            enabledConnection -> enabledConnection.getConnectionId()
                .equalsIgnoreCase(connectionToEnable.getConnectionId()));
  }

  private Organization getOrganizationByName(String name) {
    try {
      Organization organization = organizations().getByName(name).execute();

      if (organization != null && StringUtils.isNotBlank(organization.getId())) {
        return organization;
      }
    }
    catch (Auth0Exception e) {
      log.info("No organization found by name {}", name);
    }

    return null;
  }

  public Member getMemberFromOrganization(String organizationId, String email) {
    if (StringUtils.isBlank(organizationId)) {
      throw new IllegalArgumentException(INVALID_ORGANIZATION_ID);
    }

    if (StringUtils.isBlank(email)) {
      throw new IllegalArgumentException(INVALID_EMAIL);
    }

    int page = 0;
    Member member;
    List<Member> members;

    try {
      do {
        members = geMembersFromOrganization(organizationId, page);
        member = getMemberByEmail(members, email);
        page++;
      }
      while (member == null && !members.isEmpty());
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
    return member;
  }

  private List<Member> geMembersFromOrganization(String organizationId, int page) throws Auth0Exception {
    return organizations().getMembers(organizationId, new PageFilter().withPage(page, 50)).execute().getItems();
  }

  private Member getMemberByEmail(List<Member> members, String email) {
    return members.stream().filter(member -> member.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
  }

  public void addMembersToOrganization(String organizationId, List<String> userIds) {
    validateOrganizationIdAndUserIds(organizationId, userIds);

    try {
      organizations().addMembers(organizationId, new Members(userIds)).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void removeMembersFromOrganization(String organizationId, List<String> userIds) {
    validateOrganizationIdAndUserIds(organizationId, userIds);

    try {
      organizations().deleteMembers(organizationId, new Members(userIds)).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void validateOrganizationIdAndUserIds(String organizationId, List<String> userIds) {
    if (StringUtils.isBlank(organizationId)) {
      throw new IllegalArgumentException(INVALID_ORGANIZATION_ID);
    }

    if (userIds == null || userIds.isEmpty()) {
      throw new IllegalArgumentException(INVALID_USER_IDS_LIST);
    }

    for (String userId : userIds) {
      if (StringUtils.isBlank(userId)) {
        throw new IllegalArgumentException(INVALID_USER_ID);
      }
    }
  }

  public void deleteOrganization(String organizationId) {
    if (StringUtils.isBlank(organizationId)) {
      throw new IllegalArgumentException(INVALID_ORGANIZATION_ID);
    }

    try {
      organizations().delete(organizationId).execute();
    }
    catch (Auth0Exception e) {
      throw new RuntimeException(e);
    }
  }
}
