/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;
import java.util.List;

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

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestPollingTrackerTest
{
  private static final long MS_PER_MINUTE = 60_000;

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
  public void testOnPullRequestProcessed_forPullRequest() {
    // given: source control entry and DAO setup to return expected values
    Date oldPollDate = new Date(currentTimeMillis() - 30_000);
    SourceControl sourceControl = createSourceControl("sc1");
    sourceControl.setPullRequestPollTime(oldPollDate);
    sourceControl.setPullRequestErrorCount(5);
    doReturn(ImmutableList.of(sourceControl)).when(sourceControlDAO).getByRepositoryOwnerAndName("org/yes");
    doReturn(null).when(sourceControlDAO).getByRepositoryOwnerAndName("org/no");

    // when: update entry WITHOUT matching repo
    PullRequest pullRequest = new GithubPullRequest();
    pullRequest.setRepository("org/no");
    boolean updated = pollingTracker.onPullRequestProcessed(pullRequest);

    // then: should NOT have been updated
    verify(sourceControlDAO, never()).update(any(SourceControl.class));
    assertThat(updated).isFalse();

    // when: update entry WITH matching repo
    pullRequest.setRepository("org/yes");
    Date prCreated = new Date();
    pullRequest.setCreated(prCreated);
    updated = pollingTracker.onPullRequestProcessed(pullRequest);

    // then: should have been updated and errors cleared
    assertThat(updated).isTrue();
    verify(sourceControlDAO, times(1)).updatePollTimeAndErrorCounts(sourceControl.getId(), prCreated, 0);
  }

  @Test
  public void testOnPullRequestProcessed() {
    // given: source control entry with initial values
    Date date = new Date();
    String sourceControlId = "sc1";
    SourceControl sourceControl = createSourceControl(sourceControlId);
    sourceControl.setPullRequestErrorCount(3);

    // when: update poll times called
    pollingTracker.onPullRequestProcessed(sourceControlId, "org", "token", date);

    // then: verify dates and error count
    verify(sourceControlDAO, times(1)).updatePollTimeAndErrorCounts(sourceControl.getId(), date, 0);

    // and: cutoff time is correct
    Date cutoff = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24));
    assertThat(pollingTracker.getCachedCutoffTime("org", "token", cutoff)).isAfter(cutoff);
    assertThat(pollingTracker.getCachedCutoffTime("org2", "token2", cutoff)).isEqualTo(cutoff);
  }

  @Test
  public void testOnPullRequestProcessed_forApplication() {
    // given: source control entry for app with initial values
    Date date = new Date();
    String sourceControlId = "sc1";
    String appId = "app1";
    SourceControl sourceControl = createSourceControl(sourceControlId);
    sourceControl.setOwnerId(appId);
    sourceControl.setPullRequestErrorCount(3);
    doReturn(sourceControl).when(sourceControlDAO).getByOwnerId(appId);

    // when: update poll times called
    pollingTracker.onPullRequestProcessedForApplication(appId, date);

    // then: verify poll dates set correctly as well as error count
    verify(sourceControlDAO, times(1)).updatePollTimeAndErrorCounts(sourceControl.getId(), date, 0);
  }

  @Test
  public void testOnErrorProcessingPullRequests() {
    // given: initialized source control entry
    final List<Integer> expectedErrorOffsetsInMinutes = ImmutableList.of(5, 10, 15, 30, 60, 360, 720, 60 * 24, 60 * 24);
    final List<String> expectedErrorOffsetText = ImmutableList.of("5 minutes", "10 minutes", "15 minutes", "30 minutes",
        "1 hour", "6 hours", "12 hours", "24 hours", "24 hours");
    Date cutoffTime = new Date();
    String sourceControlId = "scError";
    SourceControl sourceControl = createSourceControl(sourceControlId);
    sourceControl.setPullRequestErrorCount(0);
    sourceControl.setPullRequestPollTime(cutoffTime);
    doReturn(sourceControl).when(sourceControlDAO).getById(sourceControlId);
    doAnswer(invocationOnMock -> {
      sourceControl.setPullRequestPollTime(invocationOnMock.getArgument(1));
      sourceControl.setPullRequestErrorCount(invocationOnMock.getArgument(2));
      return null;
    }).when(sourceControlDAO).updatePollTimeAndErrorCounts(eq(sourceControlId), any(), anyInt());

    for (int i = 0; i < expectedErrorOffsetsInMinutes.size(); i++) {
      // when: report error
      String offsetMessage = pollingTracker.onErrorProcessingPullRequests(sourceControlId);

      // then: error count incremented, cutoff unchanged, poll time updated per sequence
      long exactOffset = currentTimeMillis() + (MS_PER_MINUTE * expectedErrorOffsetsInMinutes.get(i));
      // bound the expected poll time by +/- 100ms
      Date minPollTime = new Date(exactOffset - 100);
      Date maxPollTime = new Date(exactOffset + 100);
      assertThat(offsetMessage).isEqualTo(expectedErrorOffsetText.get(i));
      verify(sourceControlDAO, times(1)).updatePollTimeAndErrorCounts(eq(sourceControl.getId()),
          argThat(date -> date.after(minPollTime) && date.before(maxPollTime)), eq(i + 1));
    }
  }

  @Test
  public void testInitializePullRequestPollTimes() {
    // when: update poll times called
    pollingTracker.initializePullRequestPollTimes();

    // then: verify DAO called;  I know, this seems trivial and whitebox-ish but (a) serves as a placeholder in case
    //       implementation changes in the future and (b) gives us code coverage
    verify(sourceControlDAO, times(1)).initializePullRequestPollTimes();
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
