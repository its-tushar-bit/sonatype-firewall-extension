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

public class VulnerabilitiesTable
    extends BasicElement<VulnerabilitiesTable>
{
  static final String TABLE_SELECTOR = ".iq-policy-vulnerability-table";

  public static VulnerabilitiesTable getVulnerabilitiesTableForParent(String parentSelector) {
    String combinedSelector = SelectorUtils.createSelector(parentSelector, TABLE_SELECTOR);
    return new VulnerabilitiesTable(combinedSelector);
  }

  private VulnerabilitiesTable(String selectorStringWithParent) {
    super(selectorStringWithParent);
  }

  public SelenideElement getHeaderRow() {
    return child("thead > tr:first-child");
  }

  public ElementsCollection getRows() {
    return children("tbody > tr");
  }

  public SelenideElement getRow(int rowIndex) {
    return child("tbody > tr:nth-child(" + rowIndex + ")");
  }
}
