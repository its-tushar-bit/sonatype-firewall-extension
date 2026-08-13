/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.MtiqBatchJob;
import com.sonatype.insight.error.exception.BadRequestException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobBuilder;

@ExtendWith(MockitoExtension.class)
public class CopyStorageTaskTest
{
  private static final CopyStorageConfig COPY_STORAGE_CONFIG = new CopyStorageConfig(1, 1);

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private CopyStorageService copyStorageService;

  @Mock
  private ApiConfigurationService apiConfigurationService;

  @Mock
  private ShutdownHandler shutdownHandler;

  private CopyStorageTask copyStorageTask;

  @BeforeEach
  public void setUp() {
    when(apiConfigurationService.getConfigurationNoAuthz(SystemConfigurationProperty.COPY_STORAGE_CONFIG))
        .thenReturn(COPY_STORAGE_CONFIG);

    copyStorageTask = new CopyStorageTask(
        taskScheduler,
        copyStorageService,
        apiConfigurationService,
        shutdownHandler);
  }

  @Test
  public void shouldImplementInsightJobAndMtiqBatchJob() {
    assertThat(copyStorageTask).isInstanceOf(InsightJob.class);
    assertThat(copyStorageTask).isInstanceOf(MtiqBatchJob.class);
  }

  @Test
  public void shouldDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(CopyStorageTask.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void shouldRejectMissingFromParameter() {
    Map<String, List<String>> parameters = Map.of("to", List.of(DataStoreType.S3.name()));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> copyStorageTask.execute(parameters, new PrintWriter(OutputStream.nullOutputStream())))
        .withMessageContaining("Missing required query parameter 'from'.");
  }

  @Test
  public void shouldRejectMissingToParameter() {
    Map<String, List<String>> parameters = Map.of("from", List.of(DataStoreType.FILE.name()));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> copyStorageTask.execute(parameters, new PrintWriter(OutputStream.nullOutputStream())))
        .withMessageContaining("Missing required query parameter 'to'.");
  }

  @Test
  public void shouldRejectUnknownFromDataStore() {
    Map<String, List<String>> parameters = Map.of(
        "from", List.of("unknownFrom"),
        "to", List.of(DataStoreType.S3.name()));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> copyStorageTask.execute(parameters, new PrintWriter(OutputStream.nullOutputStream())));
  }

  @Test
  public void shouldRejectUnknownToDataStore() {
    Map<String, List<String>> parameters = Map.of(
        "from", List.of(DataStoreType.FILE.name()),
        "to", List.of("unknownTo"));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> copyStorageTask.execute(parameters, new PrintWriter(OutputStream.nullOutputStream())));
  }

  @Test
  public void shouldValidateFromAndToBeforeScheduling() throws Exception {
    Map<String, List<String>> parameters = Map.of(
        "from", List.of("file"),
        "to", List.of("s3"));

    copyStorageTask.execute(parameters, new PrintWriter(OutputStream.nullOutputStream()));

    InOrder inOrder = inOrder(copyStorageService, taskScheduler);
    inOrder.verify(copyStorageService).checkSupported(DataStoreType.FILE);
    inOrder.verify(copyStorageService).checkSupported(DataStoreType.S3);
    inOrder.verify(copyStorageService).checkPrimaryStorageIsTarget(DataStoreType.S3);
    inOrder.verify(copyStorageService).checkFromAndToAreDifferent(DataStoreType.FILE, DataStoreType.S3);
    inOrder.verify(taskScheduler).scheduleOneTimeTask(same(copyStorageTask), eq(Map.of("from", "FILE", "to", "S3")));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldForwardUppercaseParametersWhenScheduling() throws Exception {
    Map<String, List<String>> parameters = Map.of(
        "from", List.of("file"),
        "to", List.of("s3"));

    copyStorageTask.execute(parameters, new PrintWriter(OutputStream.nullOutputStream()));

    ArgumentCaptor<Map<String, String>> jobParametersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(taskScheduler).scheduleOneTimeTask(same(copyStorageTask), jobParametersCaptor.capture());

    assertThat(jobParametersCaptor.getValue())
        .containsEntry("from", DataStoreType.FILE.name())
        .containsEntry("to", DataStoreType.S3.name());
  }

  @Test
  public void shouldExposeCopyStorageJobName() {
    assertThat(copyStorageTask.getJobName()).isEqualTo("CopyStorageTask");
  }
}
