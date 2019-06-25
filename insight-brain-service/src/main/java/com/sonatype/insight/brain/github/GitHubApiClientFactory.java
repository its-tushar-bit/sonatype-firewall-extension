/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.github;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.nexus.github.GitHubApiClient;
import com.sonatype.nexus.github.GitHubApiClientUtils;

@Named
@Singleton
public class GitHubApiClientFactory
{
  private final InsightProxy insightProxy;

  @Inject
  public GitHubApiClientFactory(final InsightProxy insightProxy) {
    this.insightProxy = insightProxy;
  }

  public GitHubApiClient create(final String repositoryUrl, final String token) {
    Configuration configuration = new Configuration();
    insightProxy.contextualize(configuration, GitHubApiClientUtils.apiUrl(repositoryUrl));
    return new GitHubApiClient(configuration, token);
  }
}
