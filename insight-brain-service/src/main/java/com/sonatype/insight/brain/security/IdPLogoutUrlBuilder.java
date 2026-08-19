/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.service.BaseUrl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;

/**
 * This class has the logic to build a proper logout URL to logout the user from the SAML IdP. That URL should be used
 * by the proper resources to trigger the actual logout
 */
@Named
public class IdPLogoutUrlBuilder
{
  private final SamlConfigurationService samlConfigurationService;

  private final SamlRelyingPartyRegistrationResolver relyingPartyRegistrationResolver;

  private final BaseUrl baseUrl;

  private final OidcConfigurationDAO oidcConfigurationDAO;

  private static final String AUTH0_LOGOUT_URL_FORMAT = "https://%s/v2/logout?client_id=%s&returnTo=%s";

  @Inject
  public IdPLogoutUrlBuilder(
      SamlConfigurationService samlConfigurationService,
      SamlRelyingPartyRegistrationResolver relyingPartyRegistrationResolver,
      BaseUrl baseUrl,
      OidcConfigurationDAO oidcConfigurationDAO)
  {
    this.samlConfigurationService = samlConfigurationService;
    this.relyingPartyRegistrationResolver = relyingPartyRegistrationResolver;
    this.baseUrl = baseUrl;
    this.oidcConfigurationDAO = oidcConfigurationDAO;
  }

  public URI buildIdPLogoutUrl() {
    if (SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.isEnabled()) {
      OidcConfiguration oidcConfiguration = oidcConfigurationDAO.get();

      if (SystemConfigurationPropertyFeature.OAUTH2_ENABLED.isEnabled() && oidcConfiguration != null &&
          StringUtils.isNotBlank(oidcConfiguration.getClientId()))
      {
        return buildAuth0LogoutURIFromOidcConfiguration(oidcConfiguration);
      }

      SamlConfiguration samlConfiguration = samlConfigurationService.get();
      if (samlConfiguration != null) {
        // Pass the full ACS location (${baseUrl}/saml), not the bare base URL: build() falls back to the ACS
        // location for the SP entityId when none is configured, so this keeps the SP entityId consistent with
        // every other call site (all of which use samlEndpointUrl()).
        RelyingPartyRegistration registration =
            relyingPartyRegistrationResolver.build(samlConfiguration, samlEndpointUrl());
        return buildAuth0LogoutURIFromSAMLConfiguration(registration);
      }
    }
    return null;
  }

  private String samlEndpointUrl() {
    return UriBuilder.fromUri(baseUrl.get()).path(SamlConstants.SAML_REQUEST_PATH).build().toString();
  }

  private URI buildAuth0LogoutURIFromOidcConfiguration(OidcConfiguration oidcConfiguration) {
    try {
      URL baseUri = new URL(oidcConfiguration.getId());
      String clientId = oidcConfiguration.getClientId();
      String returnToUrl = baseUrl.get();

      return URI.create(String.format(AUTH0_LOGOUT_URL_FORMAT, baseUri.getHost(), clientId, returnToUrl));
    }
    catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private URI buildAuth0LogoutURIFromSAMLConfiguration(RelyingPartyRegistration registration) {
    String singleLogoutServiceLocation = registration.getAssertingPartyMetadata().getSingleLogoutServiceLocation();
    if (singleLogoutServiceLocation == null) {
      // The IdP metadata has no SingleLogoutService endpoint, so there is no Auth0 logout URL to build.
      return null;
    }
    String baseUri = registration.getAssertingPartyMetadata().getEntityId().replace("urn:", "");
    String clientId = extractClientIdFromLogoutUrl(singleLogoutServiceLocation);
    String returnToUrl = baseUrl.get();

    return URI.create(String.format(AUTH0_LOGOUT_URL_FORMAT, baseUri, clientId, returnToUrl));
  }

  private String extractClientIdFromLogoutUrl(String logoutUrl) {
    String samlToken = "/samlp/";
    int clientIdPosition = logoutUrl.indexOf(samlToken) + samlToken.length();

    return logoutUrl.substring(clientIdPosition).replace("/logout", "");
  }
}
