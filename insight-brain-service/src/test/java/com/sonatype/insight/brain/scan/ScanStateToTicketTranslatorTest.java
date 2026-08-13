/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import com.sonatype.insight.brain.scan.ScanTask.State;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the translation from {@link ScanTask.State} to {@link ScanTicket} steps.
 */
public class ScanStateToTicketTranslatorTest
{
  private final ScanTicket updatableTicket = new ScanTicket();

  @Test
  public void totalStepsAreCalculated() {
    State.SCANNING_COMPONENTS.provideStepInfo(updatableTicket);

    assertThat(updatableTicket.totalSteps).isEqualTo(5);
  }

  @Test
  public void pendingStateIsStepZero() {
    State.PENDING.provideStepInfo(updatableTicket);

    assertThat(updatableTicket.currentStep).isEqualTo(0);
    assertThat(updatableTicket.currentStepName).isEqualTo("Queued");
  }

  @Test
  public void firstStepExtractedFromState() {
    State.SCANNING_COMPONENTS.provideStepInfo(updatableTicket);

    assertThat(updatableTicket.currentStep).isEqualTo(1);
    assertThat(updatableTicket.currentStepName).isEqualTo("Fingerprinting components");
  }

  /**
   * UI expects that when current step == total steps that it is done.
   */
  @Test
  public void lastStepExtractedFromState() {
    State.DONE.provideStepInfo(updatableTicket);

    assertThat(updatableTicket.currentStep).isEqualTo(updatableTicket.totalSteps);
    assertThat(updatableTicket.currentStepName).isEqualTo("Done");
  }
}
