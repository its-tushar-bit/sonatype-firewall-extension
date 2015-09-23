/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.Selenide.$;

public class LabelTile
    extends OwnerTile
{
  private static final String LABEL_OWNER_ELEMENT_ID = "#owner-pill-comp-labels";

  public LabelTile() {
    super($(LABEL_OWNER_ELEMENT_ID));
  }

  public static Condition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public static Condition subHeaderText(String ownerName) {
    return Condition.text("available to " + ownerName + " policies");
  }

  public ElementsCollection labelLists() {
    return root.$$(".simple-list");
  }

  public TileSimpleList labelList(int num) {
    return new TileSimpleList(labelLists().get(num));
  }
}
