/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.Configuration;

/**
 * Central check for whether IQ Server is configured to be embedded in third-party iframes.
 *
 * When {@code frameAncestorsAllowlist} is non-empty, IQ emits a CSP {@code frame-ancestors}
 * header permitting those origins to host IQ in an iframe (used by Jenkins plugin, IDE plugins,
 * embeddable scan reports, etc.). Cross-site iframe requests in that deployment require
 * {@code SameSite=None} cookies to flow — this helper is consulted by the cookie-emitting filters
 * ({@link SecureCookiesFilter}, {@link AntiCsrfFilter}, {@link SessionExpirationCookieFilter})
 * so the condition is defined in one place.
 */
@Named
@Singleton
public class FrameEmbeddingDetector
{
  private final Configuration configuration;

  @Inject
  public FrameEmbeddingDetector(Configuration configuration) {
    this.configuration = configuration;
  }

  public boolean isFrameEmbeddingEnabled() {
    List<String> allowList = configuration.getFrameAncestorsAllowList();
    return allowList != null && !allowList.isEmpty();
  }
}
