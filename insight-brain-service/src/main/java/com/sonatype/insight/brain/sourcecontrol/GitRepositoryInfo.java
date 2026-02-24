/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.util.Objects;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.nexus.scm.SourceControlProvider;

public class GitRepositoryInfo
{
  public GitRepositoryInfo() {
  }

  public GitRepositoryInfo(final String repositoryUrl,
                           final String sshRepositoryUrl,
                           final String username,
                           final String token,
                           final SourceControlProvider provider,
                           final String baseBranch,
                           final Boolean remediationPullRequestsEnabled,
                           final Boolean manualPullRequestsEnabled,
                           final Boolean innerSourceAutomatedUpdatesEnabled,
                           final Boolean statusChecksEnabled,
                           final Boolean pullRequestCommentingEnabled,
                           final Boolean sourceControlEvaluationsEnabled,
                           final Boolean sshEnabled,
                           final String sourceControlScanTarget)
  {
    this(repositoryUrl, SourceControl.normalizeRepositoryUrl(repositoryUrl), sshRepositoryUrl, username, token,
        provider, baseBranch, remediationPullRequestsEnabled, manualPullRequestsEnabled,
        innerSourceAutomatedUpdatesEnabled, statusChecksEnabled,
        pullRequestCommentingEnabled, sourceControlEvaluationsEnabled, sshEnabled, sourceControlScanTarget,
        null, null);
  }

  public GitRepositoryInfo(final String repositoryUrl,
                           final String normalizedRepositoryUrl,
                           final String sshRepositoryUrl,
                           final String username,
                           final String token,
                           final SourceControlProvider provider,
                           final String baseBranch,
                           final Boolean remediationPullRequestsEnabled,
                           final Boolean manualPullRequestsEnabled,
                           final Boolean innerSourceAutomatedUpdatesEnabled,
                           final Boolean statusChecksEnabled,
                           final Boolean pullRequestCommentingEnabled,
                           final Boolean sourceControlEvaluationsEnabled,
                           final Boolean sshEnabled,
                           final String sourceControlScanTarget,
                           final SourceControl.AuthenticationType authenticationType,
                           final String ownerId)
  {
    this.repositoryUrl = repositoryUrl;
    this.normalizedRepositoryUrl = normalizedRepositoryUrl;
    this.sshRepositoryUrl = sshRepositoryUrl;
    this.username = username;
    this.token = token;
    this.provider = provider;
    this.baseBranch = baseBranch;
    this.remediationPullRequestsEnabled = remediationPullRequestsEnabled;
    this.manualPullRequestsEnabled = manualPullRequestsEnabled;
    this.innerSourceAutomatedUpdatesEnabled = innerSourceAutomatedUpdatesEnabled;
    this.statusChecksEnabled = statusChecksEnabled;
    this.pullRequestCommentingEnabled = pullRequestCommentingEnabled;
    this.sourceControlEvaluationsEnabled = sourceControlEvaluationsEnabled;
    this.sshEnabled = sshEnabled;
    this.sourceControlScanTarget = sourceControlScanTarget;
    this.authenticationType = authenticationType;
    this.ownerId = ownerId;
  }

  public String repositoryUrl;

  public String normalizedRepositoryUrl;

  public String sshRepositoryUrl;

  public String username;

  public String token;

  public SourceControlProvider provider;

  public String baseBranch;

  public Boolean remediationPullRequestsEnabled;

  public Boolean manualPullRequestsEnabled;

  public Boolean innerSourceAutomatedUpdatesEnabled;

  public Boolean statusChecksEnabled;

  public Boolean pullRequestCommentingEnabled;

  public Boolean sourceControlEvaluationsEnabled;

  public Boolean sshEnabled;

  public String sourceControlScanTarget;

  public SourceControl.AuthenticationType authenticationType;

  public String ownerId;

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public String getSshRepositoryUrl() {
    return sshRepositoryUrl;
  }

  public String getUsername() {
    return username;
  }

  public String getToken() {
    return token;
  }

  public SourceControlProvider getProvider() {
    return provider;
  }

  public String getBaseBranch() {
    return baseBranch;
  }

  public Boolean getRemediationPullRequestsEnabled() {
    return remediationPullRequestsEnabled;
  }

  public Boolean getManualPullRequestsEnabled() {
    return manualPullRequestsEnabled;
  }

  public Boolean getInnerSourceAutomatedUpdatesEnabled() {
    return innerSourceAutomatedUpdatesEnabled;
  }

  public Boolean getStatusChecksEnabled() {
    return statusChecksEnabled;
  }

  public Boolean getPullRequestCommentingEnabled() {
    return pullRequestCommentingEnabled;
  }

  public Boolean getSourceControlEvaluationsEnabled() {
    return sourceControlEvaluationsEnabled;
  }

  public Boolean getSshEnabled() {
    return sshEnabled;
  }

  public String getSourceControlScanTarget() {
    return sourceControlScanTarget;
  }

  public SourceControl.AuthenticationType getAuthenticationType() {
    return authenticationType;
  }

  public String getOwnerId() {
    return ownerId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GitRepositoryInfo that = (GitRepositoryInfo) o;
    return Objects.equals(repositoryUrl, that.repositoryUrl) &&
        Objects.equals(normalizedRepositoryUrl, that.normalizedRepositoryUrl) &&
        Objects.equals(sshRepositoryUrl, that.sshRepositoryUrl) &&
        Objects.equals(username, that.username) &&
        Objects.equals(token, that.token) &&
        provider == that.provider &&
        Objects.equals(baseBranch, that.baseBranch) &&
        Objects.equals(remediationPullRequestsEnabled, that.remediationPullRequestsEnabled) &&
        Objects.equals(manualPullRequestsEnabled, that.manualPullRequestsEnabled) &&
        Objects.equals(innerSourceAutomatedUpdatesEnabled, that.innerSourceAutomatedUpdatesEnabled) &&
        Objects.equals(statusChecksEnabled, that.statusChecksEnabled) &&
        Objects.equals(pullRequestCommentingEnabled, that.pullRequestCommentingEnabled) &&
        Objects.equals(sourceControlEvaluationsEnabled, that.sourceControlEvaluationsEnabled) &&
        Objects.equals(sshEnabled, that.sshEnabled) &&
        Objects.equals(sourceControlScanTarget, that.sourceControlScanTarget) &&
        authenticationType == that.authenticationType &&
        Objects.equals(ownerId, that.ownerId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(normalizedRepositoryUrl);
  }
}
