/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.InsightConfig;

/**
 * Provides means to inspect the available features of the server.
 * 
 * @since 1.9
 */
public class FeaturesService
{
  private final CLMLicenseManager licenseManager;

  private final boolean showRoot;

  @Inject
  public FeaturesService(CLMLicenseManager licenseManager, InsightConfig config) {
    this.licenseManager = licenseManager;
    this.showRoot = config.isShowRootOrganization();
  }

  /**
   * Gets a list of features supported by this server instance, allowing clients (most notably the UI) to conditionally
   * expose available functionality. If there's currently no valid license installed, the feature set is deemed empty.
   */
  public Set<Feature> getFeatures() {
    Set<Feature> features = EnumSet.noneOf(Feature.class);
    if (licenseManager.isValid()) {
      addVersionSpecificFeatures(features);
      addLicenseSpecificFeatures(features);

      if (showRoot) {
        features.add(Feature.ROOT_ORG);
      }
    }
    return features;
  }

  private void addVersionSpecificFeatures(Set<Feature> features) {
    // Changes to this list should be replicated in brain.client.js
    Collections.addAll(features, Feature.POLICY, Feature.LABELS, Feature.RELEASE_GRAPH, Feature.POLICY_VIOLATIONS,
        Feature.NOTIFICATIONS, Feature.REEVALUATE_POLICY);
  }

  private void addLicenseSpecificFeatures(Set<Feature> features) {
    if (licenseManager.hasPolicyMonitoring()) {
      features.add(Feature.POLICY_MONITORING);
    }

    if (licenseManager.hasDashboard()) {
      features.add(Feature.DASHBOARD);
    }
  }
}
