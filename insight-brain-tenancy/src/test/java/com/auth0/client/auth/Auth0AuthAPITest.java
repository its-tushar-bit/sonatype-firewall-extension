/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.auth0.client.auth;

import java.io.IOException;
import java.util.Objects;

import com.auth0.exception.Auth0Exception;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Auth0AuthAPITest
{
  @Mock
  private OkHttpClient client;

  @Mock
  private Call call;

  @Captor
  private ArgumentCaptor<Request> requestCaptor;

  public Auth0AuthAPI auth0AuthAPI;

  private static final String DOMAIN = "https://sonatype.auth0.com";

  private static final String CLIENT_ID = "client-id";

  private static final String CLIENT_SECRET = "client-secret";

  @Before
  public void before() {
    auth0AuthAPI = spy(new Auth0AuthAPI(DOMAIN, CLIENT_ID, CLIENT_SECRET));
  }

  @Test
  public void testAuth0AuthAPI_resetPassword() throws Exception {
    String email = "test@company.com";
    String connectionName = "db-connection-name";
    String applicationClientId = "application-client-id";
    String organizationId = "organization-id";

    when(auth0AuthAPI.getClient()).thenReturn(client);
    when(client.newCall(any(Request.class))).thenReturn(call);
    mockAuth0Response();

    auth0AuthAPI.resetPassword(email, connectionName, applicationClientId, organizationId);

    verifySentRequestIsTheExpected(email, connectionName, applicationClientId, organizationId);
  }

  @Test
  public void testAuth0AuthAPI_resetPassword_auth0Error() throws Exception {
    String email = "test@company.com";
    String connectionName = "db-connection-name";
    String applicationClientId = "application-client-id";
    String organizationId = "organization-id";

    when(auth0AuthAPI.getClient()).thenReturn(client);
    when(client.newCall(any(Request.class))).thenReturn(call);
    doThrow(new Auth0Exception("Auth0 Error")).when(call).execute();

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> auth0AuthAPI.resetPassword(email, connectionName, applicationClientId, organizationId))
        .withMessage("com.auth0.exception.Auth0Exception: Auth0 Error");

    verifySentRequestIsTheExpected(email, connectionName, applicationClientId, organizationId);
  }

  private void mockAuth0Response() throws IOException {
    Request responseRequest = new Request.Builder().url(DOMAIN).build();
    Response response = new Response.Builder()
        .protocol(Protocol.HTTP_2)
        .request(responseRequest)
        .message("Reset sent")
        .body(ResponseBody.create("{}", MediaType.get("application/json; charset=utf-8")))
        .code(204)
        .build();
    when(call.execute()).thenReturn(response);
  }

  private void verifySentRequestIsTheExpected(
      final String email,
      final String connectionName,
      final String applicationClientId,
      final String organizationId)
      throws IOException
  {
    verify(client).newCall(requestCaptor.capture());
    verify(call).execute();
    Request request = requestCaptor.getValue();
    Buffer buffer = new Buffer();

    Objects.requireNonNull(request.body()).writeTo(buffer);

    assertThat(request.url().url().toString()).isEqualTo("https://sonatype.auth0.com/dbconnections/change_password");
    assertThat(request.method()).isEqualTo("POST");
    assertThat(buffer.readUtf8())
        .contains(email)
        .contains(connectionName)
        .contains(applicationClientId)
        .contains(organizationId);
  }
}
