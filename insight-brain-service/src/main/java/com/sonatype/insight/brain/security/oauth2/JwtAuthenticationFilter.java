/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.security.LoginErrorResponseHandler;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

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

  @Override
  protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
    if (!SystemConfigurationPropertyFeature.OAUTH2_ENABLED.isEnabled()) {
      // When OAuth2 feature is disabled we just ignore the token and continue to the next filter
      // to handle authentication
      return false;
    }

    return super.isLoginAttempt(request, response);
  }

  @Override
  protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
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
    if (authzHeader != null && isLoginAttempt(authzHeader)) {
      final String[] principalsAndCredentials = getPrincipalsAndCredentials(authzHeader, request);
      return principalsAndCredentials[0];
    }

    return null;
  }

  @Override
  protected boolean sendChallenge(final ServletRequest request, final ServletResponse response) {
    // in case of a failed login attempt, onLoginFailure() already sent the error response
    if (response.isCommitted()) {
      return false;
    }
    // for anonymous requests, send the auth challenge
    // NOTE: We specifically avoid super.sendChallenge() as we do not want the WWW-Authenticate header set which would
    // otherwise trigger browser-native login prompts instead of our own login UI
    LoginErrorResponseHandler.sendError((HttpServletResponse) response,
        new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, ErrorResponseGenerator.MSG_MISSING_CREDENTIALS));
    return false;
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
}
