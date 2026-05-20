/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.consumption;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConsumptionLimitConfigTest
{
  @Test
  public void setWarningThresholdPct_withinRange_accepts() {
    ConsumptionLimitConfig config = new ConsumptionLimitConfig();

    config.setWarningThresholdPct(0);
    assertThat(config.getWarningThresholdPct()).isEqualTo(0);

    config.setWarningThresholdPct(80);
    assertThat(config.getWarningThresholdPct()).isEqualTo(80);

    config.setWarningThresholdPct(100);
    assertThat(config.getWarningThresholdPct()).isEqualTo(100);
  }

  @Test
  public void setWarningThresholdPct_negative_throws() {
    ConsumptionLimitConfig config = new ConsumptionLimitConfig();

    assertThatThrownBy(() -> config.setWarningThresholdPct(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("-1");
  }

  @Test
  public void setWarningThresholdPct_aboveHundred_throws() {
    ConsumptionLimitConfig config = new ConsumptionLimitConfig();

    assertThatThrownBy(() -> config.setWarningThresholdPct(101))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("101");
  }

  @Test
  public void setEnforcementMode_null_throws() {
    ConsumptionLimitConfig config = new ConsumptionLimitConfig();

    assertThatThrownBy(() -> config.setEnforcementMode(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void setMonthlyLimit_null_accepts() {
    ConsumptionLimitConfig config = new ConsumptionLimitConfig();

    config.setMonthlyLimit(null);
    assertThat(config.getMonthlyLimit()).isNull();
  }

  @Test
  public void setMonthlyLimit_positive_accepts() {
    ConsumptionLimitConfig config = new ConsumptionLimitConfig();

    config.setMonthlyLimit(1L);
    assertThat(config.getMonthlyLimit()).isEqualTo(1L);

    config.setMonthlyLimit(50_000L);
    assertThat(config.getMonthlyLimit()).isEqualTo(50_000L);

    config.setMonthlyLimit(Long.MAX_VALUE);
    assertThat(config.getMonthlyLimit()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  public void setMonthlyLimit_zero_throws() {
    ConsumptionLimitConfig config = new ConsumptionLimitConfig();

    assertThatThrownBy(() -> config.setMonthlyLimit(0L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("0");
  }

  @Test
  public void setMonthlyLimit_negative_throws() {
    ConsumptionLimitConfig config = new ConsumptionLimitConfig();

    assertThatThrownBy(() -> config.setMonthlyLimit(-1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("-1");
  }
}
