/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the Legal Applications list page
 * ({@code legal.applicationsDashboard} route).
 * <p>
 * Route: {@code /legal/applicationsDashboard}
 * Root element: {@code #legal-dashboard-container}
 */
public class LegalApplicationsPage
    extends BasePage
{
  private static final String ROOT = "#legal-dashboard-container";

  private static final String TABLE = "#legal-dashboard-applications-table";

  public LegalApplicationsPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/legal/applicationsDashboard";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator title() {
    return locator(ROOT + " h1.nx-h1");
  }

  public Locator applicationsTable() {
    return locator(TABLE);
  }

  public Locator tableBodyRows() {
    return locator(TABLE + " tbody tr");
  }

  public Locator columnHeader(String columnText) {
    return locator(TABLE + " th.nx-cell--header")
        .filter(new Locator.FilterOptions().setHasText(columnText));
  }

  public Locator appRow(String appName) {
    return tableBodyRows().filter(new Locator.FilterOptions().setHasText(appName));
  }

  public void clickAppRow(String appName) {
    appRow(appName).click();
  }
}
