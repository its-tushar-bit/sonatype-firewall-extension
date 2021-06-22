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
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;

public class DashboardPage
{
  public static final Condition ACTIVE = cssClass("active");

  public static String url() {
    return urlToViolations();
  }

  public static String urlToComponents() {
    return BaseUrl.resolvePageUrl("/dashboard/components");
  }

  public static String urlToViolations() {
    return BaseUrl.resolvePageUrl("/dashboard/violations");
  }

  public static String urlToApplications() {
    return BaseUrl.resolvePageUrl("/dashboard/applications");
  }

  public static String urlToNewestRisk() {
    return BaseUrl.resolvePageUrl("/dashboard/newest-risk");
  }

  public static final String ROOT =  "#dashboard-container";

  public static final String NEEDS_ACKNOWLEDGEMENT_MESSAGE = "Select your filter criteria and click " +
      "'apply' to see results.";

  public static SelenideElement dashboardContainer() {
    return $(ROOT);
  }

  public static DashboardTab violationsTab() {
    return new DashboardTab("#nx-tabs-0-tab-0");
  }

  public static DashboardTab componentsTab() {
    return new DashboardTab("#nx-tabs-0-tab-1");
  }

  public static DashboardTab applicationsTab() {
    return new DashboardTab("#nx-tabs-0-tab-2");
  }

  public static SelenideElement exportResultsLink() {
    return $("#export-results");
  }

  public static SelenideElement filterToggle() {
    return $("#filter-toggle");
  }

  public static SelenideElement filterToggleDirtyAsterisk() {
    return $("#filter-toggle-dirty-asterisk");
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

  public static SelenideElement needsAcknowledgementMessage() {
    return $(createSelector(ROOT, "#needs-acknowledgement"));
  }
}
