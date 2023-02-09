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

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_PROVISION_TENANT_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class TenantProvisioningResourceTest
    extends AbstractMultiTenantResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.adminRequest().path("api/").path(ADMIN_PROVISION_TENANT_PATH);
  }

  @Test
  public void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = provisionTenant("global").post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void shouldProvisionTenant() throws Exception {
    HttpResponse response = provisionTenant("tenant1").post();

    assertResponseStatus(204, response);
  }

  @Test
  public void shouldSend400_whenTenantAlreadyExists() throws Exception {
    HttpResponse response = provisionTenant("tenant2").post();
    assertResponseStatus(204, response);

    response = provisionTenant("tenant2").post();
    assertResponseStatus(409, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant already exists");
  }

  @Test
  public void shouldSend400_whenTenantIsEmpty() throws Exception {
    HttpResponse response = provisionTenant("").post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  @Test
  public void shouldSend400_whenTenantIsNull() throws Exception {
    HttpResponse response = provisionTenant(null).post();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  private HttpRequest provisionTenant(String tenant) {
    if (tenant != null) {
      return restRequest().query(String.format("tenant=%s", tenant));
    }
    return restRequest();
  }
}
