/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

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
  private static final Set<String> CONFIGURABLE_PROPERTIES = ImmutableSet.of(
      BASE_URL,
      FRAME_ANCESTORS_ALLOWLIST,
      EVENT_BUS_MAX_THREAD_POOL_SIZE,
      USER_AGENT_SUFFIX,
      MAX_ADVANCED_SEARCH_CLAUSE_COUNT,
      ADVANCED_SEARCH_CSV_EXPORT_DELIMITER,
      POLICY_MONITORING_HOUR,
      WEBHOOK_SECRET_PASSPHRASE,
      HDS_URL,
      SESSION_TIMEOUT_MINUTES,
      PURGE_SCAN_FILES,
      AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES,
      QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS,
      WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED,
      SAAS_LIFECYCLE_SCM_ENABLED
  );

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
    if (!CONFIGURABLE_PROPERTIES.contains(propertyName)) {
      log.debug("Property {} is not configurable.", propertyName);
      throw new BadRequestException(String.format("Property %s is not configurable.", propertyName));
    }
  }
}
