/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
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
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature.*;
import static com.sonatype.insight.brain.features.NonLicensedFeature.REPORTS_LIST;
import static com.sonatype.insight.brain.features.TenantFeature.MULTI_TENANT;
import static com.sonatype.insight.brain.features.TenantFeature.SINGLE_TENANT;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS;
import static com.sonatype.insight.brain.successmetrics.SuccessMetricsService.PROPERTY_ENABLED;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.getTenant;
import static java.util.stream.Collectors.toSet;

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
  public static final Set<Feature> ENABLED_FEATURES = Stream.concat(ImmutableSet.of(
              DASHBOARD_CAN_BE_ENABLED,
              MULTI_TENANT,
              LOGOUT_AUTH0_ON_LOGOUT,
              WEBHOOK_CONFIGURATION,
              ADVANCED_SEARCH_CONFIGURATION,
              AUTOMATIC_SCM_CONFIGURATION,
              DEFAULT_BRANCH_MONITORING,
              PR_COMMENTING,
              PR_LINE_COMMENTING,
              EMAIL_CONFIGURATION,
              REPORTS_LIST_CAN_BE_ENABLED,
              REPORTS_LIST,
              ENABLE_SSO_ONLY).stream(),

          // Add all LicensedFeatures.
          // This is an allow list, whether they are enabled or not depends on the License used.
          // Excluding DATA_INSIGHTS for now
          Arrays.stream(LicensedFeature.values())
              .filter(f -> !f.equals(LicensedFeature.DATA_INSIGHTS)))

      .collect(toSet());

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

    return baseFeatures.stream().filter(this::isEnabled).collect(toSet());
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

  private boolean enabledFeaturesContainsFeatureWithName(String feature) {
    return ENABLED_FEATURES.stream()
        .map(Feature::name)
        .anyMatch(name -> name.equals(feature));
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
    log.info("Enabling/Disabling user configurable features for tenant {}", getTenant());

    set(PROPERTY_ENABLED, false);
    set(AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED, true);
    set(QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS, false);
  }

  private void set(String key, boolean value) {
    if (!enabledFeaturesContainsFeatureWithName(key)) {
      String operation = value ? "Enabling" : "Disabling";
      log.info("{} user configurable feature {} for tenant {}", operation, key, getTenant());
      systemConfigurationPropertyDAO.set(key, Boolean.toString(value));
    }
    else {
      log.trace("Unable to modify the feature {} as it is already enabled", key);
    }
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
