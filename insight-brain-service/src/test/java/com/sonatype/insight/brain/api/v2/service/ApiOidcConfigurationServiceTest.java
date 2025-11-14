/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiOidcConfigurationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiOidcConfigurationService service;

  @Inject
  private OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  @Inject
  private OidcConfigurationDAO oidcConfigurationDAO;

  @Inject
  private PasswordHandler passwordHandler;

  @Test
  public void testGetOidcConfiguration_NotConfigured() {
    // Given: No configuration exists
    assertThat(oidcConfigurationDAO.get()).isNull();

    // When/Then: Should throw NotFoundException
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getOidcConfiguration())
        .withMessage("Oidc configuration not set");
  }

  @Test
  public void testGetOidcConfiguration_OidcConfiguredButOAuth2NotConfigured() {
    // Given: OIDC exists but OAuth2 does not
    tempEntity.newOidcConfiguration("https://auth.example.com", "client-id",
        passwordHandler.encryptPassword("secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");

    // When/Then: Should throw NotFoundException
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getOidcConfiguration())
        .withMessage("Oidc configuration not set");
  }

  @Test
  public void testGetOidcConfiguration_Configured() {
    // Given: Both OAuth2 and OIDC configurations exist
    OAuth2Configuration oauth2Config = tempEntity.newOAuth2Configuration(
        "https://auth.example.com",
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);
    oauth2Config.setUsernameClaim("preferred_username");
    oauth2Config.setEmailClaim("email");
    oAuth2ConfigurationDAO.update(oauth2Config);

    String encryptedSecret = passwordHandler.encryptPassword("test-client-secret");
    tempEntity.newOidcConfiguration(
        "https://auth.example.com",
        "nexus-iq-client",
        encryptedSecret,
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");

    // When: Get configuration
    SsoConfigurationDTO result = service.getOidcConfiguration();

    // Then: Should return complete SSO configuration
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

    // Verify client secret is masked (not sent to frontend)
    assertThat(oidcDto.getClientSecret()).isEqualTo(ApiOidcConfigurationService.CLIENT_SECRET_MASK);
  }

  @Test
  public void testGetOidcConfiguration_WithAllOptionalFields() {
    // Given: Configuration with all optional fields
    OAuth2Configuration oauth2Config = tempEntity.newOAuth2Configuration(
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
    oAuth2ConfigurationDAO.update(oauth2Config);

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
    SsoConfigurationDTO result = service.getOidcConfiguration();

    // Then: All optional OAuth2 fields should be present
    OAuth2ConfigurationDTO oauth2Dto = result.getOAuth2Configuration();
    assertThat(oauth2Dto.getUsernameClaim()).isEqualTo("sub");
    assertThat(oauth2Dto.getFirstNameClaim()).isEqualTo("given_name");
    assertThat(oauth2Dto.getLastNameClaim()).isEqualTo("family_name");
    assertThat(oauth2Dto.getEmailClaim()).isEqualTo("email");
    assertThat(oauth2Dto.getGroupsClaim()).isEqualTo("groups");
    assertThat(oauth2Dto.getExactMatchClaimsJson()).isEqualTo("{\"role\": \"admin\"}");

    // Verify all OIDC optional fields are returned
    OidcConfigurationDTO oidcDto = result.getOidcConfiguration();
    assertThat(oidcDto.getClientId()).isEqualTo("client-id");
    assertThat(oidcDto.getIdpAuthorizationUrl()).isEqualTo("https://auth.example.com/authorize");
    assertThat(oidcDto.getAuthorizationCustomParamsJson()).isEqualTo("{\"prompt\": \"consent\"}");
    assertThat(oidcDto.getTokenRequestCustomParamsJson()).isEqualTo("{\"resource\": \"api\"}");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_Insert() {
    // Given: No existing configuration
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();

    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();

    // When: Insert new configuration
    service.insertOrUpdateOidcConfiguration(ssoConfig);

    // Then: Both OAuth2 and OIDC configurations should be persisted
    OAuth2Configuration persistedOAuth2 = oAuth2ConfigurationDAO.getById("https://auth.example.com");
    assertThat(persistedOAuth2).isNotNull();
    assertThat(persistedOAuth2.getId()).isEqualTo("https://auth.example.com");
    assertThat(persistedOAuth2.getIdpJwksUrl()).isEqualTo("https://auth.example.com/.well-known/jwks.json");
    assertThat(persistedOAuth2.getIdpJwsAlgorithm()).isEqualTo("RS256");
    assertThat(persistedOAuth2.getUsernameClaim()).isEqualTo("preferred_username");
    assertThat(persistedOAuth2.getEmailClaim()).isEqualTo("email");

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
  public void testInsertOrUpdateOidcConfiguration_Update() {
    // Given: Existing configuration
    tempEntity.newOAuth2Configuration("https://auth.example.com", "RS256",
        "https://auth.example.com/.well-known/jwks.json", null);

    tempEntity.newOidcConfiguration("https://auth.example.com", "old-client-id",
        passwordHandler.encryptPassword("old-secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");

    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();

    // When: Update configuration
    service.insertOrUpdateOidcConfiguration(ssoConfig);

    // Then: Both configurations should be updated
    OAuth2Configuration updatedOAuth2 = oAuth2ConfigurationDAO.getById("https://auth.example.com");
    assertThat(updatedOAuth2).isNotNull();
    assertThat(updatedOAuth2.getUsernameClaim()).isEqualTo("preferred_username");
    assertThat(updatedOAuth2.getEmailClaim()).isEqualTo("email");

    OidcConfiguration updatedOidc = oidcConfigurationDAO.get();
    assertThat(updatedOidc).isNotNull();
    assertThat(updatedOidc.getClientId()).isEqualTo("nexus-iq-client");

    // Verify new client secret was encrypted
    String decryptedSecret = passwordHandler.decryptPassword(updatedOidc.getClientSecret());
    assertThat(decryptedSecret).isEqualTo("test-client-secret");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_WithAllOptionalFields() {
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
    service.insertOrUpdateOidcConfiguration(ssoConfig);

    // Then: All fields should be persisted
    OAuth2Configuration persistedOAuth2 = oAuth2ConfigurationDAO.getById("https://auth.example.com");
    assertThat(persistedOAuth2.getUsernameClaim()).isEqualTo("sub");
    assertThat(persistedOAuth2.getFirstNameClaim()).isEqualTo("given_name");
    assertThat(persistedOAuth2.getLastNameClaim()).isEqualTo("family_name");
    assertThat(persistedOAuth2.getEmailClaim()).isEqualTo("email");
    assertThat(persistedOAuth2.getGroupsClaim()).isEqualTo("groups");
    assertThat(persistedOAuth2.getExactMatchClaimsJson()).isEqualTo("{\"role\": \"admin\"}");

    OidcConfiguration persistedOidc = oidcConfigurationDAO.get();
    assertThat(persistedOidc.getAuthorizationCustomParamsJson()).isEqualTo("{\"prompt\": \"consent\"}");
    assertThat(persistedOidc.getTokenRequestCustomParamsJson()).isEqualTo("{\"resource\": \"api\"}");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_InvalidIdpIssuerMismatch() {
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

    // When/Then: Should throw BadRequestException
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.insertOrUpdateOidcConfiguration(ssoConfig))
        .withMessageContaining("OIDC IdP issuer must match OAuth2 IdP issuer");

    // Verify no configuration was persisted
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oAuth2ConfigurationDAO.getById("https://different-auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_NullConfiguration() {
    // When/Then: Should throw BadRequestException
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.insertOrUpdateOidcConfiguration(null))
        .withMessageContaining("OAuth2 and OIDC configurations must be provided");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_NullOAuth2Configuration() {
    // Given: SSO config with null OAuth2
    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(null);
    ssoConfig.setOidcConfiguration(new OidcConfigurationDTO());

    // When/Then: Should throw BadRequestException
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.insertOrUpdateOidcConfiguration(ssoConfig))
        .withMessageContaining("OAuth2 and OIDC configurations must be provided");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_NullOidcConfiguration() {
    // Given: SSO config with null OIDC
    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(new OAuth2ConfigurationDTO());
    ssoConfig.setOidcConfiguration(null);

    // When/Then: Should throw BadRequestException
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.insertOrUpdateOidcConfiguration(ssoConfig))
        .withMessageContaining("OAuth2 and OIDC configurations must be provided");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_ClientSecretEncryption() {
    // Given: Plain text client secret
    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();
    String plainTextSecret = "super-secret-password-123";
    ssoConfig.getOidcConfiguration().setClientSecret(plainTextSecret);

    // When: Insert configuration
    service.insertOrUpdateOidcConfiguration(ssoConfig);

    // Then: Secret should be encrypted in database
    OidcConfiguration persistedOidc = oidcConfigurationDAO.get();
    assertThat(persistedOidc.getClientSecret()).isNotEqualTo(plainTextSecret);

    // Verify decryption works correctly
    String decryptedSecret = passwordHandler.decryptPassword(persistedOidc.getClientSecret());
    assertThat(decryptedSecret).isEqualTo(plainTextSecret);
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_MixedScenario_OAuth2ExistsOidcNotExists() {
    // Given: OAuth2 exists but OIDC does not
    tempEntity.newOAuth2Configuration("https://auth.example.com", "RS256",
        "https://auth.example.com/.well-known/jwks.json", null);

    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNotNull();
    assertThat(oidcConfigurationDAO.get()).isNull();

    // When: Insert/update configuration
    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();
    service.insertOrUpdateOidcConfiguration(ssoConfig);

    // Then: OAuth2 should be updated, OIDC should be inserted
    OAuth2Configuration updatedOAuth2 = oAuth2ConfigurationDAO.getById("https://auth.example.com");
    assertThat(updatedOAuth2).isNotNull();

    OidcConfiguration insertedOidc = oidcConfigurationDAO.get();
    assertThat(insertedOidc).isNotNull();
    assertThat(insertedOidc.getClientId()).isEqualTo("nexus-iq-client");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_MixedScenario_OAuth2NotExistsOidcExists() {
    // Given: OIDC exists but OAuth2 does not
    tempEntity.newOidcConfiguration("https://auth.example.com", "old-client-id",
        passwordHandler.encryptPassword("old-secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");

    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNotNull();

    // When: Insert/update configuration
    SsoConfigurationDTO ssoConfig = createValidSsoConfiguration();
    service.insertOrUpdateOidcConfiguration(ssoConfig);

    // Then: OAuth2 should be inserted, OIDC should be updated
    OAuth2Configuration insertedOAuth2 = oAuth2ConfigurationDAO.getById("https://auth.example.com");
    assertThat(insertedOAuth2).isNotNull();
    assertThat(insertedOAuth2.getUsernameClaim()).isEqualTo("preferred_username");

    OidcConfiguration updatedOidc = oidcConfigurationDAO.get();
    assertThat(updatedOidc).isNotNull();
    assertThat(updatedOidc.getClientId()).isEqualTo("nexus-iq-client");
  }

  @Test
  public void testDeleteOidcConfiguration_OnlyOidcExists() {
    // Given: Only OIDC configuration exists (OAuth2 does not exist)
    tempEntity.newOidcConfiguration("https://auth.example.com", "test-client-id",
        passwordHandler.encryptPassword("test-secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");

    // Verify only OIDC exists
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNotNull();

    // When: Delete OIDC configuration
    service.deleteOidcConfiguration();

    // Then: OIDC configuration was deleted
    assertThat(oidcConfigurationDAO.get()).isNull();
  }

  @Test
  public void testDeleteOidcConfiguration_NoConfigurationsExist() {
    // Given: No configurations exist
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();

    // When/Then: Delete OIDC configuration should throw NotFoundException
    assertThatExceptionOfType(com.sonatype.insight.error.exception.NotFoundException.class)
        .isThrownBy(() -> service.deleteOidcConfiguration())
        .withMessageContaining("Oidc configuration not set");
  }

  @Test
  public void testDeleteOidcConfiguration_Delete() {
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
    service.deleteOidcConfiguration();

    // Then: Both configurations should be completely deleted
    assertThat(oAuth2ConfigurationDAO.getById("https://auth.example.com")).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();
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

  @Test
  public void testUpdateOidcConfiguration_WithMaskedSecretPreservesExistingSecret() {
    // Given: Existing configuration with encrypted secret
    tempEntity.newOAuth2Configuration(
        "https://auth.example.com",
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);

    String originalSecret = "original-secret";
    String encryptedOriginalSecret = passwordHandler.encryptPassword(originalSecret);
    tempEntity.newOidcConfiguration(
        "https://auth.example.com",
        "original-client-id",
        encryptedOriginalSecret,
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");

    // When: Update with masked client secret (simulating UI sending back masked value)
    OAuth2ConfigurationDTO oauth2Config = new OAuth2ConfigurationDTO();
    oauth2Config.setIdpIssuer("https://auth.example.com");
    oauth2Config.setIdpJwksUrl("https://auth.example.com/.well-known/jwks.json");
    oauth2Config.setIdpJwsAlgorithm("RS256");
    oauth2Config.setUsernameClaim("updated_username");

    OidcConfigurationDTO oidcConfig = new OidcConfigurationDTO();
    oidcConfig.setIdpIssuer("https://auth.example.com");
    oidcConfig.setClientId("updated-client-id");
    oidcConfig.setClientSecret(ApiOidcConfigurationService.CLIENT_SECRET_MASK); // Masked value
    oidcConfig.setIdpAuthorizationUrl("https://auth.example.com/authorize/updated");
    oidcConfig.setIdpTokenUrl("https://auth.example.com/token");

    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(oauth2Config);
    ssoConfig.setOidcConfiguration(oidcConfig);

    service.insertOrUpdateOidcConfiguration(ssoConfig);

    // Then: Other fields should be updated but secret should remain the same
    OidcConfiguration updatedOidc = oidcConfigurationDAO.get();
    assertThat(updatedOidc.getClientId()).isEqualTo("updated-client-id");
    assertThat(updatedOidc.getIdpAuthorizationUrl()).isEqualTo("https://auth.example.com/authorize/updated");

    // Verify the original secret is preserved (not re-encrypted)
    String decryptedSecret = passwordHandler.decryptPassword(updatedOidc.getClientSecret());
    assertThat(decryptedSecret).isEqualTo(originalSecret);
  }

  @Test
  public void testUpdateOidcConfiguration_WithNewSecretUpdatesSecret() {
    // Given: Existing configuration
    tempEntity.newOAuth2Configuration(
        "https://auth.example.com",
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);

    String originalSecret = "original-secret";
    tempEntity.newOidcConfiguration(
        "https://auth.example.com",
        "original-client-id",
        passwordHandler.encryptPassword(originalSecret),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");

    // When: Update with new actual client secret
    OAuth2ConfigurationDTO oauth2Config = new OAuth2ConfigurationDTO();
    oauth2Config.setIdpIssuer("https://auth.example.com");
    oauth2Config.setIdpJwksUrl("https://auth.example.com/.well-known/jwks.json");
    oauth2Config.setIdpJwsAlgorithm("RS256");

    OidcConfigurationDTO oidcConfig = new OidcConfigurationDTO();
    oidcConfig.setIdpIssuer("https://auth.example.com");
    oidcConfig.setClientId("updated-client-id");
    oidcConfig.setClientSecret("new-actual-secret"); // New secret provided
    oidcConfig.setIdpAuthorizationUrl("https://auth.example.com/authorize");
    oidcConfig.setIdpTokenUrl("https://auth.example.com/token");

    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(oauth2Config);
    ssoConfig.setOidcConfiguration(oidcConfig);

    service.insertOrUpdateOidcConfiguration(ssoConfig);

    // Then: The new secret should be encrypted and stored
    OidcConfiguration updatedOidc = oidcConfigurationDAO.get();
    String decryptedSecret = passwordHandler.decryptPassword(updatedOidc.getClientSecret());
    assertThat(decryptedSecret).isEqualTo("new-actual-secret");
  }

  @Test
  public void testInsertOidcConfiguration_WithMaskedSecretThrowsException() {
    // Given: No existing configuration

    // When/Then: Attempting to create new config with masked secret should fail
    OAuth2ConfigurationDTO oauth2Config = new OAuth2ConfigurationDTO();
    oauth2Config.setIdpIssuer("https://auth.example.com");
    oauth2Config.setIdpJwksUrl("https://auth.example.com/.well-known/jwks.json");
    oauth2Config.setIdpJwsAlgorithm("RS256");

    OidcConfigurationDTO oidcConfig = new OidcConfigurationDTO();
    oidcConfig.setIdpIssuer("https://auth.example.com");
    oidcConfig.setClientId("client-id");
    oidcConfig.setClientSecret(ApiOidcConfigurationService.CLIENT_SECRET_MASK); // Masked value on new config
    oidcConfig.setIdpAuthorizationUrl("https://auth.example.com/authorize");
    oidcConfig.setIdpTokenUrl("https://auth.example.com/token");

    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(oauth2Config);
    ssoConfig.setOidcConfiguration(oidcConfig);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.insertOrUpdateOidcConfiguration(ssoConfig))
        .withMessageContaining("Client secret cannot be masked for new configuration");
  }
}
