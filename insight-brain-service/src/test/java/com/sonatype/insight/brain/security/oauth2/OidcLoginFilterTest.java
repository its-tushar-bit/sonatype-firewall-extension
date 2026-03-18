/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.inject.Binder;
import io.netty.handler.codec.http.QueryStringDecoder;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class OidcLoginFilterTest
    extends AbstractComponentTest
{
  public static final String ISSUER = "https://www.an-idp.com/";

  public static final String CLIENT_ID = "client-id";

  public static final String CLIENT_SECRET = "client-secret";

  public static final String AUTHORIZATION_URL = "https://www.an-idp.com/authorize";

  public static final String TOKEN_URL = "https://www.an-idp.com/token";

  public static final String BASE_URL = "http://localhost:8070/";

  @Rule
  public WireMockRule idpServer = new WireMockRule(wireMockConfig().dynamicPort());

  @Inject
  private OidcLoginFilter oidcLoginFilter;

  @Inject
  private PasswordHandler passwordHandler;

  @Mock
  private BaseUrl mockBaseUrl;

  private String encryptedClientSecret;

  @Override
  public void configure(final Binder binder) {
    binder.bind(BaseUrl.class).toInstance(mockBaseUrl);
    super.configure(binder);
  }

  @Before
  public void setup() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    encryptedClientSecret = passwordHandler.encryptPassword(CLIENT_SECRET);
  }

  @Test
  public void testOnPreHandle_TrueIfOAuth2NotEnabled() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(false);

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);
    assertThat(result).isTrue();
  }

  @Test
  public void testOnPreHandle_ThrowsExceptionWhenNoOidcConfigurationPresent() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);

    assertThat(result).isFalse();
    verifyErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, writer,
        OidcLoginFilter.OIDC_CONFIGURATION_INVALID);
  }

  @Test
  public void testOnPreHandle_Login_RedirectsToAuthorizationUrl() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> authorizationUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String expectedUrl = String.format("%s%s", BASE_URL, OidcLoginFilter.OAUTH_CALLBACK);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);

    assertThat(result).isFalse();
    verify(response).setHeader(eq("Location"), authorizationUrlCaptor.capture());
    verify(response).setStatus(HttpServletResponse.SC_FOUND);
    assertAuthorizationUrlIsTheExpected(authorizationUrlCaptor.getValue(), expectedUrl);
  }

  @Test
  public void testOnPreHandle_Login_RedirectsToAuthorizationUrlWithHash() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> authorizationUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String hash = "#/dashboard/violations";
    final String expectedUrl = String.format("%s%s?hash=%s", BASE_URL, OidcLoginFilter.OAUTH_CALLBACK,
        URLEncoder.encode(hash, StandardCharsets.UTF_8));
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);
    when(request.getParameter("hash")).thenReturn(hash);

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);

    assertThat(result).isFalse();
    verify(response).setHeader(eq("Location"), authorizationUrlCaptor.capture());
    verify(response).setStatus(HttpServletResponse.SC_FOUND);
    assertAuthorizationUrlIsTheExpected(authorizationUrlCaptor.getValue(), expectedUrl);
  }

  @Test
  public void testOnPreHandle_Login_ThrowErrorIfNotAbleToBuildAuthorizationUrl() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);
    when(mockBaseUrl.get()).thenReturn("{bad-url}");

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);

    assertThat(result).isFalse();
    verifyErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, writer,
        OidcLoginFilter.ERROR_BUILDING_AUTHORIZATION_REQUEST);
  }

  @Test
  public void testOnPreHandle_Callback_GetTokensAndRedirectsToIndex() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);

    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn("");
    idpServer.stubFor(post(urlPathEqualTo("/token"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-response.json"))));

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);

    assertThat(result).isFalse();
    verify(response).setHeader(eq("Location"), indexUrlCaptor.capture());
    verify(response).setStatus(HttpServletResponse.SC_FOUND);
    assertThat(indexUrlCaptor.getValue()).isEqualTo(String.format("%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML));
    assertThat(getOidcClientSecretRefValue(oidcLoginFilter)).isEqualTo(CLIENT_SECRET);
  }

  @Test
  public void testOnPreHandle_Callback_GetTokensAndRedirectsToIndexIncludingHash() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String hash = "#/dashboard/violations";
    final String expectedUrl = String.format("%s%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML, hash);
    final String tokenUrl = String.format("%s/token", issuer);

    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn(hash);
    idpServer.stubFor(post(urlPathEqualTo("/token"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-response.json"))));

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);

    assertThat(result).isFalse();
    verify(response).setHeader(eq("Location"), indexUrlCaptor.capture());
    verify(response).setStatus(HttpServletResponse.SC_FOUND);
    assertThat(indexUrlCaptor.getValue()).isEqualTo(expectedUrl);
    assertThat(getOidcClientSecretRefValue(oidcLoginFilter)).isEqualTo(CLIENT_SECRET);
  }

  @Test
  public void testOnPreHandle_Callback_handlesUnEncryptedClientSecret() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);

    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn("");
    idpServer.stubFor(post(urlPathEqualTo("/token"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-response.json"))));

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);
    assertThat(result).isFalse();
    verify(response).setHeader(eq("Location"), indexUrlCaptor.capture());
    verify(response).setStatus(HttpServletResponse.SC_FOUND);
    assertThat(indexUrlCaptor.getValue()).isEqualTo(String.format("%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML));
    assertThat(getOidcClientSecretRefValue(oidcLoginFilter)).isEqualTo(CLIENT_SECRET);
  }

  @Test
  public void testOnPreHandle_clearCache() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);

    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn("");
    idpServer.stubFor(post(urlPathEqualTo("/token"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-response.json"))));

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);
    assertThat(result).isFalse();
    assertThat(getOidcClientSecretRefValue(oidcLoginFilter)).isEqualTo(CLIENT_SECRET);

    // Update the oidc configuration secret to an encrypted value and clear the cache
    tempEntity.updateOidcConfiguration(issuer, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, tokenUrl);
    oidcLoginFilter.clearCachedOidcClientSecret();
    assertThat(getOidcClientSecretRefValue(oidcLoginFilter)).isNull();

    // Ensure the new decrypted secret value is used
    result = oidcLoginFilter.onPreHandle(request, response, null);
    assertThat(result).isFalse();
    assertThat(getOidcClientSecretRefValue(oidcLoginFilter)).isEqualTo(CLIENT_SECRET);
  }

  @Test
  public void testOnPreHandle_Callback_ThrowErrorIfNotAbleToBuildRequest() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn("");

    idpServer.stubFor(post(urlPathEqualTo("/token"))
        .willReturn(aResponse()
            .withStatus(400)
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-error-response.json"))));

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);

    assertThat(result).isFalse();
    verifyErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, writer, OidcLoginFilter.ERROR_GETTING_TOKENS);
  }

  @Test
  public void testOnPreHandle_Callback_ThrowErrorIfCallbackIsAnErrorResponse() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);
    final String errorMessage = "Something wrong happened";

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("error_description")).thenReturn(errorMessage);
    when(request.getParameter("code")).thenReturn("");
    when(request.getParameter("hash")).thenReturn("");

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);

    assertThat(result).isFalse();
    verifyErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, writer,
        String.format(OidcLoginFilter.ERROR_AUTHORIZING_REQUEST, errorMessage));
  }

  @Test
  public void testOnPreHandle_Callback_ThrowErrorIfNotAbleToGetTokens() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, ":");
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn("");

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);

    assertThat(result).isFalse();
    verifyErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, writer,
        OidcLoginFilter.ERROR_BUILDING_TOKEN_REQUEST);
  }

  @Test
  public void testOnAccessDenied_ThrowsError() {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);

    assertThatThrownBy(() -> oidcLoginFilter.onAccessDenied(request, response))
        .isInstanceOf(IllegalStateException.class);
  }

  private static PrintWriter setupPrintWriter(final HttpServletResponse response) throws IOException {
    final PrintWriter writer = mock(PrintWriter.class);
    when(response.getWriter()).thenReturn(writer);
    return writer;
  }

  private void verifyErrorResponse(
      final HttpServletResponse response,
      final int expectedStatusCode,
      final PrintWriter writer,
      final String errMessage) throws IOException
  {
    verify(response).setStatus(expectedStatusCode);
    verify(response).setContentType(ErrorResponse.CONTENT_TYPE);
    verify(response).getWriter();
    verifyNoMoreInteractions(response);

    verify(writer).print(errMessage);
    verify(writer).close();
    verifyNoMoreInteractions(writer);
  }

  private static void assertAuthorizationUrlIsTheExpected(String authorizationUrl, String expectedUrl) {
    QueryStringDecoder decoder = new QueryStringDecoder(authorizationUrl);
    Map<String, List<String>> parameters = decoder.parameters();
    assertThat(authorizationUrl).contains(AUTHORIZATION_URL);
    assertThat(parameters.get("client_id")).contains(CLIENT_ID);
    assertThat(parameters.get("redirect_uri")).contains(expectedUrl);
    assertThat(parameters.get("response_type")).contains("code");
    assertThat(parameters.get("state")).isNotEmpty();
    assertThat(parameters.get("nonce")).isNotEmpty();

    String scopes = parameters.get("scope").toString();
    for (String scope : OidcLoginFilter.OIDC_SCOPES) {
      assertThat(scopes).contains(scope);
    }
  }

  private String getOidcClientSecretRefValue(OidcLoginFilter oidcLoginFilter) throws Exception {
    Field field = OidcLoginFilter.class.getDeclaredField("oidcClientSecretRef");
    field.setAccessible(true);
    TenantReference<String> ref = (TenantReference<String>) field.get(oidcLoginFilter);
    return ref.get();
  }

  protected String getTokensResponse(String fileName) {
    try {
      return IOUtils.toString(getClass().getResourceAsStream(String.format("/OidcLoginFilterTest/%s", fileName)),
          StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
