/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import javax.inject.Provider;

import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

    repo.newScanTask(null, null, null, null, false, null, null);

    ScanTask task = repo.getByIdNotNull("stub-id");

    assertThat(task.getId()).isNotNull();
  }

  @Test
  public void getUnknownTicketThrowsException() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repo.getByIdNotNull("unknown-task");
    }).withMessageContaining("unknown-task");
  }

  @Test
  public void taskCanBeRemoved() {
    ScanTask stubTask = mock(ScanTask.class);
    when(provider.get()).thenReturn(stubTask);
    when(stubTask.getId()).thenReturn("stub-id");

    ScanTask task = repo.newScanTask(null, null, null, null, false, null, null);

    repo.remove(task.getId());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repo.getByIdNotNull(task.getId());
    });
  }

  @Test
  public void obsoleteTaskGetsPurged() {
    ScanTask task = mock(ScanTask.class);
    when(provider.get()).thenReturn(task);
    when(task.getId()).thenReturn("task-0");
    when(task.isObsolete()).thenReturn(true);
    repo.newScanTask(null, null, null, null, false, null, null);

    task = mock(ScanTask.class);
    when(provider.get()).thenReturn(task);
    when(task.getId()).thenReturn("task-1");
    repo.newScanTask(null, null, null, null, false, null, null);

    assertThat(repo.getByIdNotNull("task-1")).isNotNull();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      repo.getByIdNotNull("task-0");
    });
  }
}
