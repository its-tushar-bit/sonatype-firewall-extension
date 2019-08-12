/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.io.IOException;

import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

/**
 * Adds cache control headers to the response to ensure web browsers always fetch a fresh copy of the index page (assets
 * referenced by the index page use versioned URIs to bust caching when needed).
 * 
 * @since 1.11
 */
@Named
public class IndexCacheControlFilter
    implements Filter
{
  public static final String URL_PATTERN = "/assets/index.html";

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
    HttpServletResponse resp = (HttpServletResponse) response;
    resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate");
    chain.doFilter(request, response);
  }
}
