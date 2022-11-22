/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public class RepositoriesSummaryTile
    extends BasicElement<RepositoriesSummaryTile>
{
  public RepositoriesSummaryTile() {
    super("#repositories-summary");
  }

  public SelenideElement name() {
    return child("h1");
  }

  public PillButton configButton() {
    return new PillButton("#repositories-pill-configuration-button");
  }

  public PillButton accessButton() {
    return new PillButton("#repositories-pill-access-button");
  }
}
