/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ReportListPage
{
  public static final String ROOT = ".iq-report-list-container";

  public static String URL = BaseUrl.resolvePageUrl("/reports/violations");

  public static SelenideElement listContainer() {
    return $(ROOT);
  }

  public static ReportListRow firstRow() {
    return new ReportListRow(ROOT, ".iq-report-list-results .iq-table-row");
  }

  public static class ReportListRow
      extends BasicElement<ReportListRow>
  {
    public ReportListRow(String... selectors) {
      super(selectors);
    }

    public SelenideElement buildReportLink() {
      return child(".iq-cell:nth-child(4) .iq-report-list__report-links a");
    }

    public SelenideElement stageReleaseReportLink() {
      return child(".iq-cell:nth-child(5) .iq-report-list__report-links a");
    }

    public SelenideElement releaseReportLink() {
      return child(".iq-cell:nth-child(6) .iq-report-list__report-links a");
    }
  }
}
