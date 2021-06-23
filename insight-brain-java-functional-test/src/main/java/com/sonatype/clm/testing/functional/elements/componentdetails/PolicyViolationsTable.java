/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.ElementsCollection;

public class PolicyViolationsTable
    extends BasicElement<ViolationsTabContent>
{
  static final String TABLE_SELECTOR = ".iq-policy-violations-table";

  public static PolicyViolationsTable getPolicyViolationsTableForParent(String parentSelector) {
    String combinedSelector = SelectorUtils.createSelector(parentSelector, TABLE_SELECTOR);
    return new PolicyViolationsTable(combinedSelector);
  }

  private PolicyViolationsTable(String selectorStringWithParent) {
    super(selectorStringWithParent);
  }

  public ElementsCollection getRows() {
    return children("tbody > tr");
  }
}
