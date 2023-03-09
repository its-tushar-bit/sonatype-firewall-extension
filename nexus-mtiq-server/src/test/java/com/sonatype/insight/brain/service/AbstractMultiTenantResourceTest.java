/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_PROVISIONING_PATH;

public abstract class AbstractMultiTenantResourceTest
    extends AbstractMultiTenantBrainServiceTest
{
  private final TestRestTenantUtil tenantUtil = new TestRestTenantUtil();

  protected String generateTestTenantName() {
    return TenantTestHelper.createTenantName(testName);
  }

  public void setTenantSlug(String tenantSlug) {
    tenantUtil.setTenantSlug(tenantSlug);
  }

  public HttpResponse provisionTenant(String tenantName) throws Exception {
    setTenantSlug(tenantName);
    return adminRequest()
        .path("api/")
        .path(ADMIN_TENANT_PROVISIONING_PATH)
        .parameter(tenantName)
        .post();
  }

  @Override
  protected List<Module> getBrainModules() {
    List<Module> brainModules = super.getBrainModules();
    brainModules.add(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(TenantUtil.class).toInstance(tenantUtil);
          }
        }
    );
    return brainModules;
  }

  /**
   * This class is bound instead of the default <code>TenantUtil</code> to allow overriding of the TenantUrlFilter
   * behaviour. On receipt of any rest request the <code>TenantUrlFilter</code> will now always return the currently
   * set Tenant as the tenantSlug. Without this override tests run on localhost only and therefore the default
   * TenantUrlFilter is unable to determine a valid tenantSlug typically derived from the address.
   */
  private static class TestRestTenantUtil
      extends TenantUtil
  {
    private String tenantSlug;

    @Override
    public String getTenantName(final String serverName) {
      if (tenantSlug == null) {
        return super.getTenantName(serverName);
      }
      // Whatever the current tenant has been set to will be the assumed tenant
      return tenantSlug;
    }

    public void setTenantSlug(String tenantSlug) {
      this.tenantSlug = tenantSlug;
    }
  }
}
