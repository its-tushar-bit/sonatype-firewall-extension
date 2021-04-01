/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQThreatIndicators;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class ReportListPage
{
  public static final String ROOT = ".iq-report-list-container";

  public static final int RESULTS_PER_PAGE = 50;

  public static String url() {
    return BaseUrl.resolvePageUrl("/reports/violations");
  }

  public static SelenideElement listContainer() {
    return $(ROOT);
  }

  public static void scrollToTop() {
    ElementsCollection rows = ReportListPage.rows();
    String firstText = null;
    while (!Objects.equals(rows.first().getText(), firstText)) {
      firstText = rows.first().getText();
      ScrollUtil.awaitEndOfScrolling(rows.first().scrollIntoView(false));
    }
  }

  public static void scrollToBottom() {
    ElementsCollection rows = ReportListPage.rows();
    String lastText = null;
    while (!Objects.equals(rows.last().getText(), lastText)) {
      lastText = rows.last().getText();
      ScrollUtil.awaitEndOfScrolling(rows.last().scrollIntoView(true));
    }
  }

  public static void consumeAllRows(Consumer<ReportListRow> consumer) {
    scrollToTop();
    ElementsCollection rows = ReportListPage.rows();
    Set<String> names = new LinkedHashSet<>();
    String lastText = null;
    while (!Objects.equals(rows.last().getText(), lastText)) {
      lastText = rows.last().getText();
      for (int row = 1; row <= rows.size(); row++) {
        ReportListRow reportListRow = ReportListPage.row(row);
        if (names.add(reportListRow.applicationName().getText())) {
          consumer.accept(reportListRow);
        }
      }
      ScrollUtil.awaitEndOfScrolling(rows.last().scrollIntoView(true));
    }
  }

  public static ReportListRow firstRow() {
    return new ReportListRow(ROOT, ".iq-report-list-results .iq-table-row");
  }

  public static ReportListRow row(int number) {
    return new ReportListRow(SelectorUtils
        .createSelector(ROOT, ".iq-report-list-results .iq-table-row", SelectorUtils.nthChild(number + 1)));
  }

  public static ElementsCollection rows() {
    return $$(SelectorUtils.createSelector(ROOT, ".iq-report-list-results .iq-table-row"));
  }

  public static class ReportListRow
      extends BasicElement<ReportListRow>
  {
    public ReportListRow(String... selectors) {
      super(selectors);
    }

    public SelenideElement applicationName() {
      return child(".tm-report-list-application");
    }

    public Tooltip applicationNameTooltip() {
      return new Tooltip(".report-application-name-tooltip");
    }

    public SelenideElement contactName() {
      return child(".iq-cell--contact");
    }

    public Tooltip contactNameTooltip() {
      return new Tooltip(".report-contact-name-tooltip");
    }

    public SelenideElement organizationName() {
      return child(".tm-report-list-organization");
    }

    public Tooltip organizationNameTooltip() {
      return new Tooltip(".report-organization-name-tooltip");
    }

    public SelenideElement sourceStageCell() {
      return child(".iq-cell:nth-child(4)");
    }

    public SelenideElement sourceReportLink() {
      return child(".iq-cell:nth-child(4) .iq-report-list__report-links a");
    }

    public SelenideElement buildReportLink() {
      return child(".iq-cell:nth-child(5) .iq-report-list__report-links a");
    }

    public SelenideElement stageReleaseReportLink() {
      return child(".iq-cell:nth-child(6) .iq-report-list__report-links a");
    }

    public SelenideElement releaseReportLink() {
      return child(".iq-cell:nth-child(7) .iq-report-list__report-links a");
    }

    public IQThreatIndicators buildReportThreatIndicators() {
      return new IQThreatIndicators(".iq-cell:nth-child(5) .iq-threat-indicators");
    }

    public IQThreatIndicators stageReleaseReportThreatIndicators() {
      return new IQThreatIndicators(".iq-cell:nth-child(6) .iq-threat-indicators");
    }

    public IQThreatIndicators releaseReportThreatIndicators() {
      return new IQThreatIndicators(".iq-cell:nth-child(7) .iq-threat-indicators");
    }
  }

  public static SelenideElement filter() {
    return $("#iq-report-list-filter");
  }

  public static SelenideElement search() {
    return $("#iq-report-list-search-button");
  }

  public static SelenideElement load() {
    return $("#iq-report-list-load-button");
  }

  public static SelenideElement applicationNameHeader() {
    return $("#report-list-header-app");
  }

  public static SelenideElement organizationNameHeader() {
    return $("#report-list-header-org");
  }

  public static void sortAscending(SelenideElement header) {
    if (!header.$(".fa-caret-up").has(Condition.cssClass("up"))) {
      header.click();
      header.$(".fa-caret-up").shouldHave(Condition.cssClass("up"));
    }
  }

  public static void sortDescending(SelenideElement header) {
    if (!header.$(".fa-caret-down").has(Condition.cssClass("down"))) {
      if (!header.$(".fa-caret-up").has(Condition.cssClass("up"))) {
        header.click();
      }
      header.click();
      header.$(".fa-caret-down").shouldHave(Condition.cssClass("down"));
    }
  }
}
