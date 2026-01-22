/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.util.EnumSet;
import java.util.function.BiConsumer;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import io.dropwizard.core.setup.Environment;

/**
 * Implementation of a servlet filter for TestInsightBrainService to intercept REST calls.
 */
public class RestRequestFilter
    implements Filter
{
  private final BiConsumer<ServletRequest, ServletResponse> handler;

  public static void configure(final Environment env, BiConsumer<ServletRequest, ServletResponse> handler) {
    RestRequestFilter filter = new RestRequestFilter(handler);
    env.servlets().addFilter("RestRequestFilter", filter)
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/rest/*");
  }

  private RestRequestFilter(BiConsumer<ServletRequest, ServletResponse> handler) {
    this.handler = handler;
  }

  @Override
  public void init(final FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(
      final ServletRequest servletRequest,
      final ServletResponse servletResponse,
      final FilterChain filterChain)
      throws IOException, ServletException
  {
    if (handler != null) {
      handler.accept(servletRequest, servletResponse);
    }
    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void destroy() {
  }
}
