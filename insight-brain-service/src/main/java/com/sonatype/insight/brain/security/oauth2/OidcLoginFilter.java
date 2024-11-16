/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.security.LoginErrorResponseHandler;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest.Builder;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.eclipse.jetty.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class OidcLoginFilter
    extends HttpFilter
{
  private static final Logger log = LoggerFactory.getLogger(OidcLoginFilter.class.getName());

  public static final String OAUTH_CALLBACK = "oidc/callback";

  public static final String OAUTH_LOGIN = "oidc/login";

  public static final String INDEX_HTML = "assets/index.html";

  public static final String OIDC_CONFIGURATION_INVALID =
      "There is no OIDC configuration to trigger the login";

  public static final String ERROR_BUILDING_AUTHORIZATION_REQUEST = "Error building the authorization request";

  public static final String ERROR_GETTING_TOKENS = "Error getting the OIDC tokens from IDP";

  public static final String ERROR_BUILDING_TOKEN_REQUEST = "Error building the token request";

  public static final String[] OIDC_SCOPES = {"openid", "profile", "email"};

  public static final String ERROR_AUTHORIZING_REQUEST = "Error authorizing request: %s";

  public static final int COOKIE_MAX_AGE_IN_SECONDS = 30;

  private final OidcConfigurationDAO oidcConfigurationDAO;

  private final BaseUrl baseUrl;

  @Inject
  public OidcLoginFilter(BaseUrl baseUrl, OidcConfigurationDAO oidcConfigurationDAO) {
    this.oidcConfigurationDAO = oidcConfigurationDAO;
    this.baseUrl = baseUrl;
  }

  @Override
  protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException
  {
    String path = req.getPathInfo();
    String hash = req.getParameter("hash");
    String encodedHash = StringUtils.isNotBlank(hash) ? URLEncoder.encode(hash, StandardCharsets.UTF_8) : hash;

    log.info("Calling OAuth endpoint {}", path);

    try {
      OidcConfiguration oidcConfiguration = oidcConfigurationDAO.get();

      if (oidcConfiguration == null) {
        throw new AuthenticationException(OIDC_CONFIGURATION_INVALID);
      }

      // Check if ID Token cookie is present, so no need to login again
      String idToken = getCookie(req, JwtAuthenticationFilter.ID_TOKEN_COOKIE);
      if (StringUtils.isNotBlank(idToken)) {
        String redirectUrl = buildRedirectUrl(INDEX_HTML, hash, false);
        res.sendRedirect(redirectUrl);
        return;
      }

      // Handles the login request by sending an authentication request
      if (path.contains(OAUTH_LOGIN)) {
        String callbackUrl = buildRedirectUrl(OAUTH_CALLBACK, encodedHash, true);
        sendAuthorizationRequest(res, oidcConfiguration, callbackUrl);
      }

      // Handles the callback request from the IDP to get the Access and ID tokens
      if (path.contains(OAUTH_CALLBACK)) {
        String callbackUrl = buildRedirectUrl(INDEX_HTML, encodedHash, true);
        String redirectUrl = buildRedirectUrl(INDEX_HTML, hash, false);
        handleCallbackAndSetAuthCookie(req, res, oidcConfiguration, callbackUrl, redirectUrl);
      }
    }
    catch (AuthenticationException e) {
      ErrorResponse errorResponse = new ErrorResponse(Response.SC_UNAUTHORIZED, e.getMessage());
      LoginErrorResponseHandler.sendError(res, errorResponse);
    }
  }

  private String buildRedirectUrl(final String path, final String hash, final boolean useQueryParamForHash) {
    boolean hasHash = StringUtils.isNotBlank(hash);
    StringBuilder redirect = new StringBuilder()
        .append(baseUrl.get())
        .append(path);

    if (useQueryParamForHash && hasHash) {
      redirect.append("?hash=");
      redirect.append(hash);
    }
    else if (hasHash) {
      redirect.append(hash);
    }

    return redirect.toString();
  }

  private void sendAuthorizationRequest(
      final HttpServletResponse res, final OidcConfiguration oidcConfiguration,
      final String callbackUrl)
      throws IOException
  {
    AuthenticationRequest authorizeUrlRequest = buildAuthenticationRequest(oidcConfiguration, callbackUrl);

    String authorizeUrl = authorizeUrlRequest.toURI().toString();

    res.sendRedirect(authorizeUrl);
  }

  private AuthenticationRequest buildAuthenticationRequest(
      OidcConfiguration oidcConfiguration,
      String callbackUrl)
  {
    String clientId = oidcConfiguration.getClientId();
    String authorizationUrl = oidcConfiguration.getIdpAuthorizationUrl();
    Map<String, String> authorizationRequestParameters = oidcConfiguration.getAuthorizationCustomParams();

    try {
      // The client ID provisioned by the OpenID provider when
      // the client was registered
      ClientID clientID = new ClientID(clientId);

      // The client callback URL
      URI callback = new URI(callbackUrl);

      // Generate random state string to securely pair the callback to this request
      State state = new State();

      // Generate nonce for the ID token
      Nonce nonce = new Nonce();

      // Compose the OpenID authentication request (for the code flow)
      AuthenticationRequest.Builder builder = new Builder(
          new ResponseType("code"),
          new Scope(OIDC_SCOPES),
          clientID,
          callback)
          .endpointURI(new URI(authorizationUrl))
          .state(state)
          .nonce(nonce);

      // Add custom parameters to the request
      authorizationRequestParameters.forEach(builder::customParameter);

      // Build the request
      return builder.build();
    }
    catch (Exception exception) {
      log.error(ERROR_BUILDING_AUTHORIZATION_REQUEST, exception);
      throw new AuthenticationException(ERROR_BUILDING_AUTHORIZATION_REQUEST, exception);
    }
  }

  private void handleCallbackAndSetAuthCookie(
      final HttpServletRequest req,
      final HttpServletResponse res,
      final OidcConfiguration oidcConfiguration,
      final String callbackUrl,
      final String redirectUrl)
  {
    String codeParameter = req.getParameter("code");

    if (StringUtils.isBlank(codeParameter)) {
      String authErrorDescription = req.getParameter("error_description");
      throw new AuthenticationException(String.format(ERROR_AUTHORIZING_REQUEST, authErrorDescription));
    }

    TokenRequest tokenRequest = buildTokenRequest(oidcConfiguration, callbackUrl, codeParameter);

    try {
      TokenResponse tokenResponse = OIDCTokenResponseParser.parse(tokenRequest.toHTTPRequest().send());

      if (!tokenResponse.indicatesSuccess()) {
        // We got an error response...
        String error = tokenResponse.toErrorResponse().getErrorObject().getDescription();
        throw new AuthenticationException(error);
      }

      OIDCTokenResponse successResponse = (OIDCTokenResponse) tokenResponse.toSuccessResponse();
      addSecureCookie(res, JwtAuthenticationFilter.ID_TOKEN_COOKIE, successResponse.getOIDCTokens().getIDTokenString());

      res.sendRedirect(redirectUrl);
    }
    catch (Exception e) {
      log.error(ERROR_GETTING_TOKENS, e);
      throw new AuthenticationException(ERROR_GETTING_TOKENS, e);
    }
  }

  private TokenRequest buildTokenRequest(
      OidcConfiguration oidcConfiguration,
      String redirectUri,
      String codeParameter)
  {
    String clientId = oidcConfiguration.getClientId();
    String clientSecret = oidcConfiguration.getClientSecret();
    String oauthRequestTokensUrl = oidcConfiguration.getIdpTokenUrl();

    try {
      AuthorizationCode code = new AuthorizationCode(codeParameter);

      URI callback = new URI(redirectUri);
      AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, callback);

      // The credentials to authenticate the client at the token endpoint
      ClientID clientID = new ClientID(clientId);
      Secret secret = new Secret(clientSecret);
      ClientAuthentication clientAuth = new ClientSecretBasic(clientID, secret);

      // The token endpoint
      URI tokenEndpoint = new URI(oauthRequestTokensUrl);

      // Make the token request
      return new TokenRequest(tokenEndpoint, clientAuth, codeGrant);
    }
    catch (Exception exception) {
      log.error(ERROR_BUILDING_TOKEN_REQUEST, exception);
      throw new AuthenticationException(ERROR_BUILDING_TOKEN_REQUEST, exception);
    }
  }

  private void addSecureCookie(final HttpServletResponse res, String id, String token) {
    Cookie cookie = new Cookie(id, token);
    cookie.setPath("/");
    cookie.setSecure(true);
    cookie.setHttpOnly(true);
    cookie.setMaxAge(COOKIE_MAX_AGE_IN_SECONDS);
    res.addCookie(cookie);
  }

  private String getCookie(final HttpServletRequest request, String authCookie) {
    Cookie[] cookies = request.getCookies();

    if (cookies == null) {
      return null;
    }

    return Stream.of(cookies)
        .filter(cookie -> authCookie.equalsIgnoreCase(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }
}
