/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.Selenide.$$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.selector;

public class BaseAccessTile
    extends OwnerTile
{
  public BaseAccessTile(String root) {
    super(root);
  }

  public static Condition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public static Condition subHeaderText(String ownerName) {
    return Condition.text(ownerName + " users by role");
  }

  private String accessListSelector() {
    return selector(selector, ".simple-list");
  }

  public ElementsCollection accessLists() {
    return $$(accessListSelector());
  }

  public AccessTileList accessList(int num) {
    return new AccessTileList(accessListSelector(),
        nthChild(num + 1 + 2 /* some other elements have the same parent */));
  }
}
