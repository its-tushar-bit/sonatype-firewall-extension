/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;

public abstract class CategoryTile
    extends OwnerTile
{
  public static Condition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public CategoryTile() {
    super("#owner-pill-app-categories");
  }

  public ElementsCollection categoryLists() {
    return children(".simple-list");
  }

  public TileSimpleList categoryList(int num) {
    return new TileSimpleList(categoryLists().get(num));
  }

  public abstract Condition subHeaderText(String ownerName);

  public abstract Condition buttonText();

  public abstract Condition emptyListDescriptorText();

  public static class CategoryTileAppContext
      extends CategoryTile
  {
    public static final Condition NO_CATEGORIES_DEFINED = Condition.text("No application categories defined");

    @Override
    public Condition subHeaderText(String ownerName) {
      return Condition.text("associated with " + ownerName);
    }

    @Override
    public Condition buttonText() {
      return Condition.text("associate app categories");
    }

    @Override
    public Condition emptyListDescriptorText() {
      return Condition.text("No application categories associated");
    }
  }

  public static class CategoryTileOrgContext
      extends CategoryTile
  {
    @Override
    public Condition subHeaderText(String ownerName) {
      return Condition.text("available to apps in " + ownerName);
    }

    @Override
    public Condition buttonText() {
      return Condition.text("add a category");
    }

    @Override
    public Condition emptyListDescriptorText() {
      return Condition.text("No application categories defined");
    }
  }
}
