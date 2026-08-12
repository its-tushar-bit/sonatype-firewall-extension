/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.Collections;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_CONFIG_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class TenantConfigurationResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private SystemConfigurationPropertyDAO dao;

  @Before
  public void before() {
    dao = lookup(SystemConfigurationPropertyDAO.class);
  }

  @Test
  public void shouldGetPropertiesConfiguration() throws Exception {
    String tenantSlug = getTestTenant().tenantSlug;

    String expectedProperty = "baseUrl";
    String expectedValue = "http://baseUrl/";
    Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

    dao.set(SystemConfigurationProperty.BASE_URL, expectedValue);

    HttpResponse response = callConfigurationEndpoint(tenantSlug)
        .query("property", expectedProperty)
        .get();

    assertResponseStatus(200, response);
    Map<String, Object> result = response.getBody(Map.class);
    assertThat(result).isEqualTo(propertyConfiguration);
  }

  @Test
  public void shouldGetPropertiesConfiguration_whenTenantIsGlobal() throws Exception {
    String expectedProperty = "baseUrl";
    String expectedValue = "http://baseUrl/";
    Map<String, Object> propertyConfiguration = Collections.singletonMap(expectedProperty, expectedValue);

    testAsGlobal(global -> {
      dao.set(SystemConfigurationProperty.BASE_URL, expectedValue);
    });

    HttpResponse response = callConfigurationEndpoint("global")
        .query("property", expectedProperty)
        .get();

    assertResponseStatus(200, response);
    Map<String, Object> result = response.getBody(Map.class);
    assertThat(result).isEqualTo(propertyConfiguration);
  }

  @Test
  public void shouldSend404_getPropertiesConfiguration_whenTenantDoesntExist() throws Exception {
    HttpResponse response = callConfigurationEndpoint("non-existent")
        .query("property", "baseUrl")
        .get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant does not exist");
  }

  @Test
  public void shouldSend400_getPropertiesConfiguration_NotConfigurableProperty() throws Exception {
    String tenantSlug = getTestTenant().tenantSlug;

    HttpResponse response = callConfigurationEndpoint(tenantSlug)
        .query("property", "forceBaseUrl")
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Property forceBaseUrl is not configurable.");
  }

  @Test
  public void shouldSetPropertiesConfiguration() throws Exception {
    String tenantSlug = getTestTenant().tenantSlug;

    Map<String, Object> propertyConfiguration = Collections.singletonMap("baseUrl", "http://127.0.0.1:8070");

    HttpResponse response = callConfigurationEndpoint(tenantSlug)
        .body(propertyConfiguration)
        .put();

    assertResponseStatus(204, response);
  }

  @Test
  public void setPropertiesConfiguration_whenTenantIsGlobal() throws Exception {
    Map<String, Object> propertyConfiguration = Collections.singletonMap("baseUrl", "http://127.0.0.1:8070");

    HttpResponse response = callConfigurationEndpoint("global")
        .body(propertyConfiguration)
        .put();

    assertResponseStatus(204, response);
  }

  @Test
  public void shouldSend404_setPropertiesConfiguration_whenTenantDoesntExist() throws Exception {
    HttpResponse response = callConfigurationEndpoint("non-existent").put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant does not exist");
  }

  @Test
  public void shouldSend400_setPropertiesConfiguration_NoConfigProvided() throws Exception {
    String tenantSlug = generateTestTenantName();

    // Provisioning a tenant to set properties configuration
    provisionTenant(tenantSlug);

    Map<String, Object> propertyConfiguration = Collections.emptyMap();

    HttpResponse response = callConfigurationEndpoint(tenantSlug)
        .body(propertyConfiguration)
        .put();

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
        .body(propertyConfiguration)
        .put();

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
        .body(propertyConfiguration)
        .put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Invalid property value type for baseUrl, expected class java.lang.String but got class java.lang.Boolean.");
  }

  @Test
  public void shouldDeletePropertiesConfiguration() throws Exception {
    String tenantSlug = getTestTenant().tenantSlug;

    HttpResponse response = callConfigurationEndpoint("global")
        .query("property", "baseUrl")
        .get();

    assertResponseStatus(200, response);
    Map<String, Object> result = response.getBody(Map.class);
    String globalTenantBaseUrl = String.valueOf(result.get("baseUrl"));

    dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");

    response = callConfigurationEndpoint(tenantSlug)
        .query("property", "baseUrl")
        .delete();

    assertResponseStatus(204, response);
    assertThat(dao.getByName(SystemConfigurationProperty.BASE_URL).getValue()).isEqualTo(globalTenantBaseUrl);
  }

  @Test
  public void shouldDeletePropertiesConfiguration_whenTenantIsGlobal() throws Exception {
    testAsGlobal(global -> {
      dao.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");
    });

    HttpResponse response = callConfigurationEndpoint("global")
        .query("property", "baseUrl")
        .delete();

    assertResponseStatus(204, response);
    testAsGlobal(global -> {
      assertThat(dao.getByName(SystemConfigurationProperty.BASE_URL)).isNull();
    });
  }

  @Test
  public void shouldSend404_deletePropertiesConfiguration_whenTenantDoesntExist() throws Exception {
    HttpResponse response = callConfigurationEndpoint("non-existent")
        .query("property", "baseUrl")
        .delete();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant does not exist");
  }

  @Test
  public void shouldSend400_deletePropertiesConfiguration_NotConfigurableProperty() throws Exception {
    String tenantSlug = getTestTenant().tenantSlug;

    HttpResponse response = callConfigurationEndpoint(tenantSlug)
        .query("property", "forceBaseUrl")
        .delete();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Property forceBaseUrl is not configurable.");
  }

  private HttpRequest callConfigurationEndpoint(String tenant) {
    return adminRestRequest(ADMIN_CONFIG_PATH)
        .parameter(tenant);
  }
}
