/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.auth0.client.mgmt;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.common.test.SlowTest;

import com.auth0.client.mgmt.filter.ConnectionFilter;
import com.auth0.client.mgmt.filter.FieldsFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.Connection;
import com.auth0.json.mgmt.ConnectionsPage;
import com.auth0.json.mgmt.client.Client;
import com.auth0.json.mgmt.tickets.PasswordChangeTicket;
import com.auth0.json.mgmt.users.Identity;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.Request;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Category(SlowTest.class)
public class Auth0ManagementAPITest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

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

  public Auth0ManagementAPI auth0ManagementAPI;

  private ClientsEntity mockClientsEntity;

  private ConnectionsEntity mockConnectionsEntity;

  private UsersEntity mockUsersEntity;

  private TicketsEntity mockTicketsEntity;

  private Request<Client> createMockRequest;

  private Request<Client> updateMockRequest;

  private Request<Connection> createConnectionMockRequest;

  private Request<ConnectionsPage> listConnectionsMockRequest;

  private Request<User> createUserMockRequest;

  private Request<Void> deleteUserMockRequest;

  private Request<List<User>> listUsersMockRequest;

  private Request<PasswordChangeTicket> passwordChangeTicketMockRequest;

  @Before
  public void before() {
    auth0ManagementAPI =
        spy(new Auth0ManagementAPI("https://sonatype.auth0.com", "abcdefg"));
  }

  @Test
  public void testCreate_validateTenantName_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrUpdateTenant("", "http://tenant1.sonatype.app", "blah",
            "http://tenant1.com/logo.gif", ""))
        .withMessage("Tenant name cannot be blank or invalid characters <,>");
  }

  @Test
  public void testCreate_validateTenantSubdomain_InvalidCharacters() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrUpdateTenant("<sub-domain>", "http://tenant1.sonatype.app", "blah",
            "http://tenant1.com/logo.gif", ""))
        .withMessage("Tenant name cannot be blank or invalid characters <,>");
  }

  @Test
  public void testCreate_validateTenantUrl_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateTenant("tenant1", "", "blah", "http://tenant1.sonatype.app", ""))
        .withMessage("Tenant URL cannot be blank");
  }

  @Test
  public void testCreate_validateTenantUrl_InvalidUrl() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateTenant("tenant1", "mytenant.com", "blah",
                "http://tenant1.sonatype.app", ""))
        .withMessage("Tenant URL must be a valid URL");
  }

  @Test
  public void testCreate_validateDescription() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrUpdateTenant("tenant1", "http://tenant1.sonatype.app",
            StringUtils.repeat("a", 141),
            "http://tenant1.com/logo.gif", ""))
        .withMessage("Tenant description should be less that 140 characters");
  }

  @Test
  public void testCreate_validateLogoUrl() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateTenant("tenant1", "http://tenant1.sonatype.app", "blah", "blah", ""))
        .withMessage("Tenant logo URL must be a valid URL");
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
    Client mockClient = mockClient(name, description, logoUrl);

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
        .isThrownBy(() -> auth0ManagementAPI.createOrUpdateConnection("", Arrays.asList("client-id-1", "client-id-2")))
        .withMessage("Connection name cannot be blank");
  }

  @Test
  public void testCreateConnection_validateName_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateConnection(null, Arrays.asList("client-id-1", "client-id-2")))
        .withMessage("Connection name cannot be blank");
  }

  @Test
  public void testCreateConnection_validateClientIds_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrUpdateConnection("name", Collections.emptyList()))
        .withMessage("Client ids cannot be empty or null");
  }

  @Test
  public void testCreateConnection_validateClientIds_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateConnection("name", null))
        .withMessage("Client ids cannot be empty or null");
  }

  @Test
  public void testCreateConnection_validateClientIds_ListWithEmptyStrings() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrUpdateConnection("name", Arrays.asList("", "")))
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

    Connection connection = auth0ManagementAPI.createOrUpdateConnection(name, clientIds);
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

    Connection connection = auth0ManagementAPI.createOrUpdateConnection(name, clientIds);

    assertThat(connection).isEqualTo(mockConnection);
    verifyNewConnectionWasNotCreated();
  }

  @Test
  public void testCreateUser_validateEmail_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrGetUser("", "John", "Smith", "connection-name"))
        .withMessage("Email cannot be blank");
  }

  @Test
  public void testCreateUser_validateEmail_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrGetUser(null, "John", "Smith", "connection-name"))
        .withMessage("Email cannot be blank");
  }

  @Test
  public void testCreateUser_validateConnectionName_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createOrGetUser("email", "John", "Smith", ""))
        .withMessage("Connection name cannot be blank");
  }

  @Test
  public void testCreateUser_validateConnectionName_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createOrGetUser("email", "John", "Smith", null))
        .withMessage("Connection name cannot be blank");
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
        .withMessage("Email cannot be blank");
  }

  @Test
  public void testDeleteUser_validateEmail_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.deleteUserByEmail(null, "connection-name"))
        .withMessage("Email cannot be blank");
  }

  @Test
  public void testDeleteUser_validateConnectionName_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.deleteUserByEmail("email", ""))
        .withMessage("Connection name cannot be blank");
  }

  @Test
  public void testDeleteUser_validateConnectionName_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.deleteUserByEmail("email", null))
        .withMessage("Connection name cannot be blank");
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
        .withMessage("Email cannot be blank");
  }

  @Test
  public void testPasswordChangeTicket_validateEmail_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createPasswordChangeTicket(null, "connection-id", "client-id"))
        .withMessage("Email cannot be blank");
  }

  @Test
  public void testPasswordChangeTicket_validateConnectionId_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createPasswordChangeTicket("email", "", "client-id"))
        .withMessage("Connection id cannot be blank");
  }

  @Test
  public void testPasswordChangeTicket_validateConnectionId_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createPasswordChangeTicket("email", null, "client-id"))
        .withMessage("Connection id cannot be blank");
  }

  @Test
  public void testPasswordChangeTicket_validateClientId_Empty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> auth0ManagementAPI.createPasswordChangeTicket("email", "connection-id", ""))
        .withMessage("Client id cannot be blank");
  }

  @Test
  public void testPasswordChangeTicket_validateClientId_Null() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> auth0ManagementAPI.createPasswordChangeTicket("email", "connection-id", null))
        .withMessage("Client id cannot be blank");
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

  private void verifyCreateRequestWasSent(
      final String name,
      final String tenantUrl,
      final String description,
      final String logoUrl)
      throws Auth0Exception
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
      final String logoUrl
  ) throws Auth0Exception
  {
    verify(auth0ManagementAPI).clients();
    verify(mockClientsEntity).update(clientIdCaptor.capture(), clientCaptor.capture());
    verify(updateMockRequest).execute();

    assertThat(clientIdCaptor.getValue()).isEqualTo(clientId);
    assertCapturedClientIsTheExpected(name, tenantUrl, description, logoUrl);
  }

  private void verifyCreateConnectionRequestWasSent(
      final String name,
      final List<String> clientIds)
      throws Auth0Exception
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

  private void verifyNewConnectionWasNotCreated()
      throws Auth0Exception
  {
    verify(auth0ManagementAPI, times(1)).connections();
    verify(mockConnectionsEntity).listAll(connectionFilterCaptor.capture());
    verify(mockConnectionsEntity, never()).create(any(Connection.class));
    verify(listConnectionsMockRequest).execute();

    assertFilterIsTheExpected(connectionFilterCaptor.getValue(), "name,id,enabled_clients");
  }

  private void verifyCreateUserRequestWasSent(final String email, final String firstName, final String lastName)
      throws Exception
  {
    verify(auth0ManagementAPI, times(2)).users();
    verify(mockUsersEntity).listByEmail(emailCaptor.capture(), fieldsFilterCaptor.capture());
    verify(mockUsersEntity).create(userCaptor.capture());
    verify(createUserMockRequest).execute();
    verify(listUsersMockRequest).execute();

    assertCapturedUserIsTheExpected(email, firstName, lastName);
    assertThat(emailCaptor.getValue()).isEqualTo(email);
    assertFilterIsTheExpected(fieldsFilterCaptor.getValue(), "email,user_id,identities");
  }

  private void verifyNewUserWasNotCreated(final String email)
      throws Auth0Exception
  {
    verify(auth0ManagementAPI, times(1)).users();
    verify(mockUsersEntity).listByEmail(emailCaptor.capture(), fieldsFilterCaptor.capture());
    verify(mockUsersEntity, never()).create(any(User.class));
    verify(listUsersMockRequest).execute();

    assertThat(emailCaptor.getValue()).isEqualTo(email);
    assertFilterIsTheExpected(fieldsFilterCaptor.getValue(), "email,user_id,identities");
  }

  private void verifyDeleteUserRequestWasSent(final String email, final String userId) throws Exception {
    verify(auth0ManagementAPI, times(2)).users();
    verify(mockUsersEntity).listByEmail(emailCaptor.capture(), fieldsFilterCaptor.capture());
    verify(mockUsersEntity).delete(userIdCaptor.capture());
    verify(deleteUserMockRequest).execute();
    verify(listUsersMockRequest).execute();

    assertThat(userIdCaptor.getValue()).isEqualTo(userId);
    assertThat(emailCaptor.getValue()).isEqualTo(email);
    assertFilterIsTheExpected(fieldsFilterCaptor.getValue(), "email,user_id,identities");
  }

  private void verifyDeleteUserRequestWasNotSent(final String email)
      throws Auth0Exception
  {
    verify(auth0ManagementAPI, times(1)).users();
    verify(mockUsersEntity).listByEmail(emailCaptor.capture(), fieldsFilterCaptor.capture());
    verify(mockUsersEntity, never()).delete(any(String.class));
    verify(listUsersMockRequest).execute();

    assertThat(emailCaptor.getValue()).isEqualTo(email);
    assertFilterIsTheExpected(fieldsFilterCaptor.getValue(), "email,user_id,identities");
  }

  private void verifyPasswordChangeTicketRequestWasSent() throws Exception {
    verify(auth0ManagementAPI, times(1)).tickets();
    verify(mockTicketsEntity).requestPasswordChange(passwordChangeTicketCaptor.capture());
    verify(passwordChangeTicketMockRequest).execute();
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

  private void mockAuth0CreateClientRequest() {
    createMockRequest = mock(Request.class);
    when(mockClientsEntity.create(clientCaptor.capture())).thenReturn(createMockRequest);
  }

  private void mockAuth0UpdateClientRequest() {
    updateMockRequest = mock(Request.class);
    when(mockClientsEntity.update(any(String.class), any(Client.class))).thenReturn(updateMockRequest);
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

  private Client mockClient(String name, String description, String logoUrl) {
    Client client = new Client(name);
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
}
