/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.Date;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
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

public class QuarantinedComponentAccessPurgerTest
    extends AbstractComponentTest
{
  @Inject
  private QuarantinedComponentAccessPurger quarantinedComponentAccessPurger;

  @Inject
  private QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  @Mock
  private TaskScheduler taskSchedulerMock;

  private Date daysAgo(int days) {
    return Date.from(ZonedDateTime.now().minusDays(days).toInstant());
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
  }

  @Test
  public void testPurgeObsoleteRecords() {
    // Setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());

    for (int i = 0; i < 201; i++) {
      tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), daysAgo(63));
    }
    for (int i = 0; i < 10; i++) {
      tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    }
    quarantinedComponentAccessPurger.purgeObsoleteRecords();
    assertThat(quarantinedComponentAccessDAO.getAll()).hasSize(10);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(QuarantinedComponentAccessPurger.class).build().isConcurrentExectionDisallowed())
        .isTrue();
  }

  @Test
  public void testExecute_QuartzJob() {
    QuarantinedComponentAccessPurger purgerSpy = spy(quarantinedComponentAccessPurger);
    JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
    purgerSpy.execute(mockJobExecutionContext);
    verify(purgerSpy).purgeObsoleteRecords();
  }

  @Test
  public void testExecute_AdminTask() {
    quarantinedComponentAccessPurger.execute(null, new PrintWriter(new StringWriter()));
    verify(taskSchedulerMock).triggerTaskNow(quarantinedComponentAccessPurger, null);
  }
}
