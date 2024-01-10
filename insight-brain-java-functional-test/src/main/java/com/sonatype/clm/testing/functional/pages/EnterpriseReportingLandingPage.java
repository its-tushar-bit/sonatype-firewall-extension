/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class EnterpriseReportingLandingPage
    extends BasicElement<EnterpriseReportingLandingPage>
{
  public static final String ROOT = "#enterprise-reporting-landing-page";

  public EnterpriseReportingLandingPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/enterpriseReportingLandingPage");
  }

  public SelenideElement reports() {
    return child("#enterprise-reporting-dashboards-container");
  }

  public SelenideElement enterpriseReportingNotEnabledError() {
    return child(".nx-alert--error");
  }

  public SelenideElement heading() {
    return child("#enterprise-reporting-landing-page-title");
  }

  public SelenideElement description() {
    return child("#enterprise-reporting-landing-page-description");
  }

  public SelenideElement contactus() {
    return child(".iq-enterprise-reporting__contactus");
  }
}
