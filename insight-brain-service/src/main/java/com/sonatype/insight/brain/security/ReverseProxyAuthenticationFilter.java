/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.service.Configuration;

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
@Named
@Singleton
public class ReverseProxyAuthenticationFilter
    extends AuthenticatingFilter
{
  private static final Logger log = LoggerFactory.getLogger(ReverseProxyAuthenticationFilter.class);

  public static final String NO_SESSION_CREATION = "noSessionCreation";

  private static final String USERNAME_HEADER_NAME_ATTRIBUTE = "USERNAME_HEADER_NAME_ATTRIBUTE";

  private final Configuration configuration;

  @Inject
  public ReverseProxyAuthenticationFilter(Configuration configuration) {
    this.configuration = configuration;
  }

  private String getUsername(ServletRequest request) {
    String usernameHeaderName = (String) request.getAttribute(USERNAME_HEADER_NAME_ATTRIBUTE);
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    return httpRequest.getHeader(usernameHeaderName);
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
  protected boolean onLoginFailure(
      final AuthenticationToken token,
      final AuthenticationException e,
      final ServletRequest request,
      final ServletResponse response)
  {
    LoginErrorResponseHandler.sendError((HttpServletResponse) response, e);
    return super.onLoginFailure(token, e, request, response);
  }

  @Override
  protected boolean onAccessDenied(
      ServletRequest request,
      ServletResponse response,
      Object mappedValue) throws Exception
  {
    // not yet authenticated (e.g. via session) but if the remote-user header is present, time for a login
    if (isLoginRequest(request, response)) {
      // there's no dedicated login prompt/request in case of SSO so allow any request to start the session
      boolean allowSessionCreation =
          mappedValue == null || Stream.of((String[]) mappedValue).noneMatch(NO_SESSION_CREATION::equals);
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

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean isEnabled(ServletRequest request, ServletResponse response) throws ServletException, IOException {
    ReverseProxyAuthenticationConfiguration reverseProxyAuthenticationConfiguration =
        configuration.getReverseProxyAuthenticationConfiguration();
    if (reverseProxyAuthenticationConfiguration == null) {
      return false;
    }
    if (!reverseProxyAuthenticationConfiguration.isEnabled()) {
      return false;
    }
    request.setAttribute(USERNAME_HEADER_NAME_ATTRIBUTE, reverseProxyAuthenticationConfiguration.getUsernameHeader());
    return true;
  }

  @Override
  public boolean isEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setEnabled(boolean enabled) {
    throw new UnsupportedOperationException();
  }
}
