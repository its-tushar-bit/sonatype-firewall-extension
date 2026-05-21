/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Playwright page object for the Dashboard Components tab.
 */
public class DashboardComponentsComponent
    extends BasePage
{
  private static final String ROOT = "#dashboard-components";

  public DashboardComponentsComponent() {
    super();
  }

  // --------------- Row locators ---------------

  public Locator components() {
    return locator(ROOT + " .iq-dashboard-component-row");
  }

  public Locator component(int index) {
    return locator(ROOT + " .iq-dashboard-component-row").nth(index);
  }

  public Locator noDataMessage() {
    return locator(ROOT + " tbody .nx-table-row:last-child");
  }

  // --------------- Cell accessors ---------------

  public Locator componentName(int index) {
    return component(index).locator("td:nth-child(1) .nx-truncate-ellipsis");
  }

  public Locator affectedApplications(int index) {
    return component(index).locator("td:nth-child(2)");
  }

  // --------------- Actions ---------------

  public void clickComponent(int index) {
    component(index).click();
  }
}
