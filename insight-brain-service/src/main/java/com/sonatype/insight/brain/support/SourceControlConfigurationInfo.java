/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.json.store.JsonUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @since 1.143
 */
@Named
@Singleton
public class SourceControlConfigurationInfo
{
  private final Configuration configuration;

  @Inject
  public SourceControlConfigurationInfo(Configuration configuration) {
    this.configuration = configuration;
  }

  public String getSourceControlConfigurationInfo() {
    SourceControlConfiguration sourceControlConfig = configuration.getSourceControlConfigurationOrDefault();

    final SortedMap<String, Object> entries = new TreeMap<>();
    entries.put("cloneDirectory", sourceControlConfig.getCloneDirectory());
    entries.put("gitImplementation", sourceControlConfig.getGitImplementation());
    entries.put("gitExecutable", sourceControlConfig.getGitExecutable());
    entries.put("prCommentPurgeWindow", sourceControlConfig.getPrCommentPurgeWindow());
    entries.put("prEventPurgeWindow", sourceControlConfig.getPrEventPurgeWindow());
    entries.put("gitTimeoutSeconds", sourceControlConfig.getGitTimeoutSeconds());
    entries.put("commitUsername", sourceControlConfig.getCommitUsername());
    entries.put("commitEmail", sourceControlConfig.getCommitEmail());
    entries.put("useUsernameInRepositoryCloneUrl", sourceControlConfig.isUseUsernameInRepositoryCloneUrl());
    entries.put("defaultBranchMonitoringStartTime", sourceControlConfig.getDefaultBranchMonitoringStartTimeString());
    entries.put("defaultBranchMonitoringIntervalHours", sourceControlConfig.getDefaultBranchMonitoringIntervalHours());
    entries.put("pullRequestMonitoringIntervalSeconds", sourceControlConfig.getPullRequestMonitoringIntervalSeconds());
    entries.put("gpgSigningKey", sourceControlConfig.getGpgSigningKey());
    entries.put("gpgPassphrase", sourceControlConfig.getGpgPassphrase() != null ? "****" : null);

    return JsonUtils.format(entries);
  }
}
