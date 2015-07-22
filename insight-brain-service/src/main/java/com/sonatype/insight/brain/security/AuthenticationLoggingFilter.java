/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.security.Principal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;

import org.apache.http.auth.BasicUserPrincipal;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Request;

@Named
public class AuthenticationLoggingFilter
    implements Filter
{
  public static String URL_PATTERN = "/*";

  private final CurrentUser currentUser;

  @Inject
  public AuthenticationLoggingFilter(final CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    // no op
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException
  {
    if (!currentUser.isAnonymous()) {
      setUsernameForRequestLogging(request, currentUser.getUsername());
    }
    try (MDCUsernameScope mdcUsernameScope = currentUser.isAnonymous() ? MDCUsernameScope.forAnonymous()
        : MDCUsernameScope.forUser(currentUser.getUsername())) {
      chain.doFilter(request, response);
    }
  }

  private void setUsernameForRequestLogging(ServletRequest request, final String username) {
    if (!(request instanceof ServletRequestWrapper)) {
      throw new IllegalStateException("Expected request instanceof " + ServletRequestWrapper.class.getName()
          + " but was " + request.getClass().getName());
    }
    ServletRequestWrapper servletRequestWrapper = (ServletRequestWrapper) request;

    ServletRequest wrappedRequest = servletRequestWrapper.getRequest();
    if (!(wrappedRequest instanceof Request)) {
      throw new IllegalStateException("Expected wrappedRequest instanceof " + Request.class.getName() + " but was "
          + wrappedRequest.getClass().getName());
    }
    Request jettyRequest = (Request) servletRequestWrapper.getRequest();

    // The request logging logs the username from the Authentication instance retrieved from the jetty request.
    Principal userPrincipal = new BasicUserPrincipal(username);
    jettyRequest.setAuthentication(new UserAuthentication("for_request_logging", new DefaultUserIdentity(
        null /* subject */, userPrincipal, null /* roles */)));
  }

  @Override
  public void destroy() {
    // no op
  }
}
