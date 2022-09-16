/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.PolicyTileList.PolicyTileListElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class PolicyTileList
    extends GreedyTable<PolicyTileListElement>
{
  public static final Condition CELL_DISABLED = cssClass("policy-tile__cell--disabled");

  public PolicyTileList(String... selectors) {
    super(selectors);
  }

  @Override
  public PolicyTileListElement row(int num) {
    return new PolicyTileListElement(selector, "tbody tr", nthChild(num));
  }

  public SelenideElement ownerName() {
    return child(".nx-h3");
  }

  public SelenideElement emptyDescriptor() {
    return child(".nx-cell--meta-info");
  }

  public SelenideElement localEmptyDescriptor() {
    return child(".nx-list__item--empty");
  }

  public HeaderColumn threatLegendHeaderColumn() {
    return this.header(0);
  }

  public HeaderColumn nameHeaderColumn() {
    return this.header(1);
  }

  public HeaderColumn buildHeaderColumn() {
    return this.header(5);
  }

  public static class PolicyTileListElement
      extends BasicElement<PolicyTileListElement>
  {
    public static final Condition WARN_ICON = cssClass("fa-exclamation-triangle");

    public static final Condition WARN = cssClass("warn");

    public static final Condition FAIL_ICON = cssClass("fa-exclamation-circle");

    public static final Condition FAIL = cssClass("fail");

    public static final Condition CHEVRON = cssClass("fa-chevron-right");

    public PolicyTileListElement(String... selectors) {
      super(selectors);
    }

    public SelenideElement threatLegend() {
      return child(".nx-threat-number");
    }

    public SelenideElement name() {
      return column(2);
    }

    public SelenideElement proxy() {
      return column(3);
    }

    public SelenideElement develop() {
      return column(4);
    }

    public SelenideElement source() {
      return column(5);
    }

    public SelenideElement build() {
      return column(6);
    }

    public SelenideElement stageRelease() {
      return column(7);
    }

    public SelenideElement release() {
      return column(8);
    }

    public SelenideElement operate() {
      return column(9);
    }

    public SelenideElement column(int num) {
      return child("td", nthChild(num));
    }

    public SelenideElement chevronColumn(int num) {
      return child("td", nthChild(num), ".nx-icon");
    }

    public SelenideElement chevron() {
      return child(".fa-chevron-right");
    }
  }
}
