/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ApplicationReportContainerPage
{
  public static String url(String appId, String scanId) {
    return BaseUrl.resolvePageUrl("/reports/{appId}/{scanId}", appId, scanId);
  }

  public static SelenideElement getReportTitle() {
    return $("#report-title");
  }

  public static SelenideElement getIframe() {
    return $("#evaluation-report-container > iframe");
  }
}
