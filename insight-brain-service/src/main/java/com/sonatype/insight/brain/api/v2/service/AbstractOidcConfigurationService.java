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
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOidcConfigurationService
{
  private static final Logger log = LoggerFactory.getLogger(AbstractOidcConfigurationService.class);

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

  /**
   * Replaces the tenant's SSO configuration in a single transaction. {@code idp_issuer} is the primary
   * key of both tables, so a changed issuer is persisted as a delete of the old row plus an insert of
   * the new one; extra OAuth2 rows left by an earlier issuer change are swept, while the OIDC table is
   * single-row by construction. A masked OIDC client secret is resolved against the current row so the
   * stored secret is preserved.
   */
  protected void upsertSsoConfiguration(final SsoConfigurationDTO ssoConfigurationDTO) {
    validateSsoConfiguration(ssoConfigurationDTO);
    try (TransactionContext tx = oidcConfigurationDAO.createTransactionContext()) {
      tx.begin();
      replaceOAuth2Configuration(tx, ssoConfigurationDTO.getOAuth2Configuration());
      replaceOidcConfiguration(tx, ssoConfigurationDTO.getOidcConfiguration());
      tx.commit();
    }

    clearCachedOidcClientSecret();
  }

  private void validateSsoConfiguration(final SsoConfigurationDTO ssoConfigurationDTO) {
    if (ssoConfigurationDTO == null ||
        ssoConfigurationDTO.getOAuth2Configuration() == null ||
        ssoConfigurationDTO.getOidcConfiguration() == null)
    {
      log.debug("OAuth2 or OIDC configuration is null");
      throw new BadRequestException("OAuth2 and OIDC configurations must be provided");
    }

    String oAuth2IdpIssuer = ssoConfigurationDTO.getOAuth2Configuration().getIdpIssuer();
    String oidcIdpIssuer = ssoConfigurationDTO.getOidcConfiguration().getIdpIssuer();
    if (StringUtils.isNoneBlank(oidcIdpIssuer, oAuth2IdpIssuer) && !oAuth2IdpIssuer.equals(oidcIdpIssuer)) {
      log.debug("OIDC IdP issuer '{}' does not match OAuth2 IdP issuer '{}'", oidcIdpIssuer, oAuth2IdpIssuer);
      throw new BadRequestException("OIDC IdP issuer must match OAuth2 IdP issuer");
    }
  }

  private void replaceOAuth2Configuration(final TransactionContext tx, final OAuth2ConfigurationDTO dto) {
    OAuth2Configuration desired = OAuth2ConfigurationDTO.fromDTO(dto);
    boolean present = false;
    for (OAuth2Configuration existing : oAuth2ConfigurationDAO.getAll(tx)) {
      if (existing.getId().equals(desired.getId())) {
        present = true;
      }
      else {
        log.info("Removing SSO oauth2_configuration row with stale idpIssuer '{}'", existing.getId());
        oAuth2ConfigurationDAO.delete(tx, existing);
      }
    }
    if (present) {
      oAuth2ConfigurationDAO.update(tx, desired);
    }
    else {
      oAuth2ConfigurationDAO.insert(tx, desired);
    }
  }

  private void replaceOidcConfiguration(final TransactionContext tx, final OidcConfigurationDTO dto) {
    OidcConfiguration current = oidcConfigurationDAO.get(tx);
    OidcConfiguration desired = buildOidcConfiguration(dto, current);
    if (current == null) {
      oidcConfigurationDAO.insert(tx, desired);
    }
    else if (current.getId().equals(desired.getId())) {
      oidcConfigurationDAO.update(tx, desired);
    }
    else {
      log.info("SSO oidc_configuration idpIssuer changed '{}' -> '{}'; replacing row",
          current.getId(), desired.getId());
      oidcConfigurationDAO.delete(tx, current);
      oidcConfigurationDAO.insert(tx, desired);
    }
  }

  protected OidcConfiguration buildOidcConfiguration(
      OidcConfigurationDTO oidcConfigurationDTO,
      OidcConfiguration currentOidcConfiguration)
  {
    OidcConfiguration oidcConfiguration = OidcConfigurationDTO.fromDTO(oidcConfigurationDTO);

    // If client secret is the masked value, preserve the existing encrypted secret
    String clientSecret = oidcConfiguration.getClientSecret();
    if (ApiOidcConfigurationService.CLIENT_SECRET_MASK.equals(clientSecret)) {
      if (currentOidcConfiguration != null) {
        // Keep the existing encrypted secret
        oidcConfiguration.setClientSecret(currentOidcConfiguration.getClientSecret());
      }
      else {
        // This shouldn't happen - masked value sent when creating new configuration
        throw new BadRequestException("Client secret cannot be masked for new configuration");
      }
    }
    else {
      // New secret provided, encrypt it
      oidcConfiguration.setClientSecret(passwordHandler.encryptPassword(clientSecret));
    }

    return oidcConfiguration;
  }

  protected void clearCachedOidcClientSecret() {
    oidcLoginFilter.clearCachedOidcClientSecret();
  }
}
