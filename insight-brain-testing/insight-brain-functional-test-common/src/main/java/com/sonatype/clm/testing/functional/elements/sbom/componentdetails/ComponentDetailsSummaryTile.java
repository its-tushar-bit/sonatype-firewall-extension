/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class ComponentDetailsSummaryTile
    extends BasicElement<ComponentDetailsSummaryTile>
{
  static final String ROOT_SELECTOR = ".sbom-manager-component-detail-tile";

  public ComponentDetailsSummaryTile() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement header() {
    return child("header.nx-tile-header .nx-h2");
  }

  public SelenideElement highestScoreLabel() {
    return child(".sbom-manager-component-detail-tile__highest-cvss-score .nx-read-only__label");
  }

  public SelenideElement highestScoreValue() {
    return child(".sbom-manager-component-detail-tile__highest-cvss-score .nx-read-only__data " +
        "span[data-testid='highestCvssScore']");
  }

  public SelenideElement vulnerabilitiesVerifiedLabel() {
    return child(".sbom-manager-component-detail-tile__vulnerabilities-verified .nx-read-only__label");
  }

  public SelenideElement policyViolationsLabel() {
    return child(".sbom-manager-component-detail-tile__policy-violations .nx-read-only__label");
  }

  public SelenideElement sonatypeVerified() {
    return child(".sbom-manager-component-detail-tile__vulnerabilities-verified .nx-read-only__data " +
        "span[data-testid='verified']");
  }

  public SelenideElement unVerified() {
    return child(".sbom-manager-component-detail-tile__vulnerabilities-verified .nx-read-only__data " +
        "span[data-testid='unverified']");
  }

  public SelenideElement severePolicyViolation() {
    return child(".sbom-manager-component-detail-tile__policy-violations .nx-small-threat-counter--severe " +
        ".nx-small-threat-counter__count");
  }

  public SelenideElement criticalPolicyViolation() {
    return child(".sbom-manager-component-detail-tile__policy-violations .nx-small-threat-counter--critical " +
        ".nx-small-threat-counter__count");
  }
}
