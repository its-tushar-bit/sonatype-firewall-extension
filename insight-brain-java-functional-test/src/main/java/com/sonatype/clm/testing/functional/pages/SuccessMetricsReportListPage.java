/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class SuccessMetricsReportListPage
    extends BasicElement<SuccessMetricsReportListPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/labs/successMetrics");
  }

  private static final String ROOT_SELECTOR = "#success-metrics-report-list";

  public static final WebElementCondition EMPTY_TEXT = Condition.text("No reports have been created.");

  public SuccessMetricsReportListPage() {
    super(ROOT_SELECTOR);
  }

  public ElementsCollection reports() {
    return children(".nx-list__item.nx-list__item--link");
  }

  public SuccessMetricsListItem report(int num) {
    return new SuccessMetricsListItem(childSelector(".nx-list__item.nx-list__item--link", nthChild(num + 1)));
  }

  public AlertError errorBox() {
    return new AlertError(childSelector(".nx-alert--error"));
  }

  public SelenideElement addSuccessMetricsBtn() {
    return child("#add-success-metrics-report-btn");
  }

  public SelenideElement emptyDescriptor() {
    return child("li.nx-list__item--empty");
  }

  public SelenideElement subheaderDashboardLink() {
    return child(".nx-page-title__description a");
  }

  public class SuccessMetricsListItem
      extends BasicElement<SuccessMetricsListItem>
  {
    public SuccessMetricsListItem(String... selectors) {
      super(selectors);
    }

    public SelenideElement chevron() {
      return child(".fa-angle-right.nx-chevron");
    }

    public SelenideElement link() {
      return child("a");
    }
  }

  public class AlertError
      extends BasicElement<AlertError>
  {
    public AlertError(String... selector) {
      super(selector);
    }

    public SelenideElement retryButton() {
      return child("button");
    }
  }
}
