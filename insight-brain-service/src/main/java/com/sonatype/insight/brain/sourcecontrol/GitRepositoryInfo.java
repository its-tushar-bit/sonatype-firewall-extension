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
  public GitRepositoryInfo(final String repositoryUrl,
                           final String username,
                           final String token,
                           final SourceControlProvider provider,
                           final String baseBranch,
                           final Boolean enablePullRequests,
                           final Boolean enableStatusChecks)
  {
    this.repositoryUrl = repositoryUrl;
    this.username = username;
    this.token = token;
    this.baseBranch = baseBranch;
    this.enablePullRequests = enablePullRequests;
    this.enableStatusChecks = enableStatusChecks;
    this.provider = provider;
  }

  public String repositoryUrl;

  public String username;

  public String token;

  public String baseBranch;

  public Boolean enablePullRequests;

  public Boolean enableStatusChecks;

  public SourceControlProvider provider;

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public String getUsername() {
    return username;
  }

  public String getToken() {
    return token;
  }

  public String getBaseBranch() {
    return baseBranch;
  }

  public Boolean getEnablePullRequests() {
    return enablePullRequests;
  }

  public Boolean getEnableStatusChecks() {
    return enableStatusChecks;
  }

  public SourceControlProvider getProvider() {
    return provider;
  }

  public boolean isDataComplete() {
    return !(provider == null
        || StringUtils.isBlank(repositoryUrl)
        || StringUtils.isBlank(token)
        || (provider.requiresUsername() && StringUtils.isBlank(username))
        || enablePullRequests == null
        || enableStatusChecks == null);
  }
}
