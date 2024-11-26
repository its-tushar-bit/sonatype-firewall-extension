/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.SsoUser;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationResourceTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.banning.MTIQFeatureService;
import com.sonatype.insight.jaxrs.JsonUtils;

import com.auth0.json.mgmt.users.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

  @Override
  protected List<Module> getBrainModules() {
    List<Module> modules = new ArrayList<>(super.getBrainModules());
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(MultiTenantAuth0ManagementService.class).to(TestMultiTenantAuth0ManagementService.class);
        bind(MTIQFeatureService.class).to(TestMtiqFeatureService.class).asEagerSingleton();
      }
    });
    return modules;
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

    List<MtiqUserDTO> data = JsonUtils.parse(response.getBodyText(), new TypeReference<List<MtiqUserDTO>>() { });
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

    List<MtiqUserDTO> data = JsonUtils.parse(response.getBodyText(), new TypeReference<List<MtiqUserDTO>>() { });
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
      //no-op
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
        final DeveloperEnablementService developerEnablementService)
    {
      super(productLicense, configuration, systemConfigurationPropertyDAO, service, developerEnablementService);
    }

    @Override
    public boolean isEnabled(final SystemConfigurationPropertyFeature feature) {
      return isFeatureEnabledDuringTest;
    }
  }
}
