/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.io.IOException;
import java.util.Locale;

import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * Lets web browsers cache versioned static assets long-term. Only assets requested with a version query parameter
 * are cached: the index pages append a build token to their bundle URIs, and that token changes on each release, so
 * the URI is safe to treat as immutable. Assets requested without a version token (fonts and images referenced by
 * fixed URLs) are left untouched so a changed-but-same-named file is never served stale. The index pages themselves
 * keep the no-store behavior applied by {@link IndexCacheControlFilter}.
 */
@Named
public class StaticAssetCacheControlFilter
    implements Filter
{
  public static final String URL_PATTERN = "/assets/*";

  static final String CACHE_CONTROL_VALUE = "public, max-age=31536000, immutable";

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    String queryString = req.getQueryString();
    if (queryString != null && !queryString.isEmpty()
        && !req.getRequestURI().toLowerCase(Locale.ROOT).endsWith(".html"))
    {
      ((HttpServletResponse) response).setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE);
    }
    chain.doFilter(request, response);
  }
}
