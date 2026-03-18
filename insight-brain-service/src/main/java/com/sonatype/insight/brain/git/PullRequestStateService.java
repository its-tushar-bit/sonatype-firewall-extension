/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for creating SourceControlEvents that trigger updates to open SourceControlPullRequests
 */
@Named
@Singleton
public class PullRequestStateService
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestStateService.class);

  // This limit comes from nexus-scm-client. It'd be good if that library exposed this as a constant, but it doesn't
  private static final int BATCH_SIZE = 100;

  // We only want to update the states of IQ-generated PRs, so external ones are excluded
  public static final Set<PullRequestSource> RELEVANT_PR_SOURCES = EnumSet.of(
      PullRequestSource.AUTOMATIC,
      PullRequestSource.AUTOMATIC_INNER_SOURCE,
      PullRequestSource.MANUAL,
      PullRequestSource.MANUAL_INNER_SOURCE);

  // only fetch updates for open PRs
  public static final Set<PullRequestState> RELEVANT_PR_STATES = EnumSet.of(PullRequestState.OPEN);

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlDAO sourceControlDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  public PullRequestStateService(
      SourceControlPullRequestDAO sourceControlPullRequestDAO,
      SourceControlEventDAO sourceControlEventDAO,
      SourceControlDAO sourceControlDAO,
      ApplicationDAO applicationDAO)
  {
    this.sourceControlPullRequestDAO = sourceControlPullRequestDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlDAO = sourceControlDAO;
    this.applicationDAO = applicationDAO;
  }

  /**
   * Updates the state of all open pull requests by creating state update events for each.
   * These events can be processed later by event handlers.
   */
  public void dispatchPullRequestStateUpdateEvents() {
    log.info("Starting pull request state update process");

    try {
      List<SourceControlPullRequest> openPullRequests =
          sourceControlPullRequestDAO.getByStatesAndSources(RELEVANT_PR_STATES, RELEVANT_PR_SOURCES);
      log.debug("Found {} open pull requests", openPullRequests.size());

      var repoUrls = openPullRequests.stream()
          .map(SourceControlPullRequest::getRepositoryUrl)
          .collect(Collectors.toSet());
      Map<String, SortedSet<String>> allIdsByRepoUrl =
          applicationDAO.getApplicationIdsByNormalizedRepositoryUrls(repoUrls);

      // We only need one app per repoUrl. Use the alphabetically first one for consistency across runs
      Map<String, String> appIdsByRepoUrl = allIdsByRepoUrl.entrySet()
          .stream()
          .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().first()));

      // A map from one appId with a matching repoUrl to all of the open SourceControlPullRequests with that repoUrl
      Map<String, List<SourceControlPullRequest>> pullRequestsByAppId = openPullRequests.stream()
          .collect(Collectors.groupingBy(pr -> appIdsByRepoUrl.get(pr.getRepositoryUrl())));

      for (var entry : pullRequestsByAppId.entrySet()) {
        updatePullRequestStatus(entry.getKey(), entry.getValue());
      }

      log.info("Created state update events for {} pull requests", pullRequestsByAppId.size());
    }
    catch (Exception e) {
      log.error("Failed to update pull request states", e);
    }
  }

  /**
   * Filter out pull requests that already have an event created for them.
   *
   * @param openPullRequests the list of open pull requests to filter
   * @return a list of pull requests that do not have an event created for them
   */
  private List<SourceControlPullRequest> filterPRsWithExistingEvents(
      String applicationId,
      List<SourceControlPullRequest> openPullRequests)
  {
    if (openPullRequests.isEmpty()) {
      return openPullRequests;
    }
    else {
      List<SourceControlEvent> events =
          sourceControlEventDAO.getPullRequestStateUpdateEventsForApplication(applicationId);

      Set<Integer> prsWithExistingEvents = events.stream()
          .flatMap(event -> {
            switch (event.getEventType()) {
              case SourceControlEvent.PR_STATE_UPDATE_EVENT:
                return Stream.of(event.getPullRequestNumber()).filter(prId -> prId != 0);
              case SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT:
                try {
                  return JsonUtils.parse(event.getEventStatusDetails(), new TypeReference<List<Integer>>()
                  {
                  })
                      .stream();
                }
                catch (IOException e) {
                  throw new RuntimeException(e);
                }
              default:
                return Stream.empty();
            }
          })
          .collect(Collectors.toSet());

      return openPullRequests.stream()
          .filter(pr -> !prsWithExistingEvents.contains(pr.getPullRequestId()))
          .toList();
    }
  }

  /**
   * Create SourceControlEvents for updating each of the specified PRs, which are all in the same specified application.
   * If batch PR lifecycle fetching is supported, a minimal number of batch events will be created. Otherwise, one
   * event per PR will be created. Exceptions that occur during the creation of events are logged but do not stop
   * attempts to create the remaining events and are not thrown.
   */
  private void updatePullRequestStatus(String applicationId, List<SourceControlPullRequest> pullRequests) {
    SourceControl scmConfig = sourceControlDAO.buildCompositeSourceControlForApplicationId(applicationId);
    List<SourceControlPullRequest> prsNeedingEvents = filterPRsWithExistingEvents(applicationId, pullRequests);

    if (scmConfig.getProvider().supportsPullRequestBatchFetchById()) {
      for (var prChunk : Lists.partition(prsNeedingEvents, BATCH_SIZE)) {
        createBatchPullRequestStateEvent(applicationId, prChunk);
      }
    }
    else {
      for (var pr : prsNeedingEvents) {
        createPullRequestStateEvent(applicationId, pr);
      }
    }
  }

  /**
   * Creates a PR state update event for a single pull request. The statusDetails on the event contain the
   * SourceControlPullRequest database id while the PR Number (its id in the SCM provider) is stored in the
   * pullRequestNumber field.
   *
   * @param pullRequest the pull request to create an event for
   * @return the created event
   */
  private void createPullRequestStateEvent(String applicationId, SourceControlPullRequest pullRequest) {
    SourceControlEvent event = new SourceControlEvent().forPullRequestStateUpdate();
    event.setPullRequestNumber(pullRequest.getPullRequestId());
    event.setBranchName(pullRequest.getBranchName());
    event.setBaseBranchName(pullRequest.getBaseBranchName());
    event.setApplicationId(applicationId);

    sourceControlEventDAO.insert(event);
    log.debug("Created PR state update event {} for PR #{} in application {}",
        event.getId(), pullRequest.getPullRequestId(), applicationId);
  }

  /**
   * Creates a PR state update event for multiple pull requests. The statusDetails on the event contains a
   * JSON-serialized BatchPullRequestStateUpdateDetails object with the applicationId and a list of pull request ids.
   *
   * @param pullRequests the pullRequests to included within the event
   */
  private void createBatchPullRequestStateEvent(
      String applicationId,
      Collection<SourceControlPullRequest> pullRequests)
  {
    String statusDetails = buildBatchDetailsJson(pullRequests);

    SourceControlEvent event = new SourceControlEvent().forBatchPullRequestStateUpdate();
    event.setApplicationId(applicationId);
    event.setEventStatusDetails(statusDetails);

    sourceControlEventDAO.insert(event);
    log.debug("Created batch PR state update event {} for {} PRs in application {}",
        event.getId(), pullRequests.size(), applicationId);
    log.trace("Batch PR state update event PR ids: {}", statusDetails);
  }

  /**
   * Builds a JSON string from the given pull requests of the form {"app":"12345",prs:[1,2,3]}
   */
  private String buildBatchDetailsJson(Collection<SourceControlPullRequest> pullRequests) {
    var pullRequestIds = pullRequests.stream()
        .mapToInt(SourceControlPullRequest::getPullRequestId)
        .toArray();

    return JsonUtils.writeUnformatted(pullRequestIds);
  }
}
