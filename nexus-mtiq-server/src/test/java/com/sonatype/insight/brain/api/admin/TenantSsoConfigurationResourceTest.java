/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
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
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.common.test.SlowTest;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_SSO_CONFIGURATION_PATH;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.ISSUER;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.createSsoConfigurationDTO;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class TenantSsoConfigurationResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private static SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();

  private OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private OidcConfigurationDAO oidcConfigurationDAO;

  private SamlUserDAO samlUserDAO;

  private OAuth2UserDAO oAuth2UserDAO;

  private PasswordHandler passwordHandler;

  @Before
  public void before() {
    oAuth2ConfigurationDAO = lookup(OAuth2ConfigurationDAO.class);
    oidcConfigurationDAO = lookup(OidcConfigurationDAO.class);
    samlUserDAO = lookup(SamlUserDAO.class);
    oAuth2UserDAO = lookup(OAuth2UserDAO.class);
    passwordHandler = lookup(PasswordHandler.class);
  }

  @Test
  public void shouldSyncSsoProviderDataSources_whenTenantExists() throws Exception {
    String samlUsername = "samlUserName";
    String samlGroupName1 = "samlGroupName1";
    String samlGroupName2 = "samlGroupName2";
    Set<String> samlUserGroups = new HashSet<>(Arrays.asList(samlGroupName1, samlGroupName2));

    String oAuth2Username = "oAuth2UserName";
    String oAuth2GroupName1 = "oAuth2GroupName1";
    String oAuth2GroupName2 = "oAuth2GroupName2";
    Set<String> oAuth2UserGroups = new HashSet<>(Arrays.asList(oAuth2GroupName1, oAuth2GroupName2));

    // Create SAML User
    SamlUser samlUser =
        tenantTemporaryEntity.newSamlUser(samlUsername, samlUserGroups);
    SamlGroup samlGroup1 = tenantTemporaryEntity.newSamlGroup(samlGroupName1);
    SamlGroup samlGroup2 = tenantTemporaryEntity.newSamlGroup(samlGroupName2);
    tenantTemporaryEntity.newSamlUserGroup(samlUser.getId(), samlGroup1.getId());
    tenantTemporaryEntity.newSamlUserGroup(samlUser.getId(), samlGroup2.getId());

    // Create OAuth2 User
    OAuth2User oAuth2User =
        tenantTemporaryEntity.newOAuth2User(oAuth2Username, oAuth2UserGroups);
    OAuth2Group oAuth2Group1 = tenantTemporaryEntity.newOAuth2Group(oAuth2GroupName1);
    OAuth2Group oAuth2Group2 = tenantTemporaryEntity.newOAuth2Group(oAuth2GroupName2);
    tenantTemporaryEntity.newOAuth2UserGroup(oAuth2User.getId(), oAuth2Group1.getId());
    tenantTemporaryEntity.newOAuth2UserGroup(oAuth2User.getId(), oAuth2Group2.getId());

    // Confirm OAuth2 and SAML data sources are not synced
    assertThat(samlUserDAO.getByUsername(oAuth2Username)).isNull();
    assertThat(oAuth2UserDAO.getByUsername(samlUsername)).isNull();

    // Sync data sources
    HttpResponse response = syncSsoProviderDataSources(getTestTenant().tenantSlug).post();

    // Confirm OAuth2 and SAML data sources are synced
    assertResponseStatus(204, response);
    assertSamlUserExistsAndIsTheExpected(samlUsername, samlUserGroups);
    assertSamlUserExistsAndIsTheExpected(oAuth2Username, oAuth2UserGroups);
    assertOAuth2UserExistsAndIsTheExpected(samlUsername, samlUserGroups);
    assertOAuth2UserExistsAndIsTheExpected(oAuth2Username, oAuth2UserGroups);
  }

  @Test
  public void shouldSend400_whenCallingSyncSsoProviderDataSourcesAndTenantIsGlobal() throws Exception {
    HttpResponse response = syncSsoProviderDataSources("global").post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void shouldSend404_whenCallingSyncSsoProviderDataSourcesTenantDoesntExist() throws Exception {
    HttpResponse response = syncSsoProviderDataSources("tenant4").post();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  @Test
  public void shouldUpdateSsoConfiguration_whenTenantExists() throws Exception {
    HttpResponse response = updateSsoConfiguration(getTestTenant().tenantSlug, ssoConfigurationDTO).put();

    assertConfigurationIsTheExpected(response);
  }

  @Test
  public void shouldUpdateSsoConfiguration_whenAlreadyExists() throws Exception {
    HttpResponse response1 = updateSsoConfiguration(getTestTenant().tenantSlug, ssoConfigurationDTO).put();

    assertConfigurationIsTheExpected(response1);

    String otherAlgorithm = "RS512";
    String otherClientId = "other-client-id";
    SsoConfigurationDTO otherSsoConfigurationDTO = createSsoConfigurationDTO();
    otherSsoConfigurationDTO.getOAuth2Configuration().setIdpJwsAlgorithm(otherAlgorithm);
    otherSsoConfigurationDTO.getOidcConfiguration().setClientId(otherClientId);

    HttpResponse response2 = updateSsoConfiguration(getTestTenant().tenantSlug, otherSsoConfigurationDTO).put();

    assertResponseStatus(204, response2);
    assertOauth2ConfigurationIsTheExpected(otherSsoConfigurationDTO.getOAuth2Configuration(),
        oAuth2ConfigurationDAO.getById(ISSUER));
    assertOidcConfigurationIsTheExpected(otherSsoConfigurationDTO.getOidcConfiguration(), oidcConfigurationDAO.get());
  }

  @Test
  public void shouldSend400_whenCallingUpdateSsoConfigurationAndTenantIsGlobal() throws Exception {
    HttpResponse response = updateSsoConfiguration("global", ssoConfigurationDTO).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void shouldSend404_whenCallingUpdateSsoConfigurationAndTenantDoesntExist() throws Exception {
    HttpResponse response = updateSsoConfiguration("tenant4", ssoConfigurationDTO).put();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  private void assertSamlUserExistsAndIsTheExpected(final String username, final Set<String> samlUserGroups) {
    SamlUser user = samlUserDAO.getByUsername(username);
    assertThat(user).isNotNull();
    assertThat(user.getGroups()).containsAll(samlUserGroups);
  }

  private void assertOAuth2UserExistsAndIsTheExpected(final String username, final Set<String> samlUserGroups) {
    OAuth2User user = oAuth2UserDAO.getByUsername(username);
    assertThat(user).isNotNull();
    assertThat(user.getGroups()).containsAll(samlUserGroups);
  }

  private HttpRequest updateSsoConfiguration(
      String tenant,
      final SsoConfigurationDTO ssoConfigurationDTO) throws Exception
  {
    return adminRestRequest(ADMIN_TENANT_SSO_CONFIGURATION_PATH)
        .parameter(tenant)
        .body(objectMapper.writeValueAsString(ssoConfigurationDTO));
  }

  private HttpRequest syncSsoProviderDataSources(String tenant) {
    return adminRestRequest(ADMIN_TENANT_SSO_CONFIGURATION_PATH + TenantSsoConfigurationResource.SYNC_PATH)
        .parameter(tenant);
  }

  private void assertConfigurationIsTheExpected(final HttpResponse response) {
    assertResponseStatus(204, response);
    assertOauth2ConfigurationIsTheExpected(ssoConfigurationDTO.getOAuth2Configuration(),
        oAuth2ConfigurationDAO.getById(ISSUER));
    assertOidcConfigurationIsTheExpected(ssoConfigurationDTO.getOidcConfiguration(), oidcConfigurationDAO.get());
  }

  private void assertOauth2ConfigurationIsTheExpected(
      final OAuth2ConfigurationDTO oAuth2ConfigurationDTO,
      final OAuth2Configuration oAuth2Configuration)
  {
    TestCase.assertEquals(oAuth2ConfigurationDTO.getIdpIssuer(), oAuth2Configuration.getId());
    TestCase.assertEquals(oAuth2ConfigurationDTO.getIdpJwsAlgorithm(), oAuth2Configuration.getIdpJwsAlgorithm());
    TestCase.assertEquals(oAuth2ConfigurationDTO.getIdpJwksUrl(), oAuth2Configuration.getIdpJwksUrl());
    TestCase.assertEquals(oAuth2ConfigurationDTO.getIdpJwks(), oAuth2Configuration.getIdpJwks());
  }

  private void assertOidcConfigurationIsTheExpected(
      final OidcConfigurationDTO oidcConfigurationDTO,
      final OidcConfiguration oidcConfiguration)
  {
    TestCase.assertEquals(oidcConfigurationDTO.getIdpIssuer(), oidcConfiguration.getId());
    TestCase.assertEquals(oidcConfigurationDTO.getClientId(), oidcConfiguration.getClientId());
    TestCase.assertEquals(oidcConfigurationDTO.getClientSecret(),
        passwordHandler.decryptPassword(oidcConfiguration.getClientSecret()));
    TestCase.assertEquals(oidcConfigurationDTO.getIdpAuthorizationUrl(), oidcConfiguration.getIdpAuthorizationUrl());
    TestCase.assertEquals(oidcConfigurationDTO.getIdpTokenUrl(), oidcConfiguration.getIdpTokenUrl());
  }
}
