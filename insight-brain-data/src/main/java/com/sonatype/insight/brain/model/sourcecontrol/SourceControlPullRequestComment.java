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
@Table(name = "source_control_pull_request_comment")
public class SourceControlPullRequestComment
    implements HasStringId
{
  @Id
  @Column(name = "source_control_pull_request_comment_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "component_hash")
  private String componentHash;

  /**
   * The pathname of the file where the line comment is added. Null for summary comments.
   *
   * @since 1.125
   */
  @Column(name = "pathname")
  private String pathname;

  @Column(name = "pull_request_id")
  private int pullRequestId;

  @Column(name = "pull_request_comment_id")
  private long pullRequestCommentId;

  @Column(name = "pull_request_comment_version")
  private Integer pullRequestCommentVersion;

  @Column(name = "source_policy_evaluation_id")
  private String sourcePolicyEvaluationId;

  @Column(name = "target_policy_evaluation_id")
  private String targetPolicyEvaluationId;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "update_time")
  private Date updateTime;

  @Column(name = "content_hash")
  private String contentHash;

  public SourceControlPullRequestComment() {

  }

  /**
   * This constructor is intended for creating an overall PR comment
   */
  public SourceControlPullRequestComment(
      String applicationId,
      int pullRequestId,
      long pullRequestCommentId,
      Integer pullRequestCommentVersion,
      String contentHash,
      String sourcePolicyEvaluationId,
      String targetPolicyEvaluationId)
  {
    this.applicationId = applicationId;
    this.pullRequestId = pullRequestId;
    this.pullRequestCommentId = pullRequestCommentId;
    this.pullRequestCommentVersion = pullRequestCommentVersion;
    this.contentHash = contentHash;
    this.sourcePolicyEvaluationId = sourcePolicyEvaluationId;
    this.targetPolicyEvaluationId = targetPolicyEvaluationId;
    this.createTime = new Date();
  }

  /**
   * This constructor is intended for creating a line-level PR comment as it includes the component hash and pathname to
   * which the line comment pertains
   */
  public SourceControlPullRequestComment(
      String applicationId,
      String componentHash,
      String pathname,
      int pullRequestId,
      long pullRequestCommentId,
      Integer pullRequestCommentVersion,
      String sourcePolicyEvaluationId,
      String targetPolicyEvaluationId)
  {
    this(applicationId, pullRequestId, pullRequestCommentId, pullRequestCommentVersion, null, sourcePolicyEvaluationId,
        targetPolicyEvaluationId);
    this.componentHash = componentHash;
    this.pathname = pathname;
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

  public String getComponentHash() {
    return componentHash;
  }

  public SourceControlPullRequestComment setComponentHash(final String componentHash) {
    this.componentHash = componentHash;
    return this;
  }

  public int getPullRequestId() {
    return pullRequestId;
  }

  public SourceControlPullRequestComment setPullRequestId(final int pullRequestId) {
    this.pullRequestId = pullRequestId;
    return this;
  }

  public long getPullRequestCommentId() {
    return pullRequestCommentId;
  }

  public SourceControlPullRequestComment setPullRequestCommentId(final long pullRequestCommentId) {
    this.pullRequestCommentId = pullRequestCommentId;
    return this;
  }

  public Integer getPullRequestCommentVersion() {
    return pullRequestCommentVersion;
  }

  public SourceControlPullRequestComment setPullRequestCommentVersion(final Integer pullRequestCommentVersion) {
    this.pullRequestCommentVersion = pullRequestCommentVersion;
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

  public String getContentHash() {
    return contentHash;
  }

  public SourceControlPullRequestComment setContentHash(final String contentHash) {
    this.contentHash = contentHash;
    return this;
  }

  public String getPathname() {
    return pathname;
  }

  public void setPathname(String pathname) {
    this.pathname = pathname;
  }
}
