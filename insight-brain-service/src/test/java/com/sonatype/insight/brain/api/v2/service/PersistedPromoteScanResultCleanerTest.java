/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.policy.PersistedPromoteScanResultDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import com.google.inject.Inject;
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

public class PersistedPromoteScanResultCleanerTest
    extends AbstractComponentTest
{
  @Inject
  private PersistedPromoteScanResultCleaner persistedPromoteScanResultCleaner;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private PersistedPromoteScanResultDAO persistedPromoteScanResultDAOMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(PersistedPromoteScanResultDAO.class).toInstance(persistedPromoteScanResultDAOMock);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(PersistedPromoteScanResultCleaner.class).build().isConcurrentExectionDisallowed())
        .isTrue();
  }

  @Test
  public void testStart() {
    persistedPromoteScanResultCleaner.start();

    verify(taskSchedulerMock).schedulePeriodicTask(PersistedPromoteScanResultCleaner.class,
        PersistedPromoteScanResultCleaner.TASK_NAME, PersistedPromoteScanResultCleaner.PERIOD);
  }

  @Test
  public void testExecute() {
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(persistedPromoteScanResultDAOMock).deleteBeforeOrOn(any());

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      persistedPromoteScanResultCleaner.execute(mock(JobExecutionContext.class));
    }

    ArgumentCaptor<Date> dateArgumentCaptor = ArgumentCaptor.forClass(Date.class);
    verify(persistedPromoteScanResultDAOMock).deleteBeforeOrOn(dateArgumentCaptor.capture());
    assertThat(dateArgumentCaptor.getValue())
        .isCloseTo(new Date(System.currentTimeMillis() - PersistedPromoteScanResultCleaner.LIFESPAN.toMillis()), 5000);
  }
}
