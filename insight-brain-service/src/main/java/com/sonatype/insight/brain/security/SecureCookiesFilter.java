/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collection;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.shiro.web.servlet.AdviceFilter;

import static com.google.common.net.HttpHeaders.SET_COOKIE;

/**
 * @since 1.16.0
 */
@Named
@Singleton
public class SecureCookiesFilter
    extends AdviceFilter
{
  public static final String SECURE_FLAGS = "; Secure; SameSite=None";

  /**
   * Perform filtering as a post handler
   *
   * Performing the filtering after the other filters have finished ensures that the secure flags are always set
   * even if the filter chain is stopped.
   */
  @Override
  protected void postHandle(final ServletRequest request, final ServletResponse response) throws Exception {
    // session cookies are expected to be set already by another filter
    filterCookies(request, response);
  }

  /**
   * Perform filtering on cookie headers.
   *
   * If the request is secure, examine response for cookies and adds the Secure flag if not already present in the
   * cookie value.
   */
  private void filterCookies(final ServletRequest request, final ServletResponse response) {
    if (request.isSecure() && response instanceof HttpServletResponse) {
      secureCookies((HttpServletResponse) response);
    }
  }

  private void secureCookies(final HttpServletResponse response) {
    final Collection<String> cookies = response.getHeaders(SET_COOKIE);
    boolean mustAdd = false;
    for (final String cookie : cookies) {
      final String cookieVal = cookie.lastIndexOf(SECURE_FLAGS) == -1 ? cookie + SECURE_FLAGS : cookie;
      if (mustAdd) {
        response.addHeader(SET_COOKIE, cookieVal);
      }
      else {
        response.setHeader(SET_COOKIE, cookieVal);
      }
      mustAdd = true;
    }
  }
}
