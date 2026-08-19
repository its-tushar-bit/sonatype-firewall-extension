/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class DashboardPage
    extends BasePage
{
  private static final String ROOT = "#dashboard-container";

  public DashboardPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/dashboard/violations";
  }

  public static String urlToViolations() {
    return "/assets/index.html#/dashboard/violations";
  }

  public static String urlToComponents() {
    return "/assets/index.html#/dashboard/components";
  }

  public static String urlToApplications() {
    return "/assets/index.html#/dashboard/applications";
  }

  public static String urlToWaivers() {
    return "/assets/index.html#/dashboard/waivers";
  }

  public static String urlToWaiverRequests() {
    return "/assets/index.html#/dashboard/waiverRequests";
  }

  public Locator dashboardContainer() {
    return locator(ROOT);
  }

  public Locator pageLoadSpinner() {
    return locator(".nx-loading-spinner");
  }

  public Locator violationsTab() {
    return byRole(AriaRole.TAB, "Violations");
  }

  public Locator componentsTab() {
    return byRole(AriaRole.TAB, "Components");
  }

  public Locator applicationsTab() {
    return byRole(AriaRole.TAB, "Applications");
  }

  public Locator waiversTab() {
    return byRole(AriaRole.TAB, "Waivers");
  }

  public Locator filterToggle() {
    return locator("#filter-toggle");
  }

  public Locator filterContainer() {
    return locator("#dashboard-filter-container");
  }

  public Locator filterToggleDirtyAsterisk() {
    return locator("#filter-toggle-dirty-asterisk");
  }

  public Locator formMask() {
    return locator(".iq-dashboard-form-mask");
  }

  public Locator categoryFilter() {
    return locator("#category-filter");
  }

  public Locator categoryFilterTrigger() {
    return categoryFilter().getByRole(AriaRole.BUTTON);
  }

  public Locator categoryFilterFirstCheckbox() {
    return categoryFilter().locator("label").first();
  }

  public Locator policyTypeFilter() {
    return locator("#policy-type-filter");
  }

  public Locator policyTypeFilterTrigger() {
    return policyTypeFilter().getByRole(AriaRole.BUTTON);
  }

  public Locator policyTypeFilterFirstCheckbox() {
    return policyTypeFilter().locator("label").first();
  }

  public Locator exportResultsLink() {
    return locator("#export-results");
  }

  public Locator needsAcknowledgementMessage() {
    return locator(ROOT + " #needs-acknowledgement");
  }

  public Locator dashboardDisabledMessage() {
    return byText("The Dashboard feature has been disabled by your administrator.");
  }

  public void expandFilter() {
    assertThat(filterContainer()).isHidden(
        new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.BRIEF_UI_TRANSITION_MS));
    filterToggle().click();
    assertThat(filterContainer()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void waitUntilSpinnersGone() {
    // .first() avoids strict-mode violation when both the dashboard and the filter-container
    // each render a .nx-loading-spinner simultaneously.
    assertThat(pageLoadSpinner().first()).isHidden(
        new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  public void selectViolationsTab() {
    violationsTab().click();
  }

  public void selectComponentsTab() {
    componentsTab().click();
  }

  public void selectApplicationsTab() {
    applicationsTab().click();
  }

  public void selectWaiversTab() {
    waiversTab().click();
  }

  public void selectViolationsTabAndWait() {
    selectViolationsTab();
    PlaywrightWaitUtils.waitForUrl(page, urlToViolations());
  }

  public void selectComponentsTabAndWait() {
    selectComponentsTab();
    PlaywrightWaitUtils.waitForUrl(page, urlToComponents());
  }

  public void selectApplicationsTabAndWait() {
    selectApplicationsTab();
    PlaywrightWaitUtils.waitForUrl(page, urlToApplications());
  }

  public void selectWaiversTabAndWait() {
    selectWaiversTab();
    PlaywrightWaitUtils.waitForUrl(page, urlToWaivers());
  }

}
