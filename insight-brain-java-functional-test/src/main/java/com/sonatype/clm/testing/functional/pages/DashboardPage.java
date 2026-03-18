/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.time.Duration;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.DashboardApplications;
import com.sonatype.clm.testing.functional.elements.DashboardComponents;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardTab;
import com.sonatype.clm.testing.functional.elements.DashboardViolations;
import com.sonatype.clm.testing.functional.elements.DashboardWaiverRequests;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import static com.codeborne.selenide.Condition.clickable;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class DashboardPage
    extends BasicElement<DashboardPage>
{
  public static final WebElementCondition ACTIVE = cssClass("active");

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

  public static String urlToWaivers() {
    return BaseUrl.resolvePageUrl("/dashboard/waivers");
  }

  public static String urlToWaiverRequests() {
    return BaseUrl.resolvePageUrl("/dashboard/waiverRequests");
  }

  public static String urlToNewestRisk() {
    return BaseUrl.resolvePageUrl("/dashboard/newest-risk");
  }

  public static final String ROOT = "#dashboard-container";

  public static final String NEEDS_ACKNOWLEDGEMENT_MESSAGE = "Select your filter criteria and click " +
      "'apply' to see results.";

  public static SelenideElement dashboardContainer() {
    return $(ROOT);
  }

  public static DashboardTab violationsTab() {
    return new DashboardTab(createSelector(".nx-tab", nthChild(1)));
  }

  public static DashboardTab componentsTab() {
    return new DashboardTab(createSelector(".nx-tab", nthChild(2)));
  }

  public static DashboardTab applicationsTab() {
    return new DashboardTab(createSelector(".nx-tab", nthChild(3)));
  }

  public static DashboardTab waiversTab() {
    return new DashboardTab(createSelector(".nx-tab", nthChild(4)));
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

  public static DashboardWaivers waiversView() {
    return new DashboardWaivers();
  }

  public static DashboardWaiverRequests waiverRequestsView() {
    return new DashboardWaiverRequests();
  }

  public static SelenideElement needsAcknowledgementMessage() {
    return $(createSelector(ROOT, "#needs-acknowledgement"));
  }

  public static SelenideElement pageLoadSpinner() {
    return $(".nx-loading-spinner");
  }

  public static void expandFilter() {
    // make sure the toggle is ready before trying to click it
    DashboardFilters.filterContainer().shouldBe(hidden);
    filterToggle().shouldBe(clickable);

    filterToggle().click();

    // make sure it's visible before we move on, increasing timeout because I've observed this taking slightly longer
    // than the default 4 seconds
    waitForDrawerAnimation();
    DashboardFilters.filterContainer().shouldBe(visible, Duration.ofSeconds(10));
  }

  public static void waitUntilSpinnersGone() {
    pageLoadSpinner().shouldNotBe(visible, Duration.ofSeconds(10));
  }

  public static void waitForDrawerAnimation() {
    Selenide.sleep(300);
  }
}
