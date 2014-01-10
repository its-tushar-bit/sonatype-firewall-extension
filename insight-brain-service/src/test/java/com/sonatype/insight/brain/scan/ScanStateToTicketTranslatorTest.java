/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import com.sonatype.insight.brain.scan.ScanTask.State;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test the translation from {@link ScanTask.State} to {@link ScanTicket} steps.
 */
public class ScanStateToTicketTranslatorTest
{
  private ScanTicket updatableTicket = new ScanTicket();

  @Test
  public void totalStepsAreCalculated() {
    State.SCANNING_COMPONENTS.provideStepInfo(updatableTicket);

    assertThat(updatableTicket.totalSteps, equalTo(5));
  }

  @Test
  public void pendingStateIsStepZero() {
    State.PENDING.provideStepInfo(updatableTicket);

    assertThat(updatableTicket.currentStep, equalTo(0));
    assertThat(updatableTicket.currentStepName, equalTo("Queued"));
  }

  @Test
  public void firstStepExtractedFromState() {
    State.SCANNING_COMPONENTS.provideStepInfo(updatableTicket);

    assertThat(updatableTicket.currentStep, equalTo(1));
    assertThat(updatableTicket.currentStepName, equalTo("Fingerprinting components"));
  }

  /**
   * UI expects that when current step == total steps that it is done.
   */
  @Test
  public void lastStepExtractedFromState() {
    State.DONE.provideStepInfo(updatableTicket);

    assertThat(updatableTicket.currentStep, equalTo(updatableTicket.totalSteps));
    assertThat(updatableTicket.currentStepName, equalTo("Done"));
  }
}
