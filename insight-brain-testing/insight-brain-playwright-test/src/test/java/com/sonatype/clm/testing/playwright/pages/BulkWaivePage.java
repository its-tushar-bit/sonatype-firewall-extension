/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class BulkWaivePage
    extends BasePage
{
  private static final String ROOT = "#bulk-waive-page-container";

  private static final String TABLE = ROOT + " #bulk-waive-table";

  public BulkWaivePage() {
    super();
  }

  public static String url(String appPublicId, String scanId) {
    return "/assets/index.html#/applicationReport/" + appPublicId + "/" + scanId + "/bulkWaive";
  }

  public static String cdpUrl(String appPublicId, String scanId, String hash) {
    return "/assets/index.html#/applicationReport/" + appPublicId + "/" + scanId + "/" + hash + "/bulkWaive";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator table() {
    return locator(TABLE);
  }

  public Locator threatColumnHeader() {
    return table().locator("th").filter(new Locator.FilterOptions().setHasText("Threat"));
  }

  public Locator policyColumnHeader() {
    return table().locator("th").filter(new Locator.FilterOptions().setHasText("Policy"));
  }

  public Locator constraintOrComponentColumnHeader() {
    return table().locator("th")
        .filter(
            new Locator.FilterOptions().setHasText(java.util.regex.Pattern.compile("^(Constraint|Component)$")));
  }

  public Locator conditionColumnHeader() {
    return table().locator("th").filter(new Locator.FilterOptions().setHasText("Condition"));
  }

  public Locator violationRows() {
    return locator(TABLE + " tbody tr.nx-table-row:not(.nx-table-row--filter-header)");
  }

  public Locator conditionCellInRow(int rowIndex) {
    return violationRows().nth(rowIndex).locator("td.iq-bulk-waive__condition-name-cell");
  }

  public Locator noResultsRow() {
    return table().getByRole(AriaRole.ROW)
        .filter(
            new Locator.FilterOptions().setHasText("No Results"));
  }

  public Locator selectAllCheckbox() {
    return table().getByRole(AriaRole.CHECKBOX).first().locator("..");
  }

  public Locator selectAllCheckboxInput() {
    return table().getByRole(AriaRole.CHECKBOX).first();
  }

  public Locator firstRowCheckbox() {
    return violationRows().first().getByRole(AriaRole.CHECKBOX).locator("..");
  }

  public Locator checkboxInRow(int rowIndex) {
    return violationRows().nth(rowIndex).getByRole(AriaRole.CHECKBOX).locator("..");
  }

  public Locator violationRowCheckboxInput(int rowIndex) {
    return violationRows().nth(rowIndex).getByRole(AriaRole.CHECKBOX);
  }

  public Locator selectionCountLabel() {
    return locator(ROOT + " .iq-bulk-waive__selected-count");
  }

  public Locator nextButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.NEXT_BUTTON_OPTS);
  }

  public Locator cancelButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }

  public Locator filtersToggleButton() {
    return container().getByRole(AriaRole.BUTTON, CommonButtonOptions.FILTER_BUTTON_OPTS);
  }

  public Locator policyNameFilter() {
    return container().getByPlaceholder("policy name");
  }

  public Locator componentNameFilter() {
    return container().getByPlaceholder("component name");
  }

  public Locator constraintNameFilter() {
    return container().getByPlaceholder("constraint name");
  }

  public Locator enterpriseBanner() {
    return locator(ROOT + " [class*='EnterpriseFullWidthBanner'], " +
        ROOT + " .iq-enterprise-full-width-banner");
  }

  public Locator tile() {
    return container().locator("section.nx-tile");
  }

  public Locator violationDetailsPopover() {
    return page.locator("#component-details-policy-violations-popover");
  }

  public Locator violationDetailsPopoverCloseButton() {
    return violationDetailsPopover().getByRole(AriaRole.BUTTON, CommonButtonOptions.CLOSE_BUTTON_OPTS);
  }

  public Locator reportFilterPopover() {
    return page.locator("#iq-component-filter-popover");
  }

  public Locator reportFilterPopoverCloseButton() {
    return reportFilterPopover().getByRole(AriaRole.BUTTON, CommonButtonOptions.CLOSE_BUTTON_OPTS);
  }

  public void selectFirstViolationAndClickNext() {
    container().waitFor();
    violationRows().first().waitFor();
    firstRowCheckbox().click();
    nextButton().click();
    page.waitForURL("**/waiverConfiguration");
  }

}
