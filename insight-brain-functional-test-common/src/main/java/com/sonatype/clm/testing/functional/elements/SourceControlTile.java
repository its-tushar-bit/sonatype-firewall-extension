/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class SourceControlTile
    extends OwnerTile
{
  private static final String SOURCE_CONTROL_OWNER_ELEMENT_ID = "#owner-pill-source-control";

  public SourceControlTile() {
    super(SOURCE_CONTROL_OWNER_ELEMENT_ID);
  }

  public ElementsCollection rows() {
    return children(".nx-list__item");
  }

  public SelenideElement itemText() {
    return child(".nx-list__text");
  }

  public SelenideElement itemSubText() {
    return child(".nx-list__subtext");
  }

  public SelenideElement content() {
    return child(".nx-tile-content");
  }

  public SelenideElement sourceControlLink() {
    return child(".nx-list__link");
  }
}
