/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class DashboardComponentsComponent
    extends BasePage
{
  private static final String ROOT = "#dashboard-components";

  public DashboardComponentsComponent() {
    super();
  }

  public Locator components() {
    return locator(ROOT + " .iq-dashboard-component-row");
  }

  public Locator component(int index) {
    return locator(ROOT + " .iq-dashboard-component-row").nth(index);
  }

  public Locator noDataMessage() {
    return locator(ROOT + " tbody .nx-table-row:last-child");
  }

  public Locator componentName(int index) {
    return component(index).locator("td:nth-child(1) .nx-truncate-ellipsis");
  }

  public Locator affectedApplications(int index) {
    return component(index).locator("td:nth-child(2)");
  }

  public Locator dashboardContainer() {
    return locator("#dashboard-container");
  }

  public Locator componentRiskHeading() {
    return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1));
  }

  public Locator componentRiskRoot() {
    return page.getByRole(AriaRole.MAIN);
  }

  public void clickComponent(int index) {
    component(index).click();
  }
}
