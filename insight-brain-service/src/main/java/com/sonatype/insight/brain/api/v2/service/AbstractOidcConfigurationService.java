/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.oauth2.OidcLoginFilter;

public abstract class AbstractOidcConfigurationService
{
  protected final OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  protected final PasswordHandler passwordHandler;

  protected final OidcConfigurationDAO oidcConfigurationDAO;

  protected final OidcLoginFilter oidcLoginFilter;

  protected AbstractOidcConfigurationService(
      final OAuth2ConfigurationDAO oAuth2ConfigurationDAO,
      final PasswordHandler passwordHandler,
      final OidcConfigurationDAO oidcConfigurationDAO,
      final OidcLoginFilter oidcLoginFilter)
  {
    this.oAuth2ConfigurationDAO = oAuth2ConfigurationDAO;
    this.passwordHandler = passwordHandler;
    this.oidcConfigurationDAO = oidcConfigurationDAO;
    this.oidcLoginFilter = oidcLoginFilter;
  }

  protected void upsertOAuth2Configuration(final SsoConfigurationDTO ssoConfigurationDTO) {
    OAuth2ConfigurationDTO oAuth2ConfigurationDTO = ssoConfigurationDTO.getOAuth2Configuration();
    OAuth2Configuration oAuth2Configuration = oAuth2ConfigurationDAO.getById(oAuth2ConfigurationDTO.getIdpIssuer());

    if (oAuth2Configuration != null) {
      oAuth2ConfigurationDAO.update(OAuth2ConfigurationDTO.fromDTO(oAuth2ConfigurationDTO));
    }
    else {
      oAuth2ConfigurationDAO.insert(OAuth2ConfigurationDTO.fromDTO(oAuth2ConfigurationDTO));
    }
  }

  protected void upsertOidcConfiguration(final SsoConfigurationDTO ssoConfigurationDTO) {
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

  protected OidcConfiguration buildOidcConfiguration(OidcConfigurationDTO oidcConfigurationDTO) {
    OidcConfiguration oidcConfiguration = OidcConfigurationDTO.fromDTO(oidcConfigurationDTO);
    oidcConfiguration.setClientSecret(passwordHandler.encryptPassword(oidcConfiguration.getClientSecret()));
    return oidcConfiguration;
  }

  protected void clearCachedOidcClientSecret() {
    oidcLoginFilter.clearCachedOidcClientSecret();
  }
}
