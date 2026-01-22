/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestMonitorTaskTest
    extends AbstractComponentTest
{
  @Mock
  private TaskScheduler taskSchedulerMock;

  @Inject
  private PullRequestMonitor underTest;

  @Override
  public void configure(final Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
  }

  @Test
  public void testExecute_dropWizardTaskExecuted_shouldTriggerNow() throws Exception {
    final StringWriter writer = new StringWriter();
    underTest.execute(null, new PrintWriter(writer));
    verify(taskSchedulerMock).triggerTaskNow(underTest, null);
    assertThat(writer.toString()).isEqualTo("Triggered monitoring for all PRs");
  }
}
