/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Extension of {@link SbomManagerDashboardPage} for regression test coverage.
 * {@link SbomManagerDashboardPage} holds locators shared by sanity and regression;
 * this subclass adds regression-only locators.
 */
public class SbomManagerDashboardRegressionPage
    extends SbomManagerDashboardPage
{
  public SbomManagerDashboardRegressionPage() {
    super();
  }

  public static String url() {
    return SbomManagerDashboardPage.url();
  }

  /** Count element inside "Total SBOMs Stored" tile ({@code .sbom-manager-total-sboms-stored-tile__total}). */
  public Locator totalSbomsStoredCount() {
    return locator(".sbom-manager-total-sboms-stored-tile__total");
  }

  /** Metric list inside "Applications History" tile ({@code .sbom-manager-applications-history-tile-list}). */
  public Locator applicationsHistoryList() {
    return locator(".sbom-manager-applications-history-tile-list");
  }

  /**
   * Vulnerability list inside "High Priority Vulnerabilities" tile
   * ({@code .sbom-manager-high-priority-vulnerabilities-tile-list}).
   * Only rendered when Critical or Severe CVEs exist.
   */
  public Locator highPriorityVulnerabilitiesList() {
    return locator(".sbom-manager-high-priority-vulnerabilities-tile-list");
  }

  /**
   * Summary list inside "Vulnerabilities by Threat Level" tile
   * ({@code .sbom-manager-vulnerabilities-by-threat-level-tile__list}).
   */
  public Locator vulnerabilitiesByThreatLevelList() {
    return locator(".sbom-manager-vulnerabilities-by-threat-level-tile__list");
  }

  /**
   * Meter bars container inside "SBOM Release Status" tile
   * ({@code .sbom-manager-sbom-release-status-tile__meter-bars}).
   */
  public Locator sbomReleaseStatusMeterBars() {
    return locator(".sbom-manager-sbom-release-status-tile__meter-bars");
  }

  /**
   * Table body rows inside "Recently Imported SBOMs" tile
   * ({@code .sbom-manager-recently-imported-sboms-tile-table tbody tr}).
   * Has rows only when at least one SBOM has been imported.
   */
  public Locator recentlyImportedSbomsTableRows() {
    return locator(".sbom-manager-recently-imported-sboms-tile-table tbody tr");
  }
}
