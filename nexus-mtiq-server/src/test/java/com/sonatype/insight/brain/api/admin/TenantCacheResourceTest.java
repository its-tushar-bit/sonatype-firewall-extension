/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import javax.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.authorization.AuthorizationTestHelper;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantCacheResourceTest
    extends AbstractMultiTenantResourceTest
{
  private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  protected HttpRequest restRequest(String path) {
    return super.adminRequest().path("api/").path(path);
  }

  @Test
  public void shouldSend200_whenStatisticsRequested() throws Exception {
    String tenantSlug = generateTestTenantName();
    provisionTenant(tenantSlug);

    HttpResponse response = requestTenantStatistics(tenantSlug).get();
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void statisticShowNoCachingWhenCachingDisabled_whenStatisticsRequestedFromDifferentTenants() throws Exception {
    Tenant tenant1 = getTestTenant();
    provisionTenant(tenant1.tenantSlug);
    String tenantSlug2 = generateTestTenantName();
    provisionTenant(tenantSlug2);

    TenantTestHelper.testAs(tenant1, tenant -> organizationDAO.getAll());

    TestCacheStatistics stats1 = getCacheStatistics(tenant1.tenantSlug);
    TestCacheStatistics stats2 = getCacheStatistics(tenantSlug2);

    assertThat(stats1.totalHitCount).isEqualTo(0);
    assertThat(stats2.totalHitCount).isEqualTo(0);

    TenantTestHelper.testAs(tenant1, tenant -> organizationDAO.getAll());

    stats1 = getCacheStatistics(tenant1.tenantSlug);
    assertThat(stats1.totalHitCount).isEqualTo(0);
  }

  private TestCacheStatistics getCacheStatistics(final String tenant) throws Exception {
    HttpResponse response = requestTenantStatistics(tenant).get();
    return mapper.readValue(response.getBodyText(), TestCacheStatistics.class);
  }

  private HttpRequest requestTenantStatistics(final String tenantSlug) throws Exception {
    return restRequest(AdminApiPaths.ADMIN_TENANT_CACHE_PATH)
        .parameter(tenantSlug)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt());
  }

  private static class TestCacheStatistics
  {
    public long totalHitCount;
  }
}
