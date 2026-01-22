/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiOidcConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiOidcConfigurationService apiOidcConfigurationService;

  @Test
  public void testGetOidcConfiguration_Authorized() {
    grantConfigureSystemPermission();

    // Create valid SSO configuration first
    SsoConfigurationDTO ssoConfig = mockValidConfiguration();

    apiOidcConfigurationService.insertOrUpdateOidcConfiguration(ssoConfig);

    // Now test get with proper authorization
    apiOidcConfigurationService.getOidcConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetOidcConfiguration_Unauthorized() {
    login();
    apiOidcConfigurationService.getOidcConfiguration();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetOidcConfiguration_Unauthenticated() {
    apiOidcConfigurationService.getOidcConfiguration();
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_Authorized() {
    grantConfigureSystemPermission();
    SsoConfigurationDTO ssoConfig = mockValidConfiguration();

    apiOidcConfigurationService.insertOrUpdateOidcConfiguration(ssoConfig);
  }

  private static SsoConfigurationDTO mockValidConfiguration() {
    // Create valid SSO configuration
    OAuth2ConfigurationDTO oauth2Config = new OAuth2ConfigurationDTO();
    oauth2Config.setIdpIssuer("https://auth.example.com");
    oauth2Config.setIdpJwksUrl("https://auth.example.com/.well-known/jwks.json");
    oauth2Config.setIdpJwsAlgorithm("RS256");

    OidcConfigurationDTO oidcConfig = new OidcConfigurationDTO();
    oidcConfig.setIdpIssuer("https://auth.example.com");
    oidcConfig.setClientId("test-client");
    oidcConfig.setClientSecret("test-secret");
    oidcConfig.setIdpAuthorizationUrl("https://auth.example.com/authorize");
    oidcConfig.setIdpTokenUrl("https://auth.example.com/token");

    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(oauth2Config);
    ssoConfig.setOidcConfiguration(oidcConfig);
    return ssoConfig;
  }

  @Test(expected = UnauthorizedException.class)
  public void testInsertOrUpdateOidcConfiguration_Unauthorized() {
    login();

    // Create valid SSO configuration
    SsoConfigurationDTO ssoConfig = mockValidConfiguration();

    apiOidcConfigurationService.insertOrUpdateOidcConfiguration(ssoConfig);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testInsertOrUpdateOidcConfiguration_Unauthenticated() {
    // Create valid SSO configuration
    SsoConfigurationDTO ssoConfig = mockValidConfiguration();

    apiOidcConfigurationService.insertOrUpdateOidcConfiguration(ssoConfig);
  }

  @Test
  public void testDeleteOidcConfiguration_Authorized() {
    grantConfigureSystemPermission();

    // Create valid SSO configuration
    OAuth2ConfigurationDTO oauth2Config = new OAuth2ConfigurationDTO();
    oauth2Config.setIdpIssuer("https://auth.example.com");
    oauth2Config.setIdpJwksUrl("https://auth.example.com/.well-known/jwks.json");
    oauth2Config.setIdpJwsAlgorithm("RS256");

    OidcConfigurationDTO oidcConfig = new OidcConfigurationDTO();
    oidcConfig.setIdpIssuer("https://auth.example.com");
    oidcConfig.setClientId("test-client");
    oidcConfig.setClientSecret("test-secret");
    oidcConfig.setIdpAuthorizationUrl("https://auth.example.com/authorize");
    oidcConfig.setIdpTokenUrl("https://auth.example.com/token");

    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(oauth2Config);
    ssoConfig.setOidcConfiguration(oidcConfig);

    apiOidcConfigurationService.insertOrUpdateOidcConfiguration(ssoConfig);

    apiOidcConfigurationService.deleteOidcConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteOidcConfiguration_Unauthorized() {
    login();

    apiOidcConfigurationService.deleteOidcConfiguration();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteOidcConfiguration_Unauthenticated() {
    apiOidcConfigurationService.deleteOidcConfiguration();
  }
}
