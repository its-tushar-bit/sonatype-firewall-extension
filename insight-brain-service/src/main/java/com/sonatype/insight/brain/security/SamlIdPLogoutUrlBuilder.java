/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.URI;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.BaseUrl;

import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeployment.IDP;
import org.keycloak.adapters.saml.SamlDeployment.IDP.SingleLogoutService;

/**
 * This class has the logic to build a proper logout URL to logout the user from the SAML IdP. That URL should be used
 * by the proper resources to trigger the actual logout
 */
@Named
class SamlIdPLogoutUrlBuilder
{
  private final SamlDeploymentManager samlDeploymentManager;

  private final BaseUrl baseUrl;

  private static final String AUTH0_LOGOUT_URL_FORMAT = "https://%s/v2/logout?client_id=%s&returnTo=%s";

  @Inject
  public SamlIdPLogoutUrlBuilder(
      SamlDeploymentManager samlDeploymentManager,
      BaseUrl baseUrl)
  {
    this.samlDeploymentManager = samlDeploymentManager;
    this.baseUrl = baseUrl;
  }

  public URI buildIdPLogoutUrl() {
    if (SystemConfigurationPropertyFeature.LOGOUT_AUTH0_ON_LOGOUT.isEnabled()) {
      SamlDeployment samlDeployment = samlDeploymentManager.get();
      if (samlDeployment != null) {
        return buildAuth0LogoutURi(samlDeployment);
      }
    }
    return null;
  }

  private URI buildAuth0LogoutURi(SamlDeployment samlDeployment) {
    IDP idp = samlDeployment.getIDP();
    String baseUri = idp.getEntityID().replaceAll("urn:", "");
    String clientId = extractClientIdFromLogoutUrl(idp);
    String returnToUrl = baseUrl.get();

    return URI.create(String.format(AUTH0_LOGOUT_URL_FORMAT, baseUri, clientId, returnToUrl));
  }

  private String extractClientIdFromLogoutUrl(IDP idp) {
    SingleLogoutService singleLogoutService = idp.getSingleLogoutService();
    String logoutUrl = singleLogoutService.getRequestBindingUrl();

    String samlToken = "/samlp/";
    int clientIdPosition = logoutUrl.indexOf(samlToken) + samlToken.length();

    return logoutUrl.substring(clientIdPosition).replaceAll("/logout", "");
  }
}
