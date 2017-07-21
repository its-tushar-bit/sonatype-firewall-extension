/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class SuccessMetricsChartsPage
    extends BasicElement<SuccessMetricsChartsPage>
{
  public static final String URL = BaseUrl.uriBuilder().fragment("/labs/successMetrics").build().toString();

  private static final String ROOT_SELECTOR = "success-metrics";

  public SuccessMetricsChartsPage() {
    super(ROOT_SELECTOR);
  }

  public SelenideElement rootOrganizationActionItem() {
    return child("#root-org-success-metrics-action");
  }

  public ErrorBox errorBox() {
    return new ErrorBox(childSelector(".clm-alert"));
  }

  public SelenideElement noRootOrgWarning() {
    return child("#no-root-org-warning");
  }
}
