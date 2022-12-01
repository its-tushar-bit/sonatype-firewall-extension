/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class MultiTenantTest
{
  @BeforeClass
  public static void classSetup() {
    TenantTestHelper.setMultiTenantModeForTest(true);
  }

  @AfterClass
  public static void tearDown() {
    TenantTestHelper.setMultiTenantModeForTest(false);
  }

  @Before
  public void setup() {
    TenantThreadLocal.setGlobalTenant();
  }
}
