/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.shiro.web.filter.authc.AuthenticationFilter;

/**
 * Filter to be used at the end of the authentication chain to block access if the subject was not authenticated by any
 * of the preceding filters.
 */
@Named
@Singleton
class MissingAuthenticationFilter
    extends AuthenticationFilter
{
  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response) {
    LoginErrorResponseHandler.sendError((HttpServletResponse) response,
        new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, ErrorResponseGenerator.MSG_MISSING_CREDENTIALS));
    return false;
  }
}
