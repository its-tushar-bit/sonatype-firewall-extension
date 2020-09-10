/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;

import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

/**
 * This service creates and publishes a <i>SourceControlEvent</i> of type MANIFEST_SCAN_EVENT
 *
 * @since 1.98
 */
public class ApiManifestScanService
{
  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final InsightConfig insightConfig;

  private final SourceControlUtils sourceControlUtils;

  @Inject
  public ApiManifestScanService(
      final SourceControlEventPublisher sourceControlEventPublisher,
      final InsightConfig insightConfig,
      final SourceControlUtils sourceControlUtils)
  {
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.insightConfig = insightConfig;
    this.sourceControlUtils = sourceControlUtils;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public String performManifestScan(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final String stage,
      final String branchName,
      final String userAgent) throws IOException
  {
    if (!insightConfig.isExperimentalFeatureEnabled(Feature.MANIFEST_SCAN)) {
      return null;
    }
    final GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(applicationId);

    if (gitRepositoryInfo == null) {
      throw new IOException("No SCM configuration defined for this application");
    }

    String statusId = UUID.randomUUID().toString().replace("-", "");

    final String branch = (branchName != null)
        ? branchName
        : gitRepositoryInfo.getBaseBranch();

    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .setApplicationId(applicationId)
        .setEventType(SourceControlEvent.MANIFEST_SCAN_EVENT)
        .setStageTypeId(stage)
        .setStatusId(statusId)
        .setBranchName(branch)
        .setUserAgent(userAgent);

    sourceControlEventPublisher.publishEvent(sourceControlEvent);

    return statusId;
  }
}
