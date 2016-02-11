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

public class PolicyTileList
    extends BasicElement<PolicyTileList>
{

  public PolicyTileList(String... selectors) {
    super(selectors);
  }

  public ElementsCollection elements() {
    return $$(selector(selector, "tr"));
  }

  public PolicyTileListElement element(int num) {
    return new PolicyTileListElement(selector, "tbody tr", nthChild(num));
  }

  public SelenideElement ownerName() {
    return $(selector(selector, ".subsection-header"));
  }

  public SelenideElement emptyDescriptor() {
    return $(selector(selector, ".empty-list"));
  }

  public PolicyTileHeaderColumn threatLegendHeaderColumn() {
    return new PolicyTileHeaderColumn($(selector(selector, "thead th", nthChild(1))));
  }

  public PolicyTileHeaderColumn nameHeaderColumn() {
    return new PolicyTileHeaderColumn($(selector(selector, "thead th", nthChild(2))));
  }

  public ElementsCollection selectedHeaderElements() {
    return $$(selector(selector, "thead th .up") + ", " + selector(selector, "thead th .down"));
  }

  public PolicyTileHeaderColumn selectedHeaderColumn() {
    // XXX this seems very inefficient
    return new PolicyTileHeaderColumn(selectedHeaderElements().get(0).parent().parent());
  }

  public static Condition threatLevel(int threatLevel) {
    return cssClass("policy-threat-level-" + threatLevel);
  }

  public static class PolicyTileListElement
      extends BasicElement<PolicyTileListElement>
  {
    public static final Condition WARN_ICON = cssClass("fa-exclamation-triangle");

    public static final Condition WARN = cssClass("warn");

    public static final Condition FAIL_ICON = cssClass("fa-exclamation-circle");

    public static final Condition FAIL = cssClass("fail");

    public PolicyTileListElement(String... selectors) {
      super(selectors);
    }

    public SelenideElement threadLegend() {
      return $(selector(selector, ".threat"));
    }

    public SelenideElement name() {
      return $(selector(selector, ".threat-name"));
    }

    public SelenideElement proxy() {
      return $(selector(selector, "td", nthChild(2)));
    }

    public SelenideElement develop() {
      return $(selector(selector, "td", nthChild(3)));
    }

    public SelenideElement build() {
      return $(selector(selector, "td", nthChild(4)));
    }

    public SelenideElement stageRelease() {
      return $(selector(selector, "td", nthChild(5)));
    }

    public SelenideElement release() {
      return $(selector(selector, "td", nthChild(6)));
    }

    public SelenideElement operate() {
      return $(selector(selector, "td", nthChild(7)));
    }

    public SelenideElement chevron() {
      return $(selector(selector, ".fa-chevron-right"));
    }
  }

  public static class PolicyTileHeaderColumn
  {
    public static final Condition UP_SELECTED = cssClass("up");

    public static final Condition DOWN_SELECTED = cssClass("down");

    public static final Condition COLUMN_SELECTED = cssClass("selected-column");

    public SelenideElement root;

    public PolicyTileHeaderColumn(SelenideElement root) {
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
