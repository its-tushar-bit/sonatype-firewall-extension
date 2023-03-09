/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_SCHEMA_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class TenantSchemaResourceTest
    extends AbstractMultiTenantResourceTest
{
  protected HttpRequest restRequest(String path) {
    return super.adminRequest().path("api/").path(path);
  }

  @Test
  public void shouldGetTenantSchemaVersions() throws Exception {
    String tenantSlug = generateTestTenantName();

    // Provisioning a tenant to get schema versions
    provisionTenant(tenantSlug);

    HttpResponse response = getSchemaVersions(tenantSlug).get();
    String data = response.getBodyText();

    assertResponseStatus(200, response);
    assertThat(data).contains("insight_brain_ods");
    assertThat(data).contains("insight_brain_third_party_scans");
    assertThat(data).contains("insight_brain_aggregation");
    assertThat(data).contains("insight_brain_dm");
  }

  @Test
  public void shouldSend404_whenTenantDoesNotExist() throws Exception {
    HttpResponse response = getSchemaVersions("non-existent").get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  @Test
  public void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = getSchemaVersions("global").get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  private HttpRequest getSchemaVersions(String tenant) {
    if (tenant != null) {
      return restRequest(ADMIN_TENANT_SCHEMA_PATH).parameter(tenant);
    }
    return restRequest();
  }
}
