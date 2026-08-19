/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$$;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public abstract class GreedyTable<RowElementT>
    extends BasicElement<GreedyTable<RowElementT>>
{
  public GreedyTable(String... selectors) {
    super(selectors);
  }

  public ElementsCollection rows() {
    return children("tr");
  }

  public abstract RowElementT row(int i);

  public ElementsCollection selectedHeaderElements() {
    return $$(createSelector(selector, "thead th .up") + ", " + createSelector(selector, "thead th .down"));
  }

  public SelenideElement nxSelectedHeaderElements() {
    return $$(createSelector(selector, "thead th .fa-sort-up")).get(0);
  }

  public HeaderColumn header(int num) {
    return new HeaderColumn(child("thead th", nthChild(num + 1)));
  }

  public HeaderColumn selectedHeaderColumn() {
    // XXX this seems very inefficient
    return new HeaderColumn(selectedHeaderElements().get(0).parent().parent().parent());
  }

  public HeaderColumn nxSelectedHeaderColumn() {
    return new HeaderColumn(nxSelectedHeaderElements().closest(".nx-cell"));
  }

  public static class HeaderColumn
  {
    public static final WebElementCondition UP_SELECTED = cssClass("up");

    public static final String NX_UP_SELECTED = ".fa-sort-up";

    public static final WebElementCondition DOWN_SELECTED = cssClass("down");

    public static final String NX_DOWN_SELECTED = ".fa-sort-down";

    public static final WebElementCondition COLUMN_SELECTED = cssClass("selected-column");

    public SelenideElement root;

    public HeaderColumn(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement anchor() {
      return root.$("a");
    }

    public SelenideElement upArrow() {
      return root.$(".fa-caret-up");
    }

    public SelenideElement downArrow() {
      return root.$(".fa-caret-down");
    }

    public SelenideElement nxAnchor() {
      return root.$("button");
    }

    public SelenideElement nxAnchorHeader() {
      return root.$("span");
    }

    public SelenideElement sort(String sortClass) {
      return root.$(".nx-cell__sort-icons " + sortClass);
    }

    public SelenideElement name() {
      return root.$(".iq-cell__text--header");
    }
  }
}
