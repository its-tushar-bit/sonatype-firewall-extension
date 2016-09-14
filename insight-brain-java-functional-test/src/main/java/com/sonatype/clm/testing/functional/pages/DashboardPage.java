/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.elements.DashboardApplications;
import com.sonatype.clm.testing.functional.elements.DashboardComponents;
import com.sonatype.clm.testing.functional.elements.DashboardTab;
import com.sonatype.clm.testing.functional.elements.DashboardViolations;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.BaseUrl.uriBuilder;

public class DashboardPage
{
  public static final Condition ACTIVE = cssClass("active");

  public static final String URL = uriBuilder().fragment("/dashboard/newest-risk").build().toString();
  public static final String COMPONENTS_URL = uriBuilder().fragment("/dashboard-new/components").build().toString();
  public static final String VIOLATIONS_URL = uriBuilder().fragment("/dashboard-new/violations").build().toString();
  public static final String NEWEST_RISK_URL = uriBuilder().fragment("/dashboard-new/newest-risk").build().toString();
  public static final String APPLICATIONS_URL = uriBuilder().fragment("/dashboard-new/applications").build().toString();

  public static final String ROOT =  ".dashboard-container";

  /**
   * @deprecated use dashboardContainer() once new dashboard is in place
   */
  @Deprecated
  public static SelenideElement body() {
    return $(".dashboard-root-container");
  }

  public static SelenideElement dashboardContainer() {
    return $(ROOT);
  }

  public static DashboardTab violationsTab() {
    return new DashboardTab("#tab-button-newest");
  }

  public static DashboardTab componentsTab() {
    return new DashboardTab("#tab-button-component");
  }

  public static DashboardTab applicationsTab() {
    return new DashboardTab("#tab-button-application");
  }

  public static SelenideElement calculateTrendsButton() {
    return $("#show-trend-dialog");
  }

  public static TrendsModal trendsModal() {
    return new TrendsModal();
  }

  public static DashboardComponents componentsView() {
    return new DashboardComponents();
  }

  public static DashboardViolations violationsView() {
    return new DashboardViolations();
  }

  public static DashboardApplications applicationsView() {
    return new DashboardApplications();
  }


  public static SelenideElement tooltip() {
    return $(".tooltip.dashboard-tooltip");
  }
}
