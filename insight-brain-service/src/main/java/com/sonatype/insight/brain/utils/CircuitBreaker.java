/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens after a threshold of consecutive failures and fails fast while open, allowing a probe request
 * through once a cooldown period has elapsed. Deliberately omits half-open probe locking (single-flight):
 * any caller may probe once the cooldown elapses. Acceptable at low request volumes; avoids unnecessary
 * synchronization for callers where a handful of concurrent probes during the cooldown-elapsed window is
 * an acceptable cost.
 */
public class CircuitBreaker
{
  private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

  private final String name;

  private final int failureThreshold;

  private final Duration cooldown;

  private final AtomicInteger consecutiveFailures = new AtomicInteger();

  private final AtomicLong openedAtMillis = new AtomicLong(0);

  public CircuitBreaker(String name, int failureThreshold, Duration cooldown) {
    if (failureThreshold <= 0) {
      throw new IllegalArgumentException("failureThreshold must be positive: " + failureThreshold);
    }
    this.name = name;
    this.failureThreshold = failureThreshold;
    this.cooldown = Objects.requireNonNull(cooldown, "cooldown");
  }

  public boolean allowRequest() {
    long openedAt = openedAtMillis.get();
    if (openedAt == 0) {
      return true;
    }
    return System.currentTimeMillis() - openedAt >= cooldown.toMillis();
  }

  public void recordSuccess() {
    consecutiveFailures.set(0);
    if (openedAtMillis.getAndSet(0) != 0) {
      log.info("[{}] Circuit breaker closed after a successful probe", name);
    }
  }

  public void recordFailure() {
    // Once open, consecutiveFailures keeps growing on each failed probe (it is never capped at
    // failureThreshold) - recordSuccess() is the only path that resets it back to 0.
    if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
      // getAndSet is atomic, so exactly one concurrent caller observes the pre-transition value of 0
      // and logs the "opened" transition; any others observe the already-open timestamp and log the
      // probe-failure/extend-cooldown case instead. This closes a check-then-act race that a plain
      // "if (openedAtMillis == 0) { ...; openedAtMillis = now; }" would have.
      if (openedAtMillis.getAndSet(System.currentTimeMillis()) == 0) {
        log.warn("[{}] Circuit breaker opened after {} consecutive failures", name, failureThreshold);
      }
      else {
        log.warn("[{}] Circuit breaker probe failed, extending cooldown window", name);
      }
    }
  }
}
