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

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  private final OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private final OidcConfigurationDAO oidcConfigurationDAO;

  @Inject
  public TenantSsoConfigurationService(
      TenantUtil tenantUtil,
      TenantValidator tenantValidator,
      OAuth2ConfigurationDAO oAuth2ConfigurationDAO,
      OidcConfigurationDAO oidcConfigurationDAO)
  {
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.oAuth2ConfigurationDAO = oAuth2ConfigurationDAO;
    this.oidcConfigurationDAO = oidcConfigurationDAO;
  }

  public void updateSsoConfiguration(SsoConfigurationDTO ssoConfigurationDTO, String tenantSlug) {
    validateCurrentTenant(tenantSlug);

    upsertOAuth2Configuration(ssoConfigurationDTO);
    upsertOidcConfiguration(ssoConfigurationDTO);
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
    OidcConfigurationDTO oidcConfigurationDTO = ssoConfigurationDTO.getOidcConfiguration();
    OidcConfiguration oidcConfiguration = oidcConfigurationDAO.get();
    if (oidcConfiguration != null) {
      oidcConfigurationDAO.update(OidcConfigurationDTO.fromDTO(oidcConfigurationDTO));
    }
    else {
      oidcConfigurationDAO.insert(OidcConfigurationDTO.fromDTO(oidcConfigurationDTO));
    }
  }
}
