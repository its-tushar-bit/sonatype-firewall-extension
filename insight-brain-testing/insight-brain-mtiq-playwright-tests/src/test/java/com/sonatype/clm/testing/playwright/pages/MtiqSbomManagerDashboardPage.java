/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class MtiqSbomManagerDashboardPage
    extends SbomManagerDashboardPage
{
  public MtiqSbomManagerDashboardPage() {
    super();
  }

  /** Decorative SVG info icons — no accessible name; anchored by {@code data-icon}. */
  public Locator tileInfoIcons(Locator tile) {
    return tile.locator("svg[data-icon='info-circle']");
  }

  public Locator tileLink(Locator tile) {
    return tile.getByRole(AriaRole.LINK);
  }

  public Locator totalSbomsStoredCount() {
    return byTestId("total-sboms-stored-tile-total");
  }

  public Locator totalSbomsStoredProgressTotal() {
    return byTestId("total-sboms-stored-tile-progress-total");
  }

  public Locator totalSbomsStoredProgressThreshold() {
    return byTestId("total-sboms-stored-tile-progress-threshold");
  }

  public Locator totalSbomsStoredProgressBar() {
    return totalSbomsStoredTile().locator("progress");
  }

  public Locator applicationsHistoryTotalScanned() {
    return byTestId("applications-history-tile-total-scanned-applications");
  }

  public Locator applicationsHistoryUpdatedLastYear() {
    return byTestId("applications-history-tile-applications-updated-last-year");
  }

  public Locator applicationsHistoryUpdatedLastMonth() {
    return byTestId("applications-history-tile-applications-updated-last-month");
  }

  public Locator applicationsHistoryUpdatedLastWeek() {
    return byTestId("applications-history-tile-applications-updated-last-week");
  }

  public Locator vulnerabilitiesTotal() {
    return byTestId("vulnerabilities-by-threat-level-tile-total");
  }

  public Locator vulnerabilitiesUnannotated() {
    return byTestId("vulnerabilities-by-threat-level-tile-total-unannotated");
  }

  public Locator vulnerabilitiesAnnotated() {
    return byTestId("vulnerabilities-by-threat-level-tile-total-annotated");
  }

  public Locator vulnerabilitiesTableRowByThreatLevel(String threatLevel) {
    return vulnerabilitiesByThreatLevelTile().getByRole(AriaRole.ROW,
        new Locator.GetByRoleOptions().setName(Pattern.compile(escapeForJsRegex(threatLevel) + "\\s")));
  }

  public Locator highPrioritySeverityBadges() {
    return byTestId("high-priority-vulnerabilities-severity");
  }

  public Locator highPriorityVulnerabilityLinkByName(String vulnerabilityName) {
    return highPriorityVulnerabilitiesTile().getByRole(AriaRole.LINK,
        new Locator.GetByRoleOptions().setName(vulnerabilityName));
  }

  public Locator sbomReleaseStatusMeterBarStatus() {
    return byTestId("sbom-release-status-meter-bar-status");
  }

  public Locator sbomReleaseStatusMeterBarSbomCount() {
    return byTestId("sbom-release-status-meter-bar-sbom-count");
  }

  public Locator recentlyImportedSbomsTable() {
    return recentlyImportedSbomsTile().locator("table").first();
  }

  /** NxSortingHeader appends sort state to accessible name — filter by has-text. */
  public Locator recentlyImportedSbomsTableHeader(String name) {
    return recentlyImportedSbomsTable().locator("thead th")
        .filter(
            new Locator.FilterOptions().setHasText(name));
  }

  public Locator recentlyImportedSbomsFirstRow() {
    return recentlyImportedSbomsTable().locator("tbody tr").first();
  }
}
