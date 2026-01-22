/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.nio.CharBuffer;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiCrowdConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.CrowdClientFactory;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.134
 */
@Named
public class ApiCrowdConfigurationService
{
  public static final String CROWD_SERVER_URL_AUDIT_KEY = "serverUrl";

  public static final String CROWD_APPLICATION_NAME_AUDIT_KEY = "applicationName";

  // Visible for testing
  static final String CROWD_IS_NOT_CONFIGURED = "Crowd is not configured.";

  // Visible for testing
  static final String CROWD_CONFIGURATION_MUST_BE_SPECIFIED = "A Crowd configuration must be specified.";

  // Visible for testing
  static final String CROWD_SERVER_URL_UPDATE_NEEDS_APPLICATION_PASSWORD =
      "A Crowd server url must be updated with its application password.";

  // Visible for testing
  static final String CROWD_APPLICATION_PASSWORD_CANNOT_BE_EMPTY_OR_WHITESPACE =
      "A Crowd application password cannot be empty or only whitespace.";

  private final CrowdConfigurationDAO crowdConfigurationDAO;

  private final PasswordHandler passwordHandler;

  private final CrowdClientFactory crowdClientFactory;

  @Inject
  public ApiCrowdConfigurationService(
      CrowdConfigurationDAO crowdConfigurationDAO,
      PasswordHandler passwordHandler,
      CrowdClientFactory crowdClientFactory)
  {
    this.crowdConfigurationDAO = crowdConfigurationDAO;
    this.passwordHandler = passwordHandler;
    this.crowdClientFactory = crowdClientFactory;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiCrowdConfigurationDTO getCrowdConfiguration() {
    CrowdConfiguration crowdConfiguration = crowdConfigurationDAO.get();

    if (crowdConfiguration == null) {
      throw new NotFoundException(CROWD_IS_NOT_CONFIGURED);
    }

    return convertToDTO(crowdConfiguration);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void insertOrUpdateCrowdConfiguration(ApiCrowdConfigurationDTO dto) {
    if (dto == null) {
      throw new BadRequestException(CROWD_CONFIGURATION_MUST_BE_SPECIFIED);
    }
    if (dto.applicationPassword != null && StringUtils.isBlank(CharBuffer.wrap(dto.applicationPassword))) {
      throw new BadRequestException(CROWD_APPLICATION_PASSWORD_CANNOT_BE_EMPTY_OR_WHITESPACE);
    }
    CrowdConfiguration crowdConfiguration = crowdConfigurationDAO.get();
    if (crowdConfiguration == null) {
      crowdConfiguration = new CrowdConfiguration();
      crowdConfiguration.setServerUrl(dto.serverUrl);
      crowdConfiguration.setApplicationName(dto.applicationName);
      crowdConfiguration.setApplicationPassword(passwordHandler.encryptPassword(dto.applicationPassword));
    }
    else {
      if (dto.applicationPassword == null && dto.serverUrl != null &&
          !crowdConfiguration.getServerUrl().equalsIgnoreCase(dto.serverUrl)) {
        throw new BadRequestException(CROWD_SERVER_URL_UPDATE_NEEDS_APPLICATION_PASSWORD);
      }
      if (dto.serverUrl != null) {
        crowdConfiguration.setServerUrl(dto.serverUrl);
      }
      if (dto.applicationName != null) {
        crowdConfiguration.setApplicationName(dto.applicationName);
      }
      if (dto.applicationPassword != null) {
        crowdConfiguration.setApplicationPassword(passwordHandler.encryptPassword(dto.applicationPassword));
      }
    }
    AuditData.get().setData(CROWD_SERVER_URL_AUDIT_KEY, crowdConfiguration.getServerUrl())
        .setData(CROWD_APPLICATION_NAME_AUDIT_KEY, crowdConfiguration.getApplicationName());
    crowdConfigurationDAO.set(crowdConfiguration);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteCrowdConfiguration() {
    CrowdConfiguration crowdConfiguration = crowdConfigurationDAO.get();
    if (crowdConfiguration == null) {
      throw new NotFoundException(CROWD_IS_NOT_CONFIGURED);
    }
    AuditData.get().setData(CROWD_SERVER_URL_AUDIT_KEY, crowdConfiguration.getServerUrl())
        .setData(CROWD_APPLICATION_NAME_AUDIT_KEY, crowdConfiguration.getApplicationName());
    crowdConfigurationDAO.delete(crowdConfiguration);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiStatusDTO testCrowdConfiguration(ApiCrowdConfigurationDTO dto) {
    CrowdConfiguration crowdConfiguration;
    if (dto == null) {
      crowdConfiguration = crowdConfigurationDAO.get();
      if (crowdConfiguration == null) {
        throw new NotFoundException(CROWD_IS_NOT_CONFIGURED);
      }
    }
    else {
      crowdConfiguration = new CrowdConfiguration();
      crowdConfiguration.setServerUrl(dto.serverUrl);
      crowdConfiguration.setApplicationName(dto.applicationName);
      if (dto.applicationPassword != null && StringUtils.isNotBlank(CharBuffer.wrap(dto.applicationPassword))) {
        crowdConfiguration.setApplicationPassword(passwordHandler.encryptPassword(dto.applicationPassword));
      }
      crowdConfigurationDAO.validate(crowdConfiguration);
    }
    return testCrowdConfiguration(crowdConfiguration);
  }

  private ApiStatusDTO testCrowdConfiguration(CrowdConfiguration crowdConfiguration) {
    ApiStatusDTO dto = new ApiStatusDTO();
    try {
      crowdClientFactory.createCrowdClient(crowdConfiguration).testConnection();
      dto.code = 200;
    }
    catch (Exception e) {
      dto.code = 400;
      dto.message = e.getMessage();
    }
    return dto;
  }

  private ApiCrowdConfigurationDTO convertToDTO(CrowdConfiguration crowdConfiguration) {
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = crowdConfiguration.getServerUrl();
    dto.applicationName = crowdConfiguration.getApplicationName();
    return dto;
  }
}
