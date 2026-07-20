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

  /**
   * Request attribute carrying the authenticated username for request-log rendering. The logback-access request-log
   * path cannot read the Jetty {@link Request.AuthenticationState} - its
   * {@code ch.qos.logback.access.jetty.RequestWrapper.getRemoteUser()}/{@code getUserPrincipal()} are stubbed to
   * return {@code null}, so {@code %user} renders "-". It instead reads this request attribute via
   * {@code %reqAttribute{...}}; {@code RequestLoggingConfiguration} rewrites the access-path {@code %user}/{@code %u}
   * token to that. The classic Jetty {@code CustomRequestLog} path continues to read the AuthenticationState
   * directly. CLM-41689.
   */
  public static final String REQUEST_LOG_REMOTE_USER_ATTRIBUTE = "com.sonatype.insight.requestlog.remoteUser";

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
   * Makes the authenticated username available to the request log. The two request-log render paths need different
   * mechanisms:
   * <ul>
   * <li>The classic Jetty {@code CustomRequestLog} path ({@code %u}) reads the username from the request's
   * {@link Request.AuthenticationState}, so we set a bare one carrying the principal.</li>
   * <li>The logback-access path cannot use that state (its {@code RequestWrapper.getRemoteUser()} is stubbed to
   * return {@code null}, so {@code %user} renders "-"). It instead reads the
   * {@link #REQUEST_LOG_REMOTE_USER_ATTRIBUTE} request attribute via {@code %reqAttribute{...}}.</li>
   * </ul>
   * <p>
   * Note: The request may be wrapped by Shiro (ShiroHttpServletRequest). The attribute is set on the (possibly
   * wrapped) servlet request, which delegates down to the underlying Jetty core request the logback-access
   * RequestWrapper reads; for the AuthenticationState we unwrap to the underlying ServletApiRequest.
   */
  private void setUsernameForRequestLogging(ServletRequest servletRequest, final String username) {
    // logback-access request-log path: expose the username as a request attribute it can render via
    // %reqAttribute{...}, since it cannot read the Jetty AuthenticationState. Setting it on the (wrapped) servlet
    // request delegates down to the Jetty core request.
    servletRequest.setAttribute(REQUEST_LOG_REMOTE_USER_ATTRIBUTE, username);

    // Classic CustomRequestLog path: unwrap Shiro's request wrapper to reach the underlying Jetty ServletApiRequest
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
