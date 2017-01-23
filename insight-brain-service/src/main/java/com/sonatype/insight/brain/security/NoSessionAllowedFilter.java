/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.shiro.web.filter.PathMatchingFilter;

/**
 * Filter that prevents the use of session cookies. To be used for the public REST API (stateless).
 *
 * @since 1.25.0
 */
public class NoSessionAllowedFilter
    extends PathMatchingFilter
{
  public static final String SESSION_COOKIE_MESSAGE = "This REST API is meant for system to system integration and " +
      "can't be accessed with a web browser.";

  @Override
  protected boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
    HttpServletRequest req = (HttpServletRequest) request;
    if (req.getSession(false) != null) {
      LoginErrorResponseHandler
          .sendError((HttpServletResponse) response,
              new ErrorResponse(HttpServletResponse.SC_UNAUTHORIZED, SESSION_COOKIE_MESSAGE));
      return false;
    }
    return true;
  }
}
