/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.time.Duration;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerTest
{
  @Test
  public void testAllowRequest_initiallyClosed() {
    CircuitBreaker breaker = new CircuitBreaker("test", 3, Duration.ofMillis(50));
    assertThat(breaker.allowRequest()).isTrue();
  }

  @Test
  public void testAllowRequest_opensAfterThresholdConsecutiveFailures() {
    CircuitBreaker breaker = new CircuitBreaker("test", 3, Duration.ofMillis(50));
    breaker.recordFailure();
    breaker.recordFailure();
    assertThat(breaker.allowRequest()).isTrue();

    breaker.recordFailure();
    assertThat(breaker.allowRequest()).isFalse();
  }

  @Test
  public void testAllowRequest_successResetsConsecutiveFailureCount() {
    CircuitBreaker breaker = new CircuitBreaker("test", 3, Duration.ofMillis(50));
    breaker.recordFailure();
    breaker.recordFailure();
    breaker.recordSuccess();
    breaker.recordFailure();
    breaker.recordFailure();

    assertThat(breaker.allowRequest()).isTrue();
  }

  @Test
  public void testAllowRequest_allowsProbeAfterCooldownElapses() throws InterruptedException {
    CircuitBreaker breaker = new CircuitBreaker("test", 1, Duration.ofMillis(200));
    breaker.recordFailure();
    assertThat(breaker.allowRequest()).isFalse();

    Thread.sleep(300);
    assertThat(breaker.allowRequest()).isTrue();
  }

  @Test
  public void testRecordSuccess_afterProbeClosesBreaker() throws InterruptedException {
    CircuitBreaker breaker = new CircuitBreaker("test", 1, Duration.ofMillis(200));
    breaker.recordFailure();
    Thread.sleep(300);
    assertThat(breaker.allowRequest()).isTrue();

    breaker.recordSuccess();
    breaker.recordFailure();
    // threshold is 1 and recordSuccess reset the counter to 0, so this next failure increments
    // it back to 1 (>= threshold), which reopens the breaker.
    assertThat(breaker.allowRequest()).isFalse();
  }

  @Test
  public void testRecordFailure_afterProbeFailureKeepsBreakerOpenAndExtendsCooldown() throws InterruptedException {
    CircuitBreaker breaker = new CircuitBreaker("test", 1, Duration.ofMillis(200));
    breaker.recordFailure();
    Thread.sleep(300);
    assertThat(breaker.allowRequest()).isTrue();

    breaker.recordFailure();
    assertThat(breaker.allowRequest()).isFalse();

    Thread.sleep(300);
    assertThat(breaker.allowRequest()).isTrue();
  }
}
