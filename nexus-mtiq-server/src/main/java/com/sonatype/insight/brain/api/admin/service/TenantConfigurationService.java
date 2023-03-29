/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.ClassUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.service.ConfigurationProperty.getConfigurationPropertiesByName;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.BASE_URL;

@Named
public class TenantConfigurationService
{
  private static final Logger log = LoggerFactory.getLogger(TenantConfigurationService.class);

  public static final Set<String> CONFIGURABLE_PROPERTIES = ImmutableSet.of(
      BASE_URL
  );

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  protected final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public TenantConfigurationService(
      TenantUtil tenantUtil,
      TenantValidator tenantValidator,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
  {
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  public void setPropertiesConfiguration(Map<String, Object> propertiesConfiguration, String tenantSlug) {
    validateCurrentTenant(tenantSlug);

    if (CollectionUtils.isEmpty(propertiesConfiguration)) {
      throw new BadRequestException("No configuration was specified.");
    }

    for (Entry<String, Object> propertyConfig : propertiesConfiguration.entrySet()) {
      if (!CONFIGURABLE_PROPERTIES.contains(propertyConfig.getKey()) ||
          !getConfigurationPropertiesByName().containsKey(propertyConfig.getKey())) {
        log.error("Property {} is not configurable.", propertyConfig.getKey());
        throw new BadRequestException(String.format("Property %s is not configurable.", propertyConfig.getKey()));
      }

      validatePropertyConfigValue(propertyConfig.getKey(), propertyConfig.getValue());

      setPropertyConfiguration(propertyConfig.getKey(), propertyConfig.getValue());
    }
  }

  private void validateCurrentTenant(String tenantSlug) {
    if (tenantUtil.isGlobalTenant()) {
      throw new BadRequestException("Invalid tenant");
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.debug("Tenant {} doesn't exist", tenantSlug);
      throw new NotFoundException("Tenant doesn't exist");
    }
  }

  private void validatePropertyConfigValue(String propertyName, Object propertyValue) {
    if (propertyValue == null) {
      throw new BadRequestException(String.format("Property %s has no value.", propertyName));
    }

    Class<?> expectedType = getConfigurationPropertiesByName().get(propertyName).getType();
    Class<?> actualType = propertyValue.getClass();

    if (!ClassUtils.isAssignable(actualType, expectedType)) {
      throw new BadRequestException(
          String.format("Invalid value for %s, expected %s, but got %s.", propertyName, expectedType, actualType));
    }
  }

  private void setPropertyConfiguration(String property, Object value) {
    systemConfigurationPropertyDAO.set(property,
        getConfigurationPropertiesByName().get(property).getValueToString().apply(new Object[]{}, value));
  }
}
