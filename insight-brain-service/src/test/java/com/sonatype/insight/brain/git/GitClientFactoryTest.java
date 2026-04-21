/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.api.ContributorInfoProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.GitApiClientUtils;
import com.sonatype.nexus.scm.api.PullRequestInfoProvider;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.github.GitHubApiClient;
import com.sonatype.nexus.scm.github.graphql.GitHubGraphQlClient;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.io.IOException;
import java.util.Date;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
  private static final int WIREMOCK_PORT = 18090;

  private static final String GITHUB_INSTALLATION_TOKEN = "ghs_test_installation_token_123456";

  private static final GitRepositoryInfo GIT_REPO_INFO =
      new GitRepositoryInfo("https://github.com/org/repo", null, null, "token",
          GITHUB, "main", true, true, true, true, true, true, false, null);

  @Rule
  public WireMockRule githubMockServer = new WireMockRule(wireMockConfig().port(WIREMOCK_PORT));

  @Inject
  private GitClientFactory gitClientFactory;

  @Inject
  private GitHubAppDAO gitHubAppDAO;

  private GitClientFactory spyGitClientFactory;

  private GitApiClientUtils mockGitApiClientUtils;

  /**
   * Override GitHubAppAuthStrategyCache bean to use WireMock URL for tests.
   */
  @Override
  public void configure(Binder binder) {
    // Get providers for dependencies - they'll be resolved lazily
    Provider<GitHubAppDAO> githubAppDAOProvider = binder.getProvider(GitHubAppDAO.class);
    Provider<InsightProxy> insightProxyProvider = binder.getProvider(InsightProxy.class);
    Provider<GitApiClientFactory> gitApiClientFactoryProvider = binder.getProvider(GitApiClientFactory.class);
    Provider<PasswordHandler> passwordHandlerProvider = binder.getProvider(PasswordHandler.class);

    // Create provider that uses WireMock URL
    binder.bind(GitHubAppAuthStrategyCache.class)
        .toProvider(() -> new GitHubAppAuthStrategyCache(
            githubAppDAOProvider.get(),
            insightProxyProvider.get(),
            gitApiClientFactoryProvider.get(),
            passwordHandlerProvider.get(),
            "http://localhost:" + WIREMOCK_PORT));
    super.configure(binder);
  }

  @Before
  public void setup() {
    // Clear URL caches before each test to ensure test isolation
    gitClientFactory.clearUrlCaches();

    spyGitClientFactory = spy(gitClientFactory);
    mockGitApiClientUtils = mock(GitApiClientUtils.class);
    lenient().doReturn(mockGitApiClientUtils)
        .when(spyGitClientFactory)
        .getClientUtils(eq(GITHUB),
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

    Tenant tenant1 = testAsNewTenant("tenant1", t1 -> gitClientFactory.addApiUrlMapping(githubEnterpriseUrl1, apiUrl1));

    Tenant tenant2 = testAsNewTenant("tenant2", t2 -> gitClientFactory.addApiUrlMapping(githubEnterpriseUrl2, apiUrl2));

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

    Tenant tenant1 = testAsNewTenant("tenant1",
        t1 -> gitClientFactory.addPullRequestInfoClientUrlMapping(githubEnterpriseUrl1, prInfoUrl1));

    Tenant tenant2 = testAsNewTenant("tenant2",
        t2 -> gitClientFactory.addPullRequestInfoClientUrlMapping(githubEnterpriseUrl2, prInfoUrl2));

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

    Tenant tenant1 = testAsNewTenant("tenant1",
        t1 -> gitClientFactory.addPullRequestInfoClientUrlMapping(identicalRepoUrl, identicalPrInfoUrl));

    Tenant tenant2 = testAsNewTenant("tenant2",
        t2 -> gitClientFactory.addPullRequestInfoClientUrlMapping(identicalRepoUrl, identicalPrInfoUrl));

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

  @Test
  public void testCreateApiClient_GitHubApp_NotFound() {
    // Given: A repository configured for GitHub App auth but no GitHub App exists
    GitRepositoryInfo repoInfo = createRepoInfo("https://github.com/org/repo", GITHUB);
    repoInfo.authenticationType =
        com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType.GITHUB_APP;
    repoInfo.authOwnerId = "non-existent-owner";

    // When/Then: Creating API client should throw UncheckedExecutionException wrapping NotFoundException
    assertThatThrownBy(() -> gitClientFactory.createApiClient(repoInfo))
        .isInstanceOf(com.google.common.util.concurrent.UncheckedExecutionException.class)
        .hasMessageContaining("GitHub App not found")
        .hasMessageContaining("non-existent-owner")
        .hasCauseInstanceOf(NotFoundException.class);
  }

  @Test
  public void testCreateApiClient_GitHubApp_MissingOwnerId() {
    // Given: A repository configured for GitHub App auth but ownerId is null
    GitRepositoryInfo repoInfo = createRepoInfo("https://github.com/org/repo", GITHUB);
    repoInfo.authenticationType =
        com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType.GITHUB_APP;
    repoInfo.authOwnerId = null; // Missing ownerId - should NOT fallback to PAT

    // When/Then: Creating API client should throw IllegalStateException (fail fast)
    assertThatThrownBy(() -> gitClientFactory.createApiClient(repoInfo))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GitHub App authentication is configured but no owner ID found")
        .hasMessageContaining("https://github.com/org/repo");
  }

  @Test
  public void testCreatePullRequestInfoClient_GitHubApp_MissingOwnerId() {
    // Given: A repository configured for GitHub App auth but ownerId is null
    GitRepositoryInfo repoInfo = createRepoInfo("https://github.com/org/repo", GITHUB);
    repoInfo.authenticationType =
        com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType.GITHUB_APP;
    repoInfo.authOwnerId = null; // Missing ownerId - should NOT fallback to PAT

    // When/Then: Creating PR info client should throw IllegalStateException (fail fast)
    assertThatThrownBy(() -> gitClientFactory.createPullRequestInfoClient(repoInfo))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GitHub App authentication is configured but no owner ID found")
        .hasMessageContaining("https://github.com/org/repo");
  }

  @Test
  public void testCreateContributorInfoProvider_GitHubApp_MissingOwnerId() {
    // Given: A repository configured for GitHub App auth but ownerId is null
    GitRepositoryInfo repoInfo = createRepoInfo("https://github.com/org/repo", GITHUB);
    repoInfo.authenticationType =
        com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType.GITHUB_APP;
    repoInfo.authOwnerId = null; // Missing ownerId - should NOT fallback to PAT

    // When/Then: Creating contributor info provider should throw IllegalStateException (fail fast)
    assertThatThrownBy(() -> gitClientFactory.createContributorInfoProvider(repoInfo))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GitHub App authentication is configured but no owner ID found")
        .hasMessageContaining("https://github.com/org/repo");
  }

  @Test
  public void testCreatePullRequestInfoClient_WithGitHubApp_MakesGraphQlCallWithAuth() throws Exception {
    // Mock GitHub installation token endpoint (installation ID 7890123 from createTestGitHubApp)
    githubMockServer.stubFor(
        post(urlPathEqualTo("/app/installations/7890123/access_tokens"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"token\":\"" + GITHUB_INSTALLATION_TOKEN + "\"," +
                    "\"expires_at\":\"2099-01-01T00:00:00Z\"}")));

    // Mock GraphQL endpoint
    githubMockServer.stubFor(
        post(urlPathEqualTo("/api/graphql"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"data\":{\"search\":{\"edges\":[],\"pageInfo\":{\"hasNextPage\":false}}}}")));

    // Create test application and GitHub App
    Organization org = tempEntity.newOrganization("test-org");
    Application app = tempEntity.newApplication(org.getId());
    GitHubApp githubApp = createTestGitHubApp(app.getId());
    tempEntity.newGitHubApp(githubApp);

    // Create GitRepositoryInfo with WireMock URL and GitHub App authentication
    GitRepositoryInfo repoInfo = createRepoInfo("http://localhost:" + WIREMOCK_PORT + "/test-org/repo", GITHUB);
    repoInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    repoInfo.authOwnerId = app.getId();

    // Create GraphQL client and make API call
    PullRequestInfoProvider prInfoClient = gitClientFactory.createPullRequestInfoClient(repoInfo);
    assertThat(prInfoClient).isInstanceOf(GitHubGraphQlClient.class);

    GitHubGraphQlClient graphQlClient = (GitHubGraphQlClient) prInfoClient;
    graphQlClient.getPullRequestsSince("test-org", java.time.OffsetDateTime.now().minusDays(30), 10);

    // Verify GraphQL endpoint was called with GitHub App authentication
    githubMockServer.verify(
        postRequestedFor(urlPathEqualTo("/api/graphql"))
            .withHeader("Authorization", containing(GITHUB_INSTALLATION_TOKEN)));
  }

  @Test
  public void testCreateContributorInfoProvider_WithGitHubApp_Success() {
    // Create test application and GitHub App
    Organization org = tempEntity.newOrganization("test-org");
    Application app = tempEntity.newApplication(org.getId());

    // Create GitHub App
    GitHubApp githubApp = createTestGitHubApp(app.getId());
    tempEntity.newGitHubApp(githubApp);

    // Create GitRepositoryInfo with GitHub App authentication
    GitRepositoryInfo repoInfo = createRepoInfo("https://github.com/test-org/repo", GITHUB);
    repoInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    repoInfo.authOwnerId = app.getId();

    // Create contributor info provider
    ContributorInfoProvider contributorProvider = gitClientFactory.createContributorInfoProvider(repoInfo);

    // Verify provider was created successfully
    assertThat(contributorProvider).isNotNull();
  }

  @Test
  public void testCreateApiClient_UsesAuthStrategyCaching() throws Exception {
    // Mock GitHub installation token endpoint (installation ID 7890123 from createTestGitHubApp)
    githubMockServer.stubFor(
        post(urlPathEqualTo("/app/installations/7890123/access_tokens"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"token\":\"" + GITHUB_INSTALLATION_TOKEN + "\"," +
                    "\"expires_at\":\"2099-01-01T00:00:00Z\"}")));

    // Mock GraphQL endpoint for API calls
    githubMockServer.stubFor(
        post(urlPathEqualTo("/api/graphql"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"data\":{\"search\":{\"edges\":[],\"pageInfo\":{\"hasNextPage\":false}}}}")));

    // Create test application and GitHub App
    Organization org = tempEntity.newOrganization("test-org");
    Application app = tempEntity.newApplication(org.getId());

    // Create GitHub App
    GitHubApp githubApp = createTestGitHubApp(app.getId());
    tempEntity.newGitHubApp(githubApp);

    // Create GitRepositoryInfo with WireMock URL and GitHub App authentication
    GitRepositoryInfo repoInfo = createRepoInfo("http://localhost:" + WIREMOCK_PORT + "/test-org/repo", GITHUB);
    repoInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    repoInfo.authOwnerId = app.getId();

    // Create first PullRequestInfoProvider and make API call - should fetch installation token
    PullRequestInfoProvider prInfoClient1 = gitClientFactory.createPullRequestInfoClient(repoInfo);
    ((GitHubGraphQlClient) prInfoClient1).getPullRequestsSince("test-org",
        java.time.OffsetDateTime.now().minusDays(30), 10);

    // Verify the token endpoint was called once
    githubMockServer.verify(1, postRequestedFor(urlPathEqualTo("/app/installations/7890123/access_tokens")));

    // Create second PullRequestInfoProvider with same GitHub App and make API call
    // Should reuse cached auth strategy without fetching token again
    PullRequestInfoProvider prInfoClient2 = gitClientFactory.createPullRequestInfoClient(repoInfo);
    ((GitHubGraphQlClient) prInfoClient2).getPullRequestsSince("test-org",
        java.time.OffsetDateTime.now().minusDays(30), 10);

    // Verify the token endpoint was still only called once (cache was used)
    githubMockServer.verify(1, postRequestedFor(urlPathEqualTo("/app/installations/7890123/access_tokens")));

    // Verify both API calls were made successfully
    githubMockServer.verify(2, postRequestedFor(urlPathEqualTo("/api/graphql")));
  }

  @Test
  public void testCreateApiClient_WithGitHubApp_UsesAuthOwnerIdWhenAvailable() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    // Create GitHub App for parent organization
    GitHubApp githubApp = createTestGitHubApp(org.getId());
    tempEntity.newGitHubApp(githubApp);

    // Create GitRepositoryInfo with authOwnerId set to parent org
    GitRepositoryInfo repoInfo = createRepoInfo("https://github.com/test-org/repo", GITHUB);
    repoInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    repoInfo.authOwnerId = org.getId(); // Parent org ID

    // Execute
    GitApiClient apiClient = gitClientFactory.createApiClient(repoInfo);

    // Verify - the client should be created using the authOwnerId (parent org)
    assertThat(apiClient).isNotNull();
    // Verify the GitHub App was retrieved for the correct owner (parent org, not child app)
    GitHubApp retrievedApp = gitHubAppDAO.getByOwnerIdNotNull(org.getId());
    assertThat(retrievedApp).isNotNull();
    assertThat(retrievedApp.getOwnerId()).isEqualTo(org.getId());
    // Verify no GitHub App exists for the child app
    assertThat(gitHubAppDAO.getByOwnerId(app.getId())).isNull();
  }

  @Test
  public void testCreateApiClient_WithGitHubApp_ThrowsExceptionWhenAuthOwnerIdIsNull() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    // Create GitHub App for application
    GitHubApp githubApp = createTestGitHubApp(app.getId());
    tempEntity.newGitHubApp(githubApp);

    // Create GitRepositoryInfo with null authOwnerId
    GitRepositoryInfo repoInfo = createRepoInfo("https://github.com/test-org/repo", GITHUB);
    repoInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    repoInfo.authOwnerId = null;

    // Execute & Verify - should throw IllegalStateException when authOwnerId is null
    assertThatThrownBy(() -> gitClientFactory.createApiClient(repoInfo))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GitHub App authentication is configured but no owner ID found");
  }

  @Test
  public void testCreateApiClient_WithGitHubApp_ThrowsExceptionWhenBothOwnerIdsAreNull() {
    // Create GitRepositoryInfo with GitHub App auth but both owner IDs are null
    GitRepositoryInfo repoInfo = createRepoInfo("https://github.com/test-org/repo", GITHUB);
    repoInfo.authenticationType = SourceControl.AuthenticationType.GITHUB_APP;
    repoInfo.authOwnerId = null;
    repoInfo.authOwnerId = null;

    // Execute and verify exception
    assertThatThrownBy(() -> gitClientFactory.createApiClient(repoInfo))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GitHub App authentication is configured but no owner ID found");
  }

  @Test
  public void testCreateGeneralApiClient_GitHubAppAuth_Success() throws IOException {
    // Given: GitHub App with valid configuration
    Organization org = tempEntity.newOrganization("test-org");
    GitHubApp githubApp = createTestGitHubApp(org.getId());
    tempEntity.newGitHubApp(githubApp);

    // When: Creating general API client with GitHub App
    GeneralSCMApiClient apiClient = gitClientFactory.createGeneralApiClient(
        GITHUB,
        "https://github.com",
        githubApp);

    // Then: Client is created successfully
    assertThat(apiClient).isNotNull();
  }

  @Test
  public void testCreateGeneralApiClient_GitHubAppAuth_OnlySupportsGitHub() {
    // Given: GitHub App
    Organization org = tempEntity.newOrganization("test-org");
    GitHubApp githubApp = createTestGitHubApp(org.getId());

    // When/Then: GitLab provider should throw exception
    assertThatThrownBy(() -> gitClientFactory.createGeneralApiClient(GITLAB, "https://gitlab.com", githubApp))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("GitHub App authentication only supports GitHub provider");

    // When/Then: Bitbucket provider should throw exception
    assertThatThrownBy(() -> gitClientFactory.createGeneralApiClient(BITBUCKET, "https://bitbucket.org", githubApp))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("GitHub App authentication only supports GitHub provider");
  }

  @Test
  public void testCreateGeneralApiClient_GitHubAppAuth_RequiresInstallationId() {
    // Given: GitHub App without installation ID
    Organization org = tempEntity.newOrganization("test-org");
    GitHubApp githubApp = createTestGitHubApp(org.getId());
    githubApp.setInstallationId(null);

    // When/Then: Should throw exception
    assertThatThrownBy(() -> gitClientFactory.createGeneralApiClient(GITHUB, "https://github.com", githubApp))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("GitHub App installation ID is required");
  }

  @Test
  public void testCreateGeneralApiClient_GitHubAppAuth_WithGitHubEnterprise() throws IOException {
    // Given: GitHub App configured for GitHub Enterprise
    Organization org = tempEntity.newOrganization("test-org");
    GitHubApp githubApp = createTestGitHubApp(org.getId());
    tempEntity.newGitHubApp(githubApp);
    String enterpriseUrl = "https://github.enterprise.com";

    // When: Creating client for GitHub Enterprise
    GeneralSCMApiClient apiClient = gitClientFactory.createGeneralApiClient(
        GITHUB,
        enterpriseUrl,
        githubApp);

    // Then: Client is created successfully
    assertThat(apiClient).isNotNull();
  }

  @Test
  public void testCreateGeneralApiClient_GitHubAppAuth_DecryptsPrivateKey() throws IOException {
    // Given: GitHub App with encrypted private key
    Organization org = tempEntity.newOrganization("test-org");
    GitHubApp githubApp = createTestGitHubApp(org.getId());
    tempEntity.newGitHubApp(githubApp);

    // Verify private key is encrypted
    assertThat(githubApp.getPrivateKey()).isNotNull();

    // When: Creating client (should decrypt key internally)
    GeneralSCMApiClient apiClient = null;
    try {
      apiClient = gitClientFactory.createGeneralApiClient(
          SourceControlProvider.GITHUB,
          "https://github.com",
          githubApp);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Then: Client is created successfully (private key was decrypted)
    assertThat(apiClient).isNotNull();
  }

  @Test
  public void testCreateGeneralApiClient_GitHubAppAuth_HandlesInvalidPrivateKey() {
    // Given: GitHub App with invalid private key
    Organization org = tempEntity.newOrganization("test-org");
    GitHubApp githubApp = createTestGitHubApp(org.getId());
    githubApp.setPrivateKey("invalid-encrypted-key");
    tempEntity.newGitHubApp(githubApp);

    // When/Then: Should throw exception during key decryption/parsing
    assertThatThrownBy(() -> gitClientFactory.createGeneralApiClient(GITHUB, "https://github.com", githubApp))
        .isInstanceOf(RuntimeException.class);
  }

  private GitHubApp createTestGitHubApp(String ownerId) {
    GitHubApp githubApp = new GitHubApp();
    githubApp.setId(java.util.UUID.randomUUID().toString());
    githubApp.setOwnerId(ownerId);
    githubApp.setAppId(123456);
    githubApp.setSlug("test-app");
    githubApp.setClientId("test-client-id");
    githubApp.setClientSecret("test-client-secret");
    githubApp.setGithubOrganizationName("test-org");
    githubApp.setInstallationId(7890123L);
    githubApp.setLastUpdatedAt(new Date());

    // Convert PEM to Base64 PKCS8, then encrypt
    String pemKey = "-----BEGIN PRIVATE KEY-----\n" +
        "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCVsrVrXls0IWh5\n" +
        "ck+58RCytTi1nByt+YiOgsRQ9kB+Iy4OmTiQq8UjIUQJW/sxC2M9FMucWNmK9btQ\n" +
        "NqoLOay/JvOp5zIrBCjv9MwOyJOvx0QY5Jq2Gq9clA8eY3pOB+b/LdbtMypzi7bq\n" +
        "O5ncq5Wf4f8+8q3qEWj9FADgJTvV0jvItP6eIoZfl12SNWBHGjo0gnaltHr/WI98\n" +
        "KIlMCqYmTTmg1ncoZlN1RnDAJh0C1+QEL40vqTD1m6iEzURA3HG8QQhD4n+z+ofb\n" +
        "rSxfYe+LNpBfngRPzjR+aECYhZZ1W0nMGDv1uYe5G19+nw1x9ZXbjkkKFZ27L4j/\n" +
        "G+TA9R3DAgMBAAECggEAAs285dFTKIkTErM4PVNIyDShQiDsqJV8+4m8A4grcZ8N\n" +
        "6TODJyA1BZEgyaeD7yTuUAaM0tVgT/MX9d00zYWXAhjtO+zRuEo98OUiiK19lp00\n" +
        "y5TX7F7qbnO8Anf6fdujdZ92KVH8AGlteCfhCdWRbGZM48xaDFzLryiXm5sW6qf3\n" +
        "JfSoBR6W9ivd3BliCK7jfnk2y/trzX/1hgBnymgIXHXSk7bNU8EGxCLOdTG+7TKJ\n" +
        "K1ugFkrjrdgSj4FkOo9ckApRs+jNkZkCH9/VxUZsB/HqvJzzi3ytTebrqoNXHLuQ\n" +
        "UKDjGErnL3rLFfMTeW2Gv6p8jMIj2t5DRhYKDRk8AQKBgQDFin0MsAyMCNrM/1r5\n" +
        "goe8r5w52bkbAmdOIDsYOeMmUfO2a75F3awrxGaMUxRMxC1QdO6z2Sr5a0AuNCBq\n" +
        "dHRX5YDyjBOGoWOqX4mtw7EpNkOET02rAm2tOVEIhOOqhwz1VVBKm9Wk4AuhbO+a\n" +
        "wH6njGOoaeplwvpVJO8Wyst0IQKBgQDB/7CAVoopfqsJ6Bsl+rnm8sFiU9yr1U4P\n" +
        "94hcOUhK7f6oyU5SXiOzP1Mx5K850iUyRVCT0CbNyx/Nl1v7iWS1YAqRFPY7jSpZ\n" +
        "fK7zSvcOqFO9O/+/8czRVs09BYm/Go9NoW9zAxFIm6DYnFF5nqnnRGvGNLPo+xpq\n" +
        "uMTZs7CVYwKBgQCShRAPsxz7WS4BU35FB15qw86a0jUMJZI+ToXGiFlFeQ/NxMjS\n" +
        "xYMIy5pMhurNrcz2mmTbHT9U1Qo7uwo4K7yH3YDxZpitCVQFcOuL6VSkfs1BfBjd\n" +
        "uOVk0Nib+wVq3NTtu6PcUw36RvwZddWa8SCAYg8hQb5MUHyhXs3AGBckQQKBgCOz\n" +
        "BavYQPx5zse36qcGiIczTNrnS8hjLEZL6s/typvfR+mPgdYudKtbj9eymXwua6Hg\n" +
        "l39b4ogkROn0XHzhP6MQ1WD1VoqG47Ar/ZXPyb7swtwj2mBcArDTJFmCV2LPZGeI\n" +
        "uZWUju2plePGgEe9Js7kDGEg+ap56taQwci+BFS5AoGBALD//nynCo8oBGqVOBCp\n" +
        "e6X36qLcHE8YkM//FplnhsKPrzqdSXiP2T+BNrzj/rcHdPrA4Js5mggEtXk47/Vk\n" +
        "LoPyDbBvEvkkOnmTjwfmKtFkVykt4q1etctaUyKkzGz6ICKxC73ET/hFlN9r0LXM\n" +
        "JYwq8nvsGtyZSCMRwEVmvb+h\n" +
        "-----END PRIVATE KEY-----";

    // Convert PEM to Base64 PKCS8 (strip headers and newlines)
    String base64Pkcs8 = pemKey
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s", "");

    // Encrypt the Base64 PKCS8 key
    PasswordHandler passwordHandler = lookup(PasswordHandler.class);
    char[] encryptedKey = passwordHandler.encryptPassword(base64Pkcs8.toCharArray());
    githubApp.setPrivateKey(String.valueOf(encryptedKey));
    githubApp.setActive(true);

    return githubApp;
  }
}
