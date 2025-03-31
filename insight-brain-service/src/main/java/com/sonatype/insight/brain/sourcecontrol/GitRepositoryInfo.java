/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

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
                           final Boolean statusChecksEnabled,
                           final Boolean pullRequestCommentingEnabled,
                           final Boolean sourceControlEvaluationsEnabled,
                           final Boolean sshEnabled,
                           final String sourceControlScanTarget)
  {
    this(repositoryUrl, SourceControl.normalizeRepositoryUrl(repositoryUrl), sshRepositoryUrl, username, token,
        provider, baseBranch, remediationPullRequestsEnabled, manualPullRequestsEnabled, statusChecksEnabled,
        pullRequestCommentingEnabled, sourceControlEvaluationsEnabled, sshEnabled, sourceControlScanTarget);
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
                           final Boolean statusChecksEnabled,
                           final Boolean pullRequestCommentingEnabled,
                           final Boolean sourceControlEvaluationsEnabled,
                           final Boolean sshEnabled,
                           final String sourceControlScanTarget)
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
    this.statusChecksEnabled = statusChecksEnabled;
    this.pullRequestCommentingEnabled = pullRequestCommentingEnabled;
    this.sourceControlEvaluationsEnabled = sourceControlEvaluationsEnabled;
    this.sshEnabled = sshEnabled;
    this.sourceControlScanTarget = sourceControlScanTarget;
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

  public Boolean statusChecksEnabled;

  public Boolean pullRequestCommentingEnabled;

  public Boolean sourceControlEvaluationsEnabled;

  public Boolean sshEnabled;

  public String sourceControlScanTarget;

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
}
