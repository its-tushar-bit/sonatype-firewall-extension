/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

public class SsoConfigurationDTO
{
  private OAuth2ConfigurationDTO oAuth2Configuration;

  private OidcConfigurationDTO oidcConfiguration;

  public SsoConfigurationDTO() {
  }

  public SsoConfigurationDTO(
      final OAuth2ConfigurationDTO oAuth2Configuration,
      final OidcConfigurationDTO oidcConfiguration)
  {
    this.oAuth2Configuration = oAuth2Configuration;
    this.oidcConfiguration = oidcConfiguration;
  }

  public OAuth2ConfigurationDTO getOAuth2Configuration() {
    return oAuth2Configuration;
  }

  public void setOAuth2Configuration(final OAuth2ConfigurationDTO oAuth2Configuration) {
    this.oAuth2Configuration = oAuth2Configuration;
  }

  public OidcConfigurationDTO getOidcConfiguration() {
    return oidcConfiguration;
  }

  public void setOidcConfiguration(final OidcConfigurationDTO oidcConfiguration) {
    this.oidcConfiguration = oidcConfiguration;
  }
}
