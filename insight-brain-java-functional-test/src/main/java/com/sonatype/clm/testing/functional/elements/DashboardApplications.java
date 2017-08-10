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

  public static class ApplicationsHeaders
      extends BasicElement<ApplicationsHeaders>
  {
    ApplicationsHeaders() {
      super(ROOT, ".iq-dashboard-headers");
    }

    public SelenideElement totalRiskHeader() {
      return child(".iq-cell--total-risk a");
    }

    public SelenideElement lowRiskHeader() {
      return child(".iq-cell--low-risk a");
    }

    public SelenideElement moderateRiskHeader() {
      return child(".iq-cell--moderate-risk a");
    }

    public SelenideElement severeRiskHeader() {
      return child(".iq-cell--severe-risk a");
    }

    public SelenideElement criticalRiskHeader() {
      return child(".iq-cell--critical-risk a");
    }

    public SelenideElement applicationNameHeader() {
      return child(".iq-cell--application-name a");
    }
  }

  public static class ApplicationsResults
      extends BasicElement<ApplicationsResults>
  {
    ApplicationsResults() {
      super(ROOT, ".iq-tile--dashboard-table-container");
    }

    public ElementsCollection applications() {
      return children(".total-application-risks");
    }

    public ApplicationElement application(int index) {
      return new ApplicationElement(childSelector("tr[id^=\"app" + index + "_\"]"));
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
      return child("#dashboard-common-results-no-data");
    }

    public SelenideElement mask() {
      return child(".form-mask");
    }
  }

  public static class ApplicationElement
      extends BasicElement<ApplicationElement>
  {
    private final ApplicationStageList applicationStageList;

    ApplicationElement(String selector) {
      super(selector);               
      applicationStageList = new ApplicationStageList(this.selector + ".stage-application-risks");
    }

    public ElementsCollection getRows() {
      return children();
    }

    public ElementsCollection getTotalsInRow(int index) {
      return children(nthChild(index + 1), ".iq-cell--heatmap");
    }

    public ElementsCollection getStages() {
      return applicationStageList.getRows();
    }

    public SelenideElement getStageLink(int index) {
      return applicationStageList.getStageLinkByRow(index);
    }

    public SelenideElement name() {
      return child(".iq-cell--application-name");
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
        return children().get(index).$(createSelector(nthChild(index + 2), "a[target=_blank]"));
      }
    }
  }
}
