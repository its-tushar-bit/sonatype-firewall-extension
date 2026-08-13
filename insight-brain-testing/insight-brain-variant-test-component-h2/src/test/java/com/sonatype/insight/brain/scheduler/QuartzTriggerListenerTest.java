/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class QuartzTriggerListenerTest
    extends AbstractComponentH2Test
{
  @Inject
  private QuartzTriggerListener quartzTriggerListener;

  @Test
  public void testGetName() {
    assertThat(quartzTriggerListener.getName()).isEqualTo(QuartzTriggerListener.class.getSimpleName());
  }

  @Test
  public void testVetoJobExecution_True() {
    Trigger trigger = TriggerBuilder.newTrigger().usingJobData(QuartzTriggerListener.QUARTZ_VETO, true).build();

    assertThat(quartzTriggerListener.vetoJobExecution(trigger, null)).isTrue();
  }

  @Test
  public void testVetoJobExecution_False() {
    Trigger trigger = TriggerBuilder.newTrigger().usingJobData(QuartzTriggerListener.QUARTZ_VETO, false).build();

    assertThat(quartzTriggerListener.vetoJobExecution(trigger, null)).isFalse();
  }

  @Test
  public void testVetoJobExecution_Undefined() {
    Trigger trigger = TriggerBuilder.newTrigger().build();

    assertThat(quartzTriggerListener.vetoJobExecution(trigger, null)).isFalse();
  }
}
