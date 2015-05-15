/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

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
    try (MDCUsernameScope mdcUsernameScope = currentUser.isAnonymous() ? MDCUsernameScope.forAnonymous()
        : MDCUsernameScope.forUser(currentUser.getUsername())) {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {
    // no op
  }
}
