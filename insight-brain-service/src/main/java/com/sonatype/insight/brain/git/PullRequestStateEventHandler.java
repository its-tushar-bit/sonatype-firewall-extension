/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.BatchPullRequestInfoProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.NoSuchPullRequestException;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.api.model.PullRequestLifecycleInfo;
import com.sonatype.nexus.scm.github.graphql.dto.pullrequests.data.CheckSuiteNode;
import com.sonatype.nexus.scm.github.graphql.dto.pullrequests.data.CommitNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.sourcecontrol.PullRequestState.AUTO_CLOSED;
import static com.sonatype.insight.brain.model.sourcecontrol.PullRequestState.MISSING;

/**
 * Processor class to handle SourceControlEvents which signal that the status or one or several PRs should
 * be re-queried from SCM and the corresponding SourceControlPullRequests updated. This class does that querying
 * and updating.
 */
@Named
@Singleton
public class PullRequestStateEventHandler
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestStateEventHandler.class);

  private final GitClientFactory gitClientFactory;

  private final SourceControlUtils sourceControlUtils;

  private final SourceControlDAO sourceControlDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private final PullRequestPollingService pullRequestPollingService;

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  @Inject
  public PullRequestStateEventHandler(
      final GitClientFactory gitClientFactory,
      final SourceControlUtils sourceControlUtils,
      final SourceControlDAO sourceControlDAO,
      final SourceControlEventDAO sourceControlEventDAO,
      final SourceControlPullRequestDAO sourceControlPullRequestDAO,
      final PullRequestPollingService pullRequestPollingService,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils)
  {
    this.gitClientFactory = gitClientFactory;
    this.sourceControlUtils = sourceControlUtils;
    this.sourceControlDAO = sourceControlDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlPullRequestDAO = sourceControlPullRequestDAO;
    this.pullRequestPollingService = pullRequestPollingService;
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
  }

  public void handle(final SourceControlEvent event) {
    var gitRepoInfo = sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());
    PullRequestInfoProvider prInfoProvider = gitClientFactory.createPullRequestInfoClient(gitRepoInfo);
    GitApiClient gitApiClient = gitClientFactory.createApiClient(gitRepoInfo);
    String org = gitApiClient.getProjectUrl().getNamespace();
    String repository = gitApiClient.getProjectUrl().getProject();

    switch (event.getEventType()) {
      case SourceControlEvent.PR_STATE_UPDATE_EVENT:
        handleSinglePREvent(prInfoProvider, org, repository, event);
        break;
      case SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT:
        if (prInfoProvider instanceof BatchPullRequestInfoProvider batchPrInfoProvider) {
          handleBatchPREvent(batchPrInfoProvider, org, repository, event);
        }
        else {
          String message = String.format(
              "SourceControlEvent %s is a BATCH_PR_STATE_UPDATE_EVENT event but is " +
                  "for non-batch-capable SCM provider %s",
              event,
              prInfoProvider.getClass().getName());
          throw new IllegalArgumentException(message);
        }
        break;
      default: // ignore
    }
  }

  private void handleSinglePREvent(
      PullRequestInfoProvider prInfoProvider,
      String org,
      String repository,
      SourceControlEvent event)
  {
    int prNumber = event.getPullRequestNumber();
    if (prNumber <= 0) {
      log.warn("Pull request number is null for event {}, skipping processing", event.getId());
      sourceControlEventDAO.delete(event);
      return;
    }

    PullRequestLifecycleInfo prLifecycleInfo = null;
    boolean ioException = false;
    try {
      prLifecycleInfo =
          prInfoProvider.getPullRequestLifecycleInfoById(org, repository, prNumber);
    }
    catch (IOException e) {
      ioException = true;
      log.error(
          "Failed to obtain PullRequestLifecycleInfo from SCM for org {}, repository {}, pull request {} - reason: {}",
          org, repository, prNumber, e.getMessage());
    }
    catch (NoSuchPullRequestException noSuchPR) {
      prLifecycleInfo = null;
    }

    if (!ioException) {
      processPRLifecycleInfo(event.getApplicationId(), prNumber, prLifecycleInfo);
    }

    sourceControlEventDAO.delete(event);
  }

  private void handleBatchPREvent(
      BatchPullRequestInfoProvider prInfoProvider,
      String org,
      String repository,
      SourceControlEvent event)
  {
    Map<Integer, PullRequestLifecycleInfo> prInfoByNumber = null;
    int[] prNumbers = null;
    boolean ioException = false;
    try {
      prNumbers = JsonUtils.parse(event.getEventStatusDetails(), int[].class);

      if (prNumbers != null) {
        prInfoByNumber = prInfoProvider.getPullRequestLifecycleInfoByIds(org, repository, prNumbers);
      }
    }
    // Note that getPullRequestLifecycleInfoByIds only throws NoSuchPullRequestException if the entire org or repo
    // is missing. If individual PRs are missing they simply won't be in the results.
    catch (IOException e) {
      log.error(
          "Failed to obtain PullRequestLifecycleInfos from SCM for org {}, repository {} - reason: {}",
          org, repository, e.getMessage());

      ioException = true;
    }
    catch (NoSuchPullRequestException noSuchPR) {
      prInfoByNumber = Map.of();
    }

    if (!ioException) {
      for (int prNumber : prNumbers) {
        PullRequestLifecycleInfo prLifecycleInfo = prInfoByNumber.get(prNumber);
        processPRLifecycleInfo(event.getApplicationId(), prNumber, prLifecycleInfo);
      }
    }

    sourceControlEventDAO.delete(event);
  }

  private void processPRLifecycleInfo(
      final String applicationId,
      final int prNumber,
      final PullRequestLifecycleInfo prLifecycleInfo)
  {
    SourceControlPullRequest pullRequest = sourceControlPullRequestDAO.getByApplicationIdAndPullRequestId(
        applicationId,
        prNumber);

    if (pullRequest == null) {
      log.warn("Pull request {} not found in database, cannot update state", prNumber);
    }
    else {
      boolean autoCloseTriggered = closeAutoPullRequestIfEnabled(applicationId, pullRequest, prLifecycleInfo);
      updateSourceControlPullRequest(pullRequest, prLifecycleInfo, autoCloseTriggered);
    }
  }

  // Visible for testing
  void updateSourceControlPullRequest(
      final SourceControlPullRequest pullRequest,
      final PullRequestLifecycleInfo prLifecycleInfo,
      final boolean autoCloseTriggered)
  {
    PullRequestState oldState = pullRequest.getState();
    pullRequest.setLastCheckTime(new Date());

    if (prLifecycleInfo == null) {
      pullRequest.setState(MISSING);
      pullRequest.setLastDetectedUpdateTime(new Date());
    }
    else {
      PullRequestState newState = PullRequestState.fromSCMState(prLifecycleInfo.getState());
      boolean hasChanges = false;

      if (autoCloseTriggered) {
        pullRequest.setState(AUTO_CLOSED);
        hasChanges = true;
      }
      else if (pullRequest.getState() != newState) {
        pullRequest.setState(newState);
        hasChanges = true;
      }

      if (!Objects.equals(pullRequest.getHeadCommitHash(), prLifecycleInfo.getHeadCommitHash())) {
        pullRequest.setHeadCommitHash(prLifecycleInfo.getHeadCommitHash());
        hasChanges = true;
      }

      // Note: gitlab doesn't have this info in the lifecycle response hence the extra null check
      if (prLifecycleInfo.getBaseCommitHash() != null &&
          !Objects.equals(pullRequest.getBaseCommitHash(), prLifecycleInfo.getBaseCommitHash()))
      {
        pullRequest.setBaseCommitHash(prLifecycleInfo.getBaseCommitHash());
        hasChanges = true;
      }

      if (!Objects.equals(pullRequest.getBranchName(), prLifecycleInfo.getBranchName())) {
        pullRequest.setBranchName(prLifecycleInfo.getBranchName());
        hasChanges = true;
      }

      if (!Objects.equals(pullRequest.getBaseBranchName(), prLifecycleInfo.getBaseBranchName())) {
        pullRequest.setBaseBranchName(prLifecycleInfo.getBaseBranchName());
        hasChanges = true;
      }

      if (hasChanges) {
        pullRequest.setLastDetectedUpdateTime(new Date());
      }
    }

    sourceControlPullRequestDAO.update(pullRequest);

    // Send telemetry when PR transitions to final states (MERGED/CLOSED)
    if (isPullRequestConcluded(oldState, pullRequest.getState())) {
      sendTelemetry(pullRequest, prLifecycleInfo);
    }
  }

  // Visible for testing
  boolean closeAutoPullRequestIfEnabled(
      final String applicationId,
      final SourceControlPullRequest pullRequest,
      final PullRequestLifecycleInfo prLifecycleInfo)
  {
    boolean autoCloseTriggered = false;
    SourceControl sourceControl = sourceControlDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);

    if (!isProviderAllowedForAutoClosing(sourceControl) || !isAutomaticPullRequest(pullRequest)) {
      return false;
    }
    String closeReason = null;

    boolean isClosePrOnFailedChecksEnabled = Optional.ofNullable(sourceControl.getClosePrOnFailedChecksEnabled())
        .orElse(false);

    if (isClosePrOnFailedChecksEnabled
        && sourceControl.getProvider() == SourceControlProvider.GITHUB
        && hasPrFailedChecks(prLifecycleInfo))
    {
      closeReason = "**This pull request was automatically closed.**  \n" +
          "This automated pull request failed one or more required checks and has been closed, " +
          "per Lifecycle configuration.";
    }

    if (isClosePrOnFailedChecksEnabled
        && sourceControl.getProvider() == SourceControlProvider.GITLAB
        && isMrCloseable(prLifecycleInfo))
    {
      closeReason = "**This merge request was automatically closed.**  \n" +
          "This automated merge request failed one or more required checks and has been closed, " +
          "per Lifecycle configuration.";
    }

    boolean isClosePrAfterDaysOpenEnabled = Optional.ofNullable(sourceControl.getClosePrAfterDaysOpenEnabled())
        .orElse(false);

    if (isClosePrAfterDaysOpenEnabled
        && isPrOlderThanDays(pullRequest, sourceControl.getClosePrAfterDays()))
    {
      String codeRequest = sourceControl.getProvider() == SourceControlProvider.GITLAB
          ? "merge request"
          : "pull request";
      closeReason = String.format("**This %s was automatically closed.**  \n" +
          "This automated %s was not merged and has been closed after %s days of inactivity, " +
          "per Lifecycle configuration.", codeRequest, codeRequest, sourceControl.getClosePrAfterDays());
    }

    if (closeReason != null) {
      pullRequestPollingService.createAndSendPullRequestClosingEvent(applicationId, pullRequest, closeReason);
      autoCloseTriggered = true;
    }
    return autoCloseTriggered;
  }

  private boolean isProviderAllowedForAutoClosing(SourceControl sourceControl) {
    return sourceControl != null;
  }

  private boolean isAutomaticPullRequest(SourceControlPullRequest pullRequest) {
    return pullRequest.getSource() == PullRequestSource.AUTOMATIC ||
        pullRequest.getSource() == PullRequestSource.AUTOMATIC_INNER_SOURCE;
  }

  private boolean hasPrFailedChecks(
      final PullRequestLifecycleInfo prLifecycleInfo)
  {
    if (prLifecycleInfo.getCommits() == null || prLifecycleInfo.getCommits().nodes.length == 0) {
      return false;
    }
    CommitNode node = prLifecycleInfo.getCommits().nodes[0];
    if (node.commit == null || node.commit.checkSuites == null || node.commit.checkSuites.nodes == null) {
      return false;
    }
    CheckSuiteNode[] checkSuiteNodes = node.commit.checkSuites.nodes;
    if (checkSuiteNodes.length == 0) {
      return false;
    }
    return Arrays.stream(checkSuiteNodes)
        .flatMap(checkSuiteNode -> Arrays.stream(checkSuiteNode.checkRuns.nodes))
        .anyMatch(checkRun -> checkRun.isRequired);
  }

  private boolean isMrCloseable(PullRequestLifecycleInfo prLifecycleInfo) {
    return "ci_must_pass".equals(prLifecycleInfo.getDetailedMergeStatus());
  }

  private boolean isPrOlderThanDays(
      final SourceControlPullRequest pullRequest,
      final int openLimitDays)
  {
    long daysOpen = ChronoUnit.DAYS.between(pullRequest.getCreateTime().toInstant(), Instant.now());
    return daysOpen > openLimitDays;
  }

  /**
   * Determines if a PR state transition represents a lifecycle event that should emit telemetry.
   * We only emit telemetry when PRs transition from OPEN to MERGED or CLOSED states.
   */
  private boolean isPullRequestConcluded(PullRequestState oldState, PullRequestState newState) {
    return oldState == PullRequestState.OPEN &&
        (newState == PullRequestState.MERGED
            || newState == PullRequestState.CLOSED
            || newState == PullRequestState.AUTO_CLOSED);
  }

  /**
   * Sends PR telemetry for lifecycle events (merge/close).
   * This provides granular telemetry complementing the aggregate metrics.
   */
  private void sendTelemetry(SourceControlPullRequest pullRequest, PullRequestLifecycleInfo prLifecycleInfo) {
    // Create and send telemetry event
    TelemetryData telemetryData = createPullRequestActivityTelemetry(pullRequest, prLifecycleInfo);
    telemetrySender.send(telemetryData);
  }

  /**
   * Finds the application ID for a given pull request by looking up associated source control.
   */
  private String getApplicationIdForPR(SourceControlPullRequest pullRequest) {
    List<SourceControl> sourceControls = sourceControlDAO.getByRepositoryUrl(pullRequest.getRepositoryUrl());
    return sourceControls.isEmpty() ? null : sourceControls.get(0).getOwnerId();
  }

  /**
   * Determines golden status by finding the original remediation event that created this PR.
   * Golden PRs are those created with RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES remediation type.
   * Returns "golden", "not_golden", or "unknown" as string values.
   */
  private String getGoldenStatusFromOriginalEvent(SourceControlPullRequest pullRequest, String applicationId) {

    SourceControlEvent originalEvent = sourceControlEventDAO
        .getLatestRemediationEventForPullRequest(applicationId, pullRequest.getPullRequestId());

    if (originalEvent != null) {
      return telemetryUtils.convertGoldenStatusToString(originalEvent.isGoldenPullRequest());
    }

    return "unknown"; // Unknown golden status
  }

  /**
   * Creates a telemetry event for PR lifecycle transitions (merged/closed).
   */
  private TelemetryData createPullRequestActivityTelemetry(
      SourceControlPullRequest pullRequest,
      PullRequestLifecycleInfo prLifecycleInfo)
  {
    // Find application ID for this PR
    String applicationId = getApplicationIdForPR(pullRequest);
    if (applicationId == null) {
      log.debug("Could not find application for PR {}", pullRequest.getId());
    }

    // Determine golden status from original remediation event
    String pullRequestType = getGoldenStatusFromOriginalEvent(pullRequest, applicationId);

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_PULL_REQUEST_ACTIVITY);

    // Common fields for both merged and closed events
    telemetryData.put("pull_request_number", pullRequest.getPullRequestId());
    telemetryData.put("application_id", telemetryUtils.obfuscate(applicationId));

    // Include golden status
    telemetryData.put("pull_request_type", pullRequestType);
    telemetryData.put("pull_request_creation_type", pullRequest.getSource().name());

    // Use real SCM timestamp if available, fallback to our detected update time
    Date eventTime = prLifecycleInfo != null
        ? prLifecycleInfo.getMergedOrClosedDate()
        : pullRequest.getLastDetectedUpdateTime();
    if (eventTime == null) {
      eventTime = pullRequest.getLastDetectedUpdateTime();
    }
    telemetryData.put("event_time", eventTime);

    // Set event type and timestamp based on PR state
    if (pullRequest.getState() == PullRequestState.MERGED) {
      telemetryData.put("event_type", "pr_merged");
    }
    else if (pullRequest.getState() == PullRequestState.CLOSED ||
        pullRequest.getState() == PullRequestState.AUTO_CLOSED)
    {
      telemetryData.put("event_type", "pr_closed_unmerged");
    }

    return telemetryData;
  }
}
