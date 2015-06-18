/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;

/**
 * Specialized BASIC auth filter that ignores session cookies and instead requires a valid authc header on each request.
 * To be used for the public REST API (stateless).
 */
public class BasicHttpAuthenticationMandatoryFilter
    extends BasicHttpAuthenticationFilter
{
  public static final String INVALID_AUTHENTICATION_MESSAGE = "Invalid authentication.";
  public static final String SESSION_COOKIE_MESSAGE = "This REST API is meant for system to system integration and can't be accessed with a web browser.";

  @Override
  protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
    return false;
  }

  /**
   * This is the method called by the parent class when the access is denied.
   * In our case we don't want to send back a challenge, but only to set the HTTP response status and an error message.
   */
  @Override
  protected boolean sendChallenge(ServletRequest request, ServletResponse response) {
    setErrorInResponse(request, response);
    return false;
  }

  private void setErrorInResponse(ServletRequest request, ServletResponse response) {
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    httpResponse.setContentType("text/plain;charset=UTF-8");
    if (getSubject(request, response).isAuthenticated()) {
      setErrorMessage(httpResponse, SESSION_COOKIE_MESSAGE);
    }
    else {
      setErrorMessage(httpResponse, INVALID_AUTHENTICATION_MESSAGE);
    }
  }

  private void setErrorMessage(HttpServletResponse httpResponse, String message) {
    try {
      PrintWriter writer = httpResponse.getWriter();
      writer.print(message);
      writer.close();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
