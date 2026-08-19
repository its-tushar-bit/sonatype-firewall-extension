/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.landing.LandingService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.security.LoginErrorResponseHandler;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
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
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class OidcLoginFilter
    extends AuthenticationFilter
{
  private static final Logger log = LoggerFactory.getLogger(OidcLoginFilter.class.getName());

  public static final String OAUTH_CALLBACK = "oidc/callback";

  public static final String OAUTH_LOGIN = "oidc/login";

  public static final String INDEX_HTML = "assets/index.html";

  // Matches LandingService.getGuideDestination() path — keep in sync if Guide asset path changes
  public static final String GUIDE_INDEX_HTML = "assets/guide/index.html";

  public static final String SESSION_ATTR_SSO_ORIGIN = "sso.origin";

  public static final String ORIGIN_GUIDE = LandingService.ORIGIN_GUIDE;

  public static final String OIDC_CONFIGURATION_INVALID =
      "There is no OIDC configuration to trigger the login";

  public static final String ERROR_BUILDING_AUTHORIZATION_REQUEST = "Error building the authorization request";

  public static final String ERROR_GETTING_TOKENS = "Error getting the OIDC tokens from IDP";

  public static final String ERROR_BUILDING_TOKEN_REQUEST = "Error building the token request";

  public static final String[] OIDC_SCOPES = {"openid", "profile", "email"};

  public static final String ERROR_AUTHORIZING_REQUEST = "Error authorizing request: %s";

  private final PasswordHandler passwordHandler;

  private final OidcConfigurationDAO oidcConfigurationDAO;

  private final BaseUrl baseUrl;

  private final Configuration configuration;

  private final TenantReference<String> oidcClientSecretRef = new TenantReference<>();

  @Inject
  public OidcLoginFilter(
      final BaseUrl baseUrl,
      final PasswordHandler passwordHandler,
      final OidcConfigurationDAO oidcConfigurationDAO,
      final Configuration configuration)
  {
    this.passwordHandler = passwordHandler;
    this.oidcConfigurationDAO = oidcConfigurationDAO;
    this.baseUrl = baseUrl;
    this.configuration = configuration;
  }

  @Override
  public boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
    if (!SystemConfigurationPropertyFeature.OAUTH2_ENABLED.isEnabled()) {
      // When OAuth2 feature is disabled we just ignore the token and continue to the next filter
      // to handle authentication
      return true;
    }

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    String path = req.getPathInfo();
    if (path == null) {
      path = req.getRequestURI();
    }
    String hash = req.getParameter("hash");
    String encodedHash = StringUtils.isNotBlank(hash) ? URLEncoder.encode(hash, StandardCharsets.UTF_8) : hash;

    log.info("Calling OAuth endpoint {}", path);

    try {
      OidcConfiguration oidcConfiguration = oidcConfigurationDAO.get();

      if (oidcConfiguration == null) {
        throw new AuthenticationException(OIDC_CONFIGURATION_INVALID);
      }

      // Handles the login request by sending an authentication request
      if (path.contains(OAUTH_LOGIN)) {
        String origin = req.getParameter("origin");
        // Store origin in session since the callback comes from the IdP on a separate request.
        // SAML doesn't need this because its destination is computed before the IdP redirect.
        if (ORIGIN_GUIDE.equals(origin)) {
          req.getSession(true).setAttribute(SESSION_ATTR_SSO_ORIGIN, ORIGIN_GUIDE);
        }
        else {
          HttpSession existingSession = req.getSession(false);
          if (existingSession != null) {
            existingSession.removeAttribute(SESSION_ATTR_SSO_ORIGIN);
          }
        }
        String callbackUrl = buildRedirectUrl(OAUTH_CALLBACK, encodedHash, true);
        sendAuthorizationRequest(res, oidcConfiguration, callbackUrl);
      }

      // Handles the callback request from the IDP to get the Access and ID tokens
      if (path.contains(OAUTH_CALLBACK)) {
        String callbackUrl = buildRedirectUrl(OAUTH_CALLBACK, encodedHash, true);
        String targetPage = resolveTargetPage(req);
        String redirectUrl = buildRedirectUrl(targetPage, hash, false);
        handleCallbackAndCompleteAuthentication(req, res, oidcConfiguration, callbackUrl, redirectUrl);
      }
    }
    catch (AuthenticationException e) {
      ErrorResponse errorResponse = new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
      LoginErrorResponseHandler.sendError(res, errorResponse);
    }

    // Stop the filter chain, no other filter can handle the request
    return false;
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

  private String resolveTargetPage(HttpServletRequest req) {
    HttpSession session = req.getSession(false);
    if (session != null) {
      String origin = (String) session.getAttribute(SESSION_ATTR_SSO_ORIGIN);
      // Remove the attribute before authentication completes. If auth fails and the user retries,
      // the Guide LoginPage re-appends origin=guide, so the session attribute gets re-set.
      session.removeAttribute(SESSION_ATTR_SSO_ORIGIN);
      if (ORIGIN_GUIDE.equals(origin)) {
        return GUIDE_INDEX_HTML;
      }
    }
    return INDEX_HTML;
  }

  private void sendAuthorizationRequest(
      final HttpServletResponse res,
      final OidcConfiguration oidcConfiguration,
      final String callbackUrl)
  {
    AuthenticationRequest authorizeUrlRequest = buildAuthenticationRequest(oidcConfiguration, callbackUrl);

    String authorizeUrl = authorizeUrlRequest.toURI().toString();

    // Redirect to the initial URL
    res.setHeader("Location", authorizeUrl);
    res.setStatus(HttpServletResponse.SC_FOUND);
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

  private void handleCallbackAndCompleteAuthentication(
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

    String idToken;
    try {
      TokenResponse tokenResponse = sendTokenRequestWithProxy(tokenRequest);

      if (!tokenResponse.indicatesSuccess()) {
        ErrorObject errorObject = tokenResponse.toErrorResponse().getErrorObject();
        String error = errorObject.getDescription() != null ? errorObject.getDescription() : errorObject.getCode();
        log.error("Token endpoint returned error: code={}, description={}", errorObject.getCode(),
            errorObject.getDescription());
        throw new AuthenticationException(error);
      }

      OIDCTokenResponse successResponse = (OIDCTokenResponse) tokenResponse.toSuccessResponse();
      idToken = successResponse.getOIDCTokens().getIDTokenString();
    }
    catch (Exception e) {
      log.error(ERROR_GETTING_TOKENS, e);
      throw new AuthenticationException(ERROR_GETTING_TOKENS, e);
    }

    completeAuthentication(idToken);

    // Redirect to the initial URL
    res.setHeader("Location", redirectUrl);
    res.setStatus(HttpServletResponse.SC_FOUND);
  }

  private TokenRequest buildTokenRequest(
      OidcConfiguration oidcConfiguration,
      String redirectUri,
      String codeParameter)
  {
    String clientId = oidcConfiguration.getClientId();
    String clientSecret = getOidcClientSecret(oidcConfiguration);
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

  private TokenResponse sendTokenRequestWithProxy(final TokenRequest tokenRequest) throws Exception {
    HTTPRequest nimbusRequest = tokenRequest.toHTTPRequest();
    ProxyServerConfiguration proxyConfig = configuration.getProxyServerConfiguration();
    if (proxyConfig != null && proxyConfig.getHostname() != null) {
      String tokenHost = nimbusRequest.getURL().getHost();
      if (!isHostExcluded(tokenHost, proxyConfig.getExcludeHostsList())) {
        if (proxyConfig.getUsername() != null) {
          log.warn("Proxy authentication credentials are not supported for OIDC token requests; "
              + "proxy username is configured but will be ignored.");
        }
        InetSocketAddress proxyAddress = new InetSocketAddress(proxyConfig.getHostname(), proxyConfig.getPort());
        nimbusRequest.setProxy(new Proxy(Proxy.Type.HTTP, proxyAddress));
      }
    }
    return OIDCTokenResponseParser.parse(nimbusRequest.send());
  }

  static boolean isHostExcluded(final String hostname, final List<String> excludePatterns) {
    if (excludePatterns == null || excludePatterns.isEmpty()) {
      return false;
    }
    for (String pattern : excludePatterns) {
      String regex = "\\Q" + pattern.replace("*", "\\E.*\\Q") + "\\E";
      if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(hostname).matches()) {
        return true;
      }
    }
    return false;
  }

  private String getOidcClientSecret(OidcConfiguration oidcConfiguration) {
    String clientSecret = oidcClientSecretRef.get();

    if (clientSecret == null) {
      clientSecret = oidcConfiguration.getClientSecret();

      if (passwordHandler.isEncrypted(clientSecret)) {
        clientSecret = passwordHandler.decryptPassword(clientSecret);
      }
      else {
        log.warn("Client secret is not encrypted, please re-sync the tenant metadata to encrypt it.");
      }

      oidcClientSecretRef.set(clientSecret);
    }

    return clientSecret;
  }

  public void clearCachedOidcClientSecret() {
    oidcClientSecretRef.remove();
  }

  private void completeAuthentication(final String idToken) {
    Subject subject = SecurityUtils.getSubject();
    subject.login(new ShiroJsonWebToken(idToken));
  }

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
    throw new IllegalStateException();
  }
}
