/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class MtiqSbomManagerDashboardPageAssertions
{
  private final MtiqSbomManagerDashboardPage page;

  public MtiqSbomManagerDashboardPageAssertions(MtiqSbomManagerDashboardPage page) {
    this.page = page;
  }

  public void shouldShowDashboardHeader() {
    assertThat(page.container()).isVisible();
    assertThat(page.heading()).containsText("SBOM Manager Dashboard");
  }

  public void shouldShowAllTiles() {
    assertThat(page.totalSbomsStoredTile()).isVisible();
    assertThat(page.applicationsHistoryTile()).isVisible();
    assertThat(page.highPriorityVulnerabilitiesTile()).isVisible();
    assertThat(page.vulnerabilitiesByThreatLevelTile()).isVisible();
    assertThat(page.sbomReleaseStatusTile()).isVisible();
    assertThat(page.recentlyImportedSbomsTile()).isVisible();
  }

  public void shouldShowTotalSbomsStored(int total, int threshold) {
    assertThat(page.totalSbomsStoredCount()).containsText(String.valueOf(total));
    assertThat(page.totalSbomsStoredProgressTotal()).containsText(String.valueOf(total));
    assertThat(page.totalSbomsStoredProgressThreshold()).containsText(String.valueOf(threshold));
    assertThat(page.totalSbomsStoredProgressBar()).isVisible();
  }

  public void shouldShowApplicationsHistory(int scanned, int lastYear, int lastMonth, int lastWeek) {
    assertThat(page.applicationsHistoryTotalScanned()).containsText(String.valueOf(scanned));
    assertThat(page.applicationsHistoryUpdatedLastYear()).containsText(String.valueOf(lastYear));
    assertThat(page.applicationsHistoryUpdatedLastMonth()).containsText(String.valueOf(lastMonth));
    assertThat(page.applicationsHistoryUpdatedLastWeek()).containsText(String.valueOf(lastWeek));
  }

  public void shouldShowVulnerabilitiesTotals(int total, int unannotated, int annotated) {
    assertThat(page.vulnerabilitiesTotal()).containsText(String.valueOf(total));
    assertThat(page.vulnerabilitiesUnannotated()).containsText(String.valueOf(unannotated));
    assertThat(page.vulnerabilitiesAnnotated()).containsText(String.valueOf(annotated));
  }

  public void shouldShowVulnerabilitiesTableRow(String threatLevel, int unannotated, int annotated, int total) {
    Locator row = page.vulnerabilitiesTableRowByThreatLevel(threatLevel);
    assertThat(row).isVisible();
    assertThat(row).containsText(String.valueOf(unannotated));
    assertThat(row).containsText(String.valueOf(annotated));
    assertThat(row).containsText(String.valueOf(total));
  }

  public void shouldShowHighPriorityVulnerabilityCount(int expectedCount) {
    assertThat(page.highPrioritySeverityBadges()).hasCount(expectedCount);
  }

  public void shouldShowSbomReleaseStatusEntry(String status) {
    Locator statusEl = page.sbomReleaseStatusMeterBarStatus()
        .filter(
            new Locator.FilterOptions().setHasText(status));
    assertThat(statusEl).isVisible();
  }

  public void shouldShowRecentlyImportedSbomsHeader(String columnName) {
    assertThat(page.recentlyImportedSbomsTableHeader(columnName)).isVisible();
  }

  public void shouldShowRecentlyImportedSbomsFirstRowContains(String expectedText) {
    assertThat(page.recentlyImportedSbomsFirstRow()).containsText(expectedText);
  }
}
