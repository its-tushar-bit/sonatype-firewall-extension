/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.jaxrs.error.ErrorResponse;
import io.netty.handler.codec.http.QueryStringDecoder;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.shiro.authc.AuthenticationException;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = OidcLoginFilterTest.OidcLoginFilterTestConfiguration.class)
public class OidcLoginFilterTest
    extends AbstractComponentTest
{
  @TestConfiguration
  static class OidcLoginFilterTestConfiguration
  {
    @Bean
    @Primary
    BaseUrl mockBaseUrl() {
      return mock(BaseUrl.class);
    }

    @Bean
    @Primary
    Configuration mockConfiguration() {
      return mock(Configuration.class);
    }
  }

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

  @Inject
  private BaseUrl mockBaseUrl;

  @Inject
  private Configuration mockConfiguration;

  private String encryptedClientSecret;

  @Before
  public void setup() {
    reset(mockBaseUrl, mockConfiguration);
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
  public void testOnPreHandle_fallsBackToRequestUriWhenPathInfoIsNull() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> authorizationUrlCaptor = ArgumentCaptor.forClass(String.class);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/" + OidcLoginFilter.OAUTH_LOGIN);

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);

    assertThat(result).isFalse();
    verify(response).setHeader(eq("Location"), authorizationUrlCaptor.capture());
    verify(response).setStatus(HttpServletResponse.SC_FOUND);
  }

  @Test
  public void testOnPreHandle_ThrowsExceptionWhenNoOidcConfigurationPresent() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);

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
  public void testOnPreHandle_Callback_UsesProxyWhenConfigured() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);
    final AtomicBoolean proxyHit = new AtomicBoolean(false);

    // Start a real proxy server that records that it was hit, then serves a valid token response
    Server proxyServer = new Server(0);
    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");
    context.addServlet(new ServletHolder(new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        proxyHit.set(true);
        res.setStatus(HttpServletResponse.SC_OK);
        res.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        res.getWriter().print(getTokensResponse("token-response.json"));
      }
    }), "/*");
    proxyServer.setHandler(context);
    proxyServer.start();

    try {
      when(mockBaseUrl.get()).thenReturn(BASE_URL);

      ProxyServerConfiguration proxyConfig = new ProxyServerConfiguration();
      proxyConfig.setHostname("localhost");
      proxyConfig.setPort(((NetworkConnector) proxyServer.getConnectors()[0]).getLocalPort());
      when(mockConfiguration.getProxyServerConfiguration()).thenReturn(proxyConfig);

      tempEntity.newOidcConfiguration(issuer, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, tokenUrl);
      when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
      when(request.getParameter("code")).thenReturn("code");
      when(request.getParameter("hash")).thenReturn("");

      boolean result = oidcLoginFilter.onPreHandle(request, response, null);

      assertThat(result).isFalse();
      assertThat(proxyHit).isTrue();
      verify(response).setHeader(eq("Location"), indexUrlCaptor.capture());
      verify(response).setStatus(HttpServletResponse.SC_FOUND);
      assertThat(indexUrlCaptor.getValue()).isEqualTo(String.format("%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML));
    }
    finally {
      proxyServer.stop();
    }
  }

  @Test
  public void testOnPreHandle_Callback_BypassesProxyWhenHostExcluded() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);
    final AtomicBoolean proxyHit = new AtomicBoolean(false);

    // Start a proxy server that records if it was hit
    Server proxyServer = new Server(0);
    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");
    context.addServlet(new ServletHolder(new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
        proxyHit.set(true);
        res.setStatus(HttpServletResponse.SC_OK);
      }
    }), "/*");
    proxyServer.setHandler(context);
    proxyServer.start();

    try {
      when(mockBaseUrl.get()).thenReturn(BASE_URL);

      ProxyServerConfiguration proxyConfig = new ProxyServerConfiguration();
      proxyConfig.setHostname("localhost");
      proxyConfig.setPort(((NetworkConnector) proxyServer.getConnectors()[0]).getLocalPort());
      proxyConfig.setExcludeHosts("localhost");
      when(mockConfiguration.getProxyServerConfiguration()).thenReturn(proxyConfig);

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
      assertThat(proxyHit).isFalse();
      verify(response).setHeader(eq("Location"), indexUrlCaptor.capture());
      verify(response).setStatus(HttpServletResponse.SC_FOUND);
      assertThat(indexUrlCaptor.getValue()).isEqualTo(String.format("%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML));
    }
    finally {
      proxyServer.stop();
    }
  }

  @Test
  public void testOnPreHandle_Callback_AuthenticationFailureNotMaskedAsTokenError() throws Exception {
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
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-response-malformed-id-token.json"))));
    doThrow(new AuthenticationException("Authentication failed")).when(subject).login(any());

    boolean result = oidcLoginFilter.onPreHandle(request, response, null);

    // Token fetch succeeded, but authentication completion failed (malformed JWT).
    // The error must NOT say "Error getting the OIDC tokens from IDP".
    assertThat(result).isFalse();
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType(ErrorResponse.CONTENT_TYPE);
    verify(response).getWriter();
    verify(writer).close();
    ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
    verify(writer).print(errorCaptor.capture());
    assertThat(errorCaptor.getValue()).doesNotContain(OidcLoginFilter.ERROR_GETTING_TOKENS);
    assertThat(errorCaptor.getValue()).contains("Authentication failed");
  }

  @Test
  public void testIsHostExcluded_ReturnsTrueForMatchingPattern() {
    assertThat(OidcLoginFilter.isHostExcluded("internal.example.com", List.of("*.example.com"))).isTrue();
    assertThat(OidcLoginFilter.isHostExcluded("INTERNAL.EXAMPLE.COM", List.of("*.example.com"))).isTrue();
    assertThat(OidcLoginFilter.isHostExcluded("other.com", List.of("*.example.com"))).isFalse();
    assertThat(OidcLoginFilter.isHostExcluded("example.com", List.of("*.example.com"))).isFalse();
    assertThat(OidcLoginFilter.isHostExcluded("idp.corp", List.of("idp.corp"))).isTrue();
    assertThat(OidcLoginFilter.isHostExcluded("idp.corp", List.of())).isFalse();
    assertThat(OidcLoginFilter.isHostExcluded("idp.corp", null)).isFalse();
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
  public void testOnPreHandle_Login_StoresGuideOriginInSession() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final HttpSession session = mock(HttpSession.class);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);
    when(request.getParameter("hash")).thenReturn("");
    when(request.getParameter("origin")).thenReturn("guide");
    when(request.getSession(true)).thenReturn(session);

    oidcLoginFilter.onPreHandle(request, response, null);

    verify(session).setAttribute(OidcLoginFilter.SESSION_ATTR_SSO_ORIGIN, OidcLoginFilter.ORIGIN_GUIDE);
  }

  @Test
  public void testOnPreHandle_Login_DoesNotCreateSessionForInvalidOrigin() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);
    when(request.getParameter("hash")).thenReturn("");
    when(request.getParameter("origin")).thenReturn("evil");

    oidcLoginFilter.onPreHandle(request, response, null);

    verify(request, times(0)).getSession(true);
    // Verify that getSession(false) is called to check for stale session to clear
    verify(request).getSession(false);
  }

  @Test
  public void testOnPreHandle_Login_ClearsStaleGuideOriginForNonGuideLogin() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final HttpSession session = mock(HttpSession.class);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);
    when(request.getParameter("hash")).thenReturn("");
    when(request.getParameter("origin")).thenReturn(null);
    when(request.getSession(false)).thenReturn(session);

    oidcLoginFilter.onPreHandle(request, response, null);

    verify(session).removeAttribute(OidcLoginFilter.SESSION_ATTR_SSO_ORIGIN);
  }

  @Test
  public void testOnPreHandle_Callback_RedirectsToIqWhenSessionExistsButNoOriginAttribute() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final HttpSession session = mock(HttpSession.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);

    when(mockBaseUrl.get()).thenReturn(BASE_URL);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(OidcLoginFilter.SESSION_ATTR_SSO_ORIGIN)).thenReturn(null);

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn("");
    idpServer.stubFor(post(urlPathEqualTo("/token"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-response.json"))));

    oidcLoginFilter.onPreHandle(request, response, null);

    verify(response).setStatus(HttpServletResponse.SC_FOUND);
    verify(session).removeAttribute(OidcLoginFilter.SESSION_ATTR_SSO_ORIGIN);
    verify(response).setHeader(eq("Location"), indexUrlCaptor.capture());
    assertThat(indexUrlCaptor.getValue()).isEqualTo(String.format("%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML));
  }

  @Test
  public void testOnPreHandle_Callback_RedirectsToGuideWhenOriginInSession() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final HttpSession session = mock(HttpSession.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);

    when(mockBaseUrl.get()).thenReturn(BASE_URL);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(OidcLoginFilter.SESSION_ATTR_SSO_ORIGIN)).thenReturn(OidcLoginFilter.ORIGIN_GUIDE);

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn("");
    idpServer.stubFor(post(urlPathEqualTo("/token"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-response.json"))));

    oidcLoginFilter.onPreHandle(request, response, null);

    verify(response).setStatus(HttpServletResponse.SC_FOUND);
    verify(session).removeAttribute(OidcLoginFilter.SESSION_ATTR_SSO_ORIGIN);
    verify(response).setHeader(eq("Location"), indexUrlCaptor.capture());
    assertThat(indexUrlCaptor.getValue()).isEqualTo(String.format("%s%s", BASE_URL, OidcLoginFilter.GUIDE_INDEX_HTML));
  }

  @Test
  public void testOnPreHandle_Callback_RedirectsToGuideWithHash() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final HttpSession session = mock(HttpSession.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String hash = "#/dashboard";
    final String tokenUrl = String.format("%s/token", issuer);

    when(mockBaseUrl.get()).thenReturn(BASE_URL);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(OidcLoginFilter.SESSION_ATTR_SSO_ORIGIN)).thenReturn(OidcLoginFilter.ORIGIN_GUIDE);

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn(hash);
    idpServer.stubFor(post(urlPathEqualTo("/token"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-response.json"))));

    oidcLoginFilter.onPreHandle(request, response, null);

    verify(response).setStatus(HttpServletResponse.SC_FOUND);
    verify(response).setHeader(eq("Location"), indexUrlCaptor.capture());
    assertThat(indexUrlCaptor.getValue())
        .isEqualTo(String.format("%s%s%s", BASE_URL, OidcLoginFilter.GUIDE_INDEX_HTML, hash));
  }

  @Test
  public void testOnPreHandle_Callback_RedirectsToIqWhenNoSession() throws Exception {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);

    when(mockBaseUrl.get()).thenReturn(BASE_URL);
    when(request.getSession(false)).thenReturn(null);

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, encryptedClientSecret, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn("");
    idpServer.stubFor(post(urlPathEqualTo("/token"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-response.json"))));

    oidcLoginFilter.onPreHandle(request, response, null);

    verify(response).setStatus(HttpServletResponse.SC_FOUND);
    verify(response).setHeader(eq("Location"), indexUrlCaptor.capture());
    assertThat(indexUrlCaptor.getValue()).isEqualTo(String.format("%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML));
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
