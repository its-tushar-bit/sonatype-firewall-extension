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

    public SelenideElement component(int index) {
      return child(".iq-components-results .iq-table-row", nthChild(index + 1));
    }

    public ElementsCollection componentRisks(int index) {
      return children(".iq-components-results .iq-table-row", nthChild(index + 1), ".iq-cell:nth-child(n+3):nth-child(-n+7)");
    }

    public SelenideElement firstComponent() {
      return child(".iq-components-results .iq-table-row:first-child");
    }

    public SelenideElement lastComponent() {
      return child(".iq-components-results .iq-table-row:last-of-type");
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

    public SelenideElement componentNameHeader() {
      return child(".iq-cell--component-name", "a");
  }

    public SelenideElement totalRiskHeader() {
      return child(".iq-cell--total-risk", "a");
    }

    public SelenideElement lowRiskHeader() {
      return child(".iq-cell--low-risk", "a");
    }

    public SelenideElement moderateRiskHeader() {
      return child(".iq-cell--moderate-risk", "a");
    }

    public SelenideElement severeRiskHeader() {
      return child(".iq-cell--severe-risk", "a");
    }

    public SelenideElement criticalRiskHeader() {
      return child(".iq-cell--critical-risk", "a");
    }
  }
}
