/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Locator.WaitForOptions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;
import com.sonatype.insight.brain.model.Application;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LegalApplicationDetailsPage
    extends BasePage
{
  private static final String ROOT = "#legal-application-details-container";

  private static final Locator.GetByRoleOptions CREATE_ATTRIBUTION_REPORT_OPTS =
      new Locator.GetByRoleOptions().setName("Create Attribution Report");

  public LegalApplicationDetailsPage() {
    super();
  }

  public static String url(Application app, String stageTypeId) {
    return "/assets/index.html#/legal/application/" + app.getPublicId() + "/stage/" + stageTypeId;
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator pageTitle() {
    return locator(ROOT + " .nx-page-title h1.nx-h1");
  }

  public Locator table() {
    return locator("#legal-application-details-table");
  }

  public Locator tableDataRows() {
    return locator("#legal-application-details-table tbody tr")
        .filter(new Locator.FilterOptions().setHasNot(locator(".nx-filter-input")));
  }

  public Locator filterButton() {
    return locator("#filter-toggle");
  }

  public Locator filterDirtyAsterisk() {
    return locator("#filter-toggle-dirty-asterisk");
  }

  public Locator filterSidebar() {
    return locator(".legal-application-details-filter");
  }

  public Locator reviewStatusFilterGroup() {
    return locator("#legal-progress-options-filter");
  }

  public Locator licenseThreatGroupFilterGroup() {
    return locator("#legal-license-threat-groups-filter");
  }

  public void expandReviewStatusFilter() {
    reviewStatusFilterGroup().locator("button.nx-collapsible-items__trigger").click();
  }

  public void selectReviewStatusOption(String label) {
    reviewStatusFilterGroup()
        .locator("label.nx-radio-checkbox")
        .filter(new Locator.FilterOptions().setHasText(label))
        .click();
  }

  public void openFilterSidebar() {
    filterButton().click();
    assertThat(filterSidebar()).isVisible();
  }

  public Locator stageSubtitle() {
    return container().getByText("Stage");
  }

  public Locator backButton() {
    return container().getByRole(AriaRole.LINK, CommonButtonOptions.BACK_BUTTON_OPTS);
  }

  public Locator createAttributionReportButton() {
    return container().getByRole(AriaRole.BUTTON, CREATE_ATTRIBUTION_REPORT_OPTS);
  }

  public Locator componentColumnHeader() {
    return table().locator("th").filter(new Locator.FilterOptions().setHasText("Component"));
  }

  public Locator licensesColumnHeader() {
    return table().locator("th").filter(new Locator.FilterOptions().setHasText("Licenses"));
  }

  public Locator completedObligationsColumnHeader() {
    return table().locator("th").filter(new Locator.FilterOptions().setHasText("Completed Obligations"));
  }

  public Locator componentFilterInput() {
    return container().getByPlaceholder("Filter components");
  }

  public Locator licenseFilterInput() {
    return container().getByPlaceholder("Filter licenses");
  }

  public void clickBackButton() {
    backButton().click();
  }

  public void filterByComponent(String term) {
    componentFilterInput().waitFor(new WaitForOptions()
        .setState(WaitForSelectorState.VISIBLE));
    componentFilterInput().fill(term);
  }

  public void clearComponentFilter() {
    componentFilterInput().fill("");
  }

  public void filterByLicense(String term) {
    licenseFilterInput().fill(term);
  }

  public void clickComponentSort() {
    table().waitFor();
    componentColumnHeader().click();
  }

  public void clickLicensesSort() {
    licensesColumnHeader().click();
  }

  public void clickCompletedObligationsSort() {
    completedObligationsColumnHeader().click();
  }

  public void clickCreateAttributionReport() {
    createAttributionReportButton().click();
  }

  public void waitForAttributionReportNavigation() {
    PlaywrightWaitUtils.waitForUrl(page, "attributionReport");
  }
}
