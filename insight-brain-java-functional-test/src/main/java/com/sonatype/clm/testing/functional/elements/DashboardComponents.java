/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
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

  public ComponentsResultsMask resultsMask() {
    return new ComponentsResultsMask();
  }

  public static class ComponentsResults
      extends BasicElement<ComponentsResults>
  {
    private static final String ROW_CLASS_NAME = ".iq-dashboard-component-row";

    ComponentsResults() {
      super(ROOT, "tbody");
    }

    public ElementsCollection components() {
      return children(ROW_CLASS_NAME);
    }

    public ComponentElement component(int index) {
      return new ComponentElement(childSelector(ROW_CLASS_NAME, nthChild(index + 1)));
    }

    public ElementsCollection componentRisks(int index) {
      return children(ROW_CLASS_NAME, nthChild(index + 1),
          ".nx-cell:nth-child(n+3):nth-child(-n+7)");
    }

    public ComponentElement firstComponent() {
      return new ComponentElement(childSelector(ROW_CLASS_NAME + ":first-child"));
    }

    public ComponentElement lastComponent() {
      String lastRowSelector = createSelector(ROW_CLASS_NAME, nthChild(components().size()));
      return new ComponentElement(childSelector(lastRowSelector));
    }

    public SelenideElement maxResultsMessage() {
      return child("#max-results-shown");
    }

    public SelenideElement noDataMessage() {
      return child("tr:last-child");
    }
  }

  public static class ComponentsResultsMask
      extends BasicElement<ComponentsResultsMask>
  {
    ComponentsResultsMask() {
      super(ROOT, ".iq-dashboard-form-mask");
    }
  }

  public static class ComponentsHeaders
      extends BasicElement<ComponentsHeaders>
  {
    private static final String HEADER_CLASS_NAME = ".nx-cell--header";

    ComponentsHeaders() {
      super(ROOT, ".nx-table-row--header");
    }

    public NxSortingHeader componentNameHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(1))));
    }

    public NxSortingHeader affectedAppsHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(2))));
    }

    public NxSortingHeader totalRiskHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(3))));
    }

    public NxSortingHeader criticalRiskHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(4))));
    }

    public NxSortingHeader severeRiskHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(5))));
    }

    public NxSortingHeader moderateRiskHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(6))));
    }

    public NxSortingHeader lowRiskHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(7))));
    }
  }

  public static class ComponentElement
      extends BasicElement<ComponentElement>
  {
    private static final String CELL_CLASS_NAME = ".nx-cell";

    ComponentElement(String selector) {
      super(selector);
    }

    public SelenideElement name() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(1)));
    }

    public SelenideElement affectedApps() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(2)));
    }

    public SelenideElement totalRisk() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(3)));
    }

    public SelenideElement criticalRisk() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(4)));
    }

    public SelenideElement severeRisk() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(5)));
    }

    public SelenideElement moderateRisk() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(6)));
    }

    public SelenideElement lowRisk() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(7)));
    }
  }
}
