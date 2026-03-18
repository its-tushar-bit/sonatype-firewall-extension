/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Collections;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.UriBuilder;
import javax.xml.stream.XMLStreamWriter;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationResponseDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.SamlDeploymentManager;
import com.sonatype.insight.brain.security.SamlMetadataTool;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLMetadataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * @since 1.72
 */
@Named
public class ApiSamlConfigurationService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSamlConfigurationService.class);

  private final SamlConfigurationService samlConfigurationService;

  private final BaseUrl baseUrl;

  private final SamlMetadataTool samlMetadataTool;

  private final SamlDeploymentManager samlDeploymentManager;

  @Inject
  public ApiSamlConfigurationService(
      SamlConfigurationService samlConfigurationService,
      BaseUrl baseUrl,
      SamlMetadataTool samlMetadataTool,
      SamlDeploymentManager samlDeploymentManager)
  {
    this.samlConfigurationService = samlConfigurationService;
    this.baseUrl = baseUrl;
    this.samlMetadataTool = samlMetadataTool;
    this.samlDeploymentManager = samlDeploymentManager;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public ApiSamlConfigurationResponseDTO getSamlConfiguration() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();

    if (samlConfiguration == null) {
      throw new NotFoundException("SAML not configured.");
    }

    return convertToResponseDTO(samlConfiguration);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void insertOrUpdateSamlConfiguration(
      String identityProviderXml,
      ApiSamlConfigurationDTO apiSamlConfigurationDTO)
  {
    insertOrUpdateSamlConfigurationNoAuthz(identityProviderXml, apiSamlConfigurationDTO);
  }

  public void insertOrUpdateSamlConfigurationNoAuthz(
      String identityProviderXml,
      ApiSamlConfigurationDTO apiSamlConfigurationDTO)
  {
    SamlConfiguration persisted = samlConfigurationService.get();
    boolean update = persisted != null;

    // Updating the existing configurations xml only, meaning keep my configuration
    if (update && apiSamlConfigurationDTO == null) {
      validateAndSetIdentityProviderXml(identityProviderXml, persisted);
      checkSamlDeploymentAndPersist(persisted);
      return;
    }

    // Get a fresh instance with default values
    SamlConfiguration samlConfiguration = new SamlConfiguration();
    samlConfiguration.setEntityId(defaultEntityId());

    // Null xml when updating means, keep my xml
    if (update && identityProviderXml == null) {
      samlConfiguration.setIdentityProviderMetadataXml(persisted.getIdentityProviderMetadataXml());
    }
    // xml is needed for insert, and it must be validated for both insert and update if provided
    else {
      validateAndSetIdentityProviderXml(identityProviderXml, samlConfiguration);
    }

    // apiSamlConfigurationDTO can be null for either insert or update
    // If not null, apply the provided values, otherwise proceed with the defaults
    if (apiSamlConfigurationDTO != null) {
      overrideDefaultsWithProvided(apiSamlConfigurationDTO, samlConfiguration);
    }

    if (update) {
      samlConfiguration.setId(persisted.getId());
    }
    checkSamlDeploymentAndPersist(samlConfiguration);
  }

  private void checkSamlDeploymentAndPersist(SamlConfiguration samlConfiguration) {
    try {
      samlDeploymentManager.parse(samlConfiguration);
    }
    catch (IllegalArgumentException e) {
      log.debug("Configuration could not be validated.", e);
      throw new BadRequestException("Configuration could not be validated: " + e.getMessage(), e);
    }

    if (samlConfiguration.getId() != null) {
      samlConfigurationService.update(samlConfiguration);
    }
    else {
      samlConfigurationService.insert(samlConfiguration);
    }

    samlDeploymentManager.updateAllClusterNodesFromConfiguration();
    audit(samlConfiguration);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteSamlConfiguration() {
    SamlConfiguration samlConfiguration;
    try {
      samlConfiguration = samlConfigurationService.get();
    }
    catch (Exception e) {
      log.error("Forcing delete of SAML configuration.", e);
      samlConfigurationService.delete();
      samlDeploymentManager.updateAllClusterNodesFromConfiguration();
      return;
    }

    if (samlConfiguration == null) {
      throw new NotFoundException("SAML not configured.");
    }

    samlConfigurationService.delete();
    samlDeploymentManager.updateAllClusterNodesFromConfiguration();
    audit(samlConfiguration);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public String getMetadata() {
    SamlDeployment samlDeployment = samlDeploymentManager.get();
    if (samlDeployment == null) {
      throw new NotFoundException("SAML not configured.");
    }
    URI samlEndpointUrl = UriBuilder.fromUri(baseUrl.get()).path("saml").build();
    try {
      String certificatePem =
          Base64.getEncoder().encodeToString(samlConfigurationService.get().getCertificate().getEncoded());
      Element keyElement = SPMetadataDescriptor.buildKeyInfoElement(null, certificatePem);
      KeyDescriptorType signingCert = SPMetadataDescriptor.buildKeyDescriptorType(keyElement, KeyTypes.SIGNING);
      KeyDescriptorType encryptionCert = SPMetadataDescriptor.buildKeyDescriptorType(keyElement, KeyTypes.ENCRYPTION);
      URI bindingUri = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri();
      EntityDescriptorType spDescriptor = SPMetadataDescriptor.buildSPDescriptor(bindingUri,
          bindingUri, samlEndpointUrl, samlEndpointUrl, samlDeployment.getIDP().getSingleSignOnService().signRequest(),
          samlDeployment.getIDP().getSingleSignOnService().validateAssertionSignature(), true,
          samlDeployment.getEntityID(), samlDeployment.getNameIDPolicyFormat(), //
          Collections.singletonList(signingCert), Collections.singletonList(encryptionCert));
      return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + spDescriptorAsString(spDescriptor);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String spDescriptorAsString(EntityDescriptorType spDescriptor) throws ProcessingException {
    StringWriter sw = new StringWriter();
    XMLStreamWriter writer = StaxUtil.getXMLStreamWriter(sw);
    SAMLMetadataWriter metadataWriter = new SAMLMetadataWriter(writer);
    metadataWriter.writeEntityDescriptor(spDescriptor);
    return sw.toString();
  }

  private ApiSamlConfigurationResponseDTO convertToResponseDTO(SamlConfiguration samlConfiguration) {
    ApiSamlConfigurationResponseDTO responseDTO = new ApiSamlConfigurationResponseDTO();
    responseDTO.identityProviderName = samlConfiguration.getIdentityProviderName();
    responseDTO.identityProviderMetadataXml = samlConfiguration.getIdentityProviderMetadataXml();
    responseDTO.entityId = samlConfiguration.getEntityId();
    responseDTO.firstNameAttributeName = samlConfiguration.getFirstNameAttributeName();
    responseDTO.lastNameAttributeName = samlConfiguration.getLastNameAttributeName();
    responseDTO.emailAttributeName = samlConfiguration.getEmailAttributeName();
    responseDTO.usernameAttributeName = samlConfiguration.getUsernameAttributeName();
    responseDTO.groupsAttributeName = samlConfiguration.getGroupsAttributeName();
    responseDTO.validateResponseSignature = samlConfiguration.getValidateResponseSignature();
    responseDTO.validateAssertionSignature = samlConfiguration.getValidateAssertionSignature();
    return responseDTO;
  }

  private void validateAndSetIdentityProviderXml(String identityProviderXml, SamlConfiguration samlConfiguration) {
    if (StringUtils.isBlank(identityProviderXml)) {
      throw new BadRequestException("Identity Provider XML is required.");
    }
    try {
      samlMetadataTool.parseEntityDescriptor(identityProviderXml);
      samlConfiguration.setIdentityProviderMetadataXml(identityProviderXml);
    }
    catch (Exception e) {
      throw new BadRequestException("Identity provider metadata could not be validated: " + e.getMessage(), e);
    }
  }

  private void overrideDefaultsWithProvided(
      ApiSamlConfigurationDTO apiSamlConfigurationDTO,
      SamlConfiguration samlConfiguration)
  {
    if (StringUtils.isNotBlank(apiSamlConfigurationDTO.identityProviderName)) {
      samlConfiguration.setIdentityProviderName(apiSamlConfigurationDTO.identityProviderName);
    }
    if (StringUtils.isNotBlank(apiSamlConfigurationDTO.entityId)) {
      validateAndSetEntityId(apiSamlConfigurationDTO.entityId, samlConfiguration);
    }
    if (StringUtils.isNotBlank(apiSamlConfigurationDTO.firstNameAttributeName)) {
      samlConfiguration.setFirstNameAttributeName(apiSamlConfigurationDTO.firstNameAttributeName);
    }
    if (StringUtils.isNotBlank(apiSamlConfigurationDTO.lastNameAttributeName)) {
      samlConfiguration.setLastNameAttributeName(apiSamlConfigurationDTO.lastNameAttributeName);
    }
    if (StringUtils.isNotBlank(apiSamlConfigurationDTO.emailAttributeName)) {
      samlConfiguration.setEmailAttributeName(apiSamlConfigurationDTO.emailAttributeName);
    }
    if (StringUtils.isNotBlank(apiSamlConfigurationDTO.usernameAttributeName)) {
      samlConfiguration.setUsernameAttributeName(apiSamlConfigurationDTO.usernameAttributeName);
    }
    if (StringUtils.isNotBlank(apiSamlConfigurationDTO.groupsAttributeName)) {
      samlConfiguration.setGroupsAttributeName(apiSamlConfigurationDTO.groupsAttributeName);
    }
    if (apiSamlConfigurationDTO.validateResponseSignature != null) {
      samlConfiguration.setValidateResponseSignature(apiSamlConfigurationDTO.validateResponseSignature);
    }
    if (apiSamlConfigurationDTO.validateAssertionSignature != null) {
      samlConfiguration.setValidateAssertionSignature(apiSamlConfigurationDTO.validateAssertionSignature);
    }
  }

  private void validateAndSetEntityId(String entityId, SamlConfiguration samlConfiguration) {
    try {
      new URI(entityId);
      samlConfiguration.setEntityId(entityId);
    }
    catch (URISyntaxException e) {
      throw new BadRequestException("Not a valid entity ID: " + e.getMessage(), e);
    }
  }

  private String defaultEntityId() {
    return UriBuilder.fromUri(this.baseUrl.get())
        .path(PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2)
        .path("metadata")
        .build()
        .toString();
  }

  private void audit(SamlConfiguration samlConfiguration) {
    String identityProviderMetadataXml = samlConfiguration.getIdentityProviderMetadataXml();
    String identityProviderEntityId = samlMetadataTool.parseEntityDescriptor(identityProviderMetadataXml).getEntityID();
    AuditData.get()
        .setData("identityProviderName", samlConfiguration.getIdentityProviderName())
        .setData("entityId", samlConfiguration.getEntityId())
        .setData("firstNameAttributeName", samlConfiguration.getFirstNameAttributeName())
        .setData("lastNameAttributeName", samlConfiguration.getLastNameAttributeName())
        .setData("userNameAttributeName", samlConfiguration.getUsernameAttributeName())
        .setData("emailAttributeName", samlConfiguration.getEmailAttributeName())
        .setData("groupsAttributeName", samlConfiguration.getGroupsAttributeName())
        .setData("validateResponseSignature", samlConfiguration.getValidateResponseSignature())
        .setData("validateAssertionSignature", samlConfiguration.getValidateAssertionSignature())
        .setData("identityProviderEntityId", identityProviderEntityId);
  }
}
