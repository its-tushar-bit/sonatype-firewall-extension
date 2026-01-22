/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.apache.shiro.authc.AuthenticationException;

/**
 * @since 1.20.0
 */
public class LoginErrorResponseHandler
{
  private static final ErrorResponseGenerator errorResponseGenerator = new ErrorResponseGenerator();

  private LoginErrorResponseHandler() {
  }

  public static void sendError(final HttpServletResponse httpResponse, final AuthenticationException e) {
    sendError(httpResponse, errorResponseGenerator.mapExceptionAndLog(e));
  }

  public static void sendError(final HttpServletResponse httpResponse, final ErrorResponse errorResponse) {
    // Note: In cases where a committed response.statusCode == 401, we knowingly deviate from this spec:
    // https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.2, and do not include the "WWW-Authenticate"
    // header. See: org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter.sendChallenge() for such a header.
    httpResponse.setStatus(errorResponse.getStatusCode());
    httpResponse.setContentType(ErrorResponse.CONTENT_TYPE);
    try (PrintWriter writer = httpResponse.getWriter()) {
      writer.print(errorResponse.getMessageBody());
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
