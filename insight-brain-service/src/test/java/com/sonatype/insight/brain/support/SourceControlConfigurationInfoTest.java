/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.time.LocalTime;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlConfigurationInfoTest
    extends AbstractComponentTest
{
  @Inject
  private Configuration configuration;

  @Inject
  private SourceControlConfigurationInfo sourceControlConfigurationInfo;

  @Inject
  private SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Test
  public void testGetSourceControlConfigurationInfo() throws Exception {
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();

    sourceControlConfiguration.setCloneDirectory("some-clone-directory");
    sourceControlConfiguration.setGitImplementation(GitImplementation.NATIVE);
    sourceControlConfiguration.setGitExecutable("/usr/bin/git");
    sourceControlConfiguration.setPrCommentPurgeWindow(1);
    sourceControlConfiguration.setPrEventPurgeWindow(2);
    sourceControlConfiguration.setGitTimeoutSeconds(8);
    sourceControlConfiguration.setCommitUsername("some-commit-username");
    sourceControlConfiguration.setCommitEmail("some-commit-email@d");
    sourceControlConfiguration.setUseUsernameInRepositoryCloneUrl(true);
    sourceControlConfiguration.setDefaultBranchMonitoringStartTime(LocalTime.of(2, 22));
    sourceControlConfiguration.setDefaultBranchMonitoringIntervalHours(4);
    sourceControlConfiguration.setPullRequestMonitoringIntervalSeconds(0);
    sourceControlConfiguration.setGpgSigningKey("test-gpg-key");
    sourceControlConfiguration.setGpgPassphrase("encrypted-passphrase");
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    JsonNode configNode = JsonUtils.parse(sourceControlConfigurationInfo.getSourceControlConfigurationInfo());

    assertThat(configNode.get("cloneDirectory").asText()).isEqualTo("some-clone-directory");
    assertThat(configNode.get("gitImplementation").asText()).isEqualTo(GitImplementation.NATIVE.toString());
    assertThat(configNode.get("gitExecutable").asText()).isEqualTo("/usr/bin/git");
    assertThat(configNode.get("prCommentPurgeWindow").asInt()).isEqualTo(1);
    assertThat(configNode.get("prEventPurgeWindow").asInt()).isEqualTo(2);
    assertThat(configNode.get("gitTimeoutSeconds").asInt()).isEqualTo(8);
    assertThat(configNode.get("commitUsername").asText()).isEqualTo("some-commit-username");
    assertThat(configNode.get("commitEmail").asText()).isEqualTo("some-commit-email@d");
    assertThat(configNode.get("useUsernameInRepositoryCloneUrl").asBoolean()).isEqualTo(true);
    assertThat(configNode.get("defaultBranchMonitoringStartTime").asText()).isEqualTo("2:22");
    assertThat(configNode.get("defaultBranchMonitoringIntervalHours").asInt()).isEqualTo(4);
    assertThat(configNode.get("pullRequestMonitoringIntervalSeconds").asInt()).isEqualTo(0);
    assertThat(configNode.get("gpgSigningKey").asText()).isEqualTo("test-gpg-key");
    assertThat(configNode.get("gpgPassphrase").asText()).isEqualTo("****");
  }

  @Test
  public void testGetSourceControlConfigurationInfo_DefaultValuesReturnedWhenConfigDoesNotExist() throws Exception {
    JsonNode configNode = JsonUtils.parse(sourceControlConfigurationInfo.getSourceControlConfigurationInfo());
    SourceControlConfiguration defaultConfig = new SourceControlConfiguration();

    assertThat(configNode.get("cloneDirectory").asText()).isEqualTo(defaultConfig.getCloneDirectory());
    assertThat(configNode.get("defaultBranchMonitoringIntervalHours").asInt())
        .isEqualTo(defaultConfig.getDefaultBranchMonitoringIntervalHours());
    assertThat(configNode.get("pullRequestMonitoringIntervalSeconds").asInt())
        .isEqualTo(defaultConfig.getPullRequestMonitoringIntervalSeconds());
    assertThat(configNode.get("gitImplementation").asText()).isEqualTo("null");
    assertThat(configNode.get("gitExecutable").asText()).isEqualTo("null");
    assertThat(configNode.get("prCommentPurgeWindow").asText()).isEqualTo("null");
    assertThat(configNode.get("prEventPurgeWindow").asText()).isEqualTo("null");
    assertThat(configNode.get("gitTimeoutSeconds").asInt()).isEqualTo(0);
    assertThat(configNode.get("commitUsername").asText()).isEqualTo("null");
    assertThat(configNode.get("commitEmail").asText()).isEqualTo("null");
    assertThat(configNode.get("useUsernameInRepositoryCloneUrl").asText()).isEqualTo("false");
    assertThat(configNode.get("defaultBranchMonitoringStartTime").asText()).isEqualTo("null");
    assertThat(configNode.get("defaultBranchMonitoringIntervalHours").asInt()).isEqualTo(24);
    assertThat(configNode.get("gpgSigningKey").asText()).isEqualTo("null");
    assertThat(configNode.get("gpgPassphrase").asText()).isEqualTo("null");
  }

  @Test
  public void testGetSourceControlConfigurationInfo_WithGpgPassphrase_ReturnsMasked() throws Exception {
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGpgSigningKey("test-key");
    sourceControlConfiguration.setGpgPassphrase("encrypted-value");
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    JsonNode configNode = JsonUtils.parse(sourceControlConfigurationInfo.getSourceControlConfigurationInfo());

    assertThat(configNode.get("gpgSigningKey").asText()).isEqualTo("test-key");
    assertThat(configNode.get("gpgPassphrase").asText()).isEqualTo("****");
  }

  @Test
  public void testGetSourceControlConfigurationInfo_WithoutGpgPassphrase_ReturnsNull() throws Exception {
    SourceControlConfiguration sourceControlConfiguration = tempEntity.newSourceControlConfiguration();
    sourceControlConfiguration.setGpgSigningKey("test-key");
    sourceControlConfiguration.setGpgPassphrase(null);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);
    configuration.sourceControlConfigurationChanged();

    JsonNode configNode = JsonUtils.parse(sourceControlConfigurationInfo.getSourceControlConfigurationInfo());

    assertThat(configNode.get("gpgSigningKey").asText()).isEqualTo("test-key");
    assertThat(configNode.get("gpgPassphrase").asText()).isEqualTo("null");
  }
}
