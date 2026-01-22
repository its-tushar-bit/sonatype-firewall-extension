/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import io.dropwizard.core.setup.Environment;

/**
 * A servlet filter that puts web clients into slow motion by delaying most requests. This is useful to reveal timing
 * issues in functional tests that mistake asynchronous operations as immediate.
 */
class SlowMoFilter
    implements Filter
{
  public static void configure(Environment env) {
    boolean enable = Boolean.getBoolean("slowmo.enable");
    Long delay = Long.getLong("slowmo.delay");
    if (enable && delay != null && delay > 0) {
      SlowMoFilter filter = new SlowMoFilter(delay);
      env.servlets().addFilter("RestSlowMoFilter", filter)
          .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/rest/*");
      env.servlets().addFilter("HtmlSlowMoFilter", filter)
          .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "*.html");
    }
  }

  private final long delay;

  private SlowMoFilter(long delay) {
    this.delay = delay;
  }

  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
      ServletException
  {
    try {
      Thread.sleep(delay);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
