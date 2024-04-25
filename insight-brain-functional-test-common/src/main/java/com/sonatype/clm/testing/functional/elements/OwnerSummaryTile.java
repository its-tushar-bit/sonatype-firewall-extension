/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class OwnerSummaryTile
    extends BasicElement<OwnerSummaryTile>
{
  public OwnerSummaryTile() {
    super("#owner-summary");
  }

  public OwnerSummaryTile(String rootSelector) {
    super(rootSelector);
  }

  public SelenideElement name() {
    return child(".nx-h1");
  }

  public SelenideElement publicId() {
    return child(".iq-owner-public-id");
  }

  public SelenideElement headerIcon() {
    return child(".nx-icon", "img");
  }

  public SelenideElement contact() {
    return child(".nx-page-title__description");
  }

  public ErrorBox error() {
    return new ErrorBox(selector, ".iq-alert.iq-alert--error");
  }

  public PillButton appCategoriesButton() {
    return new PillButton("#owner-pill-app-categories-button");
  }

  public PillButton policyButton() {
    return new PillButton("#owner-pill-policy-button");
  }

  public PillButton labelsButton() {
    return new PillButton("#owner-pill-comp-labels-button");
  }

  public PillButton ltgsButton() {
    return new PillButton("#owner-pill-ltgs-button");
  }

  public PillButton dataRetentionButton() {
    return new PillButton("#owner-pill-retention-button");
  }

  public PillButton sourceControlButton() {
    return new PillButton("#owner-pill-source-control-button");
  }

  public PillButton innerSourceRepositoryButton() {
    return new PillButton("#owner-pill-innersource-repository-button");
  }

  public PillButton artifactoryRepositoryButton() {
    return new PillButton("#owner-pill-artifactory-repository-button");
  }

  public PillButton accessButton() {
    return new PillButton("#access-tile-pill-access-button");
  }
}
