/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.nexus.scm.api.model.PullRequest;
import com.sonatype.nexus.scm.github.dto.GithubPullRequest;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestPollingTrackerTest
{
  @Mock
  SourceControlDAO sourceControlDAO;

  // test subject
  PullRequestPollingTracker pollingTracker;

  @Before
  public void before() {
    pollingTracker = new PullRequestPollingTracker(sourceControlDAO);
  }

  @Test
  public void testGetNextRepositoryToPoll() {
    // given: two application source control entries
    SourceControl sourceControl1 = createSourceControl("sc1");
    SourceControl sourceControl2 = createSourceControl("sc2");
    // cycle thru source controls and start to repeat
    doReturn(sourceControl1, sourceControl2, sourceControl1).when(sourceControlDAO).getNextRepositoryToPoll();

    // when: get next repo to poll
    SourceControl sourceControl = pollingTracker.getNextRepositoryToPoll();

    // then: expecting source control 1
    assertThat(sourceControl.getId()).isEqualTo("sc1");

    // when: get next repo to poll
    sourceControl = pollingTracker.getNextRepositoryToPoll();

    // then: expecting source control 2
    assertThat(sourceControl.getId()).isEqualTo("sc2");

    // when: get next repo to poll
    sourceControl = pollingTracker.getNextRepositoryToPoll();

    // then: we've already seen both source controls in this tracker so expecting null now
    assertThat(sourceControl).isNull();
  }

  @Test
  public void testUpdateSourceControlPollTimeFromPullRequest() {
    // given: source control entry and DAO setup to return expected values
    SourceControl sourceControl = createSourceControl("sc1");
    doReturn(ImmutableList.of(sourceControl)).when(sourceControlDAO).getByRepositoryOwnerAndName("org/yes");
    doReturn(null).when(sourceControlDAO).getByRepositoryOwnerAndName("org/no");

    // when: update entry WITH match
    PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setRepository("org/yes");
    boolean updated = pollingTracker.updateSourceControlPollTimeFromPullRequest(pullRequest);

    // then: should have been updated
    assertThat(updated).isTrue();

    // when: update entry WITHOUT match
    pullRequest.setRepository("org/no");
    updated = pollingTracker.updateSourceControlPollTimeFromPullRequest(pullRequest);

    // then: should NOT have been updated
    assertThat(updated).isFalse();
  }

  @Test
  public void testUpdatePullRequestPollTime() {
    // given:
    Date date = new Date();
    String sourceControlId = "sc1";

    // when: update poll times called
    pollingTracker.updateSourceControlPollTime(sourceControlId, date);

    // then: verify DAO called;  I know, this seems trivial and whitebox-ish but (a) serves as a placeholder in case
    //       implementation changes in the future and (b) gives us code coverage
    verify(sourceControlDAO, times(1)).updatePullRequestPollTime(sourceControlId, date);
  }

  @Test
  public void testUpdateSourceControlPollTimeForApplication() {
    // given:
    Date date = new Date();
    String sourceControlId = "sc1";

    // when: update poll times called
    pollingTracker.updateSourceControlPollTimeForApplication(sourceControlId, date);

    // then: verify DAO called;  I know, this seems trivial and whitebox-ish but (a) serves as a placeholder in case
    //       implementation changes in the future and (b) gives us code coverage
    verify(sourceControlDAO, times(1)).updatePullRequestPollTimeForApplication(sourceControlId, date);
  }

  @Test
  public void testUpdatePullRequestPollTimes() {
    // when: update poll times called
    pollingTracker.updatePullRequestPollTimes();

    // then: verify DAO called;  I know, this seems trivial and whitebox-ish but (a) serves as a placeholder in case
    //       implementation changes in the future and (b) gives us code coverage
    verify(sourceControlDAO, times(1)).updatePullRequestPollTimes();
  }

  @Test
  public void testVisitAndCheckOrganizationWithToken() {
    // when: visit and org/token combo for first time
    boolean visited = pollingTracker.visitAndCheckOrganizationWithToken("org", "token");

    // then: shouldn't have been visited yet
    assertThat(visited).isFalse();

    // when: visit again
    visited = pollingTracker.visitAndCheckOrganizationWithToken("org", "token");

    // then: should indicate was visited already
    assertThat(visited).isTrue();
  }

  private SourceControl createSourceControl(String sourceControlId) {
    SourceControl sourceControl = new SourceControl();
    sourceControl.setId(sourceControlId);
    return sourceControl;
  }
}
