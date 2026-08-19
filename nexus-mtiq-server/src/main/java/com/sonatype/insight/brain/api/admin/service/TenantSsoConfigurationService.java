/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.api.v2.service.AbstractOidcConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiOidcConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.oauth2.OidcLoginFilter;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class TenantSsoConfigurationService
    extends AbstractOidcConfigurationService
{
  private static final Logger log = LoggerFactory.getLogger(TenantSsoConfigurationService.class);

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  private final SsoUserService ssoUserService;

  @Inject
  public TenantSsoConfigurationService(
      final PasswordHandler passwordHandler,
      final TenantUtil tenantUtil,
      final TenantValidator tenantValidator,
      final OAuth2ConfigurationDAO oAuth2ConfigurationDAO,
      final OidcConfigurationDAO oidcConfigurationDAO,
      final OidcLoginFilter oidcLoginFilter,
      final SsoUserService ssoUserService)
  {
    super(oAuth2ConfigurationDAO, passwordHandler, oidcConfigurationDAO, oidcLoginFilter);
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.ssoUserService = ssoUserService;
  }

  public void syncSsoProviderDataSources(String tenantSlug) {
    validateCurrentTenant(tenantSlug);

    ssoUserService.syncSsoProviderDataSources();

    // Ensuring the tenant reload the configuration for SSO
    ssoUserService.loadSsoConfiguration();
  }

  public void updateSsoConfiguration(SsoConfigurationDTO ssoConfigurationDTO, String tenantSlug) {
    validateCurrentTenant(tenantSlug);

    try {
      upsertSsoConfiguration(ssoConfigurationDTO);
    }
    catch (IllegalArgumentException e) {
      log.debug("Failed to update SSO configuration: {}", e.getMessage());
      throw new BadRequestException("Invalid OIDC configuration: " + e.getMessage(), e);
    }

    // Ensuring the tenant reload the configuration for SSO
    ssoUserService.loadSsoConfiguration();
  }

  public SsoConfigurationDTO getSsoConfiguration(String tenantSlug) {
    validateCurrentTenant(tenantSlug);

    OidcConfiguration oidcConfiguration = oidcConfigurationDAO.get();
    if (oidcConfiguration == null) {
      throw new NotFoundException("SSO configuration not set: OIDC configuration not found");
    }

    OAuth2Configuration oAuth2Configuration = oAuth2ConfigurationDAO.getById(oidcConfiguration.getId());
    if (oAuth2Configuration == null) {
      throw new NotFoundException("SSO configuration not set: OAuth2 configuration not found");
    }

    OidcConfigurationDTO oidcDto = OidcConfigurationDTO.toDTO(oidcConfiguration);
    OAuth2ConfigurationDTO oAuth2Dto = OAuth2ConfigurationDTO.toDTO(oAuth2Configuration);
    // clientSecret must never be returned in plaintext. Use the standard mask so that
    // a GET → PUT round-trip preserves the stored secret (buildOidcConfiguration checks for this value).
    oidcDto.setClientSecret(ApiOidcConfigurationService.CLIENT_SECRET_MASK);
    return new SsoConfigurationDTO(oAuth2Dto, oidcDto);
  }

  private void validateCurrentTenant(String tenantSlug) {
    if (tenantUtil.isGlobalTenant()) {
      throw new BadRequestException("Operation not supported for global tenant");
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.debug("Tenant {} doesn't exist", tenantSlug);
      throw new NotFoundException("Tenant " + tenantSlug + " doesn't exist");
    }
  }
}
