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

public class PolicyTileList
{

  protected SelenideElement root;

  public PolicyTileList(SelenideElement root) {
    this.root = root;
  }

  public ElementsCollection elements() {
    return root.$$("tr");
  }

  public PolicyTileListElement element(int num) {
    return new PolicyTileListElement(elements().get(num));
  }

  public SelenideElement ownerName() {
    return root.$(".subsection-header");
  }

  public SelenideElement emptyDescriptor() {
    return root.$(".empty-list");
  }

  public PolicyTileHeaderColumn threatLegendHeaderColumn() {
    return new PolicyTileHeaderColumn(root.$$("thead th").get(0));
  }

  public PolicyTileHeaderColumn nameHeaderColumn() {
    return new PolicyTileHeaderColumn(root.$$("thead th").get(1));
  }

  public ElementsCollection selectedHeaderElements() {
    return root.$$("thead th .up, thead th .down");
  }

  public PolicyTileHeaderColumn selectedHeaderColumn() {
    return new PolicyTileHeaderColumn(selectedHeaderElements().get(0).parent().parent());
  }

  public static Condition threatLevel(int threatLevel) {
    return cssClass("policy-threat-level-" + threatLevel);
  }

  public static class PolicyTileListElement
  {
    public static final Condition WARN_ICON = cssClass("fa-exclamation-triangle");

    public static final Condition WARN = cssClass("warn");

    public static final Condition FAIL_ICON = cssClass("fa-exclamation-circle");

    public static final Condition FAIL = cssClass("fail");

    public SelenideElement root;

    public PolicyTileListElement(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement threadLegend() {
      return root.$(".threat");
    }

    public SelenideElement name() {
      return root.$(".threat-name");
    }

    public SelenideElement proxy() {
      return root.$$("td").get(1);
    }

    public SelenideElement develop() {
      return root.$$("td").get(2);
    }

    public SelenideElement build() {
      return root.$$("td").get(3);
    }

    public SelenideElement stageRelease() {
      return root.$$("td").get(4);
    }

    public SelenideElement release() {
      return root.$$("td").get(5);
    }

    public SelenideElement operate() {
      return root.$$("td").get(6);
    }

    public SelenideElement chevron() {
      return root.$(".fa-chevron-right");
    }
  }

  public static class PolicyTileHeaderColumn
  {
    public static final Condition UP_SELECTED = cssClass("up");

    public static Condition DOWN_SELECTED = cssClass("down");

    public static Condition COLUMN_SELECTED = cssClass("selected-column");

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
