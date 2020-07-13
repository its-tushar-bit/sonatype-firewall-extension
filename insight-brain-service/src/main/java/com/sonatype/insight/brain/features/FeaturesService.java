/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides means to inspect the available features of the server.
 * 
 * @since 1.9
 */
public class FeaturesService
{
  private static final Logger log = LoggerFactory.getLogger(FeaturesService.class);

  private final ProductLicense productLicense;

  private RootOrganizationConfigMigrationUtils rootOrganizationConfigMigrationUtils;

  private final InsightConfig insightConfig;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public FeaturesService(
      ProductLicense productLicense,
      RootOrganizationConfigMigrationUtils rootOrganizationConfigMigrationUtils,
      InsightConfig insightConfig,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
  {
    this.productLicense = productLicense;
    this.rootOrganizationConfigMigrationUtils = rootOrganizationConfigMigrationUtils;
    this.insightConfig = insightConfig;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  /**
   * Gets a list of features supported by this server instance, allowing clients (most notably the UI) to conditionally
   * expose available functionality. If there's currently no valid license installed, the feature set is deemed empty.
   */
  public Set<Feature> getFeatures() {
    Set<Feature> features = new HashSet<>();
    if (productLicense.isValid()) {
      addVersionSpecificFeatures(features);
      addLicenseSpecificFeatures(features);
      features.add(NonLicensedFeature.REPORTS_LIST);

      if (rootOrganizationConfigMigrationUtils.isMigrated()) {
        features.add(NonLicensedFeature.ROOT_ORG);
      }
      else if (!rootOrganizationConfigMigrationUtils.isMigrationScheduled()) {
        features.add(NonLicensedFeature.ROOT_ORG_MIGRATE);
      }

      if (insightConfig.isExternalHyperlinksAllowed()) {
        features.add(NonLicensedFeature.ALLOW_EXTERNAL_HYPERLINKS);
      }

      // expose the scm configuration setting to the front-end
      if (insightConfig.isExperimentalFeatureEnabled("scmConfigValidator")) {
        features.add(NonLicensedFeature.SCM_CONFIG_VALIDATOR);
      }

      removeDisabledFeatures(features);
    }
    log.debug("Found features: {}", features);
    return features;
  }

  private void addVersionSpecificFeatures(Set<Feature> features) {
    // Changes to this list should be replicated in brain.client.js
    Collections.addAll(features, NonLicensedFeature.POLICY, NonLicensedFeature.LABELS, NonLicensedFeature.RELEASE_GRAPH,
        NonLicensedFeature.POLICY_VIOLATIONS, NonLicensedFeature.REEVALUATE_POLICY);
  }

  private void addLicenseSpecificFeatures(Set<Feature> features) {
    features.addAll(productLicense.getFeatures());
    if (features.contains(LicensedFeature.FIREWALL_FOR_ARTIFACTORY)) {
      features.remove(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
      features.add(LicensedFeature.FIREWALL);
    }
  }

  private void removeDisabledFeatures(Set<Feature> features) {
    if (systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.DASHBOARD_DISABLED) != null) {
      features.remove(LicensedFeature.DASHBOARD);
    }
    if (systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.REPORTS_LIST_DISABLED) != null) {
      features.remove(NonLicensedFeature.REPORTS_LIST);
    }
  }
}
