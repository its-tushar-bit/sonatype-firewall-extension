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
   * Completed tasks should be deleted once they have been retrieved in order to conserve memory.
   */
  @Test
  public void getDoneTicketDeletesTicketResources() {
    ScanTaskRepository taskRepository = mock(ScanTaskRepository.class);
    ScanService service = new ScanService(taskRepository);

    ScanTask completedTask = mock(ScanTask.class);
    when(completedTask.getState()).thenReturn(State.DONE);
    when(taskRepository.getByIdNotNull("completed-ticket-id")).thenReturn(completedTask);

    service.getTicket("any-app-id", "completed-ticket-id");

    verify(taskRepository).remove("completed-ticket-id");
  }
}
