/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.Selenide.$;

public class CategoryTile
    extends OwnerTile
{
  private static final String CATEGORY_OWNER_ELEMENT_ID = "#owner-pill-app-categories";

  public CategoryTile() {
    super($(CATEGORY_OWNER_ELEMENT_ID));
  }

  public static Condition associateAppButtonText() {
    return Condition.text("associate app category");
  }

  public static Condition subHeaderText(String ownerName) {
    return Condition.text("associated with " + ownerName);
  }

  public static Condition emptyAssociateAppCategoryListDescriptorText() {
    return Condition.text("No application categories associated");
  }

  public ElementsCollection categoryLists() {
    return root.$$(".simple-list");
  }

  public TileSimpleList categoryList(int num) {
    return new TileSimpleList(categoryLists().get(num));
  }
}
