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
import com.sonatype.insight.brain.api.v2.service.ApiOidcConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
public class ApiOidcConfigurationResourceTest
{
  private IqTestContext ctx;

  private OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private OidcConfigurationDAO oidcConfigurationDAO;

  private PasswordHandler passwordHandler;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.OIDC_CONFIG_RESOURCE_PATH_V2);
  }

  @BeforeEach
  public void setUp() {
    oAuth2ConfigurationDAO = ctx.lookup(OAuth2ConfigurationDAO.class);
    oidcConfigurationDAO = ctx.lookup(OidcConfigurationDAO.class);
    passwordHandler = ctx.lookup(PasswordHandler.class);
  }

  @Test
  public void testGetOidcConfiguration_NotConfigured() throws Exception {
    // Given: No configuration exists
    assertThat(oidcConfigurationDAO.get()).isNull();

    // When: Get configuration
    HttpResponse response = restRequest().get();

    // Then: Should return 404 Not Found
    ctx.assertResponseStatus(404, response);
  }

  @Test
  public void testGetOidcConfiguration_Configured() throws Exception {
    // Given: Both OAuth2 and OIDC configurations exist
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
        "nexus-iq-client",
        encryptedSecret,
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfigurationDAO.insert(oidcConfig);

    // When: Get configuration
    HttpResponse response = restRequest().get();

    // Then: Should return 200 OK with configuration
    ctx.assertResponseStatus(200, response);

    SsoConfigurationDTO result = response.getBody(SsoConfigurationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.getOAuth2Configuration()).isNotNull();
    assertThat(result.getOidcConfiguration()).isNotNull();

    // Verify OAuth2 configuration
    OAuth2ConfigurationDTO oauth2Dto = result.getOAuth2Configuration();
    assertThat(oauth2Dto.getIdpIssuer()).isEqualTo("https://auth.example.com");
    assertThat(oauth2Dto.getIdpJwksUrl()).isEqualTo("https://auth.example.com/.well-known/jwks.json");
    assertThat(oauth2Dto.getIdpJwsAlgorithm()).isEqualTo("RS256");
    assertThat(oauth2Dto.getUsernameClaim()).isEqualTo("preferred_username");
    assertThat(oauth2Dto.getEmailClaim()).isEqualTo("email");

    // Verify OIDC configuration
    OidcConfigurationDTO oidcDto = result.getOidcConfiguration();
    assertThat(oidcDto.getIdpIssuer()).isEqualTo("https://auth.example.com");
    assertThat(oidcDto.getClientId()).isEqualTo("nexus-iq-client");
    assertThat(oidcDto.getIdpAuthorizationUrl()).isEqualTo("https://auth.example.com/authorize");
    assertThat(oidcDto.getIdpTokenUrl()).isEqualTo("https://auth.example.com/token");

    // Verify client secret is decrypted
    assertThat(oidcDto.getClientSecret()).isEqualTo(ApiOidcConfigurationService.CLIENT_SECRET_MASK);
  }

  @Test
  public void testGetOidcConfiguration_WithAllOptionalFields() throws Exception {
    // Given: Configuration with all optional fields
    OAuth2Configuration oauth2Config = new OAuth2Configuration(
        "https://auth.example.com",
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);
    oauth2Config.setUsernameClaim("sub");
    oauth2Config.setFirstNameClaim("given_name");
    oauth2Config.setLastNameClaim("family_name");
    oauth2Config.setEmailClaim("email");
    oauth2Config.setGroupsClaim("groups");
    oauth2Config.setExactMatchClaimsJson("{\"role\": \"admin\"}");
    oAuth2ConfigurationDAO.insert(oauth2Config);

    String encryptedSecret = passwordHandler.encryptPassword("secret");
    OidcConfiguration oidcConfig = new OidcConfiguration(
        "https://auth.example.com",
        "client-id",
        encryptedSecret,
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfig.setAuthorizationCustomParamsJson("{\"prompt\": \"consent\"}");
    oidcConfig.setTokenRequestCustomParamsJson("{\"resource\": \"api\"}");
    oidcConfigurationDAO.insert(oidcConfig);

    // When: Get configuration
    HttpResponse response = restRequest().get();

    // Then: All optional fields should be present
    ctx.assertResponseStatus(200, response);

    SsoConfigurationDTO result = response.getBody(SsoConfigurationDTO.class);
    OAuth2ConfigurationDTO oauth2Dto = result.getOAuth2Configuration();
    assertThat(oauth2Dto.getUsernameClaim()).isEqualTo("sub");
    assertThat(oauth2Dto.getFirstNameClaim()).isEqualTo("given_name");
    assertThat(oauth2Dto.getLastNameClaim()).isEqualTo("family_name");
    assertThat(oauth2Dto.getEmailClaim()).isEqualTo("email");
    assertThat(oauth2Dto.getGroupsClaim()).isEqualTo("groups");
    assertThat(oauth2Dto.getExactMatchClaimsJson()).isEqualTo("{\"role\": \"admin\"}");

    OidcConfigurationDTO oidcDto = result.getOidcConfiguration();
    assertThat(oidcDto.getAuthorizationCustomParamsJson()).isEqualTo("{\"prompt\": \"consent\"}");
    assertThat(oidcDto.getTokenRequestCustomParamsJson()).isEqualTo("{\"resource\": \"api\"}");
  }

  @Test
  public void testGetOidcConfiguration_OidcExistsButOAuth2Missing() throws Exception {
    // Given: OIDC exists but OAuth2 does not
    String encryptedSecret = passwordHandler.encryptPassword("secret");
    OidcConfiguration oidcConfig = new OidcConfiguration(
        "https://auth.example.com",
        "client-id",
        encryptedSecret,
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfigurationDAO.insert(oidcConfig);

    assertThat(oidcConfigurationDAO.get()).isNotNull();
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();

    // When: Get configuration
    HttpResponse response = restRequest().get();

    // Then: Should return 404 because both configs must exist
    ctx.assertResponseStatus(404, response);
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_Insert() throws Exception {
    // Given: No existing configuration
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();

    // When: Insert new OIDC configuration
    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();
    HttpResponse response = restRequest().body(ssoConfig).put();

    // Then: Configuration should be inserted successfully
    ctx.assertResponseStatus(204, response);

    // Verify OAuth2 configuration was persisted
    OAuth2Configuration persistedOAuth2 = oAuth2ConfigurationDAO.getById("https://auth.example.com");
    assertThat(persistedOAuth2).isNotNull();
    assertThat(persistedOAuth2.getId()).isEqualTo("https://auth.example.com");
    assertThat(persistedOAuth2.getIdpJwksUrl()).isEqualTo("https://auth.example.com/.well-known/jwks.json");
    assertThat(persistedOAuth2.getIdpJwsAlgorithm()).isEqualTo("RS256");
    assertThat(persistedOAuth2.getUsernameClaim()).isEqualTo("preferred_username");
    assertThat(persistedOAuth2.getEmailClaim()).isEqualTo("email");

    // Verify OIDC configuration was persisted
    OidcConfiguration persistedOidc = oidcConfigurationDAO.get();
    assertThat(persistedOidc).isNotNull();
    assertThat(persistedOidc.getId()).isEqualTo("https://auth.example.com");
    assertThat(persistedOidc.getClientId()).isEqualTo("nexus-iq-client");
    assertThat(persistedOidc.getIdpAuthorizationUrl()).isEqualTo("https://auth.example.com/authorize");
    assertThat(persistedOidc.getIdpTokenUrl()).isEqualTo("https://auth.example.com/token");

    // Verify client secret was encrypted
    String decryptedSecret = passwordHandler.decryptPassword(persistedOidc.getClientSecret());
    assertThat(decryptedSecret).isEqualTo("test-client-secret");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_Update() throws Exception {
    // Given: Existing configuration
    OAuth2Configuration existingOAuth2 = new OAuth2Configuration(
        "https://auth.example.com",
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);
    existingOAuth2.setUsernameClaim("old_username");
    oAuth2ConfigurationDAO.insert(existingOAuth2);

    OidcConfiguration existingOidc = new OidcConfiguration(
        "https://auth.example.com",
        "old-client-id",
        passwordHandler.encryptPassword("old-secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfigurationDAO.insert(existingOidc);

    // When: Update OIDC configuration
    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();
    HttpResponse response = restRequest().body(ssoConfig).put();

    // Then: Configuration should be updated successfully
    ctx.assertResponseStatus(204, response);

    // Verify OAuth2 configuration was updated
    OAuth2Configuration updatedOAuth2 = oAuth2ConfigurationDAO.getById("https://auth.example.com");
    assertThat(updatedOAuth2).isNotNull();
    assertThat(updatedOAuth2.getUsernameClaim()).isEqualTo("preferred_username");

    // Verify OIDC configuration was updated
    OidcConfiguration updatedOidc = oidcConfigurationDAO.get();
    assertThat(updatedOidc).isNotNull();
    assertThat(updatedOidc.getClientId()).isEqualTo("nexus-iq-client");

    // Verify new client secret was encrypted
    String decryptedSecret = passwordHandler.decryptPassword(updatedOidc.getClientSecret());
    assertThat(decryptedSecret).isEqualTo("test-client-secret");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_WithAllOptionalFields() throws Exception {
    // Given: SSO configuration with all optional fields
    OAuth2ConfigurationDTO oauth2Config = new OAuth2ConfigurationDTO();
    oauth2Config.setIdpIssuer("https://auth.example.com");
    oauth2Config.setIdpJwksUrl("https://auth.example.com/.well-known/jwks.json");
    oauth2Config.setIdpJwsAlgorithm("RS256");
    oauth2Config.setUsernameClaim("sub");
    oauth2Config.setFirstNameClaim("given_name");
    oauth2Config.setLastNameClaim("family_name");
    oauth2Config.setEmailClaim("email");
    oauth2Config.setGroupsClaim("groups");
    oauth2Config.setExactMatchClaimsJson("{\"role\": \"admin\"}");

    OidcConfigurationDTO oidcConfig = new OidcConfigurationDTO();
    oidcConfig.setIdpIssuer("https://auth.example.com");
    oidcConfig.setClientId("nexus-iq-client");
    oidcConfig.setClientSecret("test-client-secret");
    oidcConfig.setIdpAuthorizationUrl("https://auth.example.com/authorize");
    oidcConfig.setIdpTokenUrl("https://auth.example.com/token");
    oidcConfig.setAuthorizationCustomParamsJson("{\"prompt\": \"consent\"}");
    oidcConfig.setTokenRequestCustomParamsJson("{\"resource\": \"api\"}");

    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(oauth2Config);
    ssoConfig.setOidcConfiguration(oidcConfig);

    // When: Insert configuration with all optional fields
    HttpResponse response = restRequest().body(ssoConfig).put();

    // Then: All fields should be persisted
    ctx.assertResponseStatus(204, response);

    // Verify OAuth2 optional fields
    OAuth2Configuration persistedOAuth2 = oAuth2ConfigurationDAO.getById("https://auth.example.com");
    assertThat(persistedOAuth2.getUsernameClaim()).isEqualTo("sub");
    assertThat(persistedOAuth2.getFirstNameClaim()).isEqualTo("given_name");
    assertThat(persistedOAuth2.getLastNameClaim()).isEqualTo("family_name");
    assertThat(persistedOAuth2.getEmailClaim()).isEqualTo("email");
    assertThat(persistedOAuth2.getGroupsClaim()).isEqualTo("groups");
    assertThat(persistedOAuth2.getExactMatchClaimsJson()).isEqualTo("{\"role\": \"admin\"}");

    // Verify OIDC optional fields
    OidcConfiguration persistedOidc = oidcConfigurationDAO.get();
    assertThat(persistedOidc.getAuthorizationCustomParamsJson()).isEqualTo("{\"prompt\": \"consent\"}");
    assertThat(persistedOidc.getTokenRequestCustomParamsJson()).isEqualTo("{\"resource\": \"api\"}");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_InvalidIdpIssuerMismatch() throws Exception {
    // Given: OAuth2 and OIDC have different IdP issuers
    OAuth2ConfigurationDTO oauth2Config = new OAuth2ConfigurationDTO();
    oauth2Config.setIdpIssuer("https://auth.example.com");
    oauth2Config.setIdpJwksUrl("https://auth.example.com/.well-known/jwks.json");
    oauth2Config.setIdpJwsAlgorithm("RS256");

    OidcConfigurationDTO oidcConfig = new OidcConfigurationDTO();
    oidcConfig.setIdpIssuer("https://different-auth.example.com"); // Different issuer
    oidcConfig.setClientId("nexus-iq-client");
    oidcConfig.setClientSecret("test-client-secret");
    oidcConfig.setIdpAuthorizationUrl("https://auth.example.com/authorize");
    oidcConfig.setIdpTokenUrl("https://auth.example.com/token");

    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(oauth2Config);
    ssoConfig.setOidcConfiguration(oidcConfig);

    // When: Attempt to insert configuration with mismatched issuers
    HttpResponse response = restRequest().body(ssoConfig).put();

    // Then: Should return 400 Bad Request
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("OIDC IdP issuer must match OAuth2 IdP issuer");

    // Verify no configuration was persisted
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oAuth2ConfigurationDAO.getById("https://different-auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_OidcMissingMandatoryFields() throws Exception {
    // Given: OAuth2 and OIDC have different IdP issuers
    OAuth2ConfigurationDTO oauth2Config = new OAuth2ConfigurationDTO();
    oauth2Config.setIdpIssuer("https://auth.example.com");
    oauth2Config.setIdpJwksUrl("https://auth.example.com/.well-known/jwks.json");
    oauth2Config.setIdpJwsAlgorithm("RS256");

    OidcConfigurationDTO oidcConfig = new OidcConfigurationDTO();
    oidcConfig.setIdpIssuer("https://auth.example.com"); // Different issuer
    oidcConfig.setClientId(null);
    oidcConfig.setClientSecret("test-client-secret");
    oidcConfig.setIdpAuthorizationUrl("https://auth.example.com/authorize");
    oidcConfig.setIdpTokenUrl("https://auth.example.com/token");

    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(oauth2Config);
    ssoConfig.setOidcConfiguration(oidcConfig);

    // When: Attempt to insert configuration with mismatched issuers
    HttpResponse response = restRequest().body(ssoConfig).put();

    // Then: Should return 400 Bad Request
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Invalid OIDC configuration: The client id is required");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_OAuth2MissingMandatoryFields() throws Exception {
    // Given: OAuth2 and OIDC have different IdP issuers
    OAuth2ConfigurationDTO oauth2Config = new OAuth2ConfigurationDTO();
    oauth2Config.setIdpIssuer(null);
    oauth2Config.setIdpJwksUrl("https://auth.example.com/.well-known/jwks.json");
    oauth2Config.setIdpJwsAlgorithm("RS256");

    OidcConfigurationDTO oidcConfig = new OidcConfigurationDTO();
    oidcConfig.setIdpIssuer("https://auth.example.com"); // Different issuer
    oidcConfig.setClientId("nexus-iq-client");
    oidcConfig.setClientSecret("test-client-secret");
    oidcConfig.setIdpAuthorizationUrl("https://auth.example.com/authorize");
    oidcConfig.setIdpTokenUrl("https://auth.example.com/token");

    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(oauth2Config);
    ssoConfig.setOidcConfiguration(oidcConfig);

    // When: Attempt to insert configuration with mismatched issuers
    HttpResponse response = restRequest().body(ssoConfig).put();

    // Then: Should return 400 Bad Request
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Invalid OIDC configuration: The IDP Issuer is required");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_NullConfiguration() throws Exception {
    // When: Attempt to insert null configuration
    HttpResponse response = restRequest().body(null).put();

    // Then: Should return 400 Bad Request
    ctx.assertResponseStatus(400, response);
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_MixedScenario_OAuth2ExistsOidcNotExists() throws Exception {
    // Given: OAuth2 exists but OIDC does not
    OAuth2Configuration existingOAuth2 = new OAuth2Configuration(
        "https://auth.example.com",
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);
    oAuth2ConfigurationDAO.insert(existingOAuth2);

    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNotNull();
    assertThat(oidcConfigurationDAO.get()).isNull();

    // When: Insert/update configuration
    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();
    HttpResponse response = restRequest().body(ssoConfig).put();

    // Then: OAuth2 should be updated, OIDC should be inserted
    ctx.assertResponseStatus(204, response);

    OAuth2Configuration updatedOAuth2 = oAuth2ConfigurationDAO.getById("https://auth.example.com");
    assertThat(updatedOAuth2).isNotNull();

    OidcConfiguration insertedOidc = oidcConfigurationDAO.get();
    assertThat(insertedOidc).isNotNull();
    assertThat(insertedOidc.getClientId()).isEqualTo("nexus-iq-client");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_MixedScenario_OAuth2NotExistsOidcExists() throws Exception {
    // Given: OIDC exists but OAuth2 does not
    OidcConfiguration existingOidc = new OidcConfiguration(
        "https://auth.example.com",
        "old-client-id",
        passwordHandler.encryptPassword("old-secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfigurationDAO.insert(existingOidc);

    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNotNull();

    // When: Insert/update configuration
    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();
    HttpResponse response = restRequest().body(ssoConfig).put();

    // Then: OAuth2 should be inserted, OIDC should be updated
    ctx.assertResponseStatus(204, response);

    OAuth2Configuration insertedOAuth2 = oAuth2ConfigurationDAO.getById("https://auth.example.com");
    assertThat(insertedOAuth2).isNotNull();
    assertThat(insertedOAuth2.getUsernameClaim()).isEqualTo("preferred_username");

    OidcConfiguration updatedOidc = oidcConfigurationDAO.get();
    assertThat(updatedOidc).isNotNull();
    assertThat(updatedOidc.getClientId()).isEqualTo("nexus-iq-client");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_ClientSecretEncryption() throws Exception {
    // Given: Plain text client secret
    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();
    String plainTextSecret = "super-secret-password-123";
    ssoConfig.getOidcConfiguration().setClientSecret(plainTextSecret);

    // When: Insert configuration
    HttpResponse response = restRequest().body(ssoConfig).put();

    // Then: Secret should be encrypted in database
    ctx.assertResponseStatus(204, response);

    OidcConfiguration persistedOidc = oidcConfigurationDAO.get();
    assertThat(persistedOidc.getClientSecret()).isNotEqualTo(plainTextSecret);

    // Verify decryption works correctly
    String decryptedSecret = passwordHandler.decryptPassword(persistedOidc.getClientSecret());
    assertThat(decryptedSecret).isEqualTo(plainTextSecret);
  }

  @Test
  public void testDeleteOidcConfiguration_OnlyOidcExists() throws Exception {
    // Given: Only OIDC configuration exists (OAuth2 does not exist)
    OidcConfiguration oidcConfig = new OidcConfiguration(
        "https://auth.example.com",
        "test-client-id",
        passwordHandler.encryptPassword("test-secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfigurationDAO.insert(oidcConfig);

    // Verify only OIDC exists
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNotNull();

    // When: Delete OIDC configuration
    HttpResponse response = restRequest().delete();

    // Then: Should return 204 No Content
    ctx.assertResponseStatus(204, response);

    // Verify OIDC configuration was deleted
    assertThat(oidcConfigurationDAO.get()).isNull();
  }

  @Test
  public void testDeleteOidcConfiguration_NoConfigurationsExist() throws Exception {
    // Given: No configurations exist
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();

    // When: Delete OIDC configuration (nothing exists)
    HttpResponse response = restRequest().delete();

    // Then: Should return 404 Not Found
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains("Oidc configuration not set");

    // Verify nothing changed
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();
  }

  @Test
  public void testDeleteOidcConfiguration_Delete() throws Exception {
    // Given: Complete OIDC and OAuth2 configuration with all optional fields
    OAuth2Configuration oauth2Config = new OAuth2Configuration(
        "https://auth.example.com",
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);
    oauth2Config.setUsernameClaim("preferred_username");
    oauth2Config.setEmailClaim("email");
    oauth2Config.setFirstNameClaim("given_name");
    oauth2Config.setLastNameClaim("family_name");
    oauth2Config.setGroupsClaim("groups");
    oauth2Config.setExactMatchClaimsJson("{\"role\": \"admin\"}");
    oAuth2ConfigurationDAO.insert(oauth2Config);

    OidcConfiguration oidcConfig = new OidcConfiguration(
        "https://auth.example.com",
        "test-client-id",
        passwordHandler.encryptPassword("test-secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfig.setAuthorizationCustomParamsJson("{\"prompt\": \"consent\"}");
    oidcConfig.setTokenRequestCustomParamsJson("{\"resource\": \"api\"}");
    oidcConfigurationDAO.insert(oidcConfig);

    // Verify both configurations exist with all fields
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNotNull();
    assertThat(oidcConfigurationDAO.get()).isNotNull();

    // When: Delete OIDC configuration
    HttpResponse response = restRequest().delete();

    // Then: Should return 204 No Content
    ctx.assertResponseStatus(204, response);

    // Verify both configurations were completely deleted
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();
  }

  @Test
  public void testDeleteOidcConfiguration_AfterInsert() throws Exception {
    // Given: Insert a new configuration
    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();
    HttpResponse insertResponse = restRequest().body(ssoConfig).put();
    ctx.assertResponseStatus(204, insertResponse);

    // Verify configuration was inserted
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNotNull();
    assertThat(oidcConfigurationDAO.get()).isNotNull();

    // When: Delete the configuration
    HttpResponse deleteResponse = restRequest().delete();

    // Then: Should return 204 No Content
    ctx.assertResponseStatus(204, deleteResponse);

    // Verify both configurations were deleted
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();
  }

  @Test
  public void testDeleteOidcConfiguration_Idempotent() throws Exception {
    // Given: Both OIDC and OAuth2 configurations exist
    OAuth2Configuration oauth2Config = new OAuth2Configuration(
        "https://auth.example.com",
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);
    oAuth2ConfigurationDAO.insert(oauth2Config);

    OidcConfiguration oidcConfig = new OidcConfiguration(
        "https://auth.example.com",
        "test-client-id",
        passwordHandler.encryptPassword("test-secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfigurationDAO.insert(oidcConfig);

    // When: Delete configuration first time
    HttpResponse firstDeleteResponse = restRequest().delete();
    ctx.assertResponseStatus(204, firstDeleteResponse);

    // Verify both configurations were deleted
    assertThat(oidcConfigurationDAO.get()).isNull();
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();

    // When: Delete configuration a second time
    HttpResponse secondDeleteResponse = restRequest().delete();

    // Then: Should return 404 Not Found (no longer exists)
    ctx.assertResponseStatus(404, secondDeleteResponse);
    assertThat(secondDeleteResponse.getBodyText()).contains("Oidc configuration not set");

    // Verify nothing changed
    assertThat(oidcConfigurationDAO.get()).isNull();
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
  }

  /**
   * Helper method to create a valid SSO configuration for testing
   */
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
