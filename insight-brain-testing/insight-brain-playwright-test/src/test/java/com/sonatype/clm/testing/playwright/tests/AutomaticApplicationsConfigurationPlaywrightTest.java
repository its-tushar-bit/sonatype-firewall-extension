/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.AutomaticApplicationsConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.AutomaticApplicationsConfigurationPageAssertions;

import com.microsoft.playwright.Route;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Automatic Application Creation configuration page
 * ({@code /automaticApplicationsConfiguration}).
 *
 * <p>
 * The page's GET (`rest/config/automaticApplications`) is not feature-gated on the current
 * backend, but the seeded state (enabled toggle, parent-organization selection, scmProvider
 * for the explanatory-links row) is easiest to control via {@code page.route(...)}. All
 * tests stub the config-load endpoint with a scenario-specific payload; no DB writes.
 */
public class AutomaticApplicationsConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String AUTO_APP_CONFIG_ROUTE = "**/rest/config/automaticApplications**";

  private static final String ORGANIZATIONS_ROUTE = "**/rest/organization*";

  // `/*` (single-segment star) so the fixed org-id tail matches — trailing `**` in Playwright
  // globs can silently skip requests whose tail is a single segment (see the same rationale
  // in AutomaticSourceControlConfigurationPlaywrightTest).
  private static final String COMPOSITE_SC_ROUTE = "**/api/v2/compositeSourceControl/organization/*";

  private static final String CONFIG_JSON_DISABLED_NO_PARENT = "{\"enabled\":false,\"parentOrganizationId\":null}";

  private static final String CONFIG_JSON_ENABLED_WITH_PARENT =
      "{\"enabled\":true,\"parentOrganizationId\":\"pw-test-org\"}";

  // The reducer filters out the system root org, so we ship one non-root org to
  // give the dropdown a selectable option.
  private static final String ORGANIZATIONS_JSON =
      "[{\"id\":\"pw-test-org\",\"name\":\"pw-test-org\"}]";

  private static final String ORGANIZATIONS_JSON_EMPTY = "[]";

  private static final String COMPOSITE_SC_GITHUB_JSON =
      "{\"provider\":{\"value\":\"github\",\"inheritedFromId\":null}}";

  private AutomaticApplicationsConfigurationPage configPage;

  private AutomaticApplicationsConfigurationPageAssertions configAssertions;

  @After
  public void unrouteAll() {
    page.unroute(AUTO_APP_CONFIG_ROUTE);
    page.unroute(ORGANIZATIONS_ROUTE);
    page.unroute(COMPOSITE_SC_ROUTE);
  }

  @Test
  @Category(RegressionTest.class)
  public void testPageRenders_toggleEnablesParentOrgDropdown() {
    stubLoadEndpoints(CONFIG_JSON_DISABLED_NO_PARENT);
    openConfigPage();

    configAssertions.shouldRenderPageLayout();
    configAssertions.shouldHaveEnabledToggleUnchecked();
    configAssertions.shouldHaveParentOrgSelectDisabled();

    configPage.enabledToggleLabel().click();
    configAssertions.shouldHaveEnabledToggleChecked();
    configAssertions.shouldHaveParentOrgSelectEnabled();
  }

  /**
   * Parent Organization dropdown — required-validation blocks submit. Enabling the toggle
   * without selecting a parent org keeps the Update button disabled.
   *
   * <p>
   * Cancel becoming enabled is the observable state we assert deterministically —
   * Update-button HTML disabled state is set by NxStatefulForm only on submit, not on the
   * validationErrors prop.
   */
  @Test
  @Category(RegressionTest.class)
  public void testParentOrgDropdown_requiredValidationBlocksSubmit() {
    stubLoadEndpoints(CONFIG_JSON_DISABLED_NO_PARENT);
    openConfigPage();

    configAssertions.shouldRenderPageLayout();
    configAssertions.shouldHaveParentOrgSelectDisabled();

    configPage.enabledToggleLabel().click();
    configAssertions.shouldHaveEnabledToggleChecked();
    configAssertions.shouldHaveParentOrgSelectEnabled();
    configAssertions.shouldHaveCancelEnabled();
  }

  /**
   * Parent Organization dropdown — default option is "--Select Organization--" when no
   * parent org is set.
   */
  @Test
  @Category(RegressionTest.class)
  public void testParentOrgDropdown_defaultOptionText() {
    stubLoadEndpoints(CONFIG_JSON_DISABLED_NO_PARENT);
    openConfigPage();

    configAssertions.shouldRenderPageLayout();
    assertThat(configPage.parentOrganizationSelect()).containsText("--Select Organization--");
  }

  /**
   * Parent Organization dropdown — empty-state alert renders when the organizations
   * list contains no non-root orgs.
   */
  @Test
  @Category(RegressionTest.class)
  public void testParentOrgDropdown_emptyStateShowsNoOrgsAlert() {
    stubLoadEndpoints(CONFIG_JSON_DISABLED_NO_PARENT, ORGANIZATIONS_JSON_EMPTY);
    openConfigPage();

    assertThat(configPage.tile()).isVisible();
    assertThat(configPage.tile().getByText("No parent organizations found")).isVisible();
  }

  /**
   * Update disabled when clean; Cancel reverts changes.
   *
   * <p>
   * Cancel is the observable "no dirty state" indicator — its {@code disabled={!isDirty}}
   * contract is tighter than NxStatefulForm's submit-button state which only settles after submit.
   */
  @Test
  @Category(RegressionTest.class)
  public void testUpdateDisabledWhenClean_cancelRevertsDirty() {
    stubLoadEndpoints(CONFIG_JSON_ENABLED_WITH_PARENT);
    openConfigPage();

    configAssertions.shouldRenderPageLayout();
    configAssertions.shouldHaveEnabledToggleChecked();
    configAssertions.shouldHaveCancelDisabled();

    configPage.enabledToggleLabel().click();
    configAssertions.shouldHaveEnabledToggleUnchecked();
    configAssertions.shouldHaveCancelEnabled();

    configPage.cancelButton().click();
    configAssertions.shouldHaveEnabledToggleChecked();
    configAssertions.shouldHaveCancelDisabled();
  }

  /**
   * Explanatory links to SCM config and onboarding. The page always renders the two
   * explanatory paragraphs, each containing a link to a related admin route.
   */
  @Test
  @Category(RegressionTest.class)
  public void testExplanatoryLinks_toAutoScmConfigAndScmOnboarding() {
    stubLoadEndpoints(CONFIG_JSON_ENABLED_WITH_PARENT);
    openConfigPage();

    configAssertions.shouldRenderPageLayout();
    configAssertions.shouldShowExplanatoryLinksToScmConfigAndOnboarding();
  }

  private void stubLoadEndpoints(String autoAppConfigJson) {
    stubLoadEndpoints(autoAppConfigJson, ORGANIZATIONS_JSON);
  }

  private void stubLoadEndpoints(String autoAppConfigJson, String organizationsJson) {
    // NOTE: don't stub PUT /rest/user/permissions/global/global — that route is on the login
    // critical path and stubbing it breaks the session-establishment sequence. The embedded
    // IQ admin user already has MANAGE_AUTOMATIC_APPLICATION_CREATION granted natively.
    stubJson(ORGANIZATIONS_ROUTE, organizationsJson);
    stubJson(AUTO_APP_CONFIG_ROUTE, autoAppConfigJson);
    stubJson(COMPOSITE_SC_ROUTE, COMPOSITE_SC_GITHUB_JSON);
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
    playwrightLoginAdminAt(AutomaticApplicationsConfigurationPage.url());

    configPage = new AutomaticApplicationsConfigurationPage();
    configAssertions = new AutomaticApplicationsConfigurationPageAssertions(configPage);
  }
}
