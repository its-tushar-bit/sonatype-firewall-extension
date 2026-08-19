/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_CONFIG_PATH;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;

import com.auth0.json.mgmt.users.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.dataaccess.configuration.MailConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.PersistedUserSessionDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.PersistedUserSession;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.SsoUser;
import com.sonatype.insight.brain.security.UserSessionResource;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationResourceTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.banning.MTIQFeatureService;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.jaxrs.JsonUtils;
import jakarta.inject.Inject;
import java.net.HttpCookie;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public class MtiqUserResourceTest
    extends AbstractMultiTenantBaseIntegrationResourceTest
{
  private SamlUserDAO samlUserDAO;

  private OAuth2UserDAO oAuth2UserDAO;

  private TenantMetadataDAO tenantMetadataDAO;

  @Before
  public void localTestBefore() {
    samlUserDAO = lookup(SamlUserDAO.class);
    oAuth2UserDAO = lookup(OAuth2UserDAO.class);
    tenantMetadataDAO = lookup(TenantMetadataDAO.class);
  }

  /**
   * Test configuration that provides test service implementations.
   */
  @TestConfiguration
  static class MtiqUserResourceTestConfig
  {
    @Bean
    @Primary
    MultiTenantAuth0ManagementService multiTenantAuth0ManagementService() {
      return new TestMultiTenantAuth0ManagementService();
    }

    @Bean(name = "MTIQFeatureService")
    @Primary
    MTIQFeatureService mtiqFeatureService(
        ProductLicense productLicense,
        Configuration configuration,
        SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
        ApiConfigFeaturesService service,
        DeveloperEnablementService developerEnablementService,
        MailConfigurationDAO mailConfigurationDAO,
        TenantUtil tenantUtil)
    {
      return new TestMtiqFeatureService(productLicense, configuration, systemConfigurationPropertyDAO,
          service, developerEnablementService, mailConfigurationDAO, tenantUtil);
    }
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path("rest/mtiqUser");
  }

  @Test
  public void test_ListOfUser_Saml() throws Exception {
    enableSsoWithSaml();

    SamlUser samlUser =
        new SamlUser("username@example.com", "firstname", "lastname", "username@example.com", Collections.emptySet());
    samlUserDAO.insert(samlUser);

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);

    List<MtiqUserDTO> data = JsonUtils.parse(response.getBodyText(), new TypeReference<List<MtiqUserDTO>>()
    {
    });
    assertThat(data).hasSize(1);
    assertThat(MtiqUserDTO.ssoUserToMtiqUser(SsoUser.fromSamlUser(samlUser))).usingRecursiveComparison()
        .isEqualTo(data.get(0));
  }

  @Test
  public void test_ListOfUser_OAuth2() throws Exception {
    enableSsoWithOAuth2();

    OAuth2User oAuth2User =
        new OAuth2User("username@example.com", "firstname", "lastname", "username@example.com", Collections.emptySet());
    oAuth2UserDAO.insert(oAuth2User);

    HttpResponse response = restRequest().get();

    assertResponseStatus(200, response);

    List<MtiqUserDTO> data = JsonUtils.parse(response.getBodyText(), new TypeReference<List<MtiqUserDTO>>()
    {
    });
    assertThat(data).hasSize(1);
    assertThat(MtiqUserDTO.ssoUserToMtiqUser(SsoUser.fromOAuth2User(oAuth2User))).usingRecursiveComparison()
        .isEqualTo(data.get(0));
  }

  @Test
  public void test_inviteUserFailsWhenNoMetadata() throws Exception {
    MtiqUserDTO mtiqUserDTO = new MtiqUserDTO();
    mtiqUserDTO.setFirstName("foo");
    mtiqUserDTO.setLastName("bar");
    mtiqUserDTO.setEmail("foo@bar.com");
    mtiqUserDTO.setUsername("foo@bar.com");

    HttpResponse response = restRequest().body(mtiqUserDTO).post();

    assertResponseStatus(500, response);
  }

  @Test
  public void test_inviteUser_Saml() throws Exception {
    enableSsoWithSaml();

    tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connId", "connName", null, null, null));

    MtiqUserDTO mtiqUserDTO = new MtiqUserDTO();
    mtiqUserDTO.setFirstName("foo");
    mtiqUserDTO.setLastName("bar");
    mtiqUserDTO.setEmail("foo@bar.com");
    mtiqUserDTO.setUsername("foo@bar.com");

    assertThat(samlUserDAO.getAll()).hasSize(0);
    HttpResponse response = restRequest().body(mtiqUserDTO).post();

    assertResponseStatus(204, response);
    assertThat(samlUserDAO.getAll()).hasSize(1);
  }

  @Test
  public void test_inviteUser_OAuth2() throws Exception {
    enableSsoWithOAuth2();

    tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connId", "connName", null, null, null));

    MtiqUserDTO mtiqUserDTO = new MtiqUserDTO();
    mtiqUserDTO.setFirstName("foo");
    mtiqUserDTO.setLastName("bar");
    mtiqUserDTO.setEmail("foo@bar.com");
    mtiqUserDTO.setUsername("foo@bar.com");

    assertThat(oAuth2UserDAO.getAll()).hasSize(0);
    HttpResponse response = restRequest().body(mtiqUserDTO).post();

    assertResponseStatus(204, response);
    assertThat(oAuth2UserDAO.getAll()).hasSize(1);
  }

  @Test
  public void test_deleteUser_Saml() throws Exception {
    enableSsoWithSaml();

    tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connId", "connName", null, null, null));
    samlUserDAO.insert(new SamlUser("foo@bar.com", "foo", "bar", "foo@bar.com", Collections.emptySet()));

    assertThat(samlUserDAO.getAll()).hasSize(1);
    HttpResponse response = restRequest().path("foo@bar.com").delete();

    assertResponseStatus(204, response);
    assertThat(samlUserDAO.getAll()).hasSize(0);
  }

  @Test
  public void test_deleteUser_OAuth2() throws Exception {
    enableSsoWithOAuth2();

    tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connId", "connName", null, null, null));
    oAuth2UserDAO.insert(new OAuth2User("foo@bar.com", "foo", "bar", "foo@bar.com", Collections.emptySet()));

    assertThat(oAuth2UserDAO.getAll()).hasSize(1);
    HttpResponse response = restRequest().path("foo@bar.com").delete();

    assertResponseStatus(204, response);
    assertThat(oAuth2UserDAO.getAll()).hasSize(0);
  }

  @Test
  public void test_ThrowsExceptionWhenNotMangedIdp() throws Exception {
    TestMtiqFeatureService.isFeatureEnabledDuringTest = false;

    try {
      // List
      assertThat(restRequest().get().getStatusCode()).isEqualTo(400);

      // Invite
      MtiqUserDTO mtiqUserDTO = new MtiqUserDTO();
      mtiqUserDTO.setFirstName("foo");
      mtiqUserDTO.setLastName("bar");
      mtiqUserDTO.setEmail("foo@bar.com");
      mtiqUserDTO.setUsername("foo@bar.com");

      assertThat(restRequest().body(mtiqUserDTO).post().getStatusCode()).isEqualTo(400);

      // Delete
      assertThat(restRequest().path("foo@bar.com").delete().getStatusCode()).isEqualTo(400);
    }
    finally {
      TestMtiqFeatureService.isFeatureEnabledDuringTest = true;
    }
  }

  @Test
  public void testSessionTimeout_PerTenant() {
    AtomicReference<HttpCookie> cookie1 = new AtomicReference<>();
    AtomicReference<HttpCookie> cookie2 = new AtomicReference<>();

    Tenant tenant1 = testAsNewTenant("tenant1", t -> {
      provisionTenant(t.tenantSlug);
      HttpResponse response =
          adminRestRequest(ADMIN_CONFIG_PATH).parameter("tenant1").body(Map.of(SESSION_TIMEOUT_MINUTES, 3)).put();
      assertResponseStatus(204, response);
      com.sonatype.insight.brain.model.security.User user = tenantTemporaryEntity.newUser();

      response = super.restRequest().path(UserSessionResource.RESOURCE_PATH).auth(user).post();

      assertResponseStatus(204, response);
      List<PersistedUserSession> persistedUserSessions = lookup(PersistedUserSessionDAO.class).getAll();
      assertThat(persistedUserSessions).hasSize(1);
      PersistedUserSession persistedUserSession = persistedUserSessions.get(0);
      assertThat(persistedUserSession.getSession().getTimeout()).isEqualTo(3 * 60 * 1000);
      cookie1.set(response.getSessionCookie());
      assertThat(cookie1.get()).isNotNull();
    });
    Tenant tenant2 = testAsNewTenant("tenant2", t -> {
      provisionTenant(t.tenantSlug);
      com.sonatype.insight.brain.model.security.User user = tenantTemporaryEntity.newUser();

      HttpResponse response = super.restRequest().path(UserSessionResource.RESOURCE_PATH).auth(user).post();

      assertResponseStatus(204, response);
      List<PersistedUserSession> persistedUserSessions = lookup(PersistedUserSessionDAO.class).getAll();
      assertThat(persistedUserSessions).hasSize(1);
      PersistedUserSession persistedUserSession = persistedUserSessions.get(0);
      assertThat(persistedUserSession.getSession().getTimeout()).isEqualTo(30 * 60 * 1000);
      cookie2.set(response.getSessionCookie());
      assertThat(cookie2.get()).isNotNull();
    });

    testAsTenant(tenant1, t -> {
      setTenantSlug(tenant1.tenantSlug);

      HttpResponse response = super.restRequest().path(UserSessionResource.RESOURCE_PATH).cookie(cookie1.get()).get();

      assertResponseStatus(200, response);
      AuthenticationStatus authenticationStatus = response.getBody(AuthenticationStatus.class);
      assertThat(authenticationStatus.getSessionTimeoutMilliseconds()).isEqualTo(3 * 60 * 1000);
    });

    testAsTenant(tenant2, t -> {
      setTenantSlug(tenant2.tenantSlug);

      HttpResponse response = super.restRequest().path(UserSessionResource.RESOURCE_PATH).cookie(cookie2.get()).get();

      assertResponseStatus(200, response);
      AuthenticationStatus authenticationStatus = response.getBody(AuthenticationStatus.class);
      assertThat(authenticationStatus.getSessionTimeoutMilliseconds()).isEqualTo(30 * 60 * 1000);
    });
  }

  private static class TestMultiTenantAuth0ManagementService
      extends MultiTenantAuth0ManagementService
  {
    @Override
    public User createOrUpdateUser(
        final String email,
        final String firstName,
        final String lastName,
        final String connectionName,
        final String applicationId,
        final String connectionId,
        final String organizationId)
    {
      return new User();
    }

    @Override
    public void deleteUser(final String username, final String connectionId) {
      // no-op
    }
  }

  private static class TestMtiqFeatureService
      extends MTIQFeatureService
  {
    public static boolean isFeatureEnabledDuringTest = true;

    @Inject
    public TestMtiqFeatureService(
        final ProductLicense productLicense,
        final Configuration configuration,
        final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
        final ApiConfigFeaturesService service,
        final DeveloperEnablementService developerEnablementService,
        final MailConfigurationDAO mailConfigurationDAO,
        final TenantUtil tenantUtil)
    {
      super(productLicense, configuration, systemConfigurationPropertyDAO, service, developerEnablementService,
          mailConfigurationDAO, tenantUtil);
    }

    @Override
    public boolean isEnabled(final SystemConfigurationPropertyFeature feature) {
      return isFeatureEnabledDuringTest;
    }
  }
}
