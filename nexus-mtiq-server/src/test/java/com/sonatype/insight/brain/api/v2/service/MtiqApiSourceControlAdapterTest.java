/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiSourceControlDTO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class MtiqApiSourceControlAdapterTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Inject
  private ApiSourceControlAdapter apiSourceControlAdapter;

  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  public void setup() {
    apiSourceControlAdapter = lookup(ApiSourceControlAdapter.class);
  }

  @Test
  public void convertFromDTO_whenDefaultState_prsEnabledByDefault() {
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
  public void convertFromDTO_whenFeatureFlagExplicitlyDisabled() {
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
  public void convertFromDTO_whenEnvironmentVariableEnablesPRs() {
    // Set environment variable to enable PRs even though database flag is disabled
    environmentVariables.set(SystemConfigurationPropertyFeature.NXIQ_SAAS_LIFECYCLE_SCM_PRS_ENABLED_ENV_VAR, "true");

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
  public void convertFromDTO_whenEnvironmentVariableDisablesPRs() {
    // Enable database feature flag first
    SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_PRS_ENABLED.setEnabled(true);
    // Then set environment variable to disable PRs (should override database setting)
    environmentVariables.set(SystemConfigurationPropertyFeature.NXIQ_SAAS_LIFECYCLE_SCM_PRS_ENABLED_ENV_VAR, "false");

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
