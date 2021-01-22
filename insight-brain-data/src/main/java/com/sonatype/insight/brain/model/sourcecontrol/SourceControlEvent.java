/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.model.policy.PolicyEvaluationTriggerType;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.95
 */
@Entity
@Table(name = "source_control_event")
public class  SourceControlEvent
    extends HasComponentId
    implements HasStringId
{
  public static final String APPLICATION_EVALUATION_EVENT = "application evaluation";

  public static final String DISCOVERED_PULL_REQUEST_EVENT = "discovered pull request";

  public static final String MANIFEST_EVALUATION_EVENT = "manifest evaluation";

  public static final String REMEDIATION_PULL_REQUEST_EVENT = "remediation pull request";

  public static final String STATUS_UPDATE_EVENT = "status update";

  public static final String REPOSITORY_URL_UPDATED_EVENT = "url update";

  public static final String EVENT_STATUS_NEW = "new";

  public static final String EVENT_STATUS_IN_PROGRESS = "in progress";

  public static final String EVENT_STATUS_COMPLETE = "complete";

  public static final String EVENT_STATUS_ERROR = "error";

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

  @Column(name = "commit_hash")
  private String commitHash;

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
  @Column(name = "policy_evaluation_trigger_type")
  @Enumerated(EnumType.STRING)
  private PolicyEvaluationTriggerType policyEvaluationTriggerType;

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

  public String getCommitHash() {
    return commitHash;
  }

  public SourceControlEvent setCommitHash(final String commitHash) {
    this.commitHash = commitHash;
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

  public PolicyEvaluationTriggerType getPolicyEvaluationTriggerType() {
    return policyEvaluationTriggerType;
  }

  public SourceControlEvent setPolicyEvaluationTriggerType(PolicyEvaluationTriggerType policyEvaluationTriggerType) {
    this.policyEvaluationTriggerType = policyEvaluationTriggerType;
    return this;
  }
}
