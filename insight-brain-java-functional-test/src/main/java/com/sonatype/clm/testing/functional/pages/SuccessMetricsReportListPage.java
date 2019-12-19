/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ActionList;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

public class SuccessMetricsReportListPage
    extends BasicElement<SuccessMetricsReportListPage>
{
  public static final String URL = BaseUrl.resolvePageUrl("/labs/successMetrics");

  private static final String ROOT_SELECTOR = "success-metrics-report-list";

  public static final Condition EMPTY_TEXT = Condition.text("No reports have been created.");

  public SuccessMetricsReportListPage() {
    super(ROOT_SELECTOR);
  }

  public ActionList successMetricsChartActionItems() {
    return new ActionList(ROOT_SELECTOR, ".iq-list");
  }

  public ErrorBox errorBox() {
    return new ErrorBox(childSelector(".iq-alert"));
  }

  public SelenideElement addSuccessMetricsBtn() {
    return child("#add-success-metrics-report-btn");
  }

  public SelenideElement emptyDescriptor() {
    return child("li.iq-list__item--empty");
  }

  public SelenideElement subheaderDashboardLink() {
    return child(".nx-page-title__description a");
  }
}
