/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_SCHEMA_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class TenantSchemaResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Test
  public void shouldGetTenantSchemaVersions() throws Exception {
    String tenantSlug = generateTestTenantName();

    // Provisioning a tenant to get schema versions
    provisionTenant(tenantSlug);

    HttpResponse response = callSchemaEndpoint(tenantSlug).get();
    String data = response.getBodyText();

    assertResponseStatus(200, response);
    assertThat(data).contains("insight_brain_ods")
        .contains("insight_brain_third_party_scans")
        .contains("insight_brain_aggregation")
        .contains("insight_brain_dm");
  }

  @Test
  public void shouldGetTenantSchemaVersions_forGlobalTenant() throws Exception {
    HttpResponse response = callSchemaEndpoint("global").get();
    String data = response.getBodyText();

    assertResponseStatus(200, response);
    assertThat(data).contains("insight_brain_ods")
        .contains("insight_brain_third_party_scans")
        .contains("insight_brain_aggregation")
        .contains("insight_brain_dm");
  }

  @Test
  public void shouldSend404_getTenantSchemaVersions_whenTenantDoesNotExist() throws Exception {
    HttpResponse response = callSchemaEndpoint("non-existent").get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  @Test
  public void shouldMigrateTenantSchema() throws Exception {
    String tenantSlug = generateTestTenantName();

    // Provisioning a tenant to execute the migration
    provisionTenant(tenantSlug);

    HttpResponse response = callSchemaEndpoint(tenantSlug).put();

    assertResponseStatus(204, response);
  }

  @Test
  public void shouldMigrateTenantSchema_forGlobalTenant() throws Exception {
    String tenantSlug = generateTestTenantName();

    // Provisioning a tenant to execute the migration
    provisionTenant(tenantSlug);

    HttpResponse response = callSchemaEndpoint("global").put();

    assertResponseStatus(204, response);
  }

  @Test
  public void shouldSend404_migrateSchema_whenTenantDoesNotExist() throws Exception {
    HttpResponse response = callSchemaEndpoint("non-existent").put();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  private HttpRequest callSchemaEndpoint(String tenant) {
    return adminRestRequest(ADMIN_TENANT_SCHEMA_PATH)
        .parameter(tenant);
  }
}
