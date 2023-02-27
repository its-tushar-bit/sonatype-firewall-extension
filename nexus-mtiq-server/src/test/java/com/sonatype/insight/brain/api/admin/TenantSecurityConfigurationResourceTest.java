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

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.dto.SecurityConfigurationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSamlConfigurationDTO;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_PROVISIONING_PATH;
import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_SECURITY_CONFIG_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class TenantSecurityConfigurationResourceTest
    extends AbstractMultiTenantResourceTest
{
  protected HttpRequest restRequest(String path) {
    return super.adminRequest().path("api/").path(path);
  }

  @Before
  public void setUp() throws Exception {
    provisionTenant("tenant6").post();
  }

  @Test
  public void shouldUpdateSecurityConfigurationForATenant() throws Exception {
    HttpResponse response = updateSecurityConfiguration("tenant6").put();

    assertResponseStatus(204, response);
  }

  @Test
  public void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = updateSecurityConfiguration("global").put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = updateSecurityConfiguration("tenant7").put();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  private HttpRequest updateSecurityConfiguration(String tenant) throws Exception {
    HttpRequest request;

    if (tenant != null) {
      request = restRequest(ADMIN_TENANT_SECURITY_CONFIG_PATH).parameter(tenant);
    }
    else {
      request = restRequest();
    }

    String xml = getEncodedIdPMetadataXml();

    SecurityConfigurationDTO securityConfiguration = new SecurityConfigurationDTO();
    securityConfiguration.setBase64IdentityProviderXml(xml);
    securityConfiguration.setSamlConfiguration(new ApiSamlConfigurationDTO());
    securityConfiguration.setAdminEmails(Arrays.asList("admin@local.com"));

    return request.body(securityConfiguration);
  }

  private HttpRequest provisionTenant(String tenant) {
    if (tenant != null) {
      return restRequest(ADMIN_TENANT_PROVISIONING_PATH).parameter(tenant);
    }
    return restRequest();
  }

  private String getEncodedIdPMetadataXml() throws Exception {
    URL resource = getClass().getResource("/" + getClass().getSimpleName() + "/identity-provider-metadata.xml");
    String xmlMetadata = FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(xmlMetadata.getBytes(StandardCharsets.UTF_8));
  }
}
