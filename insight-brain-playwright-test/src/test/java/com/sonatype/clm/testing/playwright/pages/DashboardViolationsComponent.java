/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

public class DashboardViolationsComponent
    extends BasePage
{
  private static final String ROOT = "#dashboard-violations";

  private static final Locator.GetByRoleOptions NEXT_PAGE_OPTS =
      new Locator.GetByRoleOptions().setName("next page");

  private static final Locator.GetByRoleOptions PREVIOUS_PAGE_OPTS =
      new Locator.GetByRoleOptions().setName("previous page");

  public DashboardViolationsComponent() {
    super();
  }

  public Locator container() {
    return locator(ROOT);
  }

  public enum SortableColumn
  {
    THREAT("Threat"),
    POLICY("Policy"),
    APPLICATION("Application"),
    AGE("Age");

    private final String label;

    SortableColumn(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  public Locator violations() {
    return locator(ROOT + " .iq-dashboard-violation");
  }

  public Locator violation(int index) {
    return locator(ROOT + " .iq-dashboard-violation").nth(index);
  }

  public Locator noDataMessage() {
    return locator(ROOT + " .iq-dashboard-violation-entries .nx-table-row:last-child");
  }

  public Locator threatNumber(int index) {
    return violation(index).locator(".nx-threat-number");
  }

  public Locator componentName(int index) {
    return violation(index).locator("td:nth-child(4)");
  }

  public Locator policyName(int index) {
    return violation(index).locator(".iq-policy-cell");
  }

  public Locator applicationName(int index) {
    return violation(index).locator("td:nth-child(3)");
  }

  public Locator reportTime(int index) {
    return violation(index).locator("td:nth-child(5)");
  }

  public Locator headerCell(SortableColumn column) {
    return container().locator("thead th")
        .filter(
            new Locator.FilterOptions().setHasText(column.label()));
  }

  public Locator headerButton(SortableColumn column) {
    return headerCell(column).getByRole(AriaRole.BUTTON);
  }

  public Locator allHeaders() {
    return container().locator("thead th");
  }

  public Locator paginatorBar() {
    return container().getByRole(AriaRole.NAVIGATION);
  }

  public Locator paginatorNextButton() {
    return container().getByRole(AriaRole.BUTTON, NEXT_PAGE_OPTS);
  }

  public Locator paginatorPreviousButton() {
    return container().getByRole(AriaRole.BUTTON, PREVIOUS_PAGE_OPTS);
  }

  public void goToNextPage() {
    paginatorNextButton().click();
  }

  public void goToPreviousPage() {
    paginatorPreviousButton().click();
  }

  public void clickViolation(int index) {
    violation(index).click();
  }

  public void clickHeader(SortableColumn column) {
    headerButton(column).click();
  }

  public void waitForResults(long timeoutMs) {
    violations().first()
        .waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(timeoutMs));
  }
}
