/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
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

  @Inject
  public GitClientFactory(final InsightProxy insightProxy) {
    this.insightProxy = insightProxy;
  }

  public GitApiClient create(final ApiSourceControlDTO sourceControl) {
    Configuration configuration = new Configuration();
    SourceControlProvider provider = SourceControlProvider.fromString(sourceControl.provider);
    String apiUrl = getClientUtils(provider).getApiUrl(sourceControl.repositoryUrl);
    insightProxy.contextualize(configuration, apiUrl);
    return GitApiClientFactory.getGitApiClient(
        provider, configuration, sourceControl.repositoryUrl, sourceControl.token);
  }

  private GitApiClientUtils getClientUtils(final SourceControlProvider provider) {
    return GitApiClientFactory.getGitApiClientUtils(provider);
  }
}
