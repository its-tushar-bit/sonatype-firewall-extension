/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;

import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.utils.AbstractHttpClientTest;
import com.sonatype.nexus.scm.SourceControlProvider;

public class GitClientFactoryTest
    extends AbstractHttpClientTest
{
  @Inject
  private GitClientFactory gitClientFactory;

  @Override
  protected void pingUrl(String url) throws Exception {
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo(url + "org/project", null, "token", SourceControlProvider.GITHUB, "master", false, false,
            false, false, null);
    gitClientFactory.createApiClient(gitRepositoryInfo).isRepositoryPrivate();
  }
}
