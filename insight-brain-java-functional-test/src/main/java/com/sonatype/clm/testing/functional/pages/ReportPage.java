/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.insight.brain.model.Application;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ReportPage
{
  public static SelenideElement policyTabButton() {
    return $("#componentcontainerBtn");
  }

  public static SelenideElement summaryTabButton() {
    return $("#summaryBtn");
  }

  public static SelenideElement licenseChart() {
    return $("#license-chart");
  }

  public static SelenideElement securityContainerButton() {
    return $("#securitycontainerBtn");
  }

  public static SelenideElement licenseContainerButton() {
    return $("#licensecontainerBtn");
  }

  public static SelenideElement componentContainer() {
    return $("#componentcontainer");
  }

  public static SelenideElement coverageDonut() {
    return $("#coverage_donut");
  }

  public static SelenideElement securityTable() {
    return $("#securityTable");
  }

  public static SelenideElement licenseContainer() {
    return $("#licensecontainer");
  }

  public static String url(Application app, String scanId) {
    return BaseUrl.rootUriBuilder().path("rest/report/{applicationPublicId}/{scanId}/browseReport/index.html")
        .build(app.getPublicId(), scanId).toString();
  }
}
