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
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.version.VersionService;

import com.google.common.net.HttpHeaders;

/**
 * Servlet filter that adds the "Server" header to all responses.
 */
@Named
public class ServerHeaderFilter
    implements Filter
{
  public static final String[] URL_PATTERNS = {"/*"};

  protected String headerValue;

  @Inject
  public ServerHeaderFilter(VersionService versionService) {
    headerValue = "NexusIQ/" + versionService.getVersion();
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    ((HttpServletResponse) response).setHeader(HttpHeaders.SERVER, headerValue);
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
