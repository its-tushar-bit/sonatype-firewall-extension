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

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_SUPPORT_INFO_PATH;
import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_PROVISIONING_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class SupportInfoResourceTest
    extends AbstractMultiTenantResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.adminRequest().path("api/");
  }

  @Test
  public void shouldSend200_GetSupportInfo() throws Exception {
    // Provisioning a tenant to evaluate Support Info endpoint
    String tenantSlug = generateTestTenantName();
    restRequest().path(ADMIN_TENANT_PROVISIONING_PATH).parameter(tenantSlug).post();
    HttpResponse response = restRequest().path(ADMIN_SUPPORT_INFO_PATH)
        .parameter(tenantSlug).get();
    //
    assertResponseStatus(200, response);
  }

  @Test
  public void shouldSend404_whenTenantDoesNotExist() throws Exception {
    HttpResponse response = restRequest().path(ADMIN_SUPPORT_INFO_PATH)
        .parameter("non-existent").get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant does not exist");
  }

  @Test
  public void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = restRequest().path(ADMIN_SUPPORT_INFO_PATH)
        .parameter("global").get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid Tenant");
  }
}
