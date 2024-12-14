/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.BearerHttpAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class JwtAuthenticationFilter
    extends BearerHttpAuthenticationFilter
{
  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class.getName());

  public static final String ID_TOKEN_COOKIE = "IQ-ID-TOKEN";

  public static final String LOGIN_REQUEST = "rest/user/session";

  private final OidcTokenService oidcTokenService;

  @Inject
  public JwtAuthenticationFilter(OidcTokenService oidcTokenService) {
    this.oidcTokenService = oidcTokenService;
  }

  @Override
  protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
    if (!SystemConfigurationPropertyFeature.OAUTH2_ENABLED.isEnabled()) {
      // When OAuth2 feature is disabled we just ignore the token and continue to the next filter
      // to handle authentication
      return false;
    }

    if (isLoginRequestWithCookie(request)) {
      log.debug("Found cookie with the ID Token on a login request, Handling Authentication with OAuth2 Realm");
      return true;
    }

    return super.isLoginAttempt(request, response);
  }

  private boolean isLoginRequestWithCookie(final ServletRequest request) {
    String oidcToken = getOidcToken(request);
    String path = ((HttpServletRequest) request).getPathInfo();
    return StringUtils.isNotBlank(oidcToken) && path != null && path.contains(LOGIN_REQUEST);
  }

  @Override
  protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
    String oidcToken = pullOidcToken(request);

    if (StringUtils.isNotBlank(oidcToken)) {
      log.debug("Attempting to execute login with ID Token");
      return new ShiroJsonWebToken(oidcToken, true);
    }

    String bearerToken = getTokenFromAuthzHeader(request);
    if (StringUtils.isNotBlank(bearerToken)) {
      log.debug("Attempting to execute login with auth header");
      return new ShiroJsonWebToken(bearerToken);
    }

    // Create an empty authentication token since there is no Authorization header or session token.
    return createBearerToken("", request);
  }

  private String getOidcToken(ServletRequest request) {
    String tokenId = getAuthCookie(request, ID_TOKEN_COOKIE);
    return oidcTokenService.getOidcToken(tokenId);
  }

  private String pullOidcToken(ServletRequest request) {
    String tokenId = getAuthCookie(request, ID_TOKEN_COOKIE);
    return oidcTokenService.pullOidcToken(tokenId);
  }

  private String getTokenFromAuthzHeader(ServletRequest request) {
    String authzHeader = getAuthzHeader(request);

    // Check if it is a bearer token
    if (authzHeader != null && isLoginAttempt(authzHeader)) {
      final String[] principalsAndCredentials = getPrincipalsAndCredentials(authzHeader, request);
      return principalsAndCredentials[0];
    }

    return null;
  }

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
    if ((isLoginAttempt(request, response)) && !executeLogin(request, response)) {
      sendChallenge(request, response);
      return false;
    }

    // if this wasn't a failed login attempt, continue filter chain, allowing other filters to do login
    return true;
  }

  private String getAuthCookie(final ServletRequest request, String authCookie) {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest req = (HttpServletRequest) request;
      Cookie[] cookies = req.getCookies();

      if (cookies == null) {
        return null;
      }

      return Stream.of(cookies)
          .filter(cookie -> authCookie.equalsIgnoreCase(cookie.getName()))
          .map(Cookie::getValue)
          .findFirst()
          .orElse(null);
    }

    return null;
  }
}
