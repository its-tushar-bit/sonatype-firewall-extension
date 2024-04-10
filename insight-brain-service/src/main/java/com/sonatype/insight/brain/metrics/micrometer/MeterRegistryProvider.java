/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics.micrometer;

import com.sonatype.insight.brain.service.InsightConfig;

import io.micrometer.core.instrument.MeterRegistry;

public interface MeterRegistryProvider
{
  MeterRegistry provideMeterRegistry(InsightConfig config);
}
