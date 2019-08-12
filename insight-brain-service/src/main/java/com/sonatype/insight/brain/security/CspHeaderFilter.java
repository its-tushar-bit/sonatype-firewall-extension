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
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.InsightConfig;

/**
 * @since 1.59
 */
@Named
public class CspHeaderFilter
    implements Filter
{
  public static final String URL_PATTERN = "/assets/*";

  private final boolean cspEnabled;

  @Inject
  public CspHeaderFilter(InsightConfig config) {
    this.cspEnabled = config.isCspEnabled();
  }

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException
  {
    if (cspEnabled) {
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      httpResponse.setHeader("Content-Security-Policy",
          "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:");

      // This header guards against server-reflected XSS attacks (not that our architecture is really at risk
      // of having any).  It is redundant with the CSP header but applicable for browsers that don't fully support CSP
      httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
    }

    filterChain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
