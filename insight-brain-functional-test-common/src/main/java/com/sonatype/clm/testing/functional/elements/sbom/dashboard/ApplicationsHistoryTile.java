/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.dashboard;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthOfType;

public class ApplicationsHistoryTile
    extends SbomDashboardTile
{
  private static final String ROOT = "#applications-history-tile";

  public ApplicationsHistoryTile() {
    super(ROOT);
  }

  public ApplicationsList applicationsList() {
    return new ApplicationsList();
  }

  public SelenideElement link() {
    return child(".sbom-manager-applications-history-tile__link");
  }

  public class ApplicationsList
      extends BasicElement<ApplicationsList>
  {
    ApplicationsList() {
      super(ROOT, ".sbom-manager-applications-history-tile-list");
    }

    public SelenideElement listLabel(int index) {
      return child(createSelector(".sbom-manager-applications-history-tile-list__label", nthOfType(index + 1)));
    }

    public SelenideElement listValue(int index) {
      return child(createSelector(".sbom-manager-applications-history-tile-list__value", nthOfType(index + 1)));
    }
  }
}
