/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.consumption;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConsumptionEventTest
{
  @Test
  public void getActivityType_knownValue_returnsEnum() {
    ConsumptionEvent event = new ConsumptionEvent();
    event.setActivityType(ActivityType.APP_SCAN);

    assertThat(event.getActivityType()).isEqualTo(ActivityType.APP_SCAN);
  }

  @Test
  public void getActivityType_unknownValue_returnsOthersInsteadOfThrowing() {
    ConsumptionEvent event = new ConsumptionEvent();
    event.setActivityTypeRaw("FUTURE_BUCKET_WE_DONT_KNOW");

    assertThat(event.getActivityType()).isEqualTo(ActivityType.OTHERS);
  }

  @Test
  public void getActivityType_nullRawValue_returnsOthers() {
    ConsumptionEvent event = new ConsumptionEvent();

    assertThat(event.getActivityType()).isEqualTo(ActivityType.OTHERS);
  }

  @Test
  public void setActivityType_others_throwsToPreventSentinelPersisting() {
    ConsumptionEvent event = new ConsumptionEvent();

    assertThatThrownBy(() -> event.setActivityType(ActivityType.OTHERS))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("OTHERS");
  }

  @Test
  public void setActivityType_null_throwsIllegalArgumentException() {
    ConsumptionEvent event = new ConsumptionEvent();

    assertThatThrownBy(() -> event.setActivityType(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("activityType");
  }

  @Test
  public void setComponentCount_rejectsZero() {
    ConsumptionEvent event = new ConsumptionEvent();

    assertThatThrownBy(() -> event.setComponentCount(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("componentCount");
  }

  @Test
  public void setComponentCount_rejectsNegative() {
    ConsumptionEvent event = new ConsumptionEvent();

    assertThatThrownBy(() -> event.setComponentCount(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("componentCount");
  }

  @Test
  public void setComponentCount_acceptsPositive() {
    ConsumptionEvent event = new ConsumptionEvent();
    event.setComponentCount(42);

    assertThat(event.getComponentCount()).isEqualTo(42);
  }
}
