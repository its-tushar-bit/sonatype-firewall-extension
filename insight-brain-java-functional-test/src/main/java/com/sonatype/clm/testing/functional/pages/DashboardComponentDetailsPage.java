/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.CustomRootBasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class DashboardComponentDetailsPage
{
  public static String url(String hash) {
    return BaseUrl.resolvePageUrl("/dashboard/component/{hash}", hash);
  }

  public SelenideElement header() {
    return $("#iq-component-name");
  }

  public SelenideElement totalRisk() {
    return $("#iq-component-total-risk");
  }

  public ApplicationCard getApplicationRow(int index) {
    return new ApplicationCard($$(".iq-component-risk-item-content").get(index));
  }

  public static class ApplicationCard
      extends CustomRootBasicElement<ApplicationCard>
  {
    public ApplicationCard(SelenideElement element) {
      super(element);
    }

    public ApplicationCardTotals totals() {
      return new ApplicationCardTotals(child(".iq-component-risk-data"));
    }

    public ApplicationCardTable table() {
      return new ApplicationCardTable(child(".iq-component-risk-item-table"));
    }

    public SelenideElement accordionRow() {
      return child(".nx-accordion__header");
    }

    public SelenideElement name() {
      return child(".iq-component-risk-application-name");
    }
  }

  public static class ApplicationCardTotals
      extends CustomRootBasicElement<ApplicationCardTotals>
  {
    private static final String BASE_SELECTOR = ".iq-component-risk-data-element";

    public ApplicationCardTotals(SelenideElement element) {
      super(element);
    }

    public SelenideElement pie() {
      return child(BASE_SELECTOR, nthChild(1)).$("svg");
    }

    public SelenideElement shareOfRisk() {
      return child(BASE_SELECTOR, nthChild(1)).$("dd");
    }

    public SelenideElement risk() {
      return child(BASE_SELECTOR, nthChild(2)).$("dd");
    }

    public Cell source() {
      return new Cell(child(BASE_SELECTOR, nthChild(3)).$("dd"));
    }

    public Cell build() {
      return new Cell(child(BASE_SELECTOR, nthChild(4)).$("dd"));
    }

    public Cell stage() {
      return new Cell(child(BASE_SELECTOR, nthChild(5)).$("dd"));
    }

    public Cell release() {
      return new Cell(child(BASE_SELECTOR, nthChild(6)).$("dd"));
    }

    public Cell operate() {
      return new Cell(child(BASE_SELECTOR, nthChild(7)).$("dd"));
    }
  }

  public static class ApplicationCardTable
      extends CustomRootBasicElement<ApplicationCardTable>
  {
    private static final String BASE_SELECTOR = ".iq-component-risk-cell";

    public ApplicationCardTable(SelenideElement element) {
      super(element);
    }

    public SelenideElement threatIndicator() {
      return child(".nx-threat-indicator");
    }

    public SelenideElement threat() {
      return child(".nx-threat-number");
    }

    public SelenideElement pie() {
      return child(".iq-component-risk-table-donut-chart");
    }

    public SelenideElement policyName() {
      return child(BASE_SELECTOR, nthChild(2));
    }

    public SelenideElement shareOfRisk() {
      return child(BASE_SELECTOR + ".percentage");
    }

    public SelenideElement risk() {
      return child(BASE_SELECTOR, nthChild(4));
    }

    public Cell source() {
      return new Cell(child(BASE_SELECTOR + ".stage", nthChild(5)));
    }

    public Cell build() {
      return new Cell(child(BASE_SELECTOR + ".stage", nthChild(6)));
    }

    public Cell stage() {
      return new Cell(child(BASE_SELECTOR + ".stage", nthChild(7)));
    }

    public Cell release() {
      return new Cell(child(BASE_SELECTOR + ".stage", nthChild(8)));
    }

    public Cell operate() {
      return new Cell(child(BASE_SELECTOR + ".stage", nthChild(9)));
    }
  }

  public static class Cell
      extends CustomRootBasicElement<Cell>
  {
    public Cell(SelenideElement element) {
      super(element);
    }

    public SelenideElement anchor() {
      return child("a");
    }

    public SelenideElement anchorText() {
      return child("a").$("span");
    }
  }
}
