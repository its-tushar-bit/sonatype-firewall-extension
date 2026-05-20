/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.consumption.ConsumptionSourceClassifier.Source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter that populates {@link ConsumptionContext} per request and clears it in finally.
 *
 * @since 1.204
 */
@Named
@Singleton
public class ConsumptionContextFilter
    implements Filter
{
  private static final Logger log = LoggerFactory.getLogger(ConsumptionContextFilter.class);

  private final ProductLicense productLicense;

  @Inject
  public ConsumptionContextFilter(final ProductLicense productLicense) {
    this.productLicense = productLicense;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(
      ServletRequest request,
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException
  {
    try {
      try {
        populateContext((HttpServletRequest) request);
      }
      catch (RuntimeException e) {
        log.warn("Failed to populate consumption context; proceeding without", e);
      }
      chain.doFilter(request, response);
    }
    finally {
      ConsumptionContext.clear();
    }
  }

  @Override
  public void destroy() {
  }

  private void populateContext(HttpServletRequest request) {
    String orgId = resolveOrgId();
    String tier = resolveTier();
    Source source = ConsumptionSourceClassifier.classify(
        HdsClient.getClientUserAgent(request), request.getRequestURI());

    if (orgId != null && tier != null) {
      boolean directApiRequest = source == Source.API;
      ConsumptionContext.set(orgId, tier, source.token(), directApiRequest);
      log.trace("Set ConsumptionContext: orgId={}, tier={}, source={}, directApiRequest={}",
          orgId, tier, source, directApiRequest);
    }
    else {
      log.trace("Skipping ConsumptionContext: orgId={}, tier={}, source={}", orgId, tier, source);
    }
  }

  private String resolveOrgId() {
    return ConsumptionOrgIdResolver.resolveForRequest();
  }

  private String resolveTier() {
    return ConsumptionTierResolver.resolveTier(productLicense);
  }
}
