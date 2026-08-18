/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.nexus.scm.GitApiClientFactory;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Shared reused-context cohort configuration for the GitHub App / git-API tests that override
 * {@link GitHubAppAuthStrategyCache} to point at a mock GitHub endpoint.
 *
 * <p>
 * Both {@code GitHubAppAuthStrategyCacheTest} and {@code ManualPullRequestCreationServiceTest} reference this SAME
 * top-level config via {@code @ContextConfiguration}, so Spring's TestContext cache key is identical for the two
 * classes and they share ONE cached ApplicationContext instead of each building its own. That is the reuse win over
 * the previous per-class {@code @TestConfiguration} setup (which forced a distinct context per class).
 *
 * <p>
 * The mock endpoint is a single {@link WireMockServer} bean bound to a <b>dynamic</b> port (so parallel Surefire /
 * Failsafe forks never collide on a fixed port) and started once with the cohort context. {@link
 * GitHubAppAuthStrategyCache} is wired to that server's base URL at context-build time. Tests inject the server to add
 * their own stubs and must call {@code resetAll()} between methods to avoid cross-test stub leakage on the shared
 * server.
 */
@TestConfiguration
public class GitHubAppWireMockCohortConfig
{
  @Bean(destroyMethod = "stop")
  public WireMockServer gitHubAppWireMockServer() {
    WireMockServer server = new WireMockServer(wireMockConfig().dynamicPort());
    server.start();
    return server;
  }

  @Bean
  @Primary
  public GitHubAppAuthStrategyCache gitHubAppAuthStrategyCache(
      final GitHubAppDAO githubAppDAO,
      final InsightProxy insightProxy,
      final GitApiClientFactory gitApiClientFactory,
      final PasswordHandler passwordHandler,
      final WireMockServer gitHubAppWireMockServer)
  {
    return new GitHubAppAuthStrategyCache(
        githubAppDAO,
        insightProxy,
        gitApiClientFactory,
        passwordHandler,
        gitHubAppWireMockServer.baseUrl());
  }
}
