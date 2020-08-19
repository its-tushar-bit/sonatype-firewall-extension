/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;

import com.sonatype.insight.brain.git.event.SourceControlEventService;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

/**
 * This service creates and publishes a <i>SourceControlEvent</i> of type MANIFEST_SCAN_EVENT
 *
 * @since 1.98
 */
public class ApiManifestScanService
{
  private final SourceControlEventService sourceControlEventService;

  private final InsightConfig insightConfig;

  @Inject
  public ApiManifestScanService(
      final SourceControlEventService sourceControlEventService,
      final InsightConfig insightConfig)
  {
    this.sourceControlEventService = sourceControlEventService;
    this.insightConfig = insightConfig;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public void performManifestScan(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final String stage,
      final String branchName)
  {
    if (!insightConfig.isExperimentalFeatureEnabled(Feature.MANIFEST_SCAN)) {
      return;
    }
    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .setApplicationId(applicationId)
        .setEventType(SourceControlEvent.MANIFEST_SCAN_EVENT)
        .setStageTypeId(stage)
        .setBranchName(branchName);

    sourceControlEventService.publishEvent(sourceControlEvent);
  }
}
