/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.auth;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.service.Auth0Config;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import com.auth0.client.auth.Auth0AuthAPI;
import com.auth0.client.mgmt.Auth0ManagementAPI;
import com.auth0.client.mgmt.ClientsEntity;
import com.auth0.client.mgmt.ConnectionsEntity;
import com.auth0.client.mgmt.filter.ConnectionFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.json.mgmt.Connection;
import com.auth0.json.mgmt.organizations.Member;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.Request;
import com.auth0.net.TokenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService.CONNECTION_CREATION_SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MultiTenantAuth0ManagementServiceTest
{
  private static final String EMAIL = "email";

  private static final String FIRST_NAME = "first";

  private static final String LAST_NAME = "last";

  private static final String CONNECTION_NAME = "connection";

  private static final String CLIENT_ID = "clientId";

  public static final String APPLICATION_ID = "applicationId";

  private static final String CONNECTION_ID = "connectionId";

  public static final String ORGANIZATION_ID = "organizationId";

  @Mock
  private MultiTenantAuth0ApiSupplier auth0ApiSupplier;

  @Mock
  private MultiTenantInsightConfig config;

  @Mock
  private Auth0ManagementAPI managementApi;

  @Mock
  private Auth0AuthAPI authApi;

  @Mock
  private TokenRequest tokenRequest;

  @Mock
  private TokenHolder tokenHolder;

  private MultiTenantAuth0ManagementService underTest;

  @BeforeEach
  public void setUp() throws Exception {
    Auth0Config auth0Config = new Auth0Config();
    auth0Config.setDomain("domain");
    auth0Config.setCustomDomain("customDomain");
    auth0Config.setClientId(CLIENT_ID);
    auth0Config.setClientSecret("clientSecret");
    lenient().when(config.getAuth0Config()).thenReturn(auth0Config);
    underTest = new MultiTenantAuth0ManagementService(config, auth0ApiSupplier);

    lenient().when(auth0ApiSupplier.getManagementApi(any(), any())).thenReturn(managementApi);
    lenient().when(auth0ApiSupplier.getAuthApi(any(), any(), any())).thenReturn(authApi);
  }

  @Test
  public void test_canCreateUser() throws Exception {
    when(tokenHolder.getExpiresAt()).thenReturn(new Date(System.currentTimeMillis() + 1000));
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    User user = mockUser("userId", true);

    when(managementApi.createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME))
        .thenReturn(user);

    // Set to upper case to verify email normalization
    underTest.createOrUpdateUser(EMAIL.toUpperCase(), FIRST_NAME, LAST_NAME, CONNECTION_NAME, APPLICATION_ID,
        CONNECTION_ID,
        ORGANIZATION_ID);

    verify(authApi).requestToken(any());
    verify(managementApi).createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME);
    verify(managementApi).addMembersToOrganization(eq(ORGANIZATION_ID), anyList());
    verify(authApi).resetPassword(EMAIL, CONNECTION_NAME, APPLICATION_ID, ORGANIZATION_ID);
  }

  @Test
  public void test_canCreateUserAndNotAddItToAnOrganization() throws Exception {
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    User user = mockUser("userId", true);
    when(managementApi.createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME))
        .thenReturn(user);

    underTest.createOrUpdateUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME, APPLICATION_ID, CONNECTION_ID, "");

    verify(authApi).requestToken(any());
    verify(managementApi).createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME);
    verify(managementApi, never()).addMembersToOrganization(eq(ORGANIZATION_ID), anyList());
    verify(authApi).resetPassword(EMAIL, CONNECTION_NAME, APPLICATION_ID, "");
  }

  @Test
  public void test_createUserOnAuth0FailsThatNoPasswordResetIsSent() throws Exception {
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    when(managementApi.createOrGetUser(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("user creation failed"));

    assertThatThrownBy(() -> {
      underTest.createOrUpdateUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME, APPLICATION_ID, CONNECTION_ID,
          ORGANIZATION_ID);
    }).isInstanceOf(RuntimeException.class).hasMessageContaining("user creation failed");

    verify(authApi).requestToken(any());
    verify(managementApi).createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME);
    verify(authApi, never()).resetPassword(EMAIL, CONNECTION_NAME, APPLICATION_ID, ORGANIZATION_ID);
  }

  @Test
  public void test_restPasswordFailWillDeleteAuth0User() throws Exception {
    when(tokenHolder.getExpiresAt()).thenReturn(new Date(System.currentTimeMillis() + 1000));
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    when(authApi.resetPassword(any(), any(), any(), any())).thenThrow(new RuntimeException("Password reset failed"));
    User user = mockUser("userId", true);
    when(managementApi.createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME))
        .thenReturn(user);

    assertThatThrownBy(
        () -> underTest.createOrUpdateUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME,
            APPLICATION_ID, CONNECTION_ID, ORGANIZATION_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password reset failed");

    verify(authApi).requestToken(any());
    verify(managementApi).createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME);
    verify(authApi).resetPassword(EMAIL, CONNECTION_NAME, APPLICATION_ID, ORGANIZATION_ID);
    verify(managementApi).deleteUserByEmailFromConnection(EMAIL, CONNECTION_ID);
  }

  @Test
  public void test_restPasswordNotSentIfUserHasAcceptedInvitation() throws Exception {
    when(tokenHolder.getExpiresAt()).thenReturn(new Date(System.currentTimeMillis() + 1000));
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    User user = mockUser("userId", false);
    when(managementApi.getUserByEmail(any(), any())).thenReturn(user);

    underTest.createOrUpdateUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME, APPLICATION_ID, CONNECTION_ID,
        ORGANIZATION_ID);

    verify(authApi).requestToken(any());
    verify(managementApi, never()).createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME);
    verify(authApi, never()).resetPassword(EMAIL, CONNECTION_NAME, APPLICATION_ID, ORGANIZATION_ID);
  }

  @Test
  public void test_deleteTenantFail_WhenExceptionThrown() throws Exception {
    ClientsEntity clientsEntity = Mockito.mock(ClientsEntity.class);
    Request<Void> request = Mockito.mock(Request.class);

    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);

    when(managementApi.clients()).thenReturn(clientsEntity);
    when(clientsEntity.delete(APPLICATION_ID)).thenReturn(request);
    doThrow(new Auth0Exception("mock exception")).when(request).execute();

    assertThat(underTest.deleteTenant(APPLICATION_ID, CONNECTION_ID, "")).isFalse();
    verify(managementApi, never()).connections();
  }

  @Test
  public void test_deleteTenant_WhenStrategyIsAuth0() throws Exception {
    ConnectionsEntity connectionsEntity = Mockito.mock(ConnectionsEntity.class);
    Connection connection = new Connection("connectionToDelete", Auth0ManagementAPI.AUTH0_CONNECTION_STRATEGY);
    Request<Connection> connectionRequest = Mockito.mock(Request.class);
    ClientsEntity clientsEntity = Mockito.mock(ClientsEntity.class);
    Request<Void> deletionRequest = Mockito.mock(Request.class);

    when(connectionsEntity.get(any(String.class), any(ConnectionFilter.class))).thenReturn(connectionRequest);
    when(connectionRequest.execute()).thenReturn(connection);
    when(clientsEntity.delete(APPLICATION_ID)).thenReturn(deletionRequest);
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    when(managementApi.clients()).thenReturn(clientsEntity);
    when(managementApi.connections()).thenReturn(connectionsEntity);
    when(connectionsEntity.delete(CONNECTION_ID)).thenReturn(deletionRequest);

    assertThat(underTest.deleteTenant(APPLICATION_ID, CONNECTION_ID, "")).isTrue();
    verify(managementApi.clients()).delete(APPLICATION_ID);
    verify(managementApi.connections()).delete(CONNECTION_ID);
  }

  @Test
  public void test_deleteTenant_WhenConnectionCreationIsSkipped() throws Exception {
    ClientsEntity clientsEntity = Mockito.mock(ClientsEntity.class);
    ConnectionsEntity connectionsEntity = Mockito.mock(ConnectionsEntity.class);
    Request<Void> deletionRequest = Mockito.mock(Request.class);

    when(clientsEntity.delete(APPLICATION_ID)).thenReturn(deletionRequest);
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    when(managementApi.clients()).thenReturn(clientsEntity);
    when(clientsEntity.delete(APPLICATION_ID)).thenReturn(deletionRequest);
    when(managementApi.connections()).thenReturn(connectionsEntity);

    assertThat(underTest.deleteTenant(APPLICATION_ID, CONNECTION_CREATION_SKIPPED, "")).isTrue();
    verify(managementApi.clients()).delete(APPLICATION_ID);
    verify(managementApi.connections(), never()).get(eq(CONNECTION_CREATION_SKIPPED), any(ConnectionFilter.class));
    verify(managementApi.connections(), never()).delete(CONNECTION_CREATION_SKIPPED);
  }

  @Test
  public void test_deleteTenant_WhenConnectionStrategyNotDB() throws Exception {
    ConnectionsEntity connectionsEntity = Mockito.mock(ConnectionsEntity.class);
    Connection connection = new Connection("connectionToDelete", Auth0ManagementAPI.GOOGLE_APPS_CONNECTION_STRATEGY);
    Request<Connection> connectionRequest = Mockito.mock(Request.class);
    ClientsEntity clientsEntity = Mockito.mock(ClientsEntity.class);
    Request<Void> deletionRequest = Mockito.mock(Request.class);

    when(connectionsEntity.get(any(String.class), any(ConnectionFilter.class))).thenReturn(connectionRequest);
    when(connectionRequest.execute()).thenReturn(connection);
    when(clientsEntity.delete(APPLICATION_ID)).thenReturn(deletionRequest);
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    when(managementApi.clients()).thenReturn(clientsEntity);
    when(managementApi.connections()).thenReturn(connectionsEntity);

    assertThat(underTest.deleteTenant(APPLICATION_ID, CONNECTION_ID, "")).isTrue();
    verify(managementApi.clients()).delete(APPLICATION_ID);
    verify(managementApi.connections(), never()).delete(CONNECTION_ID);
  }

  @Test
  public void test_deleteTenant_ShouldNotDeleteAuth0ResourcesWhenOrganizationIdIsSent() throws Exception {
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);

    assertThat(underTest.deleteTenant(APPLICATION_ID, CONNECTION_ID, ORGANIZATION_ID)).isTrue();

    verify(managementApi, never()).deleteOrganization(ORGANIZATION_ID);
    verify(managementApi, never()).clients();
    verify(managementApi, never()).connections();
  }

  @Test
  public void test_canAddMemberToOrganization() throws Exception {
    String userId = "userId";
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);

    underTest.addMemberToOrganization(ORGANIZATION_ID, userId);

    verify(managementApi).addMembersToOrganization(eq(ORGANIZATION_ID), anyList());
  }

  @Test
  public void test_canRemoveMemberFromOrganization() throws Exception {
    String username = "username";
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);

    Member member = mock(Member.class);
    when(managementApi.getMemberFromOrganization(ORGANIZATION_ID, username)).thenReturn(member);

    underTest.removeMemberFromOrganization(ORGANIZATION_ID, username);

    verify(managementApi).removeMembersFromOrganization(eq(ORGANIZATION_ID), anyList());
  }

  @Test
  public void test_shouldNotRemoveUserIfIsNotAMember() throws Exception {
    String username = "username";
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    when(managementApi.getMemberFromOrganization(ORGANIZATION_ID, username)).thenReturn(null);

    underTest.removeMemberFromOrganization(ORGANIZATION_ID, username);

    verify(managementApi, never()).removeMembersFromOrganization(eq(ORGANIZATION_ID), anyList());
  }

  private User mockUser(String id, boolean invitedFlag) {
    Map<String, Object> userMetadata = new HashMap<>();
    userMetadata.put(Auth0ManagementAPI.IS_INVITED_FLAG, invitedFlag);

    User user = mock(User.class);
    lenient().when(user.getId()).thenReturn(id);
    lenient().when(user.getUserMetadata()).thenReturn(userMetadata);
    return user;
  }
}
