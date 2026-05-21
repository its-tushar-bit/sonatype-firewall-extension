/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the Enterprise Reporting landing page
 * ({@code enterpriseReporting/EnterpriseReportingLandingPage.jsx}).
 */
public class EnterpriseReportingPage
    extends BasePage
{
  private static final String ROOT = "#enterprise-reporting-landing-page";

  public EnterpriseReportingPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/enterpriseReportingLandingPage";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageHeading() {
    return locator(ROOT + " #enterprise-reporting-landing-page-heading");
  }

  public Locator enterpriseDashboardsSectionHeading() {
    return container()
        .locator(".iq-enterprise-reporting__dashboard-grouping__title")
        .filter(new Locator.FilterOptions().setHasText("Enterprise Dashboards"));
  }

  public Locator enterpriseDashboardCard(String dashboardId) {
    return locator(ROOT + " #enterprise-reporting-dashboard-" + dashboardId);
  }
}
