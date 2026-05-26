/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.HstsConfig;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter that adds the HTTP Strict-Transport-Security (HSTS) header to HTTPS responses.
 * <p>
 * This replaces the Dropwizard {@code web.hsts} configuration that was previously handled by
 * {@code io.dropwizard.web.conf.HstsHeaderFactory}. The filter reads its configuration from
 * {@link InsightConfig#getHstsConfig()} and only emits the header on secure (HTTPS) responses.
 */
@Named
public class HstsHeaderFilter
    implements Filter
{
  private static final Logger log = LoggerFactory.getLogger(HstsHeaderFilter.class);

  private static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

  private final HstsConfig hstsConfig;

  @Inject
  public HstsHeaderFilter(InsightConfig insightConfig) {
    this.hstsConfig = insightConfig.getHstsConfig();
    if (hstsConfig.isEnabled()) {
      log.info("HSTS enabled: {}", hstsConfig.buildHeaderValue());
    }
    else {
      log.info("HSTS disabled by configuration");
    }
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    if (hstsConfig.isEnabled() && response instanceof HttpServletResponse httpResponse) {
      if (request instanceof HttpServletRequest httpRequest && httpRequest.isSecure()) {
        httpResponse.setHeader(STRICT_TRANSPORT_SECURITY, hstsConfig.buildHeaderValue());
      }
    }
    chain.doFilter(request, response);
  }
}
