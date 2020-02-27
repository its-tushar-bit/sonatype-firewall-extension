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
 * @since 1.86
 */
@Entity
@Table(name = "source_control_pull_request_comment")
public class SourceControlPullRequestComment
    implements HasStringId
{
  @Id
  @Column(name = "source_control_pull_request_comment_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "pull_request_id")
  private int pullRequestId;

  @Column(name = "pull_request_comment_id")
  private int pullRequestCommentId;

  @Column(name = "source_policy_evaluation_id")
  private String sourcePolicyEvaluationId;

  @Column(name = "target_policy_evaluation_id")
  private String targetPolicyEvaluationId;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "update_time")
  private Date updateTime;

  public SourceControlPullRequestComment() {

  }

  public SourceControlPullRequestComment(
      String applicationId,
      int pullRequestId,
      int pullRequestCommentId,
      String sourcePolicyEvaluationId,
      String targetPolicyEvaluationId)
  {
    this.applicationId = applicationId;
    this.pullRequestId = pullRequestId;
    this.pullRequestCommentId = pullRequestCommentId;
    this.sourcePolicyEvaluationId = sourcePolicyEvaluationId;
    this.targetPolicyEvaluationId = targetPolicyEvaluationId;
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

  public SourceControlPullRequestComment setApplicationId(final String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  public int getPullRequestId() {
    return pullRequestId;
  }

  public SourceControlPullRequestComment setPullRequestId(final int pullRequestId) {
    this.pullRequestId = pullRequestId;
    return this;
  }

  public int getPullRequestCommentId() {
    return pullRequestCommentId;
  }

  public SourceControlPullRequestComment setPullRequestCommentId(final int pullRequestCommentId) {
    this.pullRequestCommentId = pullRequestCommentId;
    return this;
  }

  public String getSourcePolicyEvaluationId() {
    return sourcePolicyEvaluationId;
  }

  public SourceControlPullRequestComment setSourcePolicyEvaluationId(String sourcePolicyEvaluationId) {
    this.sourcePolicyEvaluationId = sourcePolicyEvaluationId;
    return this;
  }

  public String getTargetPolicyEvaluationId() {
    return targetPolicyEvaluationId;
  }

  public SourceControlPullRequestComment setTargetPolicyEvaluationId(String targetPolicyEvaluationId) {
    this.targetPolicyEvaluationId = targetPolicyEvaluationId;
    return this;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public SourceControlPullRequestComment setCreateTime(final Date createTime) {
    this.createTime = createTime;
    return this;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public SourceControlPullRequestComment setUpdateTime(final Date updateTime) {
    this.updateTime = updateTime;
    return this;
  }
}
