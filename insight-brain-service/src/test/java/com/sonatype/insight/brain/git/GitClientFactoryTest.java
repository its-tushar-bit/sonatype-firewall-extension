/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClientUtils;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import com.sonatype.nexus.scm.github.GitHubApiClient;
import com.sonatype.nexus.scm.github.graphql.GitHubGraphQlClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
          GITHUB, "main", true, true, true, true, true, true, false, null);

  @Inject
  private GitClientFactory gitClientFactory;

  private GitClientFactory spyGitClientFactory;

  private GitApiClientUtils mockGitApiClientUtils;

  @Before
  public void setup() {
    // Clear URL caches before each test to ensure test isolation
    gitClientFactory.clearUrlCaches();

    spyGitClientFactory = spy(gitClientFactory);
    mockGitApiClientUtils = mock(GitApiClientUtils.class);
    lenient().doReturn(mockGitApiClientUtils).when(spyGitClientFactory).getClientUtils(eq(GITHUB),
        any(Configuration.class));
  }

  @After
  @Override
  public void tearDown() {
    // Reset tenant context after each test to prevent cross-test pollution
    TenantTestHelper.resetAfterTest();
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

  @Test
  public void testGetApiUrl_multiTenantCacheIsolation() {
    String githubEnterpriseUrl1 = "https://github.example.com/org1/repo";
    String githubEnterpriseUrl2 = "https://github.example.com/org2/repo";
    String apiUrl1 = "https://github.example.com/api/v3/org1";
    String apiUrl2 = "https://github.example.com/api/v3/org2";

    GitRepositoryInfo repoInfo1 = createRepoInfo(githubEnterpriseUrl1, GITHUB);
    GitRepositoryInfo repoInfo2 = createRepoInfo(githubEnterpriseUrl2, GITHUB);

    Tenant tenant1 = testAsNewTenant("tenant1", t1 ->
        gitClientFactory.addApiUrlMapping(githubEnterpriseUrl1, apiUrl1));

    Tenant tenant2 = testAsNewTenant("tenant2", t2 ->
        gitClientFactory.addApiUrlMapping(githubEnterpriseUrl2, apiUrl2));

    testAsTenant(tenant1, t1 -> {
      Configuration config = new Configuration();
      String retrievedUrl1 = gitClientFactory.getApiUrl(repoInfo1, config);
      String retrievedUrl2 = gitClientFactory.getApiUrl(repoInfo2, config);

      assertThat(retrievedUrl2).describedAs(
              "Tenant1 should not have access to tenant2's cached URL")
          .isNotEqualTo(apiUrl2);
      assertThat(retrievedUrl1).isEqualTo(apiUrl1);
    });

    testAsTenant(tenant2, t2 -> {
      Configuration config = new Configuration();
      String retrievedUrl2 = gitClientFactory.getApiUrl(repoInfo2, config);
      assertThat(retrievedUrl2).isEqualTo(apiUrl2);

      String retrievedUrl1 = gitClientFactory.getApiUrl(repoInfo1, config);
      assertThat(retrievedUrl1).describedAs(
              "Tenant2 should not have access to tenant1's cached URL")
          .isNotEqualTo(apiUrl1);
    });
  }

  @Test
  public void testGetPullRequestInfoClientUrl_multiTenantCacheIsolation() {
    String githubEnterpriseUrl1 = "https://github.example.com/org1/repo";
    String githubEnterpriseUrl2 = "https://github.example.com/org2/repo";
    String prInfoUrl1 = "https://github.example.com/api/graphql/org1";
    String prInfoUrl2 = "https://github.example.com/api/graphql/org2";

    GitRepositoryInfo repoInfo1 = createRepoInfo(githubEnterpriseUrl1, GITHUB);
    GitRepositoryInfo repoInfo2 = createRepoInfo(githubEnterpriseUrl2, GITHUB);

    Tenant tenant1 = testAsNewTenant("tenant1", t1 ->
        gitClientFactory.addPullRequestInfoClientUrlMapping(githubEnterpriseUrl1, prInfoUrl1));

    Tenant tenant2 = testAsNewTenant("tenant2", t2 ->
        gitClientFactory.addPullRequestInfoClientUrlMapping(githubEnterpriseUrl2, prInfoUrl2));

    testAsTenant(tenant1, t1 -> {
      Configuration config = new Configuration();
      String retrievedUrl1 = gitClientFactory.getPullRequestInfoClientUrl(repoInfo1, config);
      String retrievedUrl2 = gitClientFactory.getPullRequestInfoClientUrl(repoInfo2, config);

      assertThat(retrievedUrl2).describedAs(
          "Tenant1 should not have access to tenant2's cached PR info URL")
          .isNotEqualTo(prInfoUrl2);
      assertThat(retrievedUrl1).isEqualTo(prInfoUrl1);
    });

    testAsTenant(tenant2, t2 -> {
      Configuration config = new Configuration();
      String retrievedUrl2 = gitClientFactory.getPullRequestInfoClientUrl(repoInfo2, config);
      assertThat(retrievedUrl2).isEqualTo(prInfoUrl2);

      String retrievedUrl1 = gitClientFactory.getPullRequestInfoClientUrl(repoInfo1, config);
      assertThat(retrievedUrl1).describedAs(
          "Tenant2 should not have access to tenant1's cached PR info URL")
          .isNotEqualTo(prInfoUrl1);
    });
  }

  @Test
  public void testGetPullRequestInfoClientUrl_multiTenantIdenticalRepoAndApiUrls() {
    String identicalRepoUrl = "https://github.com/facebook/react";
    String identicalPrInfoUrl = "https://api.github.com/graphql";

    GitRepositoryInfo repoInfo = createRepoInfo(identicalRepoUrl, GITHUB);
    Configuration config = new Configuration();

    Tenant tenant1 = testAsNewTenant("tenant1", t1 ->
        gitClientFactory.addPullRequestInfoClientUrlMapping(identicalRepoUrl, identicalPrInfoUrl));

    Tenant tenant2 = testAsNewTenant("tenant2", t2 ->
        gitClientFactory.addPullRequestInfoClientUrlMapping(identicalRepoUrl, identicalPrInfoUrl));

    testAsTenant(tenant1, t1 -> {
      String retrievedUrl = gitClientFactory.getPullRequestInfoClientUrl(repoInfo, config);
      assertThat(retrievedUrl).isEqualTo(identicalPrInfoUrl);
    });

    testAsTenant(tenant2, t2 -> {
      String retrievedUrl = gitClientFactory.getPullRequestInfoClientUrl(repoInfo, config);
      assertThat(retrievedUrl).isEqualTo(identicalPrInfoUrl);
    });
  }

  @Test
  public void testGetApiUrl_cacheKeyWithContextPath() {
    String urlWithContext = "https://scm.example.com/gitlab/group1/project";
    String apiUrl = "https://scm.example.com/gitlab/api/v4";

    GitRepositoryInfo repoInfo = createRepoInfo(urlWithContext, GITLAB);
    Configuration config = new Configuration();

    gitClientFactory.addApiUrlMapping(urlWithContext, apiUrl);

    String retrievedUrl = gitClientFactory.getApiUrl(repoInfo, config);
    assertThat(retrievedUrl).isEqualTo(apiUrl);
  }

  @Test
  public void testGetPullRequestInfoClientUrl_cacheKeyWithContextPath() {
    String urlWithContext = "https://scm.example.com/gitlab/group1/project";
    String prInfoUrl = "https://scm.example.com/gitlab/api/v4/graphql";

    GitRepositoryInfo repoInfo = createRepoInfo(urlWithContext, GITLAB);
    Configuration config = new Configuration();

    gitClientFactory.addPullRequestInfoClientUrlMapping(urlWithContext, prInfoUrl);

    String retrievedUrl = gitClientFactory.getPullRequestInfoClientUrl(repoInfo, config);
    assertThat(retrievedUrl).isEqualTo(prInfoUrl);
  }

  @Test
  public void testGetApiUrl_cacheKeyWithoutContextPath() {
    String urlWithoutContext = "https://github.com/myorg/myrepo";
    String apiUrl = "https://api.github.com/myorg";

    GitRepositoryInfo repoInfo = createRepoInfo(urlWithoutContext, GITHUB);
    Configuration config = new Configuration();

    gitClientFactory.addApiUrlMapping(urlWithoutContext, apiUrl);

    String retrievedUrl = gitClientFactory.getApiUrl(repoInfo, config);
    assertThat(retrievedUrl).isEqualTo(apiUrl);
  }

  @Test
  public void testGetPullRequestInfoClientUrl_cacheKeyWithoutContextPath() {
    String urlWithoutContext = "https://github.com/myorg/myrepo";
    String prInfoUrl = "https://api.github.com/graphql/myorg";

    GitRepositoryInfo repoInfo = createRepoInfo(urlWithoutContext, GITHUB);
    Configuration config = new Configuration();

    gitClientFactory.addPullRequestInfoClientUrlMapping(urlWithoutContext, prInfoUrl);

    String retrievedUrl = gitClientFactory.getPullRequestInfoClientUrl(repoInfo, config);
    assertThat(retrievedUrl).isEqualTo(prInfoUrl);
  }

  @Test
  public void testGetApiUrl_cacheKeyWithNonStandardPort() {
    String urlWithPort = "https://gitlab.example.com:8443/group1/project";
    String apiUrl = "https://gitlab.example.com:8443/api/v4";

    GitRepositoryInfo repoInfo = createRepoInfo(urlWithPort, GITLAB);
    Configuration config = new Configuration();

    gitClientFactory.addApiUrlMapping(urlWithPort, apiUrl);

    String retrievedUrl = gitClientFactory.getApiUrl(repoInfo, config);
    assertThat(retrievedUrl).isEqualTo(apiUrl);
  }

  @Test
  public void testGetPullRequestInfoClientUrl_cacheKeyWithNonStandardPort() {
    String urlWithPort = "https://gitlab.example.com:8443/group1/project";
    String prInfoUrl = "https://gitlab.example.com:8443/api/v4/graphql";

    GitRepositoryInfo repoInfo = createRepoInfo(urlWithPort, GITLAB);
    Configuration config = new Configuration();

    gitClientFactory.addPullRequestInfoClientUrlMapping(urlWithPort, prInfoUrl);

    String retrievedUrl = gitClientFactory.getPullRequestInfoClientUrl(repoInfo, config);
    assertThat(retrievedUrl).isEqualTo(prInfoUrl);
  }

  @Test
  public void testGetApiUrl_cacheKeyWithNoPath() {
    String urlWithNoPath = "https://github.com";
    String apiUrl = "https://api.github.com";

    GitRepositoryInfo repoInfo = createRepoInfo(urlWithNoPath, GITHUB);
    Configuration config = new Configuration();

    gitClientFactory.addApiUrlMapping(urlWithNoPath, apiUrl);

    String retrievedUrl = gitClientFactory.getApiUrl(repoInfo, config);
    assertThat(retrievedUrl).isEqualTo(apiUrl);
  }

  @Test
  public void testGetPullRequestInfoClientUrl_cacheKeyWithNoPath() {
    String urlWithNoPath = "https://github.com";
    String prInfoUrl = "https://api.github.com/graphql";

    GitRepositoryInfo repoInfo = createRepoInfo(urlWithNoPath, GITHUB);
    Configuration config = new Configuration();

    gitClientFactory.addPullRequestInfoClientUrlMapping(urlWithNoPath, prInfoUrl);

    String retrievedUrl = gitClientFactory.getPullRequestInfoClientUrl(repoInfo, config);
    assertThat(retrievedUrl).isEqualTo(prInfoUrl);
  }

  private GitRepositoryInfo createRepoInfo(String url, SourceControlProvider provider) {
    return new GitRepositoryInfo(url, null, null, "token", provider, "main",
        true, true, true, true, true, true, false, null);
  }
}
