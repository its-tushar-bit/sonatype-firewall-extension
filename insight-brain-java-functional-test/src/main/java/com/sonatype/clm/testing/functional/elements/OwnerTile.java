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
  private static final String TILE_HEADER_TITLE_SELECTOR = ".nx-tile-header__title";

  private static final String TILE_HEADER_SUBTITLE_SELECTOR = ".nx-tile-header__subtitle";

  public OwnerTile(String... selectors) {
    super(selectors);
  }

  public SelenideElement nxHeader() {
    return child(TILE_HEADER_TITLE_SELECTOR);
  }

  public SelenideElement header() {
    return child(".iq-tile-header__title");
  }

  public SelenideElement subHeader() {
    return child("h3.nx-tile-header__subtitle");
  }

  public SelenideElement nxSubHeader() {
    return child(TILE_HEADER_SUBTITLE_SELECTOR);
  }

  public SelenideElement newButton() {
    return getElement().find("button");
  }

  public SelenideElement addRoleButton() {
    return child("#add-role-button");
  }
}
