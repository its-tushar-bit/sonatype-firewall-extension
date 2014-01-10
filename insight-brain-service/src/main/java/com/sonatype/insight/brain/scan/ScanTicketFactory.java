/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import com.sonatype.insight.brain.scan.ScanTask.State;

/**
 * Utility to create {@link ScanTicket} objects.
 *
 * @since 1.8
 */
class ScanTicketFactory
{
  /**
   * Initializes a {@link ScanTicket} based on a {@link ScanTask.State}.
   * 
   * Initial values are created for the step information.  The caller is expected to fill in the remainder as necessary.
   */
  public static ScanTicket forScanState(State state) {
    ScanTicket ticket = new ScanTicket();

    ticket.totalSteps = State.values().length - 1; // Discount PENDING state as step.

    ticket.currentStep = state.ordinal();
    ticket.currentStepName = state.toString();

    return ticket;
  }
}
