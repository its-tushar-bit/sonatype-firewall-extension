/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the SBOM Manager Dashboard page
 * ({@code #/sbomManager/dashboard}).
 */
public class SbomManagerDashboardPage
    extends BasePage
{
  private static final String ROOT = "#sbom-manager-dashboard";

  public SbomManagerDashboardPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/sbomManager/dashboard";
  }

  /** Root {@code NxPageMain} container ({@code #sbom-manager-dashboard}). */
  public Locator container() {
    return locator(ROOT);
  }

  /** Page H1 heading "SBOM Manager Dashboard". */
  public Locator heading() {
    return locator(ROOT).getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(1));
  }

  /** {@code div.sbom-manager-dashboard-tiles} wrapping all dashboard tiles. */
  public Locator tilesContainer() {
    return locator(".sbom-manager-dashboard-tiles");
  }

  /** "Total SBOMs Stored" tile ({@code #total-sboms-stored-tile}). */
  public Locator totalSbomsStoredTile() {
    return locator("#total-sboms-stored-tile");
  }

  /** "Applications History" tile ({@code #applications-history-tile}). */
  public Locator applicationsHistoryTile() {
    return locator("#applications-history-tile");
  }

  /** "High Priority Vulnerabilities" tile ({@code #high-priority-vulnerabilities-tile}). */
  public Locator highPriorityVulnerabilitiesTile() {
    return locator("#high-priority-vulnerabilities-tile");
  }

  /** "Vulnerabilities by Threat Level" tile ({@code #vulnerabilities-by-threat-level-tile}). */
  public Locator vulnerabilitiesByThreatLevelTile() {
    return locator("#vulnerabilities-by-threat-level-tile");
  }

  /** "SBOM Release Status" tile ({@code #sbom-release-status-tile}). */
  public Locator sbomReleaseStatusTile() {
    return locator("#sbom-release-status-tile");
  }

  /** "Recently Imported SBOMs" tile ({@code #recently-imported-sboms-tile}). */
  public Locator recentlyImportedSbomsTile() {
    return locator("#recently-imported-sboms-tile");
  }

  /**
   * C/C++ info alert — rendered when {@code isCpeMatchingSupported} is {@code true}
   * and the alert has not been dismissed via localStorage.
   */
  public Locator cppSupportAlert() {
    return locator(ROOT + " .nx-alert--info");
  }

  /** Documentation link inside the C/C++ support alert. */
  public Locator cppSupportAlertDocLink() {
    return cppSupportAlert().getByRole(AriaRole.LINK);
  }

  /** Close button inside the C/C++ support alert. */
  public Locator cppSupportAlertCloseButton() {
    return cppSupportAlert().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Close"));
  }

  /**
   * {@code NxLoadWrapper} error alert rendered inside the dashboard when the SBOM Manager
   * feature flag is absent from {@code /rest/product/features}. The error text reads
   * "The SBOM Manager license feature is not enabled."
   */
  public Locator loadWrapperError() {
    return nxLoadErrorAlert(locator(ROOT));
  }
}
