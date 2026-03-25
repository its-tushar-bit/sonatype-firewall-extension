/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.PolicyTileList.PolicyTileListElement;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.cssClass;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class PolicyTileList
    extends GreedyTable<PolicyTileListElement>
{
  public static final WebElementCondition CELL_DISABLED = cssClass("policy-tile__cell--disabled");

  public PolicyTileList(String... selectors) {
    super(selectors);
  }

  @Override
  public PolicyTileListElement row(int num) {
    return new PolicyTileListElement(selector, "tr:nth-of-type(" + (num + 1) + ")");
  }

  public SelenideElement ownerName() {
    return child(".iq-collapsible-row__header-title");
  }

  public SelenideElement emptyDescriptor() {
    return child(".iq-collapsible-row__empty-message");
  }

  public HeaderColumn threatLegendHeaderColumn() {
    return this.header(0);
  }

  public HeaderColumn nameHeaderColumn() {
    return this.header(1);
  }

  public HeaderColumn proxyHeaderColumn() {
    return this.header(2);
  }

  public HeaderColumn developHeaderColumn() {
    return this.header(3);
  }

  public HeaderColumn sourceHeaderColumn() {
    return this.header(4);
  }

  public HeaderColumn buildHeaderColumn() {
    return this.header(5);
  }

  public HeaderColumn stageHeaderColumn() {
    return this.header(6);
  }

  public HeaderColumn releaseHeaderColumn() {
    return this.header(7);
  }

  public HeaderColumn operateHeaderColumn() {
    return this.header(8);
  }

  public static class PolicyTileListElement
      extends BasicElement<PolicyTileListElement>
  {
    public static final WebElementCondition WARN_ICON = cssClass("fa-triangle-exclamation");

    public static final WebElementCondition WARN = cssClass("warn");

    public static final WebElementCondition FAIL_ICON = cssClass("fa-circle-exclamation");

    public static final WebElementCondition FAIL = cssClass("fail");

    public static final WebElementCondition CHEVRON = cssClass("fa-chevron-right");

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
