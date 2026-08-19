/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationResponseDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.SamlConfigurationCache;
import com.sonatype.insight.brain.security.SamlConstants;
import com.sonatype.insight.brain.security.SamlRelyingPartyRegistrationResolver;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml2.provider.service.metadata.OpenSaml5MetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;

/**
 * @since 1.72
 */
@Named
public class ApiSamlConfigurationService
{
  private static final Logger log = LoggerFactory.getLogger(ApiSamlConfigurationService.class);

  private final SamlConfigurationService samlConfigurationService;

  private final BaseUrl baseUrl;

  private final SamlRelyingPartyRegistrationResolver relyingPartyRegistrationResolver;

  private final SamlConfigurationCache samlConfigurationCache;

  private final OpenSaml5MetadataResolver metadataResolver = new OpenSaml5MetadataResolver();

  @Inject
  public ApiSamlConfigurationService(
      SamlConfigurationService samlConfigurationService,
      BaseUrl baseUrl,
      SamlRelyingPartyRegistrationResolver relyingPartyRegistrationResolver,
      SamlConfigurationCache samlConfigurationCache)
  {
    this.samlConfigurationService = samlConfigurationService;
    this.baseUrl = baseUrl;
    this.relyingPartyRegistrationResolver = relyingPartyRegistrationResolver;
    this.samlConfigurationCache = samlConfigurationCache;
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
      validateAndPersist(persisted);
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
    validateAndPersist(samlConfiguration);
  }

  private void validateAndPersist(SamlConfiguration samlConfiguration) {
    try {
      relyingPartyRegistrationResolver.build(samlConfiguration, samlEndpointUrl());
    }
    catch (RuntimeException e) {
      // Log at warn with the cause: build() can fail on bad admin input (returned as 400) but also on an
      // unexpected server error, which would otherwise be masked as a user "could not be validated" error.
      log.warn("SAML configuration could not be validated.", e);
      throw new BadRequestException("Configuration could not be validated: " + e.getMessage(), e);
    }

    if (samlConfiguration.getId() != null) {
      samlConfigurationService.update(samlConfiguration);
    }
    else {
      samlConfigurationService.insert(samlConfiguration);
    }

    audit(samlConfiguration);
    samlConfigurationCache.refreshAllClusterNodes();
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
      samlConfigurationCache.refreshAllClusterNodes();
      return;
    }

    if (samlConfiguration == null) {
      throw new NotFoundException("SAML not configured.");
    }

    samlConfigurationService.delete();
    audit(samlConfiguration);
    samlConfigurationCache.refreshAllClusterNodes();
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public String getMetadata() {
    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    if (samlConfiguration == null) {
      throw new NotFoundException("SAML not configured.");
    }
    RelyingPartyRegistration registration =
        relyingPartyRegistrationResolver.build(samlConfiguration, samlEndpointUrl());
    return metadataResolver.resolve(registration);
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
    samlConfiguration.setIdentityProviderMetadataXml(identityProviderXml);
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

  private String samlEndpointUrl() {
    return UriBuilder.fromUri(baseUrl.get()).path(SamlConstants.SAML_REQUEST_PATH).build().toString();
  }

  private void audit(SamlConfiguration samlConfiguration) {
    // Auditing reads only the IdP (asserting-party) entityId from the parsed metadata; the SP ACS location
    // is irrelevant here, so it must not depend on the (possibly unconfigured) base URL.
    RelyingPartyRegistration registration = relyingPartyRegistrationResolver
        .build(samlConfiguration, "https://localhost" + SamlConstants.SAML_REQUEST_PATH);
    String identityProviderEntityId = registration.getAssertingPartyMetadata().getEntityId();
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
