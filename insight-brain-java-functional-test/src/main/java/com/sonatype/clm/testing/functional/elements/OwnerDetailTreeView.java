/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;

public class OwnerDetailTreeView
{
  private static final String DETAIL_TREE_VIEW_SELECTOR = "#owner-editing-detail-tree-view";

  public static SelenideElement root() {
    return $(DETAIL_TREE_VIEW_SELECTOR);
  }

  public static SelenideElement header() {
    return root().$(".tree-view-header");
  }

  public static String headerHref(){
    return header().$("a").attr("href");
  }

  public static OwnerDetailTreeViewGroup applicationCategoryGroup() {
    return new OwnerDetailTreeViewGroup(root().$$(".tree-view-group").get(0));
  }

  public static OwnerDetailTreeViewGroup policyGroup() {
    return new OwnerDetailTreeViewGroup(root().$$(".tree-view-group").get(1));
  }

  public static OwnerDetailTreeViewGroup componentLabelGroup() {
    return new OwnerDetailTreeViewGroup(root().$$(".tree-view-group").get(2));
  }

  public static OwnerDetailTreeViewGroup LTGGroup() {
    return new OwnerDetailTreeViewGroup(root().$$(".tree-view-group").get(3));
  }

  public static OwnerDetailTreeViewGroup accessGroup() {
    return new OwnerDetailTreeViewGroup(root().$$(".tree-view-group").get(4));
  }

  public static class OwnerDetailTreeViewGroup
  {
    public static final Condition TWISTY_EXPAND_CLASS = cssClass("expand");

    public static final Condition TWISTY_COLLAPSE_CLASS = cssClass("collapse");

    private SelenideElement root;

    public OwnerDetailTreeViewGroup(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement root() {
      return root;
    }

    public SelenideElement twisty() {
      return root().$(".twisty");
    }

    public ElementsCollection items() {
      return root().$$(".tree-view-item");
    }

    public OwnerDetailTreeViewItem item(int num) {
      return new OwnerDetailTreeViewItem(root().$$(".tree-view-item").get(num));
    }

    public static class OwnerDetailTreeViewItem
    {
      public static final Condition SELECTED_CLASS = cssClass("selected");

      private SelenideElement root;

      public OwnerDetailTreeViewItem(SelenideElement root) {
        this.root = root;
      }

      public SelenideElement root() {
        return root;
      }

      public SelenideElement icon() {
        return root.$(".fa, .hexagon");
      }
    }
  }
}
