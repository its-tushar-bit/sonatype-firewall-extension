/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages.sbom;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.sonatype.clm.testing.functional.elements.sbom.dashboard.ApplicationsHistoryTile;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.HighPriorityVulnerabilitiesTile;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.RecentlyImportedSBOMsTile;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.SbomReleaseStatusTile;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.TotalSBOMsStoredTile;
import com.sonatype.clm.testing.functional.elements.sbom.dashboard.VulnerabilitiesThreatLevelTile;

import com.codeborne.selenide.SelenideElement;

public class SbomManagerDashboardPage
    extends BasicElement<SbomManagerDashboardPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/sbomManager/dashboard");
  }

  public SelenideElement container() {
    return child("#sbom-manager-dashboard");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public SelenideElement errorAlert() {
    return child(".nx-alert--error");
  }

  public SelenideElement toolTip() {
    return child(".nx-tooltip");
  }

  public static RecentlyImportedSBOMsTile recentlyImportedSBOMsTile() {
    return new RecentlyImportedSBOMsTile();
  }

  public static TotalSBOMsStoredTile totalSBOMsStoredTile() {
    return new TotalSBOMsStoredTile();
  }

  public static ApplicationsHistoryTile applicationsHistoryTile() {
    return new ApplicationsHistoryTile();
  }

  public static HighPriorityVulnerabilitiesTile highPriorityVulnerabilitiesTile() {
    return new HighPriorityVulnerabilitiesTile();
  }

  public static VulnerabilitiesThreatLevelTile vulnerabilitiesThreatLevelTile() {
    return new VulnerabilitiesThreatLevelTile();
  }

  public static SbomReleaseStatusTile sbomReleaseStatusTile() {
    return new SbomReleaseStatusTile();
  }
}
