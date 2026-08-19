/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.dashboard;

import com.codeborne.selenide.ElementsCollection;

public class SbomReleaseStatusTile
    extends SbomDashboardTile
{
  private static final String ROOT = "#sbom-release-status-tile";

  public SbomReleaseStatusTile() {
    super(ROOT);
  }

  public ElementsCollection tileLabels() {
    return children(".sbom-manager-sbom-release-status-meter-bar__status");
  }

  public ElementsCollection tileMeterBars() {
    return children(".nx-meter");
  }

  public ElementsCollection tileLabelValues() {
    return children(".sbom-manager-sbom-release-status-meter-bar__sbom-count");
  }
}
