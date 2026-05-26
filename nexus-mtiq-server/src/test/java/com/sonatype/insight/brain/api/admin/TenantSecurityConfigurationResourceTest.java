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
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.common.test.SlowTest;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_SECURITY_CONFIG_PATH;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SAML_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class TenantSecurityConfigurationResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Test
  public void shouldUpdateSamlConfigurationAndGrantAdminPermissions() throws Exception {
    String tenantSlug = generateTestTenantName();
    provisionTenant(tenantSlug, (tenant) -> {
      systemConfigurationPropertyDAO.set(SAML_ENABLED, Boolean.toString(true));
    });

    HttpResponse response = updateSamlConfigurationAndGrantAdminPermissions(tenantSlug).put();

    assertResponseStatus(204, response);
  }

  @Test
  public void updateSamlConfigurationAndGrantAdminPermissions_shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = updateSamlConfigurationAndGrantAdminPermissions("global").put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void updateSamlConfigurationAndGrantAdminPermissions_shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = updateSamlConfigurationAndGrantAdminPermissions("tenant7").put();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  @Test
  public void shouldUpdateSamlConfiguration() throws Exception {
    String tenantSlug = generateTestTenantName();
    provisionTenant(tenantSlug, (tenant) -> {
      systemConfigurationPropertyDAO.set(SAML_ENABLED, Boolean.toString(true));
    });

    HttpResponse response = updateSamlConfiguration(tenantSlug).put();

    assertResponseStatus(204, response);
  }

  @Test
  public void updateSamlConfiguration_shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = updateSamlConfiguration("global").put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void updateSamlConfiguration_shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = updateSamlConfiguration("tenant7").put();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  @Test
  public void shouldGrantAdminPermissionForAdmins() throws Exception {
    String tenantSlug = generateTestTenantName();
    provisionTenant(tenantSlug);

    HttpResponse response = grantAdminPermissionForAdmins(tenantSlug).put();

    assertResponseStatus(204, response);
  }

  @Test
  public void grantAdminPermissionForAdmins_shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = grantAdminPermissionForAdmins("global").put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void grantAdminPermissionForAdmins_shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = grantAdminPermissionForAdmins("tenant7").put();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  private HttpRequest updateSamlConfigurationAndGrantAdminPermissions(String tenant) throws Exception {
    HttpRequest request = adminRestRequest(ADMIN_TENANT_SECURITY_CONFIG_PATH)
        .parameter(tenant)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt("local/"));

    SecurityConfigurationDTO securityConfiguration = createSecurityConfigurationDTO();

    return request.body(securityConfiguration);
  }

  private HttpRequest updateSamlConfiguration(String tenant) throws Exception {
    HttpRequest request = adminRestRequest(
        ADMIN_TENANT_SECURITY_CONFIG_PATH + TenantSecurityConfigurationResource.UPDATE_SAML_CONFIGURATION_PATH)
            .parameter(tenant)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt("local/"));

    SecurityConfigurationDTO securityConfiguration = createSecurityConfigurationDTO();

    return request.body(securityConfiguration);
  }

  private HttpRequest grantAdminPermissionForAdmins(String tenant) throws Exception {
    HttpRequest request = adminRestRequest(
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
