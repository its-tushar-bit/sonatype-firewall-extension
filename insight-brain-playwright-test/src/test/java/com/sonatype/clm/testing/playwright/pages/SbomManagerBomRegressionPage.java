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

  /**
   * The "Report" tab ({@code NxTab} with role {@code tab}, name "Report"}).
   * This is the default active tab; it renders {@code BillOfMaterialsComponentsTile}
   * and {@code SummaryTile}.
   */
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

  /**
   * The "Original BOM" tab ({@code NxTab} with role {@code tab}, name "Original BOM"}).
   * Clicking it renders {@code OriginalBomViewer} in the second {@code NxTabPanel}.
   */
  public Locator originalBomTab() {
    return locator(ROOT).getByRole(AriaRole.TAB,
        new Locator.GetByRoleOptions().setName("Original BOM").setExact(true));
  }

  /**
   * The component list table ({@code NxTable id="bill-of-materials-components-table"}).
   * Rendered inside the Report tab panel by {@code BillOfMaterialsComponentsTile}.
   */
  public Locator componentTable() {
    return locator("#bill-of-materials-components-table");
  }

  /**
   * Body rows of the component table — excludes the header row.
   * Used to assert row count after applying the component search filter.
   */
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
   * The component search text input ({@code NxStatefulTextInput id="component-search"}).
   * {@code NxStatefulTextInput} places {@code id} on the {@code <input>} directly.
   * Triggers a 300 ms-debounced backend query for components matching by name or license.
   */
  public Locator componentSearchInput() {
    return locator("#component-search");
  }

  /**
   * The {@code OriginalBomViewer} container ({@code div.iq-original-bom-viewer}).
   * Present in the DOM once the "Original BOM" tab panel is active.
   */
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

  /**
   * The {@code
   *
  <h2>} heading "Original Bill of Material Data" inside the viewer.
   * Only visible after the {@code NxLoadWrapper} inside {@code OriginalBomViewer}
   * finishes loading the original SBOM tree.
   */
  public Locator originalBomViewerTitle() {
    return locator(".iq-original-bom-viewer").getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setLevel(2));
  }

  /**
   * The SBOM version dropdown container ({@code .sbom-manager-sbom-version-dropdown}).
   * Rendered as {@code NxDropdown} in the page-title button bar. The toggle label reads
   * "Viewing: {currentVersion}".
   */
  public Locator versionDropdown() {
    return locator(".sbom-manager-sbom-version-dropdown");
  }

  /**
   * The toggle button of the SBOM version dropdown ({@code button.nx-dropdown__toggle}).
   * Clicking it opens the list of available SBOM versions for the application.
   */
  public Locator versionDropdownToggle() {
    return locator(".sbom-manager-sbom-version-dropdown button.nx-dropdown__toggle");
  }

  /**
   * A specific version link inside the open dropdown menu.
   * Each item is an {@code <a class="nx-dropdown-button">} whose text is the version ID.
   * Only present in the DOM while the dropdown is open.
   *
   * @param versionId exact SBOM version string to locate (e.g. "bom-regression-v2")
   */
  public Locator versionDropdownItem(String versionId) {
    return locator(".sbom-manager-sbom-version-dropdown").getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName(versionId).setExact(true));
  }

  /**
   * Fill the component search input with {@code query}.
   * The BOM tile debounces by 300 ms before firing the backend request;
   * Playwright web-first assertions handle the wait automatically.
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

  /**
   * The segmented export button container
   * ({@code .sbom-manager-bill-of-materials-page__export-button}).
   * Rendered as {@code NxStatefulSegmentedButton} in the page-title button bar.
   */
  public Locator exportButton() {
    return locator(".sbom-manager-bill-of-materials-page__export-button");
  }

  /**
   * The primary (main) button inside the segmented export button
   * ({@code button.nx-segmented-btn__main-btn}).
   * Text reads "Export SBOM" for a valid BOM and "Export Original SBOM" for an invalid BOM.
   */
  public Locator exportButtonPrimary() {
    return locator(".sbom-manager-bill-of-materials-page__export-button .nx-segmented-btn__main-btn");
  }

  /**
   * The "more options" toggle in the segmented export button
   * ({@code button.nx-segmented-btn__dropdown-btn}, {@code aria-label="more options"}).
   * Clicking opens the export options dropdown list.
   */
  public Locator exportButtonDropdownToggle() {
    return locator(".sbom-manager-bill-of-materials-page__export-button .nx-segmented-btn__dropdown-btn");
  }

  /**
   * The "Export Original SBOM" button inside the open export dropdown.
   * Present (and enabled) for valid BOMs in the dropdown items.
   */
  public Locator exportDropdownExportOriginalSbomButton() {
    return locator(".sbom-manager-bill-of-materials-page__export-button")
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Export Original SBOM").setExact(true));
  }

  /**
   * The "Additional Export Options" button inside the open export dropdown
   * ({@code button.nx-dropdown-button}).
   * Disabled ({@code disabled} attribute) when the SBOM is invalid.
   */
  public Locator exportDropdownAdditionalExportOptions() {
    return locator(".sbom-manager-bill-of-materials-page__export-button")
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Additional Export Options").setExact(true));
  }

  /**
   * The "Export PDF" link inside the open export dropdown ({@code a.nx-dropdown-button}).
   * Rendered with {@code aria-disabled="true"} when the SBOM is invalid.
   */
  public Locator exportDropdownPdfLink() {
    return locator(".sbom-manager-bill-of-materials-page__export-button")
        .getByRole(AriaRole.LINK,
            new Locator.GetByRoleOptions().setName("Export PDF").setExact(true));
  }

  /**
   * The Additional Export Options modal container ({@code #sbom-additional-export-options-modal}).
   * Rendered by {@code SbomAdditionalExportOptionsModal} when "Additional Export Options" is clicked.
   */
  public Locator additionalExportOptionsModal() {
    return locator("#sbom-additional-export-options-modal");
  }

  /**
   * The heading inside the Additional Export Options modal ("Additional Export Options").
   * Used as the primary existence-and-visibility assertion for the modal.
   */
  public Locator additionalExportOptionsModalTitle() {
    return locator("#sbom-additional-export-options-modal")
        .getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2).setExact(true));
  }

  /**
   * The "SBOM Specification" fieldset group inside the modal ({@code <fieldset>} with role group).
   * Contains CycloneDX, SPDX 2.3, and SPDX 3.0 radio options.
   */
  public Locator additionalExportOptionsSpecificationFieldset() {
    return locator("#sbom-additional-export-options-modal")
        .getByRole(AriaRole.GROUP,
            new Locator.GetByRoleOptions().setName("SBOM Specification").setExact(true));
  }

  /**
   * The "SBOM Format" fieldset group inside the modal.
   * Contains JSON and XML radio options.
   */
  public Locator additionalExportOptionsFormatFieldset() {
    return locator("#sbom-additional-export-options-modal")
        .getByRole(AriaRole.GROUP,
            new Locator.GetByRoleOptions().setName("SBOM Format").setExact(true));
  }

  /**
   * The {@code NxLoadError} error alert ({@code .nx-alert--load-error}) rendered by
   * {@code NxLoadWrapper} when any of the 6 BOM data-source error selectors is non-null.
   * Visible in place of the page content while the error persists.
   */
  public Locator loadError() {
    return locator(".nx-alert--load-error");
  }

  /**
   * The Retry button ({@code button.nx-load-error__retry}) inside the {@code NxLoadError} alert.
   * Clicking it dispatches {@code doLoad()} which re-fetches all BOM data sources.
   */
  public Locator retryButton() {
    return locator("button.nx-load-error__retry");
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

  /**
   * The {@code NxWarningAlert} container ({@code #invalid-sbom-alert}).
   * Rendered when {@code isValid=false} and the alert has not yet been dismissed.
   * Contains heading "Invalid SBOM Detected" and the validation-error detail message.
   */
  public Locator invalidSbomAlert() {
    return locator("#invalid-sbom-alert");
  }

  /**
   * The {@code NxCloseButton} ({@code aria-label="Close"}) inside the invalid-SBOM alert.
   * Clicking it dispatches {@code dismissSbomInvalidAlert()} which sets
   * {@code validationErrorAlertDismissed=true} in the Redux slice, hiding the alert
   * and revealing {@code InvalidSbomIndicator} in the page-title {@code
   *
  <h1>}.
   */
  public Locator invalidSbomAlertCloseButton() {
    return locator("#invalid-sbom-alert").getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Close").setExact(true));
  }

  /**
   * The {@code InvalidSbomIndicator} icon ({@code .sbom-manager-invalid-sbom-indicator})
   * inside the page-title {@code
   *
  <h1>}.
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
