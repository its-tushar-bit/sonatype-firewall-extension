/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Servlet filter that captures the application's base URL from the incoming request.
 */
@Named
public class BaseUrlFilter
    implements Filter
{
  private final BaseUrl baseUrl;

  @Inject
  public BaseUrlFilter(BaseUrl baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException
  {
    try {
      baseUrl.capture((HttpServletRequest) request);
      chain.doFilter(request, response);
    }
    finally {
      baseUrl.release();
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // noop
  }

  @Override
  public void destroy() {
    // noop
  }
}
