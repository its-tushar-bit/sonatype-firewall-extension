/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.Collections;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.authorization.AuthorizationTestHelper;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_CONFIG_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class TenantConfigurationResourceTest
    extends AbstractMultiTenantResourceTest
{
  protected HttpRequest restRequest(String path) {
    return super.adminRequest().path("api/").path(path);
  }

  @Test
  public void shouldSetPropertiesConfiguration() throws Exception {
    String tenantSlug = generateTestTenantName();

    // Provisioning a tenant to set properties configuration
    provisionTenant(tenantSlug);

    Map<String, Object> propertyConfiguration = Collections.singletonMap("baseUrl", "http://127.0.0.1:8070");

    HttpResponse response = callConfigurationEndpoint(tenantSlug)
        .body(propertyConfiguration).put();

    assertResponseStatus(204, response);
  }

  @Test
  public void shouldSend400_setPropertiesConfiguration_whenTenantIsGlobal() throws Exception {
    HttpResponse response = callConfigurationEndpoint("global").put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void shouldSend404_setPropertiesConfiguration_whenTenantDoesntExist() throws Exception {
    HttpResponse response = callConfigurationEndpoint("non-existent").put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  @Test
  public void shouldSend400_setPropertiesConfiguration_NoConfigProvided() throws Exception {
    String tenantSlug = generateTestTenantName();

    // Provisioning a tenant to set properties configuration
    provisionTenant(tenantSlug);

    Map<String, Object> propertyConfiguration = Collections.emptyMap();

    HttpResponse response = callConfigurationEndpoint(tenantSlug)
        .body(propertyConfiguration).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("No configuration was specified.");
  }

  @Test
  public void shouldSend400_setPropertiesConfiguration_NotConfigurableProperty() throws Exception {
    String tenantSlug = generateTestTenantName();

    // Provisioning a tenant to set properties configuration
    provisionTenant(tenantSlug);

    Map<String, Object> propertyConfiguration = Collections.singletonMap("forceBaseUrl", true);

    HttpResponse response = callConfigurationEndpoint(tenantSlug)
        .body(propertyConfiguration).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Property forceBaseUrl is not configurable.");
  }

  @Test
  public void shouldSend400_setPropertiesConfiguration_InvalidValue() throws Exception {
    String tenantSlug = generateTestTenantName();

    // Provisioning a tenant to set properties configuration
    provisionTenant(tenantSlug);

    Map<String, Object> propertyConfiguration = Collections.singletonMap("baseUrl", true);

    HttpResponse response = callConfigurationEndpoint(tenantSlug)
        .body(propertyConfiguration).put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Invalid value for baseUrl, expected class java.lang.String, but got class java.lang.Boolean.");
  }

  private HttpRequest requestFeatures(String tenant, String path) {
    if (tenant != null) {
      return restRequest(ADMIN_CONFIG_PATH).path(path).parameter(tenant);
    }
    return restRequest();
  }

  private HttpRequest callConfigurationEndpoint(String tenant) throws Exception {
    if (tenant != null) {
      return restRequest(ADMIN_CONFIG_PATH)
          .parameter(tenant)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt());
    }
    return restRequest();
  }
}
