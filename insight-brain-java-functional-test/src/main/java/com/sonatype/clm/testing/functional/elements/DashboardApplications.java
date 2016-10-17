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
      super(ROOT, ".dashboard-headers");
    }

    public SelenideElement totalRiskHeader() {
      return child(".total-risk a");
    }

    public SelenideElement lowRiskHeader() {
      return child(".low-risk a");
    }

    public SelenideElement moderateRiskHeader() {
      return child(".moderate-risk a");
    }

    public SelenideElement severeRiskHeader() {
      return child(".severe-risk a");
    }

    public SelenideElement criticalRiskHeader() {
      return child(".critical-risk a");
    }

    public SelenideElement applicationNameHeader() {
      return child(".application-name a");
    }
  }

  public static class ApplicationsResults
      extends BasicElement<ApplicationsResults>
  {
    ApplicationsResults() {
      super(ROOT, ".dashboard-results");
    }

    public ElementsCollection applications() {
      return children(".tile");
    }

    public ApplicationTile application(int index) {
      return new ApplicationTile(childSelector(".tile", nthChild(index + 1)));
    }

    public ApplicationTile firstApplication() {
      return new ApplicationTile(childSelector(".tile:first-child"));
    }

    public ApplicationTile lastApplication() {
      return new ApplicationTile(childSelector(".tile:last-child"));
    }

    public SelenideElement maxResultsMessage() {
      return child("#max-results-shown");
    }

    public SelenideElement noDataMessage() {
      return child("#no-data");
    }
  }

  public static class ApplicationTile
      extends BasicElement<ApplicationTile>
  {
    ApplicationTile(String selector) {
      super(selector);
    }

    public ElementsCollection getRows() {
      return children(".applications-row");
    }

    public ElementsCollection getStageLinks() {
      return children(".applications-row.stage-application-risks", "a[target=_blank]");
    }

    public SelenideElement getStageLink(int index) {
      return child(".stage-application-risks", nthChild(index + 2), "a[target=_blank]");
    }

    public ElementsCollection getTotalsInRow(int index) {
      return children(".applications-row", nthChild(index + 1), ".column.fixed");
    }
  }
}
