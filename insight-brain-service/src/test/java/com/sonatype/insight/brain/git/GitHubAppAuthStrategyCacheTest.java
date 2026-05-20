/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.Date;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.nexus.scm.api.auth.AuthenticationStrategy;
import com.sonatype.nexus.scm.github.auth.GitHubAppAuthStrategy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Binder;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GitHubAppAuthStrategyCacheTest
    extends AbstractComponentTest
{
  private static final int WIREMOCK_PORT = 18089;

  @Rule
  public WireMockRule githubMockServer = new WireMockRule(wireMockConfig().port(WIREMOCK_PORT));

  @Inject
  private GitHubAppAuthStrategyCache cache;

  private static final String TEST_OWNER_ID = "test-owner-123";

  private static int appIdCounter = 10000;

  private static int installationIdCounter = 10000;

  private static int tokenCounter = 10000;

  /**
   * Override GitHubAppAuthStrategyCache bean to use WireMock URL for tests.
   * Using toProvider instead of toInstance because dependencies need to be injected first.
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

  @After
  @Override
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
    createTestGitHubApp(TEST_OWNER_ID);

    AuthenticationStrategy strategy = cache.getOrCreate(TEST_OWNER_ID);

    assertThat(strategy).isNotNull();
  }

  @Test
  public void testGetOrCreate_UsesCacheOnSecondCall() {
    stubGitHubTokenEndpointWithUniqueToken();
    createTestGitHubApp(TEST_OWNER_ID);

    AuthenticationStrategy strategy1 = cache.getOrCreate(TEST_OWNER_ID);
    AuthenticationStrategy strategy2 = cache.getOrCreate(TEST_OWNER_ID);

    assertThat(strategy1).isSameAs(strategy2);
  }

  @Test
  public void testGetOrCreate_DifferentOwnerIdsLoadSeparately() throws Exception {
    String ownerId1 = "owner-1";
    String ownerId2 = "owner-2";

    GitHubApp app1 = createTestGitHubApp(ownerId1);
    GitHubApp app2 = createTestGitHubApp(ownerId2);

    // Stub tokens for each installation ID
    stubGitHubTokenEndpointForInstallation(app1.getInstallationId());
    stubGitHubTokenEndpointForInstallation(app2.getInstallationId());

    // Create both strategies (this loads tokens and caches them)
    AuthenticationStrategy strategy1 = cache.getOrCreate(ownerId1);
    AuthenticationStrategy strategy2 = cache.getOrCreate(ownerId2);

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
    String nonExistentOwnerId = "non-existent-owner-id";

    // Guava's cache wraps exceptions in UncheckedExecutionException
    assertThatThrownBy(() -> cache.getOrCreate(nonExistentOwnerId))
        .isInstanceOf(UncheckedExecutionException.class)
        .hasCauseInstanceOf(NotFoundException.class)
        .hasMessageContaining("GitHub App not found for ownerId: " + nonExistentOwnerId);
  }

  @Test
  public void testInvalidateByGitHubAppId_EvictsOnlyMatchingEntries() {
    String ownerA = "owner-by-app-id-A";
    String ownerB = "owner-by-app-id-B";
    String ownerC = "owner-by-app-id-C";

    GitHubApp appA = createTestGitHubApp(ownerA);
    GitHubApp appB = createTestGitHubApp(ownerB);
    GitHubApp appC = createTestGitHubApp(ownerC);

    stubGitHubTokenEndpointForInstallation(appA.getInstallationId());
    stubGitHubTokenEndpointForInstallation(appB.getInstallationId());
    stubGitHubTokenEndpointForInstallation(appC.getInstallationId());

    AuthenticationStrategy strategyA = cache.getOrCreate(ownerA);
    AuthenticationStrategy strategyB = cache.getOrCreate(ownerB);
    AuthenticationStrategy strategyC = cache.getOrCreate(ownerC);

    cache.invalidateByGitHubAppId(appA.getAppId());

    // owner-A's entry was sourced from appA → evicted; the next getOrCreate must rebuild a fresh strategy.
    AuthenticationStrategy strategyAReloaded = cache.getOrCreate(ownerA);
    assertThat(strategyAReloaded).isNotSameAs(strategyA);

    // owner-B and owner-C's entries were sourced from appB / appC → untouched.
    assertThat(cache.getOrCreate(ownerB)).isSameAs(strategyB);
    assertThat(cache.getOrCreate(ownerC)).isSameAs(strategyC);
  }

  @Test
  public void testInvalidateByGitHubAppId_NullIsNoOp() {
    String ownerA = "owner-noop-A";
    GitHubApp appA = createTestGitHubApp(ownerA);
    stubGitHubTokenEndpointForInstallation(appA.getInstallationId());

    AuthenticationStrategy before = cache.getOrCreate(ownerA);
    cache.invalidateByGitHubAppId(null);

    assertThat(cache.getOrCreate(ownerA)).isSameAs(before);
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
            .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
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
    AuthenticationStrategy strategy1 = cache.getOrCreate(TEST_OWNER_ID);
    cache.invalidate(TEST_OWNER_ID);
    AuthenticationStrategy strategy2 = cache.getOrCreate(TEST_OWNER_ID);

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
    // Use unique owner IDs for each tenant (owner_id has unique constraint)
    String ownerId1 = "owner-tenant1-123";
    String ownerId2 = "owner-tenant2-123";

    // Capture strategies and tokens from both tenants for comparison
    final AuthenticationStrategy[] strategies = new AuthenticationStrategy[2];
    final String[] tokens = new String[2];

    // Tenant 1 creates and caches a strategy
    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      createTestGitHubApp(ownerId1);
      stubGitHubTokenEndpointWithUniqueToken();
      AuthenticationStrategy strategy1 = cache.getOrCreate(ownerId1);
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
      createTestGitHubApp(ownerId2);
      stubGitHubTokenEndpointWithUniqueToken();
      // Tenant 2 creates its own strategy for a different ownerId
      AuthenticationStrategy strategy2 = cache.getOrCreate(ownerId2);
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
      AuthenticationStrategy sameStrategy = cache.getOrCreate(ownerId1);
      assertThat(sameStrategy).isSameAs(strategies[0]);
    });
  }

  @Test
  public void testTenantSafety_InvalidateDoesNotAffectOtherTenants() {
    // Use unique owner IDs (owner_id has unique constraint)
    String tenant1Owner1 = "tenant1-owner-1";
    String tenant1Owner2 = "tenant1-owner-2";
    String tenant2Owner1 = "tenant2-owner-1";

    // Capture strategies for comparison
    final AuthenticationStrategy[] tenant1Strategies = new AuthenticationStrategy[2];
    final AuthenticationStrategy[] tenant2Strategies = new AuthenticationStrategy[1];

    // Tenant 1 creates two strategies
    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      createTestGitHubApp(tenant1Owner1);
      createTestGitHubApp(tenant1Owner2);
      stubGitHubTokenEndpointWithUniqueToken();
      tenant1Strategies[0] = cache.getOrCreate(tenant1Owner1);
      stubGitHubTokenEndpointWithUniqueToken();
      tenant1Strategies[1] = cache.getOrCreate(tenant1Owner2);
    });

    // Tenant 2 creates one strategy
    Tenant tenant2 = testAsNewTenant(testName, t2 -> {
      createTestGitHubApp(tenant2Owner1);
      stubGitHubTokenEndpointWithUniqueToken();
      tenant2Strategies[0] = cache.getOrCreate(tenant2Owner1);
    });

    // Tenant 1 invalidates one strategy
    testAsTenant(tenant1, t1 -> {
      cache.invalidate(tenant1Owner1);
      // tenant1Owner2 should still be cached (same object)
      AuthenticationStrategy owner2Strategy = cache.getOrCreate(tenant1Owner2);
      assertThat(owner2Strategy).isSameAs(tenant1Strategies[1]);
    });

    // Tenant 2's cache should be unaffected (still has cached strategy)
    testAsTenant(tenant2, t2 -> {
      AuthenticationStrategy owner1Strategy = cache.getOrCreate(tenant2Owner1);
      assertThat(owner1Strategy).isSameAs(tenant2Strategies[0]);
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
