/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.product.license.InvalidLicenseException;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class PolicySummaryServiceNotLicensedTest
    extends InjectedTest
{
  @Mock
  private DashboardUtils dashboardUtils;

  @InjectMocks
  private PolicySummaryService policySummaryService;

  @Before
  public void setup() {
    doThrow(new InvalidLicenseException("test")).when(dashboardUtils).validateDashboardLicensed();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetPolicySummary_Unlicensed() throws Exception {
    policySummaryService.getPolicySummary(null, null, null, null, null);
  }
}
