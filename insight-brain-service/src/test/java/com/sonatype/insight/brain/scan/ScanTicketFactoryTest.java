/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test the translation from {@link ScanTask.State} to {@link ScanTicket} steps.
 */
public class ScanTicketFactoryTest
{
  @Test
  public void totalStepsAreCalculated() {
    ScanTicket ticket = ScanTicketFactory.forScanState(ScanTask.State.SCANNING_COMPONENTS);

    assertThat(ticket.totalSteps, equalTo(5));
  }

  @Test
  public void pendingStateIsStepZero() {
    ScanTicket ticket = ScanTicketFactory.forScanState(ScanTask.State.PENDING);

    assertThat(ticket.currentStep, equalTo(0));
    assertThat(ticket.currentStepName, equalTo("Queued"));
  }

  @Test
  public void firstStepExtractedFromState() {
    ScanTicket ticket = ScanTicketFactory.forScanState(ScanTask.State.SCANNING_COMPONENTS);

    assertThat(ticket.currentStep, equalTo(1));
    assertThat(ticket.currentStepName, equalTo("Fingerprinting components"));
  }

  /**
   * UI expects that when current step == total steps that it is done.
   */
  @Test
  public void lastStepExtractedFromState() {
    ScanTicket ticket = ScanTicketFactory.forScanState(ScanTask.State.DONE);

    assertThat(ticket.currentStep, equalTo(ticket.totalSteps));
    assertThat(ticket.currentStepName, equalTo("Done"));
  }
}
