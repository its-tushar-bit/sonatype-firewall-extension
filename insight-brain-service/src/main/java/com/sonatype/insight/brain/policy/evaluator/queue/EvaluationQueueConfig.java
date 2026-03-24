/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator.queue;

import java.time.Duration;

public record EvaluationQueueConfig(
    boolean enabled,
    long producerPeriodInMilliseconds,
    int producerMaxQueuedRows,
    long cyclePeriodInMilliseconds,
    boolean resetCycleOnTimeout,
    int consumerThreadsPerTenant,
    long consumerPeriodInMilliseconds,
    int consumerMaxQueuedRows,
    long consumerRowExpirationInMilliseconds)
{

  public static final boolean DEFAULT_ENABLED = false;

  public static final Duration DEFAULT_PRODUCER_PERIOD = Duration.ofMinutes(10);

  public static final int DEFAULT_PRODUCER_MAX_QUEUED_ROWS = 1000;

  public static final Duration DEFAULT_TARGET_CYCLE_PERIOD = Duration.ofHours(24);

  public static final boolean DEFAULT_RESET_CYCLE_ON_TIMEOUT = false;

  public static final int DEFAULT_CONSUMER_THREADS_PER_TENANT = 10;

  public static final Duration DEFAULT_CONSUMER_PERIOD = Duration.ofMinutes(5);

  public static final Duration DEFAULT_ESTIMATED_EVALUATION_PERIOD = Duration.ofSeconds(20);

  public static final int DEFAULT_CONSUMER_MAX_QUEUED_ROWS =
      (int) (DEFAULT_CONSUMER_PERIOD.dividedBy(DEFAULT_ESTIMATED_EVALUATION_PERIOD)
          * DEFAULT_CONSUMER_THREADS_PER_TENANT);

  public static final Duration DEFAULT_CONSUMER_ROW_EXPIRATION =
      DEFAULT_ESTIMATED_EVALUATION_PERIOD
          .multipliedBy(DEFAULT_CONSUMER_MAX_QUEUED_ROWS / DEFAULT_CONSUMER_THREADS_PER_TENANT)
          .multipliedBy(10);

  public void validate() {
    if (producerPeriodInMilliseconds <= 0) {
      throw new IllegalArgumentException("producerPeriodInMilliseconds must be positive.");
    }
    if (producerMaxQueuedRows <= 0) {
      throw new IllegalArgumentException("producerMaxQueuedRows must be positive.");
    }
    if (cyclePeriodInMilliseconds <= 0) {
      throw new IllegalArgumentException("cyclePeriodInMilliseconds must be positive.");
    }
    if (consumerThreadsPerTenant <= 0) {
      throw new IllegalArgumentException("consumerThreadsPerTenant must be positive.");
    }
    if (consumerPeriodInMilliseconds <= 0) {
      throw new IllegalArgumentException("consumerPeriodInMilliseconds must be positive.");
    }
    if (consumerMaxQueuedRows <= 0) {
      throw new IllegalArgumentException("consumerMaxQueuedRows must be positive.");
    }
    if (consumerRowExpirationInMilliseconds <= 0) {
      throw new IllegalArgumentException("consumerRowExpirationInMilliseconds must be positive.");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder
  {
    private boolean enabled = DEFAULT_ENABLED;

    private long producerPeriodInMilliseconds = DEFAULT_PRODUCER_PERIOD.toMillis();

    private int producerMaxQueuedRows = DEFAULT_PRODUCER_MAX_QUEUED_ROWS;

    private long cyclePeriodInMilliseconds = DEFAULT_TARGET_CYCLE_PERIOD.toMillis();

    private boolean resetCycleOnTimeout = DEFAULT_RESET_CYCLE_ON_TIMEOUT;

    private int consumerThreadsPerTenant = DEFAULT_CONSUMER_THREADS_PER_TENANT;

    private long consumerPeriodInMilliseconds = DEFAULT_CONSUMER_PERIOD.toMillis();

    private int consumerMaxQueuedRows = DEFAULT_CONSUMER_MAX_QUEUED_ROWS;

    private long consumerRowExpirationMilliseconds = DEFAULT_CONSUMER_ROW_EXPIRATION.toMillis();

    public Builder enabled(final boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder producerPeriod(final Duration producerPeriod) {
      this.producerPeriodInMilliseconds = producerPeriod.toMillis();
      return this;
    }

    public Builder producerMaxQueuedRows(final int maxQueuedRows) {
      this.producerMaxQueuedRows = maxQueuedRows;
      return this;
    }

    public Builder cyclePeriod(final Duration cyclePeriod) {
      this.cyclePeriodInMilliseconds = cyclePeriod.toMillis();
      return this;
    }

    public Builder resetCycleOnTimeout(final boolean resetCycleOnTimeout) {
      this.resetCycleOnTimeout = resetCycleOnTimeout;
      return this;
    }

    public Builder consumerThreadsPerTenant(final int consumerThreadsPerTenant) {
      this.consumerThreadsPerTenant = consumerThreadsPerTenant;
      return this;
    }

    public Builder consumerPeriod(final Duration consumerPeriod) {
      this.consumerPeriodInMilliseconds = consumerPeriod.toMillis();
      return this;
    }

    public Builder consumerMaxQueuedRows(final int consumerMaxQueuedRows) {
      this.consumerMaxQueuedRows = consumerMaxQueuedRows;
      return this;
    }

    public Builder consumerRowExpiration(final Duration consumerRowExpiration) {
      this.consumerRowExpirationMilliseconds = consumerRowExpiration.toMillis();
      return this;
    }

    public EvaluationQueueConfig build() {
      EvaluationQueueConfig config = new EvaluationQueueConfig(
          enabled,
          producerPeriodInMilliseconds,
          producerMaxQueuedRows,
          cyclePeriodInMilliseconds,
          resetCycleOnTimeout,
          consumerThreadsPerTenant,
          consumerPeriodInMilliseconds,
          consumerMaxQueuedRows,
          consumerRowExpirationMilliseconds);
      config.validate();
      return config;
    }
  }
}
