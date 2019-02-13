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

import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.service.ReverseProxyAuthenticationConfig;

import org.apache.shiro.authc.AuthenticationException;
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

  private boolean allowSessionCreation = true;

  public ReverseProxyAuthenticationFilter(ReverseProxyAuthenticationConfig reverseProxyAuthentication) {
    setEnabled(reverseProxyAuthentication.isEnabled());
    this.usernameHeader = reverseProxyAuthentication.getUsernameHeader();
  }

  public ReverseProxyAuthenticationFilter(ReverseProxyAuthenticationConfig reverseProxyAuthentication,
                                          boolean allowSessionCreation)
  {
    this(reverseProxyAuthentication);
    this.allowSessionCreation = allowSessionCreation;
  }

  private String getUsername(ServletRequest request) {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    return httpRequest.getHeader(usernameHeader);
  }

  @Override
  protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
    String username = getUsername(request);
    UserPrincipal userPrincipal = (UserPrincipal) getSubject(request, response).getPrincipal();
    if (username != null && userPrincipal != null && !username.equals(userPrincipal.getUsername())) {
      log.info("Detected mismatch between user specified by reverse proxy authentication ({})"
          + " and user specified by session cookie ({})", username, userPrincipal.getUsername());
      return false;
    }

    return super.isAccessAllowed(request, response, mappedValue);
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
  protected boolean onLoginFailure(final AuthenticationToken token,
                                   final AuthenticationException e,
                                   final ServletRequest request,
                                   final ServletResponse response)
  {
    LoginErrorResponseHandler.sendError((HttpServletResponse) response, e);
    return super.onLoginFailure(token, e, request, response);
  }

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
    // not yet authenticated (e.g. via session) but if the remote-user header is present, time for a login
    if (isLoginRequest(request, response)) {
      // there's no dedicated login prompt/request in case of SSO so allow any request to start the session
      if (allowSessionCreation) {
        request.removeAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED);
      }

      if (executeLogin(request, response)) {
        return true;
      }

      log.warn("Failed to validate existence of remotely authenticated user '{}'", getUsername(request));
      return false;
    }

    // let the next filter in the chain decide on the request's fate
    return true;
  }
}
