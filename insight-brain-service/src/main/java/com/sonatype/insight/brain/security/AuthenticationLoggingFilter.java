/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.security.Principal;

import com.sonatype.insight.brain.audit.AuditData;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponse;
import org.eclipse.jetty.ee11.servlet.ServletApiRequest;
import org.eclipse.jetty.server.Request;

@Named
public class AuthenticationLoggingFilter
    implements Filter
{
  public static final String URL_PATTERN = "/*";

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
  public void doFilter(
      final ServletRequest request,
      final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException
  {
    if (!currentUser.isAnonymous()) {
      String username = currentUser.getUsername();
      AuditData.get().setUsername(username);
      setUsernameForRequestLogging(request, username);
    }
    try (MDCUsernameScope mdcUsernameScope = currentUser.isAnonymous()
        ? MDCUsernameScope.forAnonymous()
        : MDCUsernameScope.forUser(currentUser.getUsername()))
    {
      chain.doFilter(request, response);
    }
  }

  /**
   * Sets the username on the Jetty request's AuthenticationState so that it appears in the request log (%user token).
   * <p>
   * Note: The request may be wrapped by Shiro (ShiroHttpServletRequest), so we need to unwrap it to access
   * the underlying ServletApiRequest.
   */
  private void setUsernameForRequestLogging(ServletRequest servletRequest, final String username) {
    // Unwrap Shiro request wrapper to get to the underlying Jetty ServletApiRequest
    // Limit unwrapping depth to prevent infinite loops (stops at Object class in inheritance tree)
    final int MAX_UNWRAP_DEPTH = 10;
    ServletRequest unwrapped = servletRequest;
    int depth = 0;

    while (unwrapped != null && !(unwrapped instanceof ServletApiRequest) && depth < MAX_UNWRAP_DEPTH) {
      if (unwrapped instanceof ServletRequestWrapper wrapper) {
        unwrapped = wrapper.getRequest();
        depth++;
      }
      else {
        // Not a wrapper we can unwrap, stop here
        return;
      }
    }

    if (unwrapped instanceof ServletApiRequest servletApiRequest) {
      Request jettyRequest = servletApiRequest.getRequest();
      Request.setAuthenticationState(jettyRequest, new Request.AuthenticationState()
      {
        @Override
        public Principal getUserPrincipal() {
          return () -> username;
        }
      });
    }
  }

  @Override
  public void destroy() {
    // no op
  }
}
