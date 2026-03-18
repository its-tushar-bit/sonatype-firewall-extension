/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiCrowdConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiStatusDTO;
import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.security.CrowdMockServerRule;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.inject.Binder;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ApiCrowdConfigurationServiceTest
    extends AbstractComponentTest
{
  @Rule
  public CrowdMockServerRule crowdMockServer = new CrowdMockServerRule();

  @Inject
  private ApiCrowdConfigurationService service;

  @Inject
  private CrowdConfigurationDAO dao;

  private CrowdConfigurationDAO spyDAO;

  @Captor
  private ArgumentCaptor<CrowdConfiguration> crowdConfigurationArgumentCaptor;

  @Inject
  private PasswordHandler passwordHandler;

  @Override
  public void configure(Binder binder) {
    spyDAO = spy(daoFactory.createCrowdConfigurationDAO());
    binder.bind(CrowdConfigurationDAO.class).toInstance(spyDAO);
    super.configure(binder);
  }

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
  public void testInsertOrUpdateCrowdConfiguration_Update_EmptyOrWhitespaceValue() throws Exception {
    tempEntity.newCrowdConfiguration();
    testInsertOrUpdateCrowdConfiguration_Update_EmptyOrWhitespaceValue("serverUrl", "",
        "A Crowd server url is required.");
    testInsertOrUpdateCrowdConfiguration_Update_EmptyOrWhitespaceValue("serverUrl", " ",
        "A Crowd server url is required.");
    testInsertOrUpdateCrowdConfiguration_Update_EmptyOrWhitespaceValue("applicationName", "",
        "A Crowd application name is required.");
    testInsertOrUpdateCrowdConfiguration_Update_EmptyOrWhitespaceValue("applicationName", " ",
        "A Crowd application name is required.");
    testInsertOrUpdateCrowdConfiguration_Update_EmptyOrWhitespaceValue("applicationPassword", "",
        ApiCrowdConfigurationService.CROWD_APPLICATION_PASSWORD_CANNOT_BE_EMPTY_OR_WHITESPACE);
    testInsertOrUpdateCrowdConfiguration_Update_EmptyOrWhitespaceValue("applicationPassword", " ",
        ApiCrowdConfigurationService.CROWD_APPLICATION_PASSWORD_CANNOT_BE_EMPTY_OR_WHITESPACE);
  }

  private void testInsertOrUpdateCrowdConfiguration_Update_EmptyOrWhitespaceValue(
      String fieldName,
      String value,
      String expectedErrorMessage) throws Exception
  {
    CrowdConfiguration crowdConfiguration = dao.get();
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = crowdConfiguration.getServerUrl() + "2";
    dto.applicationName = crowdConfiguration.getApplicationName() + "2";
    dto.applicationPassword = (new String(crowdConfiguration.getApplicationPassword()) + "2").toCharArray();
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode objectNode = (ObjectNode) objectMapper.readTree(objectMapper.writeValueAsString(dto));
    objectNode.set(fieldName, new TextNode(value));
    ApiCrowdConfigurationDTO dtoToTest = objectMapper.convertValue(objectNode, ApiCrowdConfigurationDTO.class);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> service.insertOrUpdateCrowdConfiguration(dtoToTest)).withMessageContaining(expectedErrorMessage);
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

  @Test
  public void testTestCrowdConfiguration_NoDTO_NotConfigured() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service.testCrowdConfiguration(null))
        .withMessageContaining(ApiCrowdConfigurationService.CROWD_IS_NOT_CONFIGURED);
  }

  @Test
  public void testTestCrowdConfiguration_DTO_Success() throws Exception {
    crowdMockServer.mockTestConnection();
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = crowdMockServer.getBaseUrl() + "/crowd";
    dto.applicationName = "applicationName";
    dto.applicationPassword = "applicationPassword".toCharArray();

    ApiStatusDTO result = service.testCrowdConfiguration(dto);

    verify(spyDAO).validate(crowdConfigurationArgumentCaptor.capture());
    CrowdConfiguration crowdConfiguration = crowdConfigurationArgumentCaptor.getValue();
    assertThat(crowdConfiguration.getServerUrl()).isEqualTo(dto.serverUrl);
    assertThat(crowdConfiguration.getApplicationName()).isEqualTo(dto.applicationName);
    assertThat(passwordHandler.decryptPassword(crowdConfiguration.getApplicationPassword())).isEqualTo(
        dto.applicationPassword);
    assertThat(result).isNotNull();
    assertThat(result.code).isEqualTo(200);
    assertThat(result.message).isNull();
  }

  @Test
  public void testTestCrowdConfiguration_DTO_Fail() {
    crowdMockServer.mockTestConnectionError(401);
    ApiCrowdConfigurationDTO dto = new ApiCrowdConfigurationDTO();
    dto.serverUrl = crowdMockServer.getBaseUrl() + "/crowd";
    dto.applicationName = "applicationName";
    dto.applicationPassword = "applicationPassword".toCharArray();

    ApiStatusDTO result = service.testCrowdConfiguration(dto);

    verify(spyDAO).validate(crowdConfigurationArgumentCaptor.capture());
    CrowdConfiguration crowdConfiguration = crowdConfigurationArgumentCaptor.getValue();
    assertThat(crowdConfiguration.getServerUrl()).isEqualTo(dto.serverUrl);
    assertThat(crowdConfiguration.getApplicationName()).isEqualTo(dto.applicationName);
    assertThat(passwordHandler.decryptPassword(crowdConfiguration.getApplicationPassword())).isEqualTo(
        dto.applicationPassword);
    assertThat(result).isNotNull();
    assertThat(result.code).isEqualTo(400);
    assertThat(result.message).isEqualTo("Error");
  }

  @Test
  public void testTestCrowdConfiguration_NoDTO_Configured_Success() throws Exception {
    crowdMockServer.mockTestConnection();
    tempEntity.newCrowdConfiguration(crowdMockServer.getBaseUrl() + "/crowd", "applicationName",
        passwordHandler.encryptPassword("applicationPassword".toCharArray()));

    ApiStatusDTO result = service.testCrowdConfiguration(null);

    assertThat(result).isNotNull();
    assertThat(result.code).isEqualTo(200);
    assertThat(result.message).isNull();
  }

  @Test
  public void testTestCrowdConfiguration_NoDTO_Configured_Fail() {
    crowdMockServer.mockTestConnectionError(401);
    tempEntity.newCrowdConfiguration(crowdMockServer.getBaseUrl() + "/crowd", "applicationName",
        passwordHandler.encryptPassword("applicationPassword".toCharArray()));

    ApiStatusDTO result = service.testCrowdConfiguration(null);

    assertThat(result).isNotNull();
    assertThat(result.code).isEqualTo(400);
    assertThat(result.message).isEqualTo("Error");
  }
}
