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

import com.sonatype.insight.brain.service.ReverseProxyAuthenticationConfig;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter to support integration with a 3rd-party SSO frontend that shields the server from outside access, handling
 * authentication and forwards the validated username via a request header. To be used in front of another
 * authentication filter.
 */
public class ReverseProxyAuthenticationFilter
    extends AuthenticatingFilter
{
  private static final Logger log = LoggerFactory.getLogger(ReverseProxyAuthenticationFilter.class);

  private final String usernameHeader;

  public ReverseProxyAuthenticationFilter(ReverseProxyAuthenticationConfig reverseProxyAuthentication) {
    setEnabled(reverseProxyAuthentication.isEnabled());
    this.usernameHeader = reverseProxyAuthentication.getUsernameHeader();
  }

  private String getUsername(ServletRequest request) {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    return httpRequest.getHeader(usernameHeader);
  }

  @Override
  protected boolean isLoginRequest(ServletRequest request, ServletResponse response) {
    String header = getUsername(request);
    return header != null;
  }

  @Override
  protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
    String username = getUsername(request);
    return new ReverseProxyAuthenticationToken(username);
  }

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
    // not yet authenticated (e.g. via session) but if the remote-user header is present, time for a login
    if (isLoginRequest(request, response)) {
      // there's no dedicated login prompt/request in case of SSO so allow any request to start the session
      request.removeAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED);

      if (executeLogin(request, response)) {
        return true;
      }

      log.warn("Failed to validate existence of remotely authenticated user '{}'", getUsername(request));
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      httpResponse.setContentType("text/plain;charset=UTF-8");
      httpResponse.getWriter().print("Invalid username");
      return false;
    }

    // let the next filter in the chain decide on the request's fate
    return true;
  }
}
