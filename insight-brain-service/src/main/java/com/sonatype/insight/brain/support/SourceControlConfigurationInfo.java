/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.SortedMap;
import java.util.TreeMap;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * @since 1.143
 */
public class SourceControlConfigurationInfo
{
  private final SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Inject
  SourceControlConfigurationInfo(final SourceControlConfigurationDAO sourceControlConfigurationDAO) {
    this.sourceControlConfigurationDAO = sourceControlConfigurationDAO;
  }

  String getSourceControlConfigurationInfo() {
    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();

    final SortedMap<String, Object> entries = new TreeMap<>();
    entries.put("cloneDirectory",
        sourceControlConfiguration == null ? null : sourceControlConfiguration.getCloneDirectory());
    entries.put("gitImplementation",
        sourceControlConfiguration == null ? null : sourceControlConfiguration.getGitImplementation());
    entries.put("gitExecutable",
        sourceControlConfiguration == null ? null : sourceControlConfiguration.getGitExecutable());
    entries.put("prCommentPurgeWindow",
        sourceControlConfiguration == null ? null : sourceControlConfiguration.getPrCommentPurgeWindow());
    entries.put("prEventPurgeWindow",
        sourceControlConfiguration == null ? null : sourceControlConfiguration.getPrEventPurgeWindow());
    entries.put("gitTimeoutSeconds",
        sourceControlConfiguration == null ? null : sourceControlConfiguration.getGitTimeoutSeconds());
    entries.put("commitUsername",
        sourceControlConfiguration == null ? null : sourceControlConfiguration.getCommitUsername());
    entries.put("commitEmail", sourceControlConfiguration == null ? null : sourceControlConfiguration.getCommitEmail());
    entries.put("useUsernameInRepositoryCloneUrl",
        sourceControlConfiguration == null ? null : sourceControlConfiguration.isUseUsernameInRepositoryCloneUrl());
    entries.put("defaultBranchMonitoringStartTime", sourceControlConfiguration ==
        null ? null : sourceControlConfiguration.getDefaultBranchMonitoringStartTimeString());
    entries.put("defaultBranchMonitoringIntervalHours", sourceControlConfiguration ==
        null ? null : sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours());
    entries.put("pullRequestMonitoringIntervalSeconds", sourceControlConfiguration ==
        null ? null : sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds());

    return JsonUtils.format(entries);
  }
}
