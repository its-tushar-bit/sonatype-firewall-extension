/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.FrameOptionsConfig;
import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Optionally adds an X-Frame-Options header when enabled via web.frame-options in config.yml.
 */
@Named
public class FrameOptionsHeaderFilter
    implements Filter
{
  static final String HEADER_NAME = "X-Frame-Options";

  static final String HEADER_VALUE = "DENY";

  private final FrameOptionsConfig frameOptionsConfig;

  @Inject
  public FrameOptionsHeaderFilter(final InsightConfig insightConfig) {
    this.frameOptionsConfig = insightConfig.getFrameOptionsConfig();
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // noop
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain filterChain) throws IOException, ServletException
  {
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    if (frameOptionsConfig.isEnabled()) {
      httpResponse.setHeader(HEADER_NAME, frameOptionsConfig.buildHeaderValue());
    }
    filterChain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // noop
  }
}
