/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;

import com.sonatype.insight.brain.product.license.InvalidLicenseException;

import org.junit.Test;
import org.mockito.InjectMocks;

public class DashboardFilterServiceNotLicensedTest
    extends AbstractServiceNotLicensedTest
{
  @InjectMocks
  private DashboardFilterService dashboardFilterService;
  
  @Test(expected = InvalidLicenseException.class)
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Unlicensed() throws Exception {
    dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(null);
  }
  
  @Test(expected = InvalidLicenseException.class)
  public void testGetFilterSummary_Unlicensed() throws Exception {
    dashboardFilterService.getFilterSummary(null, null, null, null, null, null);
  }
  
  @Test(expected = InvalidLicenseException.class)
  public void testDeleteDashboardFiltersForCurrentUserByFilterName_Unlicensed() {
    dashboardFilterService.deleteDashboardFiltersForCurrentUserByFilterName(null);
  }
  
  @Test(expected = InvalidLicenseException.class)
  public void testGetNamedDashboardFiltersForCurrentUser_Unlicensed() throws IOException {
    dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetActiveDashboardFilterForCurrentUser_Unlicensed() throws IOException {
    dashboardFilterService.getActiveDashboardFilterForCurrentUser();
  }
}
