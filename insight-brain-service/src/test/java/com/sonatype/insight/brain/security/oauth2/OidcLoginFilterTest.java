/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.dataaccess.security.OidcTokenDAO;
import com.sonatype.insight.brain.model.security.OidcToken;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.inject.Binder;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.server.Response;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
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
  private OidcTokenDAO oidcTokenDAO;

  @Inject
  private JWTGenerator jwtGenerator;

  @Mock
  private BaseUrl mockBaseUrl;

  @Override
  public void configure(final Binder binder) {
    binder.bind(BaseUrl.class).toInstance(mockBaseUrl);
    super.configure(binder);
  }

  @Test
  public void testDoFilter_RedirectsToIndexIfOidcTokenExistsInCookie() throws ServletException, IOException {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);
    final String expectedUrl = String.format("%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML);

    String token = jwtGenerator.generateJWT("sub", issuer);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);
    when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(JwtAuthenticationFilter.ID_TOKEN_COOKIE, token)});

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);

    oidcLoginFilter.doFilter(request, response, null);

    verify(response).sendRedirect(indexUrlCaptor.capture());
    verify(request).getCookies();
    assertThat(indexUrlCaptor.getValue()).isEqualTo(expectedUrl);
  }

  @Test
  public void testDoFilter_RedirectsToIndexIfTokenIdExistsInCookieAndOidcTokenIsOnDB()
      throws ServletException, IOException
  {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);
    final String expectedUrl = String.format("%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML);

    String token = jwtGenerator.generateJWT("sub", issuer);
    OidcToken oidcToken = new OidcToken(token);
    oidcTokenDAO.insert(oidcToken);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);
    when(request.getCookies()).thenReturn(
        new Cookie[]{new Cookie(JwtAuthenticationFilter.ID_TOKEN_COOKIE, oidcToken.getToken())});

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);

    oidcLoginFilter.doFilter(request, response, null);

    verify(response).sendRedirect(indexUrlCaptor.capture());
    verify(request).getCookies();
    assertThat(indexUrlCaptor.getValue()).isEqualTo(expectedUrl);
  }

  @Test
  public void testDoFilter_RedirectsToIndexWithHashIfTokenIdIsPresentInCookieAndHashParameterIsSent()
      throws ServletException, IOException
  {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);
    final String hash = "#/dashboard/violations";
    final String expectedUrl = String.format("%s%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML, hash);

    String token = jwtGenerator.generateJWT("sub", issuer);
    OidcToken oidcToken = new OidcToken(token);
    oidcTokenDAO.insert(oidcToken);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);
    when(request.getCookies()).thenReturn(
        new Cookie[]{new Cookie(JwtAuthenticationFilter.ID_TOKEN_COOKIE, oidcToken.getId())});

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);
    when(request.getParameter("hash")).thenReturn(hash);

    oidcLoginFilter.doFilter(request, response, null);

    verify(response).sendRedirect(indexUrlCaptor.capture());
    verify(request).getCookies();
    assertThat(indexUrlCaptor.getValue()).isEqualTo(expectedUrl);
  }

  @Test
  public void testDoFilter_ThrowsExceptionWhenNoOidcConfigurationPresent() throws ServletException, IOException {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);

    oidcLoginFilter.doFilter(request, response, null);

    verifyErrorResponse(response, Response.SC_UNAUTHORIZED, writer, OidcLoginFilter.OIDC_CONFIGURATION_INVALID);
  }

  @Test
  public void testDoFilter_Login_RedirectsToAuthorizationUrl() throws ServletException, IOException {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> authorizationUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String expectedUrl = String.format("%s%s", BASE_URL, OidcLoginFilter.OAUTH_CALLBACK);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);

    oidcLoginFilter.doFilter(request, response, null);

    verify(response).sendRedirect(authorizationUrlCaptor.capture());
    assertAuthorizationUrlIsTheExpected(authorizationUrlCaptor.getValue(), expectedUrl);
  }

  @Test
  public void testDoFilter_Login_RedirectsToAuthorizationUrlWithHash() throws ServletException, IOException {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> authorizationUrlCaptor = ArgumentCaptor.forClass(String.class);
    final String hash = "#/dashboard/violations";
    final String expectedUrl = String.format("%s%s?hash=%s", BASE_URL, OidcLoginFilter.OAUTH_CALLBACK,
        URLEncoder.encode(hash, StandardCharsets.UTF_8));
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);
    when(request.getParameter("hash")).thenReturn(hash);

    oidcLoginFilter.doFilter(request, response, null);

    verify(response).sendRedirect(authorizationUrlCaptor.capture());
    assertAuthorizationUrlIsTheExpected(authorizationUrlCaptor.getValue(), expectedUrl);
  }

  @Test
  public void testDoFilter_Login_ThrowErrorIfNotAbleToBuildAuthorizationUrl() throws ServletException, IOException {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);
    when(mockBaseUrl.get()).thenReturn("{bad-url}");

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_LOGIN);

    oidcLoginFilter.doFilter(request, response, null);

    verifyErrorResponse(response, Response.SC_UNAUTHORIZED, writer,
        OidcLoginFilter.ERROR_BUILDING_AUTHORIZATION_REQUEST);
  }

  @Test
  public void testDoFilter_Callback_GetTokensAndRedirectsToIndex() throws ServletException, IOException {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
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

    oidcLoginFilter.doFilter(request, response, null);

    verify(response).sendRedirect(indexUrlCaptor.capture());
    verify(response).addCookie(cookieCaptor.capture());
    assertThat(cookieCaptor.getValue().getName()).isEqualTo(JwtAuthenticationFilter.ID_TOKEN_COOKIE);
    assertThat(cookieCaptor.getValue().getValue()).isNotBlank();
    assertThat(oidcTokenDAO.getById(cookieCaptor.getValue().getValue())).isNotNull();
    assertThat(cookieCaptor.getValue().isHttpOnly()).isTrue();
    assertThat(cookieCaptor.getValue().getSecure()).isTrue();
    assertThat(indexUrlCaptor.getValue()).isEqualTo(String.format("%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML));
  }

  @Test
  public void testDoFilter_Callback_GetTokensAndRedirectsToIndexIncludingHash() throws ServletException, IOException {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final ArgumentCaptor<String> indexUrlCaptor = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
    final String issuer = idpServer.baseUrl();
    final String hash = "#/dashboard/violations";
    final String expectedUrl = String.format("%s%s%s", BASE_URL, OidcLoginFilter.INDEX_HTML, hash);
    final String tokenUrl = String.format("%s/token", issuer);

    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn(hash);
    idpServer.stubFor(post(urlPathEqualTo("/token"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-response.json"))));

    oidcLoginFilter.doFilter(request, response, null);

    verify(response).sendRedirect(indexUrlCaptor.capture());
    verify(response).addCookie(cookieCaptor.capture());
    assertThat(cookieCaptor.getValue().getName()).isEqualTo(JwtAuthenticationFilter.ID_TOKEN_COOKIE);
    assertThat(cookieCaptor.getValue().getValue()).isNotBlank();
    assertThat(oidcTokenDAO.getById(cookieCaptor.getValue().getValue())).isNotNull();
    assertThat(cookieCaptor.getValue().isHttpOnly()).isTrue();
    assertThat(cookieCaptor.getValue().getSecure()).isTrue();
    assertThat(indexUrlCaptor.getValue()).isEqualTo(expectedUrl);
  }

  @Test
  public void testDoFilter_Callback_ThrowErrorIfNotAbleToBuildRequest() throws ServletException, IOException {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);
    final String issuer = idpServer.baseUrl();
    final String tokenUrl = String.format("%s/token", issuer);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(issuer, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, tokenUrl);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn("");

    idpServer.stubFor(post(urlPathEqualTo("/token"))
        .willReturn(aResponse()
            .withStatus(400)
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getTokensResponse("token-error-response.json"))));

    oidcLoginFilter.doFilter(request, response, null);

    verifyErrorResponse(response, Response.SC_UNAUTHORIZED, writer, OidcLoginFilter.ERROR_GETTING_TOKENS);
  }

  @Test
  public void testDoFilter_Callback_ThrowErrorIfCallbackIsAnErrorResponse() throws ServletException, IOException {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);
    final String errorMessage = "Something wrong happened";

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, TOKEN_URL);
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("error_description")).thenReturn(errorMessage);
    when(request.getParameter("code")).thenReturn("");
    when(request.getParameter("hash")).thenReturn("");

    oidcLoginFilter.doFilter(request, response, null);

    verifyErrorResponse(response, Response.SC_UNAUTHORIZED, writer,
        String.format(OidcLoginFilter.ERROR_AUTHORIZING_REQUEST, errorMessage));
  }

  @Test
  public void testDoFilter_Callback_ThrowErrorIfNotAbleToGetTokens() throws ServletException, IOException {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final PrintWriter writer = setupPrintWriter(response);
    when(mockBaseUrl.get()).thenReturn(BASE_URL);

    tempEntity.newOidcConfiguration(ISSUER, CLIENT_ID, CLIENT_SECRET, AUTHORIZATION_URL, ":");
    when(request.getPathInfo()).thenReturn(OidcLoginFilter.OAUTH_CALLBACK);
    when(request.getParameter("code")).thenReturn("code");
    when(request.getParameter("hash")).thenReturn("");

    oidcLoginFilter.doFilter(request, response, null);

    verifyErrorResponse(response, Response.SC_UNAUTHORIZED, writer, OidcLoginFilter.ERROR_BUILDING_TOKEN_REQUEST);
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
      final String errMessage)
      throws IOException
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
