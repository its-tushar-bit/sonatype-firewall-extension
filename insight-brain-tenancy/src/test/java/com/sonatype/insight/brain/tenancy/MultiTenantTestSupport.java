/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import com.sonatype.insight.brain.tenancy.TenantTestHelper.ConsumerWithException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;

/**
 * This class is to support multi-tenant tests OUTSIDE of the nexus-mtiq-server module. Specifically this calls
 * {@link TenantTestHelper#resetAfterTest()} which puts it back into single-tenant mode which is not wanted in the full
 * multi-tenant tests. Tests in the `nexus-mtiq-server` module should use `AbstractMultiTenantTest`
 */
public abstract class MultiTenantTestSupport
{
  @Rule
  public TestName testName = new TestName();

  @Before
  public void setup() {
    TenantTestHelper.initMultiTenantMode();
  }

  @After
  public void resetAfterTest() {
    TenantTestHelper.resetAfterTest();
  }

  protected Tenant testAsNewTenant(ConsumerWithException<Tenant> test) {
    return TenantTestHelper.testAsNewTenant(testName, test);
  }

  protected void testAsSingleTenant(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAs(SINGLE_TENANT, test);
  }

  protected void testAsGlobalTenant(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAs(GLOBAL_TENANT, test);
  }
}
