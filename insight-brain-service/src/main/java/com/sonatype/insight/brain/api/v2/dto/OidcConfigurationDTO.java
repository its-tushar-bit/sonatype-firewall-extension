/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;

public class OidcConfigurationDTO
{
  private String idpIssuer;

  private String clientId;

  private String clientSecret;

  private String idpAuthorizationUrl;

  private String idpTokenUrl;

  private String authorizationCustomParamsJson;

  private String tokenRequestCustomParamsJson;

  public OidcConfigurationDTO() {
  }

  public OidcConfigurationDTO(
      final String idpIssuer,
      final String clientId,
      final String clientSecret,
      final String idpAuthorizationUrl,
      final String idpTokenUrl)
  {
    this.idpIssuer = idpIssuer;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.idpAuthorizationUrl = idpAuthorizationUrl;
    this.idpTokenUrl = idpTokenUrl;
  }

  public String getIdpIssuer() {
    return idpIssuer;
  }

  public void setIdpIssuer(final String idpIssuer) {
    this.idpIssuer = idpIssuer;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(final String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(final String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getIdpAuthorizationUrl() {
    return idpAuthorizationUrl;
  }

  public void setIdpAuthorizationUrl(final String idpAuthorizationUrl) {
    this.idpAuthorizationUrl = idpAuthorizationUrl;
  }

  public String getIdpTokenUrl() {
    return idpTokenUrl;
  }

  public void setIdpTokenUrl(final String idpTokenUrl) {
    this.idpTokenUrl = idpTokenUrl;
  }

  public String getAuthorizationCustomParamsJson() {
    return authorizationCustomParamsJson;
  }

  public void setAuthorizationCustomParamsJson(final String authorizationCustomParamsJson) {
    this.authorizationCustomParamsJson = authorizationCustomParamsJson;
  }

  public String getTokenRequestCustomParamsJson() {
    return tokenRequestCustomParamsJson;
  }

  public void setTokenRequestCustomParamsJson(final String tokenRequestCustomParamsJson) {
    this.tokenRequestCustomParamsJson = tokenRequestCustomParamsJson;
  }

  public static OidcConfiguration fromDTO(OidcConfigurationDTO oidcConfigurationDTO) {
    OidcConfiguration oidcConfiguration =
        new OidcConfiguration(oidcConfigurationDTO.idpIssuer, oidcConfigurationDTO.clientId,
            oidcConfigurationDTO.clientSecret, oidcConfigurationDTO.idpAuthorizationUrl,
            oidcConfigurationDTO.idpTokenUrl);
    oidcConfiguration.setAuthorizationCustomParamsJson(oidcConfigurationDTO.authorizationCustomParamsJson);
    oidcConfiguration.setTokenRequestCustomParamsJson(oidcConfigurationDTO.tokenRequestCustomParamsJson);
    return oidcConfiguration;
  }

  public static OidcConfigurationDTO toDTO(OidcConfiguration oidcConfiguration) {
    OidcConfigurationDTO dto = new OidcConfigurationDTO(
        oidcConfiguration.getId(),
        oidcConfiguration.getClientId(),
        oidcConfiguration.getClientSecret(),
        oidcConfiguration.getIdpAuthorizationUrl(),
        oidcConfiguration.getIdpTokenUrl());
    dto.setAuthorizationCustomParamsJson(oidcConfiguration.getAuthorizationCustomParamsJson());
    dto.setTokenRequestCustomParamsJson(oidcConfiguration.getTokenRequestCustomParamsJson());
    return dto;
  }
}
