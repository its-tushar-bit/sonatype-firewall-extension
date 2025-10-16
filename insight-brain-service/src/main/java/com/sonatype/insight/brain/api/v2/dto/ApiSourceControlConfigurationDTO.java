/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;

/**
 * @since 1.140
 */
public class ApiSourceControlConfigurationDTO
{
  public String cloneDirectory = SourceControlConfiguration.DEFAULT_SOURCE_CONTROL_CLONE_DIR;

  public GitImplementation gitImplementation;

  public Integer prCommentPurgeWindow;

  public Integer prEventPurgeWindow;

  public String gitExecutable;

  public int gitTimeoutSeconds;

  public String commitUsername;

  public String commitEmail;

  public boolean useUsernameInRepositoryCloneUrl;

  public String defaultBranchMonitoringStartTime;

  public int defaultBranchMonitoringIntervalHours = SourceControlConfiguration.DEFAULT_BRANCH_MONITORING_INTERVAL_HOURS;

  public int pullRequestMonitoringIntervalSeconds =
      SourceControlConfiguration.DEFAULT_PULL_REQUEST_MONITORING_INTERVAL_SECONDS;

  public String gpgSigningKey;

  public String gpgPassphrase;
}
