/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class ViolationsTabContent
    extends BasicElement<ViolationsTabContent>
{
  public static final String VIOLATIONS_TAB_SELECTOR = "#component-details-policy-violations";

  public ViolationsTabContent() {
    super(VIOLATIONS_TAB_SELECTOR);
  }

  public PolicyViolationsTable policyViolationsTable() {
    return PolicyViolationsTable.getPolicyViolationsTableForParent(VIOLATIONS_TAB_SELECTOR);
  }

  public SelenideElement componentWaiversButton() {
    return child("#component-details-view-waivers");
  }
}
