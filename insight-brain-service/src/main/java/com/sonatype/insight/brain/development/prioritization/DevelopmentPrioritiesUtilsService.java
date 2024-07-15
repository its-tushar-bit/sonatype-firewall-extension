/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

@Named
@Singleton
public class DevelopmentPrioritiesUtilsService
{
  private final FeaturesService featuresService;

  @Inject
  public DevelopmentPrioritiesUtilsService(FeaturesService featuresService) {
    this.featuresService = featuresService;
  }

  public boolean arePrioritiesFeaturesEnabled() {
    Set<Feature> features = featuresService.getFeatures();
    final boolean isPrioritizedFindingsEnabled = features
        .contains(SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT);
    final boolean isDeveloperDashboardEnabled = features
        .contains(LicensedFeature.DEVELOPER_DASHBOARD);

    return isPrioritizedFindingsEnabled && isDeveloperDashboardEnabled;
  }
}
