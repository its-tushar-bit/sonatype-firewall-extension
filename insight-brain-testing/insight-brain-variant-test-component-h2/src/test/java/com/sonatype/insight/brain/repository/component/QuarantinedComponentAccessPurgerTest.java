/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

@ComponentH2Test
public class QuarantinedComponentAccessPurgerTest
    extends AbstractComponentH2Test
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

  @Test
  public void testPurgeObsoleteRecords() {
    // Setup
    final Repository repository = tempEntity.newRepository("repo");
    final ProxyRepositoryComponent proxyRepositoryComponent = tempEntity.newRepositoryComponent(repository.getId());

    for (int i = 0; i < 201; i++) {
      tempEntity.newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId(), daysAgo(63));
    }
    for (int i = 0; i < 10; i++) {
      tempEntity.newQuarantinedComponentAccess(repository.getId(), proxyRepositoryComponent.getId());
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
  public void testExecute_AdminTask() throws Exception {
    quarantinedComponentAccessPurger.execute((Map<String, List<String>>) null,
        new PrintWriter(OutputStream.nullOutputStream()));

    verify(taskSchedulerMock).triggerTaskNow(quarantinedComponentAccessPurger, null);
    verifyNoMoreInteractions(taskSchedulerMock);
  }
}
