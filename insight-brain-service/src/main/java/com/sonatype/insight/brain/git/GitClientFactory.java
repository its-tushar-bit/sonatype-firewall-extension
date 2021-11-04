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
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClientUtils;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;

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

  public GitApiClient createApiClient(GitRepositoryInfo gitRepositoryInfo) {
    Configuration configuration = gitApiClientFactory.createConfiguration();
    String apiUrl = getClientUtils(gitRepositoryInfo.provider).getApiUrl(gitRepositoryInfo.normalizedRepositoryUrl);
    insightProxy.contextualize(configuration, apiUrl);
    return gitApiClientFactory.getGitApiClient(gitRepositoryInfo.provider, configuration,
        gitRepositoryInfo.normalizedRepositoryUrl, gitRepositoryInfo.username, gitRepositoryInfo.token);
  }

  public PullRequestInfoProvider createPullRequestInfoClient(GitRepositoryInfo gitRepositoryInfo) {
    Configuration configuration = gitApiClientFactory.createConfiguration();

    String graphqlApiUrl = getClientUtils(gitRepositoryInfo.provider).getPullRequestInfoProviderUrl(
        gitRepositoryInfo.normalizedRepositoryUrl);
    insightProxy.contextualize(configuration, graphqlApiUrl);

    return gitApiClientFactory.getPullRequestInfoClient(gitRepositoryInfo.provider, configuration,
        gitRepositoryInfo.username, gitRepositoryInfo.token, gitRepositoryInfo.normalizedRepositoryUrl);
  }

  public GeneralSCMApiClient createGeneralApiClient(
      final SourceControlProvider sourceControlProvider,
      final String hostUrl,
      final String username,
      final String token)
  {
    Configuration configuration = gitApiClientFactory.createConfiguration();
    String baseApiUrl = getClientUtils(sourceControlProvider).getBaseApiUrl(hostUrl);
    insightProxy.contextualize(configuration, baseApiUrl);
    return gitApiClientFactory.getGeneralSCMApiClient(sourceControlProvider, configuration, username, token);
  }

  public GitApiClientUtils getClientUtils(final SourceControlProvider provider) {
    return gitApiClientFactory.getGitApiClientUtils(provider);
  }
}
