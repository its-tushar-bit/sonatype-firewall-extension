/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.SelenideElement;

public class ApplicationAttributionReportPage
    extends BasicElement<ApplicationAttributionReportPage>
{
  public static final String ROOT = "body";

  public static String url(Application app, String stage) {
    return BaseUrl.resolveApiV2Url("/licenseLegalMetadata/application/{applicationPublicId}/stage/{stage}/report",
        app.getPublicId(),
        stage);
  }

  public ApplicationAttributionReportPage() {
    super(ROOT);
  }

  public SelenideElement reportTitle() {
    return child("h1");
  }
}
