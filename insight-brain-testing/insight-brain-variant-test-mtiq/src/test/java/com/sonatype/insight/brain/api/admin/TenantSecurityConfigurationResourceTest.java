/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import jakarta.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.authorization.AuthorizationTestHelper;
import com.sonatype.insight.brain.api.admin.dto.SecurityConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.variant.MtiqTest;
import com.sonatype.insight.brain.variant.MtiqTestContext;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_SECURITY_CONFIG_PATH;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SAML_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * MTIQ variant conversion of {@code TenantSecurityConfigurationResourceTest} (which extended
 * {@code AbstractMultiTenantBaseIntegrationTest}). Retains the exact original simple name/package because
 * {@link #getEncodedIdPMetadataXml()} resolves its fixture via {@code getClass().getSimpleName()}.
 */
@MtiqTest
class TenantSecurityConfigurationResourceTest
{
  private MtiqTestContext ctx;

  @Test
  void shouldUpdateSamlConfigurationAndGrantAdminPermissions() throws Exception {
    ctx.testAsTestTenant(
        t -> ctx.lookup(com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO.class)
            .set(SAML_ENABLED, Boolean.toString(true)));

    HttpResponse response = updateSamlConfigurationAndGrantAdminPermissions(ctx.tenantSlug()).put();

    ctx.assertResponseStatus(204, response);
  }

  @Test
  void updateSamlConfigurationAndGrantAdminPermissions_shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = updateSamlConfigurationAndGrantAdminPermissions("global").put();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  void updateSamlConfigurationAndGrantAdminPermissions_shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = updateSamlConfigurationAndGrantAdminPermissions("tenant7").put();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  @Test
  void shouldUpdateSamlConfiguration() throws Exception {
    ctx.testAsTestTenant(
        t -> ctx.lookup(com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO.class)
            .set(SAML_ENABLED, Boolean.toString(true)));

    HttpResponse response = updateSamlConfiguration(ctx.tenantSlug()).put();

    ctx.assertResponseStatus(204, response);
  }

  @Test
  void updateSamlConfiguration_shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = updateSamlConfiguration("global").put();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  void updateSamlConfiguration_shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = updateSamlConfiguration("tenant7").put();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  @Test
  void shouldGrantAdminPermissionForAdmins() throws Exception {
    HttpResponse response = grantAdminPermissionForAdmins(ctx.tenantSlug()).put();

    ctx.assertResponseStatus(204, response);
  }

  @Test
  void grantAdminPermissionForAdmins_shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = grantAdminPermissionForAdmins("global").put();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  void grantAdminPermissionForAdmins_shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = grantAdminPermissionForAdmins("tenant7").put();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  @Test
  void shouldGetAdminEmails() throws Exception {
    grantAdminPermissionForAdmins(ctx.tenantSlug()).put(); // seed admin@local.com

    HttpResponse response = getAdminEmails(ctx.tenantSlug()).get();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText()).contains("admin@local.com");
  }

  @Test
  void getAdminEmails_shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = getAdminEmails("global").get();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  void getAdminEmails_shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = getAdminEmails("tenant7").get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  private HttpRequest getAdminEmails(String tenant) throws Exception {
    return ctx.adminRestRequest(
        ADMIN_TENANT_SECURITY_CONFIG_PATH + TenantSecurityConfigurationResource.GRANT_ADMIN_PERMISSIONS_PATH)
        .parameter(tenant)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt("local/"));
  }

  private HttpRequest updateSamlConfigurationAndGrantAdminPermissions(String tenant) throws Exception {
    HttpRequest request = ctx.adminRestRequest(ADMIN_TENANT_SECURITY_CONFIG_PATH)
        .parameter(tenant)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt("local/"));

    SecurityConfigurationDTO securityConfiguration = createSecurityConfigurationDTO();

    return request.body(securityConfiguration);
  }

  private HttpRequest updateSamlConfiguration(String tenant) throws Exception {
    HttpRequest request = ctx.adminRestRequest(
        ADMIN_TENANT_SECURITY_CONFIG_PATH + TenantSecurityConfigurationResource.UPDATE_SAML_CONFIGURATION_PATH)
        .parameter(tenant)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt("local/"));

    SecurityConfigurationDTO securityConfiguration = createSecurityConfigurationDTO();

    return request.body(securityConfiguration);
  }

  private HttpRequest grantAdminPermissionForAdmins(String tenant) throws Exception {
    HttpRequest request = ctx.adminRestRequest(
        ADMIN_TENANT_SECURITY_CONFIG_PATH + TenantSecurityConfigurationResource.GRANT_ADMIN_PERMISSIONS_PATH)
        .parameter(tenant)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt("local/"));

    List<String> emails = Arrays.asList("admin@local.com");

    return request.body(emails);
  }

  private SecurityConfigurationDTO createSecurityConfigurationDTO() throws Exception {
    String xml = getEncodedIdPMetadataXml();
    SecurityConfigurationDTO securityConfiguration = new SecurityConfigurationDTO();
    securityConfiguration.setBase64IdentityProviderXml(xml);
    securityConfiguration.setSamlConfiguration(new ApiSamlConfigurationDTO());
    securityConfiguration.setAdminEmails(Arrays.asList("admin@local.com"));
    return securityConfiguration;
  }

  private String getEncodedIdPMetadataXml() throws Exception {
    URL resource = getClass().getResource("/" + getClass().getSimpleName() + "/identity-provider-metadata.xml");
    String xmlMetadata = FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(xmlMetadata.getBytes(StandardCharsets.UTF_8));
  }
}
