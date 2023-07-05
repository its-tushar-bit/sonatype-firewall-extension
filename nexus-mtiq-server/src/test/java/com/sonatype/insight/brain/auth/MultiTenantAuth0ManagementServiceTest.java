/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.auth;

import com.sonatype.insight.brain.service.Auth0Config;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import com.auth0.client.auth.Auth0AuthAPI;
import com.auth0.client.mgmt.Auth0ManagementAPI;
import com.auth0.client.mgmt.ClientsEntity;
import com.auth0.client.mgmt.ConnectionsEntity;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.net.Request;
import com.auth0.net.TokenRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService.CONNECTION_CREATION_SKIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantAuth0ManagementServiceTest
{
  private static final String EMAIL = "email";

  private static final String FIRST_NAME = "first";

  private static final String LAST_NAME = "last";

  private static final String CONNECTION_NAME = "connection";

  private static final String CLIENT_ID = "clientId";

  public static final String APPLICATION_ID = "applicationId";

  private static final String CONNECTION_ID = "connectionId";

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

  @Before
  public void setUp() throws Exception {
    Auth0Config auth0Config = new Auth0Config();
    auth0Config.setDomain("domain");
    auth0Config.setClientId(CLIENT_ID);
    auth0Config.setClientSecret("clientSecret");
    when(config.getAuth0Config()).thenReturn(auth0Config);
    underTest = new MultiTenantAuth0ManagementService(config, auth0ApiSupplier);

    when(auth0ApiSupplier.getManagementApi(any(), any())).thenReturn(managementApi);
    when(auth0ApiSupplier.getAuthApi(any(), any(), any())).thenReturn(authApi);
  }

  @Test
  public void test_canCreateUser() throws Exception {
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);

    underTest.createOrUpdateUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME, APPLICATION_ID, CONNECTION_ID);

    verify(authApi).requestToken(any());
    verify(managementApi).createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME);
    verify(authApi).resetPassword(EMAIL, CONNECTION_NAME, APPLICATION_ID);
  }

  @Test
  public void test_createUserOnAuth0FailsThatNoPasswordResetIsSent() throws Exception {
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    when(managementApi.createOrGetUser(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("user creation failed"));

    assertThatThrownBy(() -> {
      underTest.createOrUpdateUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME, APPLICATION_ID, CONNECTION_ID);
    }).isInstanceOf(RuntimeException.class).hasMessageContaining("user creation failed");

    verify(authApi).requestToken(any());
    verify(managementApi).createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME);
    verify(authApi, never()).resetPassword(EMAIL, CONNECTION_NAME, APPLICATION_ID);
  }

  @Test
  public void test_restPasswordFailWillDeleteAuth0User() throws Exception {
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    when(authApi.resetPassword(any(), any(), any())).thenThrow(new RuntimeException("Password reset failed"));
    when(managementApi.userExists(any(), any())).thenReturn(false);

    assertThatThrownBy(
        () -> underTest.createOrUpdateUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME,
            APPLICATION_ID, CONNECTION_ID))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Password reset failed");

    verify(authApi).requestToken(any());
    verify(managementApi).createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME);
    verify(authApi).resetPassword(EMAIL, CONNECTION_NAME, APPLICATION_ID);
    verify(managementApi).deleteUserByEmailFromConnection(EMAIL, CONNECTION_ID);
  }

  @Test
  public void test_restPasswordNotSentIfUserAlreadyExists() throws Exception {
    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    when(managementApi.userExists(any(), any())).thenReturn(true);

    underTest.createOrUpdateUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME, APPLICATION_ID, CONNECTION_ID);

    verify(authApi).requestToken(any());
    verify(managementApi, never()).createOrGetUser(EMAIL, FIRST_NAME, LAST_NAME, CONNECTION_NAME);
    verify(authApi, never()).resetPassword(EMAIL, CONNECTION_NAME, APPLICATION_ID);
  }

  @Test
  public void test_deleteTenantFailWhenExceptionThrown() throws Exception {
    ClientsEntity clientsEntity = Mockito.mock(ClientsEntity.class);
    Request<Void> request = Mockito.mock(Request.class);

    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);

    when(managementApi.clients()).thenReturn(clientsEntity);
    when(clientsEntity.delete(APPLICATION_ID)).thenReturn(request);
    doThrow(new Auth0Exception("mock exception")).when(request).execute();

    assertThat(underTest.deleteTenant(APPLICATION_ID, CONNECTION_ID)).isFalse();
    verify(managementApi, never()).connections();
  }

  @Test
  public void test_deleteTenant() throws Exception {
    ClientsEntity clientsEntity = Mockito.mock(ClientsEntity.class);
    ConnectionsEntity connectionsEntity = Mockito.mock(ConnectionsEntity.class);
    Request<Void> request = Mockito.mock(Request.class);

    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    when(managementApi.clients()).thenReturn(clientsEntity);
    when(clientsEntity.delete(APPLICATION_ID)).thenReturn(request);
    when(managementApi.connections()).thenReturn(connectionsEntity);
    when(connectionsEntity.delete(CONNECTION_ID)).thenReturn(request);

    assertThat(underTest.deleteTenant(APPLICATION_ID, CONNECTION_ID)).isTrue();
    verify(managementApi.clients()).delete(APPLICATION_ID);
    verify(managementApi.connections()).delete(CONNECTION_ID);
  }

  @Test
  public void test_deleteTenant_whenConnectionCreationIsSkipped() throws Exception {
    ClientsEntity clientsEntity = Mockito.mock(ClientsEntity.class);
    ConnectionsEntity connectionsEntity = Mockito.mock(ConnectionsEntity.class);
    Request<Void> request = Mockito.mock(Request.class);

    when(tokenRequest.execute()).thenReturn(tokenHolder);
    when(authApi.requestToken(any())).thenReturn(tokenRequest);
    when(managementApi.clients()).thenReturn(clientsEntity);
    when(clientsEntity.delete(APPLICATION_ID)).thenReturn(request);
    when(managementApi.connections()).thenReturn(connectionsEntity);

    assertThat(underTest.deleteTenant(APPLICATION_ID, CONNECTION_CREATION_SKIPPED)).isTrue();
    verify(managementApi.clients()).delete(APPLICATION_ID);
    verify(managementApi.connections(), never()).delete(CONNECTION_CREATION_SKIPPED);
  }
}
