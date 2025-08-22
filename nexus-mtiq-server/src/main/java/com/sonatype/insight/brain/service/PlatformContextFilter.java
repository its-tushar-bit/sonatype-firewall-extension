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
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import io.dropwizard.core.server.DefaultServerFactory;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServletPathMapping;
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

    final Request request = (Request) req;
    final HttpURI uri = request.getHttpURI();
    // As a special case, maintain Atrium redirect behavior.
    if ("/platform".equals(uri.getPath()) || "/platform/".equals(uri.getPath())) {
      ((HttpServletResponse) res).sendRedirect("/platform/assets/index.html");
      return;
    }

    // Remove the /platform prefix from the path in its various forms
    final String rewrittenPath = uri.getPath().substring(9);
    request.setContext(request.getContext(), rewrittenPath);
    request.setHttpURI(HttpURI.build(uri, rewrittenPath, uri.getParam(), uri.getQuery()));

    // Asset requests will get 404 from this filter chain.  Instead, forward to the asset servlet
    if (rewrittenPath.startsWith("/assets")) {
      final PathSpec pathSpec = PathSpec.from("/assets/*");
      request.setServletPathMapping(new ServletPathMapping(
          pathSpec,
          "assets",
          rewrittenPath,
          pathSpec.matched(rewrittenPath)
      ));
      RequestDispatcher requestDispatcher = request.getRequestDispatcher(rewrittenPath);
      requestDispatcher.forward(request, res);
      return;
    }

    // Otherwise, modify remaining request attributes and continue the filter chain
    final ServletPathMapping currentMapping = request.getServletPathMapping();
    if (currentMapping != null) {
      final PathSpec pathSpec = PathSpec.from(currentMapping.getPattern());
      request.setServletPathMapping(new ServletPathMapping(
          pathSpec,
          currentMapping.getServletName(),
          rewrittenPath,
          pathSpec.matched(rewrittenPath)
      ));
    }

    // Continue the filter chain with the modified request
    fc.doFilter(request, res);
  }
}
