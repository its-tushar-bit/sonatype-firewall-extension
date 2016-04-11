/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

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

  public static SelenideElement backLink() {
    return $(SelectorUtils.createSelector(DETAIL_TREE_VIEW_SELECTOR, ".tree-view-header", "a"));
  }

  public static String headerHref() {
    return header().$("a").attr("href");
  }

  public static OwnerDetailTreeViewGroup applicationCategoryGroup() {
    return new OwnerDetailTreeViewGroup($("#applicationCategoryTreeViewGroup"));
  }

  public static OwnerDetailTreeViewGroup policyGroup() {
    return new OwnerDetailTreeViewGroup($("#policyTreeViewGroup"));
  }

  public static OwnerDetailTreeViewGroup componentLabelGroup() {
    return new OwnerDetailTreeViewGroup($("#labelTreeViewGroup"));
  }

  public static OwnerDetailTreeViewGroup LTGGroup() {
    return new OwnerDetailTreeViewGroup($("#licenseThreatGroupTreeViewGroup"));
  }

  public static OwnerDetailTreeViewGroup accessGroup() {
    return new OwnerDetailTreeViewGroup($("#accessTreeViewGroup"));
  }

  public static class OwnerDetailTreeViewGroup
  {
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

    public ElementsCollection entryItems() {
      return root().$$(".tree-view-item:nth-child(n+3)"); // skip root and 'Add ...' entries
    }

    public OwnerDetailTreeViewItem item(int num) {
      return new OwnerDetailTreeViewItem(root().$$(".tree-view-item").get(num));
    }

    public static class OwnerDetailTreeViewItem
    {
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
