/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.oauth2.OidcLoginFilter;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ApiOidcConfigurationService
    extends AbstractOidcConfigurationService
{
  private static final Logger log = LoggerFactory.getLogger(ApiOidcConfigurationService.class);

  public static final String CLIENT_SECRET_MASK = "*******";

  private final SsoUserService ssoUserService;

  @Inject
  public ApiOidcConfigurationService(
      final PasswordHandler passwordHandler,
      final OAuth2ConfigurationDAO oAuth2ConfigurationDAO,
      final OidcConfigurationDAO oidcConfigurationDAO,
      final OidcLoginFilter oidcLoginFilter,
      final SsoUserService ssoUserService)
  {
    super(oAuth2ConfigurationDAO, passwordHandler, oidcConfigurationDAO, oidcLoginFilter);
    this.ssoUserService = ssoUserService;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public SsoConfigurationDTO getOidcConfiguration() {
    OidcConfiguration oidcConfiguration = oidcConfigurationDAO.get();
    if (oidcConfiguration == null) {
      throw new NotFoundException("Oidc configuration not set");
    }

    OAuth2Configuration oAuth2Configuration = oAuth2ConfigurationDAO.getById(oidcConfiguration.getId());
    if (oAuth2Configuration == null) {
      throw new NotFoundException("Oidc configuration not set");
    }

    OidcConfigurationDTO oidcDto = OidcConfigurationDTO.toDTO(oidcConfiguration);
    OAuth2ConfigurationDTO oAuth2Dto = OAuth2ConfigurationDTO.toDTO(oAuth2Configuration);
    // Mask the client secret - never send actual secret to frontend
    oidcDto.setClientSecret(CLIENT_SECRET_MASK);
    return new SsoConfigurationDTO(oAuth2Dto, oidcDto);
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void insertOrUpdateOidcConfiguration(final SsoConfigurationDTO ssoConfigurationDTO) {
    audit(ssoConfigurationDTO);
    try {
      validateIdpIssuerMatches(ssoConfigurationDTO);
      upsertOAuth2Configuration(ssoConfigurationDTO);
      upsertOidcConfiguration(ssoConfigurationDTO);
    }
    catch (IllegalArgumentException e) {
      log.debug("Failed to insert or update OIDC configuration: {}", e.getMessage());
      throw new BadRequestException("Invalid OIDC configuration: " + e.getMessage(), e);
    }

    // Ensuring the tenant reload the configuration for SSO
    ssoUserService.loadSsoConfiguration();
  }

  private void audit(final SsoConfigurationDTO ssoConfigurationDTO) {
    if (ssoConfigurationDTO == null) {
      return;
    }

    AuditData auditData = AuditData.get();
    if (ssoConfigurationDTO.getOAuth2Configuration() != null) {
      OAuth2ConfigurationDTO oauth2Config = ssoConfigurationDTO.getOAuth2Configuration();
      auditData.setData("oauth2IdpIssuer", oauth2Config.getIdpIssuer());
      auditData.setData("oauth2IdpJwksUrl", oauth2Config.getIdpJwksUrl());
      auditData.setData("oauth2IdpJwsAlgorithm", oauth2Config.getIdpJwsAlgorithm());
      auditData.setData("oauth2UsernameClaim", oauth2Config.getUsernameClaim());
      auditData.setData("oauth2EmailClaim", oauth2Config.getEmailClaim());
      auditData.setData("oauth2FirstNameClaim", oauth2Config.getFirstNameClaim());
      auditData.setData("oauth2LastNameClaim", oauth2Config.getLastNameClaim());
      auditData.setData("oauth2GroupsClaim", oauth2Config.getGroupsClaim());
    }

    if (ssoConfigurationDTO.getOidcConfiguration() != null) {
      OidcConfigurationDTO oidcConfig = ssoConfigurationDTO.getOidcConfiguration();
      auditData.setData("oidcIdpIssuer", oidcConfig.getIdpIssuer());
      auditData.setData("oidcClientId", oidcConfig.getClientId());
      auditData.setData("oidcClientSecret",
          StringUtils.isNotBlank(oidcConfig.getClientSecret()) ? CLIENT_SECRET_MASK : "");
      auditData.setData("oidcIdpAuthorizationUrl", oidcConfig.getIdpAuthorizationUrl());
      auditData.setData("oidcIdpTokenUrl", oidcConfig.getIdpTokenUrl());
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteOidcConfiguration() {
    OidcConfiguration oidcConfiguration = oidcConfigurationDAO.get();
    if (oidcConfiguration == null) {
      throw new NotFoundException("Oidc configuration not set");
    }
    OAuth2Configuration oAuth2Configuration = oAuth2ConfigurationDAO.getById(oidcConfiguration.getId());
    if (oAuth2Configuration != null) {
      // Delete OAuth2 configuration using the issuer from OIDC config
      oAuth2ConfigurationDAO.delete(oAuth2Configuration);
    }
    oidcConfigurationDAO.delete(oidcConfiguration);

    // Clear cached client secret
    clearCachedOidcClientSecret();

    // Reload SSO configuration to reflect deletion
    ssoUserService.loadSsoConfiguration();
  }

  private void validateIdpIssuerMatches(final SsoConfigurationDTO ssoConfigurationDTO) {
    if (ssoConfigurationDTO == null ||
        ssoConfigurationDTO.getOAuth2Configuration() == null ||
        ssoConfigurationDTO.getOidcConfiguration() == null)
    {
      log.debug("OAuth2 or OIDC configuration is null");
      throw new IllegalArgumentException("OAuth2 and OIDC configurations must be provided");
    }

    String oAuth2IdpIssuer = ssoConfigurationDTO.getOAuth2Configuration().getIdpIssuer();
    String oidcIdpIssuer = ssoConfigurationDTO.getOidcConfiguration().getIdpIssuer();
    if (StringUtils.isNoneBlank(oidcIdpIssuer, oAuth2IdpIssuer) && !oAuth2IdpIssuer.equals(oidcIdpIssuer)) {
      log.debug("OIDC IdP issuer '{}' does not match OAuth2 IdP issuer '{}'", oidcIdpIssuer, oAuth2IdpIssuer);
      throw new IllegalArgumentException("OIDC IdP issuer must match OAuth2 IdP issuer");
    }
  }
}
