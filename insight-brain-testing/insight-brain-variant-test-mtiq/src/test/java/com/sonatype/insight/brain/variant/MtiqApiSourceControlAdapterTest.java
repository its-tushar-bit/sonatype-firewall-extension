/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MTIQ variant conversion of {@code MtiqApiSourceControlAdapterTest} (which extended
 * {@code AbstractMultiTenantBaseIntegrationTest}). Pure adapter-logic test: no tenant/REST interaction is
 * exercised, so only the injected {@link MtiqTestContext} bean lookup is used. Environment-variable
 * manipulation (originally via the JUnit 4 {@code EnvironmentVariables} rule) is reproduced with a small
 * self-contained reflective mutator on the process environment map, restored in {@link #tearDown()}.
 */
@MtiqTest
class MtiqApiSourceControlAdapterTest
{
  // Injected by MtiqServerExtension: the reused multi-tenant server + a fresh per-test tenant context.
  private MtiqTestContext ctx;

  private ApiSourceControlAdapter apiSourceControlAdapter;

  private final Map<String, String> envOverridesApplied = new HashMap<>();

  @BeforeEach
  void setup() {
    apiSourceControlAdapter = ctx.lookup(ApiSourceControlAdapter.class);
  }

  @AfterEach
  void tearDown() {
    for (String key : envOverridesApplied.keySet()) {
      removeEnvironmentVariable(key);
    }
    envOverridesApplied.clear();
    // Restore the shared feature flag to its default-enabled state. Tests here run in one reused server
    // process (reuseForks=true, forkCount=1) and some call setEnabled(...) directly, which is a DB write
    // MtiqTestContext.afterTest() does not roll back; reset it so tests stay order-independent.
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED.setEnabled(true);
  }

  /**
   * Sets a process environment variable for the duration of the test (self-contained replacement for the
   * legacy JUnit 4 {@code org.junit.contrib.java.lang.system.EnvironmentVariables} rule, which is not
   * available under JUnit 5). Restored automatically in {@link #tearDown()}.
   */
  @SuppressWarnings("unchecked")
  private void setEnvironmentVariable(final String name, final String value) {
    try {
      // Known technical debt: mutates the JVM-internal Collections$UnmodifiableMap.m backing field of the
      // process environment. This is not public API and may require --add-opens on future JDKs; replace with
      // a JUnit 5 env helper (e.g. system-stubs-jupiter) when one is adopted. Works today under Java 25.
      Map<String, String> env = System.getenv();
      Class<?> unmodifiableMapClass = Class.forName("java.util.Collections$UnmodifiableMap");
      Field field = unmodifiableMapClass.getDeclaredField("m");
      field.setAccessible(true);
      Map<String, String> writableEnv = (Map<String, String>) field.get(env);
      writableEnv.put(name, value);
      envOverridesApplied.put(name, value);
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to set environment variable " + name + " for test", e);
    }
  }

  @SuppressWarnings("unchecked")
  private void removeEnvironmentVariable(final String name) {
    try {
      Map<String, String> env = System.getenv();
      Class<?> unmodifiableMapClass = Class.forName("java.util.Collections$UnmodifiableMap");
      Field field = unmodifiableMapClass.getDeclaredField("m");
      field.setAccessible(true);
      Map<String, String> writableEnv = (Map<String, String>) field.get(env);
      writableEnv.remove(name);
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to remove environment variable " + name + " for test", e);
    }
  }

  @Test
  void convertFromDTO_whenDefaultState_prsEnabledByDefault() {
    // With no env var and no database override, PRs are enabled by default (enabledWhenAbsent = true)
    assertThat(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED.isEnabled()).isTrue();

    ApiSourceControlDTO apiSourceControlDTO = new ApiSourceControlDTO();
    apiSourceControlDTO.id = "id";
    apiSourceControlDTO.ownerId = "ownerId";
    apiSourceControlDTO.repositoryUrl = "repo_url";
    apiSourceControlDTO.username = "username";
    apiSourceControlDTO.token = "TOKEN";
    apiSourceControlDTO.provider = "github";
    apiSourceControlDTO.baseBranch = "master";
    apiSourceControlDTO.remediationPullRequestsEnabled = true;
    apiSourceControlDTO.statusChecksEnabled = false;
    apiSourceControlDTO.pullRequestCommentingEnabled = true;
    apiSourceControlDTO.sourceControlEvaluationsEnabled = true;
    apiSourceControlDTO.sourceControlScanTarget = "/target/*";
    apiSourceControlDTO.sshEnabled = true;
    apiSourceControlDTO.commitStatusEnabled = false;
    apiSourceControlDTO.manualPullRequestsEnabled = true;
    apiSourceControlDTO.innerSourceAutomatedUpdatesEnabled = true;
    apiSourceControlDTO.nonGoldenPullRequestsEnabled = true;

    SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(apiSourceControlDTO);

    assertThat(sourceControl.getId()).isNull();
    assertThat(sourceControl.getOwnerId()).isEqualTo("ownerId");
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo("repo_url");
    assertThat(sourceControl.getUsername()).isEqualTo("username");
    assertThat(sourceControl.getToken()).isEqualTo("TOKEN");
    assertThat(sourceControl.getProvider()).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(sourceControl.getBaseBranch()).isEqualTo("master");
    // When default (no override), PRs are enabled in MTIQ
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isEqualTo(true);
    assertThat(sourceControl.getManualPullRequestsEnabled()).isEqualTo(true);
    assertThat(sourceControl.getInnerSourceAutomatedUpdatesEnabled()).isEqualTo(true);
    assertThat(sourceControl.getNonGoldenPullRequestsEnabled()).isEqualTo(true);
    assertThat(sourceControl.getStatusChecksEnabled()).isEqualTo(false);
    assertThat(sourceControl.getPullRequestCommentingEnabled()).isEqualTo(true);
    assertThat(sourceControl.getSourceControlEvaluationsEnabled()).isEqualTo(true);
    assertThat(sourceControl.getSourceControlScanTarget()).isEqualTo("/target/*");
    assertThat(sourceControl.getSshEnabled()).isTrue();
    assertThat(sourceControl.getCommitStatusEnabled()).isFalse();
  }

  @Test
  void convertFromDTO_whenFeatureFlagExplicitlyDisabled() {
    // Explicitly disable the feature flag to simulate tenant opt-out
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED.setEnabled(false);
    try {
      assertThat(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED.isEnabled()).isFalse();

      ApiSourceControlDTO apiSourceControlDTO = new ApiSourceControlDTO();
      apiSourceControlDTO.id = "id";
      apiSourceControlDTO.ownerId = "ownerId";
      apiSourceControlDTO.repositoryUrl = "repo_url";
      apiSourceControlDTO.username = "username";
      apiSourceControlDTO.token = "TOKEN";
      apiSourceControlDTO.provider = "github";
      apiSourceControlDTO.baseBranch = "master";
      apiSourceControlDTO.remediationPullRequestsEnabled = true;
      apiSourceControlDTO.statusChecksEnabled = false;
      apiSourceControlDTO.pullRequestCommentingEnabled = true;
      apiSourceControlDTO.sourceControlEvaluationsEnabled = true;
      apiSourceControlDTO.sourceControlScanTarget = "/target/*";
      apiSourceControlDTO.sshEnabled = true;
      apiSourceControlDTO.commitStatusEnabled = false;
      apiSourceControlDTO.manualPullRequestsEnabled = true;
      apiSourceControlDTO.innerSourceAutomatedUpdatesEnabled = true;
      apiSourceControlDTO.nonGoldenPullRequestsEnabled = true;

      SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(apiSourceControlDTO);

      assertThat(sourceControl.getId()).isNull();
      assertThat(sourceControl.getOwnerId()).isEqualTo("ownerId");
      assertThat(sourceControl.getRepositoryUrl()).isEqualTo("repo_url");
      assertThat(sourceControl.getUsername()).isEqualTo("username");
      assertThat(sourceControl.getToken()).isEqualTo("TOKEN");
      assertThat(sourceControl.getProvider()).isEqualTo(SourceControlProvider.GITHUB);
      assertThat(sourceControl.getBaseBranch()).isEqualTo("master");
      // When explicitly disabled, PRs should be DISABLED in MTIQ
      assertThat(sourceControl.getRemediationPullRequestsEnabled()).isEqualTo(false);
      assertThat(sourceControl.getManualPullRequestsEnabled()).isEqualTo(false);
      assertThat(sourceControl.getInnerSourceAutomatedUpdatesEnabled()).isEqualTo(false);
      assertThat(sourceControl.getNonGoldenPullRequestsEnabled()).isEqualTo(false);
      assertThat(sourceControl.getStatusChecksEnabled()).isEqualTo(false);
      assertThat(sourceControl.getPullRequestCommentingEnabled()).isEqualTo(true);
      assertThat(sourceControl.getSourceControlEvaluationsEnabled()).isEqualTo(true);
      assertThat(sourceControl.getSourceControlScanTarget()).isEqualTo("/target/*");
      assertThat(sourceControl.getSshEnabled()).isTrue();
      assertThat(sourceControl.getCommitStatusEnabled()).isFalse();
    }
    finally {
      // Reset the feature flag to its default state (enabled)
      SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED.setEnabled(true);
    }
  }

  @Test
  void convertFromDTO_whenEnvironmentVariableEnablesPRs() {
    // Set environment variable to enable PRs even though database flag is disabled
    setEnvironmentVariable(SystemConfigurationPropertyFeature.NXIQ_SAAS_LIFECYCLE_SCM_PRS_ENABLED_ENV_VAR, "true");

    // Database flag remains disabled (default state)
    assertThat(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED.isEnabled()).isTrue();

    ApiSourceControlDTO apiSourceControlDTO = new ApiSourceControlDTO();
    apiSourceControlDTO.id = "id";
    apiSourceControlDTO.ownerId = "ownerId";
    apiSourceControlDTO.repositoryUrl = "repo_url";
    apiSourceControlDTO.username = "username";
    apiSourceControlDTO.token = "TOKEN";
    apiSourceControlDTO.provider = "github";
    apiSourceControlDTO.baseBranch = "master";
    apiSourceControlDTO.remediationPullRequestsEnabled = true;
    apiSourceControlDTO.manualPullRequestsEnabled = true;
    apiSourceControlDTO.innerSourceAutomatedUpdatesEnabled = true;
    apiSourceControlDTO.nonGoldenPullRequestsEnabled = true;

    SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(apiSourceControlDTO);

    // Environment variable should override database setting and enable PR creation
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isEqualTo(true);
    assertThat(sourceControl.getManualPullRequestsEnabled()).isEqualTo(true);
    assertThat(sourceControl.getInnerSourceAutomatedUpdatesEnabled()).isEqualTo(true);
    assertThat(sourceControl.getNonGoldenPullRequestsEnabled()).isEqualTo(true);
  }

  @Test
  void convertFromDTO_whenEnvironmentVariableDisablesPRs() {
    // Enable database feature flag first
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED.setEnabled(true);
    // Then set environment variable to disable PRs (should override database setting)
    setEnvironmentVariable(SystemConfigurationPropertyFeature.NXIQ_SAAS_LIFECYCLE_SCM_PRS_ENABLED_ENV_VAR, "false");

    // Environment variable should override database setting
    assertThat(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED.isEnabled()).isFalse();

    ApiSourceControlDTO apiSourceControlDTO = new ApiSourceControlDTO();
    apiSourceControlDTO.id = "id";
    apiSourceControlDTO.ownerId = "ownerId";
    apiSourceControlDTO.repositoryUrl = "repo_url";
    apiSourceControlDTO.username = "username";
    apiSourceControlDTO.token = "TOKEN";
    apiSourceControlDTO.provider = "github";
    apiSourceControlDTO.baseBranch = "master";
    apiSourceControlDTO.remediationPullRequestsEnabled = true;
    apiSourceControlDTO.manualPullRequestsEnabled = true;
    apiSourceControlDTO.innerSourceAutomatedUpdatesEnabled = true;
    apiSourceControlDTO.nonGoldenPullRequestsEnabled = true;

    SourceControl sourceControl = apiSourceControlAdapter.convertFromDTO(apiSourceControlDTO);

    // Environment variable should override database setting and disable PR creation
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isEqualTo(false);
    assertThat(sourceControl.getManualPullRequestsEnabled()).isEqualTo(false);
    assertThat(sourceControl.getInnerSourceAutomatedUpdatesEnabled()).isEqualTo(false);
    assertThat(sourceControl.getNonGoldenPullRequestsEnabled()).isEqualTo(false);
  }
}
