/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantTestHelper.ConsumerWithException;

import org.junit.Rule;
import org.junit.rules.TestName;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;

public abstract class AbstractMultiTenantTest
{
  @Rule
  public TestName testName = new TestName();

  @Rule
  public MultiTenantRule multiTenantRule = new MultiTenantRule();

  protected Tenant testAsNewTenant(ConsumerWithException<Tenant> test) {
    return TenantTestHelper.testAsNewTenant(testName, test);
  }

  protected void testAsGlobalTenant(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAsTenant(GLOBAL_TENANT, test);
  }
}
