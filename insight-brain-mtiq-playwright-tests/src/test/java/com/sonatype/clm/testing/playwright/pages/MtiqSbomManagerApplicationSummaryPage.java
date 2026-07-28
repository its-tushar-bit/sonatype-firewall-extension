/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class MtiqSbomManagerApplicationSummaryPage
    extends SbomManagerOwnerSummaryPage
{
  private static final String SBOMS_TILE = "#owner-pill-sboms";

  public MtiqSbomManagerApplicationSummaryPage() {
    super();
  }

  public static String url(String applicationPublicId) {
    return OwnerSummaryPage.sbomManagerAppUrl(applicationPublicId);
  }

  /** TODO(CLM-42839): remove once AbstractPlaywrightTest has a hard-open helper. */
  public static String neutralDetourUrl() {
    return SbomManagerDashboardPage.url();
  }

  /** NxTile renders as an unlabelled section; anchor by id since no ARIA role/name is exposed. */
  public Locator sbomsTile() {
    return locator(SBOMS_TILE);
  }

  public Locator sbomsTileHeader() {
    return sbomsTile().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2));
  }

  public Locator sbomsTileImportButton() {
    return sbomsTile()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Import").setExact(true));
  }

  public Locator applicationTitle() {
    return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1));
  }

  public Locator sbomsTable() {
    return sbomsTile().locator("table").first();
  }

  public Locator sbomsTableColumnHeaders() {
    return sbomsTile().locator("thead th");
  }

  public Locator sbomsTableColumnHeader(int zeroBasedIndex) {
    return sbomsTableColumnHeaders().nth(zeroBasedIndex);
  }

  public Locator sbomsTableBodyRows() {
    return sbomsTile().locator("tbody tr");
  }

  public Locator sbomsTableBodyRowColumn(int rowIndex, int columnIndex) {
    return sbomsTableBodyRows().nth(rowIndex).locator("td").nth(columnIndex);
  }

  /** Numeric page-number buttons; excludes prev/next arrow-only buttons via text-filter. */
  public Locator paginationButtons() {
    return sbomsTile().getByRole(AriaRole.BUTTON)
        .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\d+$")));
  }

  /** NxIconDropdown labels the toggle with {@code aria-label="<version>-options"}. */
  public Locator sbomActionsDropdownFor(String applicationVersion) {
    return page.getByLabel(applicationVersion + "-options");
  }

  /**
   * NxIconDropdown items lack {@code role="menuitem"} and its container has no queryable role;
   * anchor by RSC's {@code .nx-dropdown-menu} class and filter by visible text.
   */
  public Locator sbomActionsMenuItem(String name) {
    return page.locator(".nx-dropdown-menu").getByText(name);
  }

  /** NxModal doesn't expose a discoverable ARIA role in this RSC build; anchor by frontend id. */
  public Locator deleteSbomModal() {
    return locator("#delete-sbom-version-modal");
  }

  public Locator deleteSbomModalPrimaryButton() {
    return deleteSbomModal().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Delete").setExact(true));
  }

  /** Decorative FontAwesome icon with no accessible name; anchored by component class. */
  public Locator invalidSbomIndicatorInRow(int rowIndex) {
    return sbomsTableBodyRows().nth(rowIndex).locator(".sbom-manager-invalid-sbom-indicator");
  }

  public Locator bomNavigationLinkInRow(int rowIndex) {
    return sbomsTableBodyRows().nth(rowIndex).getByRole(AriaRole.LINK).first();
  }

  public Locator emptyStateCell() {
    return sbomsTable().locator("tbody").getByText("No SBOMs found");
  }

  /** NxModal doesn't expose a discoverable ARIA role in this RSC build; anchor by frontend id. */
  public Locator additionalExportOptionsModal() {
    return locator("#sbom-additional-export-options-modal");
  }

  public Locator additionalExportSpecificationRadio(String label) {
    return radioInFieldsetByLegend("SBOM Specification")
        .getByText(label, new Locator.GetByTextOptions().setExact(true));
  }

  public Locator additionalExportFormatRadio(String label) {
    return radioInFieldsetByLegend("SBOM Format")
        .getByText(label, new Locator.GetByTextOptions().setExact(true));
  }

  private Locator radioInFieldsetByLegend(String legendText) {
    return additionalExportOptionsModal()
        .getByRole(AriaRole.GROUP, new Locator.GetByRoleOptions().setName(legendText));
  }
}
