/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
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
      super(ROOT, ".dashboard-results");
    }

    public ElementsCollection components() {
      return children(".tile");
    }

    public SelenideElement component(int index) {
      return child(".tile", nthChild(index + 1));
    }

    public SelenideElement firstComponent() {
      return child(".tile:first-child");
    }

    public SelenideElement lastComponent() {
      return child(".tile:last-child");
    }

    public SelenideElement maxResultsMessage() {
      return child("#max-results-shown");
    }

    public SelenideElement noDataMessage() {
      return child("#no-data");
    }
  }

  public static class ComponentsHeaders
      extends BasicElement<ComponentsHeaders>
  {
    ComponentsHeaders() {
      super(ROOT, ".dashboard-headers");
    }

    public SelenideElement totalRiskHeader() {
      return child(".total-risk", "a");
    }

    public SelenideElement lowRiskHeader() {
      return child(".low-risk", "a");
    }

    public SelenideElement moderateRiskHeader() {
      return child(".moderate-risk", "a");
    }

    public SelenideElement severeRiskHeader() {
      return child(".severe-risk", "a");
    }

    public SelenideElement criticalRiskHeader() {
      return child(".critical-risk", "a");
    }
  }
}
