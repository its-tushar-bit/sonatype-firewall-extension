/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_PATH;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class TenantResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Test
  public void getAllTenants_shouldReturnTenantsList() throws Exception {
    String tenant1Slug = generateTestTenantName();
    provisionTenant(tenant1Slug);
    String tenant2Slug = generateTestTenantName();
    provisionTenant(tenant2Slug);

    HttpResponse response = getAllTenantsNames().get();
    List<String> tenantsList = response.getBodyList();

    assertResponseStatus(200, response);
    // the 2 new tenants + tenant created by test setup in super class
    assertThat(tenantsList).hasSize(3);
    assertThat(tenantsList).contains(tenant1Slug, tenant2Slug);
  }

  private HttpRequest getAllTenantsNames() {
    return adminRestRequest(ADMIN_PATH + TenantResource.LIST_TENANTS)
        .query("tenant=global");
  }
}
