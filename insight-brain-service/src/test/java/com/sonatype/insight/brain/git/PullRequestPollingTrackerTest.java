/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestPollingTrackerTest
    extends AbstractComponentTest
{
  private static final long MS_PER_MINUTE = 60_000;

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  // test subject
  private PullRequestPollingTracker pollingTracker;

  @Before
  public void before() {
    pollingTracker = new PullRequestPollingTracker(sourceControlDAO);
  }

  @Test
  public void testGetNextRepositoryToPoll() {
    // given: two application source control entries
    long now = System.currentTimeMillis();
    SourceControl sourceControl1 = createSourceControl();
    sourceControl1.setPullRequestPollTime(new Date(now - 2000));
    sourceControlDAO.update(sourceControl1);
    SourceControl sourceControl2 = createSourceControl();
    sourceControl2.setPullRequestPollTime(new Date(now - 1000));
    sourceControlDAO.update(sourceControl2);

    // when: get next repo to poll
    SourceControl sourceControl = pollingTracker.getNextRepositoryToPoll();

    // then: expecting source control 1
    assertThat(sourceControl.getId()).isEqualTo(sourceControl1.getId());

    // Simulate that sourceControl1 was processed
    sourceControl1.setPullRequestPollTime(new Date(now));
    sourceControlDAO.update(sourceControl1);

    // when: get next repo to poll
    sourceControl = pollingTracker.getNextRepositoryToPoll();

    // then: expecting source control 2
    assertThat(sourceControl.getId()).isEqualTo(sourceControl2.getId());

    // when: get next repo to poll
    sourceControl = pollingTracker.getNextRepositoryToPoll();

    // then: we've already seen both source controls in this tracker so expecting null now
    assertThat(sourceControl).isNull();
  }

  @Test
  public void testOnPullRequestProcessed() {
    // given: source control entry with initial values
    Date date = new Date();
    SourceControl sourceControl = createSourceControl();
    sourceControl.setPullRequestErrorCount(3);
    sourceControlDAO.update(sourceControl);

    // when: update poll times called
    pollingTracker.onPullRequestProcessed(sourceControl.getId(), "org", "repo", "token", date);

    // then: verify dates and error count
    sourceControl = sourceControlDAO.getById(sourceControl.getId());
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(date);
    assertThat(sourceControl.getPullRequestErrorCount()).isEqualTo(0);

    // and: cutoff time is correct
    Date cutoff = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24));
    assertThat(pollingTracker.getCachedCutoffTime("org", "repo", "token", cutoff)).isAfter(cutoff);
    assertThat(pollingTracker.getCachedCutoffTime("org2", "repo", "token2", cutoff)).isEqualTo(cutoff);
  }

  @Test
  public void testOnPullRequestProcessed_withNullRepo() {
    // given: source control entry with initial values
    Date date = new Date();
    SourceControl sourceControl = createSourceControl();
    sourceControl.setPullRequestErrorCount(3);
    sourceControlDAO.update(sourceControl);

    // when: update poll times called
    pollingTracker.onPullRequestProcessed(sourceControl.getId(), "org", null, "token", date);

    // then: verify dates and error count
    sourceControl = sourceControlDAO.getById(sourceControl.getId());
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(date);
    assertThat(sourceControl.getPullRequestErrorCount()).isEqualTo(0);

    // and: cutoff time is correct
    Date cutoff = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24));
    assertThat(pollingTracker.getCachedCutoffTime("org", null, "token", cutoff)).isAfter(cutoff);
    assertThat(pollingTracker.getCachedCutoffTime("org2", null, "token2", cutoff)).isEqualTo(cutoff);
  }

  @Test
  public void testOnPullRequestProcessed_forApplication() {
    // given: source control entry for app with initial values
    Date date = new Date();
    SourceControl sourceControl = createSourceControl();
    sourceControl.setPullRequestErrorCount(3);
    sourceControlDAO.update(sourceControl);

    // when: update poll times called
    pollingTracker.onPullRequestProcessedForApplication(sourceControl.getOwnerId(), date);

    // then: verify poll dates set correctly as well as error count
    sourceControl = sourceControlDAO.getById(sourceControl.getId());
    assertThat(sourceControl.getPullRequestPollTime()).isEqualTo(date);
  }

  @Test
  public void testOnErrorProcessingPullRequests() {
    // given: initialized source control entry
    final List<Integer> expectedErrorOffsetsInMinutes = ImmutableList.of(5, 10, 15, 30, 60, 360, 720, 60 * 24, 60 * 24);
    final List<String> expectedErrorOffsetText = ImmutableList.of("5 minutes", "10 minutes", "15 minutes", "30 minutes",
        "1 hour", "6 hours", "12 hours", "24 hours", "24 hours");
    Date cutoffTime = new Date();
    SourceControl sourceControl = createSourceControl();
    sourceControl.setPullRequestErrorCount(0);
    sourceControl.setPullRequestPollTime(cutoffTime);
    sourceControlDAO.update(sourceControl);

    for (int i = 0; i < expectedErrorOffsetsInMinutes.size(); i++) {
      // when: report error
      String offsetMessage = pollingTracker.onErrorProcessingPullRequests(sourceControl.getId());

      // then: error count incremented, cutoff unchanged, poll time updated per sequence
      long exactOffset = currentTimeMillis() + (MS_PER_MINUTE * expectedErrorOffsetsInMinutes.get(i));
      // bound the expected poll time by +/- 100ms
      Date minPollTime = new Date(exactOffset - 100);
      Date maxPollTime = new Date(exactOffset + 100);
      assertThat(offsetMessage).isEqualTo(expectedErrorOffsetText.get(i));
      sourceControl = sourceControlDAO.getById(sourceControl.getId());
      assertThat(sourceControl.getPullRequestPollTime()).isBetween(minPollTime, maxPollTime);
      assertThat(sourceControl.getPullRequestErrorCount()).isEqualTo(i + 1);
    }
  }

  @Test
  public void testVisitAndCheckKeyAlreadyUsed() {
    // when: visit and key combo used for first time
    boolean visited = pollingTracker.visitAndCheckKeyAlreadyUsed("org", "repo","token");

    // then: shouldn't have been visited yet
    assertThat(visited).isFalse();

    // when: visit again
    visited = pollingTracker.visitAndCheckKeyAlreadyUsed("org", "repo", "token");

    // then: should indicate was visited already
    assertThat(visited).isTrue();
  }

  @Test
  public void testVisitAndCheckKeyAlreadyUsed_withNullRepo() {
    // when: visit and key combo used for first time
    boolean visited = pollingTracker.visitAndCheckKeyAlreadyUsed("org", null, "token");

    // then: shouldn't have been visited yet
    assertThat(visited).isFalse();

    // when: visit again
    visited = pollingTracker.visitAndCheckKeyAlreadyUsed("org", null, "token");

    // then: should indicate was visited already
    assertThat(visited).isTrue();
  }

  private SourceControl createSourceControl() {
    Application app = tempEntity.newApplicationWithParent();
    return tempEntity.newSourceControl(app.getId(), "http://localhost/test/" + app.getId(), "testToken",
        SourceControlProvider.GITHUB);
  }
}
