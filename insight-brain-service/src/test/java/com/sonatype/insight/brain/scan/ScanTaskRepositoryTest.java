/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import javax.inject.Provider;

import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanTaskRepositoryTest
{
  @SuppressWarnings("unchecked")
  private Provider<ScanTask> provider = mock(Provider.class);

  private ScanTaskRepository repo = new ScanTaskRepository(provider);

  @Test
  public void getCreatedTask() {
    ScanTask stubTask = mock(ScanTask.class);
    when(provider.get()).thenReturn(stubTask);
    when(stubTask.getId()).thenReturn("stub-id");

    repo.newScanTask(null, null, null, null, false);

    ScanTask task = repo.getByIdNotNull("stub-id");

    assertThat(task.getId(), is(notNullValue()));
  }

  @Test
  public void getUnknownTicketThrowsException() {
    try {
      repo.getByIdNotNull("unknown-task");
      fail("Exception should have been thrown");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), containsString("unknown-task"));
    }
  }

  @Test
  public void taskCanBeRemoved() {
    ScanTask stubTask = mock(ScanTask.class);
    when(provider.get()).thenReturn(stubTask);
    when(stubTask.getId()).thenReturn("stub-id");

    ScanTask task = repo.newScanTask(null, null, null, null, false);

    repo.remove(task.getId());

    try {
      repo.getByIdNotNull(task.getId());
      fail("Task should have been removed from storage");
    }
    catch (NotFoundException expected) {
    }
  }

  @Test
  public void obsoleteTaskGetsPurged() {
    ScanTask task = mock(ScanTask.class);
    when(provider.get()).thenReturn(task);
    when(task.getId()).thenReturn("task-0");
    when(task.isObsolete()).thenReturn(true);
    repo.newScanTask(null, null, null, null, false);

    task = mock(ScanTask.class);
    when(provider.get()).thenReturn(task);
    when(task.getId()).thenReturn("task-1");
    repo.newScanTask(null, null, null, null, false);

    assertThat(repo.getByIdNotNull("task-1"), is(notNullValue()));
    try {
      repo.getByIdNotNull("task-0");
      fail("Task should have been purged from storage");
    }
    catch (NotFoundException expected) {
    }
  }
}
