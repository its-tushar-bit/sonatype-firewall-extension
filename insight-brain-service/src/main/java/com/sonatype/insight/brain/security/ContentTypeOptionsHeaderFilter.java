/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;

import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Adds the X-Content-Type-Options: nosniff header to the response, which forces the browser to respect the
 * Content-Type on the response, protecting against some types of security vulnerabilities
 *
 * @since 1.57
 */
@Named
public class ContentTypeOptionsHeaderFilter
    implements Filter
{
  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain filterChain) throws IOException, ServletException
  {
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    httpResponse.setHeader("X-Content-Type-Options", "nosniff");

    filterChain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
