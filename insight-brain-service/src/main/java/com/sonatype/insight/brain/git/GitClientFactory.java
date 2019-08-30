/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClientUtils;

@Named
@Singleton
public class GitClientFactory
{
  private final InsightProxy insightProxy;

  private final GitApiClientFactory gitApiClientFactory = new GitApiClientFactory();

  @Inject
  public GitClientFactory(final InsightProxy insightProxy) {
    this.insightProxy = insightProxy;
  }

  public GitApiClient create(GitRepositoryInfo gitRepositoryInfo) {
    Configuration configuration = new Configuration();
    String apiUrl = getClientUtils(gitRepositoryInfo.provider).getApiUrl(gitRepositoryInfo.repositoryUrl);
    insightProxy.contextualize(configuration, apiUrl);
    return gitApiClientFactory.getGitApiClient(
        gitRepositoryInfo.provider, configuration, gitRepositoryInfo.repositoryUrl, gitRepositoryInfo.token);
  }

  private GitApiClientUtils getClientUtils(final SourceControlProvider provider) {
    return gitApiClientFactory.getGitApiClientUtils(provider);
  }
}
