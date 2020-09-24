/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

/**
 * This service configures manifest scans.
 *
 * @since 1.99
 */
public class ScmOnboardingConfigService
{
  private final InsightConfig insightConfig;

  @Inject
  public ScmOnboardingConfigService(final InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  @Authorize(permission = Permission.READ)
  public boolean isScmOnboardingEnabled() {
    return insightConfig.isExperimentalFeatureEnabled(Feature.SCM_ONBOARDING);
  }
}
