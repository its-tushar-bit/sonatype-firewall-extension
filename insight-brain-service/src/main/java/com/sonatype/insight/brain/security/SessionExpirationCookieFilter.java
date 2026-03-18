/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.web.servlet.OncePerRequestFilter;

/**
 * Filter that adds a cookie to all responses indicating the expiration time of the current session. This is
 * done as a cookie so that the client's view of it gets updated no matter how the request happens (be it AJAX or
 * an <img> tag or something else).
 *
 * @since 1.27.0
 */
@Named
@Singleton
public class SessionExpirationCookieFilter
    extends OncePerRequestFilter
{
  public static final String EXPIRATION_COOKIE_NAME = "IQ-SESSION-EXPIRATION-TIMESTAMP";

  @Override
  protected void doFilterInternal(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    Session session = SecurityUtils.getSubject().getSession(false);

    // if there is no session the cookie isn't added
    if (session != null) {
      long timeout = session.getTimeout();
      long lastAccess = session.getLastAccessTime().getTime();
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      Cookie cookie = new Cookie(EXPIRATION_COOKIE_NAME, Long.toString(timeout + lastAccess));
      cookie.setPath("/");

      httpResponse.addCookie(cookie);
    }

    chain.doFilter(request, response);
  }
}
