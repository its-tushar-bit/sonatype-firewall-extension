/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.firewall;

import java.io.IOException;

import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.api.PublicApiPaths;

@Named
public class FirewallRedirectFilter
    implements Filter
{
  private static final String DEPRECATED_MALWARE_DEFENSE_RESOURCE_PATH = "api/v2/malware-defense";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // noop
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException
  {
    HttpServletRequest httpRequest = (HttpServletRequest) request;

    String pathInfo = httpRequest.getPathInfo();
    if (pathInfo != null && pathInfo.contains("/" + DEPRECATED_MALWARE_DEFENSE_RESOURCE_PATH) &&
        !pathInfo.endsWith("/evaluate") && !pathInfo.endsWith("/metrics")) {
      pathInfo = pathInfo.replace(DEPRECATED_MALWARE_DEFENSE_RESOURCE_PATH, PublicApiPaths.FIREWALL_RESOURCE_PATH);
      request.getRequestDispatcher(pathInfo).forward(request, response);
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
