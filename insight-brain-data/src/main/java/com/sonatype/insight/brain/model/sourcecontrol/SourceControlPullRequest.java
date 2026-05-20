/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.util.Date;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.114
 */
@Entity
@Table(name = "source_control_pull_request")
public class SourceControlPullRequest
    implements HasStringId
{
  @Id
  @Column(name = "source_control_pull_request_id")
  private String id;

  @Column(name = "repository_url")
  private String repositoryUrl;

  @Column(name = "pull_request_id")
  private int pullRequestId;

  @Column(name = "head_commit_hash")
  private String headCommitHash;

  @Column(name = "base_commit_hash")
  private String baseCommitHash;

  @Column(name = "branch_name")
  private String branchName;

  @Column(name = "base_branch_name")
  private String baseBranchName;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "last_check_time")
  private Date lastCheckTime;

  @Column(name = "last_detected_update_time")
  private Date lastDetectedUpdateTime;

  @Column(name = "state")
  @Enumerated(EnumType.STRING)
  private PullRequestState state;

  // Note that prior to SDEV-1952 all SourceControlPullRequest were external
  // and when this column was added, it was allowed to be nullable to avoid any migration cost,
  // which means that a null value means external
  @Column(name = "source")
  @Enumerated(EnumType.STRING)
  private PullRequestSource source;

  @Column(name = "source_control_event_id")
  private String sourceControlEventId;

  @Column(name = "authentication_type")
  private String authenticationType;

  @Column(name = "auth_owner_id")
  private String authOwnerId;

  @Column(name = "github_app_id")
  private String githubAppId;

  @Column(name = "installation_id")
  private String installationId;

  public SourceControlPullRequest() {
  }

  public SourceControlPullRequest(
      String repositoryUrl,
      int pullRequestId,
      String headCommitHash,
      String baseCommitHash,
      String branchName,
      String baseBranchName,
      Date createTime,
      Date lastCheckTime,
      Date lastDetectedUpdateTime,
      PullRequestState state,
      PullRequestSource source)
  {
    setRepositoryUrl(repositoryUrl);
    this.pullRequestId = pullRequestId;
    this.headCommitHash = headCommitHash;
    this.baseCommitHash = baseCommitHash;
    this.branchName = branchName;
    this.baseBranchName = baseBranchName;
    this.createTime = createTime;
    this.lastCheckTime = lastCheckTime;
    this.lastDetectedUpdateTime = lastDetectedUpdateTime;
    this.state = state;
    this.source = source;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public void setRepositoryUrl(String repositoryUrl) {
    if (StringUtils.isBlank(repositoryUrl)) {
      repositoryUrl = null;
    }

    if (repositoryUrl != null) {
      repositoryUrl = repositoryUrl.trim();
      repositoryUrl = SourceControl.normalizeRepositoryUrl(repositoryUrl);
    }
    this.repositoryUrl = repositoryUrl;
  }

  public int getPullRequestId() {
    return pullRequestId;
  }

  public void setPullRequestId(int pullRequestId) {
    this.pullRequestId = pullRequestId;
  }

  public String getHeadCommitHash() {
    return headCommitHash;
  }

  public void setHeadCommitHash(String headCommitHash) {
    this.headCommitHash = headCommitHash;
  }

  public String getBaseCommitHash() {
    return baseCommitHash;
  }

  public void setBaseCommitHash(final String baseCommitHash) {
    this.baseCommitHash = baseCommitHash;
  }

  public String getBaseBranchName() {
    return baseBranchName;
  }

  public void setBaseBranchName(final String baseBranchName) {
    this.baseBranchName = baseBranchName;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Date getLastCheckTime() {
    return lastCheckTime;
  }

  public void setLastCheckTime(Date lastCheckTime) {
    this.lastCheckTime = lastCheckTime;
  }

  public Date getLastDetectedUpdateTime() {
    return lastDetectedUpdateTime;
  }

  public void setLastDetectedUpdateTime(Date lastDetectedUpdateTime) {
    this.lastDetectedUpdateTime = lastDetectedUpdateTime;
  }

  public PullRequestState getState() {
    return state;
  }

  public void setState(final PullRequestState state) {
    this.state = state;
  }

  public PullRequestSource getSource() {
    return source;
  }

  public void setSource(final PullRequestSource source) {
    this.source = source;
  }

  public String getSourceControlEventId() {
    return sourceControlEventId;
  }

  public SourceControlPullRequest setSourceControlEventId(final String sourceControlEventId) {
    this.sourceControlEventId = sourceControlEventId;
    return this;
  }

  public String getAuthenticationType() {
    return authenticationType;
  }

  public SourceControlPullRequest setAuthenticationType(final String authenticationType) {
    this.authenticationType = authenticationType;
    return this;
  }

  public String getAuthOwnerId() {
    return authOwnerId;
  }

  public SourceControlPullRequest setAuthOwnerId(final String authOwnerId) {
    this.authOwnerId = authOwnerId;
    return this;
  }

  public String getGithubAppId() {
    return githubAppId;
  }

  public SourceControlPullRequest setGithubAppId(final String githubAppId) {
    this.githubAppId = githubAppId;
    return this;
  }

  public String getInstallationId() {
    return installationId;
  }

  public SourceControlPullRequest setInstallationId(final String installationId) {
    this.installationId = installationId;
    return this;
  }

  @Override
  public String toString() {
    return "SourceControlPullRequest [id=" + id + ", repositoryUrl=" + repositoryUrl + ", pullRequestId="
        + pullRequestId + ", headCommitHash=" + headCommitHash + ", baseCommitHash=" + baseCommitHash + ", branchName="
        + branchName + ", baseBranchName=" + baseBranchName + ", createTime=" + createTime + ", lastCheckTime="
        + lastCheckTime + ", lastDetectedUpdateTime=" + lastDetectedUpdateTime + ", state=" + state + ", source="
        + source + "]";
  }
}
