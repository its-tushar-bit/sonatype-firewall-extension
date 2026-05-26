/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

public final class MtiqConfigSupport
{
  private MtiqConfigSupport() {
    // utility class
  }

  public static MultiTenantInsightConfig requireMultiTenantInsightConfig(InsightConfig insightConfig, String consumer) {
    if (insightConfig instanceof MultiTenantInsightConfig multiTenantInsightConfig) {
      return multiTenantInsightConfig;
    }

    String actualConfigType = insightConfig == null ? "null" : insightConfig.getClass().getName();
    throw new IllegalStateException(
        String.format(
            "%s requires %s for MTIQ startup, but got %s. Ensure config.class=%s for MTIQ bootstrap paths.",
            consumer,
            MultiTenantInsightConfig.class.getName(),
            actualConfigType,
            MultiTenantInsightConfig.class.getName()));
  }
}
