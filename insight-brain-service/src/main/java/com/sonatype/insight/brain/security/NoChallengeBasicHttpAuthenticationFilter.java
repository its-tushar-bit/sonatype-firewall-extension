/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

/**
 * Specialized BASIC auth filter that does not send the challenge response.
 * To be used for the public REST API (stateless).
 * 
 * @since 1.26
 */
public class NoChallengeBasicHttpAuthenticationFilter
    extends UserFriendlyBasicHttpAuthenticationFilter
{
  /**
   * This is the method called by the parent class when the access is denied.
   * In our case we don't want to send back a challenge, but only to set the HTTP response status and an error message.
   */
  @Override
  protected boolean sendChallenge(ServletRequest request, ServletResponse response) {
    // in case of a failed login attempt, onLoginFailure() already sent the error response
    if (response.isCommitted()) {
      return false;
    }

    LoginErrorResponseHandler
        .sendError((HttpServletResponse) response, new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED,
            ErrorResponseGenerator.MSG_LOGIN_FAILURE_DEFAULT));
    return false;
  }
}
