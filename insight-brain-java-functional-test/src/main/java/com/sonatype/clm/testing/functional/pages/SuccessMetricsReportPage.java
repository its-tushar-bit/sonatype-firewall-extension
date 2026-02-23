/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.ViolationTrendPlot;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class SuccessMetricsReportPage
    extends BasicElement<SuccessMetricsReportPage>
{
  private static final String ROOT_SELECTOR = "#success-metrics-report";

  public static final WebElementCondition NO_DATA_INFO_TEXT_MONTHLY = Condition.text(
      "There's not enough data to generate Success Metrics. Run some evaluations and check again next month.");

  public static final WebElementCondition NO_DATA_INFO_TEXT_LATEST = Condition.text(
      "There's not enough data to generate Success Metrics. Run some evaluations and check again.");

  public static final WebElementCondition CONFIRM_REMOVAL_HEADER_TEXT = Condition.text("Delete Report");

  public static final String ALL_CLASS = "iq-chart__dataset--overall";

  public static final String CRITICAL_CLASS = "iq-chart__dataset--critical";

  public static final String TOTAL_CLASS = "iq-chart__dataset--overall";

  public static final String SECURITY_CLASS = "iq-chart__dataset--security";

  public static final String LICENSE_CLASS = "iq-chart__dataset--license";

  public static final String QUALITY_CLASS = "iq-chart__dataset--quality";

  public static final String OTHER_CLASS = "iq-chart__dataset--other";

  public SuccessMetricsReportPage() {
    super(ROOT_SELECTOR);
  }

  public SuccessMetricsReportPage shouldBeFullyLoaded() {
    Header.title().should(exist);
    ViolationTrendTile.title().should(exist);
    ViolationsByCategoryTile.title().should(exist);
    ViolationAveragesTile.title().should(exist);
    MttrTile.chart().should(exist);
    ApplicationCountsTile.description().should(exist);
    ComponentCountsTile.averages().should(exist);

    // attempt to stabilize the applitools tests for this page by giving it a chance to adjust chart sizes after the
    // scrollbar appears (something that seems to happen asynchronously)
    Selenide.sleep(500);

    return this;
  }

  public NxBackButton backButton() {
    return new NxBackButton();
  }

  public ErrorBox errorBox() {
    return new ErrorBox(childSelector(".nx-alert.nx-alert--error"));
  }

  public SelenideElement noDataInfoPane() {
    return $("#no-data-warning");
  }

  public SelenideElement deleteBtn() {
    return child("#delete-report-button");
  }

  public static String url(String successMetricsId) {
    return BaseUrl.resolvePageUrl("/labs/successMetrics/{successMetricsId}", successMetricsId);
  }

  public static WebElementCondition confirmRemovalText(String successMetricsName) {
    return Condition.text("You are about to delete " + successMetricsName + ". This action cannot be undone.");
  }

  public static class Header
  {
    private static final String ROOT = "#success-metrics-header";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement title() {
      return $(SelectorUtils.createSelector(ROOT, ".nx-h1"));
    }

    public static SelenideElement description() {
      return $(SelectorUtils.createSelector(ROOT, ".nx-page-title__description"));
    }
  }

  public static class ViolationAveragesTile
  {
    private static final String ROOT = "#violation-averages-chart";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement title() {
      return $(SelectorUtils.createSelector(ROOT, ".nx-tile-header__title .nx-h2"));
    }

    public static SelenideElement averages() {
      return $(SelectorUtils.createSelector(ROOT, ".nx-tile-header__subtitle"));
    }

    public static SelenideElement averageCriticalPolicyViolations() {
      return $("#average-critical-policy-violations");
    }
  }

  public static class ApplicationCountsTile
  {
    private static final String ROOT = "#application-counts-chart";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement description() {
      return $(SelectorUtils.createSelector(ROOT, ".nx-tile-header__subtitle"));
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
    private static final String ROOT = "#mttr-chart";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement chart() {
      return $(SelectorUtils.createSelector(ROOT, "#mttr-chart-container"));
    }

    public static ElementsCollection mttrPoints() {
      return $$(SelectorUtils.createSelector(ROOT, "#mttr-chart-container", ".scatter-plot", "path"));
    }

    public static ElementsCollection mttrLines() {
      return $$(SelectorUtils.createSelector(ROOT, "#mttr-chart-container", ".line-plot", "path"));
    }

    public static ElementsCollection mttrXAxisLabels() {
      return $$(SelectorUtils.createSelector(ROOT, "#mttr-chart-container", ".x-axis",
      ".tick-label-container", "text"));
    }
  }

  public static class ComponentCountsTile
  {
    private static final String ROOT = "#component-counts-chart";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement averages() {
      return $(SelectorUtils.createSelector(ROOT, ".nx-tile-header__subtitle"));
    }

    public static ElementsCollection componentsInMostApplications() {
      return $$(SelectorUtils.createSelector(ROOT, "#component-in-most-applications", ".iq-chart__bar-label"));
    }

    public static ElementsCollection componentsWithMostViolations() {
      return $$(SelectorUtils.createSelector(ROOT, "#component-with-most-violations", ".iq-chart__bar-label"));
    }
  }

  public static class ViolationsByCategoryTile
  {
    private static final String ROOT = "#violations-by-category-chart";

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement title() {
      return $(SelectorUtils.createSelector(ROOT, ".nx-tile-header__title .nx-h2"));
    }

    public static SelenideElement description() {
      return $(SelectorUtils.createSelector(ROOT, ".nx-tile-header__subtitle"));
    }

    public static SelenideElement chart() {
      return $(SelectorUtils.createSelector(ROOT, "#bycategory-chart-container"));
    }

    public static ElementsCollection points() {
      return $$(SelectorUtils.createSelector(ROOT, "#bycategory-chart-container", ".scatter-plot", "path"));
    }

    public static ElementsCollection lines() {
      return $$(SelectorUtils.createSelector(ROOT, "#bycategory-chart-container", ".line-plot", "path"));
    }

    public static ElementsCollection xAxisLabels() {
      return $$(SelectorUtils.createSelector(ROOT, "#bycategory-chart-container",
      ".x-axis", ".tick-label-container", "text"));
    }
  }

  public static class ViolationTrendTile
  {
    private static final String ROOT = "#violation-trends-chart";

    public static final String HEIGHT_ATTR = "height";

    public static final String[] GUIDELINE_TOOLTIP_VALUES = {
      "Week of May 21st",
      "Week of May 28th",
      "Week of June 4th",
      "Week of June 11th",
      "Week of June 18th",
      "Week of June 25th",
      "Week of July 2nd",
      "Week of July 9th",
      "Week of July 16th",
      "Week of July 23rd",
      "Week of July 30th",
      "Week of August 6th"
    };

    public static final WebElementCondition TITLE_TEXT = Condition.text("12 Week Policy Violation Activity");

    public static final WebElementCondition DESCRIPTION_TEXT = Condition
        .text("Violations and remediation over the past 12 weeks.");

    public static final WebElementCondition TRENDS_DELTA_UP_CLASS =
        Condition.cssClass("iq-violation-trends__bar--delta-up");

    public static final WebElementCondition TRENDS_DELTA_DOWN_CLASS =
        Condition.cssClass("iq-violation-trends__bar--delta-down");

    public static final WebElementCondition TRENDS_DISCOVERED_CLASS =
        Condition.cssClass("iq-violation-trends__bar--discovered");

    public static final WebElementCondition TRENDS_FIXED_CLASS = Condition.cssClass("iq-violation-trends__bar--fixed");

    public static SelenideElement guidelineTooltip = $("#guidelineTooltip");

    public static SelenideElement deltaBarTooltip = $("#deltaBarTooltip");

    public static SelenideElement newBarTooltip = $("#newBarTooltip");

    public static SelenideElement waivedBarTooltip = $("#waivedBarTooltip");

    public static SelenideElement fixedBarTooltip = $("#fixedBarTooltip");

    public static SelenideElement root() {
      return $(ROOT);
    }

    public static SelenideElement title() {
      return $(SelectorUtils.createSelector(ROOT, ".nx-tile-header__title .nx-h2"));
    }

    public static SelenideElement description() {
      return $(SelectorUtils.createSelector(ROOT, ".nx-tile-header__subtitle"));
    }

    public static ViolationTrendPlot allViolationsPlot() {
      return new ViolationTrendPlot("#iq-violation-trends-all");
    }

    public static ViolationTrendPlot securityViolationsPlot() {
      return new ViolationTrendPlot("#iq-violation-trends-security");
    }

    public static ViolationTrendPlot licenseViolationsPlot() {
      return new ViolationTrendPlot("#iq-violation-trends-license");
    }

    public static ViolationTrendPlot qualityViolationsPlot() {
      return new ViolationTrendPlot("#iq-violation-trends-quality");
    }

    public static ViolationTrendPlot otherViolationsPlot() {
      return new ViolationTrendPlot("#iq-violation-trends-other");
    }
  }
}
