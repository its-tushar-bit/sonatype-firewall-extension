/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.tenancy.AllTenantsJob;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Binder;
import com.google.inject.Inject;
import io.dropwizard.servlets.tasks.Task;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class CopyStorageTaskTest
    extends AbstractComponentTest
{
  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private CopyStorageService mockCopyStorageService;

  @Mock
  private TenantUtil mockTenantUtil;

  @Inject
  private CopyStorageTask copyStorageTask;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(CopyStorageService.class).toInstance(mockCopyStorageService);
    binder.bind(TenantUtil.class).toInstance(mockTenantUtil);
    super.configure(binder);
  }

  @Before
  public void before() {
    lenient().when(mockTenantUtil.isMultiTenant()).thenReturn(true);
  }

  @Test
  public void testCopyStorageTask() {
    assertThat(copyStorageTask).isInstanceOf(Task.class);
    assertThat(copyStorageTask).isInstanceOf(InsightJob.class);
    assertThat(copyStorageTask).isInstanceOf(AllTenantsJob.class);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(CopyStorageTask.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute_TenantNotSpecified_Global() {
    Map<String, List<String>> map = Map.of(
        "from", List.of(DataStoreType.FILE.name()),
        "to", List.of(DataStoreType.S3.name())
    );

    testAsTenant(Tenant.GLOBAL_TENANT, t -> copyStorageTask.execute(map, null));

    ArgumentCaptor<Map<String, String>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(copyStorageTask), argumentCaptor.capture());
    Map<String, String> value = argumentCaptor.getValue();
    assertThat(value).containsEntry("tenant", ".*");
    assertThat(value).containsEntry("from", DataStoreType.FILE.name());
    assertThat(value).containsEntry("to", DataStoreType.S3.name());
  }

  @Test
  public void testExecute_TenantNotSpecified_NotGlobal() {
    Map<String, List<String>> map = Map.of(
        "from", List.of(DataStoreType.FILE.name()),
        "to", List.of(DataStoreType.S3.name())
    );

    testAsNewTenant("some-tenant", t -> copyStorageTask.execute(map, null));

    ArgumentCaptor<Map<String, String>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(copyStorageTask), argumentCaptor.capture());
    Map<String, String> value = argumentCaptor.getValue();
    assertThat(value).containsEntry("tenant", "some-tenant");
    assertThat(value).containsEntry("from", DataStoreType.FILE.name());
    assertThat(value).containsEntry("to", DataStoreType.S3.name());
  }

  @Test
  public void testExecute_FromNotSpecified() {
    Map<String, List<String>> map = Map.of(
        "tenant", List.of("some-tenant"),
        "to", List.of(DataStoreType.S3.name())
    );

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> copyStorageTask.execute(map, null))
        .withMessageContaining("Missing required query parameter 'from'.");
  }

  @Test
  public void testExecute_ToNotSpecified() {
    Map<String, List<String>> map = Map.of(
        "tenant", List.of("some-tenant"),
        "from", List.of(DataStoreType.FILE.name())
    );

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> copyStorageTask.execute(map, null))
        .withMessageContaining("Missing required query parameter 'to'.");
  }

  @Test
  public void testExecute_UnknownFrom() {
    Map<String, List<String>> map = Map.of(
        "tenant", List.of("some-tenant"),
        "from", List.of("unknownFrom"),
        "to", List.of(DataStoreType.S3.name())
    );

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> copyStorageTask.execute(map, null));
  }

  @Test
  public void testExecute_UnknownTo() {
    Map<String, List<String>> map = Map.of(
        "tenant", List.of("some-tenant"),
        "from", List.of(DataStoreType.FILE.name()),
        "to", List.of("unknownTo")
    );

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> copyStorageTask.execute(map, null));
  }

  @Test
  public void testExecute_ChecksFromAndTo() throws Exception {
    Map<String, List<String>> map = Map.of(
        "tenant", List.of("some-tenant"),
        "from", List.of(DataStoreType.FILE.name()),
        "to", List.of(DataStoreType.S3.name())
    );

    copyStorageTask.execute(map, null);

    verify(mockCopyStorageService).checkSupported(DataStoreType.FILE);
    verify(mockCopyStorageService).checkSupported(DataStoreType.S3);
    verify(mockCopyStorageService).checkFromAndToAreDifferent(DataStoreType.FILE, DataStoreType.S3);
    verify(mockCopyStorageService).checkPrimaryStorageIsTarget(DataStoreType.S3);
  }

  @Test
  public void testExecute_ChecksTheTenantPattern() {
    Map<String, List<String>> map = Map.of(
        "tenant", List.of("("),
        "from", List.of(DataStoreType.FILE.name()),
        "to", List.of(DataStoreType.S3.name())
    );

    assertThatExceptionOfType(PatternSyntaxException.class)
        .isThrownBy(() -> copyStorageTask.execute(map, null));
  }

  @Test
  public void testExecute_TriggersTheJob() throws Exception {
    Map<String, List<String>> map = Map.of(
        "tenant", List.of("some-tenant"),
        "from", List.of(DataStoreType.FILE.name()),
        "to", List.of(DataStoreType.S3.name())
    );

    copyStorageTask.execute(map, null);

    ArgumentCaptor<Map<String, String>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(copyStorageTask), argumentCaptor.capture());
    Map<String, String> value = argumentCaptor.getValue();
    assertThat(value).containsEntry("tenant", "some-tenant");
    assertThat(value).containsEntry("from", DataStoreType.FILE.name());
    assertThat(value).containsEntry("to", DataStoreType.S3.name());
  }

  @Test
  public void testExecuteForTenant_SlugDoesNotMatch() {
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("tenant", "doesNotMatch");
    jobDataMap.put("from", DataStoreType.FILE.name());
    jobDataMap.put("to", DataStoreType.S3.name());
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

    testAsNewTenant("some-tenant", t -> copyStorageTask.executeForTenant(mockJobExecutionContext, t));

    verifyNoInteractions(mockCopyStorageService);
  }

  @Test
  public void testExecuteForTenant_SlugWildcardDoesNotMatch() {
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("tenant", "does.*");
    jobDataMap.put("from", DataStoreType.FILE.name());
    jobDataMap.put("to", DataStoreType.S3.name());
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

    testAsNewTenant("some-tenant", t -> copyStorageTask.executeForTenant(mockJobExecutionContext, t));

    verifyNoInteractions(mockCopyStorageService);
  }

  @Test
  public void testExecuteForTenant_SlugExactMatch() {
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("tenant", "some-tenant");
    jobDataMap.put("from", DataStoreType.FILE.name());
    jobDataMap.put("to", DataStoreType.S3.name());
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

    testAsNewTenant("some-tenant", t -> copyStorageTask.executeForTenant(mockJobExecutionContext, t));

    verify(mockCopyStorageService).execute(DataStoreType.FILE, DataStoreType.S3);
  }

  @Test
  public void testExecuteForTenant_SlugWildcardMatch() {
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("tenant", "some.*");
    jobDataMap.put("from", DataStoreType.FILE.name());
    jobDataMap.put("to", DataStoreType.S3.name());
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

    testAsNewTenant("some-tenant", t -> copyStorageTask.executeForTenant(mockJobExecutionContext, t));

    verify(mockCopyStorageService).execute(DataStoreType.FILE, DataStoreType.S3);
  }

  @Test
  public void testExecuteForTenant_SingleTenant() {
    when(mockTenantUtil.isMultiTenant()).thenReturn(false);
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("from", DataStoreType.FILE.name());
    jobDataMap.put("to", DataStoreType.S3.name());
    when(mockJobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

    copyStorageTask.executeForTenant(mockJobExecutionContext, Tenant.SINGLE_TENANT);

    verify(mockCopyStorageService).execute(DataStoreType.FILE, DataStoreType.S3);
  }

  @Test
  public void testGetJobName() {
    assertThat(copyStorageTask.getJobName()).isEqualTo("CopyStorageTask");
  }
}
