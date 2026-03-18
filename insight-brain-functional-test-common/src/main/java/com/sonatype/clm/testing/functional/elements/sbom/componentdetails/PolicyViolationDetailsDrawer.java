/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class PolicyViolationDetailsDrawer
    extends BasicElement<PolicyViolationDetailsDrawer>
{
  static final String ROOT_SELECTOR = "#sbom-manager-policy-violation-details-drawer";

  public PolicyViolationDetailsDrawer() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement title() {
    return child("#policy-violation-details-drawer-title");
  }

  public SelenideElement closeButton() {
    return child("header .nx-btn--close");
  }

  public static SbomManagerViolationDetailsTile sbomManagerViolationDetailsTile() {
    return new SbomManagerViolationDetailsTile();
  }

  public static PolicyViolationConstraintInfo policyViolationConstraintInfo() {
    return new PolicyViolationConstraintInfo();
  }

  public static class SbomManagerViolationDetailsTile
      extends BasicElement<SbomManagerViolationDetailsTile>
  {
    public SbomManagerViolationDetailsTile() {
      super("#sbom-manager-violation-details-tile");
    }

    public SelenideElement threatLevelValue() {
      return child(".sbom-manager-violation-details__threat-level dd");
    }

    public SelenideElement policyTypeValue() {
      return child(".sbom-manager-violation-details__policy-type dd");
    }
  }

  public static class PolicyViolationConstraintInfo
      extends BasicElement<PolicyViolationConstraintInfo>
  {
    public PolicyViolationConstraintInfo() {
      super("#policy-violation-constraint-info");
    }

    public SelenideElement title() {
      return child(".nx-h3");
    }

    public SelenideElement reasons() {
      return child("#policy-violation-reasons");
    }

    public SelenideElement packageUrl() {
      return child(".sbom-vulnerability-details > span");
    }
  }

  public static VulnerabilityDetails vulnerabilityDetails() {
    return new VulnerabilityDetails();
  }

  public static class VulnerabilityDetails
      extends BasicElement<VulnerabilityDetails>
  {
    public SelenideElement packageUrl() {
      return child(".sbom-vulnerability-details > span");
    }

    public SelenideElement vulnerabilityId() {
      return child(".sbom-vulnerability-details .sbom-vulnerability-details__vulnerability-id");
    }

    public SelenideElement getVulnerabilityDetailsContentByFirstColumnIdx(int num) {
      return child(".sbom-vulnerability-details .nx-grid-col", nthChild(1), ".nx-read-only",
          nthChild(num + 1), ".nx-read-only__data");
    }

    public SelenideElement getVulnerabilityDetailsContentBySecondColumnIdx(int num) {
      return child(".sbom-vulnerability-details .nx-grid-col", nthChild(2), ".nx-read-only",
          nthChild(num), ".nx-read-only__data");
    }
  }
}
