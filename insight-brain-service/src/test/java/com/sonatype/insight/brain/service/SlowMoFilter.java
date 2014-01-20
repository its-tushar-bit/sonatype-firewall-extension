/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.yammer.dropwizard.config.Environment;

/**
 * A servlet filter that puts REST clients into slow motion by delaying every request. This is useful to reveal timing
 * issues in functional tests that mistake asynchronous operations as immediate.
 */
class SlowMoFilter
    implements Filter
{
  public static void configure(Environment env) {
    boolean enable = Boolean.getBoolean("slowmo.enable");
    Long delay = Long.getLong("slowmo.delay");
    if (enable && delay != null && delay > 0) {
      env.addFilter(new SlowMoFilter(delay), "/rest/*");
    }
  }

  private final long delay;

  private SlowMoFilter(long delay) {
    this.delay = delay;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
      ServletException
  {
    try {
      Thread.sleep(delay);
    }
    catch (InterruptedException e) {
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
