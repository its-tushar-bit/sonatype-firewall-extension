/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

@ComponentH2Test
public class RepositoryIdentifiedComponentPurgerTest
    extends AbstractComponentH2Test
{
  @Inject
  private RepositoryIdentifiedComponentPurger repositoryIdentifiedComponentPurger;

  @Mock
  private RepositoryIdentifiedComponentDAO mockRepositoryIdentifiedComponentDAO;

  @Mock
  private TaskScheduler mockTaskScheduler;

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
  public void testExecute_AdminTask() throws Exception {
    repositoryIdentifiedComponentPurger.execute((Map<String, List<String>>) null,
        new PrintWriter(OutputStream.nullOutputStream()));

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
