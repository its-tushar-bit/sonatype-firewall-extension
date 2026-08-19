/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.enterprise.reporting.EnterpriseReportingService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.product.license.ProductLicense;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.59
 */
@Named
public class CspHeaderFilter
    implements Filter
{
  private static final Logger log = LoggerFactory.getLogger(CspHeaderFilter.class);

  public static final String URL_PATTERN = "/assets/*";

  private final Configuration configuration;

  private final EnterpriseReportingService enterpriseReportingService;

  private final ProductLicense productLicense;

  @Inject
  public CspHeaderFilter(
      Configuration configuration,
      EnterpriseReportingService enterpriseReportingService,
      ProductLicense productLicense)
  {
    this.configuration = configuration;
    this.enterpriseReportingService = enterpriseReportingService;
    this.productLicense = productLicense;
  }

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain filterChain) throws IOException, ServletException
  {
    if (configuration.isCspEnabled()) {
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      httpResponse.setHeader("Content-Security-Policy",
          "default-src 'self'; " + getFrameSrc() + "style-src 'self' 'unsafe-inline'; img-src 'self' data:");

      // This header guards against server-reflected XSS attacks (not that our architecture is really at risk
      // of having any). It is redundant with the CSP header but applicable for browsers that don't fully support CSP
      httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
    }

    filterChain.doFilter(request, response);
  }

  // visible for testing
  String getFrameSrc() {
    String lookerHost = null;
    if (productLicense.isValid()) {
      try {
        lookerHost = getUrlHost(enterpriseReportingService.getEnterpriseReportingConfigDTOBaseUrl());
      }
      catch (Exception ex) {
        log.warn("Could not resolve a looker host: " + ex.getMessage());
      }
    }
    return lookerHost != null ? String.format("frame-src 'self' %s; ", lookerHost) : "";
  }

  private String getUrlHost(String urlString) {
    try {
      URL url = new URL(urlString);
      return url.getHost();
    }
    catch (MalformedURLException e) {
      // in an unlikely case of a wrong config we don't want to break the filter
      return null;
    }
  }

  @Override
  public void destroy() {
  }
}
