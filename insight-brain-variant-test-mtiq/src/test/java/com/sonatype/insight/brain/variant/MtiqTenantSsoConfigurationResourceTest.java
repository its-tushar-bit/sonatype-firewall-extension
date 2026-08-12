/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.TenantSsoConfigurationResource;
import com.sonatype.insight.brain.api.v2.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiOidcConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.security.PasswordHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_SSO_CONFIGURATION_PATH;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.ISSUER;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.createSsoConfigurationDTO;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * MTIQ variant conversion of {@code TenantSsoConfigurationResourceTest} (which extended
 * {@code AbstractMultiTenantBaseIntegrationTest}). No base class; an injected {@link MtiqTestContext} supplies the
 * reused multi-tenant server, a fresh per-test tenant, and REST/lookup access.
 */
@MtiqTest
class MtiqTenantSsoConfigurationResourceTest
{
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();

  // Injected by MtiqServerExtension: the reused multi-tenant server + a fresh per-test tenant context.
  private MtiqTestContext ctx;

  private OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private OidcConfigurationDAO oidcConfigurationDAO;

  private SamlUserDAO samlUserDAO;

  private OAuth2UserDAO oAuth2UserDAO;

  private PasswordHandler passwordHandler;

  @BeforeEach
  void before() {
    oAuth2ConfigurationDAO = ctx.lookup(OAuth2ConfigurationDAO.class);
    oidcConfigurationDAO = ctx.lookup(OidcConfigurationDAO.class);
    samlUserDAO = ctx.lookup(SamlUserDAO.class);
    oAuth2UserDAO = ctx.lookup(OAuth2UserDAO.class);
    passwordHandler = ctx.lookup(PasswordHandler.class);
  }

  @Test
  void shouldSyncSsoProviderDataSources_whenTenantExists() throws Exception {
    String samlUsername = "samlUserName";
    String samlGroupName1 = "samlGroupName1";
    String samlGroupName2 = "samlGroupName2";
    Set<String> samlUserGroups = new HashSet<>(Arrays.asList(samlGroupName1, samlGroupName2));

    String oAuth2Username = "oAuth2UserName";
    String oAuth2GroupName1 = "oAuth2GroupName1";
    String oAuth2GroupName2 = "oAuth2GroupName2";
    Set<String> oAuth2UserGroups = new HashSet<>(Arrays.asList(oAuth2GroupName1, oAuth2GroupName2));

    // Create SAML User
    ctx.testAsTestTenant(t -> {
      SamlUser samlUser = ctx.tempEntity().newSamlUser(samlUsername, samlUserGroups);
      SamlGroup samlGroup1 = ctx.tempEntity().newSamlGroup(samlGroupName1);
      SamlGroup samlGroup2 = ctx.tempEntity().newSamlGroup(samlGroupName2);
      ctx.tempEntity().newSamlUserGroup(samlUser.getId(), samlGroup1.getId());
      ctx.tempEntity().newSamlUserGroup(samlUser.getId(), samlGroup2.getId());

      // Create OAuth2 User
      OAuth2User oAuth2User = ctx.tempEntity().newOAuth2User(oAuth2Username, oAuth2UserGroups);
      OAuth2Group oAuth2Group1 = ctx.tempEntity().newOAuth2Group(oAuth2GroupName1);
      OAuth2Group oAuth2Group2 = ctx.tempEntity().newOAuth2Group(oAuth2GroupName2);
      ctx.tempEntity().newOAuth2UserGroup(oAuth2User.getId(), oAuth2Group1.getId());
      ctx.tempEntity().newOAuth2UserGroup(oAuth2User.getId(), oAuth2Group2.getId());

      // Confirm OAuth2 and SAML data sources are not synced
      assertThat(samlUserDAO.getByUsername(oAuth2Username)).isNull();
      assertThat(oAuth2UserDAO.getByUsername(samlUsername)).isNull();
    });

    // Sync data sources
    HttpResponse response = syncSsoProviderDataSources(ctx.getTestTenant().tenantSlug).post();

    // Confirm OAuth2 and SAML data sources are synced
    ctx.assertResponseStatus(204, response);
    assertSamlUserExistsAndIsTheExpected(samlUsername, samlUserGroups);
    assertSamlUserExistsAndIsTheExpected(oAuth2Username, oAuth2UserGroups);
    assertOAuth2UserExistsAndIsTheExpected(samlUsername, samlUserGroups);
    assertOAuth2UserExistsAndIsTheExpected(oAuth2Username, oAuth2UserGroups);
  }

  @Test
  void shouldSend400_whenCallingSyncSsoProviderDataSourcesAndTenantIsGlobal() throws Exception {
    HttpResponse response = syncSsoProviderDataSources("global").post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Operation not supported for global tenant");
  }

  @Test
  void shouldSend404_whenCallingSyncSsoProviderDataSourcesTenantDoesntExist() throws Exception {
    HttpResponse response = syncSsoProviderDataSources("tenant4").post();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant tenant4 doesn't exist");
  }

  @Test
  void shouldUpdateSsoConfiguration_whenTenantExists() throws Exception {
    HttpResponse response = updateSsoConfiguration(ctx.getTestTenant().tenantSlug, ssoConfigurationDTO).put();

    assertConfigurationIsTheExpected(response);
  }

  @Test
  void shouldUpdateSsoConfiguration_whenAlreadyExists() throws Exception {
    HttpResponse response1 = updateSsoConfiguration(ctx.getTestTenant().tenantSlug, ssoConfigurationDTO).put();

    assertConfigurationIsTheExpected(response1);

    String otherAlgorithm = "RS512";
    String otherClientId = "other-client-id";
    SsoConfigurationDTO otherSsoConfigurationDTO = createSsoConfigurationDTO();
    otherSsoConfigurationDTO.getOAuth2Configuration().setIdpJwsAlgorithm(otherAlgorithm);
    otherSsoConfigurationDTO.getOidcConfiguration().setClientId(otherClientId);

    HttpResponse response2 = updateSsoConfiguration(ctx.getTestTenant().tenantSlug, otherSsoConfigurationDTO).put();

    ctx.assertResponseStatus(204, response2);
    assertOauth2ConfigurationIsTheExpected(otherSsoConfigurationDTO.getOAuth2Configuration(),
        oAuth2ConfigurationDAO.getById(ISSUER));
    assertOidcConfigurationIsTheExpected(otherSsoConfigurationDTO.getOidcConfiguration(), oidcConfigurationDAO.get());
  }

  @Test
  void shouldSend400_whenCallingUpdateSsoConfigurationAndTenantIsGlobal() throws Exception {
    HttpResponse response = updateSsoConfiguration("global", ssoConfigurationDTO).put();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Operation not supported for global tenant");
  }

  @Test
  void shouldSend404_whenCallingUpdateSsoConfigurationAndTenantDoesntExist() throws Exception {
    HttpResponse response = updateSsoConfiguration("tenant4", ssoConfigurationDTO).put();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant tenant4 doesn't exist");
  }

  private void assertSamlUserExistsAndIsTheExpected(final String username, final Set<String> samlUserGroups) {
    ctx.testAsTestTenant(t -> {
      SamlUser user = samlUserDAO.getByUsername(username);
      assertThat(user).isNotNull();
      assertThat(user.getGroups()).containsAll(samlUserGroups);
    });
  }

  private void assertOAuth2UserExistsAndIsTheExpected(final String username, final Set<String> samlUserGroups) {
    ctx.testAsTestTenant(t -> {
      OAuth2User user = oAuth2UserDAO.getByUsername(username);
      assertThat(user).isNotNull();
      assertThat(user.getGroups()).containsAll(samlUserGroups);
    });
  }

  private HttpRequest updateSsoConfiguration(
      String tenant,
      final SsoConfigurationDTO ssoConfigurationDTO) throws Exception
  {
    return ctx.adminRestRequest(ADMIN_TENANT_SSO_CONFIGURATION_PATH)
        .parameter(tenant)
        .body(objectMapper.writeValueAsString(ssoConfigurationDTO));
  }

  private HttpRequest syncSsoProviderDataSources(String tenant) {
    return ctx.adminRestRequest(ADMIN_TENANT_SSO_CONFIGURATION_PATH + TenantSsoConfigurationResource.SYNC_PATH)
        .parameter(tenant);
  }

  private void assertConfigurationIsTheExpected(final HttpResponse response) {
    ctx.assertResponseStatus(204, response);
    assertOauth2ConfigurationIsTheExpected(ssoConfigurationDTO.getOAuth2Configuration(),
        oAuth2ConfigurationDAO.getById(ISSUER));
    assertOidcConfigurationIsTheExpected(ssoConfigurationDTO.getOidcConfiguration(), oidcConfigurationDAO.get());
  }

  private void assertOauth2ConfigurationIsTheExpected(
      final OAuth2ConfigurationDTO oAuth2ConfigurationDTO,
      final OAuth2Configuration oAuth2Configuration)
  {
    assertThat(oAuth2Configuration.getId()).isEqualTo(oAuth2ConfigurationDTO.getIdpIssuer());
    assertThat(oAuth2Configuration.getIdpJwsAlgorithm()).isEqualTo(oAuth2ConfigurationDTO.getIdpJwsAlgorithm());
    assertThat(oAuth2Configuration.getIdpJwksUrl()).isEqualTo(oAuth2ConfigurationDTO.getIdpJwksUrl());
    assertThat(oAuth2Configuration.getIdpJwks()).isEqualTo(oAuth2ConfigurationDTO.getIdpJwks());
  }

  private void assertOidcConfigurationIsTheExpected(
      final OidcConfigurationDTO oidcConfigurationDTO,
      final OidcConfiguration oidcConfiguration)
  {
    assertThat(oidcConfiguration.getId()).isEqualTo(oidcConfigurationDTO.getIdpIssuer());
    assertThat(oidcConfiguration.getClientId()).isEqualTo(oidcConfigurationDTO.getClientId());
    assertThat(passwordHandler.decryptPassword(oidcConfiguration.getClientSecret()))
        .isEqualTo(oidcConfigurationDTO.getClientSecret());
    assertThat(oidcConfiguration.getIdpAuthorizationUrl()).isEqualTo(oidcConfigurationDTO.getIdpAuthorizationUrl());
    assertThat(oidcConfiguration.getIdpTokenUrl()).isEqualTo(oidcConfigurationDTO.getIdpTokenUrl());
  }

  @Test
  void shouldGetSsoConfiguration_whenConfigured() throws Exception {
    // Pre-populate via the PUT endpoint so the GET has something to read.
    HttpResponse putResponse = updateSsoConfiguration(ctx.getTestTenant().tenantSlug, ssoConfigurationDTO).put();
    ctx.assertResponseStatus(204, putResponse);

    HttpResponse response = getSsoConfiguration(ctx.getTestTenant().tenantSlug).get();

    ctx.assertResponseStatus(200, response);
    SsoConfigurationDTO body = objectMapper.readValue(response.getBodyText(), SsoConfigurationDTO.class);

    OidcConfigurationDTO expectedOidc = ssoConfigurationDTO.getOidcConfiguration();
    OidcConfigurationDTO actualOidc = body.getOidcConfiguration();
    assertThat(actualOidc.getIdpIssuer()).isEqualTo(expectedOidc.getIdpIssuer());
    assertThat(actualOidc.getClientId()).isEqualTo(expectedOidc.getClientId());
    // The plaintext secret must never be returned; the mask signals "configured but redacted".
    assertThat(actualOidc.getClientSecret()).isEqualTo(ApiOidcConfigurationService.CLIENT_SECRET_MASK);
    assertThat(actualOidc.getIdpAuthorizationUrl()).isEqualTo(expectedOidc.getIdpAuthorizationUrl());
    assertThat(actualOidc.getIdpTokenUrl()).isEqualTo(expectedOidc.getIdpTokenUrl());

    OAuth2ConfigurationDTO expectedOAuth2 = ssoConfigurationDTO.getOAuth2Configuration();
    OAuth2ConfigurationDTO actualOAuth2 = body.getOAuth2Configuration();
    assertThat(actualOAuth2.getIdpIssuer()).isEqualTo(expectedOAuth2.getIdpIssuer());
    assertThat(actualOAuth2.getIdpJwsAlgorithm()).isEqualTo(expectedOAuth2.getIdpJwsAlgorithm());
    assertThat(actualOAuth2.getIdpJwksUrl()).isEqualTo(expectedOAuth2.getIdpJwksUrl());
    assertThat(actualOAuth2.getIdpJwks()).isEqualTo(expectedOAuth2.getIdpJwks());
  }

  @Test
  void shouldGet404_whenGettingSsoConfigurationAndNotConfigured() throws Exception {
    HttpResponse response = getSsoConfiguration(ctx.getTestTenant().tenantSlug).get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("SSO configuration not set: OIDC configuration not found");
  }

  @Test
  void shouldGet400_whenGettingSsoConfigurationAndTenantIsGlobal() throws Exception {
    HttpResponse response = getSsoConfiguration("global").get();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Operation not supported for global tenant");
  }

  @Test
  void shouldGet404_whenGettingSsoConfigurationAndTenantDoesntExist() throws Exception {
    HttpResponse response = getSsoConfiguration("tenant4").get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant tenant4 doesn't exist");
  }

  private HttpRequest getSsoConfiguration(String tenant) {
    return ctx.adminRestRequest(ADMIN_TENANT_SSO_CONFIGURATION_PATH).parameter(tenant);
  }
}
