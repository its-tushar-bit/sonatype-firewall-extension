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

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_SUPPORT_INFO_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class TenantSupportInfoResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Test
  public void shouldSend200_GetSupportInfo() throws Exception {
    String tenantSlug = generateTestTenantName();

    // Provisioning a tenant to evaluate Support Info endpoint
    provisionTenant(tenantSlug);

    HttpResponse response = getSupportInfoZip(tenantSlug).get();

    assertResponseStatus(200, response);
  }

  @Test
  public void shouldSend404_whenTenantDoesNotExist() throws Exception {
    HttpResponse response = getSupportInfoZip("non-existent").get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo("Tenant doesn't exist");
  }

  @Test
  public void shouldSend400_whenTenantIsGlobal() throws Exception {
    HttpResponse response = getSupportInfoZip("global").get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid tenant");
  }

  private HttpRequest getSupportInfoZip(String tenant) {
    return adminRestRequest(ADMIN_SUPPORT_INFO_PATH)
        .parameter(tenant);
  }
}
