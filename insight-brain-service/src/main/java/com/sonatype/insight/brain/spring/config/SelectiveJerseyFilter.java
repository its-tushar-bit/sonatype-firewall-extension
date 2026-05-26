/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Routes requests to Jersey or Spring MVC based on path.
 *
 * <p>
 * Requests whose top-level path segment matches a known Spring MVC resource (static assets,
 * actuator, etc.) bypass Jersey and go directly to Spring MVC. All other requests - including
 * unknown paths - go through Jersey first, which preserves the original Dropwizard behavior
 * of returning plain-text 404 responses via {@code JaxRsExceptionMapper}.
 */
public final class SelectiveJerseyFilter
    implements Filter
{
  private static final Set<String> SPRING_MVC_PREFIXES = Set.of("assets", "static", "actuator", "ping");

  private final ServletContainer delegate;

  private final JerseyRequestMatcher matcher;

  public SelectiveJerseyFilter(ServletContainer delegate, JerseyRequestMatcher matcher) {
    this.delegate = delegate;
    this.matcher = matcher;
  }

  ServletContainer getDelegate() {
    return delegate;
  }

  JerseyRequestMatcher getMatcher() {
    return matcher;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    delegate.init(filterConfig);
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    if (request instanceof HttpServletRequest && isSpringMvcPath((HttpServletRequest) request)) {
      chain.doFilter(request, response);
      return;
    }

    delegate.doFilter(request, response, chain);
  }

  private static boolean isSpringMvcPath(HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
      path = path.substring(contextPath.length());
    }
    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    String topSegment = path.contains("/") ? path.substring(0, path.indexOf('/')) : path;
    if ("favicon.ico".equals(topSegment)) {
      return true;
    }
    return SPRING_MVC_PREFIXES.contains(topSegment);
  }

  @Override
  public void destroy() {
    delegate.destroy();
  }
}
