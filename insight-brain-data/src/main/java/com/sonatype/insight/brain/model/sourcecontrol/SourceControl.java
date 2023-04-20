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

import com.sonatype.insight.model.HasStringId;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.annotations.VisibleForTesting;

import static com.sonatype.nexus.git.utils.repository.RepositoryUrlFinderUtils.sanitizeUrl;

/**
 * @since 1.66
 */
@Entity
@Table(name = "source_control")
public class SourceControl
    implements HasStringId
{
  public static final String FAKE_SECRET_KEY = "#~FAKE~SECRET~KEY~#";

  public static final boolean ENABLE_REMEDIATION_PULL_REQUESTS_BY_DEFAULT = true;

  public static final boolean ENABLE_STATUS_CHECKS_BY_DEFAULT = true;

  @Id
  @Column(name = "source_control_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "repository_url")
  private String repositoryUrl;

  @Column(name = "normalized_repository_url")
  private String normalizedRepositoryUrl;

  @Column(name = "repository_ssh_url")
  private String repositorySshUrl;

  @Column(name = "username")
  private String username;

  @Column(name = "token")
  private String token;

  @Column(name = "provider")
  @Enumerated(EnumType.STRING)
  private SourceControlProvider provider;

  @Column(name = "base_branch")
  private String baseBranch;

  @Column(name = "remediation_pull_requests_enabled")
  private Boolean remediationPullRequestsEnabled;

  @Column(name = "status_checks_enabled")
  private Boolean statusChecksEnabled;

  @Column(name = "pull_request_poll_time")
  private Date pullRequestPollTime;

  @Column(name = "pull_request_error_count")
  private int pullRequestErrorCount;

  @Column(name = "pull_request_commenting_enabled")
  private Boolean pullRequestCommentingEnabled;

  @Column(name = "source_control_evaluations_enabled")
  private Boolean sourceControlEvaluationsEnabled;

  @Column(name = "source_control_scan_target")
  private String sourceControlScanTarget;

  @Column(name = "ssh_enabled")
  private Boolean sshEnabled;

  @Column(name = "commit_status_enabled")
  private Boolean commitStatusEnabled;

  public SourceControl() {
  }

  public SourceControl(
      final String ownerId,
      final String repositoryUrl,
      final String repositorySshUrl,
      final String username,
      final String token,
      final SourceControlProvider provider,
      final Boolean pullRequestsEnabled,
      final Boolean statusChecksEnabled,
      final String baseBranch,
      final Boolean pullRequestCommentingEnabled,
      final Boolean sourceControlEvaluationsEnabled,
      final String sourceControlScanTarget,
      final Boolean sshEnabled,
      final Boolean commitStatusEnabled)
  {
    this.ownerId = ownerId;
    setRepositoryUrl(repositoryUrl);
    this.repositorySshUrl = repositorySshUrl;
    this.username = username;
    this.token = token;
    this.provider = provider;
    this.remediationPullRequestsEnabled = pullRequestsEnabled;
    this.statusChecksEnabled = statusChecksEnabled;
    this.baseBranch = baseBranch;
    this.pullRequestCommentingEnabled = pullRequestCommentingEnabled;
    this.sourceControlEvaluationsEnabled = sourceControlEvaluationsEnabled;
    this.sourceControlScanTarget = sourceControlScanTarget;
    this.sshEnabled = sshEnabled;
    this.commitStatusEnabled = commitStatusEnabled;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(final String ownerId) {
    this.ownerId = ownerId;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public static String normalizeRepositoryUrl(String repositoryUrl) {
    return repositoryUrl != null ? convertUrlIfNeeded(repositoryUrl) : null;
  }

  public void setRepositoryUrl(String repositoryUrl) {
    this.repositoryUrl = repositoryUrl;
    this.normalizedRepositoryUrl = normalizeRepositoryUrl(repositoryUrl);
  }

  public String getNormalizedRepositoryUrl() {
    return normalizedRepositoryUrl;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getToken() {
    return token;
  }

  public void setToken(final String token) {
    this.token = token;
  }

  public SourceControlProvider getProvider() {
    return provider;
  }

  public void setProvider(final SourceControlProvider provider) {
    this.provider = provider;
  }

  public String getBaseBranch() {
    return baseBranch;
  }

  public void setBaseBranch(final String baseBranch) {
    this.baseBranch = baseBranch;
  }

  public Boolean getRemediationPullRequestsEnabled() {
    return remediationPullRequestsEnabled;
  }

  public void setRemediationPullRequestsEnabled(final Boolean remediationPullRequestsEnabled) {
    this.remediationPullRequestsEnabled = remediationPullRequestsEnabled;
  }

  public Boolean getStatusChecksEnabled() {
    return statusChecksEnabled;
  }

  public void setStatusChecksEnabled(final Boolean statusChecksEnabled) {
    this.statusChecksEnabled = statusChecksEnabled;
  }

  public Date getPullRequestPollTime() {
    return pullRequestPollTime;
  }

  public void setPullRequestPollTime(Date date) {
    this.pullRequestPollTime = date;
  }

  public int getPullRequestErrorCount() {
    return pullRequestErrorCount;
  }

  public void setPullRequestErrorCount(int errorCount) {
    this.pullRequestErrorCount = errorCount;
  }

  public Boolean getPullRequestCommentingEnabled() {
    return pullRequestCommentingEnabled;
  }

  public void setPullRequestCommentingEnabled(final Boolean pullRequestCommentingEnabled) {
    this.pullRequestCommentingEnabled = pullRequestCommentingEnabled;
  }

  public Boolean getSourceControlEvaluationsEnabled() {
    return sourceControlEvaluationsEnabled;
  }

  public void setSourceControlEvaluationsEnabled(final Boolean sourceControlEvaluationsEnabled) {
    this.sourceControlEvaluationsEnabled = sourceControlEvaluationsEnabled;
  }

  public String getSourceControlScanTarget() {
    return sourceControlScanTarget;
  }

  public void setSourceControlScanTarget(final String sourceControlScanTarget) {
    this.sourceControlScanTarget = sourceControlScanTarget;
  }

  public String getRepositorySshUrl() {
    return repositorySshUrl;
  }

  public void setRepositorySshUrl(final String repositorySshUrl) {
    this.repositorySshUrl = repositorySshUrl;
  }

  public Boolean getSshEnabled() {
    return sshEnabled;
  }

  public void setSshEnabled(final Boolean sshEnabled) {
    this.sshEnabled = sshEnabled;
  }

  public Boolean getCommitStatusEnabled() {
    return commitStatusEnabled;
  }

  public void setCommitStatusEnabled(final Boolean commitStatusEnabled) {
    this.commitStatusEnabled = commitStatusEnabled;
  }

  public static class Builder
  {
    private String ownerId;

    private String repositoryUrl;

    private String repositorySshUrl;

    private String username;

    private String token;

    private SourceControlProvider provider;

    private Boolean remediationPullRequestsEnabled;

    private Boolean statusChecksEnabled;

    private String baseBranch;

    private Date pullRequestPollTime;

    private Boolean pullRequestCommentingEnabled;

    private Boolean sourceControlEvaluationsEnabled;

    private String sourceControlScanTarget;

    private Boolean sshEnabled;

    private Boolean commitStatusEnabled;

    public Builder setOwnerId(final String ownerId) {
      this.ownerId = ownerId;
      return this;
    }

    public Builder setRepositoryUrl(final String repositoryUrl) {
      this.repositoryUrl = repositoryUrl;
      return this;
    }

    public Builder setRepositorySshUrl(final String repositorySshUrl) {
      this.repositorySshUrl = repositorySshUrl;
      return this;
    }

    public Builder setUsername(final String username) {
      this.username = username;
      return this;
    }

    public Builder setToken(final String token) {
      this.token = token;
      return this;
    }

    public Builder setProvider(final SourceControlProvider provider) {
      this.provider = provider;
      return this;
    }

    public Builder setRemediationPullRequestsEnabled(final Boolean remediationPullRequestsEnabled) {
      this.remediationPullRequestsEnabled = remediationPullRequestsEnabled;
      return this;
    }

    public Builder setStatusChecksEnabled(final Boolean statusChecksEnabled) {
      this.statusChecksEnabled = statusChecksEnabled;
      return this;
    }

    public Builder setBaseBranch(final String baseBranch) {
      this.baseBranch = baseBranch;
      return this;
    }

    public Builder setPullRequestPollTime(final Date pullRequestPollTime) {
      this.pullRequestPollTime = pullRequestPollTime;
      return this;
    }

    public Builder setPullRequestCommentingEnabled(final Boolean pullRequestCommentingEnabled) {
      this.pullRequestCommentingEnabled = pullRequestCommentingEnabled;
      return this;
    }

    public Builder setSourceControlEvaluationsEnabled(final Boolean sourceControlEvaluationsEnabled) {
      this.sourceControlEvaluationsEnabled = sourceControlEvaluationsEnabled;
      return this;
    }

    public Builder setSourceControlScanTarget(final String sourceControlScanTarget) {
      this.sourceControlScanTarget = sourceControlScanTarget;
      return this;
    }

    public Builder setSshEnabled(final Boolean sshEnabled) {
      this.sshEnabled = sshEnabled;
      return this;
    }

    public Builder setCommitStatusEnabled(final Boolean commitStatusEnabled) {
      this.commitStatusEnabled = commitStatusEnabled;
      return this;
    }

    public SourceControl build() {
      SourceControl sourceControl =
          new SourceControl(ownerId, repositoryUrl, repositorySshUrl, username, token, provider,
              remediationPullRequestsEnabled, statusChecksEnabled, baseBranch, pullRequestCommentingEnabled,
              sourceControlEvaluationsEnabled, sourceControlScanTarget, sshEnabled, commitStatusEnabled);
      sourceControl.setPullRequestPollTime(pullRequestPollTime);
      return sourceControl;
    }
  }

  public static void coalesce(SourceControl accumulator, SourceControl other) {
    if (null == other) {
      return;
    }
    accumulator.setId(coalesce(accumulator.getId(), other.getId()));
    accumulator.setOwnerId(coalesce(accumulator.getOwnerId(), other.getOwnerId()));
    accumulator.setToken(coalesce(accumulator.getToken(), other.getToken()));
    accumulator.setProvider(coalesce(accumulator.getProvider(), other.getProvider()));
    accumulator.setRepositoryUrl(coalesce(accumulator.getRepositoryUrl(), other.getRepositoryUrl()));
    accumulator.setUsername(coalesce(accumulator.getUsername(), other.getUsername()));
    accumulator.setBaseBranch(coalesce(accumulator.getBaseBranch(), other.getBaseBranch()));
    accumulator.setRemediationPullRequestsEnabled(
        coalesce(accumulator.getRemediationPullRequestsEnabled(), other.getRemediationPullRequestsEnabled()));
    accumulator.setStatusChecksEnabled(coalesce(accumulator.getStatusChecksEnabled(), other.getStatusChecksEnabled()));
    accumulator.setPullRequestCommentingEnabled(
        coalesce(accumulator.getPullRequestCommentingEnabled(), other.getPullRequestCommentingEnabled()));
    accumulator.setSourceControlEvaluationsEnabled(
        coalesce(accumulator.getSourceControlEvaluationsEnabled(), other.getSourceControlEvaluationsEnabled()));
    accumulator.setSourceControlScanTarget(
        coalesce(accumulator.getSourceControlScanTarget(), other.getSourceControlScanTarget()));
    accumulator.setPullRequestPollTime(coalesce(accumulator.getPullRequestPollTime(), other.getPullRequestPollTime()));
    accumulator.setPullRequestErrorCount(
        coalesce(accumulator.getPullRequestErrorCount(), other.getPullRequestErrorCount()));
    accumulator.setSshEnabled(coalesce(accumulator.getSshEnabled(), other.getSshEnabled()));
    accumulator.setCommitStatusEnabled(coalesce(accumulator.getCommitStatusEnabled(), other.getCommitStatusEnabled()));
    accumulator.setRepositorySshUrl(coalesce(accumulator.getRepositorySshUrl(), other.getRepositorySshUrl()));
  }

  private static int coalesce(int value1, int value2) {
    return 0 != value1 ? value1 : value2;
  }

  private static <T> T coalesce(T value1, T value2) {
    return null != value1 ? value1 : value2;
  }

  @VisibleForTesting
  static String convertUrlIfNeeded(String repositoryUrl) {
    if (!repositoryUrl.startsWith("http")) {
      return repositoryUrl;
    }
    return sanitizeUrl(repositoryUrl);
  }

  @Override
  public String toString() {
    return "SourceControl{" +
        "id='" + id + '\'' +
        ", ownerId='" + ownerId + '\'' +
        ", repositoryUrl='" + repositoryUrl + '\'' +
        ", repositorySshUrl='" + repositorySshUrl + '\'' +
        ", username='" + username + '\'' +
        ", token='MASKED" + '\'' +
        ", provider=" + provider +
        ", baseBranch='" + baseBranch + '\'' +
        ", remediationPullRequestsEnabled=" + remediationPullRequestsEnabled +
        ", statusChecksEnabled=" + statusChecksEnabled +
        ", pullRequestPollTime=" + pullRequestPollTime +
        ", pullRequestErrorCount=" + pullRequestErrorCount +
        ", pullRequestCommentingEnabled=" + pullRequestCommentingEnabled +
        ", sourceControlEvaluationsEnabled=" + sourceControlEvaluationsEnabled +
        ", sourceControlScanTarget=" + sourceControlScanTarget +
        ", sshEnabled=" + sshEnabled +
        ", commitStatusEnabled=" + commitStatusEnabled +
        '}';
  }
}
