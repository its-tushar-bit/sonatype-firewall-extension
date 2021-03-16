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

public class DashboardApplications
{
  private static final String ROOT = "#dashboard-applications";

  public ApplicationsHeaders headers() {
    return new ApplicationsHeaders();
  }

  public ApplicationsResults results() {
    return new ApplicationsResults();
  }

  public ApplicationsResultsMask resultsMask() {
    return new ApplicationsResultsMask();
  }

  public static class ApplicationsHeaders
      extends BasicElement<ApplicationsHeaders>
  {
    private static final String HEADER_CLASS_NAME = ".nx-cell--header";

    ApplicationsHeaders() {
      super(ROOT, ".nx-table-row--header");
    }

    public NxSortingHeader applicationNameHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(1))));
    }

    public NxSortingHeader totalRiskHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(2))));
    }

    public NxSortingHeader criticalRiskHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(3))));
    }

    public NxSortingHeader severeRiskHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(4))));
    }

    public NxSortingHeader moderateRiskHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(5))));
    }

    public NxSortingHeader lowRiskHeader() {
      return new NxSortingHeader(childSelector(createSelector(HEADER_CLASS_NAME, nthChild(6))));
    }
  }

  public static class ApplicationsResults
      extends BasicElement<ApplicationsResults>
  {
    private static final String ROW_CLASS_NAME = ".iq-dashboard-application-row";

    ApplicationsResults() {
      super(ROOT, "tbody");
    }

    public ElementsCollection applications() {
      return children(ROW_CLASS_NAME);
    }

    public ApplicationElement application(int index) {
      String selectorQuery = String.format("tr[id^=\"app%d_\"]", index);
      return new ApplicationElement(childSelector(selectorQuery));
    }

    public ApplicationElement firstApplication() {
      return application(0);
    }

    public ApplicationElement lastApplication() {
      return application(applications().size() - 1);
    }

    public SelenideElement maxResultsMessage() {
      return child("#max-results-shown");
    }

    public SelenideElement noDataMessage() {
      return child("tr:last-child");
    }
  }

  public static class ApplicationElement
      extends BasicElement<ApplicationElement>
  {
    private static final String CELL_CLASS_NAME = ".nx-cell";

    private final ApplicationStageList applicationStageList;

    ApplicationElement(String selector) {
      super(selector);
      applicationStageList = new ApplicationStageList(this.selector + ".iq-dashboard-application-risk-row");
    }

    public ElementsCollection getRows() {
      return children();
    }

    public ElementsCollection getTotalsInRow(int index) {
      return children(nthChild(index + 1), ".iq-cell--heatmap");
    }

    public ElementsCollection getTotalsInStageRow(int index) {
      return applicationStageList.getTotalsInRow(index);
    }

    public ElementsCollection getStages() {
      return applicationStageList.getRows();
    }

    public SelenideElement getStageLink(int index) {
      return applicationStageList.getStageLinkByRow(index);
    }

    public SelenideElement name() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(1)));
    }

    public SelenideElement totalRisk() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(2)));
    }

    public SelenideElement criticalRisk() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(3)));
    }

    public SelenideElement severeRisk() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(4)));
    }

    public SelenideElement moderateRisk() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(5)));
    }

    public SelenideElement lowRisk() {
      return child(createSelector(CELL_CLASS_NAME, nthChild(6)));
    }

    private static class ApplicationStageList
        extends BasicElement<ApplicationStageList>
    {
      ApplicationStageList(String... selectors) {
        super(selectors);
      }

      public ElementsCollection getRows() {
        return children();
      }

      public SelenideElement getStageLinkByRow(int index) {
        return children().get(index).$(createSelector("a[target=_blank]"));
      }

      public ElementsCollection getTotalsInRow(int index) {
        return children().get(index).$$(createSelector(CELL_CLASS_NAME)).last(5);
      }
    }
  }

  public static class ApplicationsResultsMask
      extends BasicElement<ApplicationsResultsMask>
  {
    ApplicationsResultsMask() {
      super(ROOT, ".iq-dashboard-form-mask");
    }
  }
}
