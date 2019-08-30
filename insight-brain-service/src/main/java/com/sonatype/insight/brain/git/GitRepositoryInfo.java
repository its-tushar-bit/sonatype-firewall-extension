/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.nexus.scm.SourceControlProvider;

public class GitRepositoryInfo
{
  public GitRepositoryInfo(final String repositoryUrl,
                           final String token,
                           final SourceControlProvider provider)
  {
    this.repositoryUrl = repositoryUrl;
    this.token = token;
    this.provider = provider;
  }

  public String repositoryUrl;

  public String token;

  public SourceControlProvider provider;
}
