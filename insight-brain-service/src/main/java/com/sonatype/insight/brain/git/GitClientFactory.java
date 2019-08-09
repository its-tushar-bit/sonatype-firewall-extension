/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlProvider;
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

  public GitApiClient create(final SourceControl sourceControl) {
    Configuration configuration = new Configuration();
    String apiUrl = getClientUtils(sourceControl.getProvider()).getApiUrl(sourceControl.getRepositoryUrl());
    insightProxy.contextualize(configuration, apiUrl);
    return GitApiClientFactory.getGitApiClient(getScmClientProvider(sourceControl.getProvider()),
        configuration, sourceControl.getRepositoryUrl(), sourceControl.getToken());
  }

  private GitApiClientUtils getClientUtils(final SourceControlProvider provider) {
    return GitApiClientFactory.getGitApiClientUtils(getScmClientProvider(provider));
  }

  private com.sonatype.nexus.scm.SourceControlProvider getScmClientProvider(final SourceControlProvider provider) {
    return com.sonatype.nexus.scm.SourceControlProvider.fromString(provider.toString());
  }
}
