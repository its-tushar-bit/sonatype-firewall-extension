/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class DashboardComponents
{
  private static final String ROOT = "#dashboard-components";

  public ComponentsHeaders headers() {
    return new ComponentsHeaders();
  }

  public ComponentsResults results() {
    return new ComponentsResults();
  }

  public static class ComponentsResults
      extends BasicElement<ComponentsResults>
  {
    ComponentsResults() {
      super(ROOT, ".iq-tile--dashboard-table-container");
    }

    public ElementsCollection components() {
      return children(".iq-components-results .iq-table-row");
    }

    public ComponentElement component(int index) {
      return new ComponentElement(childSelector(".iq-components-results .iq-table-row", nthChild(index + 1)));
    }

    public ElementsCollection componentRisks(int index) {
      return children(".iq-components-results .iq-table-row", nthChild(index + 1),
          ".iq-cell:nth-child(n+3):nth-child(-n+7)");
    }

    public ComponentElement firstComponent() {
      return new ComponentElement(childSelector(".iq-components-results .iq-table-row:first-child"));
    }

    public ComponentElement lastComponent() {
      return new ComponentElement(childSelector(".iq-components-results .iq-table-row:last-of-type"));
    }

    public SelenideElement maxResultsMessage() {
      return child("#max-results-shown");
    }

    public SelenideElement noDataMessage() {
      return child("#dashboard-common-results-no-data");
    }

    public SelenideElement mask() {
      return child(".form-mask");
    }
  }

  public static class ComponentsHeaders
      extends BasicElement<ComponentsHeaders>
  {
    ComponentsHeaders() {
      super(ROOT, ".iq-dashboard-headers");
    }

    public IqSortingHeader componentNameHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--component-name", "a"));
    }

    public IqSortingHeader totalRiskHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--total-risk", "a"));
    }

    public IqSortingHeader lowRiskHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--low-risk", "a"));
    }

    public IqSortingHeader moderateRiskHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--moderate-risk", "a"));
    }

    public IqSortingHeader severeRiskHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--severe-risk", "a"));
    }

    public IqSortingHeader criticalRiskHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--critical-risk", "a"));
    }

    public IqSortingHeader affectedAppsHeader() {
      return new IqSortingHeader(childSelector(".iq-cell--affected-apps", "a"));
    }
  }

  public static class ComponentElement
      extends BasicElement<ComponentElement>
  {
    ComponentElement(String selector) {
      super(selector);
    }

    public SelenideElement name() {
      return child(".iq-cell--component-name");
    }

    public SelenideElement totalRisk() {
      return child(".iq-cell--total-risk");
    }

    public SelenideElement criticalRisk() {
      return child(".iq-cell--critical-risk");
    }

    public SelenideElement severeRisk() {
      return child(".iq-cell--severe-risk");
    }

    public SelenideElement moderateRisk() {
      return child(".iq-cell--moderate-risk");
    }

    public SelenideElement lowRisk() {
      return child(".iq-cell--low-risk");
    }

    public SelenideElement affectedApps() {
      return child(".iq-cell--affected-apps");
    }
  }
}
