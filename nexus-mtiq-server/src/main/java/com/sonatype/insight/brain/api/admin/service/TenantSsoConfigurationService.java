/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.admin.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.admin.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.admin.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
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
{
  private static final Logger log = LoggerFactory.getLogger(TenantSsoConfigurationService.class);

  private final PasswordHandler passwordHandler;

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  private final OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private final OidcConfigurationDAO oidcConfigurationDAO;

  private final OidcLoginFilter oidcLoginFilter;

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
    this.passwordHandler = passwordHandler;
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.oAuth2ConfigurationDAO = oAuth2ConfigurationDAO;
    this.oidcConfigurationDAO = oidcConfigurationDAO;
    this.oidcLoginFilter = oidcLoginFilter;
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

    upsertOAuth2Configuration(ssoConfigurationDTO);
    upsertOidcConfiguration(ssoConfigurationDTO);

    // Ensuring the tenant reload the configuration for SSO
    ssoUserService.loadSsoConfiguration();
  }

  private void validateCurrentTenant(String tenantSlug) {
    if (tenantUtil.isGlobalTenant()) {
      throw new BadRequestException("Invalid tenant");
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.debug("Tenant {} doesn't exist", tenantSlug);
      throw new NotFoundException("Tenant doesn't exist");
    }
  }

  private void upsertOAuth2Configuration(final SsoConfigurationDTO ssoConfigurationDTO) {
    OAuth2ConfigurationDTO oAuth2ConfigurationDTO = ssoConfigurationDTO.getOAuth2Configuration();
    OAuth2Configuration oAuth2Configuration = oAuth2ConfigurationDAO.getById(oAuth2ConfigurationDTO.getIdpIssuer());

    if (oAuth2Configuration != null) {
      oAuth2ConfigurationDAO.update(OAuth2ConfigurationDTO.fromDTO(oAuth2ConfigurationDTO));
    }
    else {
      oAuth2ConfigurationDAO.insert(OAuth2ConfigurationDTO.fromDTO(oAuth2ConfigurationDTO));
    }
  }

  private void upsertOidcConfiguration(final SsoConfigurationDTO ssoConfigurationDTO) {
    OidcConfiguration updatedOidcConfiguration = buildOidcConfiguration(ssoConfigurationDTO.getOidcConfiguration());
    OidcConfiguration currentOidcConfiguration = oidcConfigurationDAO.get();
    if (currentOidcConfiguration != null) {
      oidcConfigurationDAO.update(updatedOidcConfiguration);
    }
    else {
      oidcConfigurationDAO.insert(updatedOidcConfiguration);
    }

    clearCachedOidcClientSecret();
  }

  public void clearCachedOidcClientSecret() {
    oidcLoginFilter.clearCachedOidcClientSecret();
  }

  OidcConfiguration buildOidcConfiguration(OidcConfigurationDTO oidcConfigurationDTO) {
    OidcConfiguration oidcConfiguration = OidcConfigurationDTO.fromDTO(oidcConfigurationDTO);
    oidcConfiguration.setClientSecret(passwordHandler.encryptPassword(oidcConfiguration.getClientSecret()));
    return oidcConfiguration;
  }
}
