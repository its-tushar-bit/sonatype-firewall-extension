/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * Servlet filter that handles requests with the /platform/ prefix. When running with applicationContextPath="/", this
 * filter: - Redirects /platform and /platform/ to /platform/assets/index.html - Rewrites /platform/* to /* so
 * downstream filters (Shiro) can match security rules - Maintains /platform in getRequestURL() for JavaScript BASE_URL
 * detection - Forwards /platform/assets/* requests to the assets servlet
 * <p>
 * This ensures that: 1. Shiro filter chains (configured for /rest/..., /api/...) work correctly 2. Anonymous access
 * rules (e.g., /rest/product/version) apply to /platform/rest/product/version 3. JAX-RS resources are accessible at
 * their configured paths 4. Frontend JavaScript can detect the correct BASE_URL from window.location
 */
@Named
public class PlatformContextFilter
    implements Filter
{
  // only supports these patterns!
  public static final String[] URL_PATTERNS = {"/platform", "/platform/*"};

  private static final Logger log = LoggerFactory.getLogger(PlatformContextFilter.class);

  private final String applicationContextPath;

  private ServletContext servletContext;

  @Inject
  public PlatformContextFilter(
      @Value("${server.servlet.context-path:/}") String applicationContextPath)
  {
    this.applicationContextPath = applicationContextPath;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.servletContext = filterConfig.getServletContext();
    log.debug("PlatformContextFilter initialized with applicationContextPath: {}", applicationContextPath);
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

    log.trace("PlatformContextFilter processing request: URI={}, ContextPath={}, ServletPath={}, PathInfo={}",
        request.getRequestURI(), request.getContextPath(), request.getServletPath(), request.getPathInfo());

    // As a special case, maintain Atrium redirect behavior.
    if ("/platform".equals(path) || "/platform/".equals(path)) {
      log.trace("Redirecting {} to /platform/assets/index.html", path);
      ((HttpServletResponse) res).sendRedirect("/platform/assets/index.html");
      return;
    }

    // Only rewrite paths that start with /platform/
    // This ensures Shiro filter chains (which expect /rest/..., /api/...) work correctly
    if (!path.startsWith("/platform/")) {
      log.trace("Path does not start with /platform/, passing through unchanged: {}", path);
      fc.doFilter(req, res);
      return;
    }

    // Remove the /platform prefix from all paths
    final String rewrittenPath = path.substring(9);

    log.trace("Rewriting path: original={}, rewritten={}", path, rewrittenPath);

    // Wrap the request to override path-related methods
    // CRITICAL: Jersey uses PathInfo for routing when servlet is mapped at /*
    // The wrapper must maintain /platform in getRequestURL() for JavaScript BASE_URL detection
    HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request)
    {
      @Override
      public String getRequestURI() {
        return rewrittenPath;
      }

      @Override
      public String getServletPath() {
        // Jersey servlet is mapped at /* so ServletPath should be empty
        // Assets servlet is mapped at /assets so return that for asset paths
        if (rewrittenPath.startsWith("/assets")) {
          return "/assets";
        }
        return "";
      }

      @Override
      public String getPathInfo() {
        // Jersey uses PathInfo for routing when servlet is mapped at /*
        // For assets, return the file path portion after /assets
        if (rewrittenPath.startsWith("/assets") && rewrittenPath.length() > 7) {
          return rewrittenPath.substring(7); // Everything after "/assets"
        }
        // For non-asset paths (API/REST), return the full rewritten path for Jersey routing
        return rewrittenPath;
      }

      @Override
      public String getContextPath() {
        return "";
      }

      @Override
      public StringBuffer getRequestURL() {
        // CRITICAL: Keep /platform in the URL so JavaScript can detect BASE_URL correctly
        StringBuffer url = new StringBuffer();
        String scheme = request.getScheme();
        int port = request.getServerPort();

        url.append(scheme).append("://").append(request.getServerName());
        if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
          url.append(':').append(port);
        }
        url.append("/platform").append(rewrittenPath);

        return url;
      }
    };

    // Asset requests need to be forwarded to avoid 404s from the main security filter chain.
    // The asset servlet is registered separately from that chain.
    if (rewrittenPath.startsWith("/assets")) {
      log.trace("Forwarding asset request to asset servlet: {}", rewrittenPath);
      ServletContext ctx = servletContext != null ? servletContext : request.getServletContext();

      // Try to get dispatcher by path
      RequestDispatcher dispatcher = ctx.getRequestDispatcher(rewrittenPath);

      if (dispatcher != null) {
        log.trace("Found RequestDispatcher for path: {}", rewrittenPath);
        // Forward with wrapped request to maintain proper paths
        dispatcher.forward(wrappedRequest, res);
        return;
      }
      else {
        log.error("No RequestDispatcher found for asset path: {}. Trying named servlet...", rewrittenPath);
        // Try getting dispatcher by servlet name as fallback
        dispatcher = ctx.getNamedDispatcher("assets");
        if (dispatcher != null) {
          log.trace("Found RequestDispatcher by servlet name 'assets'");
          dispatcher.forward(wrappedRequest, res);
          return;
        }
        log.trace("Could not find asset servlet. Assets may not be loaded yet.");
        ((HttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND, "Asset servlet not found");
        return;
      }
    }

    // Non-asset paths continue through the filter chain with wrapped request
    // This allows Shiro filters to see the rewritten path (e.g., /rest/product/version)
    // and correctly apply security rules
    log.trace("Continuing filter chain with rewritten path: {}", rewrittenPath);
    log.trace("Wrapped request will report URL as: {}", wrappedRequest.getRequestURL());
    fc.doFilter(wrappedRequest, res);

    // Log what happened after filter chain completes
    HttpServletResponse httpResponse = (HttpServletResponse) res;
    log.trace("Filter chain completed for: {}, response status: {}, committed: {}, location: {}",
        rewrittenPath, httpResponse.getStatus(), httpResponse.isCommitted(),
        httpResponse.getHeader("Location"));
  }
}
