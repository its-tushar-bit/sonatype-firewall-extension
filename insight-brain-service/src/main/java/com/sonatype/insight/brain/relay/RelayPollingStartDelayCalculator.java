/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

/**
 * Computes the initial delay (in seconds) to use when starting a tenant's relay polling loop.
 * Single-tenant deployments use the configured default; multi-tenant deployments stagger
 * tenants across the polling interval to avoid stampeding the relay on cold start.
 */
public interface RelayPollingStartDelayCalculator
{
  int computeInitialDelaySeconds(int pollIntervalSeconds, int defaultInitialDelaySeconds);
}
