/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.product.license.InvalidLicenseException;

import org.junit.Test;
import org.mockito.InjectMocks;

public class DashboardFilterServiceNotLicensedTest
    extends AbstractServiceNotLicensedTest
{
  @InjectMocks
  private DashboardFilterService dashboardFilterService;

  @Test(expected = InvalidLicenseException.class)
  public void testGetDashboardFilterForCurrentUser_Unlicensed() throws Exception {
    dashboardFilterService.getDashboardFilterForCurrentUser();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Unlicensed() throws Exception {
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(null);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testDeleteDashboardFilterForCurrentUser_Unlicensed() throws Exception {
    dashboardFilterService.deleteDashboardFilterForCurrentUser();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetFilterSummary_Unlicensed() throws Exception {
    dashboardFilterService.getFilterSummary(null, null, null, null, null);
  }
}
