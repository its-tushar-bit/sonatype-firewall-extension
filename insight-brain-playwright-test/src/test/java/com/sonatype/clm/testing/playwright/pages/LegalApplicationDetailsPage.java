/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.sonatype.insight.brain.model.Application;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Legal Application Details page
 * ({@code legal.applicationDetails} route).
 * <p>
 * Route: {@code /legal/application/{applicationPublicId}/stage/{stageTypeId}}
 * Root element: {@code #legal-application-details-container}
 */
public class LegalApplicationDetailsPage
    extends BasePage
{
  private static final String ROOT = "#legal-application-details-container";

  public LegalApplicationDetailsPage() {
    super();
  }

  public static String url(Application app, String stageTypeId) {
    return "/assets/index.html#/legal/application/" + app.getPublicId() + "/stage/" + stageTypeId;
  }

  public Locator pageTitle() {
    return locator(ROOT + " .nx-page-title h1.nx-h1");
  }

  public Locator table() {
    return locator("#legal-application-details-table");
  }

  /**
   * Data rows in the components table — excludes the inline filter row
   * (which contains NxFilterInput elements, not component data).
   */
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

  /** Root div of the filter sidebar (rendered inside IqPopover when open). */
  public Locator filterSidebar() {
    return locator(".legal-application-details-filter");
  }

  public Locator reviewStatusFilterGroup() {
    return locator("#legal-progress-options-filter");
  }

  public Locator licenseThreatGroupFilterGroup() {
    return locator("#legal-license-threat-groups-filter");
  }

  /**
   * Expands the Review Status collapsible group so its checkboxes become visible.
   * NxStatefulTreeViewMultiSelect renders as NxCollapsibleItems — the toggle is
   * {@code button.nx-collapsible-items__trigger}.
   */
  public void expandReviewStatusFilter() {
    reviewStatusFilterGroup().locator("button.nx-collapsible-items__trigger").click();
  }

  /**
   * Selects a checkbox inside the Review Status filter group by its visible label text.
   * NxCollapsibleMultiSelect renders each option as NxCheckbox: the {@code <input>} has
   * {@code role="menuitemcheckbox"} (not the implicit "checkbox" role), so
   * {@code getByRole(CHECKBOX)} will not find it. Instead we click the {@code <label>}
   * element whose text matches, which triggers the checkbox via its {@code for} binding.
   */
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
}
