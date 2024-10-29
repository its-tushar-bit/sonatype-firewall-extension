/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.util.stream.Stream;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

  @Override
  protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
    if (!SystemConfigurationPropertyFeature.OAUTH2_ENABLED.isEnabled()) {
      // When OAuth2 feature is disabled we just ignore the token and continue to the next filter
      // to handle authentication
      return false;
    }

    if (StringUtils.isNotBlank(getAuthCookie(request, ID_TOKEN_COOKIE))) {
      return true;
    }

    return super.isLoginAttempt(request, response);
  }

  @Override
  protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
    String idToken = getAuthCookie(request, ID_TOKEN_COOKIE);

    if (StringUtils.isNotBlank(idToken)) {
      log.debug("Attempting to execute login with ID Token");
      // Remove tokens from session
      removeCookie(request, response, ID_TOKEN_COOKIE);
      return new ShiroJsonWebToken(idToken, true);
    }

    String bearerToken = getTokenFromAuthzHeader(request);
    if (StringUtils.isNotBlank(bearerToken)) {
      log.debug("Attempting to execute login with auth header");
      return new ShiroJsonWebToken(bearerToken);
    }

    // Create an empty authentication token since there is no Authorization header or session token.
    return createBearerToken("", request);
  }

  private String getTokenFromAuthzHeader(ServletRequest request) {
    String authzHeader = getAuthzHeader(request);

    // Check if it is a bearer token
    if (isLoginAttempt(authzHeader)) {
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

  private void removeCookie(final ServletRequest request, final ServletResponse response, String authCookie) {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletRequest req = (HttpServletRequest) request;
      HttpServletResponse res = (HttpServletResponse) response;

      for (Cookie cookie : req.getCookies()) {
        if (authCookie.equals(cookie.getName())) {
          cookie.setMaxAge(0);
          res.addCookie(cookie);
        }
      }
    }
  }
}
