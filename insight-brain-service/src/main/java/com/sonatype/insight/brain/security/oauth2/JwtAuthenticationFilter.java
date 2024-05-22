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
    String authorizationHeaderContent = getAuthzHeader(request);

    // Check if it is a bearer token
    if (isLoginAttempt(authorizationHeaderContent)) {
      log.debug("Attempting to execute login with auth header");
      final String[] principalsAndCredentials = getPrincipalsAndCredentials(authorizationHeaderContent, request);
      return new ShiroJsonWebToken(principalsAndCredentials[0]);
    }

    // Create an empty authentication token since there is no Authorization header.
    return createBearerToken("", request);
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
