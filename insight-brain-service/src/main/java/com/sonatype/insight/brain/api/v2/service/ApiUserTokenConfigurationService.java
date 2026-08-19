/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.ApiUserTokenConfigurationDTO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS;

/**
 * Service for managing user token configuration.
 *
 * @since 1.198
 */
@Named
@Singleton
public class ApiUserTokenConfigurationService
{
  private static final Logger log = LoggerFactory.getLogger(ApiUserTokenConfigurationService.class);

  private final ApiConfigurationService configurationService;

  @Inject
  public ApiUserTokenConfigurationService(final ApiConfigurationService configurationService) {
    this.configurationService = configurationService;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiUserTokenConfigurationDTO getConfiguration() {
    Map<String, Object> config =
        configurationService.getConfigurationNoAuthz(Set.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS));
    Integer expirationDays = parseExpirationDays(config.get(USER_TOKEN_DEFAULT_EXPIRATION_DAYS));
    return new ApiUserTokenConfigurationDTO(expirationDays);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void updateConfiguration(final ApiUserTokenConfigurationDTO configuration) {
    if (configuration == null) {
      throw new BadRequestException("Configuration cannot be null");
    }

    Integer expirationDays = configuration.userTokenDefaultExpirationDays();
    if (expirationDays != null) {
      validateExpirationDays(expirationDays);
      configurationService.setConfigurationNoAuthz(Map.of(USER_TOKEN_DEFAULT_EXPIRATION_DAYS, expirationDays));
      log.debug("Updated user token expiration days to: {}", expirationDays);
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void resetConfiguration(final Set<String> properties) {
    if (properties == null || properties.isEmpty()) {
      throw new BadRequestException("No properties specified for reset");
    }

    // Validate all properties are user token properties
    for (String property : properties) {
      if (!USER_TOKEN_DEFAULT_EXPIRATION_DAYS.equals(property)) {
        throw new BadRequestException(
            String.format("Invalid user token property: %s. Valid properties are: %s",
                property, USER_TOKEN_DEFAULT_EXPIRATION_DAYS));
      }
    }

    configurationService.deleteConfigurationNoAuthz(properties);
    log.debug("Reset user token configuration properties: {}", properties);
  }

  private Integer parseExpirationDays(final Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof Integer integer) {
      return integer;
    }

    try {
      return Integer.parseInt(value.toString());
    }
    catch (NumberFormatException e) {
      log.warn("Invalid integer value for {}: {}. Returning null.", USER_TOKEN_DEFAULT_EXPIRATION_DAYS, value);
      return null;
    }
  }

  private void validateExpirationDays(final int expirationDays) {
    if (expirationDays < 1 || expirationDays > 365) {
      throw new BadRequestException("Expiration days must be between 1 and 365");
    }
  }
}
