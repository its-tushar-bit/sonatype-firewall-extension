/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Organization;

/**
 * Regression-only locators and actions for the Source Control Configuration editor.
 * Locators not present on {@link SourceControlConfigurationPage} or
 * {@link SourceControlConfigurationPageAssertions}.
 *
 * <p>
 * Do NOT add methods to the existing page objects.
 */
public class SourceControlRegressionPage
    extends BasePage
{
  /** Matches {@code /rest/product/features} with optional query params (e.g. {@code ?timestamp=}). */
  private static final Pattern PRODUCT_FEATURES_PATTERN =
      Pattern.compile(".*/rest/product/features([?#][^/]*)?$");

  private static final Pattern SC_METRICS_PATTERN =
      Pattern.compile(".*/api/v2/sourceControlMetrics/application/.*");

  /** Matches {@code /api/v2/compositeSourceControlConfigValidator/application/<id>}. */
  private static final Pattern SC_VALIDATE_PATTERN =
      Pattern.compile(".*/api/v2/compositeSourceControlConfigValidator/.*");

  private static final String PROVIDER_RADIO = "label:has(input[name='provider'])";

  // Capital-C "Credentials" matches the NxFieldset name prop set in OrgSourceControlConfiguration.jsx
  private static final String CREDENTIALS_RADIO = "label:has(input[name='Credentials'])";

  private static final String BRANCH_RADIO = "label:has(input[name='baseBranch'])";

  private static final String INHERIT = "Inherit";

  private static final String OVERRIDE = "Override";

  private static final String CONTINUE = "Continue";

  private static final String BTN_MANAGE_GITHUB_APPS = "Manage GitHub Apps";

  public SourceControlRegressionPage() {
    super();
  }

  /**
   * Username input ({@code #source-control-username}); only mounted for Azure DevOps and Bitbucket providers.
   * ID selector used because the NxTextInput label is conditionally composed from the provider
   * name at runtime, so {@code getByLabel()} would require a provider-specific string and is fragile.
   */
  public Locator usernameInput() {
    return locator("#source-control-username");
  }

  /**
   * GitHub App authentication fieldset ({@code #github-authentication-method}).
   * ID selector used because the container is a bare {@code <div>} with no ARIA role or accessible
   * name; {@code getByRole(GROUP)} requires a legend/label to scope by name, which this element lacks.
   */
  public Locator githubAuthFieldset() {
    return locator("#github-authentication-method");
  }

  /** "Manage GitHub Apps" button inside the GitHub App auth fieldset. */
  public Locator manageGitHubAppsButton() {
    return githubAuthFieldset().getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName(BTN_MANAGE_GITHUB_APPS));
  }

  /**
   * Returns {@code true} when the GitHub App auth fieldset is visible, indicating the feature flag is enabled.
   * Waits up to {@link PlaywrightTiming#ELEMENT_TIMEOUT_MS} for the fieldset to appear,
   * since it renders asynchronously after provider selection.
   * Returns {@code false} if the fieldset does not appear within the timeout.
   */
  public boolean isGitHubAppAvailable() {
    try {
      githubAuthFieldset()
          .waitFor(new Locator.WaitForOptions()
              .setState(WaitForSelectorState.VISIBLE)
              .setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
      return true;
    }
    catch (TimeoutError e) {
      return false;
    }
  }

  /**
   * Reset config button ({@code #reset-source-control-button}); disabled when no existing config.
   * ID selector used because the button accessible name includes the owner name at runtime
   * (e.g. "Reset Root Organization"), making {@code getByRole(BUTTON, setName(...))} fragile.
   */
  public Locator resetButton() {
    return locator("#reset-source-control-button");
  }

  /**
   * Reset confirmation modal ({@code #reset-source-control-modal}).
   * ID selector used because NxModal renders as a bare {@code <div>} with no accessible name;
   * {@code getByRole(DIALOG)} without a name would match any open modal on the page.
   */
  public Locator resetModal() {
    return locator("#reset-source-control-modal");
  }

  public Locator resetModalHeading() {
    return resetModal().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setLevel(2));
  }

  /** "Continue" submit button inside the reset modal. */
  public Locator resetModalContinueButton() {
    return resetModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(CONTINUE));
  }

  /**
   * Auth-required warning alert ({@code data-testid="source-control-token-warning"}).
   * Rendered by {@code SourceControlConfiguration.jsx} when {@code isShowAccessTokenWarning} is true.
   * {@code data-testid} used because the alert text varies by auth context (GitHub App vs PAT),
   * making {@code getByRole(ALERT, setName(...))} fragile across provider combinations.
   */
  public Locator tokenWarningAlert() {
    return byTestId("source-control-token-warning");
  }

  /**
   * License-gate error alert ({@code #source-control-not-supported}).
   * Rendered when {@code !isSourceControlSupported && !isLoading}.
   * ID selector used because multiple NxErrorAlerts can appear on the page simultaneously;
   * no stable accessible name is set on this element to scope {@code getByRole(ALERT)} uniquely.
   */
  public Locator unsupportedAlert() {
    return locator("#source-control-not-supported");
  }

  /**
   * Repository Clone URL input ({@code #editor-source-control-url}); present on app-level editor only.
   * ID selector used because the NxTextInput renders no {@code <label for="...">} association —
   * the visible "Repository Clone URL" label is in a sibling {@code NxFormGroup} span, so
   * {@code getByLabel()} has no explicit association to resolve.
   */
  public Locator repoUrlInput() {
    return locator("#editor-source-control-url");
  }

  /**
   * Update-URL confirmation modal ({@code #update-source-control-url-modal}).
   * ID selector used because NxModal renders as a bare {@code <div>} with no accessible name;
   * {@code getByRole(DIALOG)} without a name would match any open modal on the page.
   */
  public Locator updateUrlModal() {
    return locator("#update-source-control-url-modal");
  }

  /** "Continue" submit button inside the update-URL confirmation modal. */
  public Locator updateUrlModalContinueButton() {
    return updateUrlModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(CONTINUE));
  }

  /** "Cancel" button inside the update-URL confirmation modal. */
  public Locator updateUrlModalCancelButton() {
    return updateUrlModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"));
  }

  /**
   * "Test Configuration" button ({@code #test-source-control-config-button}).
   * ID selector used because the button is rendered disabled during loading and dirty-form states;
   * scoping via ID avoids potential name collisions with other action buttons on the same page.
   */
  public Locator testConfigButton() {
    return locator("#test-source-control-config-button");
  }

  /**
   * Automated PR metrics table ({@code .iq-automated-pr-table}).
   * Rendered by {@code SourceControlAutomatedPullRequestTable} when {@code sourceControlMetrics.results}
   * is non-empty; only present on the application-level editor.
   * CSS class used because the table has no ARIA role or accessible name that uniquely identifies
   * it — {@code getByRole(TABLE)} would match any table on the page without a scoping name.
   */
  public Locator automatedPrTable() {
    return locator(".iq-automated-pr-table");
  }

  /**
   * "Configuration Test Results" section ({@code #scm-config-results}).
   * Rendered by {@code TestConfigurationResults} after {@code actions.validate()} resolves
   * (success, error, or loading). Only present on the application-level editor.
   * ID selector used because the section is a bare {@code <div>} container with no ARIA role
   * or accessible name; it is used as a structural anchor for inner role-based queries.
   */
  public Locator testConfigResultsSection() {
    return locator("#scm-config-results");
  }

  /**
   * Intercepts {@code /rest/product/features} and returns the given feature names as a JSON array.
   * Pass no arguments to simulate an empty feature set ({@code []}).
   *
   * @param features feature name strings; must not contain {@code "} or {@code \}
   */
  public void mockProductFeatures(String... features) {
    String body = Arrays.stream(features)
        .map(f -> "\"" + f + "\"")
        .collect(Collectors.joining(",", "[", "]"));
    page.route(PRODUCT_FEATURES_PATTERN, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("application/json")
        .setBody(body)));
  }

  /**
   * Intercepts {@code /api/v2/compositeSourceControlConfigValidator/*} and returns a canned
   * validation result. The embedded test server has no real SCM connectivity, so the validate
   * endpoint would always return a network error without this mock.
   */
  public void mockSourceControlValidate() {
    page.route(SC_VALIDATE_PATTERN, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("application/json")
        .setBody("{\"configurationComplete\":{\"valid\":true},"
            + "\"repoPrivate\":{\"valid\":true},"
            + "\"tokenPermissions\":{\"valid\":true}}")));
  }

  /**
   * Intercepts {@code /api/v2/sourceControlMetrics/application/*} and returns a single result
   * row with the given PR title. The embedded test server does not generate real PR history.
   *
   * @param prTitle PR title string; must not contain {@code "} or {@code \}
   */
  public void mockSourceControlMetrics(String prTitle) {
    page.route(SC_METRICS_PATTERN, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType("application/json")
        .setBody(("{\"results\":[{\"title\":\"%s\",\"status\":\"SUCCESS\","
            + "\"totalTime\":\"2m\",\"startTime\":\"2025-01-15T10:00:00Z\","
            + "\"reasoning\":\"All checks passed\"}]}").formatted(prTitle))));
  }

  /**
   * "Inherit (Not Configured)" / "Inherit from X" radio for the Provider field group in the
   * org-level editor ({@code OrgSourceControlConfiguration.jsx}).
   *
   * <p>
   * CSS attribute selector scopes to the hidden {@code <input name="provider">} inside the
   * NxRadio label because there is no stable ID on the fieldset label element.
   */
  public Locator providerInheritRadio() {
    return locator(PROVIDER_RADIO).filter(new Locator.FilterOptions().setHasText(INHERIT));
  }

  /** "Override" radio for the Provider field group in the org-level editor. */
  public Locator providerOverrideRadio() {
    return locator(PROVIDER_RADIO).filter(new Locator.FilterOptions().setHasText(OVERRIDE));
  }

  /**
   * "Inherit" radio for the Credentials field group in the org-level editor.
   * NxRadio {@code name="Credentials"} (capital C, matching the component's fieldset name).
   */
  public Locator credentialsInheritRadio() {
    return locator(CREDENTIALS_RADIO).filter(new Locator.FilterOptions().setHasText(INHERIT));
  }

  /** "Override" radio for the Credentials field group in the org-level editor. */
  public Locator credentialsOverrideRadio() {
    return locator(CREDENTIALS_RADIO).filter(new Locator.FilterOptions().setHasText(OVERRIDE));
  }

  /**
   * "Inherit" radio for the Default Branch field group in the org-level editor.
   * NxRadio {@code name="baseBranch"}.
   */
  public Locator branchInheritRadio() {
    return locator(BRANCH_RADIO).filter(new Locator.FilterOptions().setHasText(INHERIT));
  }

  /** "Override" radio for the Default Branch field group in the org-level editor. */
  public Locator branchOverrideRadio() {
    return locator(BRANCH_RADIO).filter(new Locator.FilterOptions().setHasText(OVERRIDE));
  }

  /**
   * "Close AutoPRs when one or more required checks fail" NxCheckbox input.
   * Only rendered for GitHub/GitLab providers at the root-org level. Always present in the DOM
   * but {@code disabled} unless {@code remediationPullRequestsEnabled=true}; the input is
   * CSS-hidden so use {@code isEnabled()} rather than {@code isVisible()}.
   * Role + accessible name is used because NxCheckbox forwards {@code name} only via
   * {@code inputAttributes}, which is not guaranteed across RSC versions.
   */
  public Locator failedChecksAutoprCheckbox() {
    return page.getByRole(AriaRole.CHECKBOX,
        new Page.GetByRoleOptions().setName("Close AutoPRs when one or more required checks fail"));
  }

  /**
   * "Close AutoPRs that have not been merged or closed after:" NxCheckbox input.
   * Always present in the DOM at root-org level but {@code disabled} unless
   * {@code remediationPullRequestsEnabled=true}; CSS-hidden, so use {@code isEnabled()}
   * rather than {@code isVisible()}.
   * Role + accessible name is used because NxCheckbox forwards {@code name} only via
   * {@code inputAttributes}, which is not guaranteed across RSC versions.
   */
  public Locator afterDaysAutoprCheckbox() {
    return page.getByRole(AriaRole.CHECKBOX,
        new Page.GetByRoleOptions().setName("Close AutoPRs that have not been merged or closed after:"));
  }

  /**
   * Navigates to the root-organisation Source Control editor via UI clicks:
   * global sidebar "Orgs and Policies" → root org summary → Source Control tile link.
   */
  public void navigateToRootOrgSourceControlViaUi() {
    globalSidebarLink("Orgs and Policies").click();
    page.waitForURL(Pattern.compile(".*" + Organization.ROOT_ORGANIZATION_ID + ".*"));
    new OwnerSummaryPage().clickSourceControlConfigurationLink();
  }

}
