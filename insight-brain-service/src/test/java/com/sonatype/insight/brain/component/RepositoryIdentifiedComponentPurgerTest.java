/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class RepositoryIdentifiedComponentPurgerTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryIdentifiedComponentPurger repositoryIdentifiedComponentPurger;

  @Mock
  private RepositoryIdentifiedComponentDAO mockRepositoryIdentifiedComponentDAO;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(RepositoryIdentifiedComponentDAO.class).toInstance(mockRepositoryIdentifiedComponentDAO);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(
        JobBuilder.newJob(RepositoryIdentifiedComponentPurger.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testStart_Disabled() {
    repositoryIdentifiedComponentPurger.disableForTesting = true;

    repositoryIdentifiedComponentPurger.register();

    verifyNoInteractions(mockTaskScheduler);
  }

  @Test
  public void testStart() {
    repositoryIdentifiedComponentPurger.register();

    verify(mockTaskScheduler).scheduleDailyTask(repositoryIdentifiedComponentPurger,
        RepositoryIdentifiedComponentPurger.EXECUTION_TIME);
  }

  @Test
  public void testExecute_AdminTask() {
    repositoryIdentifiedComponentPurger.execute(null, new PrintWriter(new StringWriter()));
    verify(mockTaskScheduler).triggerTaskNow(repositoryIdentifiedComponentPurger, null);
  }

  @Test
  public void testExecute_QuartzJob() {
    RepositoryIdentifiedComponentPurger spyRepositoryIdentifiedComponentPurger =
        spy(repositoryIdentifiedComponentPurger);
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    spyRepositoryIdentifiedComponentPurger.execute(mockJobExecutionContext);
    verify(spyRepositoryIdentifiedComponentPurger).purgeRepositoryIdentifiedComponents();
  }

  @Test
  public void testPurgeRepositoryIdentifiedComponents() {
    repositoryIdentifiedComponentPurger.purgeRepositoryIdentifiedComponents();

    verify(mockRepositoryIdentifiedComponentDAO).deleteInfrequentlyAccessed(
        RepositoryIdentifiedComponentPurger.MAX_LAST_ACCESSED);
  }
}
