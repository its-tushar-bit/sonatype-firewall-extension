/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantTestHelper.ConsumerWithException;

import org.junit.BeforeClass;

public class AbstractMultiTenantBrainServiceTest
    extends AbstractBrainServiceTest
{
  /**
   * beforeAllTests initializes MultiTenantBrainServiceTestHelper this sets up AbstractBrainServiceTest
   * ready for use with MTIQ registering before, after and afterClass handlers.
   */
  @BeforeClass
  public static void beforeAllTests() {
    MultiTenantBrainServiceTestHelper.setup();
  }

  /**
   * getTestTenant returns the pre provisioned tenant ready for use in test
   */
  protected static Tenant getTestTenant() {
    return MultiTenantBrainServiceTestHelper.getTestTenant();
  }

  protected static void testAsTenant(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAs(getTestTenant(), test);
  }

  protected static void testAsGlobal(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAs(Tenant.GLOBAL_TENANT, test);
  }

  protected String generateTestTenantName() {
    return TenantTestHelper.createTenantName(testName);
  }

  protected void setTenantSlug(String tenantSlug) {
    MultiTenantBrainServiceTestHelper.setTenantBySlug(tenantSlug);
  }

  protected HttpResponse provisionTenant(String tenantName) throws Exception {
    setTenantSlug(tenantName);
    return MultiTenantBrainServiceTestHelper.provisionTenant(this, tenantName);
  }
}
