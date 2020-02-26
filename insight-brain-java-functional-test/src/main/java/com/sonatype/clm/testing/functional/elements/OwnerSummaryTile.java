/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class OwnerSummaryTile
    extends BasicElement<OwnerSummaryTile>
{
  public OwnerSummaryTile() {
    super("#owner-summary");
  }

  public SelenideElement name() {
    return child(".nx-h1");
  }

  public SelenideElement publicId() {
    return child(".iq-owner-public-id");
  }

  public SelenideElement headerIcon() {
    return child(".nx-page-title__page-icon", "img");
  }

  public SelenideElement contact() {
    return child(".nx-page-title__description");
  }

  public ErrorBox error() {
    return new ErrorBox(selector, ".iq-alert.iq-alert--error");
  }

  private SelenideElement scrollContainer() {
    return $(".iq-tile-scroll-container");
  }

  public PillButton appCategoriesButton() {
    return new PillButton(scrollContainer(), "#owner-app-categories-button");
  }

  public PillButton policyButton() {
    return new PillButton(scrollContainer(), "#owner-policy-button");
  }

  public PillButton labelsButton() {
    return new PillButton(scrollContainer(), "#owner-comp-labels-button");
  }

  public PillButton ltgsButton() {
    return new PillButton(scrollContainer(), "#owner-ltgs-button");
  }

  public PillButton dataRetentionButton() {
    return new PillButton(scrollContainer(), "#owner-data-retention-button");
  }

  public PillButton sourceControlButton() {
    return new PillButton(scrollContainer(), "#owner-source-control-button");
  }

  public PillButton accessButton() {
    return new PillButton(scrollContainer(), "#owner-access-button");
  }

  public SelenideElement dropdownButton() {
    return child("#nav-pill-dropdown");
  }
}
