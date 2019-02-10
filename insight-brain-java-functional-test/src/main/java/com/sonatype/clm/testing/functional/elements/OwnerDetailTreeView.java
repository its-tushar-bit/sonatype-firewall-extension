/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class OwnerDetailTreeView
{
  private static final String DETAIL_TREE_VIEW_SELECTOR = "#owner-editing-detail-tree-view";

  public static SelenideElement root() {
    return $(DETAIL_TREE_VIEW_SELECTOR);
  }

  public static SelenideElement header() {
    return root().$(".iq-owner-tree-view__header");
  }

  public static SelenideElement backLink() {
    return $(SelectorUtils.createSelector(DETAIL_TREE_VIEW_SELECTOR, ".iq-owner-tree-view__header", "a"));
  }

  public static String headerHref() {
    return header().$("a").attr("href");
  }

  public static OwnerDetailTreeViewGroup applicationCategoryGroup() {
    return new OwnerDetailTreeViewGroup("#application-category-tree-view-group");
  }

  public static OwnerDetailTreeViewGroup policyGroup() {
    return new OwnerDetailTreeViewGroup("#policy-tree-view-group");
  }

  public static OwnerDetailTreeViewGroup componentLabelGroup() {
    return new OwnerDetailTreeViewGroup("#label-tree-view-group");
  }

  public static OwnerDetailTreeViewGroup ltgGroup() {
    return new OwnerDetailTreeViewGroup("#license-threat-group-tree-view-group");
  }

  public static OwnerDetailTreeViewGroup accessGroup() {
    return new OwnerDetailTreeViewGroup("#access-tree-view-group");
  }

  public static class OwnerDetailTreeViewGroup
      extends BasicElement<OwnerDetailTreeViewGroup>
  {
    public OwnerDetailTreeViewGroup(String... selectors) {
      super(selectors);
    }

    public SelenideElement twisty() {
      return child(".iq-tree-view__twisty-icon");
    }

    public ElementsCollection items() {
      return children(".iq-tree-view__child");
    }

    public ElementsCollection entryItems() {
      return children(".iq-tree-view__child:nth-child(n+2)"); // skip 'Add ...' entries
    }

    public OwnerDetailTreeViewItem item(int num) {
      return new OwnerDetailTreeViewItem(childSelector(".iq-tree-view__child", nthChild(num + 1)));
    }

    public static class OwnerDetailTreeViewItem
        extends BasicElement<OwnerDetailTreeViewItem>
    {
      public OwnerDetailTreeViewItem(String... selectors) {
        super(selectors);
      }

      public SelenideElement icon() {
        return child(".iq-owner-tree-view__icon");
      }
    }
  }
}
