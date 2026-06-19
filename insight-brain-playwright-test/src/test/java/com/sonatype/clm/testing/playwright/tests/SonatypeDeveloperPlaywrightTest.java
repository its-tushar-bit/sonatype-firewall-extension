/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Route;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.SonatypeDeveloperPage;
import com.sonatype.clm.testing.playwright.pages.SonatypeDeveloperPageAssertions;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;

/**
 * Lock-screen vs Overview is gated on {@code DEVELOPER_DASHBOARD}; summary section on {@code DEVELOPER_SUMMARY_TABLE}.
 */
public class SonatypeDeveloperPlaywrightTest
    extends AbstractIqUiTest
{
  /**
   * Capture-and-restore the flag's original value; {@code @Rule} ordering is deterministic vs the parent
   * {@code @After}.
   */
  @Rule
  public final ExternalResource resetFeatureFlags = new ExternalResource()
  {
    private boolean originalSummaryTableEnabled;

    @Override
    protected void before() {
      originalSummaryTableEnabled = SystemConfigurationPropertyFeature.DEVELOPER_SUMMARY_TABLE.isEnabled();
    }

    @Override
    protected void after() {
      SystemConfigurationPropertyFeature.DEVELOPER_SUMMARY_TABLE.setEnabled(originalSummaryTableEnabled);
      page.unrouteAll();
    }
  };

  /**
   * Documented exception to the no-IQ-backend-mocking rule (guardrails §13.6): the framework
   * has no per-test path to drop {@code DEVELOPER_DASHBOARD} that propagates through the
   * SPA's cached {@code productFeatures} state under {@code reuseForks=true}. Remove the
   * stub when the framework gains that path.
   */
  @Test
  @Category(RegressionTest.class)
  public void testDeveloperDashboard_licenseLockScreen() {
    page.route("**/rest/product/features*", route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("application/json")
        .setBody("[]")));

    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());
    playwrightLogin();

    SonatypeDeveloperPage developerPage = new SonatypeDeveloperPage();
    SonatypeDeveloperPageAssertions assertions = new SonatypeDeveloperPageAssertions(developerPage);

    assertions.shouldShowContainer();
    assertions.shouldShowPageTitle(SonatypeDeveloperPage.LOCK_SCREEN_HEADING);
    assertions.shouldShowLicenseLockScreen();
  }

  /** Cards row is outside the summary ternary (Overview.jsx:55-59), so both layout tests assert it. */
  @Test
  @Category(RegressionTest.class)
  public void testDeveloperDashboard_summaryEnabledLayout() {
    SystemConfigurationPropertyFeature.DEVELOPER_SUMMARY_TABLE.setEnabled(true);

    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());
    playwrightLogin();

    SonatypeDeveloperPage developerPage = new SonatypeDeveloperPage();
    SonatypeDeveloperPageAssertions assertions = new SonatypeDeveloperPageAssertions(developerPage);

    assertions.shouldShowContainer();
    assertions.shouldShowPageTitle(SonatypeDeveloperPage.DASHBOARD_HEADING);
    assertions.shouldShowSummaryTableSection();
    assertions.shouldShowAllIntegrationCards();
  }

  @Test
  @Category(RegressionTest.class)
  public void testDeveloperDashboard_summaryDisabledLayout() {
    SystemConfigurationPropertyFeature.DEVELOPER_SUMMARY_TABLE.setEnabled(false);

    playwrightRefreshOrOpen(SonatypeDeveloperPage.url());
    playwrightLogin();

    SonatypeDeveloperPage developerPage = new SonatypeDeveloperPage();
    SonatypeDeveloperPageAssertions assertions = new SonatypeDeveloperPageAssertions(developerPage);

    assertions.shouldShowContainer();
    assertions.shouldShowSummaryDisabledInfoAlert();
    assertions.shouldShowAllIntegrationCards();
  }
}
