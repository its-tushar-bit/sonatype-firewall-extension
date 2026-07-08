/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Regression-only page object for the SBOM Manager BOM (Bill of Materials) page
 * ({@code #/sbomManager/management/view/application/{publicId}/bom/{versionId}/overview}).
 * Contains locators for the tab strip, component table, component search, and
 * the Original BOM viewer. Do not add methods to the existing
 * {@link SbomManagerBomPage} (sanity navigation helper).
 */
public class SbomManagerBomRegressionPage
    extends SbomManagerBomPage
{
  private static final String ROOT = "#sbom-manager-bom";

  public SbomManagerBomRegressionPage() {
    super();
  }

  public static String url(String publicId, String versionId) {
    return "/assets/index.html#/sbomManager/management/view/application/" + publicId
        + "/bom/" + versionId + "/overview";
  }

  /** The "Report" tab (default active tab). */
  public Locator reportTab() {
    return locator(ROOT).getByRole(AriaRole.TAB,
        new Locator.GetByRoleOptions().setName("Report").setExact(true));
  }

  /**
   * The "Report" tab when it is the active (selected) tab.
   * Uses {@code selected=true} in the role filter so the locator only resolves
   * when {@code aria-selected="true"} — no separate attribute assertion needed.
   */
  public Locator selectedReportTab() {
    return locator(ROOT).getByRole(AriaRole.TAB,
        new Locator.GetByRoleOptions().setName("Report").setExact(true).setSelected(true));
  }

  /** The "Original BOM" tab. */
  public Locator originalBomTab() {
    return locator(ROOT).getByRole(AriaRole.TAB,
        new Locator.GetByRoleOptions().setName("Original BOM").setExact(true));
  }

  /** The component list table. */
  public Locator componentTable() {
    return locator("#bill-of-materials-components-table");
  }

  /** Body rows of the component table — excludes the header row. */
  public Locator componentTableBodyRows() {
    return locator("#bill-of-materials-components-table tbody .nx-table-row:not(.nx-table-row--header)");
  }

  /**
   * Body rows of the component table that contain {@code text}.
   * Use with {@code hasCount(1)} after {@link #searchComponents} to avoid a strict-mode
   * violation: the unfiltered locator may briefly resolve to 2 rows during re-renders between
   * {@code hasCount(1)} and a subsequent {@code containsText} call.
   */
  public Locator componentTableBodyRowsContaining(String text) {
    return componentTableBodyRows().filter(new Locator.FilterOptions().setHasText(text));
  }

  /**
   * The component search text input.
   * Triggers a 300 ms-debounced backend query for components matching by name or license.
   */
  public Locator componentSearchInput() {
    return locator("#component-search");
  }

  /** The {@code OriginalBomViewer} container — present once the "Original BOM" tab panel is active. */
  public Locator originalBomViewer() {
    return locator(".iq-original-bom-viewer");
  }

  /** Search is debounced (~300 ms); callers rely on web-first waiting on the results count. */
  public Locator originalBomSearchInput() {
    return originalBomViewer().getByRole(AriaRole.TEXTBOX,
        new Locator.GetByRoleOptions().setName("Search SBOM components and attributes"));
  }

  /** Visible only when search input is non-empty; renders "N result(s) found". */
  public Locator originalBomSearchResultsCount() {
    return originalBomViewer().getByRole(AriaRole.STATUS)
        .filter(new Locator.FilterOptions().setHasText("found"));
  }

  /** The H2 heading "Original Bill of Material Data" — visible after the viewer finishes loading. */
  public Locator originalBomViewerTitle() {
    return locator(".iq-original-bom-viewer").getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(2));
  }

  /** The SBOM version dropdown container. Toggle label reads "Viewing: {currentVersion}". */
  public Locator versionDropdown() {
    return locator(".sbom-manager-sbom-version-dropdown");
  }

  /** The toggle button of the SBOM version dropdown — opens the list of available versions. */
  public Locator versionDropdownToggle() {
    return locator(".sbom-manager-sbom-version-dropdown button.nx-dropdown__toggle");
  }

  /**
   * A specific version link inside the open dropdown menu.
   *
   * @param versionId exact SBOM version string to locate (e.g. "bom-regression-v2")
   */
  public Locator versionDropdownItem(String versionId) {
    return locator(".sbom-manager-sbom-version-dropdown").getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName(versionId).setExact(true));
  }

  /**
   * Fill the component search input with {@code query}.
   * The BOM tile debounces by 300 ms before firing the backend request.
   */
  public void searchComponents(String query) {
    componentSearchInput().fill(query);
  }

  /** Click the "Original BOM" tab to activate the second {@code NxTabPanel}. */
  public void clickOriginalBomTab() {
    originalBomTab().click();
  }

  /** Click the version dropdown toggle to open the SBOM version selection menu. */
  public void clickVersionDropdownToggle() {
    versionDropdownToggle().click();
  }

  /**
   * Click a version link in the open dropdown to navigate to that version's BOM overview.
   *
   * @param versionId exact SBOM version string to click
   */
  public void clickVersionDropdownItem(String versionId) {
    versionDropdownItem(versionId).click();
  }

  /** The segmented export button container. */
  public Locator exportButton() {
    return locator(".sbom-manager-bill-of-materials-page__export-button");
  }

  /**
   * The primary (main) button inside the segmented export button.
   * Text reads "Export SBOM" for a valid BOM and "Export Original SBOM" for an invalid BOM.
   */
  public Locator exportButtonPrimary() {
    return locator(".sbom-manager-bill-of-materials-page__export-button .nx-segmented-btn__main-btn");
  }

  /** The "more options" toggle in the segmented export button — opens the export options dropdown. */
  public Locator exportButtonDropdownToggle() {
    return locator(".sbom-manager-bill-of-materials-page__export-button .nx-segmented-btn__dropdown-btn");
  }

  /** The "Export Original SBOM" button inside the open export dropdown. */
  public Locator exportDropdownExportOriginalSbomButton() {
    return locator(".sbom-manager-bill-of-materials-page__export-button")
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Export Original SBOM").setExact(true));
  }

  /**
   * The "Additional Export Options" button inside the open export dropdown.
   * Disabled when the SBOM is invalid.
   */
  public Locator exportDropdownAdditionalExportOptions() {
    return locator(".sbom-manager-bill-of-materials-page__export-button")
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Additional Export Options").setExact(true));
  }

  /**
   * The "Export PDF" link inside the open export dropdown.
   * Rendered with {@code aria-disabled="true"} when the SBOM is invalid.
   */
  public Locator exportDropdownPdfLink() {
    return locator(".sbom-manager-bill-of-materials-page__export-button")
        .getByRole(AriaRole.LINK,
            new Locator.GetByRoleOptions().setName("Export PDF").setExact(true));
  }

  /** The Additional Export Options modal container. */
  public Locator additionalExportOptionsModal() {
    return locator("#sbom-additional-export-options-modal");
  }

  /** The heading inside the Additional Export Options modal. */
  public Locator additionalExportOptionsModalTitle() {
    return locator("#sbom-additional-export-options-modal")
        .getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2).setExact(true));
  }

  /** The "SBOM Specification" fieldset — contains CycloneDX, SPDX 2.3, SPDX 3.0 radio options. */
  public Locator additionalExportOptionsSpecificationFieldset() {
    return locator("#sbom-additional-export-options-modal")
        .getByRole(AriaRole.GROUP,
            new Locator.GetByRoleOptions().setName("SBOM Specification").setExact(true));
  }

  /** The "SBOM Format" fieldset — contains JSON and XML radio options. */
  public Locator additionalExportOptionsFormatFieldset() {
    return locator("#sbom-additional-export-options-modal")
        .getByRole(AriaRole.GROUP,
            new Locator.GetByRoleOptions().setName("SBOM Format").setExact(true));
  }

  /** Load-error alert shown when any of the 6 BOM data-source selectors returns non-null. */
  public Locator loadError() {
    return nxLoadErrorAlert();
  }

  public Locator retryButton() {
    return nxLoadErrorRetryButton();
  }

  /** Click the Retry button inside the load-error alert. */
  public void clickRetry() {
    retryButton().click();
  }

  /**
   * The "Policy Violation Summary" chart-and-progress section
   * ({@code #bill-of-materials-summary-tile-chart-and-progress-policy-violation-summary}).
   * Only rendered by {@code BillOfMaterialSummaryTile} when {@code isSbomPoliciesSupported=true}
   * (derived from {@code productFeatures['sbom-policies']}, controlled by
   * {@code SystemConfigurationPropertyFeature.SBOM_POLICIES}).
   * Contains an {@code NxH3} "Policy Violation Summary" and a set of {@code NxProgressBar} rows.
   */
  public Locator policyViolationSummarySection() {
    return locator(
        "#bill-of-materials-summary-tile-chart-and-progress-policy-violation-summary");
  }

  /** The invalid-SBOM warning alert — rendered when {@code isValid=false} and not yet dismissed. */
  public Locator invalidSbomAlert() {
    return locator("#invalid-sbom-alert");
  }

  /**
   * The close button inside the invalid-SBOM alert.
   * Clicking dismisses the alert and reveals {@link #invalidSbomIndicator()} in the page title.
   */
  public Locator invalidSbomAlertCloseButton() {
    return locator("#invalid-sbom-alert").getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Close").setExact(true));
  }

  /**
   * The invalid-SBOM indicator icon inside the page-title H1.
   * Only present when {@code isValid=false} AND the alert has been dismissed.
   */
  public Locator invalidSbomIndicator() {
    return locator(".sbom-manager-invalid-sbom-indicator");
  }

  /** Click the close button on the "Invalid SBOM Detected" alert. */
  public void clickInvalidSbomAlertClose() {
    invalidSbomAlertCloseButton().click();
  }

  /** Click the export button's "more options" dropdown toggle to open the options list. */
  public void clickExportButtonDropdownToggle() {
    exportButtonDropdownToggle().click();
  }

  /** Click "Additional Export Options" in the open export dropdown to open the modal. */
  public void clickAdditionalExportOptions() {
    exportDropdownAdditionalExportOptions().click();
  }

  /** Click the primary Export button and wait for the browser download event. */
  public Download clickExportPrimaryAndWaitForDownload() {
    return page.waitForDownload(() -> exportButtonPrimary().click());
  }
}
