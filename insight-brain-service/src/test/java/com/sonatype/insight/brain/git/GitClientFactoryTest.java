/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClientUtils;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.github.GitHubApiClient;
import com.sonatype.nexus.scm.github.graphql.GitHubGraphQlClient;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitClientFactoryTest
    extends AbstractComponentTest
{
  private static final GitRepositoryInfo GIT_REPO_INFO =
      new GitRepositoryInfo("https://github.com/org/repo", null, null, "token",
          SourceControlProvider.GITHUB, "main", true, true,true, true, true, true, false, null);

  @Inject
  private GitClientFactory gitClientFactory;

  private GitClientFactory spyGitClientFactory;

  private GitApiClientUtils mockGitApiClientUtils;

  @Before
  public void setup() {
    // Spy needed in order to override getClientUtils
    spyGitClientFactory = spy(gitClientFactory);

    mockGitApiClientUtils = mock(GitApiClientUtils.class);

    doReturn(mockGitApiClientUtils).when(spyGitClientFactory).getClientUtils(eq(SourceControlProvider.GITHUB),
        any(Configuration.class));
  }

  @Test
  public void test_createApiClient_urlCaching() {
    // setup:
    when(mockGitApiClientUtils.getApiUrl(any(), any())).thenReturn("https://github.com/api/v3/");
    GIT_REPO_INFO.normalizedRepositoryUrl = "https://github.com/org/repo";

    // when: createApiClient is called for the first time for a GitHub repo
    GitApiClient apiClient = spyGitClientFactory.createApiClient(GIT_REPO_INFO);

    // then: a client instance is created
    assertThat(apiClient).isInstanceOf(GitHubApiClient.class);

    // and: api url was not cached, so GitApiClientUtils is called to compute and cache it
    verify(mockGitApiClientUtils, times(1)).getApiUrl(eq(GIT_REPO_INFO.normalizedRepositoryUrl), any());

    // when: createApiClient is called for another GitHub repo
    GIT_REPO_INFO.normalizedRepositoryUrl = "https://github.com/org/repo2";
    GitApiClient apiClient2 = spyGitClientFactory.createApiClient(GIT_REPO_INFO);

    // then: another client instance is created
    assertThat(apiClient2).isInstanceOf(GitHubApiClient.class);
    assertThat(apiClient2).isNotEqualTo(apiClient);

    // and: api url was cached so GitApiClientUtils is not called to compute it
    verify(mockGitApiClientUtils, never()).getApiUrl(eq(GIT_REPO_INFO.normalizedRepositoryUrl), any());
  }

  @Test
  public void test_createPullRequestInfoClient_urlCaching() {
    // setup:
    when(mockGitApiClientUtils.getPullRequestInfoProviderUrl(any(), any())).thenReturn("https://github.com/api/v4/");
    GIT_REPO_INFO.normalizedRepositoryUrl = "https://github.com/org/repo";

    // when: createPullRequestInfoClient is called for the first time for a GitHub repo
    PullRequestInfoProvider apiClient = spyGitClientFactory.createPullRequestInfoClient(GIT_REPO_INFO);

    // then: a client instance is created
    assertThat(apiClient).isInstanceOf(GitHubGraphQlClient.class);

    // and: api url was not cached, so GitApiClientUtils is called to compute and cache it
    verify(mockGitApiClientUtils, times(1)).getPullRequestInfoProviderUrl(
        eq(GIT_REPO_INFO.normalizedRepositoryUrl), any());

    // when: createPullRequestInfoClient is called for another GitHub repo
    GIT_REPO_INFO.normalizedRepositoryUrl = "https://github.com/org/repo2";
    PullRequestInfoProvider apiClient2 = spyGitClientFactory.createPullRequestInfoClient(GIT_REPO_INFO);

    // then: another client instance is created
    assertThat(apiClient2).isInstanceOf(GitHubGraphQlClient.class);
    assertThat(apiClient2).isNotEqualTo(apiClient);

    // and: api url was cached so GitApiClientUtils is not called to compute it
    verify(mockGitApiClientUtils, never()).getPullRequestInfoProviderUrl(
        eq(GIT_REPO_INFO.normalizedRepositoryUrl), any());
  }
}
