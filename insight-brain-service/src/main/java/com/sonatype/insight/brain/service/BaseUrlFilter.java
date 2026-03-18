/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

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
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
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
