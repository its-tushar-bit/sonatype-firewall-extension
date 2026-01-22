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

import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.service.BaseUrl;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeployment.IDP;
import org.keycloak.adapters.saml.SamlDeployment.IDP.SingleLogoutService;

/**
 * This class has the logic to build a proper logout URL to logout the user from the SAML IdP. That URL should be used
 * by the proper resources to trigger the actual logout
 */
@Named
public class IdPLogoutUrlBuilder
{
  private final SamlDeploymentManager samlDeploymentManager;

  private final BaseUrl baseUrl;

  private final OidcConfigurationDAO oidcConfigurationDAO;

  private static final String AUTH0_LOGOUT_URL_FORMAT = "https://%s/v2/logout?client_id=%s&returnTo=%s";

  @Inject
  public IdPLogoutUrlBuilder(
      SamlDeploymentManager samlDeploymentManager,
      BaseUrl baseUrl,
      OidcConfigurationDAO oidcConfigurationDAO)
  {
    this.samlDeploymentManager = samlDeploymentManager;
    this.baseUrl = baseUrl;
    this.oidcConfigurationDAO = oidcConfigurationDAO;
  }

  public URI buildIdPLogoutUrl() {
    if (SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.isEnabled()) {
      OidcConfiguration oidcConfiguration = oidcConfigurationDAO.get();

      if (SystemConfigurationPropertyFeature.OAUTH2_ENABLED.isEnabled() && oidcConfiguration != null &&
          StringUtils.isNotBlank(oidcConfiguration.getClientId())) {
        return buildAuth0LogoutURIFromOidcConfiguration(oidcConfiguration);
      }

      SamlDeployment samlDeployment = samlDeploymentManager.get();
      if (samlDeployment != null) {
        return buildAuth0LogoutURIFromSAMLConfiguration(samlDeployment);
      }
    }
    return null;
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

  private URI buildAuth0LogoutURIFromSAMLConfiguration(SamlDeployment samlDeployment) {
    IDP idp = samlDeployment.getIDP();
    String baseUri = idp.getEntityID().replace("urn:", "");
    String clientId = extractClientIdFromLogoutUrl(idp);
    String returnToUrl = baseUrl.get();

    return URI.create(String.format(AUTH0_LOGOUT_URL_FORMAT, baseUri, clientId, returnToUrl));
  }

  private String extractClientIdFromLogoutUrl(IDP idp) {
    SingleLogoutService singleLogoutService = idp.getSingleLogoutService();
    String logoutUrl = singleLogoutService.getRequestBindingUrl();

    String samlToken = "/samlp/";
    int clientIdPosition = logoutUrl.indexOf(samlToken) + samlToken.length();

    return logoutUrl.substring(clientIdPosition).replace("/logout", "");
  }
}
