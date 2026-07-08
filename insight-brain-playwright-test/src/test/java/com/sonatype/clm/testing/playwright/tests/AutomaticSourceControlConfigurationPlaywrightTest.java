/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.AutomaticSourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.AutomaticSourceControlConfigurationPageAssertions;

import com.microsoft.playwright.Route;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Automatic Source Control configuration page ({@code /automaticSourceControlConfiguration}).
 *
 * <p>
 * The backing {@code AutomaticSourceControlConfigurationResource.get()} is gated by
 * {@code @HasFeature(SAAS_LIFECYCLE_SCM_ENABLED)} which is not on the default test license,
 * so the page's config-load call is stubbed via {@code page.route(...)} for all tests. This
 * lets the page render deterministically under whatever seeded state the test needs without
 * touching the DB.
 */
public class AutomaticSourceControlConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String AUTO_SCM_CONFIG_ROUTE = "**/rest/config/automaticScmConfiguration**";

  private static final String AUTO_APP_CONFIG_ROUTE = "**/rest/config/automaticApplications**";

  private static final String ORGANIZATIONS_ROUTE = "**/rest/organization";

  // `/*` (single-segment star) instead of `/**` to guarantee the tail `pw-root-org` matches —
  // trailing `/**` in Playwright globs can silently skip requests when the tail is a single
  // segment, which would let the composite request escape the stub and hit the (feature-gated)
  // backend, freezing the load thunk (the composite promise has no .catch).
  private static final String COMPOSITE_SC_ROUTE = "**/api/v2/compositeSourceControl/organization/*";

  private static final String SCM_CONFIG_JSON_DISABLED = "{\"enabled\":false}";

  private static final String AUTO_APP_CONFIG_DISABLED_JSON =
      "{\"enabled\":false,\"parentOrganizationId\":null}";

  private static final String ORGANIZATIONS_JSON =
      "[{\"id\":\"pw-root-org\",\"name\":\"pw-root-org\"}]";

  private static final String COMPOSITE_SC_EMPTY_JSON = "{\"provider\":null}";

  private AutomaticSourceControlConfigurationPage configPage;

  private AutomaticSourceControlConfigurationPageAssertions configAssertions;

  @After
  public void unrouteAll() {
    page.unroute(AUTO_SCM_CONFIG_ROUTE);
    page.unroute(AUTO_APP_CONFIG_ROUTE);
    page.unroute(ORGANIZATIONS_ROUTE);
    page.unroute(COMPOSITE_SC_ROUTE);
  }

  @Test
  @Category(RegressionTest.class)
  public void testPageRenders_toggleAndUpdate() {
    stubLoadEndpoints();
    openConfigPage();

    configAssertions.shouldRenderPageLayout();
    configAssertions.shouldHaveEnabledToggleUnchecked();

    configPage.enabledToggleLabel().click();
    configAssertions.shouldHaveEnabledToggleChecked();
    configAssertions.shouldHaveCancelEnabled();
  }

  /** Cancel disabled when clean; Cancel reverts when dirty. */
  @Test
  @Category(RegressionTest.class)
  public void testCancel_disabledWhenClean_revertsWhenDirty() {
    stubLoadEndpoints();
    openConfigPage();

    configAssertions.shouldRenderPageLayout();
    configAssertions.shouldHaveCancelDisabled();

    configPage.enabledToggleLabel().click();
    configAssertions.shouldHaveEnabledToggleChecked();
    configAssertions.shouldHaveCancelEnabled();

    configPage.cancelButton().click();
    configAssertions.shouldHaveEnabledToggleUnchecked();
    configAssertions.shouldHaveCancelDisabled();
  }

  /**
   * Installs the four endpoints the {@code loadAutomaticSourceControlConfiguration} thunk calls.
   * Both tests drive the "toggle OFF, no auto-apps, no SCM provider" branch.
   *
   * <p>
   * Note: don't stub {@code PUT /rest/user/permissions/global/global} — that route is on
   * the login critical path and stubbing it breaks session establishment. The embedded IQ
   * admin's native permissions are sufficient here.
   */
  private void stubLoadEndpoints() {
    stubJson(AUTO_SCM_CONFIG_ROUTE, SCM_CONFIG_JSON_DISABLED);
    stubJson(AUTO_APP_CONFIG_ROUTE, AUTO_APP_CONFIG_DISABLED_JSON);
    stubJson(ORGANIZATIONS_ROUTE, ORGANIZATIONS_JSON);
    stubJson(COMPOSITE_SC_ROUTE, COMPOSITE_SC_EMPTY_JSON);
  }

  private void stubJson(String routePattern, String body) {
    page.route(routePattern, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("application/json")
        .setBody(body)));
  }

  private void openConfigPage() {
    // Atomic login (navigate + login + wait-for-auth-header) — otherwise route guards can
    // race the login POST and issue 401s that never recover.
    playwrightLoginAdminAt(AutomaticSourceControlConfigurationPage.url());

    configPage = new AutomaticSourceControlConfigurationPage();
    configAssertions = new AutomaticSourceControlConfigurationPageAssertions(configPage);
  }
}
