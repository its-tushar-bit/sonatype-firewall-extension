/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Date;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersistedPolicyEvaluationPollingResultCleanerTest
    extends AbstractComponentTest
{
  @Inject
  private PersistedPolicyEvaluationPollingResultCleaner persistedPolicyEvaluationPollingResultCleaner;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAOMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(PersistedPolicyEvaluationPollingResultDAO.class).toInstance(
        persistedPolicyEvaluationPollingResultDAOMock);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(PersistedPolicyEvaluationPollingResultCleaner.class).build()
        .isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testStart() {
    persistedPolicyEvaluationPollingResultCleaner.register();

    verify(taskSchedulerMock).schedulePeriodicTask(persistedPolicyEvaluationPollingResultCleaner,
        PersistedPolicyEvaluationPollingResultCleaner.PERIOD);
  }

  @Test
  public void testExecute() {
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(persistedPolicyEvaluationPollingResultDAOMock).deleteBeforeOrOn(any());

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      persistedPolicyEvaluationPollingResultCleaner.execute(mock(JobExecutionContext.class));
    }

    ArgumentCaptor<Date> dateArgumentCaptor = ArgumentCaptor.forClass(Date.class);
    verify(persistedPolicyEvaluationPollingResultDAOMock).deleteBeforeOrOn(dateArgumentCaptor.capture());
    assertThat(dateArgumentCaptor.getValue()).isCloseTo(
        new Date(System.currentTimeMillis() - PersistedPolicyEvaluationPollingResultCleaner.LIFESPAN.toMillis()), 5000);
  }
}
