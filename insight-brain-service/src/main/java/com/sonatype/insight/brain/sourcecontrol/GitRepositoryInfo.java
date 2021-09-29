/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import com.sonatype.nexus.scm.SourceControlProvider;

import org.apache.commons.lang3.StringUtils;

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
                           final Boolean statusChecksEnabled,
                           final Boolean pullRequestCommentingEnabled,
                           final Boolean sourceControlScansEnabled,
                           final Boolean sshEnabled,
                           final String sourceControlScanTarget)
  {
    this.repositoryUrl = repositoryUrl;
    this.sshRepositoryUrl = sshRepositoryUrl;
    this.username = username;
    this.token = token;
    this.provider = provider;
    this.baseBranch = baseBranch;
    this.remediationPullRequestsEnabled = remediationPullRequestsEnabled;
    this.statusChecksEnabled = statusChecksEnabled;
    this.pullRequestCommentingEnabled = pullRequestCommentingEnabled;
    this.sourceControlScansEnabled = sourceControlScansEnabled;
    this.sshEnabled = sshEnabled;
    this.sourceControlScanTarget = sourceControlScanTarget;
  }

  public String repositoryUrl;

  public String sshRepositoryUrl;

  public String username;

  public String token;

  public SourceControlProvider provider;

  public String baseBranch;

  public Boolean remediationPullRequestsEnabled;

  public Boolean statusChecksEnabled;

  public Boolean pullRequestCommentingEnabled;

  public Boolean sourceControlScansEnabled;

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

  public Boolean getStatusChecksEnabled() {
    return statusChecksEnabled;
  }

  public Boolean getPullRequestCommentingEnabled() {
    return pullRequestCommentingEnabled;
  }

  public Boolean getSourceControlScansEnabled() {
    return sourceControlScansEnabled;
  }

  public Boolean getSshEnabled() {
    return sshEnabled;
  }

  public String getSourceControlScanTarget() {
    return sourceControlScanTarget;
  }

  /**
   * Is the object considered 'complete'? A complete {@link GitRepositoryInfo} object is one that no longer needs any
   * attributes loaded from the hierarchy above.
   */
  public boolean isDataComplete() {
    return !(provider == null
        || StringUtils.isBlank(repositoryUrl)
        || StringUtils.isBlank(token)
        || (provider.requiresUsername() && StringUtils.isBlank(username))
        || remediationPullRequestsEnabled == null
        || statusChecksEnabled == null
        || pullRequestCommentingEnabled == null
        || sourceControlScansEnabled == null
        || sshEnabled == null
        || sourceControlScanTarget == null);
  }
}
