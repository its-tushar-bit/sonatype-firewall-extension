/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.86
 */
@Entity
@Table(name = "source_control_default_branch_commit_history")
public class SourceControlDefaultBranchCommitHistory
    implements HasStringId
{
  @Id
  @Column(name = "source_control_default_branch_commit_history_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "commit_hash")
  private String commitHash;

  @Column(name = "commit_time")
  private Date commitTime;

  @Column(name = "policy_evaluation_id")
  private String policyEvaluationId;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "update_time")
  private Date updateTime;

  public SourceControlDefaultBranchCommitHistory() {

  }

  public SourceControlDefaultBranchCommitHistory(
      String applicationId,
      String commitHash,
      Date commitTime,
      String policyEvaluationId)
  {
    this.applicationId = applicationId;
    this.commitHash = commitHash;
    this.commitTime = commitTime;
    this.policyEvaluationId = policyEvaluationId;
    this.createTime = new Date();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public SourceControlDefaultBranchCommitHistory setApplicationId(String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  public String getCommitHash() {
    return commitHash;
  }

  public SourceControlDefaultBranchCommitHistory setCommitHash(String commitHash) {
    this.commitHash = commitHash;
    return this;
  }

  public Date getCommitTime() {
    return commitTime;
  }

  public SourceControlDefaultBranchCommitHistory setCommitTime(Date commitTime) {
    this.commitTime = commitTime;
    return this;
  }

  public String getPolicyEvaluationId() {
    return policyEvaluationId;
  }

  public SourceControlDefaultBranchCommitHistory setPolicyEvaluationId(String policyEvaluationId) {
    this.policyEvaluationId = policyEvaluationId;
    return this;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public SourceControlDefaultBranchCommitHistory setCreateTime(Date createTime) {
    this.createTime = createTime;
    return this;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public SourceControlDefaultBranchCommitHistory setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
    return this;
  }
}
