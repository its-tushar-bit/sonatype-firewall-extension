/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.web.filter.authc.BearerHttpAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class JwtAuthenticationFilter
    extends BearerHttpAuthenticationFilter
{
  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class.getName());

  public static final String ACCESS_TOKEN_PARAM = "accessToken";

  public static final String ID_TOKEN_PARAM = "idToken";

  @Override
  protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
    if (!SystemConfigurationPropertyFeature.OAUTH2_ENABLED.isEnabled()) {
      // When OAuth2 feature is disabled we just ignore the token and continue to the next filter
      // to handle authentication
      return false;
    }

    if (StringUtils.isNotBlank(getTokenFromSession(ID_TOKEN_PARAM))) {
      return true;
    }

    return super.isLoginAttempt(request, response);
  }

  @Override
  protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
    String idToken = getTokenFromSession(ID_TOKEN_PARAM);

    if (StringUtils.isNotBlank(idToken)) {
      log.debug("Attempting to execute login with ID Token");
      // Remove tokens from session
      removeTokenFromSession(ID_TOKEN_PARAM);
      removeTokenFromSession(ACCESS_TOKEN_PARAM);
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

  private String getTokenFromSession(String token) {
    Session session = getSession();

    if (session == null) {
      return null;
    }

    return (String) session.getAttribute(token);
  }

  private void removeTokenFromSession(String token) {
    Session session = getSession();

    if (session == null) {
      return;
    }

    session.removeAttribute(token);
  }

  private Session getSession() {
    return SecurityUtils.getSubject().getSession(false);
  }
}
