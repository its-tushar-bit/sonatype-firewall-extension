/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class RepositoriesSummaryTile
    extends BasicElement<RepositoriesSummaryTile>
{
  public RepositoriesSummaryTile() {
    super("#repositories-summary");
  }

  public SelenideElement name() {
    return child("h1");
  }

  private SelenideElement scrollContainer() {
    return $(".tile-scroll-container");
  }

  public PillButton configButton() {
    return new PillButton(scrollContainer(), "#repositories-configuration-button");
  }

  public PillButton accessButton() {
    return new PillButton(scrollContainer(), "#repositories-access-button");
  }
}
