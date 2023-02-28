/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_LICENSE_PATH;
import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_PROVISIONING_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class TenantLicenseResourceTest
    extends AbstractMultiTenantResourceTest
{
  protected HttpRequest restRequest(String path) {
    return super.adminRequest().path("api/").path(path);
  }

  private String tenantSlug;

  @Before
  public void setUp() throws Exception {
    tenantSlug = generateTestTenantName();
    provisionTenant(tenantSlug).post();
  }

  @Test
  public void shouldUpdateLicense() throws Exception {
    HttpResponse response = updateLicense(tenantSlug).put();

    assertResponseStatus(204, response);
  }

  @Test
  public void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = updateLicense("global").put();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void shouldSend404_whenTenantDoesntExist() throws Exception {
    HttpResponse response = updateLicense("tenant4").put();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  private HttpRequest updateLicense(String tenant) {
    HttpRequest request;

    if (tenant != null) {
      request = restRequest(ADMIN_TENANT_LICENSE_PATH).parameter(tenant);
    }
    else {
      request = restRequest();
    }
    request.part("file", "sonatype.lic", new byte[1]);
    return request;
  }

  private HttpRequest provisionTenant(String tenant) {
    if (tenant != null) {
      return restRequest(ADMIN_TENANT_PROVISIONING_PATH).parameter(tenant);
    }
    return restRequest();
  }
}
