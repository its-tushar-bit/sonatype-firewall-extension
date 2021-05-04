/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.util.Date;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

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

  @Column(name = "repository_url_lowercase")
  private String repositoryUrlLowercase;

  @Column(name = "pull_request_id")
  private int pullRequestId;

  @Column(name = "head_commit_hash")
  private String headCommitHash;

  @Column(name = "branch_name")
  private String branchName;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "last_check_time")
  private Date lastCheckTime;

  @Column(name = "last_detected_update_time")
  private Date lastDetectedUpdateTime;

  public SourceControlPullRequest() {
  }

  public SourceControlPullRequest(
      String repositoryUrl,
      int pullRequestId,
      String headCommitHash,
      String branchName,
      Date createTime,
      Date lastCheckTime,
      Date lastDetectedUpdateTime)
  {
    setRepositoryUrlLowercase(repositoryUrl);
    this.pullRequestId = pullRequestId;
    this.headCommitHash = headCommitHash;
    this.branchName = branchName;
    this.createTime = createTime;
    this.lastCheckTime = lastCheckTime;
    this.lastDetectedUpdateTime = lastDetectedUpdateTime;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getRepositoryUrlLowercase() {
    return repositoryUrlLowercase;
  }

  public void setRepositoryUrlLowercase(String repositoryUrl) {
    if (StringUtils.isBlank(repositoryUrl)) {
      repositoryUrl = null;
    }

    if (repositoryUrl != null) {
      repositoryUrlLowercase = repositoryUrl.trim().toLowerCase(Locale.ENGLISH);
    }
    else {
      repositoryUrlLowercase = null;
    }
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
}
