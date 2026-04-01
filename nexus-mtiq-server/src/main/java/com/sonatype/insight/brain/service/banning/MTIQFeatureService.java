/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.api.v2.FeatureAlreadyDisabledException;
import com.sonatype.insight.brain.api.v2.FeatureAlreadyEnabledException;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   * This is the list of features that are always enabled in MTIQ.
   */
  private static final List<SystemConfigurationPropertyFeature> MTIQ_ENABLED_FEATURES = Arrays.asList(
      SystemConfigurationPropertyFeature.AUTOMATIC_APPLICATION_CONFIGURATION,
      SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION,
      SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER);

  /**
   * This is the list of features that are never enabled in MTIQ.
   */
  private static final List<Feature> MTIQ_BANNED_FEATURES = Arrays.asList(
      LicensedFeature.DATA_INSIGHTS,
      SystemConfigurationPropertyFeature.SUCCESS_METRICS_CONFIGURATION,
      SystemConfigurationPropertyFeature.PRODUCT_LICENSE_CONFIGURATION,
      SystemConfigurationPropertyFeature.SYSTEM_NOTICE_CONFIGURATION,
      SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES,
      SystemConfigurationPropertyFeature.PROXY_CONFIGURATION,
      SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API,
      SystemConfigurationPropertyFeature.CROWD_INTEGRATION,
      SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE,
      SystemConfigurationPropertyFeature.LDAP_CONFIGURATION,
      SystemConfigurationPropertyFeature.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES,
      SystemConfigurationPropertyFeature.SCAN_POM_FILES_IN_META_INF_DIRECTORY,
      SystemConfigurationPropertyFeature.VULNERABILITY_SOURCE,
      SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE,
      SystemConfigurationPropertyFeature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS,
      SystemConfigurationPropertyFeature.GUIDE_MCP);

  private final ApiConfigFeaturesService service;

  private final MailConfigurationDAO mailConfigurationDAO;

  private final TenantUtil tenantUtil;

  public static final List<SystemConfigurationPropertyFeature> BANNED_SYSTEM_CONFIGURATION_PROPERTY_FEATURES =
      MTIQ_BANNED_FEATURES.stream()
          .filter(SystemConfigurationPropertyFeature.class::isInstance)
          .map(SystemConfigurationPropertyFeature.class::cast)
          .toList();

  @Inject
  public MTIQFeatureService(
      final ProductLicense productLicense,
      final Configuration configuration,
      final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      final ApiConfigFeaturesService service,
      final DeveloperEnablementService developerEnablementService,
      final MailConfigurationDAO mailConfigurationDAO,
      final TenantUtil tenantUtil)
  {
    super(productLicense, configuration, systemConfigurationPropertyDAO, developerEnablementService);

    this.service = service;
    this.mailConfigurationDAO = mailConfigurationDAO;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public Set<Feature> getFeatures() {
    Set<Feature> features = super.getFeatures();

    features.remove(SINGLE_TENANT);
    features.add(MULTI_TENANT);

    MTIQ_BANNED_FEATURES.forEach(features::remove);

    // CLM-38607: Hide email configuration for tenants that do not have a custom mail config.
    // Tenants with existing custom configs retain access; global tenant always has access.
    if (!tenantUtil.isGlobalTenant() && mailConfigurationDAO.getWithoutFallback() == null) {
      features.remove(SystemConfigurationPropertyFeature.EMAIL_CONFIGURATION);
    }

    return features;
  }

  @Override
  protected void removeDisabledFeatures(Set<Feature> features) {
    super.removeDisabledFeatures(features);
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
