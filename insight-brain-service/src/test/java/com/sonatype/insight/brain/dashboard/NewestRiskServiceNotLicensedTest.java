/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.product.license.InvalidLicenseException;

import org.junit.Test;
import org.mockito.InjectMocks;

public class NewestRiskServiceNotLicensedTest
    extends AbstractServiceNotLicensedTest
{
  @InjectMocks
  private NewestRiskService newestRiskService;

  @Test(expected = InvalidLicenseException.class)
  public void testGetNewestRisks_Unlicensed() throws Exception {
    newestRiskService
        .getNewestRisks(null, null, null, null, null, null, null, null, DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0);
  }
}
