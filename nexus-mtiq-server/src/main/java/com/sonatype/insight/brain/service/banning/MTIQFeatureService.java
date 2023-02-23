/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.api.v2.FeatureAlreadyDisabledException;
import com.sonatype.insight.brain.api.v2.FeatureAlreadyEnabledException;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.Feature;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_APPLICATION_CREATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS;
import static com.sonatype.insight.brain.features.TenantFeature.MULTI_TENANT;
import static com.sonatype.insight.brain.features.TenantFeature.SINGLE_TENANT;
import static com.sonatype.insight.brain.successmetrics.SuccessMetricsService.PROPERTY_ENABLED;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.getTenant;
import static com.sonatype.insight.license.model.LicensedFeature.*;

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
  public static final Set<Feature> ENABLED_FEATURES = ImmutableSet.of(
      DASHBOARD,
      DASHBOARD_CAN_BE_ENABLED,
      HYGIENE,
      FIREWALL,
      BREAKING_CHANGE,
      EXTERNAL_DATABASE,
      FIREWALL_AUTO_UNQUARANTINE,
      ADVANCED_RECOMMENDATION_STRATEGIES,
      NODE_CLUSTERING,
      POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES,
      QUALITY,
      RELEASE_INTEGRITY,
      RM_STAGING_INTEGRATION,
      SAML_USER_TOKENS,
      MULTI_TENANT
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
    Set<Feature> baseFeatures = getBaseFeatures();

    return baseFeatures.stream().filter(this::isEnabled).collect(Collectors.toSet());
  }

  //Visible for testing
  Set<Feature> getBaseFeatures() {
    Set<Feature> features = super.getFeatures();
    features.remove(SINGLE_TENANT);
    features.add(MULTI_TENANT);
    return features;
  }

  public boolean isEnabled(Feature feature) {
    boolean enabled = ENABLED_FEATURES.contains(feature);

    if (!enabled && log.isTraceEnabled()) {
      log.trace("Feature {} is hard disabled for MTIQ. See FeatureService.java for more info.", feature.getId());
    }

    return enabled;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void enableFeature(String feature) {
    if (enabledFeaturesContainsFeatureWithId(feature)) {
      service.enableFeatureNoAuthz(feature);
    }
    else {
      throw new BadRequestException("Feature not supported: " + feature);
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void disableFeature(String feature) {
    if (enabledFeaturesContainsFeatureWithId(feature)) {
      service.disableFeatureNoAuthz(feature);
    }
    else {
      throw new BadRequestException("Feature not supported: " + feature);
    }
  }

  private boolean enabledFeaturesContainsFeatureWithId(String feature) {
    return ENABLED_FEATURES.stream()
        .map(Feature::getId)
        .anyMatch(id -> id.equals(feature));
  }

  @Override
  public void register() {
    for (SystemConfigurationPropertyFeature feature : SystemConfigurationPropertyFeature.values()) {
      toggleFeature(feature);
    }

    setConfigurationBasedFeatures();
  }

  @Override
  public boolean includeGlobalTenantDuringRegistration() {
    return true;
  }

  /**
   * Certain features are "user controlled" and system admins can decide whether the feature is on or off. We've
   * disabled the ability to configure these settings. This method make sure the features themselves are all switched
   * off.
   */
  private void setConfigurationBasedFeatures() {
    log.info("Disabling unsupported user configurable features for tenant {}", getTenant());

    set(PROPERTY_ENABLED, false);
    set(ADVANCED_SEARCH_ENABLED, false);
    set(AUTOMATIC_APPLICATION_CREATION_ENABLED, false);
    set(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED, false);
    set(QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS, false);
  }

  private void set(String key, boolean value) {
    String operation = value ? "Enabling" : "Disabling";

    log.info("{} user configurable feature {} for tenant {}", operation, key, getTenant());

    systemConfigurationPropertyDAO.set(key, Boolean.toString(value));
  }

  private void toggleFeature(SystemConfigurationPropertyFeature feature) {
    try {
      if (ENABLED_FEATURES.contains(feature)) {
        log.info("Enabling feature {} for tenant {}", feature.getPropertyName(), getTenant());

        service.enableFeatureNoAuthz(feature.getPropertyName());
      }
      else {
        log.info("Disabling feature {} for tenant {}", feature.getPropertyName(), getTenant());

        service.disableFeatureNoAuthz(feature.getPropertyName());
      }
    }
    catch (FeatureAlreadyDisabledException e) {
      log.trace("Attempting to disable a feature that is already disabled", e);
    }
    catch (FeatureAlreadyEnabledException e) {
      log.trace("Attempting to enable a feature that is already enabled", e);
    }
  }
}
