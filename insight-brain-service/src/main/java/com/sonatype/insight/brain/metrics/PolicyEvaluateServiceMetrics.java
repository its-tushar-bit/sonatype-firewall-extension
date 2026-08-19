/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics;

import java.time.Duration;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.LongTaskTimer.Sample;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

@Named
@Singleton
public class PolicyEvaluateServiceMetrics
{
  private static final String KIND_TAG = "kind";

  private static final String METRIC_KIND = "policy_evaluation";

  private static final String EVALUATION_LONG_TASK_TIME = "evaluation.in_flight";

  private static final String EVALUATION_COMPLETED_NAME = "evaluation_completed";

  private static final String DURATION_NAME = "duration";

  private final MeterRegistry meterRegistry;

  @Inject
  public PolicyEvaluateServiceMetrics(@Nullable final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public Sample emitStartPolicyEvaluation() {
    if (meterRegistry == null) {
      return null;
    }

    return LongTaskTimer.builder(EVALUATION_LONG_TASK_TIME)
        .tag(KIND_TAG, METRIC_KIND)
        .register(meterRegistry)
        .start();
  }

  public void emitEndPolicyEvaluation(final Sample sample) {
    if (meterRegistry == null || sample == null) {
      return;
    }

    long analysisDurationNanos = sample.stop();
    long analysisDurationMs = Duration.ofNanos(analysisDurationNanos).toMillis();
    meterRegistry.timer(EVALUATION_COMPLETED_NAME + "." + DURATION_NAME, Tags.of(KIND_TAG, METRIC_KIND))
        .record(Duration.ofMillis(analysisDurationMs));

    meterRegistry.counter(EVALUATION_COMPLETED_NAME, Tags.of(KIND_TAG, METRIC_KIND)).increment();
  }
}
