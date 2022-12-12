/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import org.junit.Before;

public abstract class MultiTenantTest
{
  @Before
  public void setup() {
    TenantTestHelper.initMultiTenantMode();
  }
}
