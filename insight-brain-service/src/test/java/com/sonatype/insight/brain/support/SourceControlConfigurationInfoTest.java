/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.time.LocalTime;

import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SourceControlConfigurationInfoTest
    extends AbstractComponentTest
{
  @Test
  public void testGetSourceControlConfigurationInfo() throws Exception {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();

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
    sourceControlConfiguration.setPullRequestMonitoringIntervalSeconds(280);
    sourceControlConfiguration.setGpgSigningKey("test-gpg-key");
    sourceControlConfiguration.setGpgPassphrase("encrypted-passphrase");

    JsonNode configNode = getSourceControlConfigurationInfo(sourceControlConfiguration);

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
    assertThat(configNode.get("pullRequestMonitoringIntervalSeconds").asInt()).isEqualTo(280);
    assertThat(configNode.get("gpgSigningKey").asText()).isEqualTo("test-gpg-key");
    assertThat(configNode.get("gpgPassphrase").asText()).isEqualTo("****");
  }

  @Test
  public void testGetSourceControlConfigurationInfo_DefaultValuesReturnedWhenConfigDoesNotExist() throws Exception {
    JsonNode configNode = getSourceControlConfigurationInfo(null);
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
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setGpgSigningKey("test-key");
    sourceControlConfiguration.setGpgPassphrase("encrypted-value");

    JsonNode configNode = getSourceControlConfigurationInfo(sourceControlConfiguration);

    assertThat(configNode.get("gpgSigningKey").asText()).isEqualTo("test-key");
    assertThat(configNode.get("gpgPassphrase").asText()).isEqualTo("****");
  }

  @Test
  public void testGetSourceControlConfigurationInfo_WithoutGpgPassphrase_ReturnsNull() throws Exception {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setGpgSigningKey("test-key");
    sourceControlConfiguration.setGpgPassphrase(null);

    JsonNode configNode = getSourceControlConfigurationInfo(sourceControlConfiguration);

    assertThat(configNode.get("gpgSigningKey").asText()).isEqualTo("test-key");
    assertThat(configNode.get("gpgPassphrase").asText()).isEqualTo("null");
  }

  private JsonNode getSourceControlConfigurationInfo(
      SourceControlConfiguration sourceControlConfiguration) throws Exception
  {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getSourceControlConfigurationOrDefault()).thenReturn(
        sourceControlConfiguration == null ? new SourceControlConfiguration() : sourceControlConfiguration);

    return JsonUtils.parse(new SourceControlConfigurationInfo(configuration).getSourceControlConfigurationInfo());
  }
}
