/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Date;

public class PullRequestStateUpdateJobTest
    extends AbstractComponentTest
{
  @Mock
  private TaskScheduler taskScheduler;

  @Inject
  private PullRequestStateService pullRequestStateService;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  private PullRequestStateUpdateJob pullRequestStateUpdateJob;

  private Application application;

  private String repositoryUrl = "https://example.com/scm/test/repo";

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(TaskScheduler.class).toInstance(taskScheduler);
  }

  @Before
  public void setup() {
    // Create the job after the mocks are set up
    pullRequestStateUpdateJob = new PullRequestStateUpdateJob(taskScheduler, pullRequestStateService);

    application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, "", SourceControlProvider.BITBUCKET);
    tempEntity.newSourceControl(application.getId(), repositoryUrl);
    applicationDAO.update(application);
  }

  @Test
  public void testRegister() {
    // Test that the job registers itself with the task scheduler
    pullRequestStateUpdateJob.register();

    // Verify job was scheduled with the correct period
    verify(taskScheduler).schedulePeriodicTask(eq(pullRequestStateUpdateJob), eq(Duration.ofDays(1)));
  }

  @Test
  public void testExecute() throws Exception {
    // Create an open pull request to verify the job processes it
    tempEntity.newSourceControlPullRequest(
        repositoryUrl,
        1,
        "headCommitHash1",
        "baseCommitHash1",
        "branch1",
        "baseBranch1",
        new Date(),
        new Date(),
        new Date(),
        PullRequestState.OPEN,
        PullRequestSource.MANUAL);

    // Execute the job directly (without going through the scheduler)
    pullRequestStateUpdateJob.execute(mock(JobExecutionContext.class));

    // Get all PR state update events
    var events = sourceControlEventDAO.getAll();

    // There should be one event
    assertThat(events).satisfiesExactly(event -> {
      assertThat(event.getApplicationId()).isEqualTo(application.getId());
      assertThat(event.getEventType()).isEqualTo(SourceControlEvent.PR_STATE_UPDATE_EVENT);
      assertThat(event.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_NEW);
      assertThat(event.getPullRequestNumber()).isEqualTo(1);
    });
  }
}
