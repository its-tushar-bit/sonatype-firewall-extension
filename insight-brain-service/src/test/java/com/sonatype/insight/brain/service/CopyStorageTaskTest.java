/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.tenancy.MtiqBatchJob;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Binder;
import com.google.inject.Inject;
import io.dropwizard.servlets.tasks.Task;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.quartz.JobBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class CopyStorageTaskTest
    extends AbstractComponentTest
{
  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private CopyStorageService mockCopyStorageService;

  @Inject
  private CopyStorageTask copyStorageTask;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(CopyStorageService.class).toInstance(mockCopyStorageService);
    super.configure(binder);
  }

  @Test
  public void testCopyStorageTask() {
    assertThat(copyStorageTask).isInstanceOf(Task.class);
    assertThat(copyStorageTask).isInstanceOf(InsightJob.class);
    assertThat(copyStorageTask).isInstanceOf(MtiqBatchJob.class);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(CopyStorageTask.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute_FromNotSpecified() {
    Map<String, List<String>> map = Map.of(
        "tenant", List.of("some-tenant"),
        "to", List.of(DataStoreType.S3.name()));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> copyStorageTask.execute(map, null))
        .withMessageContaining("Missing required query parameter 'from'.");
  }

  @Test
  public void testExecute_ToNotSpecified() {
    Map<String, List<String>> map = Map.of(
        "tenant", List.of("some-tenant"),
        "from", List.of(DataStoreType.FILE.name()));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> copyStorageTask.execute(map, null))
        .withMessageContaining("Missing required query parameter 'to'.");
  }

  @Test
  public void testExecute_UnknownFrom() {
    Map<String, List<String>> map = Map.of(
        "tenant", List.of("some-tenant"),
        "from", List.of("unknownFrom"),
        "to", List.of(DataStoreType.S3.name()));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> copyStorageTask.execute(map, null));
  }

  @Test
  public void testExecute_UnknownTo() {
    Map<String, List<String>> map = Map.of(
        "tenant", List.of("some-tenant"),
        "from", List.of(DataStoreType.FILE.name()),
        "to", List.of("unknownTo"));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> copyStorageTask.execute(map, null));
  }

  @Test
  public void testExecute_ChecksFromAndTo() throws Exception {
    Map<String, List<String>> map = Map.of(
        "from", List.of(DataStoreType.FILE.name()),
        "to", List.of(DataStoreType.S3.name()));

    copyStorageTask.execute(map, null);

    verify(mockCopyStorageService).checkSupported(DataStoreType.FILE);
    verify(mockCopyStorageService).checkSupported(DataStoreType.S3);
    verify(mockCopyStorageService).checkFromAndToAreDifferent(DataStoreType.FILE, DataStoreType.S3);
    verify(mockCopyStorageService).checkPrimaryStorageIsTarget(DataStoreType.S3);
  }

  @Test
  public void testExecute_TriggersTheJob() throws Exception {
    Map<String, List<String>> map = Map.of(
        "from", List.of(DataStoreType.FILE.name()),
        "to", List.of(DataStoreType.S3.name()));

    copyStorageTask.execute(map, null);

    ArgumentCaptor<Map<String, String>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(copyStorageTask), argumentCaptor.capture());
    Map<String, String> value = argumentCaptor.getValue();
    assertThat(value).containsEntry("from", DataStoreType.FILE.name());
    assertThat(value).containsEntry("to", DataStoreType.S3.name());
  }

  @Test
  public void testGetJobName() {
    assertThat(copyStorageTask.getJobName()).isEqualTo("CopyStorageTask");
  }
}
