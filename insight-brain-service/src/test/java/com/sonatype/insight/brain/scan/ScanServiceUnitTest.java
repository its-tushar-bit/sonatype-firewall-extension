/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import com.sonatype.insight.brain.scan.ScanTask.State;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScanServiceUnitTest
{
  @Test(expected = NotFoundException.class)
  @SuppressWarnings("unchecked")
  public void testGetTicketNotFound() {
    ScanTaskRepository taskRepository = mock(ScanTaskRepository.class);
    ScanService service = new ScanService(taskRepository);

    when(taskRepository.getByIdNotNull(anyString())).thenThrow(NotFoundException.class);

    service.getTicket("any-app-id", "unknown-ticket");
  }

  /**
   * Once the obtained ticket for a task indicates its done, the task should be removed in order to conserve memory.
   */
  @Test
  public void getDoneTicketDeletesTicketResources() {
    ScanTaskRepository taskRepository = mock(ScanTaskRepository.class);
    ScanService service = new ScanService(taskRepository);

    ScanTicket ticket = new ScanTicket();
    ticket.totalSteps = 5;
    ticket.currentStep = 4;
    ScanTask task = mock(ScanTask.class);
    // due to concurrency, the task state might be ahead of the state given by the retrieved ticket
    when(task.getState()).thenReturn(State.DONE);
    when(task.getTicket()).thenReturn(ticket);
    when(taskRepository.getByIdNotNull("completed-ticket-id")).thenReturn(task);

    service.getTicket("any-app-id", "completed-ticket-id");
    verify(taskRepository, never()).remove("completed-ticket-id");

    ticket.currentStep = ticket.totalSteps;

    service.getTicket("any-app-id", "completed-ticket-id");
    verify(taskRepository).remove("completed-ticket-id");
  }
}
