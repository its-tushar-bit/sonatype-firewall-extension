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
                           final String token,
                           final SourceControlProvider provider,
                           final String baseBranch,
                           final Boolean enablePullRequests,
                           final Boolean enableStatusChecks)
  {
    this.repositoryUrl = repositoryUrl;
    this.token = token;
    this.baseBranch = baseBranch;
    this.enablePullRequests = enablePullRequests;
    this.enableStatusChecks = enableStatusChecks;
    this.provider = provider;
  }

  public String repositoryUrl;

  public String token;

  public String baseBranch;

  public Boolean enablePullRequests;

  public Boolean enableStatusChecks;

  public SourceControlProvider provider;

  public boolean isDataComplete() {
    return !(StringUtils.isBlank(token)
        || StringUtils.isBlank(repositoryUrl)
        || provider == null
        || enablePullRequests == null
        || enableStatusChecks == null);
  }
}
