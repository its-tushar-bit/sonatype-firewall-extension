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
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
public class ApiOidcConfigurationResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private static final String IDP_ISSUER = "https://auth.example.com";

  private OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private OidcConfigurationDAO oidcConfigurationDAO;

  private PasswordHandler passwordHandler;

  private SsoConfigurationDTO validSsoConfiguration;

  private User unauthorizedUserAccount;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.OIDC_CONFIG_RESOURCE_PATH_V2);
  }

  @BeforeEach
  public void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUserAccount = ctx.tempEntity().newUser();

    oAuth2ConfigurationDAO = ctx.lookup(OAuth2ConfigurationDAO.class);
    oidcConfigurationDAO = ctx.lookup(OidcConfigurationDAO.class);
    passwordHandler = ctx.lookup(PasswordHandler.class);

    // Setup valid test configuration
    validSsoConfiguration = createValidSsoConfiguration();
  }

  @AfterEach
  public void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUserAccount.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUserAccount);
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_Insert() throws Exception {
    // Given: No existing configuration
    assertThat(oAuth2ConfigurationDAO.getById(IDP_ISSUER)).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();

    // When: Insert new OIDC configuration
    HttpResponse response = restRequest().body(validSsoConfiguration).put();

    // Then: Audit log should be created
    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_OIDC, null);
    assertAuditData(auditDTO);
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_Update() throws Exception {
    // Given: Existing configuration
    OAuth2Configuration existingOAuth2 = new OAuth2Configuration(
        IDP_ISSUER,
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);
    existingOAuth2.setUsernameClaim("old_username");
    oAuth2ConfigurationDAO.insert(existingOAuth2);

    OidcConfiguration existingOidc = new OidcConfiguration(
        IDP_ISSUER,
        "old-client-id",
        passwordHandler.encryptPassword("old-secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfigurationDAO.insert(existingOidc);

    // When: Update OIDC configuration
    HttpResponse response = restRequest().body(validSsoConfiguration).put();

    // Then: Audit log should be created
    ctx.assertResponseStatus(204, response);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_OIDC, null);
    assertAuditData(auditDTO);
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_BadRequest() throws Exception {
    // When: Attempt to insert null configuration
    HttpResponse response = restRequest().body(null).put();

    // Then: Audit log should capture the bad request
    ctx.assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.CONFIGURE_OIDC, "bad-request");
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

    SsoConfigurationDTO invalidSsoConfig = new SsoConfigurationDTO();
    invalidSsoConfig.setOAuth2Configuration(oauth2Config);
    invalidSsoConfig.setOidcConfiguration(oidcConfig);

    // When: Attempt to insert invalid configuration
    HttpResponse response = restRequest().body(invalidSsoConfig).put();

    // Then: Audit log should capture the bad request
    ctx.assertResponseStatus(400, response);
    assertAuditLog(AuditEvent.CONFIGURE_OIDC, "bad-request");
  }

  @Test
  public void testInsertOrUpdateOidcConfiguration_Unauthorized() throws Exception {
    // When: Unauthorized user attempts to configure OIDC
    HttpResponse response = restRequest().with(unauthorizedUser()).body(validSsoConfiguration).put();

    // Then: Audit log should capture the unauthorized attempt
    ctx.assertResponseStatus(403, response);
    assertAuditLog(AuditEvent.CONFIGURE_OIDC, "unauthorized");
  }

  /**
   * Helper method to assert audit data contains expected configuration values
   */
  private void assertAuditData(AuditDTO auditDTO) {
    OAuth2Configuration oauth2Config = oAuth2ConfigurationDAO.getById(IDP_ISSUER);
    OidcConfiguration oidcConfig = oidcConfigurationDAO.get();

    assertThat(oauth2Config).isNotNull();
    assertThat(oidcConfig).isNotNull();

    // Assert OAuth2 configuration audit data
    assertThat(auditDTO.data)
        .containsEntry("oauth2IdpIssuer", oauth2Config.getId())
        .containsEntry("oauth2IdpJwksUrl", oauth2Config.getIdpJwksUrl())
        .containsEntry("oauth2IdpJwsAlgorithm", oauth2Config.getIdpJwsAlgorithm());

    // Assert OIDC configuration audit data
    assertThat(auditDTO.data)
        .containsEntry("oidcIdpIssuer", oidcConfig.getId())
        .containsEntry("oidcClientId", oidcConfig.getClientId())
        .containsEntry("oidcIdpAuthorizationUrl", oidcConfig.getIdpAuthorizationUrl())
        .containsEntry("oidcIdpTokenUrl", oidcConfig.getIdpTokenUrl())
        .containsEntry("oidcClientSecret", ApiOidcConfigurationService.CLIENT_SECRET_MASK);

    // Assert optional fields if present
    if (oauth2Config.getUsernameClaim() != null) {
      assertThat(auditDTO.data).containsEntry("oauth2UsernameClaim", oauth2Config.getUsernameClaim());
    }
    if (oauth2Config.getEmailClaim() != null) {
      assertThat(auditDTO.data).containsEntry("oauth2EmailClaim", oauth2Config.getEmailClaim());
    }
    if (oauth2Config.getFirstNameClaim() != null) {
      assertThat(auditDTO.data).containsEntry("oauth2FirstNameClaim", oauth2Config.getFirstNameClaim());
    }
    if (oauth2Config.getLastNameClaim() != null) {
      assertThat(auditDTO.data).containsEntry("oauth2LastNameClaim", oauth2Config.getLastNameClaim());
    }
    if (oauth2Config.getGroupsClaim() != null) {
      assertThat(auditDTO.data).containsEntry("oauth2GroupsClaim", oauth2Config.getGroupsClaim());
    }
  }

  @Test
  public void testDeleteOidcConfiguration_Success() throws Exception {
    // Given: Existing OIDC configuration
    OAuth2Configuration existingOAuth2 = new OAuth2Configuration(
        IDP_ISSUER,
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);
    oAuth2ConfigurationDAO.insert(existingOAuth2);

    OidcConfiguration existingOidc = new OidcConfiguration(
        IDP_ISSUER,
        "test-client-id",
        passwordHandler.encryptPassword("test-secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfigurationDAO.insert(existingOidc);

    // When: Delete OIDC configuration
    HttpResponse response = restRequest().delete();

    // Then: Audit log should be created
    ctx.assertResponseStatus(204, response);
    assertAuditLog(AuditEvent.DELETE_OIDC, null);

    // Verify configuration is deleted
    assertThat(oAuth2ConfigurationDAO.getById(IDP_ISSUER)).isNull();
    assertThat(oidcConfigurationDAO.get()).isNull();
  }

  @Test
  public void testDeleteOidcConfiguration_NotFound() throws Exception {
    // Given: No existing configuration
    assertThat(oidcConfigurationDAO.get()).isNull();

    // When: Attempt to delete non-existent configuration
    HttpResponse response = restRequest().delete();

    // Then: Audit log should capture the not found error
    ctx.assertResponseStatus(404, response);
    assertAuditLog(AuditEvent.DELETE_OIDC, "not-found");
  }

  @Test
  public void testDeleteOidcConfiguration_Unauthorized() throws Exception {
    // Given: Existing OIDC configuration
    OAuth2Configuration existingOAuth2 = new OAuth2Configuration(
        IDP_ISSUER,
        "RS256",
        "https://auth.example.com/.well-known/jwks.json",
        null);
    oAuth2ConfigurationDAO.insert(existingOAuth2);

    OidcConfiguration existingOidc = new OidcConfiguration(
        IDP_ISSUER,
        "test-client-id",
        passwordHandler.encryptPassword("test-secret"),
        "https://auth.example.com/authorize",
        "https://auth.example.com/token");
    oidcConfigurationDAO.insert(existingOidc);

    // When: Unauthorized user attempts to delete OIDC configuration
    HttpResponse response = restRequest().with(unauthorizedUser()).delete();

    // Then: Audit log should capture the unauthorized attempt
    ctx.assertResponseStatus(403, response);
    assertAuditLog(AuditEvent.DELETE_OIDC, "unauthorized");

    // Verify configuration still exists
    assertThat(oAuth2ConfigurationDAO.getById(IDP_ISSUER)).isNotNull();
    assertThat(oidcConfigurationDAO.get()).isNotNull();
  }

  /**
   * Helper method to create a valid SSO configuration for testing
   */
  private SsoConfigurationDTO createValidSsoConfiguration() {
    OAuth2ConfigurationDTO oauth2Config = new OAuth2ConfigurationDTO();
    oauth2Config.setIdpIssuer(IDP_ISSUER);
    oauth2Config.setIdpJwksUrl("https://auth.example.com/.well-known/jwks.json");
    oauth2Config.setIdpJwsAlgorithm("RS256");
    oauth2Config.setUsernameClaim("preferred_username");
    oauth2Config.setEmailClaim("email");

    OidcConfigurationDTO oidcConfig = new OidcConfigurationDTO();
    oidcConfig.setIdpIssuer(IDP_ISSUER);
    oidcConfig.setClientId("nexus-iq-client");
    oidcConfig.setClientSecret("test-client-secret");
    oidcConfig.setIdpAuthorizationUrl("https://auth.example.com/authorize");
    oidcConfig.setIdpTokenUrl("https://auth.example.com/token");

    SsoConfigurationDTO ssoConfig = new SsoConfigurationDTO();
    ssoConfig.setOAuth2Configuration(oauth2Config);
    ssoConfig.setOidcConfiguration(oidcConfig);

    return ssoConfig;
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
