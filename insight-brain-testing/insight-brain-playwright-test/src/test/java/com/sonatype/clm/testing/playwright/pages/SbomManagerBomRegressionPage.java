/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/** Regression page object for the SBOM Manager BOM page; extends {@link SbomManagerBomPage}. */
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

  /** The "Report" tab when selected — {@code aria-selected="true"}. */
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

  /** Filtered rows containing {@code text} — avoids strict-mode violations during re-renders. */
  public Locator componentTableBodyRowsContaining(String text) {
    return componentTableBodyRows().filter(new Locator.FilterOptions().setHasText(text));
  }

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

  public Locator versionDropdownItem(String versionId) {
    return locator(".sbom-manager-sbom-version-dropdown").getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName(versionId).setExact(true));
  }

  public void searchComponents(String query) {
    componentSearchInput().fill(query);
  }

  public void clickOriginalBomTab() {
    originalBomTab().click();
  }

  public void clickVersionDropdownToggle() {
    versionDropdownToggle().click();
  }

  public void clickVersionDropdownItem(String versionId) {
    versionDropdownItem(versionId).click();
  }

  public Locator exportButton() {
    return locator(".sbom-manager-bill-of-materials-page__export-button");
  }

  /** Text reads "Export SBOM" (valid) or "Export Original SBOM" (invalid BOM). */
  public Locator exportButtonPrimary() {
    return locator(".sbom-manager-bill-of-materials-page__export-button .nx-segmented-btn__main-btn");
  }

  public Locator exportButtonDropdownToggle() {
    return locator(".sbom-manager-bill-of-materials-page__export-button .nx-segmented-btn__dropdown-btn");
  }

  public Locator exportDropdownExportOriginalSbomButton() {
    return locator(".sbom-manager-bill-of-materials-page__export-button")
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Export Original SBOM").setExact(true));
  }

  /** Disabled when the SBOM is invalid. */
  public Locator exportDropdownAdditionalExportOptions() {
    return locator(".sbom-manager-bill-of-materials-page__export-button")
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Additional Export Options").setExact(true));
  }

  /** {@code aria-disabled="true"} when the SBOM is invalid. */
  public Locator exportDropdownPdfLink() {
    return locator(".sbom-manager-bill-of-materials-page__export-button")
        .getByRole(AriaRole.LINK,
            new Locator.GetByRoleOptions().setName("Export PDF").setExact(true));
  }

  public Locator additionalExportOptionsModal() {
    return locator("#sbom-additional-export-options-modal");
  }

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

  public Locator additionalExportOptionsFormatFieldset() {
    return locator("#sbom-additional-export-options-modal")
        .getByRole(AriaRole.GROUP,
            new Locator.GetByRoleOptions().setName("SBOM Format").setExact(true));
  }

  public Locator loadError() {
    return nxLoadErrorAlert();
  }

  public Locator retryButton() {
    return nxLoadErrorRetryButton();
  }

  public void clickRetry() {
    retryButton().click();
  }

  /** Only rendered when {@code SystemConfigurationPropertyFeature.SBOM_POLICIES} is enabled. */
  public Locator policyViolationSummarySection() {
    return locator(
        "#bill-of-materials-summary-tile-chart-and-progress-policy-violation-summary");
  }

  public Locator invalidSbomAlert() {
    return locator("#invalid-sbom-alert");
  }

  public Locator invalidSbomAlertCloseButton() {
    return locator("#invalid-sbom-alert").getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Close").setExact(true));
  }

  /** Only present when {@code isValid=false} and the alert has been dismissed. */
  public Locator invalidSbomIndicator() {
    return locator(".sbom-manager-invalid-sbom-indicator");
  }

  public void clickInvalidSbomAlertClose() {
    invalidSbomAlertCloseButton().click();
  }

  /** Small SBOMs (≤ 1000 nodes) are auto-expanded on load ({@code aria-expanded="true"}). */
  public Locator originalBomCollapsibleItems() {
    return originalBomViewer().locator(".nx-tree__item--collapsible");
  }

  /**
   * {@code .first()} required: a collapsible item with collapsible descendants exposes multiple rects; the first
   * belongs to the item itself.
   */
  public Locator firstCollapsibleItemToggle() {
    return originalBomCollapsibleItems().first().locator(".nx-tree__collapse-click").first();
  }

  /** CSS class used: {@code <mark>} has no ARIA role; {@code iq-original-bom-viewer__highlight} is the stable hook. */
  public Locator originalBomHighlightMarks() {
    return originalBomViewer().locator("mark.iq-original-bom-viewer__highlight");
  }

  /**
   * Key label spans — display name derived by intelligent title logic (e.g. {@code "guava@30.1-jre"} rather than the
   * raw tag/index).
   */
  public Locator originalBomKeyLabels() {
    return originalBomViewer().locator(".iq-original-bom-viewer__key");
  }

  /** Present only when a node's child count exceeds the current batch size and the node is expanded. */
  public Locator originalBomLoadMoreButton() {
    return originalBomViewer().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(Pattern.compile("Load \\d+ more")));
  }

  public void clickExportButtonDropdownToggle() {
    exportButtonDropdownToggle().click();
  }

  public void clickAdditionalExportOptions() {
    exportDropdownAdditionalExportOptions().click();
  }

  /** Click the primary Export button and wait for the browser download event. */
  public Download clickExportPrimaryAndWaitForDownload() {
    return page.waitForDownload(() -> exportButtonPrimary().click());
  }
}
