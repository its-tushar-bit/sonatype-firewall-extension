/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiCrowdConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiCrowdConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiCrowdConfigurationService service;

  @Inject
  private CrowdConfigurationDAO dao;

  @Inject
  private PasswordHandler passwordHandler;

  @Test
  public void testGetCrowdConfiguration_NotConfigured() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service.getCrowdConfiguration())
        .withMessageContaining(ApiCrowdConfigurationService.CROWD_IS_NOT_CONFIGURED);
  }

  @Test
  public void testGetCrowdConfiguration_Configured() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();

    ApiCrowdConfigurationDTO result = service.getCrowdConfiguration();

    assertThat(result).isNotNull();
    assertThat(result.serverUrl).isEqualTo(crowdConfiguration.getServerUrl());
    assertThat(result.applicationName).isEqualTo(crowdConfiguration.getApplicationName());
    assertThat(result.applicationPassword).isNull();
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.insertOrUpdateCrowdConfiguration(null))
        .withMessageContaining(ApiCrowdConfigurationService.CROWD_CONFIGURATION_MUST_BE_SPECIFIED);
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_Insert() {
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = "serverUrl";
    dto.applicationName = "applicationName";
    dto.applicationPassword = "applicationPassword".toCharArray();
    assertThat(dao.get()).isNull();

    service.insertOrUpdateCrowdConfiguration(dto);

    CrowdConfiguration crowdConfiguration = dao.get();
    assertThat(crowdConfiguration).isNotNull();
    assertThat(crowdConfiguration.getServerUrl()).isEqualTo(dto.serverUrl);
    assertThat(crowdConfiguration.getApplicationName()).isEqualTo(dto.applicationName);
    assertThat(passwordHandler.decryptPassword(crowdConfiguration.getApplicationPassword())).isEqualTo(
        dto.applicationPassword);
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_Update() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration("http://localhost:8095/crowd", "iq server",
        passwordHandler.encryptPassword("password".toCharArray()));
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = crowdConfiguration.getServerUrl() + "2";
    dto.applicationName = crowdConfiguration.getApplicationName() + "2";
    dto.applicationPassword = (new String(crowdConfiguration.getApplicationPassword()) + "2").toCharArray();

    service.insertOrUpdateCrowdConfiguration(dto);

    crowdConfiguration = dao.get();
    assertThat(crowdConfiguration).isNotNull();
    assertThat(crowdConfiguration.getServerUrl()).isEqualTo(dto.serverUrl);
    assertThat(crowdConfiguration.getApplicationName()).isEqualTo(dto.applicationName);
    assertThat(passwordHandler.decryptPassword(crowdConfiguration.getApplicationPassword())).isEqualTo(
        dto.applicationPassword);
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_Update_OnlyServerUrl() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = crowdConfiguration.getServerUrl() + "2";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service.insertOrUpdateCrowdConfiguration(dto))
        .withMessageContaining(ApiCrowdConfigurationService.CROWD_SERVER_URL_UPDATE_NEEDS_APPLICATION_PASSWORD);
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_Update_ServerUrlAndPassword() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = crowdConfiguration.getServerUrl() + "2";
    dto.applicationPassword = crowdConfiguration.getApplicationPassword();

    service.insertOrUpdateCrowdConfiguration(dto);

    CrowdConfiguration storedCrowdConfiguration = dao.get();
    assertThat(storedCrowdConfiguration).isNotNull();
    assertThat(storedCrowdConfiguration.getServerUrl()).isEqualTo(dto.serverUrl);
    assertThat(storedCrowdConfiguration.getApplicationName()).isEqualTo(crowdConfiguration.getApplicationName());
    assertThat(passwordHandler.decryptPassword(storedCrowdConfiguration.getApplicationPassword())).isEqualTo(
        crowdConfiguration.getApplicationPassword());
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_Update_OnlyApplicationName() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.applicationName = crowdConfiguration.getApplicationName() + "2";

    service.insertOrUpdateCrowdConfiguration(dto);

    CrowdConfiguration storedCrowdConfiguration = dao.get();
    assertThat(storedCrowdConfiguration).isNotNull();
    assertThat(storedCrowdConfiguration.getServerUrl()).isEqualTo(crowdConfiguration.getServerUrl());
    assertThat(storedCrowdConfiguration.getApplicationName()).isEqualTo(dto.applicationName);
    assertThat(storedCrowdConfiguration.getApplicationPassword()).isEqualTo(
        crowdConfiguration.getApplicationPassword());
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_Update_OnlyApplicationPassword() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.applicationPassword = (new String(crowdConfiguration.getApplicationPassword()) + "2").toCharArray();

    service.insertOrUpdateCrowdConfiguration(dto);

    CrowdConfiguration storedCrowdConfiguration = dao.get();
    assertThat(storedCrowdConfiguration).isNotNull();
    assertThat(storedCrowdConfiguration.getServerUrl()).isEqualTo(crowdConfiguration.getServerUrl());
    assertThat(storedCrowdConfiguration.getApplicationName()).isEqualTo(crowdConfiguration.getApplicationName());
    assertThat(passwordHandler.decryptPassword(storedCrowdConfiguration.getApplicationPassword())).isEqualTo(
        dto.applicationPassword);
  }

  @Test
  public void testDeleteCrowdConfiguration_NotConfigured() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service.deleteCrowdConfiguration())
        .withMessageContaining(ApiCrowdConfigurationService.CROWD_IS_NOT_CONFIGURED);
  }

  @Test
  public void testDeleteCrowdConfiguration_Configured() {
    tempEntity.newCrowdConfiguration();

    service.deleteCrowdConfiguration();

    assertThat(dao.get()).isNull();
  }
}
