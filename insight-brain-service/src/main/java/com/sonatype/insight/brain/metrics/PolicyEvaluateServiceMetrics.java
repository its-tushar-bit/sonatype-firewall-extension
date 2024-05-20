/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.LongTaskTimer.Sample;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Named
@Singleton
public class PolicyEvaluateServiceMetrics
{
  private static final String KIND_TAG = "kind";

  private static final String METRIC_KIND = "policy_evaluation";

  public static final String THREAD_UTILIZATION = "thread_utilization";

  private static final String EVALUATION_LONG_TASK_TIME = "evaluation.in_flight";

  private static final String EVALUATION_COMPLETED_NAME = "evaluation_completed";

  private static final String DURATION_NAME = "duration";

  private final MeterRegistry meterRegistry;

  private final AtomicInteger activeEvaluationTask = new AtomicInteger();

  @Inject
  public PolicyEvaluateServiceMetrics(@Nullable final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public ExecutorService registerAndGetTimedPolicyEvaluationExecutor(final ExecutorService executor) {
    if (meterRegistry == null) {
      return executor;
    }

    return ExecutorServiceMetrics.monitor(meterRegistry,
        executor,
        "PolicyEvaluateService",
        Tags.of(KIND_TAG, METRIC_KIND));
  }

  public void registerGaugePolicyEvaluationThreadUtilization(final int corePoolSize) {
    if (meterRegistry == null) {
      return;
    }

    meterRegistry.gauge(THREAD_UTILIZATION,
        Tags.of(KIND_TAG, METRIC_KIND),
        this,
        object -> object.activeEvaluationTask.get() * 100 / corePoolSize);
  }

  public Sample emitStartPolicyEvaluation() {
    if (meterRegistry == null) {
      return null;
    }

    activeEvaluationTask.incrementAndGet();

    return LongTaskTimer.builder(EVALUATION_LONG_TASK_TIME)
        .tag(KIND_TAG, METRIC_KIND)
        .register(meterRegistry)
        .start();
  }

  public void emitEndPolicyEvaluation(final Sample sample) {
    if (meterRegistry == null) {
      return;
    }

    activeEvaluationTask.decrementAndGet();

    long analysisDurationNanos = sample.stop();
    long analysisDurationMs = Duration.ofNanos(analysisDurationNanos).toMillis();
    meterRegistry.timer(EVALUATION_COMPLETED_NAME + "." + DURATION_NAME, Tags.of(KIND_TAG, METRIC_KIND))
        .record(Duration.ofMillis(analysisDurationMs));

    meterRegistry.counter(EVALUATION_COMPLETED_NAME, Tags.of(KIND_TAG, METRIC_KIND)).increment();
  }
}
