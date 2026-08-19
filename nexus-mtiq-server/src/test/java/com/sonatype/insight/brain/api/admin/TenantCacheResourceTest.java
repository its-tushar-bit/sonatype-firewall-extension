/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentCache;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TenantCacheResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private RepositoryIdentifiedComponentCache repositoryIdentifiedComponentCache;

  private RoleDAO roleDAO;

  private final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Before
  public void before() {
    repositoryIdentifiedComponentCache = lookup(RepositoryIdentifiedComponentCache.class);
    roleDAO = lookup(RoleDAO.class);
  }

  @Test
  public void shouldSend200_whenStatisticsRequested() throws Exception {
    HttpResponse response = requestTenantStatistics(getTestTenant().tenantSlug).get();
    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void statisticShowNoCachingWhenCachingDisabled_whenStatisticsRequestedFromDifferentTenants() throws Exception {
    Tenant tenant1 = getTestTenant();
    String tenantSlug2 = generateTestTenantName();

    provisionTenant(tenant1.tenantSlug);
    provisionTenant(tenantSlug2);

    long initialCount1 = getCacheStatistics(tenant1.tenantSlug).totalHitCount;
    long initialCount2 = getCacheStatistics(tenantSlug2).totalHitCount;

    // Use a DAO for an entity that is not cached (Role) and verify that the cache hits does not increase
    TenantTestHelper.testAsTenant(tenant1, tenant -> roleDAO.getAll());

    TestCacheStatistics stats1 = getCacheStatistics(tenant1.tenantSlug);
    TestCacheStatistics stats2 = getCacheStatistics(tenantSlug2);

    assertThat(stats1.totalHitCount).isEqualTo(initialCount1);
    assertThat(stats2.totalHitCount).isEqualTo(initialCount2);
  }

  @Test
  public void shouldSendPerTenantData_whenStatisticsRequestedFromDifferentTenants() throws Exception {
    Tenant tenant1 = getTestTenant();
    provisionTenant(tenant1.tenantSlug);
    String tenantSlug2 = generateTestTenantName();
    provisionTenant(tenantSlug2);

    // Capture initial cache counts before the test operations
    long initialCount1 = getCacheStatistics(tenant1.tenantSlug).totalHitCount;
    long initialCount2 = getCacheStatistics(tenantSlug2).totalHitCount;

    // Use RepositoryIdentifiedComponentCache which actually tracks cache statistics
    // First put an entry in the cache, then get it to generate cache hits
    String testHash = "test-hash-" + System.currentTimeMillis();
    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v", null, null);

    TenantTestHelper.testAsTenant(tenant1, tenant -> {
      repositoryIdentifiedComponentCache.put(testHash, componentId);
      // Getting an existing entry should register a cache hit
      repositoryIdentifiedComponentCache.get(testHash);
    });

    TestCacheStatistics stats1 = getCacheStatistics(tenant1.tenantSlug);
    TestCacheStatistics stats2 = getCacheStatistics(tenantSlug2);

    // tenant1 should have increased cache hits from the get() call, tenant2 should be unchanged
    assertThat(stats1.totalHitCount).isGreaterThan(initialCount1);
    assertThat(stats2.totalHitCount).isEqualTo(initialCount2);

    long countAfterFirstCall = stats1.totalHitCount;

    TenantTestHelper.testAsTenant(tenant1, tenant -> repositoryIdentifiedComponentCache.get(testHash));

    stats1 = getCacheStatistics(tenant1.tenantSlug);
    // tenant1 should have more cache hits after second get() call
    assertThat(stats1.totalHitCount).isGreaterThan(countAfterFirstCall);
  }

  private TestCacheStatistics getCacheStatistics(final String tenant) throws Exception {
    HttpResponse response = requestTenantStatistics(tenant).get();
    return mapper.readValue(response.getBodyText(), TestCacheStatistics.class);
  }

  private HttpRequest requestTenantStatistics(final String tenantSlug) {
    return adminRestRequest(AdminApiPaths.ADMIN_TENANT_CACHE_PATH)
        .parameter(tenantSlug);
  }

  private static class TestCacheStatistics
  {
    public long totalHitCount;
  }
}
