/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeProgressDAO;
import com.sonatype.insight.brain.dataaccess.repository.ReevaluateCascadeRequestDAO;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequestStatus;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReevaluateCascadeRequestCleanerTest
    extends AbstractComponentTest
{
  @Inject
  private ReevaluateCascadeRequestCleaner reevaluateCascadeRequestCleaner;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private ReevaluateCascadeRequestDAO reevaluateCascadeRequestDAOMock;

  @Mock
  private ReevaluateCascadeProgressDAO reevaluateCascadeProgressDAOMock;

  @Mock
  private TransactionContext transactionContextMock;

  @Rule
  public LogOutput logOutput = new LogOutput(ReevaluateCascadeRequestCleaner.class.getName());

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(ReevaluateCascadeRequestDAO.class).toInstance(reevaluateCascadeRequestDAOMock);
    binder.bind(ReevaluateCascadeProgressDAO.class).toInstance(reevaluateCascadeProgressDAOMock);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(ReevaluateCascadeRequestCleaner.class)
        .build()
        .isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testRegister() {
    reevaluateCascadeRequestCleaner.register();

    verify(taskSchedulerMock).schedulePeriodicTask(reevaluateCascadeRequestCleaner,
        ReevaluateCascadeRequestCleaner.PERIOD);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testExecute() {
    String cascadeRequestId1 = "cascade_request_id_1";
    String cascadeRequestId2 = "cascade_request_id_2";
    String componentHash = "status_test_hash";
    ReevaluateCascadeRequest expiredRequest1 = new ReevaluateCascadeRequest(componentHash, "testuser",
        ReevaluateCascadeRequestStatus.COMPLETED);
    expiredRequest1.setId(cascadeRequestId1);
    ReevaluateCascadeRequest expiredRequest2 = new ReevaluateCascadeRequest(componentHash, "testuser",
        ReevaluateCascadeRequestStatus.COMPLETED);
    expiredRequest2.setId(cascadeRequestId2);

    when(reevaluateCascadeRequestDAOMock.createTransactionContext()).thenReturn(transactionContextMock);

    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return Arrays.asList(expiredRequest1, expiredRequest2);
    }).when(reevaluateCascadeRequestDAOMock).findBeforeOrOn(any(TransactionContext.class), any(Date.class));

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      reevaluateCascadeRequestCleaner.execute(mock(JobExecutionContext.class));
    }

    ArgumentCaptor<Date> dateArgumentCaptor = ArgumentCaptor.forClass(Date.class);
    verify(reevaluateCascadeRequestDAOMock).findBeforeOrOn(any(TransactionContext.class),
        dateArgumentCaptor.capture());
    assertThat(dateArgumentCaptor.getValue()).isCloseTo(
        new Date(System.currentTimeMillis() - ReevaluateCascadeRequestCleaner.LIFESPAN.toMillis()), 5000);

    ArgumentCaptor<Set<String>> requestIdsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(reevaluateCascadeRequestDAOMock).deleteByRequestIds(any(TransactionContext.class),
        requestIdsCaptor.capture());
    assertThat(requestIdsCaptor.getValue()).containsExactlyInAnyOrder(cascadeRequestId1, cascadeRequestId2);

    // Progress DAO should not be called directly - cascade delete handles it
    verify(reevaluateCascadeProgressDAOMock, never()).deleteByRequestIds(any(TransactionContext.class), anySet());
    verify(transactionContextMock).begin();
    verify(transactionContextMock).commit();
  }

  @Test
  public void testExecuteWithNoExpiredRequests() {
    when(reevaluateCascadeRequestDAOMock.createTransactionContext()).thenReturn(transactionContextMock);
    when(reevaluateCascadeRequestDAOMock.findBeforeOrOn(any(TransactionContext.class), any(Date.class)))
        .thenReturn(Collections.emptyList());

    reevaluateCascadeRequestCleaner.execute(mock(JobExecutionContext.class));

    verify(reevaluateCascadeRequestDAOMock, never()).deleteByRequestIds(any(TransactionContext.class), anySet());
    verify(reevaluateCascadeProgressDAOMock, never()).deleteByRequestIds(any(TransactionContext.class), anySet());
    verify(transactionContextMock).begin();
    verify(transactionContextMock).commit();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testExecuteWithMixedStatusRequests() {
    String cascadeRequestId1 = "cascade_request_id_pending";
    String cascadeRequestId2 = "cascade_request_id_in_progress";
    String cascadeRequestId3 = "cascade_request_id_completed";
    String componentHash = "mixed_status_test_hash";

    ReevaluateCascadeRequest pendingRequest = new ReevaluateCascadeRequest(componentHash, "testuser",
        ReevaluateCascadeRequestStatus.PENDING);
    pendingRequest.setId(cascadeRequestId1);

    ReevaluateCascadeRequest inProgressRequest = new ReevaluateCascadeRequest(componentHash, "testuser",
        ReevaluateCascadeRequestStatus.IN_PROGRESS);
    inProgressRequest.setId(cascadeRequestId2);

    ReevaluateCascadeRequest completedRequest = new ReevaluateCascadeRequest(componentHash, "testuser",
        ReevaluateCascadeRequestStatus.COMPLETED);
    completedRequest.setId(cascadeRequestId3);

    when(reevaluateCascadeRequestDAOMock.createTransactionContext()).thenReturn(transactionContextMock);

    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return Arrays.asList(pendingRequest, inProgressRequest, completedRequest);
    }).when(reevaluateCascadeRequestDAOMock).findBeforeOrOn(any(TransactionContext.class), any(Date.class));

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      reevaluateCascadeRequestCleaner.execute(mock(JobExecutionContext.class));
    }

    ArgumentCaptor<Date> dateArgumentCaptor = ArgumentCaptor.forClass(Date.class);
    verify(reevaluateCascadeRequestDAOMock).findBeforeOrOn(any(TransactionContext.class),
        dateArgumentCaptor.capture());
    assertThat(dateArgumentCaptor.getValue()).isCloseTo(
        new Date(System.currentTimeMillis() - ReevaluateCascadeRequestCleaner.LIFESPAN.toMillis()), 5000);

    ArgumentCaptor<Set<String>> requestIdsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(reevaluateCascadeRequestDAOMock).deleteByRequestIds(any(TransactionContext.class),
        requestIdsCaptor.capture());
    assertThat(requestIdsCaptor.getValue()).containsExactlyInAnyOrder(
        cascadeRequestId1, cascadeRequestId2, cascadeRequestId3);

    // Progress DAO should not be called directly - cascade delete handles it
    verify(reevaluateCascadeProgressDAOMock, never()).deleteByRequestIds(any(TransactionContext.class), anySet());
    verify(transactionContextMock).begin();
    verify(transactionContextMock).commit();

    // Verify warning logs are generated for PENDING and IN_PROGRESS requests
    assertThat(logOutput).atWarnLevel()
        .contains("Re-evaluate cascade request with ID " + cascadeRequestId1 + " for component hash " + componentHash
            + " has not completed in 24 hours and is being cleaned up. Status: PENDING");
    assertThat(logOutput).atWarnLevel()
        .contains("Re-evaluate cascade request with ID " + cascadeRequestId2 + " for component hash " + componentHash
            + " has not completed in 24 hours and is being cleaned up. Status: IN_PROGRESS");

    // Verify no warning log for COMPLETED request
    assertThat(logOutput).atWarnLevel().doesNotContain(cascadeRequestId3);
  }

  @Test
  public void testGetJobName() {
    assertThat(reevaluateCascadeRequestCleaner.getJobName()).isEqualTo(ReevaluateCascadeRequestCleaner.TASK_NAME);
  }
}
