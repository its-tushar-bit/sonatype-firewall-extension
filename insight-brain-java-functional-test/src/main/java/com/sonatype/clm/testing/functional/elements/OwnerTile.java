/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

public abstract class OwnerTile
    extends BasicElement<OwnerTile>
{
  public OwnerTile(String... selectors) {
    super(selectors);
  }

  public SelenideElement subHeader() {
    return child(".iq-tile-header__subtitle");
  }

  public SelenideElement nxSubHeader() {
    return child(".nx-tile-header__subtitle");
  }

  public SelenideElement newButton() {
    return child("button");
  }
}
