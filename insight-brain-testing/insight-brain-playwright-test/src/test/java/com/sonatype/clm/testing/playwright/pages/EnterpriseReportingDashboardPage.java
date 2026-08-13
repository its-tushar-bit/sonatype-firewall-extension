/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page object for the Enterprise Reporting dashboard embed page.
 * Root element: {@code #enterprise-reporting-dashboard-page}.
 */
public class EnterpriseReportingDashboardPage
    extends BasePage
{
  private static final String ROOT = "#enterprise-reporting-dashboard-page";

  public EnterpriseReportingDashboardPage() {
  }

  /**
   * Hash-fragment URL for the Enterprise Reporting Dashboard page for a given dashboard ID.
   * Route: {@code enterpriseReportingDashboard} → {@code /enterpriseReportingDashboard/{id}}.
   *
   * @param dashboardId the dashboard's identifier (e.g., {@code "sbom-scorecard"})
   */
  public static String url(String dashboardId) {
    return "/assets/index.html#/enterpriseReportingDashboard/" + dashboardId;
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return locator(ROOT).getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }
}
