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

public class FirewallPolicyViolationsTable
    extends BasicElement<FirewallPolicyViolationsTable>
{
  static final String TABLE_SELECTOR = ".firewall-policy-violation-table";

  public static FirewallPolicyViolationsTable getPolicyViolationsTableForParent(String parentSelector) {
    String combinedSelector = SelectorUtils.createSelector(parentSelector, TABLE_SELECTOR);
    return new FirewallPolicyViolationsTable(combinedSelector);
  }

  private FirewallPolicyViolationsTable(String selectorStringWithParent) {
    super(selectorStringWithParent);
  }

  public ElementsCollection getRows() {
    return children("tbody > tr");
  }

  public SelenideElement getRow(int rowIndex) {
    return child("tbody > tr.nx-clickable:nth-child(" + rowIndex + ")");
  }

  public SelenideElement waiversDropdownButton(int rowIndex) {
    return getRow(rowIndex).find(".iq-policy-violation__waivers-dropdown-btn");
  }

  public SelenideElement waiversDropdownArrow(int rowIndex) {
    return waiversDropdownButton(rowIndex).find("button.nx-segmented-btn__dropdown-btn");
  }

  public SelenideElement requestWaiverDropdownButton(int rowIndex) {
    return waiversDropdownButton(rowIndex).find("button.nx-dropdown-button");
  }

  public SelenideElement addWaiverButton(int rowIndex) {
    return waiversDropdownButton(rowIndex).find("#policy-violations-add-waiver-button");
  }

  public ElementsCollection getCellsByNthRow(int rowIndex) {
    return children("tbody > tr:nth-child(" + rowIndex + ") .nx-cell");
  }
}
