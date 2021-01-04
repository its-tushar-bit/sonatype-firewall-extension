/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class DashboardComponentDetailsPage
{
  public static String url(String hash) {
    return BaseUrl.resolvePageUrl("/dashboard/component/{hash}", hash);
  }

  private static final String ROOT = ".component-container";

  public SelenideElement header() {
    return $("#component-name");
  }

  public SelenideElement totalRisk() {
    return $("#total-risk");
  }

  public ApplicationRow getApplicationRow(int index) {
    return new ApplicationRow(index);
  }

  public SelenideElement breadCrumb() {
    return $(createSelector(ROOT, " [breadcrumb]"));
  }

  public SelenideElement breadCrumbLink() {
    return $(createSelector(ROOT, " [breadcrumb] a"));
  }

  public static class ApplicationRow
      extends ComponentDetailsRow
  {
    private final String accordionSelector;

    public static String appIconImageSource(String applicationId) {
      return BaseUrl.resolveRestUrl("/application/icon/{applicationId}", applicationId);
    }

    public ApplicationRow(int index) {
      super("#application-row-" + index, 4);
      accordionSelector = "#application-" + index;
    }

    public SelenideElement twisty() {
      return child(".twisty");
    }

    public SelenideElement name() {
      return child("td", nthChild(2));
    }

    public SelenideElement appIcon() {
      return child("td", nthChild(2), "img.image-thumbnail");
    }

    public Accordion accordion() {
      return new Accordion(accordionSelector);
    }
  }

  public static class Accordion
      extends BasicElement<Accordion>
  {
    public Accordion(String selector) {
      super(selector);
    }

    public AccordionRow entry(int index) {
      return new AccordionRow(childSelector("tr", nthChild(index)));
    }

    public ElementsCollection entries() {
      return children("tr");
    }
  }

  public static class AccordionRow
      extends ComponentDetailsRow
  {
    public AccordionRow(String selector) {
      super(selector, 5);
    }

    public SelenideElement threatBar() {
      return child("td.clm-bar");
    }

    public SelenideElement threat() {
      return child("td.threat-column");
    }

    public SelenideElement policyName() {
      return child("td", nthChild(3));
    }
  }

  public static class ComponentDetailsRow
      extends BasicElement<ComponentDetailsRow>
  {
    private final int stageColumnOffset;

    public ComponentDetailsRow(String selector, int stageColumnOffset) {
      super(selector);
      this.stageColumnOffset = stageColumnOffset;
    }

    public SelenideElement pie() {
      return child("svg");
    }

    public SelenideElement shareOfRisk() {
      return child("td.share-risk-column");
    }

    public SelenideElement risk() {
      return child("td.risk-column");
    }

    public Cell build() {
      return new Cell(childSelector("td.stage-column", nthChild(stageColumnOffset + 1)));
    }

    public Cell stage() {
      return new Cell(childSelector("td.stage-column", nthChild(stageColumnOffset + 2)));
    }

    public Cell release() {
      return new Cell(childSelector("td.stage-column", nthChild(stageColumnOffset + 3)));
    }

    public Cell operate() {
      return new Cell(childSelector("td.stage-column", nthChild(stageColumnOffset + 4)));
    }
  }

  public static class Cell
      extends BasicElement<Cell>
  {
    public Cell(String selector) {
      super(selector);
    }

    public SelenideElement anchor() {
      return child("a");
    }
  }
}
