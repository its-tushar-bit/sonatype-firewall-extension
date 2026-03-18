/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.95
 */
@Entity
@Table(name = "source_control_event")
public class SourceControlEvent
    extends HasComponentId
    implements HasStringId
{
  public static final String APPLICATION_EVALUATION_EVENT = "application evaluation";

  public static final String DISCOVERED_PULL_REQUEST_EVENT = "discovered pull request";

  public static final String REMEDIATION_PULL_REQUEST_EVENT = "remediation pull request";

  public static final String REPOSITORY_URL_UPDATED_EVENT = "url update";

  public static final String SOURCE_CONTROL_EVALUATION_EVENT = "source control evaluation";

  public static final String STATUS_UPDATE_EVENT = "status update";

  public static final String UPDATED_PULL_REQUEST_EVENT = "updated pull request";

  public static final String MANUAL_REMEDIATION_PULL_REQUEST_EVENT = "manual_remediation_pull_request";

  public static final String CLOSE_PULL_REQUEST_EVENT = "close pull request";

  // an event to trigger the refreshing of a specified PR's state from the SCM provider
  public static final String PR_STATE_UPDATE_EVENT = "pr state update";

  // an event to trigger the refreshing of multiple specified PRs' states from an SCM provider
  public static final String BATCH_PR_STATE_UPDATE_EVENT = "batch pr state update";

  public static final List<String> EVENT_TYPES = ImmutableList.of(
      APPLICATION_EVALUATION_EVENT,
      BATCH_PR_STATE_UPDATE_EVENT,
      CLOSE_PULL_REQUEST_EVENT,
      DISCOVERED_PULL_REQUEST_EVENT,
      MANUAL_REMEDIATION_PULL_REQUEST_EVENT,
      PR_STATE_UPDATE_EVENT,
      REMEDIATION_PULL_REQUEST_EVENT,
      REPOSITORY_URL_UPDATED_EVENT,
      SOURCE_CONTROL_EVALUATION_EVENT,
      STATUS_UPDATE_EVENT,
      UPDATED_PULL_REQUEST_EVENT);

  public static final String EVENT_STATUS_NEW = "new";

  public static final String EVENT_STATUS_IN_PROGRESS = "in progress";

  public static final String EVENT_STATUS_COMPLETE = "complete";

  public static final String EVENT_STATUS_PARTIALLY_COMPLETE = "partially complete";

  public static final String EVENT_STATUS_ERROR = "error";

  public static final List<String> EVENT_STATUSES = ImmutableList.of(
      EVENT_STATUS_NEW,
      EVENT_STATUS_IN_PROGRESS,
      EVENT_STATUS_PARTIALLY_COMPLETE,
      EVENT_STATUS_COMPLETE,
      EVENT_STATUS_ERROR);

  public static final int EVENT_PRIORITY_HIGHER = 1;

  public static final int EVENT_PRIORITY_NORMAL = 2;

  public static final int EVENT_PRIORITY_LOWER = 3;

  @Id
  @Column(name = "source_control_event_id")
  private String id;

  @Column(name = "instance_id")
  private String instanceId;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "event_type")
  private String eventType;

  @Column(name = "event_priority")
  private int eventPriority = EVENT_PRIORITY_NORMAL;

  @Column(name = "event_status")
  private String eventStatus;

  @Column(name = "event_status_details")
  private String eventStatusDetails;

  @Column(name = "event_error_details")
  private String eventErrorDetails;

  @Column(name = "commit_hash")
  private String commitHash;

  @Column(name = "base_commit_hash")
  private String baseCommitHash;

  @Column(name = "policy_evaluation_id")
  private String policyEvaluationId;

  @Column(name = "policy_evaluation_outcome")
  private String policyEvaluationOutcome;

  @Column(name = "critical_component_count")
  private int criticalComponentCount;

  @Column(name = "severe_component_count")
  private int severeComponentCount;

  @Column(name = "moderate_component_count")
  private int moderateComponentCount;

  @Column(name = "scan_id")
  private String scanId;

  @Column(name = "stage_type_id")
  private String stageTypeId;

  @Column(name = "remediation_version")
  private String remediationVersion;

  @Column(name = "pull_request_contents")
  private String pullRequestContents;

  @Column(name = "branch_name")
  private String branchName;

  /**
   * @since 1.126
   */
  @Column(name = "scan_targets_json")
  private String scanTargetsJson;

  @Transient
  private List<String> scanTargets;

  @Column(name = "base_branch_name")
  private String baseBranchName;

  @Column(name = "pull_request_number")
  private int pullRequestNumber;

  @Column(name = "scm_username")
  private String scmUsername;

  @Column(name = "initiator")
  private String initiator;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "start_time")
  private Date startTime;

  @Column(name = "complete_time")
  private Date completeTime;

  @Column(name = "status_id")
  private String statusId;

  @Column(name = "user_agent")
  private String userAgent;

  /**
   * The trigger type for the policy evaluation if the event triggers a policy evaluation, null otherwise.
   *
   * @since 1.105
   */
  @Column(name = "scan_trigger_type")
  @Enumerated(EnumType.STRING)
  private ScanTriggerType scanTriggerType;

  @Column(name = "is_golden_pull_request")
  private Boolean goldenPullRequest;

  public SourceControlEvent() {
    eventStatus = EVENT_STATUS_NEW;
    createTime = new Date();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public SourceControlEvent withId(String id) {
    this.id = id;
    return this;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public SourceControlEvent setInstanceId(final String instanceId) {
    this.instanceId = instanceId;
    return this;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public SourceControlEvent setApplicationId(final String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  public String getEventType() {
    return eventType;
  }

  public SourceControlEvent setEventType(final String eventType) {
    this.eventType = eventType;
    return this;
  }

  public SourceControlEvent forApplicationEvaluation() {
    return setEventType(APPLICATION_EVALUATION_EVENT);
  }

  public SourceControlEvent forDiscoveredPullRequest() {
    return setEventType(DISCOVERED_PULL_REQUEST_EVENT);
  }

  public SourceControlEvent forUpdatedPullRequest() {
    return setEventType(UPDATED_PULL_REQUEST_EVENT);
  }

  public SourceControlEvent forSourceControlEvaluation() {
    return setEventType(SOURCE_CONTROL_EVALUATION_EVENT);
  }

  public SourceControlEvent forRemediationPullRequest() {
    setEventPriority(EVENT_PRIORITY_LOWER);
    return setEventType(REMEDIATION_PULL_REQUEST_EVENT);
  }

  public SourceControlEvent forRepositoryUrlUpdated() {
    return setEventType(REPOSITORY_URL_UPDATED_EVENT);
  }

  public SourceControlEvent forStatusUpdate() {
    return setEventType(STATUS_UPDATE_EVENT);
  }

  public SourceControlEvent forManualRemediationPullRequest() {
    return setEventType(MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
  }

  public SourceControlEvent forPullRequestStateUpdate() {
    setEventPriority(EVENT_PRIORITY_LOWER);
    return setEventType(PR_STATE_UPDATE_EVENT);
  }

  public SourceControlEvent forBatchPullRequestStateUpdate() {
    setEventPriority(EVENT_PRIORITY_LOWER);
    return setEventType(BATCH_PR_STATE_UPDATE_EVENT);
  }

  public SourceControlEvent forRemediationPullRequestClosing() {
    return setEventType(CLOSE_PULL_REQUEST_EVENT);
  }

  public int getEventPriority() {
    return eventPriority;
  }

  public SourceControlEvent setEventPriority(int eventPriority) {
    this.eventPriority = eventPriority;
    return this;
  }

  public String getEventStatus() {
    return eventStatus;
  }

  public SourceControlEvent setEventStatus(final String eventStatus) {
    this.eventStatus = eventStatus;
    return this;
  }

  public String getEventStatusDetails() {
    return eventStatusDetails;
  }

  public SourceControlEvent setEventStatusDetails(final String eventStatusDetails) {
    this.eventStatusDetails = eventStatusDetails;
    return this;
  }

  public String getEventErrorDetails() {
    return eventErrorDetails;
  }

  public void setEventErrorDetails(String eventErrorDetails) {
    this.eventErrorDetails = eventErrorDetails;
  }

  public String getCommitHash() {
    return commitHash;
  }

  public SourceControlEvent setCommitHash(final String commitHash) {
    this.commitHash = commitHash;
    return this;
  }

  public String getBaseCommitHash() {
    return baseCommitHash;
  }

  public SourceControlEvent setBaseCommitHash(final String baseCommitHash) {
    this.baseCommitHash = baseCommitHash;
    return this;
  }

  public String getBaseBranchName() {
    return baseBranchName;
  }

  public SourceControlEvent setBaseBranchName(final String baseBranchName) {
    this.baseBranchName = baseBranchName;
    return this;
  }

  public String getPolicyEvaluationId() {
    return policyEvaluationId;
  }

  public SourceControlEvent setPolicyEvaluationId(final String policyEvaluationId) {
    this.policyEvaluationId = policyEvaluationId;
    return this;
  }

  public String getPolicyEvaluationOutcome() {
    return policyEvaluationOutcome;
  }

  public SourceControlEvent setPolicyEvaluationOutcome(String policyEvaluationOutcome) {
    this.policyEvaluationOutcome = policyEvaluationOutcome;
    return this;
  }

  public int getCriticalComponentCount() {
    return criticalComponentCount;
  }

  public SourceControlEvent setCriticalComponentCount(int criticalComponentCount) {
    this.criticalComponentCount = criticalComponentCount;
    return this;
  }

  public int getSevereComponentCount() {
    return severeComponentCount;
  }

  public SourceControlEvent setSevereComponentCount(int severeComponentCount) {
    this.severeComponentCount = severeComponentCount;
    return this;
  }

  public int getModerateComponentCount() {
    return moderateComponentCount;
  }

  public SourceControlEvent setModerateComponentCount(int moderateComponentCount) {
    this.moderateComponentCount = moderateComponentCount;
    return this;
  }

  public SourceControlEvent withComponentCounts(int critical, int severe, int moderate) {
    this.criticalComponentCount = critical;
    this.severeComponentCount = severe;
    this.moderateComponentCount = moderate;
    return this;
  }

  public String getScanId() {
    return scanId;
  }

  public SourceControlEvent setScanId(String scanId) {
    this.scanId = scanId;
    return this;
  }

  public String getStageTypeId() {
    return stageTypeId;
  }

  public SourceControlEvent setStageTypeId(String stageTypeId) {
    this.stageTypeId = stageTypeId;
    return this;
  }

  public String getStatusId() {
    return statusId;
  }

  public SourceControlEvent setStatusId(String statusId) {
    this.statusId = statusId;
    return this;
  }

  public String getRemediationVersion() {
    return remediationVersion;
  }

  public SourceControlEvent setRemediationVersion(String remediationVersion) {
    this.remediationVersion = remediationVersion;
    return this;
  }

  public String getPullRequestContents() {
    return pullRequestContents;
  }

  public SourceControlEvent setPullRequestContents(String pullRequestContents) {
    this.pullRequestContents = pullRequestContents;
    return this;
  }

  public SourceControlEvent withComponentIdentifier(ComponentIdentifier componentIdentifier) {
    super.setComponentIdentifier(componentIdentifier);
    return this;
  }

  public String getBranchName() {
    return branchName;
  }

  public SourceControlEvent setBranchName(final String branchName) {
    this.branchName = branchName;
    return this;
  }

  public int getPullRequestNumber() {
    return pullRequestNumber;
  }

  public SourceControlEvent setPullRequestNumber(int pullRequestNumber) {
    this.pullRequestNumber = pullRequestNumber;
    return this;
  }

  public String getScmUsername() {
    return scmUsername;
  }

  public SourceControlEvent setScmUsername(final String scmUsername) {
    this.scmUsername = scmUsername;
    return this;
  }

  public String getInitiator() {
    return initiator;
  }

  public SourceControlEvent setInitiator(final String initiator) {
    this.initiator = initiator;
    return this;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public SourceControlEvent setCreateTime(final Date createTime) {
    this.createTime = createTime;
    return this;
  }

  public Date getStartTime() {
    return startTime;
  }

  public SourceControlEvent setStartTime(final Date startTime) {
    this.startTime = startTime;
    return this;
  }

  public Date getCompleteTime() {
    return completeTime;
  }

  public SourceControlEvent setCompleteTime(final Date completeTime) {
    this.completeTime = completeTime;
    return this;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public SourceControlEvent setUserAgent(final String userAgent) {
    this.userAgent = userAgent;
    return this;
  }

  public ScanTriggerType getScanTriggerType() {
    return scanTriggerType;
  }

  public SourceControlEvent setScanTriggerType(ScanTriggerType scanTriggerType) {
    this.scanTriggerType = scanTriggerType;
    return this;
  }

  public Boolean isGoldenPullRequest() {
    return goldenPullRequest;
  }

  public SourceControlEvent setIsGoldenPullRequest(Boolean goldenPullRequest) {
    this.goldenPullRequest = goldenPullRequest;
    return this;
  }

  public SourceControlEvent copyAsNew() {
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId(applicationId)
        .setBranchName(branchName)
        .setScanTargets(scanTargets)
        .setCommitHash(commitHash)
        .setBaseCommitHash(baseCommitHash)
        .setBaseBranchName(baseBranchName)
        .setCriticalComponentCount(criticalComponentCount)
        .setEventPriority(eventPriority)
        .setEventStatus(EVENT_STATUS_NEW)
        .setEventType(eventType)
        .setInitiator(initiator)
        .setInstanceId(instanceId)
        .setModerateComponentCount(moderateComponentCount)
        .setPolicyEvaluationId(policyEvaluationId)
        .setPolicyEvaluationOutcome(policyEvaluationOutcome)
        .setPullRequestContents(pullRequestContents)
        .setPullRequestNumber(pullRequestNumber)
        .setRemediationVersion(remediationVersion)
        .setScanId(scanId)
        .setScanTriggerType(scanTriggerType)
        .setScmUsername(scmUsername)
        .setSevereComponentCount(severeComponentCount)
        .setStageTypeId(stageTypeId)
        .setStatusId(statusId)
        .setIsGoldenPullRequest(goldenPullRequest)
        .setUserAgent(userAgent);
    event.setComponentIdentifier(getComponentIdentifier());
    return event;
  }

  public String getScanTargetsJson() {
    return scanTargetsJson;
  }

  public void setScanTargetsJson(String scanTargetsJson) {
    if (StringUtils.isBlank(scanTargetsJson)) {
      scanTargetsJson = null;
    }
    this.scanTargetsJson = scanTargetsJson;
    scanTargets = null;
  }

  public SourceControlEvent setScanTargets(List<String> scanTargets) {
    if (scanTargets == null || scanTargets.isEmpty()) {
      this.scanTargets = null;
      scanTargetsJson = null;
    }
    else {
      this.scanTargets = scanTargets;
      scanTargetsJson = JsonUtils.writeUnformatted(scanTargets);
    }
    return this;
  }

  public List<String> getScanTargets() {
    if (scanTargets == null && !StringUtils.isBlank(scanTargetsJson)) {
      try {
        scanTargets = Arrays.asList(JsonUtils.parse(scanTargetsJson, String[].class));
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed to read scan targets for scource control event " + id, e);
      }
    }
    return scanTargets;
  }
}
