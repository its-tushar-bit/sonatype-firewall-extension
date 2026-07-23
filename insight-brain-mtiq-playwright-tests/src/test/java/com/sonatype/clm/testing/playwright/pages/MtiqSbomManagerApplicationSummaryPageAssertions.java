/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class MtiqSbomManagerApplicationSummaryPageAssertions
{
  private final MtiqSbomManagerApplicationSummaryPage page;

  public MtiqSbomManagerApplicationSummaryPageAssertions(MtiqSbomManagerApplicationSummaryPage page) {
    this.page = page;
  }

  public void shouldShowSbomsTileWithHeader() {
    assertThat(page.sbomsTile()).isVisible();
    assertThat(page.sbomsTileHeader()).containsText("SBOMs");
    assertThat(page.sbomsTileImportButton()).isVisible();
  }

  public void shouldShowSbomsTableColumns() {
    assertThat(page.sbomsTableColumnHeaders()).hasCount(6);
    assertThat(page.sbomsTableColumnHeader(0)).containsText("Versions");
    assertThat(page.sbomsTableColumnHeader(1)).containsText("Vulnerabilities");
    assertThat(page.sbomsTableColumnHeader(2)).containsText("Release Status");
    assertThat(page.sbomsTableColumnHeader(3)).containsText("BOM Format");
    assertThat(page.sbomsTableColumnHeader(4)).containsText("Import Date");
    assertThat(page.sbomsTableColumnHeader(5)).containsText("Actions");
  }

  public void shouldShowApplicationTitle(String expectedTitle) {
    assertThat(page.applicationTitle()).containsText(expectedTitle);
  }

  public void shouldShowEmptyState() {
    assertThat(page.emptyStateCell()).isVisible();
  }
}
