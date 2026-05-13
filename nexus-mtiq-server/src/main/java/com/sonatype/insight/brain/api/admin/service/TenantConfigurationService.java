/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableSet;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.*;

@Named
public class TenantConfigurationService
{
  // Visible for testing
  /**
   * CONFIGURABLE_PROPERTIES may be configured for all tenants
   */
  static final Set<String> CONFIGURABLE_PROPERTIES = ImmutableSet.of(
      ADVANCED_SEARCH_CSV_EXPORT_DELIMITER,
      AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES,
      BASE_URL,
      EVENT_BUS_MAX_THREAD_POOL_SIZE,
      FRAME_ANCESTORS_ALLOWLIST,
      MAX_ADVANCED_SEARCH_CLAUSE_COUNT,
      POLICY_MONITORING_HOUR,
      PURGE_SCAN_FILES,
      QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS,
      SAAS_LIFECYCLE_SCM_ENABLED,
      SKIP_SBOM_IMPORT_VALIDATION,
      WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED,
      WEBHOOK_SECRET_PASSPHRASE,
      WEBHOOK_SECRET_PASSPHRASE_FIPS,
      CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT,
      SBOM_BINARY_SCANNING,
      SBOM_CONTINUOUS_MONITORING_UI,
      SBOM_POLICIES,
      AUTO_WAIVERS,
      QUARANTINED_ITEM_CUSTOM_MESSAGE,
      MALWARE_DEFENSE_API_MAX_COMPONENTS,
      COMPONENT_CHANGE_DETECTION_MAX_COMPONENTS,
      COMPONENT_CHANGE_DETECTION_BATCH_SIZE,
      COMPONENT_CHANGE_DETECTION_TASK_PERIOD,
      COMPONENT_CHANGE_DETECTION_MAX_EVENTS,
      ALP_OBSERVED_LICENSE_DETECTION_ENABLED,
      COMPONENT_CHANGE_DETECTION_API,
      CONTAINER_IMAGES_EVAL_ENABLED,
      ZSCALER_UPDATE_TASK_PERIOD,
      ZSCALER,
      THIRD_PARTY_KEV_LOOKUP,
      EPSS_DATA,
      SESSION_TIMEOUT_MINUTES,
      USER_ACTIVITY_TRACKING,
      USER_TOKEN_DEFAULT_EXPIRATION_DAYS,
      MALICIOUS_URLS_PARTNER_ACCESS,
      EVALUATION_QUEUE_CONFIG,
      LIFECYCLE_TIER,
      HOSTED_REPOSITORY_EVALUATION);

  // Visible for testing
  /**
   * GLOBAL_CONFIGURABLE_PROPERTIES can only be configured globally with the global tenant
   */
  static final Set<String> GLOBAL_CONFIGURABLE_PROPERTIES = ImmutableSet.of(
      HDS_URL,
      HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR,
      SAAS_POLICY_MONITOR_POOL_SIZE,
      SOURCE_CONTROL_IMPORT_POOL_SIZE,
      SOURCE_CONTROL_EVENT_PROCESSOR_POOL_SIZE,
      USER_AGENT_SUFFIX,
      WARN_ON_NON_PRIMARY_STORAGE_ACCESS,
      COPY_STORAGE_CONFIG,
      MAX_CONCURRENT_TENANT_INDEX_CREATION);

  private static final String NO_CONFIG_SPECIFIED = "No configuration was specified.";

  private static final Logger log = LoggerFactory.getLogger(TenantConfigurationService.class);

  private final ApiConfigurationService apiConfigurationService;

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  @Inject
  public TenantConfigurationService(
      ApiConfigurationService apiConfigurationService,
      TenantUtil tenantUtil,
      TenantValidator tenantValidator)
  {
    this.apiConfigurationService = apiConfigurationService;
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
  }

  public Map<String, Object> getPropertiesConfiguration(String tenantSlug, Set<String> propertyNames) {
    validateCurrentTenant(tenantSlug);

    if (CollectionUtils.isEmpty(propertyNames)) {
      throw new BadRequestException(NO_CONFIG_SPECIFIED);
    }
    validatePropertyNames(propertyNames);

    return apiConfigurationService.getConfigurationNoAuthz(propertyNames);
  }

  public void setPropertiesConfiguration(String tenantSlug, Map<String, Object> propertiesConfiguration) {
    validateCurrentTenant(tenantSlug);

    if (CollectionUtils.isEmpty(propertiesConfiguration)) {
      throw new BadRequestException(NO_CONFIG_SPECIFIED);
    }
    validatePropertyNames(propertiesConfiguration.keySet());

    apiConfigurationService.setConfigurationNoAuthz(propertiesConfiguration);
  }

  public void deletePropertiesConfiguration(String tenantSlug, Set<String> propertyNames) {
    validateCurrentTenant(tenantSlug);

    if (CollectionUtils.isEmpty(propertyNames)) {
      throw new BadRequestException(NO_CONFIG_SPECIFIED);
    }
    validatePropertyNames(propertyNames);

    apiConfigurationService.deleteConfigurationNoAuthz(propertyNames);
  }

  private void validateCurrentTenant(String tenantSlug) {
    if (tenantUtil.isGlobalTenant()) {
      return;
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.debug("Tenant {} does not exist", tenantSlug);
      throw new NotFoundException("Tenant does not exist");
    }
  }

  private void validatePropertyNames(Set<String> propertyNames) {
    for (String propertyName : propertyNames) {
      validatePropertyName(propertyName);
    }
  }

  private void validatePropertyName(String propertyName) {
    if (GLOBAL_CONFIGURABLE_PROPERTIES.contains(propertyName) && !tenantUtil.isGlobalTenant()) {
      log.debug("Property {} is only configurable globally.", propertyName);
      throw new BadRequestException(String.format("Property %s is only configurable globally.", propertyName));
    }
    if (!CONFIGURABLE_PROPERTIES.contains(propertyName) && !GLOBAL_CONFIGURABLE_PROPERTIES.contains(propertyName)) {
      log.debug("Property {} is not configurable.", propertyName);
      throw new BadRequestException(String.format("Property %s is not configurable.", propertyName));
    }
  }
}
