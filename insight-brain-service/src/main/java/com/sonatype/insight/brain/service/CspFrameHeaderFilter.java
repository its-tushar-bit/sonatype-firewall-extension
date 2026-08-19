/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter that adds the Content-Security-Policy frame-ancestors header to the responses.
 */
@Named
public class CspFrameHeaderFilter
    implements Filter
{
  protected static final String[] URL_PATTERN = {"/*"};

  private final Configuration configuration;

  @Inject
  public CspFrameHeaderFilter(final Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    List<String> allowList = configuration.getFrameAncestorsAllowList();
    if (allowList != null && !allowList.isEmpty()) {
      httpServletResponse.addHeader("Content-Security-Policy",
          "frame-ancestors " + String.join(" ", allowList) + ";");
    }
    chain.doFilter(request, response);
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // noop
  }

  @Override
  public void destroy() {
    // noop
  }
}
