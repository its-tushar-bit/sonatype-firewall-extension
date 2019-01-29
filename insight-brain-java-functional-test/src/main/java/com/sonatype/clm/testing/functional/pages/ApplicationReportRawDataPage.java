/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.IqBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.SelenideElement;

public class ApplicationReportRawDataPage
    extends BasicElement<ApplicationReportRawDataPage>
{
  public static final String ROOT = "#application-report-raw-data";

  public static String url(Application app, String scanId) {
    return BaseUrl.resolvePageUrl("/applicationReport/{applicationPublicId}/{scanId}/raw", app.getPublicId(), scanId);
  }

  public ApplicationReportRawDataPage() {
    super(ROOT);
  }

  public IqBackButton backButton() {
    return new IqBackButton(ROOT);
  }

  public SelenideElement reportTitle() {
    return child("#raw-data-report-title");
  }
}
