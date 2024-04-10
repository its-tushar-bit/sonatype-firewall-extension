/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics.datadog;

import javax.annotation.Nullable;

import com.sonatype.insight.brain.metrics.micrometer.MeterRegistryProvider;
import com.sonatype.insight.brain.service.InsightConfig;

import io.micrometer.core.instrument.MeterRegistry;

public abstract class StatsdMetrics
{
  protected static final String KIND_TAG = "kind";

  private DatadogTags datadogTags;

  private MeterRegistry meterRegistry;

  protected StatsdMetrics(InsightConfig insightConfig, @Nullable MeterRegistryProvider meterRegistryProvider) {
    if (meterRegistryProvider != null) {
      this.meterRegistry = meterRegistryProvider.provideMeterRegistry(insightConfig);
    }
  }

  public DatadogTags getDatadogTags() {
    return datadogTags;
  }

  public void setDatadogTags(DatadogTags datadogTags) {
    this.datadogTags = datadogTags;
  }

  public MeterRegistry getMeterRegistry() {
    return meterRegistry;
  }

  public final void publishStatsdMetrics() {
    if (meterRegistry != null) {
      addDataDogTags();
      sendStatsdMetrics();
    }
  }

  public abstract void addDataDogTags();

  public abstract void sendStatsdMetrics();
}
