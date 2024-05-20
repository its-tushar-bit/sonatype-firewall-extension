/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer.Sample;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.internal.TimedExecutorService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.api.AbstractLongAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyEvaluateServiceMetricsTest
{
  private PolicyEvaluateServiceMetrics policyEvaluateServiceMetrics;

  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @Before
  public void setUp() {
    policyEvaluateServiceMetrics = new PolicyEvaluateServiceMetrics(meterRegistry);
  }

  @Test
  public void testRegisterAndGetTimedPolicyEvaluationExecutor() {
    ExecutorService executorService = policyEvaluateServiceMetrics
        .registerAndGetTimedPolicyEvaluationExecutor(Executors.newFixedThreadPool(2));

    assertThat(executorService).isInstanceOf(TimedExecutorService.class);
  }

  @Test
  public void testRegisterAndGetTimedPolicyEvaluationExecutor_MeterRegistryIsNull() {
    policyEvaluateServiceMetrics = new PolicyEvaluateServiceMetrics(null);
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    ExecutorService result = policyEvaluateServiceMetrics.registerAndGetTimedPolicyEvaluationExecutor(executorService);

    assertThat(result).isEqualTo(executorService);
    assertThat(executorService).isNotInstanceOf(TimedExecutorService.class);
  }

  @Test
  public void testRegisterGaugePolicyEvaluationThreadUtilization() {
    Gauge gauge = meterRegistry.find("thread_utilization").tag("kind", "policy_evaluation").gauge();
    assertThat(gauge).isNull();

    policyEvaluateServiceMetrics.registerGaugePolicyEvaluationThreadUtilization(4);

    gauge = meterRegistry.find("thread_utilization").tag("kind", "policy_evaluation").gauge();
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isZero();
  }

  @Test
  public void testRegisterGaugePolicyEvaluationThreadUtilization_MeterRegistryIsNull() {
    policyEvaluateServiceMetrics = new PolicyEvaluateServiceMetrics(null);

    policyEvaluateServiceMetrics.registerGaugePolicyEvaluationThreadUtilization(4);

    Gauge gauge = meterRegistry.find("thread_utilization").tag("kind", "policy_evaluation").gauge();
    assertThat(gauge).isNull();
  }

  @Test
  public void testEmitStartPolicyEvaluation() {
    policyEvaluateServiceMetrics.registerGaugePolicyEvaluationThreadUtilization(4);

    Sample sample = policyEvaluateServiceMetrics.emitStartPolicyEvaluation();
    Gauge gauge = meterRegistry.find("thread_utilization").tag("kind", "policy_evaluation").gauge();

    assertThat(sample).isNotNull();
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(25);
  }

  @Test
  public void testEmitStartPolicyEvaluation_MeterRegistryIsNull() {
    policyEvaluateServiceMetrics = new PolicyEvaluateServiceMetrics(null);

    Sample sample = policyEvaluateServiceMetrics.emitStartPolicyEvaluation();

    assertThat(sample).isNull();
  }

  @Test
  public void testEmitEndPolicyEvaluation() {
    Sample sample = Mockito.mock(Sample.class);
    when(sample.stop()).thenReturn(Duration.ofMillis(10000L).toNanos());

    policyEvaluateServiceMetrics.emitEndPolicyEvaluation(sample);

    verify(sample).stop();

    assertTimer("evaluation_completed.duration", "kind", "policy_evaluation")
        .isEqualTo(10000L);

    assertCounter("evaluation_completed", "kind", "policy_evaluation")
        .isEqualTo(1);
  }

  @Test
  public void testEmitEndPolicyEvaluation_MeterRegistryIsNull() {
    policyEvaluateServiceMetrics = new PolicyEvaluateServiceMetrics(null);
    Sample sample = Mockito.mock(Sample.class);

    policyEvaluateServiceMetrics.emitEndPolicyEvaluation(sample);

    verify(sample, never()).stop();
  }

  private AbstractDoubleAssert<?> assertCounter(String name, String... tags) {
    Counter counter = meterRegistry.find(name).tags(tags).counter();
    if (counter == null) {
      // if lookup by tags fails, fallback to just name and manually assert tags for better failure message
      counter = meterRegistry.find(name).counter();
      assertThat(counter).isNotNull();
      assertThat(counter.getId().getTags()).containsAll(Tags.of(tags));
    }
    return assertThat(counter.count());
  }

  private AbstractLongAssert<?> assertTimer(String name, String... tags) {
    Timer timer = meterRegistry.find(name).tags(tags).timer();
    if (timer == null) {
      // if lookup by tags fails, fallback to just name and manually assert tags for better failure message
      timer = meterRegistry.find(name).timer();
      assertThat(timer).isNotNull();
      assertThat(timer.getId().getTags()).containsAll(Tags.of(tags));
    }
    return assertThat((long) timer.totalTime(TimeUnit.MILLISECONDS));
  }
}
