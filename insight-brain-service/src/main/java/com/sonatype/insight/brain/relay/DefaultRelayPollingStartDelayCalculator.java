/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Default single-tenant implementation: returns the configured initial delay verbatim.
 */
@Named
@Singleton
public class DefaultRelayPollingStartDelayCalculator
    implements RelayPollingStartDelayCalculator
{
  @Override
  public int computeInitialDelaySeconds(int pollIntervalSeconds, int defaultInitialDelaySeconds) {
    return defaultInitialDelaySeconds;
  }
}
