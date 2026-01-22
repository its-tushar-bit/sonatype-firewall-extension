/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;

/**
 * Servlet filter that catches throwables during request processing and converts them
 * to appropriate HTTP error responses using JaxRsExceptionMapper.
 *
 * Migrated from Jetty Handler to Servlet Filter for Jetty 12 compatibility.
 * In Jetty 12, servlet-layer exception handling should use Filters instead of Handlers.
 */
@Named
public class ThrowableHandler
    implements Filter
{
  private final JaxRsExceptionMapper jaxRsExceptionMapper;

  @Inject
  public ThrowableHandler(final JaxRsExceptionMapper jaxRsExceptionMapper) {
    this.jaxRsExceptionMapper = jaxRsExceptionMapper;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // No initialization needed
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
      throws IOException, ServletException
  {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    try {
      chain.doFilter(request, response);
    }
    catch (Throwable t) {
      if (!response.isCommitted()) {
        // Note if "t" is an Error, JaxRsExceptionMapper will try to find/log it and then exit if configured to do so
        Response errorResponse = jaxRsExceptionMapper.toResponse(t);
        response.setStatus(errorResponse.getStatus());
        response.setContentType(errorResponse.getMediaType().toString());
        try (PrintWriter printWriter = response.getWriter()) {
          printWriter.print(errorResponse.getEntity());
        }
      }
    }
  }

  @Override
  public void destroy() {
    // No cleanup needed
  }
}
