/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.96
 */
@Entity
@Table(name = "source_control_event")
public class  SourceControlEvent
    implements HasStringId
{
  public static final String APPLICATION_EVALUATION_EVENT = "application evaluation";

  public static final String DISCOVERED_PULL_REQUEST_EVENT = "discovered pull request";

  public static final String REMEDIATION_PULL_REQUEST_EVENT = "remediation pull request";

  public static final String EVENT_STATUS_NEW = "new";

  public static final String EVENT_STATUS_IN_PROGRESS = "in progress";

  public static final String EVENT_STATUS_COMPLETE = "complete";

  public static final String EVENT_STATUS_ERROR = "error";

  @Id
  @Column(name = "source_control_event_id")
  private String id;

  @Column(name = "instance_id")
  private String instanceId;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "event_type")
  private String eventType;

  @Column(name = "event_status")
  private String eventStatus;

  @Column(name = "event_status_details")
  private String eventStatusDetails;

  @Column(name = "commit_hash")
  private String commitHash;

  @Column(name = "policy_evaluation_id")
  private String policyEvaluationId;

  @Column(name = "target_policy_evaluation_id")
  private String targetPolicyEvaluationId;

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

  public String getTargetPolicyEvaluationId() {
    return targetPolicyEvaluationId;
  }

  public SourceControlEvent setTargetPolicyEvaluationId(final String targetPolicyEvaluationId) {
    this.targetPolicyEvaluationId = targetPolicyEvaluationId;
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
}
