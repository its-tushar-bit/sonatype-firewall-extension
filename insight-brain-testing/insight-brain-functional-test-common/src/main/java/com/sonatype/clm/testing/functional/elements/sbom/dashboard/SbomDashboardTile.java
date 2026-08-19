/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.dashboard;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;

public class SbomDashboardTile
    extends BasicElement<SbomDashboardTile>
{
  public SbomDashboardTile(String rootSelector) {
    super(rootSelector);
  }

  public SelenideElement header() {
    return child(".nx-tile-header__title");
  }

  public SelenideElement infoIcon() {
    return child(".fa-circle-info");
  }

  public ElementsCollection allInfoIcons() {
    return children(".fa-circle-info");
  }
}
