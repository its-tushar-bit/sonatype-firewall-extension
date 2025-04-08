/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;

public class AutoWaiversTile
    extends OwnerTile
{
  private static final String AUTO_WAIVERS_TILE_ID = "#owner-pill-auto-waivers-configuration";

  public AutoWaiversTile() {
    super(AUTO_WAIVERS_TILE_ID);
  }

  public ElementsCollection configRows() {
    return children(".nx-list__item");
  }
}
