/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DashboardServiceNotLicensedTest
    extends InjectedTest
{

  @InjectMocks
  private DashboardService dashboardService;

  @Mock
  private CLMLicenseManager licenseManager;

  @Before
  public void setup() {
    when(licenseManager.hasDashboard()).thenReturn(false);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetApplicationRisks_Unlicensed() {
    dashboardService.getApplicationRisks(null, Collections.singleton(DevelopStageType.ID), null, null, null, 0);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetComponentRisks_Unlicensed() {
    dashboardService.getComponentRisks(null, null, null, null, null, 0);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetDashboardFilterForCurrentUser_Unlicensed() throws Exception {
    dashboardService.getDashboardFilterForCurrentUser();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testCreateOrUpdateDashboardFilterForCurrentUser_Unlicensed() throws Exception {
    dashboardService.createOrUpdateDashboardFilterForCurrentUser(null);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testDeleteDashboardFilterForCurrentUser_Unlicensed() throws Exception {
    dashboardService.deleteDashboardFilterForCurrentUser();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetFilterSummary_Unlicensed() throws Exception {
    dashboardService.getFilterSummary(null, null, null, null, null);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetNewestRisks_Unlicensed() throws Exception {
    dashboardService.getNewestRisks(null, null, null, null, null, 0);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetComponentSummary_Unlicensed() throws Exception {
    dashboardService.getComponentSummary(null, null, null);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testGetPolicySummary_Unlicensed() throws Exception {
    dashboardService.getPolicySummary(null, null, null, null, null);
  }
}
