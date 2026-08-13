/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.dashboard;

import com.codeborne.selenide.SelenideElement;

public class TotalSBOMsStoredTile
    extends SbomDashboardTile
{
  private static final String ROOT = "#total-sboms-stored-tile";

  public TotalSBOMsStoredTile() {
    super(ROOT);
  }

  public SelenideElement totalSBOMsStored() {
    return child(".sbom-manager-total-sboms-stored-tile__total");
  }

  public SelenideElement sbomProgressBar() {
    return child(".nx-progress-bar__progress");
  }

  public SelenideElement sbomProgressBarLabel() {
    return child(".sbom-manager-total-sboms-stored-tile-progress__label");
  }

  public SelenideElement sbomsAddedMetricLabel() {
    return child(".sbom-manager-total-sboms-stored-tile-progress__total");
  }

  public SelenideElement sbomThresholdLabel() {
    return child(".sbom-manager-total-sboms-stored-tile-progress__threshold");
  }
}
