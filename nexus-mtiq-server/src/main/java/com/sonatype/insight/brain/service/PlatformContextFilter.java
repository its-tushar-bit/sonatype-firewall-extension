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
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import io.dropwizard.core.server.DefaultServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter that handles requests with the /platform/ prefix.
 * When running with applicationContextPath="/", this filter:
 * - Redirects /platform and /platform/ to index.html (consistent with Atrium)
 * - Forwards /platform/* to /* (transparent to API clients)
 */
@Named
public class PlatformContextFilter
    implements Filter
{
  // only supports these patterns!
  public static final String[] URL_PATTERNS = {"/platform", "/platform/*"};

  private static final Logger log = LoggerFactory.getLogger(PlatformContextFilter.class);

  private final String applicationContextPath;

  @Inject
  public PlatformContextFilter(MultiTenantInsightConfig insightConfiguration) {
    // Get the actual context path from Dropwizard config
    if (insightConfiguration.getServerFactory() instanceof DefaultServerFactory serverFactory) {
      applicationContextPath = serverFactory.getApplicationContextPath();
    }
    else {
      log.warn(
          "Unrecognized server factory type: {}. Assuming default application context path '/'",
          insightConfiguration.getServerFactory().getClass().getName()
      );
      applicationContextPath = "/";
    }
  }

  protected PlatformContextFilter(String applicationContextPath) {
    this.applicationContextPath = applicationContextPath;
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain fc) throws IOException, ServletException {
    // Only enable when running on root context path
    if (!"/".equals(applicationContextPath)) {
      fc.doFilter(req, res);
      return;
    }

    final HttpServletRequest request = (HttpServletRequest) req;
    final String path = request.getRequestURI();

    // As a special case, maintain Atrium redirect behavior.
    if ("/platform".equals(path) || "/platform/".equals(path)) {
      ((HttpServletResponse) res).sendRedirect("/platform/assets/index.html");
      return;
    }

    // Remove the /platform prefix from the path
    final String rewrittenPath = path.substring(9);

    // Asset requests should be forwarded to the asset servlet
    if (rewrittenPath.startsWith("/assets")) {
      RequestDispatcher requestDispatcher = request.getRequestDispatcher(rewrittenPath);
      requestDispatcher.forward(request, res);
      return;
    }

    // For other paths, wrap the request to override path-related methods
    HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
      @Override
      public String getRequestURI() {
        return rewrittenPath;
      }

      @Override
      public String getServletPath() {
        return rewrittenPath;
      }

      @Override
      public String getPathInfo() {
        return null;
      }
    };

    // Continue the filter chain with the wrapped request
    fc.doFilter(wrappedRequest, res);
  }
}
