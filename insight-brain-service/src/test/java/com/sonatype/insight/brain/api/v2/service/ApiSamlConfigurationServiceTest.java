/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationResponseDTO;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.SamlDeploymentManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Base64;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.util.JAXPValidationUtil;
import org.mockito.Mock;

public class ApiSamlConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiSamlConfigurationService apiSamlConfigurationService;

  @Inject
  private SamlConfigurationService samlConfigurationService;

  @Inject
  private SamlDeploymentManager samlDeploymentManager;

  @Inject
  private OperationalDataStore operationalDataStore;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Before
  public void setBaseUrl() {
    setBaseUrl("http://iq-server:8070/");
  }

  @Rule
  public LogOutput logOutput = new LogOutput(ApiSamlConfigurationService.class);

  @Test
  public void testGetSamlConfiguration() {
    SamlConfiguration existing = tempEntity.newSamlConfiguration("My Awesome IdP", "<xml></xml>", "ent-id",
        "first-name", "last-name", "e-mail", "user-name", "teams", true, false);
    samlConfigurationService.insert(existing);

    ApiSamlConfigurationResponseDTO response = apiSamlConfigurationService.getSamlConfiguration();

    assertThat(response.identityProviderMetadataXml).isEqualTo(existing.getIdentityProviderMetadataXml());
    assertThat(existing.getEntityId()).isEqualTo(response.entityId);
    assertConfigIdentical(existing, response);
  }

  @Test
  public void testGetSamlConfiguration_NotConfigured() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiSamlConfigurationService.getSamlConfiguration())
        .withMessage("SAML not configured.");
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_Insert() throws Exception {
    try {
      ApiSamlConfigurationDTO dto = dtoWithCustomValues();

      apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

      SamlConfiguration persisted = samlConfigurationService.get();
      assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
      assertThat(persisted.getEntityId()).isEqualTo(dto.entityId);
      assertConfigIdentical(persisted, dto);
    }
    finally {
      samlConfigurationService.delete();
    }
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertNoEntityIdProvided() throws Exception {
    try {
      ApiSamlConfigurationDTO dto = dtoWithCustomValues();
      dto.entityId = null; // Override with null to test default value is set

      apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

      SamlConfiguration persisted = samlConfigurationService.get();
      assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
      assertThat(persisted.getEntityId()).isEqualTo(getBaseUrl() + "api/v2/config/saml/metadata");
      assertConfigIdentical(persisted, dto);
    }
    finally {
      samlConfigurationService.delete();
    }
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertEmptyConfiguration() throws Exception {
    try {
      ApiSamlConfigurationDTO dto = new ApiSamlConfigurationDTO();

      apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

      SamlConfiguration persisted = samlConfigurationService.get();
      assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
      // Should have default values
      assertThat(persisted.getEntityId()).isEqualTo(getBaseUrl() + "api/v2/config/saml/metadata");
      assertConfigIdentical(persisted, new SamlConfiguration());
    }
    finally {
      samlConfigurationService.delete();
    }
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertNullConfiguration() throws Exception {
    try {
      String idpXml = validIdentityProviderXml();

      apiSamlConfigurationService.insertOrUpdateSamlConfiguration(idpXml, null);

      SamlConfiguration persisted = samlConfigurationService.get();
      assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(idpXml);
      // Should have default values
      assertThat(persisted.getEntityId()).isEqualTo(getBaseUrl() + "api/v2/config/saml/metadata");
      assertConfigIdentical(persisted, new SamlConfiguration());
    }
    finally {
      samlConfigurationService.delete();
    }
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertPartialConfiguration() throws Exception {
    try {
      ApiSamlConfigurationDTO dto = new ApiSamlConfigurationDTO();
      dto.entityId = "http://custom-entity-id/";
      dto.firstNameAttributeName = "custom-firstname-attribute";
      dto.validateAssertionSignature = false;
      SamlConfiguration defaultProperties = new SamlConfiguration();

      apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

      SamlConfiguration persisted = samlConfigurationService.get();
      // Partial values are provided ones
      assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
      assertThat(persisted.getEntityId()).isEqualTo(dto.entityId);
      assertThat(persisted.getFirstNameAttributeName()).isEqualTo(dto.firstNameAttributeName);
      assertThat(persisted.getValidateAssertionSignature()).isFalse();
      // Rest should have the defaults
      assertThat(persisted.getIdentityProviderName()).isEqualTo(defaultProperties.getIdentityProviderName());
      assertThat(persisted.getLastNameAttributeName()).isEqualTo(defaultProperties.getLastNameAttributeName());
      assertThat(persisted.getEmailAttributeName()).isEqualTo(defaultProperties.getEmailAttributeName());
      assertThat(persisted.getUsernameAttributeName()).isEqualTo(defaultProperties.getUsernameAttributeName());
      assertThat(persisted.getGroupsAttributeName()).isEqualTo(defaultProperties.getGroupsAttributeName());
      assertThat(persisted.getValidateResponseSignature()).isNull();
    }
    finally {
      samlConfigurationService.delete();
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
  public void testInsertOrUpdateSamlConfiguration_InsertBadCertificate() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiSamlConfigurationService
        .insertOrUpdateSamlConfiguration(invalidCertificate(), dtoWithCustomValues()))
        .withMessageContainingAll("Configuration could not be validated", "invalid certificate");
    assertThat(samlConfigurationService.get()).isNull();
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_InsertBadCertificateNullConfiguration() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiSamlConfigurationService
        .insertOrUpdateSamlConfiguration(invalidCertificate(), null))
        .withMessageContainingAll("Configuration could not be validated", "invalid certificate");
    assertThat(samlConfigurationService.get()).isNull();
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateBadCertificate() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", "<xml></xml>", "ent-id", "first-name", "l-name", "e-mail",
            "user-name", "teams", null, null);
    samlConfigurationService.insert(samlConfiguration);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiSamlConfigurationService
        .insertOrUpdateSamlConfiguration(invalidCertificate(), dtoWithCustomValues()))
        .withMessageContainingAll("Configuration could not be validated", "invalid certificate");

    samlConfiguration = samlConfigurationService.get();
    assertThat(samlConfiguration.getIdentityProviderMetadataXml()).isEqualTo("<xml></xml>");
    assertThat(samlConfiguration.getEntityId()).isEqualTo("ent-id");
    assertThat(samlConfiguration.getFirstNameAttributeName()).isEqualTo("first-name");
    assertThat(samlConfiguration.getLastNameAttributeName()).isEqualTo("l-name");
    assertThat(samlConfiguration.getEmailAttributeName()).isEqualTo("e-mail");
    assertThat(samlConfiguration.getUsernameAttributeName()).isEqualTo("user-name");
    assertThat(samlConfiguration.getGroupsAttributeName()).isEqualTo("teams");
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateBadCertificateNullConfiguration() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", "<xml></xml>", "ent-id", "first-name", "l-name", "e-mail",
            "user-name", "teams", null, null);
    samlConfigurationService.insert(samlConfiguration);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiSamlConfigurationService
        .insertOrUpdateSamlConfiguration(invalidCertificate(), null))
        .withMessageContainingAll("Configuration could not be validated", "invalid certificate");

    samlConfiguration = samlConfigurationService.get();
    assertThat(samlConfiguration.getIdentityProviderMetadataXml()).isEqualTo("<xml></xml>");
    assertThat(samlConfiguration.getEntityId()).isEqualTo("ent-id");
    assertThat(samlConfiguration.getFirstNameAttributeName()).isEqualTo("first-name");
    assertThat(samlConfiguration.getLastNameAttributeName()).isEqualTo("l-name");
    assertThat(samlConfiguration.getEmailAttributeName()).isEqualTo("e-mail");
    assertThat(samlConfiguration.getUsernameAttributeName()).isEqualTo("user-name");
    assertThat(samlConfiguration.getGroupsAttributeName()).isEqualTo("teams");
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_Update() throws Exception {
    tempEntity.newSamlConfiguration("My Awesome IdP", "<xml></xml>", "ent-id", "name-first", "name-last", "mail-e",
        "name-user", "teamz", null, null);
    ApiSamlConfigurationDTO dto = dtoWithCustomValues();

    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

    SamlConfiguration persisted = samlConfigurationService.get();
    assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
    assertThat(persisted.getEntityId()).isEqualTo(dto.entityId);
    assertConfigIdentical(persisted, dto);
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateWithNullIdentityProviderMetadataXml() throws Exception {
    String idpXml = validIdentityProviderXml();
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", idpXml, "ent-id", "name-first", "name-last", "mail-e",
            "name-user", "teamz", null, null);
    samlConfigurationService.insert(samlConfiguration);

    ApiSamlConfigurationDTO dto = dtoWithCustomValues();

    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(null, dto);

    SamlConfiguration persisted = samlConfigurationService.get();
    // xml is not changed
    assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(idpXml);
    // Entity Id and all other config is updated
    assertThat(persisted.getEntityId()).isEqualTo(dto.entityId);
    assertConfigIdentical(persisted, dto);
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateWithNullConfiguration() throws Exception {
    SamlConfiguration existing = tempEntity.newSamlConfiguration("My Awesome IdP", "<xml></xml>", "ent-id",
        "name-first", "name-last", "mail-e", "name-user", "teamz", null, null);
    samlConfigurationService.insert(existing);

    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), null);

    SamlConfiguration persisted = samlConfigurationService.get();
    assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
    assertThat(persisted.getEntityId()).isEqualTo(existing.getEntityId());
    assertConfigIdentical(persisted, existing);
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateWithEmptyConfiguration() throws Exception {
    tempEntity.newSamlConfiguration("My Awesome IdP", "<xml></xml>", "ent-id", "name-first", "name-last", "mail-e",
        "name-user", "teamz", null, null);
    ApiSamlConfigurationDTO emptyDto = new ApiSamlConfigurationDTO();

    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), emptyDto);

    SamlConfiguration persisted = samlConfigurationService.get();
    // xml is updated
    assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
    // Config is overridden with defaults
    assertThat(persisted.getEntityId()).isEqualTo(getBaseUrl() + "api/v2/config/saml/metadata");
    assertConfigIdentical(persisted, new SamlConfiguration());
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateXmlAndEntityId() throws Exception {
    tempEntity.newSamlConfiguration("My Awesome IdP", "<xml></xml>", "ent-id", "name-first", "name-last", "mail-e",
        "name-user", "teamz", null, null);
    ApiSamlConfigurationDTO dto = new ApiSamlConfigurationDTO();
    dto.entityId = "http://custom-entity-id";

    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), dto);

    SamlConfiguration persisted = samlConfigurationService.get();
    // xml is updated
    assertThat(persisted.getIdentityProviderMetadataXml()).isEqualTo(validIdentityProviderXml());
    assertThat(persisted.getEntityId()).isEqualTo(dto.entityId);
    // Config is overridden with defaults
    assertConfigIdentical(persisted, new SamlConfiguration());
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateWithInvalidIdentityProviderXml() {
    tempEntity.newSamlConfiguration("My Awesome IdP", "<xml></xml>", "ent-id", "name-first", "name-last", "mail-e",
        "name-user", "teamz", null, null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiSamlConfigurationService.insertOrUpdateSamlConfiguration(invalidIdentityProviderXml(), null))
        .withMessageContaining("Identity provider metadata could not be validated");
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateWithInvalidEntityId() {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", "<xml></xml>", "ent-id", "name-first", "name-last", "mail-e",
            "name-user", "teamz", null, null);
    samlConfigurationService.insert(samlConfiguration);

    ApiSamlConfigurationDTO dto = dtoWithCustomValues();
    dto.entityId = "Bad Entity Id";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiSamlConfigurationService.insertOrUpdateSamlConfiguration(null, dto))
        .withMessageContaining("Not a valid entity ID");
  }

  @Test
  public void testInsertOrUpdateSamlConfiguration_UpdateSamlDeployment() throws Exception {
    try {
      assertThat(samlDeploymentManager.get()).isNull();

      apiSamlConfigurationService.insertOrUpdateSamlConfiguration(validIdentityProviderXml(), null);

      assertThat(samlDeploymentManager.get().getIDP().getEntityID()).isEqualTo("http://localhost");
    }
    finally {
      samlConfigurationService.delete();
    }
  }

  @Test
  public void testDeleteSamlConfiguration() throws Exception {
    String idpXml = validIdentityProviderXml();
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", idpXml, "ent-id", "first-name", "last-name", "e-mail",
            "user-name", "teams", null, null);
    samlConfigurationService.insert(samlConfiguration);

    apiSamlConfigurationService.deleteSamlConfiguration();

    assertThat(samlConfigurationService.get()).isNull();
  }

  @Test
  public void testDeleteSamlConfiguration_NotConfigured() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> apiSamlConfigurationService.deleteSamlConfiguration())
        .withMessage("SAML not configured.");
  }

  @Test
  public void testDeleteSamlConfiguration_UpdateSamlDeployment() throws Exception {
    SamlConfiguration samlConfiguration =
        tempEntity.newSamlConfiguration("My Awesome IdP", validIdentityProviderXml(), "ent-id", "first-name",
            "last-name",
            "e-mail", "user-name", "teams", null, null);
    samlConfigurationService.insert(samlConfiguration);

    samlDeploymentManager.updateFromConfiguration();
    assertThat(samlDeploymentManager.get()).isNotNull();

    apiSamlConfigurationService.deleteSamlConfiguration();

    assertThat(samlDeploymentManager.get()).isNull();
  }

  @Test
  public void testGetMetadata_NotConfigured() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> apiSamlConfigurationService.getMetadata())
        .withMessage("SAML not configured.");
  }

  @Test
  public void testGetMetadata() throws Exception {
    SamlConfiguration samlConfiguration = tempEntity.newSamlConfiguration("My Awesome IdP", validIdentityProviderXml(),
        "ent-id", "first-name", "last-name", "e-mail", "user-name", "teams", null, null);
    samlConfigurationService.insert(samlConfiguration);
    samlDeploymentManager.updateFromConfiguration();

    String xmlMetadata = apiSamlConfigurationService.getMetadata();

    JAXPValidationUtil.validator().validate(new StreamSource(new StringReader(xmlMetadata)));
    Object parsed = SAMLParser.getInstance().parse(new StreamSource(new StringReader(xmlMetadata)));
    assertThat(parsed).isInstanceOf(EntityDescriptorType.class);
    EntityDescriptorType entityDescriptorType = (EntityDescriptorType) parsed;
    assertThat(entityDescriptorType.getEntityID()).isEqualTo(samlConfiguration.getEntityId());
    assertThat(entityDescriptorType.getChoiceType()).hasSize(1);
    assertThat(entityDescriptorType.getChoiceType().get(0).getDescriptors()).hasSize(1);
    SPSSODescriptorType spssoDescriptorType =
        entityDescriptorType.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();
    assertThat(spssoDescriptorType).isNotNull();
    assertThat(spssoDescriptorType.isAuthnRequestsSigned()).isTrue();
    assertThat(spssoDescriptorType.isWantAssertionsSigned()).isTrue();
    String expectedCertificatePem =
        Base64.getEncoder().encodeToString(samlConfigurationService.get().getCertificate().getEncoded());
    assertThat(spssoDescriptorType.getKeyDescriptor()).extracting(KeyDescriptorType::getUse)
        .containsExactlyInAnyOrder(KeyTypes.SIGNING, KeyTypes.ENCRYPTION);
    assertThat(spssoDescriptorType.getKeyDescriptor())
        .extracting(key -> key.getKeyInfo().getElementsByTagNameNS("*", "X509Certificate"))
        .allSatisfy(nodes -> {
          assertThat(nodes.getLength()).isEqualTo(1);
          assertThat(nodes.item(0).getTextContent()).isEqualTo(expectedCertificatePem);
        });
    String expectedUrl = getBaseUrl() + "saml";
    assertThat(spssoDescriptorType.getSingleLogoutService())
        .extracting(endpoint -> endpoint.getLocation().toString(), endpoint -> endpoint.getBinding().toString())
        .containsExactly(tuple(expectedUrl, "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"));
    assertThat(spssoDescriptorType.getNameIDFormat())
        .containsExactly("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
    assertThat(spssoDescriptorType.getAssertionConsumerService())
        .extracting(endpoint -> endpoint.getLocation().toString(), endpoint -> endpoint.getBinding().toString(),
            IndexedEndpointType::getIndex, IndexedEndpointType::isIsDefault)
        .containsExactly(tuple(expectedUrl, "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST", 1, true));
  }

  @Test
  public void testDelete_InvalidConfiguration() throws Exception {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        Statement statement = connection.createStatement())
    {
      statement.execute("INSERT INTO " + operationalDataStore.getDatabaseSchema() + ".saml_configuration " +
          "VALUES ('474878d8bfe44d2086ca8387e340692f', '{}', '', '');");
    }
    assertThatThrownBy(samlConfigurationService::get).hasMessageContaining("Could not load SAML keystore");
    apiSamlConfigurationService.deleteSamlConfiguration();
    assertThat(samlConfigurationService.get()).isNull();
    assertThat(samlDeploymentManager.get()).isNull();
    assertThat(logOutput).atErrorLevel().contains("Forcing delete of SAML configuration.");
  }

  private String validIdentityProviderXml() throws Exception {
    URL resource = ApiSamlConfigurationServiceTest.class.getResource(
        "/" + ApiSamlConfigurationServiceTest.class.getSimpleName() + "/identity-provider-metadata.xml");
    return FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
  }

  private String invalidIdentityProviderXml() throws Exception {
    URL resource = ApiSamlConfigurationServiceTest.class.getResource(
        "/" + ApiSamlConfigurationServiceTest.class.getSimpleName() + "/missing-entity-descriptor.xml");
    return FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
  }

  private String invalidCertificate() throws Exception {
    URL resource = ApiSamlConfigurationServiceTest.class.getResource(
        "/" + ApiSamlConfigurationServiceTest.class.getSimpleName() + "/invalid-certificate.xml");
    return FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
  }

  private ApiSamlConfigurationDTO dtoWithCustomValues() {
    ApiSamlConfigurationDTO dto = new ApiSamlConfigurationDTO();
    dto.identityProviderName = "My Awesome IdP";
    dto.entityId = "http://my-host:9090/config/saml/";
    dto.firstNameAttributeName = "first-name";
    dto.lastNameAttributeName = "last-name";
    dto.emailAttributeName = "e-mail";
    dto.usernameAttributeName = "user-name";
    dto.groupsAttributeName = "teamz";
    dto.validateResponseSignature = true;
    dto.validateAssertionSignature = false;
    return dto;
  }

  private void assertConfigIdentical(SamlConfiguration actual, ApiSamlConfigurationDTO expected) {
    assertThat(actual.getIdentityProviderName()).isEqualTo(expected.identityProviderName);
    assertThat(actual.getFirstNameAttributeName()).isEqualTo(expected.firstNameAttributeName);
    assertThat(actual.getLastNameAttributeName()).isEqualTo(expected.lastNameAttributeName);
    assertThat(actual.getEmailAttributeName()).isEqualTo(expected.emailAttributeName);
    assertThat(actual.getUsernameAttributeName()).isEqualTo(expected.usernameAttributeName);
    assertThat(actual.getGroupsAttributeName()).isEqualTo(expected.groupsAttributeName);
    assertThat(actual.getValidateResponseSignature()).isEqualTo(expected.validateResponseSignature);
    assertThat(actual.getValidateAssertionSignature()).isEqualTo(expected.validateAssertionSignature);
  }

  private void assertConfigIdentical(SamlConfiguration actual, SamlConfiguration expected) {
    assertThat(actual.getIdentityProviderName()).isEqualTo(expected.getIdentityProviderName());
    assertThat(actual.getFirstNameAttributeName()).isEqualTo(expected.getFirstNameAttributeName());
    assertThat(actual.getLastNameAttributeName()).isEqualTo(expected.getLastNameAttributeName());
    assertThat(actual.getEmailAttributeName()).isEqualTo(expected.getEmailAttributeName());
    assertThat(actual.getUsernameAttributeName()).isEqualTo(expected.getUsernameAttributeName());
    assertThat(actual.getGroupsAttributeName()).isEqualTo(expected.getGroupsAttributeName());
    assertThat(actual.getValidateResponseSignature()).isEqualTo(expected.getValidateResponseSignature());
    assertThat(actual.getValidateAssertionSignature()).isEqualTo(expected.getValidateAssertionSignature());
  }
}
