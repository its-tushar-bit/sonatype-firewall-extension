/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationResponseDTO;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiSamlConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiSamlConfigurationService apiSamlConfigurationService;

  private SamlConfigurationDAO samlConfigurationDAO = new SamlConfigurationDAO();

  @Inject
  private InsightConfig config;

  @Before
  public void setBaseUrl() {
    config.setBaseUrl("http://iq-server:8070/");
  }

  @Test
  public void testGetSamlConfiguration() {
    SamlConfiguration existing = tempEntity
        .newSamlConfiguration("<xml></xml>", "ent-id", "first-name", "last-name", "e-mail", "user-name", "teams");

    ApiSamlConfigurationResponseDTO response = apiSamlConfigurationService.getSamlConfiguration();

    assertThat(response.identityProviderMetadataXml).isEqualTo(existing.getIdentityProviderMetadataXml());
    assertThat(existing.getEntityId()).isEqualTo(response.entityId);
    assertConfigIdentical(existing, response);
  }

  @Test
  public void testGetSamlConfiguration_NotConfigured() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiSamlConfigurationService.getSamlConfiguration()).withMessage("SAML not configured.");
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_Insert() throws Exception {
    try {
      ApiSamlConfigurationDTO dto = dtoWithCustomValues();

      apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

      SamlConfiguration persisted = samlConfigurationDAO.get();
      assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
      assertThat(persisted.getEntityId()).isEqualTo(dto.entityId);
      assertConfigIdentical(persisted, dto);
    }
    finally {
      samlConfigurationDAO.delete(samlConfigurationDAO.get());
    }
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertNoEntityIdProvided() throws Exception {
    try {
      ApiSamlConfigurationDTO dto = dtoWithCustomValues();
      dto.entityId = null;  // Override with null to test default value is set

      apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

      SamlConfiguration persisted = samlConfigurationDAO.get();
      assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
      assertThat(persisted.getEntityId()).isEqualTo(config.getBaseUrl() + "api/v2/config/saml/metadata");
      assertConfigIdentical(persisted, dto);
    }
    finally {
      samlConfigurationDAO.delete(samlConfigurationDAO.get());
    }
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertEmptyConfiguration() throws Exception {
    try {
      ApiSamlConfigurationDTO dto = new ApiSamlConfigurationDTO();

      apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

      SamlConfiguration persisted = samlConfigurationDAO.get();
      assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
      // Should have default values
      assertThat(persisted.getEntityId()).isEqualTo(config.getBaseUrl() + "api/v2/config/saml/metadata");
      assertConfigIdentical(persisted, new SamlConfiguration());
    }
    finally {
      samlConfigurationDAO.delete(samlConfigurationDAO.get());
    }
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertNullConfiguration() throws Exception {
    try {
      String idpXml = validIdentityProviderXml();

      apiSamlConfigurationService.insertOrUpdateSamlConfiguration(idpXml, null);

      SamlConfiguration persisted = samlConfigurationDAO.get();
      assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(idpXml);
      // Should have default values
      assertThat(persisted.getEntityId()).isEqualTo(config.getBaseUrl() + "api/v2/config/saml/metadata");
      assertConfigIdentical(persisted, new SamlConfiguration());
    }
    finally {
      samlConfigurationDAO.delete(samlConfigurationDAO.get());
    }
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertPartialConfiguration() throws Exception {
    try {
      ApiSamlConfigurationDTO dto = new ApiSamlConfigurationDTO();
      dto.entityId = "http://custom-entity-id/";
      dto.firstNameAttributeName = "custom-firstname-attribute";
      SamlConfiguration defaultProperties = new SamlConfiguration();

      apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

      SamlConfiguration persisted = samlConfigurationDAO.get();
      // Partial values are provided ones
      assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
      assertThat(persisted.getEntityId()).isEqualTo(dto.entityId);
      assertThat(persisted.getFirstNameAttributeName()).isEqualTo(dto.firstNameAttributeName);
      // Rest should have the defaults
      assertThat(persisted.getLastNameAttributeName()).isEqualTo(defaultProperties.getLastNameAttributeName());
      assertThat(persisted.getEmailAttributeName()).isEqualTo(defaultProperties.getEmailAttributeName());
      assertThat(persisted.getUsernameAttributeName()).isEqualTo(defaultProperties.getUsernameAttributeName());
      assertThat(persisted.getGroupsAttributeName()).isEqualTo(defaultProperties.getGroupsAttributeName());
    }
    finally {
      samlConfigurationDAO.delete(samlConfigurationDAO.get());
    }
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertInvalidEntityId() {
    ApiSamlConfigurationDTO dto = dtoWithCustomValues();
    dto.entityId = "Bad Entity Id";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto))
        .withMessageMatching("Not a valid entity ID: .+ Bad Entity Id");
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertInvalidIdentityProviderMetadataXml() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiSamlConfigurationService
        .insertOrUpdateSamlConfiguration(invalidIdentityProviderXml(), dtoWithCustomValues()))
        .withMessageContaining("Identity provider metadata could not be validated");
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertNullIdentityProviderXml() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiSamlConfigurationService.insertOrUpdateSamlConfiguration(null, dtoWithCustomValues()))
        .withMessage("Identity Provider XML is required.");
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_Update() throws Exception {
    tempEntity.newSamlConfiguration("<xml></xml>", "ent-id", "name-first", "name-last", "mail-e", "name-user", "teamz");
    ApiSamlConfigurationDTO dto = dtoWithCustomValues();

    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

    SamlConfiguration persisted = samlConfigurationDAO.get();
    assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
    assertThat(persisted.getEntityId()).isEqualTo(dto.entityId);
    assertConfigIdentical(persisted, dto);
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateWithNullIdentityProviderMetadataXml() {
    tempEntity.newSamlConfiguration("<xml></xml>", "ent-id", "name-first", "name-last", "mail-e", "name-user", "teamz");
    ApiSamlConfigurationDTO dto = dtoWithCustomValues();

    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(null, dto);

    SamlConfiguration persisted = samlConfigurationDAO.get();
    // xml is neither validated nor changed
    assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo("<xml></xml>");
    // Entity Id and all other config is updated
    assertThat(persisted.getEntityId()).isEqualTo(dto.entityId);
    assertConfigIdentical(persisted, dto);
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateWithNullConfiguration() throws Exception {
    SamlConfiguration existing = tempEntity
        .newSamlConfiguration("<xml></xml>", "ent-id", "name-first", "name-last", "mail-e", "name-user", "teamz");

    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), null);

    SamlConfiguration persisted = samlConfigurationDAO.get();
    assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
    assertThat(persisted.getEntityId()).isEqualTo(existing.getEntityId());
    assertConfigIdentical(persisted, existing);
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateWithEmptyConfiguration() throws Exception {
    tempEntity.newSamlConfiguration("<xml></xml>", "ent-id", "name-first", "name-last", "mail-e", "name-user", "teamz");
    ApiSamlConfigurationDTO emptyDto = new ApiSamlConfigurationDTO();

    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), emptyDto);

    SamlConfiguration persisted = samlConfigurationDAO.get();
    // xml is updated
    assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
    // Config is overridden with defaults
    assertThat(persisted.getEntityId()).isEqualTo(config.getBaseUrl() + "api/v2/config/saml/metadata");
    assertConfigIdentical(persisted, new SamlConfiguration());
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateXmlAndEntityId() throws Exception {
    tempEntity.newSamlConfiguration("<xml></xml>", "ent-id", "name-first", "name-last", "mail-e", "name-user", "teamz");
    ApiSamlConfigurationDTO dto = new ApiSamlConfigurationDTO();
    dto.entityId = "http://custom-entity-id";

    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

    SamlConfiguration persisted = samlConfigurationDAO.get();
    // xml is updated
    assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
    assertThat(persisted.getEntityId()).isEqualTo(dto.entityId);
    // Config is overridden with defaults
    assertConfigIdentical(persisted, new SamlConfiguration());
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateWithInvalidIdentityProviderXml() {
    tempEntity.newSamlConfiguration("<xml></xml>", "ent-id", "name-first", "name-last", "mail-e", "name-user", "teamz");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiSamlConfigurationService.insertOrUpdateSamlConfiguration(invalidIdentityProviderXml(), null))
        .withMessageContaining("Identity provider metadata could not be validated");
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateWithInvalidEntityId() {
    tempEntity.newSamlConfiguration("<xml></xml>", "ent-id", "name-first", "name-last", "mail-e", "name-user", "teamz");
    ApiSamlConfigurationDTO dto = dtoWithCustomValues();
    dto.entityId = "Bad Entity Id";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiSamlConfigurationService.insertOrUpdateSamlConfiguration(null, dto))
        .withMessageContaining("Not a valid entity ID");
  }

  @Test
  public void testDeleteSamlConfiguration() {
    tempEntity.newSamlConfiguration("<xml></xml>", "ent-id", "first-name", "last-name", "e-mail", "user-name", "teams");

    apiSamlConfigurationService.deleteSamlConfiguration();

    assertThat(samlConfigurationDAO.get()).isNull();
  }

  @Test
  public void testDeleteSamlConfiguration_NotConfigured() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiSamlConfigurationService.deleteSamlConfiguration()).withMessage("SAML not configured.");
  }

  private String validIdentityProviderXml() throws Exception {
    URL resource = getClass().getResource("/" + getClass().getSimpleName() + "/identity-provider-metadata.xml");
    return FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
  }

  private String invalidIdentityProviderXml() throws Exception {
    URL resource = getClass().getResource("/" + getClass().getSimpleName() + "/missing-entity-descriptor.xml");
    return FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
  }

  private ApiSamlConfigurationDTO dtoWithCustomValues() {
    ApiSamlConfigurationDTO dto = new ApiSamlConfigurationDTO();
    dto.entityId = "http://my-host:9090/config/saml/";
    dto.firstNameAttributeName = "first-name";
    dto.lastNameAttributeName = "last-name";
    dto.emailAttributeName = "e-mail";
    dto.usernameAttributeName = "user-name";
    dto.groupsAttributeName = "teamz";
    return dto;
  }

  private void assertConfigIdentical(SamlConfiguration actual, ApiSamlConfigurationDTO expected) {
    assertThat(actual.getFirstNameAttributeName()).isEqualTo(expected.firstNameAttributeName);
    assertThat(actual.getLastNameAttributeName()).isEqualTo(expected.lastNameAttributeName);
    assertThat(actual.getEmailAttributeName()).isEqualTo(expected.emailAttributeName);
    assertThat(actual.getUsernameAttributeName()).isEqualTo(expected.usernameAttributeName);
    assertThat(actual.getGroupsAttributeName()).isEqualTo(expected.groupsAttributeName);
  }

  private void assertConfigIdentical(SamlConfiguration actual, SamlConfiguration expected) {
    assertThat(actual.getFirstNameAttributeName()).isEqualTo(expected.getFirstNameAttributeName());
    assertThat(actual.getLastNameAttributeName()).isEqualTo(expected.getLastNameAttributeName());
    assertThat(actual.getEmailAttributeName()).isEqualTo(expected.getEmailAttributeName());
    assertThat(actual.getUsernameAttributeName()).isEqualTo(expected.getUsernameAttributeName());
    assertThat(actual.getGroupsAttributeName()).isEqualTo(expected.getGroupsAttributeName());
  }
}
