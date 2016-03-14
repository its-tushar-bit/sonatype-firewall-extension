/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;

/**
 * Specialized BASIC auth filter that better conveys why authentication failed.
 * 
 * @since 1.20.0
 */
class UserFriendlyBasicHttpAuthenticationFilter
    extends BasicHttpAuthenticationFilter
{
  @Override
  protected boolean onLoginFailure(final AuthenticationToken token,
                                   final AuthenticationException e,
                                   final ServletRequest request,
                                   final ServletResponse response)
  {
    LoginErrorHandler.sendError((HttpServletResponse) response, e);
    return super.onLoginFailure(token, e, request, response);
  }

  @Override
  protected boolean sendChallenge(final ServletRequest request, final ServletResponse response) {
    // in case of a failed login attempt, onLoginFailure() already sent the error response
    if (response.isCommitted()) {
      return false;
    }
    // for anonymous requests, send the ordinary auth challenge
    return super.sendChallenge(request, response);
  }
}
