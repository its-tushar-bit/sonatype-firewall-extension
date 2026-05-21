/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the Operational Reporting landing page
 * ({@code operationalReporting/OperationalReportingLandingPage.jsx}).
 * <p>
 * The IQ frontend renders this landing page (and its sidebar entry
 * {@code #operational-reporting-button}) when the license does NOT include the HDS-controlled
 * {@code integrated-enterprise-reporting} feature. The default test license falls into this
 * branch, which is why functional tests target Operational rather than Enterprise Reporting.
 */
public class OperationalReportingPage
    extends BasePage
{
  /** Stable container id from {@code <NxPageMain id="operational-reporting-landing-page">}. */
  private static final String ROOT = "#operational-reporting-landing-page";

  public OperationalReportingPage() {
    super();
  }

  // --------------- URL helpers ---------------

  /**
   * Hash route registered in {@code operationalReporting/route.js} as state
   * {@code operationalReporting} → {@code /operationalReporting}.
   */
  public static String url() {
    return "/assets/index.html#/operationalReporting";
  }

  // --------------- Locators ---------------

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageTitle() {
    return locator(ROOT + " #operational-reporting-landing-page-title");
  }

  public Locator pageHeading() {
    return locator(ROOT + " #operational-reporting-landing-page-heading");
  }

  public Locator pageDescription() {
    return locator(ROOT + " #operational-reporting-landing-page-description");
  }

}
