/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.tenancy.TenantTestHelper;

public abstract class AbstractMultiTenantResourceTest
    extends AbstractMultiTenantBrainServiceTest
{
  protected String generateTestTenantName() {
    return TenantTestHelper.createTenantName(testName);
  }
}
