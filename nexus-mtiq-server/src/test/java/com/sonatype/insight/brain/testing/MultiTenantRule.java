/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import org.junit.rules.ExternalResource;

public class MultiTenantRule
    extends ExternalResource
{
  @Override
  protected void before() throws Throwable {
    TenantTestHelper.initMultiTenantMode();
  }
}
