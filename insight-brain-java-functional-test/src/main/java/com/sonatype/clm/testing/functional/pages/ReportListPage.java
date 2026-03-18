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

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class ReportListPage
{
  public static final String ROOT = "#iq-report-container";

  public static final int RESULTS_PER_PAGE = 50;

  public static final String TABLE_BODY_ROW_SELECTOR = "#iq-violation-table #iq-violation-table-body .nx-table-row";

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
    return new ReportListRow(ROOT, TABLE_BODY_ROW_SELECTOR + ":nth-child(1)");
  }

  public static ReportListRow row(int number) {
    return new ReportListRow(SelectorUtils
        .createSelector(ROOT, "#iq-violation-table .iq-violation-table-row", SelectorUtils.nthChild(number)));
  }

  public static ElementsCollection rows() {
    return $$(SelectorUtils.createSelector(ROOT, "#iq-violation-table-body .iq-violation-table-row"));
  }

  public static ElementsCollection tableHeaders() {
    return $$(SelectorUtils.createSelector(ROOT, "thead > tr > th"));
  }

  public static class ReportListRow
      extends BasicElement<ReportListRow>
  {
    public ReportListRow(String... selectors) {
      super(selectors);
    }

    public SelenideElement applicationName() {
      return child(".iq-violation-table-report-name");
    }

    public Tooltip applicationNameTooltip() {
      return Tooltip.get();
    }

    public SelenideElement showContactName() {
      return child(".iq-violation-show-contact-name");
    }

    public SelenideElement contactName() {
      return child(".iq-violation-contact-name");
    }

    public Tooltip contactNameTooltip() {
      return Tooltip.get();
    }

    public SelenideElement organizationName() {
      return child(".iq-violation-table-organization-name");
    }

    public Tooltip organizationNameTooltip() {
      return Tooltip.get();
    }

    public SelenideElement sourceStageCell() {
      return child(".nx-cell:nth-child(3)");
    }

    public SelenideElement sourceReportLink() {
      return child(".nx-cell:nth-child(3) #iq-report-link");
    }

    public SelenideElement buildReportLink() {
      return child(".nx-cell:nth-child(4) #iq-report-link");
    }

    public SelenideElement stageReleaseReportLink() {
      return child(".nx-cell:nth-child(5) #iq-report-link");
    }

    public SelenideElement releaseReportLink() {
      return child(".nx-cell:nth-child(6) #iq-report-link");
    }

    public IQThreatIndicators buildReportThreatIndicators() {
      return new IQThreatIndicators(
          TABLE_BODY_ROW_SELECTOR + " .nx-cell:nth-child(4) .nx-small-threat-counter-container");
    }

    public IQThreatIndicators stageReleaseReportThreatIndicators() {
      return new IQThreatIndicators(
          TABLE_BODY_ROW_SELECTOR + " .nx-cell:nth-child(5) .nx-small-threat-counter-container");
    }

    public IQThreatIndicators releaseReportThreatIndicators() {
      return new IQThreatIndicators(
          TABLE_BODY_ROW_SELECTOR + " .nx-cell:nth-child(6) .nx-small-threat-counter-container");
    }
  }

  public static SelenideElement filter() {
    return $("#iq-report-list-filter");
  }

  public static SelenideElement load() {
    return $("#iq-report-list-load-button");
  }
}
