/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class OtherVersionsTable
    extends BasicElement<ViolationsTabContent>
{
  static final String TABLE_SELECTOR = ".iq-quarantine-report-component-other-versions .nx-table";

  public static OtherVersionsTable getOtherVersionsTableForParent(String parentSelector) {
    String combinedSelector = SelectorUtils.createSelector(parentSelector, TABLE_SELECTOR);
    return new OtherVersionsTable(combinedSelector);
  }

  private OtherVersionsTable(String selectorStringWithParent) {
    super(selectorStringWithParent);
  }

  public ElementsCollection getRows() {
    return children("tbody > tr");
  }

  public SelenideElement getRow(int rowIndex) {
    return child("tbody > tr:nth-child(" + rowIndex + ")");
  }
}
