/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.Selenide.$;

public class AccessTile
    extends OwnerTile
{
  public AccessTile() {
    super($("#owner-pill-access"));
  }

  public static Condition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public static Condition subHeaderText(String ownerName) {
    return Condition.text(ownerName + " users by role");
  }

  public ElementsCollection accessLists() {
    return root.$$(".simple-list");
  }

  public AccessTileList accessList(int num) {
    return new AccessTileList(accessLists().get(num));
  }
}
