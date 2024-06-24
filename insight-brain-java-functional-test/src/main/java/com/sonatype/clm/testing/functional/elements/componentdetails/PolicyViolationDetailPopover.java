/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationApplicableWaiversInfoTile;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationApplicableWaiversTab;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationSimilarWaiversInfoTile;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationSimilarWaiversTab;

import com.codeborne.selenide.SelenideElement;

public class PolicyViolationDetailPopover
    extends BasicElement<PolicyViolationDetailPopover>
{
  static final String POPOVER_SELECTOR = "#component-details-policy-violations-popover";

  public PolicyViolationDetailPopover() {
    super(POPOVER_SELECTOR);
  }

  public SelenideElement headerPopoverTitle() {
    return child("#policy-violation-details-popover-title");
  }

  public SelenideElement getAddWaiversButton() {
    return child("#violation-page-add-waiver");
  }

  public SelenideElement getAddWaiversSegmentedDropdownButton() {
    return child("#violation-page-add-waiver .nx-segmented-btn__dropdown-btn");
  }

  public SelenideElement getRequestWaiversButton() {
    return child("#violation-page-request-waiver");
  }

  public SelenideElement getCloseButton() {
    return child(".nx-drawer-header__close-button");
  }

  // FIXME Remove this CLM-28689
  public SelenideElement getCloseFirewallButton() {
    return child("#policy-violation-close-btn");
  }

  public SelenideElement popoverList() {
    return child("#policy-violation-reasons");
  }

  public SelenideElement popoverThreatLevel() {
    return child(".iq-threat-level");
  }

  public SelenideElement policyViolationText() {
    return child(".nx-h3 span");
  }

  public SelenideElement getCustomizeButton() {
    return child("#customize-vulnerability-button");
  }

  // The resulting object is tied to ViolationDetailsPage as we are reusing that component as part of the contents here
  public PolicyViolationApplicableWaiversInfoTile applicableWaiversInfoTile() {
    return new PolicyViolationApplicableWaiversInfoTile(childSelector("#applicable-waivers-tile"));
  }

  // The resulting object is tied to ViolationDetailsPage as we are reusing that component as part of the contents here
  public PolicyViolationApplicableWaiversTab applicableWaiversTab() {
    return new PolicyViolationApplicableWaiversTab(childSelector("#violation-applicable-waivers-tab"));
  }

  // The resulting object is tied to ViolationDetailsPage as we are reusing that component as part of the contents here
  public PolicyViolationSimilarWaiversInfoTile similarWaiversInfoTile() {
    return new PolicyViolationSimilarWaiversInfoTile(childSelector("#similar-waivers-tile"));
  }

  // The resulting object is tied to ViolationDetailsPage as we are reusing that component as part of the contents here
  public PolicyViolationSimilarWaiversTab similarWaiversTab() {
    return new PolicyViolationSimilarWaiversTab(childSelector("#violation-similar-waivers-tab"));
  }

  public SelenideElement securityVulnerabilityDetailsTab() {
    return child("#violation-security-vulnerability-details-tab");
  }
}
