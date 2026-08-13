/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.auth0.client.mgmt;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.auth0.client.mgmt.filter.ConnectionFilter;
import com.auth0.client.mgmt.filter.FieldsFilter;
import com.auth0.client.mgmt.filter.PageFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.Connection;
import com.auth0.json.mgmt.ConnectionsPage;
import com.auth0.json.mgmt.client.Client;
import com.auth0.json.mgmt.organizations.EnabledConnection;
import com.auth0.json.mgmt.organizations.EnabledConnectionsPage;
import com.auth0.json.mgmt.organizations.Member;
import com.auth0.json.mgmt.organizations.Members;
import com.auth0.json.mgmt.organizations.MembersPage;
import com.auth0.json.mgmt.organizations.Organization;
import com.auth0.json.mgmt.tickets.PasswordChangeTicket;
import com.auth0.json.mgmt.users.Identity;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.Request;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Auth0ManagementAPITest
{
  @Captor
  private ArgumentCaptor<Client> clientCaptor;

  @Captor
  private ArgumentCaptor<String> clientIdCaptor;

  @Captor
  private ArgumentCaptor<ConnectionFilter> connectionFilterCaptor;

  @Captor
  private ArgumentCaptor<Connection> connectionCaptor;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  @Captor
  private ArgumentCaptor<String> emailCaptor;

  @Captor
  private ArgumentCaptor<String> userIdCaptor;

  @Captor
  private ArgumentCaptor<FieldsFilter> fieldsFilterCaptor;

  @Captor
  private ArgumentCaptor<PasswordChangeTicket> passwordChangeTicketCaptor;

  @Captor
  private ArgumentCaptor<Organization> organizationCaptor;

  @Captor
  private ArgumentCaptor<PageFilter> pageFilerCaptor;

  @Captor
  private ArgumentCaptor<Members> membersCaptor;

  public Auth0ManagementAPI auth0ManagementAPI;

  private ClientsEntity mockClientsEntity;

  private ConnectionsEntity mockConnectionsEntity;

  private UsersEntity mockUsersEntity;

  private TicketsEntity mockTicketsEntity;

  private OrganizationsEntity mockOrganizationsEntity;

  private Request<Client> createMockRequest;

  private Request<Client> updateMockRequest;

  private Request<Client> getClientMockRequest;

  private Request<Connection> createConnectionMockRequest;

  private Request<Connection> updateConnectionMockRequest;

  private Request<ConnectionsPage> listConnectionsMockRequest;

  private Request<Connection> getConnectionMockRequest;

  private Request<User> createUserMockRequest;

  private Request<Void> deleteUserMockRequest;

  private Request<List<User>> listUsersMockRequest;

  private Request<PasswordChangeTicket> passwordChangeTicketMockRequest;

  private Request<Organization> createOrganizationMockRequest;

  private Request<Organization> updateOrganizationMockRequest;

  private Request<Organization> getOrganizationMockRequest;

  private Request<EnabledConnection> addConnectionToOrganizationMockRequest;

  private Request<EnabledConnectionsPage> getEnabledConnectionMockRequest;

  private Request<EnabledConnection> updateConnectionForOrganizationMockRequest;

  private Request<MembersPage> getOrganizationMembersMockRequest;

  private Request<Void> addMembersMockRequest;

  private Request<Void> deleteMembersMockRequest;

  private Request<Void> deleteOrganizationMockRequest;

  @BeforeEach
  public void before() {
    auth0ManagementAPI =
        spy(new Auth0ManagementAPI("https://sonatype.auth0.com", "abcdefg"));
  }

  @Test
  public void testCreate_validateTenantName_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrUpdateTenant("", "http://tenant1.sonatype.app", "blah",
            "http://tenant1.com/logo.gif", ""))
        .withMessage(Auth0ManagementAPI.INVALID_TENANT_NAME);
  }

  @Test
  public void testCreate_validateTenantSubdomain_InvalidCharacters() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrUpdateTenant("<sub-domain>", "http://tenant1.sonatype.app", "blah",
            "http://tenant1.com/logo.gif", ""))
        .withMessage(Auth0ManagementAPI.INVALID_TENANT_NAME);
  }

  @Test
  public void testCreate_validateTenantUrl_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateTenant("tenant1", "", "blah", "http://tenant1.sonatype.app", ""))
        .withMessage(Auth0ManagementAPI.INVALID_BLANK_TENANT_URL);
  }

  @Test
  public void testCreate_validateTenantUrl_InvalidUrl() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateTenant("tenant1", "mytenant.com", "blah",
                "http://tenant1.sonatype.app", ""))
        .withMessage(Auth0ManagementAPI.INVALID_TENANT_URL);
  }

  @Test
  public void testCreate_validateDescription() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrUpdateTenant("tenant1", "http://tenant1.sonatype.app",
            StringUtils.repeat("a", 141),
            "http://tenant1.com/logo.gif", ""))
        .withMessage(Auth0ManagementAPI.INVALID_TENANT_DESCRIPTION);
  }

  @Test
  public void testCreate_validateLogoUrl() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateTenant("tenant1", "http://tenant1.sonatype.app", "blah", "blah", ""))
        .withMessage(Auth0ManagementAPI.INVALID_LOGO_URL);
  }

  @Test
  public void testCreate_createNewClient() throws Exception {
    String name = "tenant1-mtiq";
    String description = "blah";
    String tenantUrl = "http://tenant1.sonatype.app/";
    String logoUrl = "http://tenanat1.com/logo.gif";

    mockAuth0ClientsEntity();
    mockAuth0CreateClientRequest();
    Client mockClient = mockClient(name, description, logoUrl);
    when(createMockRequest.execute()).thenReturn(mockClient);

    Client tenant = auth0ManagementAPI.createOrUpdateTenant(name, tenantUrl, description, logoUrl, null);

    assertThat(tenant).isEqualTo(mockClient);
    verifyCreateRequestWasSent(name, tenantUrl, description, logoUrl);
  }

  @Test
  public void testCreate_updateExistingClient() throws Exception {
    String name = "tenant1";
    String description = "blah";
    String tenantUrl = "http://tenant1.sonatype.app/";
    String logoUrl = "http://tenanat1.com/logo.gif";
    String clientId = "client-id";
    Client mockClient = spyClient(name, description, logoUrl);

    mockAuth0ClientsEntity();
    mockAuth0UpdateClientRequest();
    when(updateMockRequest.execute()).thenReturn(mockClient);

    Client tenant = auth0ManagementAPI.createOrUpdateTenant(name, tenantUrl, description, logoUrl, clientId);

    assertThat(tenant).isEqualTo(mockClient);
    verifyUpdateRequestWasSent(clientId, name, tenantUrl, description, logoUrl);
  }

  @Test
  public void testCreate_Auth0Error() throws Exception {
    String name = "tenant1";
    String description = "blah";
    String tenantUrl = "http://tenant1.sonatype.app";
    String logoUrl = "http://tenanat1.com/logo.gif";

    mockAuth0ClientsEntity();
    mockAuth0CreateClientRequest();
    when(createMockRequest.execute()).thenThrow(new Auth0Exception("remote error"));

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrUpdateTenant(name, tenantUrl, description, logoUrl, null))
        .withMessageContaining("remote error");
  }

  @Test
  public void testGetSamlMetaDataFile() throws Exception {
    doReturn("saml content").when(auth0ManagementAPI)
        .downloadSamlMetadata("https://sonatype.auth0.com/samlp/metadata/abcdefg");
    File samlMetaDataFile = null;
    try {
      samlMetaDataFile = auth0ManagementAPI.getSamlMetaDataFile("abcdefg");
      assertThat(samlMetaDataFile).isNotNull().isFile().hasContent("saml content");
    }
    finally {
      if (samlMetaDataFile != null) {
        Files.delete(samlMetaDataFile.toPath());
      }
    }
  }

  @Test
  public void testGetSamlMetaData() {
    doReturn("saml content").when(auth0ManagementAPI)
        .downloadSamlMetadata("https://sonatype.auth0.com/samlp/metadata/abcdefg");

    String samlMetaDataFile = auth0ManagementAPI.getSamlMetaData("abcdefg");
    assertThat(samlMetaDataFile).isNotNull().isEqualTo("saml content");
  }

  @Test
  public void testGetSamlMetaData_NoAuth0Content() {
    doReturn(null).when(auth0ManagementAPI)
        .downloadSamlMetadata("https://sonatype.auth0.com/samlp/metadata/abcdefg");
    File samlMetaDataFile = auth0ManagementAPI.getSamlMetaDataFile("abcdefg");
    assertThat(samlMetaDataFile).isNull();
  }

  @Test
  public void testCreateConnection_validateName_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrGetConnectionByName("", Arrays.asList("client-id-1", "client-id-2")))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_NAME);
  }

  @Test
  public void testCreateConnection_validateName_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrGetConnectionByName(null, Arrays.asList("client-id-1", "client-id-2")))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_NAME);
  }

  @Test
  public void testCreateConnection_validateClientIds_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrGetConnectionByName("name", Collections.emptyList()))
        .withMessage(Auth0ManagementAPI.INVALID_CLIENT_IDS);
  }

  @Test
  public void testCreateConnection_validateClientIds_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrGetConnectionByName("name", null))
        .withMessage(Auth0ManagementAPI.INVALID_CLIENT_IDS);
  }

  @Test
  public void testCreateConnection_validateClientIds_ListWithEmptyStrings() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrGetConnectionByName("name", Arrays.asList("", "")))
        .withMessage("Client id cannot be be blank");
  }

  @Test
  public void testCreateConnection_createNew() throws Exception {
    String name = "connection";
    List<String> clientIds = Arrays.asList("client-id-1", "client-id-2");
    Connection mockConnection = mockConnection(name, clientIds);

    mockAuth0ConnectionsEntity();
    mockAuth0CreateConnectionRequest();
    mockAuth0ListConnectionsRequest();
    when(createConnectionMockRequest.execute()).thenReturn(mockConnection);

    Connection connection = auth0ManagementAPI.createOrGetConnectionByName(name, clientIds);
    assertThat(connection).isEqualTo(mockConnection);
    verifyCreateConnectionRequestWasSent(name, clientIds);
  }

  @Test
  public void testCreateConnection_connectionExists() throws Exception {
    String name = "connection";
    List<String> clientIds = Arrays.asList("client-id-1", "client-id-2");
    Connection mockConnection = mockConnection(name, clientIds);
    ConnectionsPage mockPage = new ConnectionsPage(Collections.singletonList(mockConnection));

    mockAuth0ConnectionsEntity();
    mockAuth0ListConnectionsRequest();
    when(listConnectionsMockRequest.execute()).thenReturn(mockPage);

    Connection connection = auth0ManagementAPI.createOrGetConnectionByName(name, clientIds);

    assertThat(connection).isEqualTo(mockConnection);
    verifyNewConnectionWasNotCreated();
  }

  @Test
  public void testUpdateConnection_validateId_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.updateAndGetConnectionById("", Arrays.asList("client-id-1", "client-id-2")))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_ID);
  }

  @Test
  public void testUpdateConnection_validateId_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.updateAndGetConnectionById(null, Arrays.asList("client-id-1", "client-id-2")))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_ID);
  }

  @Test
  public void testUpdateConnection_validateClientIds_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.updateAndGetConnectionById("id", Collections.emptyList()))
        .withMessage(Auth0ManagementAPI.INVALID_CLIENT_IDS);
  }

  @Test
  public void testUpdateConnection_validateClientIds_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.updateAndGetConnectionById("id", null))
        .withMessage(Auth0ManagementAPI.INVALID_CLIENT_IDS);
  }

  @Test
  public void testUpdateConnection() throws Exception {
    String id = "connection-id";
    List<String> existingIds = Arrays.asList("exiting-id-1", "exiting-id-2");
    List<String> clientIds = Arrays.asList("client-id-1", "client-id-2");
    Connection mockConnection = mockConnection(id, existingIds);

    mockAuth0ConnectionsEntity();
    mockAuth0GetConnectionRequest();
    mockAuth0UpdateConnectionRequest();
    when(getConnectionMockRequest.execute()).thenReturn(mockConnection);
    when(updateConnectionMockRequest.execute()).thenReturn(mockConnection);

    Connection connection = auth0ManagementAPI.updateAndGetConnectionById(id, clientIds);

    assertThat(connection).isEqualTo(mockConnection);
    List<String> allIds = new ArrayList<>(existingIds);
    allIds.addAll(clientIds);
    verifyUpdateConnectionRequestWasSent(id, allIds);
  }

  @Test
  public void testCreateUser_validateEmail_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrGetUser("", "John", "Smith", "connection-name"))
        .withMessage(Auth0ManagementAPI.INVALID_EMAIL);
  }

  @Test
  public void testCreateUser_validateEmail_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrGetUser(null, "John", "Smith", "connection-name"))
        .withMessage(Auth0ManagementAPI.INVALID_EMAIL);
  }

  @Test
  public void testCreateUser_validateConnectionName_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrGetUser("email", "John", "Smith", ""))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_NAME);
  }

  @Test
  public void testCreateUser_validateConnectionName_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrGetUser("email", "John", "Smith", null))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_NAME);
  }

  @Test
  public void testCreateUser_createNew() throws Exception {
    String email = "user@company.com";
    String connectionName = "connection-id";
    String firstName = "John";
    String lastName = "Smith";

    mockAuth0UsersEntity();
    mockAuth0CreateUserRequest();
    mockAuth0ListUsersRequest();
    User mockUser = mockUser(email, firstName, lastName, connectionName);
    when(createUserMockRequest.execute()).thenReturn(mockUser);

    User user = auth0ManagementAPI.createOrGetUser(email, firstName, lastName, connectionName);
    assertThat(user).isEqualTo(mockUser);
    verifyCreateUserRequestWasSent(email, firstName, lastName);
  }

  @Test
  public void testCreateUser_userExists() throws Exception {
    String email = "user@company.com";
    String connectionName = "connection-id";
    String firstName = "John";
    String lastName = "Smith";
    User mockUser = mockUserWithIdentities(connectionName);
    List<User> mockUsers = Collections.singletonList(mockUser);

    mockAuth0UsersEntity();
    mockAuth0ListUsersRequest();
    when(listUsersMockRequest.execute()).thenReturn(mockUsers);

    User user = auth0ManagementAPI.createOrGetUser(email, firstName, lastName, connectionName);

    verify(user).getIdentities();
    verify(user.getIdentities().get(0)).getConnection();
    verifyNewUserWasNotCreated(email);
  }

  @Test
  public void testCreateUser_Auth0Error() throws Exception {
    String email = "user@company.com";
    String connectionName = "connection-id";
    String firstName = "John";
    String lastName = "Smith";
    mockAuth0UsersEntity();
    mockAuth0CreateUserRequest();
    mockAuth0ListUsersRequest();
    when(createUserMockRequest.execute()).thenThrow(new Auth0Exception("remote error"));

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrGetUser(email, firstName, lastName, connectionName))
        .withMessageContaining("remote error");
  }

  @Test
  public void testDeleteUser_validateEmail_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.deleteUserByEmail("", "connection-name"))
        .withMessage(Auth0ManagementAPI.INVALID_EMAIL);
  }

  @Test
  public void testDeleteUser_validateEmail_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.deleteUserByEmail(null, "connection-name"))
        .withMessage(Auth0ManagementAPI.INVALID_EMAIL);
  }

  @Test
  public void testDeleteUser_validateConnectionName_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.deleteUserByEmail("email", ""))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_NAME);
  }

  @Test
  public void testDeleteUser_validateConnectionName_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.deleteUserByEmail("email", null))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_NAME);
  }

  @Test
  public void testDeleteUser_deleteExistingUser() throws Exception {
    String userId = "user-id";
    String email = "user@company.com";
    String connectionName = "connection-id";
    User mockUser = mockUserWithIdentities(connectionName);
    List<User> mockUsers = Collections.singletonList(mockUser);

    mockAuth0UsersEntity();
    mockAuth0DeleteUserRequest();
    mockAuth0ListUsersRequest();
    when(mockUser.getId()).thenReturn(userId);
    when(listUsersMockRequest.execute()).thenReturn(mockUsers);

    auth0ManagementAPI.deleteUserByEmail(email, connectionName);

    verify(mockUser).getId();
    verifyDeleteUserRequestWasSent(email, userId);
  }

  @Test
  public void testDeleteUser_userDoesntExist() throws Exception {
    String email = "user@company.com";
    String connectionName = "connection-id";

    mockAuth0UsersEntity();
    mockAuth0ListUsersRequest();

    auth0ManagementAPI.deleteUserByEmail(email, connectionName);

    verifyDeleteUserRequestWasNotSent(email);
  }

  @Test
  public void testPasswordChangeTicket_validateEmail_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createPasswordChangeTicket("", "connection-id", "client-id"))
        .withMessage(Auth0ManagementAPI.INVALID_EMAIL);
  }

  @Test
  public void testPasswordChangeTicket_validateEmail_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createPasswordChangeTicket(null, "connection-id", "client-id"))
        .withMessage(Auth0ManagementAPI.INVALID_EMAIL);
  }

  @Test
  public void testPasswordChangeTicket_validateConnectionId_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createPasswordChangeTicket("email", "", "client-id"))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_ID);
  }

  @Test
  public void testPasswordChangeTicket_validateConnectionId_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createPasswordChangeTicket("email", null, "client-id"))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_ID);
  }

  @Test
  public void testPasswordChangeTicket_validateClientId_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createPasswordChangeTicket("email", "connection-id", ""))
        .withMessage(Auth0ManagementAPI.INVALID_CLIENT_ID);
  }

  @Test
  public void testPasswordChangeTicket_validateClientId_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createPasswordChangeTicket("email", "connection-id", null))
        .withMessage(Auth0ManagementAPI.INVALID_CLIENT_ID);
  }

  @Test
  public void testPasswordChangeTicket_createNew() throws Exception {
    String email = "user@company.com";
    String connectionId = "connection-id";
    String clientId = "client-id";
    String ticketUrl = "https://tenant.auth0.com/my-ticket";

    mockAuth0TicketsEntity();
    mockAuth0PasswordChangeTicketRequest();
    PasswordChangeTicket mockPasswordChangeTicket = mockPasswordChangeTicket(ticketUrl);
    when(passwordChangeTicketMockRequest.execute()).thenReturn(mockPasswordChangeTicket);

    PasswordChangeTicket passwordChangeTicket =
        auth0ManagementAPI.createPasswordChangeTicket(email, connectionId, clientId);

    assertThat(passwordChangeTicket.getTicket()).isEqualTo(ticketUrl);
    verify(passwordChangeTicket).getTicket();
    verifyPasswordChangeTicketRequestWasSent();
  }

  @Test
  public void testCreateOrUpdateOrganization_validateName_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateOrganization("", "Display Name",
                "logo", createEnabledConnectionsList()))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_NAME);
  }

  @Test
  public void testCreateOrUpdateOrganization_validateName_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateOrganization(null, "Display Name",
                "logo", createEnabledConnectionsList()))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_NAME);
  }

  @Test
  public void testCreateOrUpdateOrganization_validateDisplayName_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrUpdateOrganization("Name", "",
            "logo", createEnabledConnectionsList()))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_DISPLAY_NAME);
  }

  @Test
  public void testCreateOrUpdateOrganization_validateDisplayName_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateOrganization("Name", null,
                "logo", createEnabledConnectionsList()))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_DISPLAY_NAME);
  }

  @Test
  public void testCreateOrUpdateOrganization_validateConnectionsToEnable_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrUpdateOrganization("Name", "Display Name", "logo", null))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_CONNECTIONS);
  }

  @Test
  public void testCreateOrUpdateOrganization_validateConnectionsToEnable_EmptyList() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateOrganization("Name", "Display Name", "logo",
                Collections.emptyList()))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_CONNECTIONS);
  }

  @Test
  public void testCreateOrUpdateOrganization_createNew() throws Exception {
    String name = "organization";
    String displayName = "My Organization";
    String logoUrl = "https://my.domain.com/logo.jpg";
    List<EnabledConnection> connectionIds = createEnabledConnectionsList();
    Organization mockOrganization = mockOrganization(name, displayName, connectionIds);

    mockAuth0OrganizationsEntity();
    mockGetOrganizationRequestThrowingException();
    mockCreateOrganizationRequest();
    when(createOrganizationMockRequest.execute()).thenReturn(mockOrganization);

    Organization organization =
        auth0ManagementAPI.createOrUpdateOrganization(name, displayName, logoUrl, connectionIds);

    assertThat(organization).isEqualTo(mockOrganization);
    verifyCreateOrganizationRequestWasSent(name, displayName, connectionIds);
  }

  @Test
  public void testCreateOrUpdateOrganization_updateOrganization() throws Exception {
    String id = "orgId";
    String name = "organization";
    String displayName = "My Organization";
    String logoUrl = "https://my.domain.com/logo.jpg";
    List<EnabledConnection> connectionsToEnable = createEnabledConnectionsList();
    Organization mockOrganization = mockOrganization(id);
    EnabledConnectionsPage enabledConnectionsPage = new EnabledConnectionsPage(Collections.emptyList());

    mockAuth0OrganizationsEntity();
    mockGetOrganizationRequest();
    mockAddConnectionToOrganizationRequest();
    mockGetEnabledConnectionsRequest();
    mockUpdateOrganizationRequest();
    when(getOrganizationMockRequest.execute()).thenReturn(mockOrganization);
    when(updateOrganizationMockRequest.execute()).thenReturn(mockOrganization);
    when(getEnabledConnectionMockRequest.execute()).thenReturn(enabledConnectionsPage);

    Organization organization =
        auth0ManagementAPI.createOrUpdateOrganization(name, displayName, logoUrl, connectionsToEnable);

    assertThat(organization).isEqualTo(mockOrganization);
    verifyUpdateOrganizationRequestWasSent(id, name, displayName, connectionsToEnable, Collections.emptyList());
  }

  @Test
  public void testCreateOrUpdateOrganization_updateOrganization_oneConnectionAlreadyEnabled() throws Exception {
    String id = "orgId";
    String name = "organization";
    String displayName = "My Organization";
    String logoUrl = "https://my.domain.com/logo.jpg";
    List<EnabledConnection> allConnectionsToEnable = createEnabledConnectionsList();
    List<EnabledConnection> connectionsToEnable = Collections.singletonList(allConnectionsToEnable.get(1));
    List<EnabledConnection> connectionsToUpdate = Collections.singletonList(allConnectionsToEnable.get(0));
    Organization mockOrganization = mockOrganization(id);
    EnabledConnectionsPage enabledConnectionsPage = new EnabledConnectionsPage(connectionsToUpdate);

    mockAuth0OrganizationsEntity();
    mockGetOrganizationRequest();
    mockAddConnectionToOrganizationRequest();
    mockGetEnabledConnectionsRequest();
    mockUpdateConnectionForOrganizationRequest();
    mockUpdateOrganizationRequest();
    when(getOrganizationMockRequest.execute()).thenReturn(mockOrganization);
    when(updateOrganizationMockRequest.execute()).thenReturn(mockOrganization);
    when(getEnabledConnectionMockRequest.execute()).thenReturn(enabledConnectionsPage);

    Organization organization =
        auth0ManagementAPI.createOrUpdateOrganization(name, displayName, logoUrl, allConnectionsToEnable);

    assertThat(organization).isEqualTo(mockOrganization);
    verifyUpdateOrganizationRequestWasSent(id, name, displayName, connectionsToEnable, connectionsToUpdate);
  }

  @Test
  public void testGetMemberFromOrganization_validateOrganizationId_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getMemberFromOrganization(null, "email"))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_ID);
  }

  @Test
  public void testGetMemberFromOrganization_validateOrganizationId_Blank() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getMemberFromOrganization("", "email"))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_ID);
  }

  @Test
  public void testGetMemberFromOrganization_validateEmail_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getMemberFromOrganization("orgId", null))
        .withMessage(Auth0ManagementAPI.INVALID_EMAIL);
  }

  @Test
  public void testGetMemberFromOrganization_validateEmail_Blank() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getMemberFromOrganization("orgId", ""))
        .withMessage(Auth0ManagementAPI.INVALID_EMAIL);
  }

  @Test
  public void testGetMemberFromOrganization() throws Exception {
    String email = "member@domain.com";
    String orgId = "orgId";
    Member mockMember = mockMember(email);
    MembersPage membersPage = new MembersPage(Collections.singletonList(mockMember));

    mockAuth0OrganizationsEntity();
    mockGetOrganizationMembersRequest();
    when(getOrganizationMembersMockRequest.execute()).thenReturn(membersPage);

    Member member = auth0ManagementAPI.getMemberFromOrganization(orgId, email);

    assertThat(member).isEqualTo(mockMember);
    verifyGetOrganizationMembersRequestWasSent(orgId);
  }

  @Test
  public void testGetMemberFromOrganization_returnNullWhenMemberIsNotPresent() throws Exception {
    String email = "member@domain.com";
    String orgId = "orgId";
    MembersPage membersPage = new MembersPage(Collections.emptyList());

    mockAuth0OrganizationsEntity();
    mockGetOrganizationMembersRequest();
    when(getOrganizationMembersMockRequest.execute()).thenReturn(membersPage);

    Member member = auth0ManagementAPI.getMemberFromOrganization(orgId, email);

    assertThat(member).isNull();
    verifyGetOrganizationMembersRequestWasSent(orgId);
  }

  @Test
  public void testAddMembersToOrganization_validateOrganizationId_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.addMembersToOrganization(null, Collections.singletonList("user-id-1")))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_ID);
  }

  @Test
  public void testAddMembersToOrganization_validateOrganizationId_Blank() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.addMembersToOrganization("", Collections.singletonList("user-id-1")))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_ID);
  }

  @Test
  public void testAddMembersToOrganization_validateUserIdsList_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.addMembersToOrganization("orgId", null))
        .withMessage(Auth0ManagementAPI.INVALID_USER_IDS_LIST);
  }

  @Test
  public void testAddMembersToOrganization_validateUserIdsList_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.addMembersToOrganization("orgId", Collections.emptyList()))
        .withMessage(Auth0ManagementAPI.INVALID_USER_IDS_LIST);
  }

  @Test
  public void testAddMembersToOrganization_validateUserIdsList_BlankUserIds() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.addMembersToOrganization("orgId", Arrays.asList("user-id-1", "")))
        .withMessage(Auth0ManagementAPI.INVALID_USER_ID);
  }

  @Test
  public void testAddMembersToOrganization() throws Exception {
    String orgId = "orgId";
    List<String> userIds = Arrays.asList("user-id-1", "user-id-2");

    mockAuth0OrganizationsEntity();
    mockAddMembersRequest();

    auth0ManagementAPI.addMembersToOrganization(orgId, userIds);

    verify(auth0ManagementAPI, times(1)).organizations();
    verify(mockOrganizationsEntity).addMembers(eq(orgId), membersCaptor.capture());
    verify(addMembersMockRequest).execute();

    Members members = membersCaptor.getValue();
    assertThat(members.getMembers()).containsAll(userIds);
  }

  @Test
  public void testRemoveMembersFromOrganization_validateOrganizationId_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.removeMembersFromOrganization(null, Collections.singletonList("user-id-1")))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_ID);
  }

  @Test
  public void testRemoveMembersFromOrganization_validateOrganizationId_Blank() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.removeMembersFromOrganization("", Collections.singletonList("user-id-1")))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_ID);
  }

  @Test
  public void testRemoveMembersFromOrganization_validateUserIdsList_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.removeMembersFromOrganization("orgId", null))
        .withMessage(Auth0ManagementAPI.INVALID_USER_IDS_LIST);
  }

  @Test
  public void testRemoveMembersFromOrganization_validateUserIdsList_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.removeMembersFromOrganization("orgId", Collections.emptyList()))
        .withMessage(Auth0ManagementAPI.INVALID_USER_IDS_LIST);
  }

  @Test
  public void testRemoveMembersFromOrganization_validateUserIdsList_BlankUserIds() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.removeMembersFromOrganization("orgId", Arrays.asList("user-id-1", "")))
        .withMessage(Auth0ManagementAPI.INVALID_USER_ID);
  }

  @Test
  public void testRemoveMembersFromOrganization() throws Exception {
    String orgId = "orgId";
    List<String> userIds = Arrays.asList("user-id-1", "user-id-2");

    mockAuth0OrganizationsEntity();
    mockDeleteMembersRequest();

    auth0ManagementAPI.removeMembersFromOrganization(orgId, userIds);

    verify(auth0ManagementAPI, times(1)).organizations();
    verify(mockOrganizationsEntity).deleteMembers(eq(orgId), membersCaptor.capture());
    verify(deleteMembersMockRequest).execute();

    Members members = membersCaptor.getValue();
    assertThat(members.getMembers()).containsAll(userIds);
  }

  @Test
  public void testDeleteOrganization_validateOrganizationId_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.deleteOrganization(null))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_ID);
  }

  @Test
  public void testDeleteOrganization_validateOrganizationId_Blank() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.deleteOrganization(""))
        .withMessage(Auth0ManagementAPI.INVALID_ORGANIZATION_ID);
  }

  @Test
  public void testDeleteOrganization() throws Exception {
    String orgId = "orgId";

    mockAuth0OrganizationsEntity();
    mockDeleteOrganizationRequest();

    auth0ManagementAPI.deleteOrganization(orgId);

    verify(auth0ManagementAPI, times(1)).organizations();
    verify(mockOrganizationsEntity).delete(orgId);
    verify(deleteOrganizationMockRequest).execute();
  }

  @Test
  public void testGetUserByEmail_validateEmail_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getUserByEmail(null, "connection"))
        .withMessage(Auth0ManagementAPI.INVALID_EMAIL);
  }

  @Test
  public void testGetUserByEmail_validateEmail_Blank() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getUserByEmail("", "connection"))
        .withMessage(Auth0ManagementAPI.INVALID_EMAIL);
  }

  @Test
  public void testGetUserByEmail_validateConnectionName_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getUserByEmail("email", null))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_NAME);
  }

  @Test
  public void testGetUserByEmail_validateConnectionName_Blank() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getUserByEmail("email", ""))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_NAME);
  }

  @Test
  public void testGetUserByEmail() throws Exception {
    String email = "user@company.com";
    String connectionName = "connection-id";

    mockAuth0UsersEntity();
    mockAuth0ListUsersRequest();
    User mockUser = mockUserWithIdentities(connectionName);
    when(listUsersMockRequest.execute()).thenReturn(Collections.singletonList(mockUser));

    User user = auth0ManagementAPI.getUserByEmail(email, connectionName);

    assertThat(user).isEqualTo(mockUser);
    verify(auth0ManagementAPI, times(1)).users();
    verify(mockUsersEntity).listByEmail(emailCaptor.capture(), fieldsFilterCaptor.capture());
    verify(listUsersMockRequest).execute();
    assertThat(emailCaptor.getValue()).isEqualTo(email);
    assertFilterIsTheExpected(fieldsFilterCaptor.getValue(), "email,user_id,identities,user_metadata");
  }

  @Test
  public void testGetClientById_validateClientId_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getClientById(null))
        .withMessage(Auth0ManagementAPI.INVALID_CLIENT_ID);
  }

  @Test
  public void testGetClientById_validateClientId_Blank() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getClientById(""))
        .withMessage(Auth0ManagementAPI.INVALID_CLIENT_ID);
  }

  @Test
  public void testGetClientById() throws Exception {
    String name = "name";
    String logo = "logo";
    String clientId = "clientId";

    mockAuth0ClientsEntity();
    mockGetClientRequest();

    Client mockClient = mockClient(name, name, logo);
    when(getClientMockRequest.execute()).thenReturn(mockClient);

    Client client = auth0ManagementAPI.getClientById(clientId);

    assertThat(client).isEqualTo(mockClient);
    verify(auth0ManagementAPI, times(1)).clients();
    verify(mockClientsEntity).get(eq(clientId), eq(null));
    verify(getClientMockRequest).execute();
  }

  @Test
  public void testGetConnectionById_validateConnectionId_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getConnectionById(null))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_ID);
  }

  @Test
  public void testGetConnectionById_validateConnectionId_Blank() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.getConnectionById(""))
        .withMessage(Auth0ManagementAPI.INVALID_CONNECTION_ID);
  }

  @Test
  public void testGetConnectionById() throws Exception {
    String connectionId = "connection-id";
    List<String> existingIds = Arrays.asList("exiting-id-1", "exiting-id-2");
    Connection mockConnection = mockConnection(connectionId, existingIds);

    mockAuth0ConnectionsEntity();
    mockAuth0GetConnectionRequest();
    when(getConnectionMockRequest.execute()).thenReturn(mockConnection);

    Connection connection = auth0ManagementAPI.getConnectionById(connectionId);

    assertThat(connection).isEqualTo(mockConnection);
    verify(auth0ManagementAPI, times(1)).connections();
    verify(mockConnectionsEntity).get(eq(connectionId), eq(null));
    verify(getConnectionMockRequest).execute();
  }

  private void verifyCreateRequestWasSent(
      final String name,
      final String tenantUrl,
      final String description,
      final String logoUrl) throws Auth0Exception
  {
    verify(auth0ManagementAPI).clients();
    verify(mockClientsEntity).create(clientCaptor.capture());
    verify(createMockRequest).execute();

    assertCapturedClientIsTheExpected(name, tenantUrl, description, logoUrl);
  }

  private void verifyUpdateRequestWasSent(
      final String clientId,
      final String name,
      final String tenantUrl,
      final String description,
      final String logoUrl) throws Auth0Exception
  {
    verify(auth0ManagementAPI).clients();
    verify(mockClientsEntity).update(clientIdCaptor.capture(), clientCaptor.capture());
    verify(updateMockRequest).execute();

    assertThat(clientIdCaptor.getValue()).isEqualTo(clientId);
    assertCapturedClientIsTheExpected(name, tenantUrl, description, logoUrl);
  }

  private void verifyCreateConnectionRequestWasSent(
      final String name,
      final List<String> clientIds) throws Auth0Exception
  {
    verify(auth0ManagementAPI, times(2)).connections();
    verify(mockConnectionsEntity).listAll(connectionFilterCaptor.capture());
    verify(mockConnectionsEntity).create(connectionCaptor.capture());
    verify(createConnectionMockRequest).execute();
    verify(listConnectionsMockRequest).execute();

    ConnectionFilter filter = connectionFilterCaptor.getValue();
    assertThat(filter.getAsMap()).containsEntry("name", name);
    assertFilterIsTheExpected(filter, "name,id,enabled_clients");
    assertCapturedConnectionIsTheExpected(name, clientIds);
  }

  private void verifyUpdateConnectionRequestWasSent(
      final String id,
      final List<String> clientIds) throws Auth0Exception
  {
    verify(auth0ManagementAPI, times(2)).connections();
    verify(mockConnectionsEntity).get(id, null);
    verify(mockConnectionsEntity).update(eq(id), connectionCaptor.capture());
    verify(getConnectionMockRequest).execute();
    verify(updateConnectionMockRequest).execute();

    assertCapturedConnectionToUpdateIsTheExpected(clientIds);
  }

  private void verifyNewConnectionWasNotCreated() throws Auth0Exception {
    verify(auth0ManagementAPI, times(1)).connections();
    verify(mockConnectionsEntity).listAll(connectionFilterCaptor.capture());
    verify(mockConnectionsEntity, never()).create(any(Connection.class));
    verify(listConnectionsMockRequest).execute();

    assertFilterIsTheExpected(connectionFilterCaptor.getValue(), "name,id,enabled_clients");
  }

  private void verifyCreateUserRequestWasSent(
      final String email,
      final String firstName,
      final String lastName) throws Exception
  {
    verify(auth0ManagementAPI, times(2)).users();
    verify(mockUsersEntity).listByEmail(emailCaptor.capture(), fieldsFilterCaptor.capture());
    verify(mockUsersEntity).create(userCaptor.capture());
    verify(createUserMockRequest).execute();
    verify(listUsersMockRequest).execute();

    assertCapturedUserIsTheExpected(email, firstName, lastName);
    assertThat(emailCaptor.getValue()).isEqualTo(email);
    assertFilterIsTheExpected(fieldsFilterCaptor.getValue(), "email,user_id,identities,user_metadata");
  }

  private void verifyNewUserWasNotCreated(final String email) throws Auth0Exception {
    verify(auth0ManagementAPI, times(1)).users();
    verify(mockUsersEntity).listByEmail(emailCaptor.capture(), fieldsFilterCaptor.capture());
    verify(mockUsersEntity, never()).create(any(User.class));
    verify(listUsersMockRequest).execute();

    assertThat(emailCaptor.getValue()).isEqualTo(email);
    assertFilterIsTheExpected(fieldsFilterCaptor.getValue(), "email,user_id,identities,user_metadata");
  }

  private void verifyDeleteUserRequestWasSent(final String email, final String userId) throws Exception {
    verify(auth0ManagementAPI, times(2)).users();
    verify(mockUsersEntity).listByEmail(emailCaptor.capture(), fieldsFilterCaptor.capture());
    verify(mockUsersEntity).delete(userIdCaptor.capture());
    verify(deleteUserMockRequest).execute();
    verify(listUsersMockRequest).execute();

    assertThat(userIdCaptor.getValue()).isEqualTo(userId);
    assertThat(emailCaptor.getValue()).isEqualTo(email);
    assertFilterIsTheExpected(fieldsFilterCaptor.getValue(), "email,user_id,identities,user_metadata");
  }

  private void verifyDeleteUserRequestWasNotSent(final String email) throws Auth0Exception {
    verify(auth0ManagementAPI, times(1)).users();
    verify(mockUsersEntity).listByEmail(emailCaptor.capture(), fieldsFilterCaptor.capture());
    verify(mockUsersEntity, never()).delete(any(String.class));
    verify(listUsersMockRequest).execute();

    assertThat(emailCaptor.getValue()).isEqualTo(email);
    assertFilterIsTheExpected(fieldsFilterCaptor.getValue(), "email,user_id,identities,user_metadata");
  }

  private void verifyPasswordChangeTicketRequestWasSent() throws Exception {
    verify(auth0ManagementAPI, times(1)).tickets();
    verify(mockTicketsEntity).requestPasswordChange(passwordChangeTicketCaptor.capture());
    verify(passwordChangeTicketMockRequest).execute();
  }

  private void verifyCreateOrganizationRequestWasSent(
      final String name,
      final String displayName,
      final List<EnabledConnection> connectionsToEnable) throws Auth0Exception
  {
    verify(auth0ManagementAPI, times(2)).organizations();
    verify(mockOrganizationsEntity).getByName(name);
    verify(mockOrganizationsEntity).create(organizationCaptor.capture());
    verify(getOrganizationMockRequest).execute();
    verify(createOrganizationMockRequest).execute();

    Organization organization = organizationCaptor.getValue();
    assertThat(organization.getName()).isEqualTo(name);
    assertThat(organization.getDisplayName()).isEqualTo(displayName);
    assertThat(organization.getEnabledConnections()).containsAll(connectionsToEnable);
  }

  private void verifyUpdateOrganizationRequestWasSent(
      final String orgId,
      final String name,
      final String displayName,
      final List<EnabledConnection> connectionsToEnable,
      final List<EnabledConnection> connectionsToUpdate) throws Auth0Exception
  {
    verify(auth0ManagementAPI, times(5)).organizations();
    verify(mockOrganizationsEntity).getByName(name);
    verify(mockOrganizationsEntity).update(eq(orgId), organizationCaptor.capture());

    connectionsToEnable.forEach(connection -> {
      verify(mockOrganizationsEntity).addConnection(orgId, connection);
      verify(mockOrganizationsEntity, never()).updateConnection(eq(orgId), eq(connection.getConnectionId()),
          any(EnabledConnection.class));
    });

    connectionsToUpdate.forEach(connection -> {
      verify(mockOrganizationsEntity).updateConnection(eq(orgId), eq(connection.getConnectionId()),
          any(EnabledConnection.class));
      verify(mockOrganizationsEntity, never()).addConnection(orgId, connection);
    });

    verify(getOrganizationMockRequest).execute();
    verify(updateOrganizationMockRequest).execute();
    verify(getEnabledConnectionMockRequest).execute();
    verify(addConnectionToOrganizationMockRequest, times(connectionsToEnable.size())).execute();

    Organization organization = organizationCaptor.getValue();
    assertThat(organization.getName()).isEqualTo(name);
    assertThat(organization.getDisplayName()).isEqualTo(displayName);
  }

  private void verifyGetOrganizationMembersRequestWasSent(final String orgId) throws Auth0Exception {
    verify(auth0ManagementAPI, times(1)).organizations();
    verify(mockOrganizationsEntity).getMembers(eq(orgId), pageFilerCaptor.capture());
    verify(getOrganizationMembersMockRequest).execute();

    PageFilter pageFilter = pageFilerCaptor.getValue();
    assertThat(pageFilter.getAsMap().get("page")).isEqualTo(0);
    assertThat(pageFilter.getAsMap().get("per_page")).isEqualTo(50);
  }

  private void assertFilterIsTheExpected(FieldsFilter filter, String fields) {
    assertThat(filter.getAsMap()).containsEntry("include_fields", true);
    assertThat(filter.getAsMap()).containsEntry("fields", fields);
  }

  private void assertCapturedClientIsTheExpected(
      final String name,
      final String tenantUrl,
      final String description,
      final String logoUrl)
  {
    Client clientParameter = clientCaptor.getValue();
    assertThat(clientParameter.getName()).isEqualTo(name);
    assertThat(clientParameter.getDescription()).isEqualTo(description);
    assertThat(clientParameter.getLogoUri()).isEqualTo(logoUrl);
    assertThat(clientParameter.getAllowedLogoutUrls()).contains(tenantUrl);
    assertThat(clientParameter.getCallbacks()).contains(tenantUrl + "saml");
    assertThat(clientParameter.getGrantTypes()).containsExactly("authorization_code", "implicit");
  }

  private void assertCapturedConnectionIsTheExpected(final String name, final List<String> clientIds) {
    Connection connection = connectionCaptor.getValue();
    assertThat(connection.getName()).isEqualTo(name);
    assertThat(connection.getStrategy()).isEqualTo(Auth0ManagementAPI.AUTH0_CONNECTION_STRATEGY);
    assertThat(connection.getEnabledClients()).isEqualTo(clientIds);
    assertThat(connection.getOptions()).containsEntry(Auth0ManagementAPI.DISABLE_SIGNUP_OPTION, true);
    assertThat(connection.getOptions()).containsEntry(Auth0ManagementAPI.PASSWORD_POLICY_OPTION,
        Auth0ManagementAPI.PASSWORD_POLICY);

    Map<String, Object> passwordHistory = new HashMap<>();
    passwordHistory.put("enable", true);
    passwordHistory.put("size", Auth0ManagementAPI.PASSWORD_HISTORY_SIZE);

    Map<String, Object> passwordPersonalInfo = new HashMap<>();
    passwordPersonalInfo.put("enable", true);

    Map<String, Object> passwordDictionary = new HashMap<>();
    passwordDictionary.put("enable", true);

    assertThat(connection.getOptions()).containsEntry(Auth0ManagementAPI.PASSWORD_HISTORY_OPTION, passwordHistory);
    assertThat(connection.getOptions()).containsEntry(Auth0ManagementAPI.PASSWORD_NO_PERSONAL_INFO_OPTION,
        passwordPersonalInfo);
    assertThat(connection.getOptions()).containsEntry(Auth0ManagementAPI.PASSWORD_DICTIONARY_OPTION,
        passwordDictionary);
  }

  private void assertCapturedConnectionToUpdateIsTheExpected(final List<String> clientIds) {
    Connection connection = connectionCaptor.getValue();
    assertThat(connection.getEnabledClients()).isEqualTo(clientIds);
  }

  private void assertCapturedUserIsTheExpected(final String email, final String firstName, final String lastName) {
    User user = userCaptor.getValue();
    assertThat(user.getEmail()).isEqualTo(email);
    assertThat(user.getGivenName()).isEqualTo(firstName);
    assertThat(user.getFamilyName()).isEqualTo(lastName);
    assertThat(user.isEmailVerified()).isTrue();
    assertThat(user.getUserMetadata()).containsEntry(Auth0ManagementAPI.IS_INVITED_FLAG, true);
  }

  private void mockAuth0ClientsEntity() {
    mockClientsEntity = mock(ClientsEntity.class);
    when(auth0ManagementAPI.clients()).thenReturn(mockClientsEntity);
  }

  private void mockAuth0ConnectionsEntity() {
    mockConnectionsEntity = mock(ConnectionsEntity.class);
    when(auth0ManagementAPI.connections()).thenReturn(mockConnectionsEntity);
  }

  private void mockAuth0UsersEntity() {
    mockUsersEntity = mock(UsersEntity.class);
    when(auth0ManagementAPI.users()).thenReturn(mockUsersEntity);
  }

  private void mockAuth0TicketsEntity() {
    mockTicketsEntity = mock(TicketsEntity.class);
    when(auth0ManagementAPI.tickets()).thenReturn(mockTicketsEntity);
  }

  private void mockAuth0OrganizationsEntity() {
    mockOrganizationsEntity = mock(OrganizationsEntity.class);
    when(auth0ManagementAPI.organizations()).thenReturn(mockOrganizationsEntity);
  }

  private void mockAuth0CreateClientRequest() {
    createMockRequest = mock(Request.class);
    when(mockClientsEntity.create(clientCaptor.capture())).thenReturn(createMockRequest);
  }

  private void mockAuth0UpdateClientRequest() {
    updateMockRequest = mock(Request.class);
    when(mockClientsEntity.update(any(String.class), any(Client.class))).thenReturn(updateMockRequest);
  }

  private void mockGetClientRequest() {
    getClientMockRequest = mock(Request.class);
    when(mockClientsEntity.get(any(String.class), eq(null))).thenReturn(getClientMockRequest);
  }

  private void mockAuth0CreateConnectionRequest() {
    createConnectionMockRequest = mock(Request.class);
    when(mockConnectionsEntity.create(any(Connection.class))).thenReturn(createConnectionMockRequest);
  }

  private void mockAuth0ListConnectionsRequest() throws Exception {
    listConnectionsMockRequest = mock(Request.class);
    when(mockConnectionsEntity.listAll(any(ConnectionFilter.class))).thenReturn(listConnectionsMockRequest);
    when(listConnectionsMockRequest.execute()).thenReturn(new ConnectionsPage(Collections.emptyList()));
  }

  private void mockAuth0GetConnectionRequest() {
    getConnectionMockRequest = mock(Request.class);
    when(mockConnectionsEntity.get(any(String.class), eq(null))).thenReturn(
        getConnectionMockRequest);
  }

  private void mockAuth0UpdateConnectionRequest() {
    updateConnectionMockRequest = mock(Request.class);
    when(mockConnectionsEntity.update(any(String.class), any(Connection.class))).thenReturn(
        updateConnectionMockRequest);
  }

  private void mockAuth0CreateUserRequest() {
    createUserMockRequest = mock(Request.class);
    when(mockUsersEntity.create(any(User.class))).thenReturn(createUserMockRequest);
  }

  private void mockAuth0ListUsersRequest() throws Exception {
    listUsersMockRequest = mock(Request.class);
    when(mockUsersEntity.listByEmail(any(String.class), any(FieldsFilter.class))).thenReturn(listUsersMockRequest);
    when(listUsersMockRequest.execute()).thenReturn(Collections.emptyList());
  }

  private void mockAuth0DeleteUserRequest() {
    deleteUserMockRequest = mock(Request.class);
    when(mockUsersEntity.delete(any(String.class))).thenReturn(deleteUserMockRequest);
  }

  private void mockAuth0PasswordChangeTicketRequest() {
    passwordChangeTicketMockRequest = mock(Request.class);
    when(mockTicketsEntity.requestPasswordChange(any(PasswordChangeTicket.class))).thenReturn(
        passwordChangeTicketMockRequest);
  }

  private void mockCreateOrganizationRequest() {
    createOrganizationMockRequest = mock(Request.class);
    when(mockOrganizationsEntity.create(any(Organization.class))).thenReturn(createOrganizationMockRequest);
  }

  private void mockUpdateOrganizationRequest() {
    updateOrganizationMockRequest = mock(Request.class);
    when(mockOrganizationsEntity.update(any(String.class), any(Organization.class))).thenReturn(
        updateOrganizationMockRequest);
  }

  private void mockGetOrganizationRequestThrowingException() throws Auth0Exception {
    getOrganizationMockRequest = mock(Request.class);
    when(mockOrganizationsEntity.getByName(any(String.class))).thenReturn(getOrganizationMockRequest);
    when(getOrganizationMockRequest.execute()).thenThrow(new Auth0Exception("Organization doesn't exists"));
  }

  private void mockGetOrganizationRequest() throws Auth0Exception {
    getOrganizationMockRequest = mock(Request.class);
    when(mockOrganizationsEntity.getByName(any(String.class))).thenReturn(getOrganizationMockRequest);
    when(getOrganizationMockRequest.execute()).thenReturn(null);
  }

  private void mockGetEnabledConnectionsRequest() {
    getEnabledConnectionMockRequest = mock(Request.class);
    when(mockOrganizationsEntity.getConnections(any(String.class), eq(null))).thenReturn(
        getEnabledConnectionMockRequest);
  }

  private void mockAddConnectionToOrganizationRequest() {
    addConnectionToOrganizationMockRequest = mock(Request.class);
    when(mockOrganizationsEntity.addConnection(any(String.class), any(EnabledConnection.class))).thenReturn(
        addConnectionToOrganizationMockRequest);
  }

  private void mockUpdateConnectionForOrganizationRequest() {
    updateConnectionForOrganizationMockRequest = mock(Request.class);
    when(mockOrganizationsEntity.updateConnection(any(String.class), any(String.class),
        any(EnabledConnection.class))).thenReturn(
            updateConnectionForOrganizationMockRequest);
  }

  private void mockGetOrganizationMembersRequest() {
    getOrganizationMembersMockRequest = mock(Request.class);
    when(mockOrganizationsEntity.getMembers(any(String.class), any(PageFilter.class))).thenReturn(
        getOrganizationMembersMockRequest);
  }

  private void mockAddMembersRequest() {
    addMembersMockRequest = mock(Request.class);
    when(mockOrganizationsEntity.addMembers(any(String.class), any(Members.class))).thenReturn(
        addMembersMockRequest);
  }

  private void mockDeleteMembersRequest() {
    deleteMembersMockRequest = mock(Request.class);
    when(mockOrganizationsEntity.deleteMembers(any(String.class), any(Members.class))).thenReturn(
        deleteMembersMockRequest);
  }

  private void mockDeleteOrganizationRequest() {
    deleteOrganizationMockRequest = mock(Request.class);
    when(mockOrganizationsEntity.delete(any(String.class))).thenReturn(
        deleteOrganizationMockRequest);
  }

  private Client mockClient(String name, String description, String logoUrl) {
    Client client = new Client(name);
    client.setDescription(description);
    client.setLogoUri(logoUrl);
    return client;
  }

  private Client spyClient(String name, String description, String logoUrl) {
    Client client = spy(new Client(name));
    client.setDescription(description);
    client.setLogoUri(logoUrl);
    return client;
  }

  private Connection mockConnection(final String name, final List<String> clientIds) {
    Connection connection = new Connection(name, Auth0ManagementAPI.AUTH0_CONNECTION_STRATEGY);
    connection.setEnabledClients(clientIds);
    return connection;
  }

  private User mockUser(
      final String email,
      final String firstName,
      final String lastName,
      final String connectionName)
  {
    User user = new User(connectionName);
    user.setEmail(email);
    user.setGivenName(firstName);
    user.setFamilyName(lastName);
    user.setPassword(UUID.randomUUID().toString().toCharArray());
    user.setEmailVerified(false);
    return user;
  }

  private User mockUserWithIdentities(final String connectionName) {
    Identity identity = mock(Identity.class);
    when(identity.getConnection()).thenReturn(connectionName);

    User user = mock(User.class);
    when(user.getIdentities()).thenReturn(Collections.singletonList(identity));
    return user;
  }

  private PasswordChangeTicket mockPasswordChangeTicket(final String ticketUrl) {
    PasswordChangeTicket passwordChangeTicket = mock(PasswordChangeTicket.class);
    when(passwordChangeTicket.getTicket()).thenReturn(ticketUrl);
    return passwordChangeTicket;
  }

  private Organization mockOrganization(
      final String name,
      final String displayName,
      final List<EnabledConnection> enabledConnections)
  {
    Organization organization = new Organization(name);
    organization.setDisplayName(displayName);
    organization.setEnabledConnections(enabledConnections);
    return organization;
  }

  private Organization mockOrganization(final String orgId) {
    Organization organization = mock(Organization.class);

    when(organization.getId()).thenReturn(orgId);

    return organization;
  }

  private Member mockMember(final String email) {
    Member member = new Member();
    member.setEmail(email);
    return member;
  }

  private static List<EnabledConnection> createEnabledConnectionsList() {
    List<String> connections = Arrays.asList("connection-1", "connection-2");
    return connections.stream().map(connection -> {
      EnabledConnection enabledConnection = new EnabledConnection(connection);
      enabledConnection.setAssignMembershipOnLogin(true);
      return enabledConnection;
    }).collect(Collectors.toList());
  }
}
