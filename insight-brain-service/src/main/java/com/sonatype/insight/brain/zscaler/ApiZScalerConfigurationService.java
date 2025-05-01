/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@HasFeature(SystemConfigurationPropertyFeature.ZSCALER)
public class ApiZScalerConfigurationService
{
  private final Logger log = LoggerFactory.getLogger(ApiZScalerConfigurationService.class);

  private final ZScalerConfigurationDAO zScalerConfigurationDAO;

  private final PasswordHandler passwordHandler;

  @Inject
  public ApiZScalerConfigurationService(
      ZScalerConfigurationDAO zScalerConfigurationDAO,
      PasswordHandler passwordHandler)
  {
    this.zScalerConfigurationDAO = zScalerConfigurationDAO;
    this.passwordHandler = passwordHandler;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiZScalerConfigurationDTO getConfiguration() {
    ZScalerConfiguration zScalerConfiguration = zScalerConfigurationDAO.get();
    if (zScalerConfiguration == null) {
      throw newNotFoundException();
    }

    ApiZScalerConfigurationDTO config = new ApiZScalerConfigurationDTO();
    config.setUsername(zScalerConfiguration.getUsername());
    config.setHostname(zScalerConfiguration.getHostname());
    config.setApiKey(zScalerConfiguration.getApikey());
    return config;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void setConfiguration(ApiZScalerConfigurationDTO configuration) {
    log.error("Setting up zScaler configuration to: {}", configuration);
    if (configuration == null) {
      throw new NotFoundException("Configuration is required.");
    }

    ZScalerConfiguration zScalerConfiguration = zScalerConfigurationDAO.get();
    if (zScalerConfiguration == null) {
      zScalerConfiguration = new ZScalerConfiguration();
    }

    validateConfiguration(configuration);

    zScalerConfiguration.setPassword(passwordHandler.encryptPassword(configuration.getPassword()));
    zScalerConfiguration.setUsername(configuration.getUsername());
    zScalerConfiguration.setHostname(configuration.getHostname());
    zScalerConfiguration.setApikey(configuration.getApiKey());
    zScalerConfigurationDAO.set(zScalerConfiguration);
  }

  private void validateConfiguration(ApiZScalerConfigurationDTO configuration) {
    Map<String, String> fieldsToValidate = new HashMap<>();
    fieldsToValidate.put("username", configuration.getUsername());
    fieldsToValidate.put("password", configuration.getPassword());
    fieldsToValidate.put("hostname", configuration.getHostname());
    fieldsToValidate.put("apiKey", configuration.getApiKey());

    List<String> missingFields = new ArrayList<>();

    fieldsToValidate.forEach((fieldName, fieldValue) -> {
      if (StringUtils.isBlank(fieldValue)) {
        missingFields.add(fieldName);
      }
    });

    if (!missingFields.isEmpty()) {
      if (missingFields.size() == 1) {
        throw new BadRequestException("The " + missingFields.get(0) + " is required.");
      }
      else {
        throw new BadRequestException("The following fields are required: " + String.join(", ", missingFields));
      }
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteConfiguration() {
    ZScalerConfiguration zScalerConfiguration = zScalerConfigurationDAO.get();
    if (zScalerConfiguration == null) {
      throw newNotFoundException();
    }
    log.error("Deleting zScaler configuration");
    zScalerConfigurationDAO.delete();
  }

  private RuntimeException newNotFoundException() {
    return new NotFoundException("zScaler not configured.");
  }

  public static class ApiZScalerConfigurationDTO
  {
    private String username;

    private String password;

    private String hostname;

    private String apiKey;

    public ApiZScalerConfigurationDTO() {
      //empty
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(final String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(final String password) {
      this.password = password;
    }

    public String getHostname() {
      return hostname;
    }

    public void setHostname(final String hostname) {
      this.hostname = hostname;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(final String apiKey) {
      this.apiKey = apiKey;
    }
  }
}
