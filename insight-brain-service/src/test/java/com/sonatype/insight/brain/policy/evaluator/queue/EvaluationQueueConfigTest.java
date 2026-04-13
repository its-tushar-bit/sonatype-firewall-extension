/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator.queue;

import java.time.Duration;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EvaluationQueueConfigTest
{
  @Test
  public void testValidate_defaultValues() {
    EvaluationQueueConfig config = EvaluationQueueConfig.builder().build();

    assertThatNoException().isThrownBy(config::validate);
  }

  @Test
  public void testValidate_producerPeriodZero() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().producerPeriod(Duration.ZERO).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("producerPeriodInMilliseconds must be positive.");
  }

  @Test
  public void testValidate_producerPeriodNegative() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().producerPeriod(Duration.ofMillis(-1)).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("producerPeriodInMilliseconds must be positive.");
  }

  @Test
  public void testValidate_producerMaxQueuedRowsZero() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().producerMaxQueuedRows(0).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("producerMaxQueuedRows must be positive.");
  }

  @Test
  public void testValidate_producerMaxQueuedRowsNegative() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().producerMaxQueuedRows(-1).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("producerMaxQueuedRows must be positive.");
  }

  @Test
  public void testValidate_cyclePeriodZero() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().cyclePeriod(Duration.ZERO).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cyclePeriodInMilliseconds must be positive.");
  }

  @Test
  public void testValidate_cyclePeriodNegative() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().cyclePeriod(Duration.ofMillis(-1)).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cyclePeriodInMilliseconds must be positive.");
  }

  @Test
  public void testValidate_consumerThreadsPerTenantZero() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().consumerThreadsPerTenant(0).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("consumerThreadsPerTenant must be positive.");
  }

  @Test
  public void testValidate_consumerThreadsPerTenantNegative() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().consumerThreadsPerTenant(-1).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("consumerThreadsPerTenant must be positive.");
  }

  @Test
  public void testValidate_consumerPeriodZero() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().consumerPeriod(Duration.ZERO).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("consumerPeriodInMilliseconds must be positive.");
  }

  @Test
  public void testValidate_consumerPeriodNegative() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().consumerPeriod(Duration.ofMillis(-1)).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("consumerPeriodInMilliseconds must be positive.");
  }

  @Test
  public void testValidate_consumerMaxQueuedRowsZero() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().consumerMaxQueuedRows(0).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("consumerMaxQueuedRows must be positive.");
  }

  @Test
  public void testValidate_consumerMaxQueuedRowsNegative() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().consumerMaxQueuedRows(-1).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("consumerMaxQueuedRows must be positive.");
  }

  @Test
  public void testValidate_consumerRowExpirationZero() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().consumerRowExpiration(Duration.ZERO).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("consumerRowExpirationInMilliseconds must be positive.");
  }

  @Test
  public void testValidate_consumerRowExpirationNegative() {
    assertThatThrownBy(() -> EvaluationQueueConfig.builder().consumerRowExpiration(Duration.ofMillis(-1)).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("consumerRowExpirationInMilliseconds must be positive.");
  }

  @Test
  public void testValidate_minimumValidValues() {
    EvaluationQueueConfig config = new EvaluationQueueConfig(
        false,
        1,
        1,
        1,
        false,
        false,
        1,
        1,
        1,
        1);

    assertThatNoException().isThrownBy(config::validate);
  }

  @Test
  public void testValidate_maximumValidValues() {
    EvaluationQueueConfig config = new EvaluationQueueConfig(
        true,
        Long.MAX_VALUE,
        Integer.MAX_VALUE,
        Long.MAX_VALUE,
        true,
        true,
        Integer.MAX_VALUE,
        Long.MAX_VALUE,
        Integer.MAX_VALUE,
        Long.MAX_VALUE);

    assertThatNoException().isThrownBy(config::validate);
  }

  @Test
  public void testMerge_emptyOverrides() {
    EvaluationQueueConfig base = EvaluationQueueConfig.builder().build();

    EvaluationQueueConfig result = EvaluationQueueConfig.merge(base, Map.of());

    assertThat(result).isEqualTo(base);
  }

  @Test
  public void testMerge_singleOverride() {
    EvaluationQueueConfig base = EvaluationQueueConfig.builder().enabled(false).build();

    EvaluationQueueConfig result = EvaluationQueueConfig.merge(base, Map.of("enabled", true));

    assertThat(result).usingRecursiveComparison().ignoringFields("enabled").isEqualTo(base);
    assertThat(result.enabled()).isTrue();
  }

  @Test
  public void testMerge_multipleOverrides() {
    EvaluationQueueConfig base = EvaluationQueueConfig.builder().build();

    EvaluationQueueConfig result = EvaluationQueueConfig.merge(base,
        Map.of("enabled", true, "consumerThreadsPerTenant", 2, "producerMaxQueuedRows", 500));

    assertThat(result.enabled()).isTrue();
    assertThat(result.consumerThreadsPerTenant()).isEqualTo(2);
    assertThat(result.producerMaxQueuedRows()).isEqualTo(500);
    assertThat(result.consumerPeriodInMilliseconds()).isEqualTo(base.consumerPeriodInMilliseconds());
  }

  @Test
  public void testMerge_overridesOntoNonDefault() {
    EvaluationQueueConfig base = EvaluationQueueConfig.builder()
        .enabled(true)
        .consumerThreadsPerTenant(5)
        .producerMaxQueuedRows(200)
        .build();

    EvaluationQueueConfig result = EvaluationQueueConfig.merge(base, Map.of("consumerThreadsPerTenant", 3));

    assertThat(result.enabled()).isTrue();
    assertThat(result.consumerThreadsPerTenant()).isEqualTo(3);
    assertThat(result.producerMaxQueuedRows()).isEqualTo(200);
  }

  @Test
  public void testBuild_usesDefaults() {
    assertThat(EvaluationQueueConfig.DEFAULT_ENABLED).isTrue();
    assertThat(EvaluationQueueConfig.DEFAULT_PRODUCER_PERIOD).isEqualTo(Duration.ofMinutes(10));
    assertThat(EvaluationQueueConfig.DEFAULT_PRODUCER_MAX_QUEUED_ROWS).isEqualTo(1000);
    assertThat(EvaluationQueueConfig.DEFAULT_TARGET_CYCLE_PERIOD).isEqualTo(Duration.ofHours(24));
    assertThat(EvaluationQueueConfig.DEFAULT_RESET_CYCLE_ON_TIMEOUT).isFalse();
    assertThat(EvaluationQueueConfig.DEFAULT_CONSUMER_THREADS_PER_TENANT).isEqualTo(1);
    assertThat(EvaluationQueueConfig.DEFAULT_CONSUMER_PERIOD).isEqualTo(Duration.ofMinutes(5));
    assertThat(EvaluationQueueConfig.DEFAULT_CONSUMER_MAX_QUEUED_ROWS).isEqualTo(30);

    EvaluationQueueConfig config = EvaluationQueueConfig.builder().build();

    assertThat(config.enabled()).isEqualTo(EvaluationQueueConfig.DEFAULT_ENABLED);
    assertThat(config.producerPeriodInMilliseconds()).isEqualTo(
        EvaluationQueueConfig.DEFAULT_PRODUCER_PERIOD.toMillis());
    assertThat(config.producerMaxQueuedRows()).isEqualTo(EvaluationQueueConfig.DEFAULT_PRODUCER_MAX_QUEUED_ROWS);
    assertThat(config.cyclePeriodInMilliseconds()).isEqualTo(
        EvaluationQueueConfig.DEFAULT_TARGET_CYCLE_PERIOD.toMillis());
    assertThat(config.resetCycleOnTimeout()).isEqualTo(EvaluationQueueConfig.DEFAULT_RESET_CYCLE_ON_TIMEOUT);
    assertThat(config.consumerThreadsPerTenant()).isEqualTo(EvaluationQueueConfig.DEFAULT_CONSUMER_THREADS_PER_TENANT);
    assertThat(config.consumerPeriodInMilliseconds()).isEqualTo(
        EvaluationQueueConfig.DEFAULT_CONSUMER_PERIOD.toMillis());
    assertThat(config.consumerMaxQueuedRows()).isEqualTo(EvaluationQueueConfig.DEFAULT_CONSUMER_MAX_QUEUED_ROWS);
  }
}
