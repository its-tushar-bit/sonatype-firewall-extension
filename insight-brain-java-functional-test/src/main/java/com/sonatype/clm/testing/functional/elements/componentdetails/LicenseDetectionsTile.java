/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.SelenideElement;

public class LicenseDetectionsTile
    extends BasicElement<LicenseDetectionsTile>
{
  private static final String TILE_SELECTOR = "#component-details-legal-license-detections-tile";

  private LicenseDetectionsTile(String selectorStringWithParent) {
    super(selectorStringWithParent);
  }

  public static LicenseDetectionsTile getLicenseDetectionsTileForParent(String parentSelector) {
    String combinedSelector = SelectorUtils.createSelector(parentSelector, TILE_SELECTOR);
    return new LicenseDetectionsTile(combinedSelector);
  }

  public SelenideElement editLicenseButton() {
    return child("#component-details-edit-licenses");
  }
}
