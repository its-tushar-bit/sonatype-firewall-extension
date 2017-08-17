/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.IqBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class RootOrganizationSuccessMetricsPage
    extends BasicElement<RootOrganizationSuccessMetricsPage>
{
  public static final String URL = BaseUrl.uriBuilder().fragment("/labs/successMetrics/root-org").build().toString();

  private static final String ROOT_SELECTOR = "root-organization";

  public static final Condition NO_DATA_INFO_TEXT = Condition.text(
      "There's not enough data to generate Success Metrics. Run some evaluations and check back next month!");

  public RootOrganizationSuccessMetricsPage() {
    super(ROOT_SELECTOR);
  }

  public IqBackButton backButton() {
    return new IqBackButton(ROOT_SELECTOR);
  }

  public ErrorBox errorBox() {
    return new ErrorBox(childSelector(".iq-alert.iq-alert--error"));
  }

  public SelenideElement noRootOrgError() {
    return child("#no-root-org-warning");
  }

  public SelenideElement noDataInfoPane() {
    return $("#no-data-warning");
  }

  public static class SummaryStatementTile
  {
    private static final String ROOT = "summary-statement-tile";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement activeApplicationsCount() {
      return $("#active-applications-count");
    }

    public static SelenideElement averageEvaluations() {
      return $("#average-evaluations");
    }

    public static SelenideElement averagePolicyViolations() {
      return $("#average-policy-violations");
    }

    public static SelenideElement averageCriticalPolicyViolations() {
      return $("#average-critical-policy-violations");
    }

    public static SelenideElement months() {
      return $(SelectorUtils.createSelector(ROOT, "#success-metrics-summary-months"));
    }
  }

  public static class ViolationAveragesTile
  {
    private static final String ROOT = "violation-averages-chart";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement averageEvaluations() {
      return $("#average-evaluations");
    }

    public static SelenideElement averagePolicyViolations() {
      return $("#average-policy-violations");
    }

    public static SelenideElement averageCriticalPolicyViolations() {
      return $("#average-critical-policy-violations");
    }
  }

  public static class ApplicationCountsTile
  {
    private static final String ROOT = "application-counts-chart";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement activeApplicationsCount() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-chart__averages", ".iq-chart__display-number:nth-child(1)"));
    }

    public static SelenideElement totalViolatingApplicationsCount() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-chart__averages", ".iq-chart__display-number:nth-child(2)"));
    }

    public static SelenideElement securityViolatingApplicationsCount() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-chart__bar-label:nth-child(1)", "span:first-child",
          ".iq-chart__highlight"));
    }

    public static SelenideElement licenseViolatingApplicationsCount() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-chart__bar-label:nth-child(2)", "span:first-child",
          ".iq-chart__highlight"));
    }

    public static SelenideElement qualityViolatingApplicationsCount() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-chart__bar-label:nth-child(3)", "span:first-child",
          ".iq-chart__highlight"));
    }

    public static SelenideElement otherViolatingApplicationsCount() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-chart__bar-label:nth-child(4)", "span:first-child",
          ".iq-chart__highlight"));
    }

    public static SelenideElement totalCriticalViolatingApplicationsCount() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-chart__averages", ".iq-chart__display-number:nth-child(3)"));
    }

    public static SelenideElement securityCriticalViolatingApplicationsCount() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-chart__bar-label:nth-child(1)", "span:last-child",
          ".iq-chart__highlight"));
    }

    public static SelenideElement licenseCriticalViolatingApplicationsCount() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-chart__bar-label:nth-child(2)", "span:last-child",
          ".iq-chart__highlight"));
    }

    public static SelenideElement qualityCriticalViolatingApplicationsCount() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-chart__bar-label:nth-child(3)", "span:last-child",
          ".iq-chart__highlight"));
    }

    public static SelenideElement otherCriticalViolatingApplicationsCount() {
      return $(SelectorUtils.createSelector(ROOT, ".iq-chart__bar-label:nth-child(4)", "span:last-child",
          ".iq-chart__highlight"));
    }

  }

  public static class MttrTile
  {
    private static final String ROOT = "mttr-chart";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement chart() {
      return $(SelectorUtils.createSelector(ROOT, "iq-render-plottable"));
    }
    
    public static ElementsCollection mttrPoints() {
      return $$(SelectorUtils.createSelector(ROOT, "iq-render-plottable", ".scatter-plot", "path"));
    }

    public static ElementsCollection mttrLines() {
      return $$(SelectorUtils.createSelector(ROOT, "iq-render-plottable", ".line-plot", "path"));
    }

    public static ElementsCollection mttrXAxisLabels() {
      return $$(SelectorUtils.createSelector(ROOT, "iq-render-plottable", ".x-axis", ".tick-label-container", "text"));
    }
  }

  public static class ComponentCountsTile
  {
    private static final String ROOT = "component-counts-chart";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static ElementsCollection componentsInMostApplications() {
      return $$(SelectorUtils.createSelector(ROOT, "#component-in-most-applications", ".iq-chart__bar-label"));
    }

    public static ElementsCollection componentsWithMostViolations() {
      return $$(SelectorUtils.createSelector(ROOT, "#component-with-most-violations", ".iq-chart__bar-label"));
    }
  }
}
