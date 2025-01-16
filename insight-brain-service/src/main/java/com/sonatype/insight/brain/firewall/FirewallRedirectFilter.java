/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.firewall;

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

import com.sonatype.insight.brain.api.PublicApiPaths;

@Named
public class FirewallRedirectFilter
    implements Filter
{
  private static final String DEPRECATED_FIREWALL_RESOURCE_PATH = "api/v2/firewall";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // noop
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException
  {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String requestUri = httpRequest.getRequestURI();
    if (requestUri.contains("/" + DEPRECATED_FIREWALL_RESOURCE_PATH)) {
      requestUri = requestUri.replace(DEPRECATED_FIREWALL_RESOURCE_PATH, PublicApiPaths.FIREWALL_RESOURCE_PATH);
      httpResponse.sendRedirect(requestUri);
    }
    else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {
    // noop
  }
}
