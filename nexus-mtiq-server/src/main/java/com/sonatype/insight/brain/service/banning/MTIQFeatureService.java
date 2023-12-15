/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.FeatureAlreadyDisabledException;
import com.sonatype.insight.brain.api.v2.FeatureAlreadyEnabledException;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature.*;
import static com.sonatype.insight.brain.features.TenantFeature.MULTI_TENANT;
import static com.sonatype.insight.brain.features.TenantFeature.SINGLE_TENANT;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS;
import static com.sonatype.insight.brain.successmetrics.SuccessMetricsService.PROPERTY_ENABLED;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.getTenant;

/**
 * Configures which features are available to an MTIQ deployment.
 */
@Named
@Singleton
public class MTIQFeatureService
    extends FeaturesService
    implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(MTIQFeatureService.class);

  /**
   * This is the list of features that are _allowed_ to be enabled in MTIQ but doesn't necessarily mean all these things
   * _will_ be enabled, that depends on the license itself. Another way to think about it is these are an additional
   * filter that is applied on top of the license. We decided to go with a list of "enabled" rather than "disabled"
   * features so that any new features don't automatically get released in MTIQ.
   */

  /**
   * This is the list of features that are always enabled in MTIQ.
   */
  private static final List<SystemConfigurationPropertyFeature> MTIQ_ENABLED_FEATURES = Arrays.asList(
      ENABLE_SSO_ONLY,
      AUTOMATIC_APPLICATION_CONFIGURATION,
      INNER_SOURCE_REPOSITORY_INTEGRATION,
      INNER_SOURCE_TRANSITIVE_WAIVER,
      LOGOUT_AUTH0_ON_LOGOUT);

  /**
   * This is the list of features that are never enabled in MTIQ.
   */
  private static final List<Feature> MTIQ_BANNED_FEATURES = Arrays.asList(
      LicensedFeature.DATA_INSIGHTS,
      SystemConfigurationPropertyFeature.API_PAGE,
      SUCCESS_METRICS_CONFIGURATION,
      PRODUCT_LICENSE_CONFIGURATION,
      SYSTEM_NOTICE_CONFIGURATION,
      ENABLE_UNAUTHENTICATED_PAGES,
      PROXY_CONFIGURATION,
      DEPENDENCY_DATA_IN_API,
      CROWD_INTEGRATION,
      COMPONENT_SEARCH_API_WITH_INNERSOURCE,
      CODE_INSIGHTS,
      LDAP_CONFIGURATION,
      SCAN_NPM_DEV_AND_OPT_DEPENDENCIES,
      TRANSITIVE_SOLVER,
      SCAN_POM_FILES_IN_META_INF_DIRECTORY,
      VULNERABILITY_SOURCE,
      BUILT_FROM_SOURCE,
      INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS,
      ADVANCED_SEARCH_CONFIGURATION,
      LicensedFeature.ADVANCED_LEGAL_PACK,
      ORG_APP_MANAGEMENT_WEBHOOK_EVENT
  );

  private final ApiConfigFeaturesService service;

  @Inject
  public MTIQFeatureService(
      ProductLicense productLicense,
      Configuration configuration,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      ApiConfigFeaturesService service)
  {
    super(productLicense, configuration, systemConfigurationPropertyDAO);

    this.service = service;
  }

  @Override
  public Set<Feature> getFeatures() {
    Set<Feature> features = getBaseFeatures();

    features.remove(SINGLE_TENANT);
    features.add(MULTI_TENANT);

    MTIQ_BANNED_FEATURES.forEach(feature -> {
      features.remove(feature);
    });

    return features;
  }

  Set<Feature> getBaseFeatures() {
    return super.getFeatures();
  }

  public void enableFeature(String feature) {
    if (!isBannedMTIQFeatureWithId(feature)) {
      service.enableFeatureNoAuthz(feature);
    }
    else {
      throw new BadRequestException("Feature not supported: " + feature);
    }
  }

  private boolean isBannedMTIQFeatureWithId(String feature) {
    return MTIQ_BANNED_FEATURES.stream()
        .map(Feature::getId)
        .anyMatch(id -> id.equals(feature));
  }

  public void disableFeature(String feature) {
    if (!isMTIQFeatureWithId(feature)) {
      service.disableFeatureNoAuthz(feature);
    }
    else {
      throw new BadRequestException("Feature not supported: " + feature);
    }
  }

  private boolean isMTIQFeatureWithId(String feature) {
    return MTIQ_ENABLED_FEATURES.stream()
        .map(Feature::getId)
        .anyMatch(id -> id.equals(feature));
  }

  @Override
  public boolean includeGlobalTenantDuringRegistration() {
    return true;
  }

  @Override
  public void register() {
    for (SystemConfigurationPropertyFeature feature : SystemConfigurationPropertyFeature.values()) {
      toggleFeature(feature);
    }

    setConfigurationBasedFeatures();
  }

  private void toggleFeature(SystemConfigurationPropertyFeature feature) {
    try {
      if (isEnabled(feature)) {
        log.info("Enabling feature {} for tenant {}", feature.getPropertyName(), getTenant());

        service.enableFeatureNoAuthz(feature.getPropertyName());
      }
      else {
        log.info("Disabling feature {} for tenant {}", feature.getPropertyName(), getTenant());

        service.disableFeatureNoAuthz(feature.getPropertyName());
      }
    }
    catch (FeatureAlreadyDisabledException e) {
      log.trace("Attempting to disable a feature that is already disabled");
    }
    catch (FeatureAlreadyEnabledException e) {
      log.trace("Attempting to enable a feature that is already enabled");
    }
  }

  public boolean isEnabled(SystemConfigurationPropertyFeature feature) {
    if (isBanned(feature)) {
      return false;
    }
    else if (MTIQ_ENABLED_FEATURES.contains(feature)) {
      return true;
    }

    return feature.isEnabled();
  }

  public boolean isBanned(SystemConfigurationPropertyFeature feature) {
    if (MTIQ_BANNED_FEATURES.contains(feature)) {
      log.trace("Feature {} is hard disabled for MTIQ. See MTIQFeatureService.java for more info.", feature.getId());

      return true;
    }
    return false;
  }

  /**
   * setConfigurationBasedFeatures used for setting SystemConfigurationProperty based features.
   * Certain features are "user controlled" and system admins can decide whether the feature is on or off. We've
   * disabled the ability to configure these settings. This method make sure the features themselves are all switched
   * on or off for MTIQ.
   */
  private void setConfigurationBasedFeatures() {
    log.info("Enabling/Disabling user configurable features for tenant {}", getTenant());

    set(PROPERTY_ENABLED, false);
    set(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED, true);
    set(QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS, false);
  }

  private void set(String key, boolean value) {
    String operation = value ? "Enabling" : "Disabling";
    log.info("{} user configurable feature {} for tenant {}", operation, key, getTenant());
    systemConfigurationPropertyDAO.set(key, Boolean.toString(value));
  }
}
