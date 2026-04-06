/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ZscalerFormatDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@HasFeature(SystemConfigurationPropertyFeature.ZSCALER)
public class ApiZScalerConfigurationService
{
  private final Logger log = LoggerFactory.getLogger(ApiZScalerConfigurationService.class);

  private final ZScalerConfigurationDAO zScalerConfigurationDAO;

  private final ZscalerFormatDAO zscalerFormatDAO;

  private final PasswordHandler passwordHandler;

  public static final String EULA_MESSAGE = """
      access to and use of Sonatype's Zscaler integration is subject to and governed by these
      <a href="https://links.sonatype.com/products/firewall/docs/zscaler/zscaler-eula">License Terms</a>.
      """;

  @Inject
  public ApiZScalerConfigurationService(
      final ZScalerConfigurationDAO zScalerConfigurationDAO,
      final ZscalerFormatDAO zscalerFormatDAO,
      final PasswordHandler passwordHandler)
  {
    this.zScalerConfigurationDAO = zScalerConfigurationDAO;
    this.zscalerFormatDAO = zscalerFormatDAO;
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

    List<ZscalerFormat> zscalerFormats = zscalerFormatDAO.getAll();
    for (ZscalerFormat format : zscalerFormats) {
      switch (format.getFormat()) {
        case "maven" -> config.setMavenFormatEnabled(format.isEnabled());
        case "npm" -> config.setNpmFormatEnabled(format.isEnabled());
        case "pypi" -> config.setPypiFormatEnabled(format.isEnabled());
        case "nuget" -> config.setNugetFormatEnabled(format.isEnabled());
        default -> {
        }
      }
    }

    config.setEulaAgreed(true); // User must agree EULA to save the configuration. So, it is always true here.
    return config;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public String setConfiguration(ApiZScalerConfigurationDTO configuration) {
    log.debug("Setting up Zscaler configuration to: {}", configuration);
    if (configuration == null) {
      throw new NotFoundException("Configuration is required.");
    }

    validateConfiguration(configuration);

    ZScalerConfiguration zScalerConfiguration = zScalerConfigurationDAO.get();
    if (zScalerConfiguration == null) {
      zScalerConfiguration = new ZScalerConfiguration();
    }

    zScalerConfiguration.setPassword(passwordHandler.encryptPassword(configuration.getPassword()));
    zScalerConfiguration.setUsername(configuration.getUsername());
    zScalerConfiguration.setHostname(configuration.getHostname());
    zScalerConfiguration.setApikey(configuration.getApiKey());

    Map<String, ZscalerFormat> formatMap = zscalerFormatDAO.getAll()
        .stream()
        .collect(Collectors.toMap(ZscalerFormat::getFormat, f -> f));
    for (ZScalerSupportedFormat format : ZScalerSupportedFormat.values()) {
      boolean enabled = switch (format) {
        case MAVEN -> configuration.isMavenFormatEnabled();
        case NPM -> configuration.isNpmFormatEnabled();
        case PYPI -> configuration.isPypiFormatEnabled();
        case NUGET -> configuration.isNugetFormatEnabled();
      };
      formatMap.computeIfAbsent(String.valueOf(format).toLowerCase(),
          f -> new ZscalerFormat(f, enabled)).setEnabled(enabled);
    }
    List<ZscalerFormat> zscalerFormats = new ArrayList<>(formatMap.values());

    zScalerConfigurationDAO.set(zScalerConfiguration, zscalerFormats);

    return String.format("You have acknowledged and agreed that %s", EULA_MESSAGE);
  }

  private void validateConfiguration(ApiZScalerConfigurationDTO configuration) {
    if (Boolean.FALSE.equals(configuration.isEulaAgreed())) {
      throw new BadRequestException(String.format("You must acknowledge and agree that %s", EULA_MESSAGE));
    }

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

    if (configuration.isEulaAgreed() == null) {
      missingFields.add("eulaAgreed");
    }

    if (!missingFields.isEmpty()) {
      if (missingFields.size() == 1) {
        throw new BadRequestException("The " + missingFields.get(0) + " is required.");
      }
      else {
        throw new BadRequestException("The following fields are required: " + String.join(", ", missingFields));
      }
    }

    if (!StringUtils.isBlank(configuration.getHostname())) {
      ZScalerValidator.validateHostName(configuration.getHostname());
    }

    if (!StringUtils.isBlank(configuration.getApiKey()) && configuration.getApiKey().length() != 12) {
      throw new BadRequestException("The apiKey must be exactly 12 characters.");
    }

    if (!configuration.isMavenFormatEnabled() &&
        !configuration.isNpmFormatEnabled() &&
        !configuration.isPypiFormatEnabled() &&
        !configuration.isNugetFormatEnabled())
    {
      throw new BadRequestException("At least one format must be enabled.");
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteConfiguration() {
    ZScalerConfiguration zScalerConfiguration = zScalerConfigurationDAO.get();
    if (zScalerConfiguration == null) {
      throw newNotFoundException();
    }
    log.debug("Deleting Zscaler configuration");
    zScalerConfigurationDAO.delete();
  }

  private RuntimeException newNotFoundException() {
    return new NotFoundException("Zscaler not configured.");
  }
}
