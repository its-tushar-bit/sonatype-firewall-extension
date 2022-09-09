/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class PolicyViolationDetailPopover
    extends BasicElement<PolicyViolationDetailPopover>
{
  static final String POPOVER_SELECTOR = "#component-details-policy-violations-popover";

  public PolicyViolationDetailPopover() {
    super(POPOVER_SELECTOR);
  }

  public SelenideElement getManageWaiversButton() {
    return child("#violation-page-manage-waivers");
  }

  public SelenideElement getCloseButton() {
    return child("#policy-violation-close-btn");
  }

  public SelenideElement popoverList() {
    return child("#policy-violation-reasons");
  }

  public SelenideElement popoverHeaderTitle() {
    return child(".nx-tile-header__title span em");
  }

  public SelenideElement popoverThreatLevel() {
    return child(".iq-threat-level");
  }

  public SelenideElement policyViolationText() {
    return child(".nx-h3 strong");
  }
}
