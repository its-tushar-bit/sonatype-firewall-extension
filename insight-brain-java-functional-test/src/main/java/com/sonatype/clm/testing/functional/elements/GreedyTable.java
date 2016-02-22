/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.selector;

public abstract class GreedyTable<RowElement>
    extends BasicElement<GreedyTable<RowElement>>
{
  public GreedyTable(String... selectors) {
    super(selectors);
  }

  public ElementsCollection rows() {
    return $$(selector(selector, "tr"));
  }

  public abstract RowElement row(int i);

  public ElementsCollection selectedHeaderElements() {
    return $$(selector(selector, "thead th .up") + ", " + selector(selector, "thead th .down"));
  }

  public HeaderColumn header(int i) {
    return new HeaderColumn($(selector(selector, "thead th", nthChild(i))));
  }

  public HeaderColumn selectedHeaderColumn() {
    // XXX this seems very inefficient
    return new HeaderColumn(selectedHeaderElements().get(0).parent().parent());
  }

  public static class HeaderColumn
  {
    public static final Condition UP_SELECTED = cssClass("up");

    public static final Condition DOWN_SELECTED = cssClass("down");

    public static final Condition COLUMN_SELECTED = cssClass("selected-column");

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

    public SelenideElement name() {
      return root.$(".header-text");
    }
  }
}
