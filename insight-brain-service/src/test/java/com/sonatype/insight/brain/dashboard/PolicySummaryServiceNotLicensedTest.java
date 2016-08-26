/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.product.license.InvalidLicenseException;

import org.junit.Test;
import org.mockito.InjectMocks;

public class PolicySummaryServiceNotLicensedTest
    extends AbstractServiceNotLicensedTest
{
  @InjectMocks
  private PolicySummaryService policySummaryService;

  @Test(expected = InvalidLicenseException.class)
  public void testGetPolicySummary_Unlicensed() throws Exception {
    policySummaryService.getPolicySummary(null, null, null, null, null, null);
  }
}
