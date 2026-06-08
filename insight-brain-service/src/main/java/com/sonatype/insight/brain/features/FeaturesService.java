/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import static com.sonatype.insight.brain.features.TenantFeature.SINGLE_TENANT;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides means to inspect the available features of the server.
 *
 * @since 1.9
 */
@Named
@Singleton
public class FeaturesService
{
  private static final Logger log = LoggerFactory.getLogger(FeaturesService.class);

  private final ProductLicense productLicense;

  protected final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final Configuration configuration;

  private final DeveloperEnablementService developerEnablementService;

  @Inject
  public FeaturesService(
      ProductLicense productLicense,
      Configuration configuration,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      DeveloperEnablementService developerEnablementService)
  {
    this.productLicense = productLicense;
    this.configuration = configuration;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.developerEnablementService = developerEnablementService;
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

      if (configuration.isExternalHyperlinksAllowed()) {
        features.add(NonLicensedFeature.ALLOW_EXTERNAL_HYPERLINKS);
      }

      features.addAll(
          Arrays.stream(SystemConfigurationPropertyFeature.values())
              .filter(SystemConfigurationPropertyFeature::isEnabled)
              .collect(Collectors.toSet()));

      if (developerEnablementService.shouldEnableDeveloperProduct()) {
        features.add(LicensedFeature.DEVELOPER_DASHBOARD);
      }

      removeDisabledFeatures(features);

      features.add(SINGLE_TENANT);
    }
    log.debug("Found features: {}", features);
    return features;
  }

  private void addVersionSpecificFeatures(Set<Feature> features) {
    // Changes to this list should be replicated in brain.client.js
    Collections.addAll(features, NonLicensedFeature.POLICY, NonLicensedFeature.LABELS, NonLicensedFeature.RELEASE_GRAPH,
        NonLicensedFeature.REEVALUATE_POLICY);
  }

  private void addLicenseSpecificFeatures(Set<Feature> features) {
    features.addAll(productLicense.getFeatures());
    if (features.contains(LicensedFeature.FIREWALL_FOR_ARTIFACTORY)) {
      features.remove(LicensedFeature.FIREWALL_FOR_ARTIFACTORY);
      features.add(LicensedFeature.FIREWALL);
    }
  }

  protected void removeDisabledFeatures(Set<Feature> features) {
    if (systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.DASHBOARD_DISABLED) != null) {
      features.remove(LicensedFeature.DASHBOARD);
    }
    if (systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.REPORTS_LIST_DISABLED) != null) {
      features.remove(NonLicensedFeature.REPORTS_LIST);
    }
    if (features.contains(SystemConfigurationPropertyFeature.API_PAGE) && features.contains(LicensedFeature.API_PAGE)) {
      // We only need one API_PAGE feature to be returned
      features.remove(SystemConfigurationPropertyFeature.API_PAGE);
    }
    else {
      features.remove(SystemConfigurationPropertyFeature.API_PAGE);
      features.remove(LicensedFeature.API_PAGE);
    }
  }
}
