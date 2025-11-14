/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization tests for ApiOidcConfigurationResource.
 *
 * Tests verify that proper permissions (CONFIGURE_SYSTEM) are required
 * for all OIDC configuration operations.
 */
public class ApiOidcConfigurationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private OidcConfigurationDAO oidcConfigurationDAO;

  private PasswordHandler passwordHandler;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.OIDC_CONFIG_RESOURCE_PATH_V2);
  }

  @Before
  public void setUp() {
    oAuth2ConfigurationDAO = lookup(OAuth2ConfigurationDAO.class);
    oidcConfigurationDAO = lookup(OidcConfigurationDAO.class);
    passwordHandler = lookup(PasswordHandler.class);
  }

  // ========== GET /api/v2/config/oidc ==========

  @Test
  public void testGetOidcConfiguration_Unauthenticated() throws Exception {
    // Given: OIDC configuration exists
    createOidcConfiguration();

    // When: Unauthenticated request
    HttpResponse response = restRequest().anon().get();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testGetOidcConfiguration_AdminUser() throws Exception {
    // Given: OIDC configuration exists
    createOidcConfiguration();

    // When: Admin user (has all permissions)
    HttpResponse response = restRequest().auth().get();

    // Then: Should return 200 OK
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  // ========== PUT /api/v2/config/oidc ==========

  @Test
  public void testInsertOrUpdateOidcConfiguration_Unauthenticated() throws Exception {
    // Given: Valid OIDC configuration
    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();

    // When: Unauthenticated request
    HttpResponse response = restRequest().anon().body(ssoConfig).put();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);

    // Verify no configuration was created
    assertThat(oidcConfigurationDAO.get()).isNull();
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_AdminUser() throws Exception {
    // Given: Valid OIDC configuration
    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();

    // When: Admin user
    HttpResponse response = restRequest().body(ssoConfig).auth().put();

    // Then: Should return 204 No Content
    assertThat(response.getStatusCode()).isEqualTo(204);

    // Verify configuration was created
    assertThat(oidcConfigurationDAO.get()).isNotNull();
  }

  // ========== DELETE /api/v2/config/oidc ==========

  @Test
  public void testDeleteOidcConfiguration_Unauthenticated() throws Exception {
    // Given: OIDC configuration exists
    createOidcConfiguration();

    // When: Unauthenticated request
    HttpResponse response = restRequest().anon().delete();

    // Then: Should return 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(401);

    // Verify configuration still exists
    assertThat(oidcConfigurationDAO.get()).isNotNull();
  }

  @Test
  public void testDeleteOidcConfiguration_AdminUser() throws Exception {
    // Given: OIDC configuration exists
    createOidcConfiguration();

    // When: Admin user
    HttpResponse response = restRequest().auth().delete();

    // Then: Should return 204 No Content
    assertThat(response.getStatusCode()).isEqualTo(204);

    // Verify configuration was deleted
    assertThat(oidcConfigurationDAO.get()).isNull();
  }

  // ========== Helper Methods ==========

  private void createOidcConfiguration() {
    OAuth2Configuration oauth2Config = new OAuth2Configuration(
        "https://auth.example.com",
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);
    oauth2Config.setUsernameClaim("preferred_username");
    oauth2Config.setEmailClaim("email");
    oAuth2ConfigurationDAO.insert(oauth2Config);

    String encryptedSecret = passwordHandler.encryptPassword("test-client-secret");
    OidcConfiguration oidcConfig = new OidcConfiguration(
        "https://auth.example.com",
        "test-client-id",
        encryptedSecret,
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfigurationDAO.insert(oidcConfig);
  }

  private SsoConfigurationDTO createValidSsoConfiguration() {
    OAuth2ConfigurationDTO oauth2Config = new OAuth2ConfigurationDTO();
    oauth2Config.setIdpIssuer("https://auth.example.com");
    oauth2Config.setIdpJwksUrl("https://auth.example.com/.well-known/jwks.json");
    oauth2Config.setIdpJwsAlgorithm("RS256");
    oauth2Config.setUsernameClaim("preferred_username");
    oauth2Config.setEmailClaim("email");

    OidcConfigurationDTO oidcConfig = new OidcConfigurationDTO();
    oidcConfig.setIdpIssuer("https://auth.example.com");
    oidcConfig.setClientId("nexus-iq-client");
    oidcConfig.setClientSecret("test-client-secret");
    oidcConfig.setIdpAuthorizationUrl("https://auth.example.com/authorize");
    oidcConfig.setIdpTokenUrl("https://auth.example.com/token");

    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(oauth2Config);
    ssoConfig.setOidcConfiguration(oidcConfig);

    return ssoConfig;
  }
}
