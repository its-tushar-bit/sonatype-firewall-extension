/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class PullRequestStateServiceTest
    extends AbstractComponentH2Test
{
  private static final String REPO_URL_WITH_BATCH_SUPPORT = "https://github.com/test/repo";

  private static final String REPO_URL_WITHOUT_BATCH_SUPPORT = "https://bitbucket.org/scm/test/repo";

  @Inject
  private PullRequestStateService pullRequestStateService;

  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Inject
  private SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private Application applicationWithBatchSupport;

  private Application applicationWithoutBatchSupport;

  @BeforeEach
  public void setup() {
    // Create two applications with different SCM providers
    applicationWithBatchSupport = tempEntity.newApplicationWithParent();
    applicationWithoutBatchSupport = tempEntity.newApplicationWithParent();

    // Create source control entries for each application
    // GitHub supports batch fetch
    tempEntity.newSourceControl(
        applicationWithBatchSupport.getId(),
        REPO_URL_WITH_BATCH_SUPPORT,
        "token",
        SourceControlProvider.GITHUB);

    // For this test, we'll use BITBUCKET as a provider that doesn't support batch fetch
    tempEntity.newSourceControl(
        applicationWithoutBatchSupport.getId(),
        REPO_URL_WITHOUT_BATCH_SUPPORT,
        "token",
        SourceControlProvider.BITBUCKET);
  }

  @Test
  public void testDispatchPullRequestStateUpdateEvents_WithBatchSupportProvider() {
    // Create several pull requests for the application with batch support
    for (int i = 1; i <= 5; i++) {
      createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, i, PullRequestState.OPEN, PullRequestSource.AUTOMATIC);
    }

    // Run the service method
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();

    // Verify batch events were created for the application with batch support
    List<SourceControlEvent> events = sourceControlEventDAO.getAll();

    // Should have created a single batch event
    assertThat(events).satisfiesExactly(onlyEvent -> {
      assertThat(onlyEvent.getApplicationId()).isEqualTo(applicationWithBatchSupport.getId());
      assertThat(onlyEvent.getEventType()).isEqualTo(SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT);
      assertThat(onlyEvent.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_NEW);

      assertThat(parseDetails(onlyEvent)).containsExactly(1, 2, 3, 4, 5);
    });
  }

  @Test
  public void testDispatchPullRequestStateUpdateEvents_BatchCountLimit() {
    // Create several pull requests for the application with batch support
    PullRequestSource[] sources = {
      PullRequestSource.AUTOMATIC,
      PullRequestSource.AUTOMATIC_INNER_SOURCE,
      PullRequestSource.MANUAL,
      PullRequestSource.MANUAL_INNER_SOURCE
    };
    for (int i = 1; i <= 105; i++) {
      // Alternate between AUTOMATIC, AUTOMATIC_INNER_SOURCE, MANUAL, and MANUAL_INNER_SOURCE (all should be processed)
      PullRequestSource source = sources[i % 4];
      createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, i, PullRequestState.OPEN, source);
    }

    // Run the service method
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();

    // Verify batch events were created for the application with batch support
    List<SourceControlEvent> events = sourceControlEventDAO.getAll();

    // Should have created two events
    assertThat(events).hasSize(2);
    assertThat(events).allSatisfy(event -> {
      assertThat(event.getApplicationId()).isEqualTo(applicationWithBatchSupport.getId());
      assertThat(event.getEventType()).isEqualTo(SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT);
      assertThat(event.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_NEW);
    });

    int[] allEventPRIds = events.stream()
        .map(this::parseDetails)
        .flatMapToInt(Arrays::stream)
        .toArray();

    for (int i = 1; i < 105; i++) {
      assertThat(allEventPRIds).contains(i);
    }
  }

  @Test
  public void testDispatchPullRequestStateUpdateEvents_WithoutBatchSupportProvider() {
    // Create several pull requests for the application without batch support
    for (int i = 1; i <= 3; i++) {
      // Use AUTOMATIC source for all PRs in this test
      createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, i, PullRequestState.OPEN, PullRequestSource.AUTOMATIC);
    }

    // Run the service method
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();

    // Verify individual events were created for the application without batch support
    List<SourceControlEvent> events = sourceControlEventDAO.getAll();

    // Should have created individual events for each PR
    assertThat(events).extracting(SourceControlEvent::getPullRequestNumber).containsExactlyInAnyOrder(1, 2, 3);

    // Verify each event is a regular PR state update event
    assertThat(events).allSatisfy(event -> {
      assertThat(event.getApplicationId()).isEqualTo(applicationWithoutBatchSupport.getId());
      assertThat(event.getEventType()).isEqualTo(SourceControlEvent.PR_STATE_UPDATE_EVENT);
      assertThat(event.getEventStatus()).isEqualTo(SourceControlEvent.EVENT_STATUS_NEW);
    });

    assertThat(events).filteredOn(e -> e.getPullRequestNumber() == 1)
        .first()
        .extracting(SourceControlEvent::getBranchName, SourceControlEvent::getBaseBranchName)
        .containsExactly("branch1", "baseBranch1");

    assertThat(events).filteredOn(e -> e.getPullRequestNumber() == 2)
        .first()
        .extracting(SourceControlEvent::getBranchName, SourceControlEvent::getBaseBranchName)
        .containsExactly("branch2", "baseBranch2");

    assertThat(events).filteredOn(e -> e.getPullRequestNumber() == 3)
        .first()
        .extracting(SourceControlEvent::getBranchName, SourceControlEvent::getBaseBranchName)
        .containsExactly("branch3", "baseBranch3");
  }

  @Test
  public void testDispatchPullRequestStateUpdateEvents_MixedProviderSupport() {
    // Create pull requests for both applications
    // Explicit AUTOMATIC, AUTOMATIC_INNER_SOURCE, MANUAL, and MANUAL_INNER_SOURCE (these should be processed)
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 1, PullRequestState.OPEN, PullRequestSource.AUTOMATIC);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 2, PullRequestState.OPEN, PullRequestSource.AUTOMATIC_INNER_SOURCE);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 3, PullRequestState.OPEN, PullRequestSource.MANUAL);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 4, PullRequestState.OPEN, PullRequestSource.MANUAL_INNER_SOURCE);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 5, PullRequestState.OPEN, PullRequestSource.AUTOMATIC);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 6, PullRequestState.OPEN,
        PullRequestSource.AUTOMATIC_INNER_SOURCE);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 7, PullRequestState.OPEN, PullRequestSource.MANUAL);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 8, PullRequestState.OPEN, PullRequestSource.MANUAL_INNER_SOURCE);

    // Run the service method
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();

    // Get all events
    List<SourceControlEvent> allEvents = sourceControlEventDAO.getAll();
    assertThat(allEvents).hasSize(5);

    assertThat(allEvents).filteredOn(e -> e.getEventType().equals(SourceControlEvent.PR_STATE_UPDATE_EVENT))
        .hasSize(4)
        .extracting(SourceControlEvent::getPullRequestNumber)
        .containsExactlyInAnyOrder(5, 6, 7, 8);

    assertThat(allEvents).filteredOn(e -> e.getEventType().equals(SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT))
        .hasSize(1)
        .first()
        .extracting(this::parseDetails)
        .asInstanceOf(InstanceOfAssertFactories.INT_ARRAY)
        .containsExactlyInAnyOrder(1, 2, 3, 4);
  }

  @Test
  public void testDispatchPullRequestStateUpdateEvents_NonOpenPullRequests() {
    // Create open and non-open pull requests for both applications
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 1, PullRequestState.OPEN, PullRequestSource.AUTOMATIC);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 2, PullRequestState.CLOSED, PullRequestSource.AUTOMATIC);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 3, PullRequestState.MERGED, PullRequestSource.AUTOMATIC);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 4, PullRequestState.LOCKED, PullRequestSource.AUTOMATIC);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 5, PullRequestState.OPEN, PullRequestSource.MANUAL);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 6, PullRequestState.CLOSED, PullRequestSource.MANUAL);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 7, PullRequestState.MERGED, PullRequestSource.MANUAL);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 8, PullRequestState.LOCKED, PullRequestSource.MANUAL);

    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 9, PullRequestState.OPEN, PullRequestSource.AUTOMATIC_INNER_SOURCE);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 10, PullRequestState.CLOSED,
        PullRequestSource.AUTOMATIC_INNER_SOURCE);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 11, PullRequestState.MERGED,
        PullRequestSource.AUTOMATIC_INNER_SOURCE);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 12, PullRequestState.LOCKED,
        PullRequestSource.AUTOMATIC_INNER_SOURCE);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 13, PullRequestState.OPEN, PullRequestSource.MANUAL_INNER_SOURCE);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 14, PullRequestState.CLOSED,
        PullRequestSource.MANUAL_INNER_SOURCE);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 15, PullRequestState.MERGED,
        PullRequestSource.MANUAL_INNER_SOURCE);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 16, PullRequestState.LOCKED,
        PullRequestSource.MANUAL_INNER_SOURCE);

    // Run the service method
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();

    // Get all events
    List<SourceControlEvent> allEvents = sourceControlEventDAO.getAll();
    assertThat(allEvents).hasSize(3);

    assertThat(allEvents).filteredOn(e -> e.getEventType().equals(SourceControlEvent.PR_STATE_UPDATE_EVENT))
        .hasSize(2)
        .extracting(SourceControlEvent::getPullRequestNumber)
        .containsExactly(5, 13);

    assertThat(allEvents).filteredOn(e -> e.getEventType().equals(SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT))
        .hasSize(1)
        .first()
        .extracting(this::parseDetails)
        .asInstanceOf(InstanceOfAssertFactories.INT_ARRAY)
        .containsExactly(1, 9);
  }

  @Test
  public void testDispatchPullRequestStateUpdateEvents_DoesNotCreateDuplicateEvents() {
    // Create PRs for application with batch support
    for (int i = 1; i <= 5; i++) {
      createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, i, PullRequestState.OPEN, PullRequestSource.AUTOMATIC);
    }

    // First run - should create events for all PRs
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();

    // Verify events were created
    List<SourceControlEvent> events = sourceControlEventDAO.getAll();
    assertThat(events).hasSize(1); // One batch event for all 5 PRs

    SourceControlEvent batchEvent = events.get(0);
    assertThat(batchEvent.getEventType()).isEqualTo(SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT);
    assertThat(parseDetails(batchEvent)).containsExactlyInAnyOrder(1, 2, 3, 4, 5);

    // Create new PRs mixed with existing ones
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 6, PullRequestState.OPEN, PullRequestSource.MANUAL);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 7, PullRequestState.OPEN, PullRequestSource.MANUAL);

    // Second run - should only create events for new PRs
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();

    // Verify new events were created only for PRs 6 and 7
    events = sourceControlEventDAO.getAll();
    assertThat(events).hasSize(2); // Original batch event + new batch event for PRs 6 and 7

    assertThat(events).satisfiesExactlyInAnyOrder(
        e1 -> {
          assertThat(e1.getEventType()).isEqualTo(SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT);
          assertThat(parseDetails(e1)).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        },
        e2 -> {
          assertThat(e2.getEventType()).isEqualTo(SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT);
          assertThat(parseDetails(e2)).containsExactlyInAnyOrder(6, 7);
        });
  }

  @Test
  public void testDispatchPullRequestStateUpdateEvents_DoesNotCreateDuplicateEventsForNonBatchProvider() {
    // Create PRs for application without batch support
    for (int i = 1; i <= 3; i++) {
      createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, i, PullRequestState.OPEN, PullRequestSource.MANUAL);
    }

    // First run - should create individual events for all PRs
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();

    // Verify individual events were created
    List<SourceControlEvent> events = sourceControlEventDAO.getAll();
    assertThat(events).hasSize(3); // Three individual events
    assertThat(events).extracting(SourceControlEvent::getPullRequestNumber).containsExactlyInAnyOrder(1, 2, 3);

    // Create a new PR
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 4, PullRequestState.OPEN, PullRequestSource.MANUAL);

    // Second run - should only create an event for PR #4
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();

    // Verify only one new event was created
    events = sourceControlEventDAO.getAll();
    assertThat(events).hasSize(4); // Original three events + one new event

    assertThat(events).extracting(SourceControlEvent::getPullRequestNumber).containsExactlyInAnyOrder(1, 2, 3, 4);
  }

  @Test
  public void testDispatchPullRequestStateUpdateEvents_IgnoresExternalPullRequests() {
    // Create pull requests with different sources
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 1, PullRequestState.OPEN, PullRequestSource.AUTOMATIC);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 2, PullRequestState.OPEN, PullRequestSource.AUTOMATIC_INNER_SOURCE);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 3, PullRequestState.OPEN, PullRequestSource.MANUAL);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 4, PullRequestState.OPEN, PullRequestSource.MANUAL_INNER_SOURCE);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 5, PullRequestState.OPEN, PullRequestSource.EXTERNAL);
    createPullRequest(REPO_URL_WITH_BATCH_SUPPORT, 6, PullRequestState.OPEN, null); // null == EXTERNAL

    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 7, PullRequestState.OPEN, PullRequestSource.AUTOMATIC);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 8, PullRequestState.OPEN,
        PullRequestSource.AUTOMATIC_INNER_SOURCE);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 9, PullRequestState.OPEN, PullRequestSource.MANUAL);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 10, PullRequestState.OPEN, PullRequestSource.MANUAL_INNER_SOURCE);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 11, PullRequestState.OPEN, PullRequestSource.EXTERNAL);
    createPullRequest(REPO_URL_WITHOUT_BATCH_SUPPORT, 12, PullRequestState.OPEN, null); // null == EXTERNAL

    // Run the service method
    pullRequestStateService.dispatchPullRequestStateUpdateEvents();

    // Get all events
    List<SourceControlEvent> allEvents = sourceControlEventDAO.getAll();

    // Verify events were created only for
    // AUTOMATIC, AUTOMATIC_INNER_SOURCE, MANUAL, and MANUAL_INNER_SOURCE pull requests
    assertThat(allEvents).filteredOn(e -> e.getEventType().equals(SourceControlEvent.PR_STATE_UPDATE_EVENT))
        .hasSize(4)
        .extracting(SourceControlEvent::getPullRequestNumber)
        .containsExactlyInAnyOrder(7, 8, 9, 10);

    // Verify batch event contains only AUTOMATIC and MANUAL PRs (1 and 2), not EXTERNAL ones (3 and 4)
    assertThat(allEvents).filteredOn(e -> e.getEventType().equals(SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT))
        .hasSize(1)
        .satisfiesExactly(event -> {
          assertThat(parseDetails(event)).containsExactly(1, 2, 3, 4);
        });
  }

  private void createPullRequest(
      String repositoryUrl,
      int pullRequestId,
      PullRequestState state,
      PullRequestSource source)
  {
    SourceControlPullRequest pullRequest = tempEntity.newSourceControlPullRequest(
        repositoryUrl,
        pullRequestId,
        "headCommitHash" + pullRequestId,
        "baseCommitHash" + pullRequestId,
        "branch" + pullRequestId,
        "baseBranch" + pullRequestId);
    pullRequest.setState(state);
    pullRequest.setSource(source);
    sourceControlPullRequestDAO.update(pullRequest);
  }

  private int[] parseDetails(SourceControlEvent event) {
    try {
      return JsonUtils.parse(event.getEventStatusDetails(), int[].class);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
