/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.api.auth.AuthenticationStrategy;
import com.sonatype.nexus.scm.github.auth.GitHubAppAuthStrategy;
import jakarta.inject.Inject;
import java.util.Date;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = GitHubAppAuthStrategyCacheTest.GitHubAppAuthStrategyCacheTestConfig.class)
public class GitHubAppAuthStrategyCacheTest
    extends AbstractComponentTest
{
  @TestConfiguration
  static class GitHubAppAuthStrategyCacheTestConfig
  {
    @Bean
    @Primary
    GitHubAppAuthStrategyCache gitHubAppAuthStrategyCache(
        final GitHubAppDAO githubAppDAO,
        final InsightProxy insightProxy,
        final GitApiClientFactory gitApiClientFactory,
        final PasswordHandler passwordHandler)
    {
      return new GitHubAppAuthStrategyCache(
          githubAppDAO,
          insightProxy,
          gitApiClientFactory,
          passwordHandler,
          "http://localhost:" + WIREMOCK_PORT);
    }
  }

  private static final int WIREMOCK_PORT = 18089;

  @Rule
  public WireMockRule githubMockServer = new WireMockRule(wireMockConfig().port(WIREMOCK_PORT));

  @Inject
  private GitHubAppAuthStrategyCache cache;

  private static final String TEST_OWNER_ID = "test-owner-123";

  private static int appIdCounter = 10000;

  private static int installationIdCounter = 10000;

  private static int tokenCounter = 10000;

  @After
  public void tearDown() {
    // Reset tenant context after each test to prevent cross-test pollution
    TenantTestHelper.resetAfterTest();
  }

  /**
   * Add a WireMock stub that returns a unique token for each installation ID.
   * Call this before each operation that requires a fresh token.
   */
  private void stubGitHubTokenEndpointWithUniqueToken() {
    String uniqueToken = "ghs_test_token_" + (tokenCounter++);
    githubMockServer.stubFor(
        post(urlPathMatching("/app/installations/\\d+/access_tokens"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{" +
                    "\"token\":\"" + uniqueToken + "\"," +
                    "\"expires_at\":\"2099-01-01T00:00:00Z\"" +
                    "}")));
  }

  /**
   * Add a WireMock stub for a specific installation ID that returns a unique token.
   * Use this when you need different tokens for different installation IDs in the same test.
   */
  private void stubGitHubTokenEndpointForInstallation(long installationId) {
    String uniqueToken = "ghs_test_token_" + installationId + "_" + (tokenCounter++);
    githubMockServer.stubFor(
        post(urlPathMatching("/app/installations/" + installationId + "/access_tokens"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{" +
                    "\"token\":\"" + uniqueToken + "\"," +
                    "\"expires_at\":\"2099-01-01T00:00:00Z\"" +
                    "}")));
  }

  /**
   * Test-only RSA private key in Base64-encoded PKCS#8 format - 2048-bit key for RS256 algorithm.
   * This is the raw Base64 PKCS8 bytes (PEM format with headers/footers removed and newlines stripped).
   * This matches the format that is stored in the database BEFORE encryption.
   */
  private static final String VALID_BASE64_PKCS8 =
      "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCVsrVrXls0IWh5" +
          "ck+58RCytTi1nByt+YiOgsRQ9kB+Iy4OmTiQq8UjIUQJW/sxC2M9FMucWNmK9btQ" +
          "NqoLOay/JvOp5zIrBCjv9MwOyJOvx0QY5Jq2Gq9clA8eY3pOB+b/LdbtMypzi7bq" +
          "O5ncq5Wf4f8+8q3qEWj9FADgJTvV0jvItP6eIoZfl12SNWBHGjo0gnaltHr/WI98" +
          "KIlMCqYmTTmg1ncoZlN1RnDAJh0C1+QEL40vqTD1m6iEzURA3HG8QQhD4n+z+ofb" +
          "rSxfYe+LNpBfngRPzjR+aECYhZZ1W0nMGDv1uYe5G19+nw1x9ZXbjkkKFZ27L4j/" +
          "G+TA9R3DAgMBAAECggEAAs285dFTKIkTErM4PVNIyDShQiDsqJV8+4m8A4grcZ8N" +
          "6TODJyA1BZEgyaeD7yTuUAaM0tVgT/MX9d00zYWXAhjtO+zRuEo98OUiiK19lp00" +
          "y5TX7F7qbnO8Anf6fdujdZ92KVH8AGlteCfhCdWRbGZM48xaDFzLryiXm5sW6qf3" +
          "JfSoBR6W9ivd3BliCK7jfnk2y/trzX/1hgBnymgIXHXSk7bNU8EGxCLOdTG+7TKJ" +
          "K1ugFkrjrdgSj4FkOo9ckApRs+jNkZkCH9/VxUZsB/HqvJzzi3ytTebrqoNXHLuQ" +
          "UKDjGErnL3rLFfMTeW2Gv6p8jMIj2t5DRhYKDRk8AQKBgQDFin0MsAyMCNrM/1r5" +
          "goe8r5w52bkbAmdOIDsYOeMmUfO2a75F3awrxGaMUxRMxC1QdO6z2Sr5a0AuNCBq" +
          "dHRX5YDyjBOGoWOqX4mtw7EpNkOET02rAm2tOVEIhOOqhwz1VVBKm9Wk4AuhbO+a" +
          "wH6njGOoaeplwvpVJO8Wyst0IQKBgQDB/7CAVoopfqsJ6Bsl+rnm8sFiU9yr1U4P" +
          "94hcOUhK7f6oyU5SXiOzP1Mx5K850iUyRVCT0CbNyx/Nl1v7iWS1YAqRFPY7jSpZ" +
          "fK7zSvcOqFO9O/+/8czRVs09BYm/Go9NoW9zAxFIm6DYnFF5nqnnRGvGNLPo+xpq" +
          "uMTZs7CVYwKBgQCShRAPsxz7WS4BU35FB15qw86a0jUMJZI+ToXGiFlFeQ/NxMjS" +
          "xYMIy5pMhurNrcz2mmTbHT9U1Qo7uwo4K7yH3YDxZpitCVQFcOuL6VSkfs1BfBjd" +
          "uOVk0Nib+wVq3NTtu6PcUw36RvwZddWa8SCAYg8hQb5MUHyhXs3AGBckQQKBgCOz" +
          "BavYQPx5zse36qcGiIczTNrnS8hjLEZL6s/typvfR+mPgdYudKtbj9eymXwua6Hg" +
          "l39b4ogkROn0XHzhP6MQ1WD1VoqG47Ar/ZXPyb7swtwj2mBcArDTJFmCV2LPZGeI" +
          "uZWUju2plePGgEe9Js7kDGEg+ap56taQwci+BFS5AoGBALD//nynCo8oBGqVOBCp" +
          "e6X36qLcHE8YkM//FplnhsKPrzqdSXiP2T+BNrzj/rcHdPrA4Js5mggEtXk47/Vk" +
          "LoPyDbBvEvkkOnmTjwfmKtFkVykt4q1etctaUyKkzGz6ICKxC73ET/hFlN9r0LXM" +
          "JYwq8nvsGtyZSCMRwEVmvb+h";

  @Test
  public void testGetOrCreate_CreatesNewStrategy() {
    stubGitHubTokenEndpointWithUniqueToken();
    GitHubApp app = createTestGitHubApp(TEST_OWNER_ID);

    AuthenticationStrategy strategy = cache.getOrCreate(app.getId());

    assertThat(strategy).isNotNull();
  }

  @Test
  public void testGetOrCreate_UsesCacheOnSecondCall() {
    stubGitHubTokenEndpointWithUniqueToken();
    GitHubApp app = createTestGitHubApp(TEST_OWNER_ID);

    AuthenticationStrategy strategy1 = cache.getOrCreate(app.getId());
    AuthenticationStrategy strategy2 = cache.getOrCreate(app.getId());

    assertThat(strategy1).isSameAs(strategy2);
  }

  @Test
  public void testGetOrCreate_DifferentGitHubAppIdsLoadSeparately() throws Exception {
    String ownerId1 = "owner-1";
    String ownerId2 = "owner-2";

    GitHubApp app1 = createTestGitHubApp(ownerId1);
    GitHubApp app2 = createTestGitHubApp(ownerId2);

    // Stub tokens for each installation ID
    stubGitHubTokenEndpointForInstallation(app1.getInstallationId());
    stubGitHubTokenEndpointForInstallation(app2.getInstallationId());

    // Create both strategies (this loads tokens and caches them)
    AuthenticationStrategy strategy1 = cache.getOrCreate(app1.getId());
    AuthenticationStrategy strategy2 = cache.getOrCreate(app2.getId());

    // Now retrieve tokens from cached strategies
    GitHubAppAuthStrategy ghStrategy1 = (GitHubAppAuthStrategy) strategy1;
    String token1 = ghStrategy1.getInstallationToken().getToken();

    GitHubAppAuthStrategy ghStrategy2 = (GitHubAppAuthStrategy) strategy2;
    String token2 = ghStrategy2.getInstallationToken().getToken();

    // Verify they are different strategy objects
    assertThat(strategy1).isNotSameAs(strategy2);

    // Verify the actual token strings are different
    assertThat(token1).isNotEqualTo(token2);
  }

  @Test
  public void testGetOrCreate_ThrowsNotFoundException() {
    String nonExistentGithubAppId = "non-existent-github-app-id";

    // Guava's cache wraps exceptions in UncheckedExecutionException
    assertThatThrownBy(() -> cache.getOrCreate(nonExistentGithubAppId))
        .isInstanceOf(UncheckedExecutionException.class)
        .hasCauseInstanceOf(NotFoundException.class)
        .hasMessageContaining("GitHub App not found: " + nonExistentGithubAppId);
  }

  @Test
  public void testInvalidate_RemovesFromCache() throws Exception {
    GitHubApp app = createTestGitHubApp(TEST_OWNER_ID);

    // Stub to return different tokens on consecutive calls using scenarios
    String token1Value = "ghs_test_token_" + (tokenCounter++);
    String token2Value = "ghs_test_token_" + (tokenCounter++);

    githubMockServer.stubFor(
        post(urlPathMatching("/app/installations/" + app.getInstallationId() + "/access_tokens"))
            .inScenario("token-refresh")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"token\":\"" + token1Value + "\",\"expires_at\":\"2099-01-01T00:00:00Z\"}"))
            .willSetStateTo("second-call"));

    githubMockServer.stubFor(
        post(urlPathMatching("/app/installations/" + app.getInstallationId() + "/access_tokens"))
            .inScenario("token-refresh")
            .whenScenarioStateIs("second-call")
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"token\":\"" + token2Value + "\",\"expires_at\":\"2099-01-01T00:00:00Z\"}")));

    // Create both strategies
    AuthenticationStrategy strategy1 = cache.getOrCreate(app.getId());
    cache.invalidate(app.getId());
    AuthenticationStrategy strategy2 = cache.getOrCreate(app.getId());

    // Now retrieve tokens from both strategies
    GitHubAppAuthStrategy ghStrategy1 = (GitHubAppAuthStrategy) strategy1;
    String token1 = ghStrategy1.getInstallationToken().getToken();

    GitHubAppAuthStrategy ghStrategy2 = (GitHubAppAuthStrategy) strategy2;
    String token2 = ghStrategy2.getInstallationToken().getToken();

    // Verify they are different strategy objects
    assertThat(strategy1).isNotSameAs(strategy2);

    // Verify the actual token strings are different (new token generated)
    assertThat(token1).isNotEqualTo(token2);
  }

  @Test
  public void testTenantSafety_OneTenantCannotAccessAnotherTenantAuthStrategies() {
    // Capture strategies and tokens from both tenants for comparison
    final AuthenticationStrategy[] strategies = new AuthenticationStrategy[2];
    final String[] tokens = new String[2];
    final String[] appIds = new String[2];

    // Tenant 1 creates and caches a strategy
    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      GitHubApp app1 = createTestGitHubApp("owner-tenant1-123");
      appIds[0] = app1.getId();
      stubGitHubTokenEndpointWithUniqueToken();
      AuthenticationStrategy strategy1 = cache.getOrCreate(app1.getId());
      assertThat(strategy1).isNotNull();
      strategies[0] = strategy1;

      // Capture token immediately while within tenant context
      try {
        GitHubAppAuthStrategy ghStrategy1 = (GitHubAppAuthStrategy) strategy1;
        tokens[0] = ghStrategy1.getInstallationToken().getToken();
      }
      catch (Exception e) {
        throw new RuntimeException("Failed to get token for tenant 1", e);
      }
    });

    // Tenant 2 should have an empty cache (isolated from Tenant 1)
    testAsNewTenant(testName, t2 -> {
      GitHubApp app2 = createTestGitHubApp("owner-tenant2-123");
      appIds[1] = app2.getId();
      stubGitHubTokenEndpointWithUniqueToken();
      AuthenticationStrategy strategy2 = cache.getOrCreate(app2.getId());
      assertThat(strategy2).isNotNull();
      strategies[1] = strategy2;

      // Capture token immediately while within tenant context
      try {
        GitHubAppAuthStrategy ghStrategy2 = (GitHubAppAuthStrategy) strategy2;
        tokens[1] = ghStrategy2.getInstallationToken().getToken();
      }
      catch (Exception e) {
        throw new RuntimeException("Failed to get token for tenant 2", e);
      }
    });

    // Verify strategies are different objects (tenant isolation)
    assertThat(strategies[0]).isNotSameAs(strategies[1]);

    // Verify the actual tokens are different
    assertThat(tokens[0]).isNotEqualTo(tokens[1]);

    // Tenant 1's cache should still return the same cached strategy
    testAsTenant(tenant1, t1 -> {
      AuthenticationStrategy sameStrategy = cache.getOrCreate(appIds[0]);
      assertThat(sameStrategy).isSameAs(strategies[0]);
    });
  }

  @Test
  public void testTenantSafety_InvalidateDoesNotAffectOtherTenants() {
    // Capture strategies for comparison
    final AuthenticationStrategy[] tenant1Strategies = new AuthenticationStrategy[2];
    final AuthenticationStrategy[] tenant2Strategies = new AuthenticationStrategy[1];
    final String[] tenant1AppIds = new String[2];
    final String[] tenant2AppIds = new String[1];

    // Tenant 1 creates two strategies
    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      GitHubApp app1 = createTestGitHubApp("tenant1-owner-1");
      GitHubApp app2 = createTestGitHubApp("tenant1-owner-2");
      tenant1AppIds[0] = app1.getId();
      tenant1AppIds[1] = app2.getId();
      stubGitHubTokenEndpointWithUniqueToken();
      tenant1Strategies[0] = cache.getOrCreate(app1.getId());
      stubGitHubTokenEndpointWithUniqueToken();
      tenant1Strategies[1] = cache.getOrCreate(app2.getId());
    });

    // Tenant 2 creates one strategy
    Tenant tenant2 = testAsNewTenant(testName, t2 -> {
      GitHubApp app = createTestGitHubApp("tenant2-owner-1");
      tenant2AppIds[0] = app.getId();
      stubGitHubTokenEndpointWithUniqueToken();
      tenant2Strategies[0] = cache.getOrCreate(app.getId());
    });

    // Tenant 1 invalidates one strategy
    testAsTenant(tenant1, t1 -> {
      cache.invalidate(tenant1AppIds[0]);
      // Second app should still be cached (same object)
      AuthenticationStrategy app2Strategy = cache.getOrCreate(tenant1AppIds[1]);
      assertThat(app2Strategy).isSameAs(tenant1Strategies[1]);
    });

    // Tenant 2's cache should be unaffected (still has cached strategy)
    testAsTenant(tenant2, t2 -> {
      AuthenticationStrategy appStrategy = cache.getOrCreate(tenant2AppIds[0]);
      assertThat(appStrategy).isSameAs(tenant2Strategies[0]);
    });
  }

  private GitHubApp createTestGitHubApp(String ownerId) {
    PasswordHandler passwordHandler = lookup(PasswordHandler.class);
    char[] encryptedKey = passwordHandler.encryptPassword(VALID_BASE64_PKCS8.toCharArray());

    GitHubApp githubApp = new GitHubApp();
    githubApp.setOwnerId(ownerId);
    githubApp.setAppId(appIdCounter++);
    githubApp.setSlug("test-app-" + appIdCounter);
    githubApp.setClientId("Iv1.test-client-id");
    githubApp.setClientSecret("test-client-secret");
    githubApp.setInstallationId((long) installationIdCounter++);
    githubApp.setGithubOrganizationName("test-org");
    githubApp.setLastUpdatedAt(new Date());
    githubApp.setPrivateKey(String.valueOf(encryptedKey));
    githubApp.setActive(true);
    return tempEntity.newGitHubApp(githubApp);
  }
}
