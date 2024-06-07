/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.dto.OAuth2ConfigurationDTO;
import com.sonatype.insight.brain.api.admin.dto.OidcConfigurationDTO;
import com.sonatype.insight.brain.api.admin.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OidcConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.configuration.oauth2.OidcConfiguration;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_SSO_CONFIGURATION_PATH;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.ISSUER;
import static com.sonatype.insight.brain.api.admin.SsoConfigurationTestHelper.createSsoConfigurationDTO;
import static org.assertj.core.api.Assertions.assertThat;

public class TenantSsoConfigurationResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private static SsoConfigurationDTO ssoConfigurationDTO = createSsoConfigurationDTO();

  private OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private OidcConfigurationDAO oidcConfigurationDAO;

  @Before
  public void before() {
    oAuth2ConfigurationDAO = lookup(OAuth2ConfigurationDAO.class);
    oidcConfigurationDAO = lookup(OidcConfigurationDAO.class);
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
  public void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = updateSsoConfiguration("global", ssoConfigurationDTO).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = updateSsoConfiguration("tenant4", ssoConfigurationDTO).put();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  private HttpRequest updateSsoConfiguration(String tenant, final SsoConfigurationDTO ssoConfigurationDTO)
      throws Exception
  {
    return adminRestRequest(ADMIN_TENANT_SSO_CONFIGURATION_PATH)
        .parameter(tenant)
        .body(objectMapper.writeValueAsString(ssoConfigurationDTO));
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
    TestCase.assertEquals(oidcConfigurationDTO.getClientSecret(), oidcConfiguration.getClientSecret());
    TestCase.assertEquals(oidcConfigurationDTO.getIdpAuthorizationUrl(), oidcConfiguration.getIdpAuthorizationUrl());
    TestCase.assertEquals(oidcConfigurationDTO.getIdpTokenUrl(), oidcConfiguration.getIdpTokenUrl());
  }
}
