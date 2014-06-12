/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;

import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Blocks the HTTP TRACE method to prevent cross-site tracing attacks. Note that the MS-specific TRACK method is already
 * not supported by the web container.
 * 
 * @since 1.11
 */
@Named
public class TraceMethodBlockFilter
    implements Filter
{
  public static String URL_PATTERN = "/*";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
      ServletException
  {
    if ("TRACE".equalsIgnoreCase(((HttpServletRequest) request).getMethod())) {
      ((HttpServletResponse) response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    else {
      chain.doFilter(request, response);
    }
  }
}
