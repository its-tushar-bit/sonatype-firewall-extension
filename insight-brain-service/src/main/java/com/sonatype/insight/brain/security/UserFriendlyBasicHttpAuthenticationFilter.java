/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;

/**
 * Specialized BASIC auth filter that better conveys why authentication failed.
 *
 * @since 1.20.0
 */
@Named
@Singleton
class UserFriendlyBasicHttpAuthenticationFilter
    extends BasicHttpAuthenticationFilter
{
  @Override
  protected boolean onLoginFailure(
      final AuthenticationToken token,
      final AuthenticationException e,
      final ServletRequest request,
      final ServletResponse response)
  {
    LoginErrorResponseHandler.sendError((HttpServletResponse) response, e);
    return super.onLoginFailure(token, e, request, response);
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
    if (isLoginAttempt(request, response) && !executeLogin(request, response)) {
      sendChallenge(request, response);
      return false;
    }
    // if this wasn't a failed login attempt, continue filter chain, allowing other filters to do login
    return true;
  }
}
