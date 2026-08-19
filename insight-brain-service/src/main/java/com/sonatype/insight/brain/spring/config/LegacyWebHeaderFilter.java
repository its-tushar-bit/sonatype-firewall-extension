/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Emits headers parsed from legacy Dropwizard {@code web:} configuration.
 */
public class LegacyWebHeaderFilter
    implements Filter
{
  static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";

  static final String CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only";

  static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

  static final String X_XSS_PROTECTION = "X-XSS-Protection";

  private final Map<String, String> headers;

  public LegacyWebHeaderFilter(final DropwizardWebSettings webSettings) {
    this.headers = webSettings.getHeaders();
  }

  @Override
  public void doFilter(
      final ServletRequest request,
      final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException
  {
    if (response instanceof HttpServletResponse httpResponse) {
      headers.forEach(httpResponse::setHeader);
    }
    chain.doFilter(request, response);
  }
}
