/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlRegressionPage;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression tests for the Source Control Configuration editor.
 *
 * <p>
 * Row 89 (HTTPS alert) omitted: the alert exists only in the Webhook editor, not Source Control.
 *
 * <p>
 * Row 88 (GitHub App OAuth flow) partially automated: the github.com callback cannot be
 * intercepted; seeded post-install state is covered in
 * {@link #testGitHubAppSection_seededInstallShowsReplaceButton}.
 */
public class SourceControlRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private static final LocatorAssertions.IsVisibleOptions VISIBLE_OPTS =
      new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsHiddenOptions HIDDEN_OPTS =
      new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.HasTextOptions TEXT_OPTS =
      new LocatorAssertions.HasTextOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsEnabledOptions ENABLED_OPTS =
      new LocatorAssertions.IsEnabledOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final LocatorAssertions.IsDisabledOptions DISABLED_OPTS =
      new LocatorAssertions.IsDisabledOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS);

  private static final String PROVIDER_GITHUB = "GitHub";

  private static final String PROVIDER_BITBUCKET = "Bitbucket";

  private static final String PROVIDER_AZURE_DEVOPS = "Azure DevOps";

  private static final String PROVIDER_GITLAB = "GitLab";

  private static final String TEST_TOKEN = "test-token";

  private static final String DEFAULT_BRANCH = "main";

  private static final String BTN_CREATE = "Create";

  private static final String BTN_UPDATE = "Update";

  private static final String FEATURE_NOTIFICATIONS = "notifications";

  private static final String FEATURE_AUTOMATION = "automation";

  private static final String FEATURE_SAAS_SCM_PRS = "saas-lifecycle-scm-prs-enabled";

  private static final String TOGGLE_PR_COMMENTING = "source-control-pull-request-commenting";

  private static final String TOGGLE_REMEDIATION_PR = "source-control-remediation-pull-requests";

  private static final String TOGGLE_SSH = "source-control-ssh";

  private static final String MSG_GITHUB_APP_COUNT = "1 GitHub App configured";

  private static final String BTN_MANAGE_GITHUB_APPS = "Manage GitHub Apps";

  private static final String MSG_AUTH_NOT_CONFIGURED = "Authentication method must be configured";

  private static final String MSG_GITHUB_APP_UNAVAILABLE =
      "GitHub App authentication feature is not available in this environment";

  private static final String RESET_MODAL_HEADING = "Reset Source Control";

  private static final String MSG_SC_UNSUPPORTED = "Source Control is not supported by your license";

  private static final String PR_TABLE_HEADING = "Daily Automated Pull Requests";

  private static final String PR_TITLE = "Automated-PR-Regression-Test";

  private static final String REPO_URL_WARN = "https://github.com/test/warn-repo";

  private static final String REPO_URL = "https://github.com/test/repo";

  private static final String REPO_URL_UPDATED = "https://github.com/test/repo-new";

  private static final String REPO_URL_PR = "https://github.com/test/pr-repo";

  @BeforeEach
  public void openDashboardAndLoginAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  private void navigateToRootOrgSourceControl() {
    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(
            Organization.ROOT_ORGANIZATION_ID, SourceControlConfigurationPage.URL_FRAGMENT),
        SourceControlConfigurationPage.URL_FRAGMENT);
  }

  private void navigateToAppSourceControl(String appPublicId) {
    navigateAndWaitForUrl(
        OwnerSummaryPage.editApplicationUrl(appPublicId, SourceControlConfigurationPage.URL_FRAGMENT),
        SourceControlConfigurationPage.URL_FRAGMENT);
  }

  /**
   * Extends {@link SourceControlPlaywrightTest#testOrgSourceControlConfigurationLayout} to cover
   * username-visibility across all four providers (sanity only checks GitHub+PAT). Navigates via
   * UI clicks to cover manual step 2: sidebar → root org → Source Control tile.
   */
  @Test
  @Tag("regression")
  public void testProviderFieldVisibility_usernameHiddenForGitHub_shownForBitbucket() {
    SourceControlRegressionPage regressionPage = new SourceControlRegressionPage();
    regressionPage.navigateToRootOrgSourceControlViaUi();
    playwrightWaitUntilUrlContains(SourceControlConfigurationPage.URL_FRAGMENT);

    SourceControlConfigurationPage editor = new SourceControlConfigurationPage();

    assertThat(editor.providerSelect()).isVisible(VISIBLE_OPTS);

    // selectGitHubPersonalAccessTokenCredentials handles the optional GitHub App auth radios
    // before the access token field becomes assertable.
    editor.selectGitHubPersonalAccessTokenCredentials();
    assertThat(regressionPage.usernameInput()).isHidden(HIDDEN_OPTS);
    assertThat(editor.accessTokenInput()).isVisible(VISIBLE_OPTS);
    assertThat(editor.defaultBranchInput()).isVisible(VISIBLE_OPTS);
    assertThat(editor.toggle(TOGGLE_PR_COMMENTING)).isVisible(VISIBLE_OPTS);

    editor.selectProvider(PROVIDER_BITBUCKET);
    assertThat(regressionPage.usernameInput()).isVisible(VISIBLE_OPTS);
    assertThat(editor.accessTokenInput()).isVisible(VISIBLE_OPTS);
    assertThat(editor.defaultBranchInput()).isVisible(VISIBLE_OPTS);

    editor.selectProvider(PROVIDER_AZURE_DEVOPS);
    assertThat(regressionPage.usernameInput()).isVisible(VISIBLE_OPTS);

    editor.selectProvider(PROVIDER_GITLAB);
    assertThat(regressionPage.usernameInput()).isHidden(HIDDEN_OPTS);
  }

  /**
   * Full OAuth flow (modal → github.com callback) cannot be automated; only seeded post-install
   * state is verified. Skipped when the GitHub App feature flag is not enabled.
   */
  @Test
  @Tag("regression")
  public void testGitHubAppSection_seededInstallShowsReplaceButton() {
    // A source control config record must exist for the owner so the SC API response
    // includes the githubApps list with count > 0.
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    tempEntity.newGitHubApp(Organization.ROOT_ORGANIZATION_ID);

    SourceControlRegressionPage regressionPage = new SourceControlRegressionPage();
    regressionPage.navigateToRootOrgSourceControlViaUi();
    playwrightWaitUntilUrlContains(SourceControlConfigurationPage.URL_FRAGMENT);

    SourceControlConfigurationPage editor = new SourceControlConfigurationPage();

    editor.selectProvider(PROVIDER_GITHUB);

    Assumptions.assumeTrue(regressionPage.isGitHubAppAvailable(), MSG_GITHUB_APP_UNAVAILABLE);

    assertThat(regressionPage.githubAuthFieldset()).isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.githubAuthFieldset().getByText(MSG_GITHUB_APP_COUNT))
        .isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.githubAuthFieldset()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(BTN_MANAGE_GITHUB_APPS)))
            .isVisible(VISIBLE_OPTS);
  }

  /**
   * Phase 1 — Features mocked to {@code ["notifications","saas-lifecycle-scm-prs-enabled"]}:
   * forces {@code isAutomationSupported = false} while rendering the SSH and Remediation PR
   * toggles (both are {@code saas-lifecycle-scm-prs-enabled}-gated). Asserts feature toggles
   * are disabled and Default Branch is disabled, but {@code sshEnabled} remains enabled
   * (it is never gated on automation support).
   *
   * <p>
   * Phase 2 — Re-mocks to {@code ["automation","saas-lifecycle-scm-prs-enabled","notifications"]}
   * and reloads so automation is supported. Enables the Remediation Pull Requests toggle, then
   * asserts the advanced AutoPR checkboxes appear and are enabled.
   */
  @Test
  @Tag("regression")
  public void testAutomationToggles_disabledWhenAutomationNotSupported() {
    SourceControlRegressionPage regressionPage = new SourceControlRegressionPage();
    regressionPage.mockProductFeatures(FEATURE_NOTIFICATIONS, FEATURE_SAAS_SCM_PRS);

    regressionPage.navigateToRootOrgSourceControlViaUi();
    // Reload triggers a fresh /rest/product/features fetch that the route intercept captures.
    page.reload();
    playwrightWaitUntilUrlContains(SourceControlConfigurationPage.URL_FRAGMENT);

    SourceControlConfigurationPage editor = new SourceControlConfigurationPage();
    editor.selectGitHubPersonalAccessTokenCredentials();

    assertThat(editor.defaultBranchInput()).isDisabled(DISABLED_OPTS);
    assertThat(editor.toggle(TOGGLE_PR_COMMENTING)).isDisabled(DISABLED_OPTS);
    // sshEnabled is the only toggle that stays enabled when automation is not supported
    assertThat(editor.toggle(TOGGLE_SSH)).isEnabled(ENABLED_OPTS);

    // Phase 2: enable automation support; new handler takes precedence on the next fetch
    regressionPage.mockProductFeatures(FEATURE_AUTOMATION, FEATURE_SAAS_SCM_PRS, FEATURE_NOTIFICATIONS);
    page.reload();
    playwrightWaitUntilUrlContains(SourceControlConfigurationPage.URL_FRAGMENT);

    editor.selectGitHubPersonalAccessTokenCredentials();
    assertThat(editor.toggle(TOGGLE_REMEDIATION_PR)).isVisible(VISIBLE_OPTS);
    editor.toggle(TOGGLE_REMEDIATION_PR).click();

    assertThat(regressionPage.failedChecksAutoprCheckbox()).isEnabled(ENABLED_OPTS);
    assertThat(regressionPage.afterDaysAutoprCheckbox()).isEnabled(ENABLED_OPTS);
  }

  /**
   * Mocks {@code ["automation","saas-lifecycle-scm-prs-enabled","notifications"]} so the
   * Remediation Pull Requests toggle renders (SaaS-gated on-prem) and automation is supported.
   * Seeds the root-org config with {@code remediationPullRequestsEnabled=true} so the advanced
   * AutoPR checkboxes load in the enabled state without a UI click; the NxCheckbox inputs are
   * CSS-hidden so only {@code isEnabled()} (not {@code isVisible()}) is reliable on them.
   */
  @Test
  @Tag("regression")
  public void testRemediationPrToggle_advancedAutoprOptionsEnabledWhenToggleOn() {
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null,
        SourceControlProvider.GITHUB, true, null, "main");

    SourceControlRegressionPage regressionPage = new SourceControlRegressionPage();
    regressionPage.mockProductFeatures(FEATURE_AUTOMATION, FEATURE_SAAS_SCM_PRS, FEATURE_NOTIFICATIONS);

    regressionPage.navigateToRootOrgSourceControlViaUi();
    page.reload();
    playwrightWaitUntilUrlContains(SourceControlConfigurationPage.URL_FRAGMENT);

    SourceControlConfigurationPage editor = new SourceControlConfigurationPage();
    // Seeded config has provider=GITHUB and remediationPullRequestsEnabled=true; just wait for
    // the form to load without any UI interaction (no need to call selectGitHubPersonalAccessTokenCredentials).
    assertThat(editor.defaultBranchInput()).isEnabled(ENABLED_OPTS);
    assertThat(editor.toggle(TOGGLE_PR_COMMENTING)).isEnabled(ENABLED_OPTS);
    assertThat(editor.toggle(TOGGLE_REMEDIATION_PR)).isVisible(VISIBLE_OPTS);
    assertThat(editor.toggle(TOGGLE_REMEDIATION_PR)).isEnabled(ENABLED_OPTS);

    assertThat(regressionPage.failedChecksAutoprCheckbox()).isEnabled(ENABLED_OPTS);
    assertThat(regressionPage.afterDaysAutoprCheckbox()).isEnabled(ENABLED_OPTS);
  }

  /** Root-org SC config seeded via tempEntity — cleanup is guaranteed even if Reset fails. */
  @Test
  @Tag("regression")
  public void testSaveCreatesConfig_resetModalClearsConfig() {
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, TEST_TOKEN, SourceControlProvider.GITHUB);

    SourceControlRegressionPage regressionPage = new SourceControlRegressionPage();
    regressionPage.navigateToRootOrgSourceControlViaUi();
    playwrightWaitUntilUrlContains(SourceControlConfigurationPage.URL_FRAGMENT);

    SourceControlConfigurationPage editor = new SourceControlConfigurationPage();

    assertThat(editor.submitButton()).hasText(BTN_UPDATE, TEXT_OPTS);
    assertThat(regressionPage.resetButton()).isEnabled(ENABLED_OPTS);
    regressionPage.resetButton().click();

    assertThat(regressionPage.resetModal()).isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.resetModalHeading()).hasText(RESET_MODAL_HEADING, TEXT_OPTS);

    regressionPage.resetModalContinueButton().click();

    assertThat(regressionPage.resetModal()).isHidden(HIDDEN_OPTS);
    assertThat(editor.submitButton()).hasText(BTN_CREATE, TEXT_OPTS);
  }

  /**
   * {@code isAccessTokenRequiredOnNode} is true only for app-level nodes; seeds an app with
   * {@code null} token. Navigates via the owner summary page Source Control tile to exercise
   * the tile-click path from the manual regression step.
   *
   * <p>
   * Note: the frontend renders a single message ("Authentication method must be configured")
   * regardless of auth context; the manual-spec variant "Access Token must be configured" does
   * not exist in the current implementation.
   */
  @Test
  @Tag("regression")
  public void testAuthWarningBanner_shownWhenTokenNotConfigured() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(app.getId(), REPO_URL_WARN, null, SourceControlProvider.GITHUB);

    // Navigate to the app owner summary page, then click the Source Control tile.
    navigateAndWaitForUrl(OwnerSummaryPage.applicationUrl(app.getPublicId()), OwnerSummaryPage.APP_URL_FRAGMENT);
    new OwnerSummaryPage().clickSourceControlConfigurationLink();
    playwrightWaitUntilUrlContains(SourceControlConfigurationPage.URL_FRAGMENT);

    SourceControlRegressionPage regressionPage = new SourceControlRegressionPage();
    assertThat(regressionPage.tokenWarningAlert()).isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.tokenWarningAlert()).hasText(MSG_AUTH_NOT_CONFIGURED, TEXT_OPTS);
  }

  @Test
  @Tag("regression")
  public void testOrgSourceControl_inheritOverrideRadios_inheritedFieldsReadOnly() {
    // Seed root org with a non-GitHub provider so child can inherit a non-null parentValue.
    // GitLab is chosen because shouldShowGitHubAppAuth returns false for non-GitHub providers,
    // which means shouldShowTokenAuth = true and the Credentials Inherit/Override radios render.
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, TEST_TOKEN, SourceControlProvider.GITLAB);
    Organization childOrg = tempEntity.newOrganization();

    // Navigate via owner summary → Source Control tile click (covers manual steps 2–3).
    // Direct URL to org summary is used because the org has a dynamic seeded id that cannot
    // be located in the sidebar by name; the key UI interaction is the tile click.
    navigateAndWaitForUrl(OwnerSummaryPage.url(childOrg.getId()), OwnerSummaryPage.ORG_URL_FRAGMENT);
    new OwnerSummaryPage().clickSourceControlConfigurationLink();
    playwrightWaitUntilUrlContains(SourceControlConfigurationPage.URL_FRAGMENT);

    SourceControlRegressionPage regressionPage = new SourceControlRegressionPage();

    SourceControlConfigurationPage editor = new SourceControlConfigurationPage();

    // Provider field group: both radios visible; select is disabled while inheriting.
    assertThat(regressionPage.providerInheritRadio()).isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.providerOverrideRadio()).isVisible(VISIBLE_OPTS);
    assertThat(editor.providerSelect()).isDisabled(DISABLED_OPTS);

    // Credentials field group: radios and token input visible/disabled while inheriting.
    // Assert before clicking Provider Override — that click hides the Credentials radios
    // because provider.isInherited becomes false, making hasProviderSelected false.
    assertThat(regressionPage.credentialsInheritRadio()).isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.credentialsOverrideRadio()).isVisible(VISIBLE_OPTS);
    assertThat(editor.accessTokenInput()).isDisabled(DISABLED_OPTS);

    // Branch field group: both radios visible; branch input disabled while inheriting.
    assertThat(regressionPage.branchInheritRadio()).isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.branchOverrideRadio()).isVisible(VISIBLE_OPTS);
    assertThat(editor.defaultBranchInput()).isDisabled(DISABLED_OPTS);

    // Override Credentials → token input becomes editable.
    regressionPage.credentialsOverrideRadio().click();
    assertThat(editor.accessTokenInput()).isEnabled(ENABLED_OPTS);

    // Override Branch → branch input becomes editable.
    regressionPage.branchOverrideRadio().click();
    assertThat(editor.defaultBranchInput()).isEnabled(ENABLED_OPTS);

    // Override Provider → provider select becomes editable.
    regressionPage.providerOverrideRadio().click();
    assertThat(editor.providerSelect()).isEnabled(ENABLED_OPTS);
  }

  @Test
  @Tag("regression")
  public void testAppSourceControl_urlValidation_updateModal_testConfigButtonDisabledWhenDirty() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(app.getId(), REPO_URL, TEST_TOKEN, SourceControlProvider.GITHUB);

    navigateToAppSourceControl(app.getPublicId());

    SourceControlRegressionPage regressionPage = new SourceControlRegressionPage();

    // NxStatefulForm validates on submit: empty URL blocks save without opening the update-URL modal.
    assertThat(regressionPage.repoUrlInput()).isVisible(VISIBLE_OPTS);
    regressionPage.repoUrlInput().fill("");
    regressionPage.repoUrlInput().blur();

    SourceControlConfigurationPage editor = new SourceControlConfigurationPage();
    editor.submitButton().click();

    // Validation blocks submission — the update-URL confirmation modal must NOT appear.
    assertThat(regressionPage.updateUrlModal()).isHidden(HIDDEN_OPTS);

    // Re-enter a different URL and submit — modal opens (URL change requires confirmation).
    regressionPage.repoUrlInput().fill(REPO_URL_UPDATED);
    editor.submitButton().click();
    assertThat(regressionPage.updateUrlModal()).isVisible(VISIBLE_OPTS);

    // Cancel returns to the form without saving.
    regressionPage.updateUrlModalCancelButton().click();
    assertThat(regressionPage.updateUrlModal()).isHidden(HIDDEN_OPTS);

    // Confirm path: re-open the modal and click Continue — save completes.
    editor.submitButton().click();
    assertThat(regressionPage.updateUrlModal()).isVisible(VISIBLE_OPTS);
    regressionPage.updateUrlModalContinueButton().click();
    assertThat(regressionPage.updateUrlModal()).isHidden(HIDDEN_OPTS);

    // After save the form is no longer dirty — Test Config button becomes enabled.
    assertThat(regressionPage.testConfigButton()).isEnabled(ENABLED_OPTS);

    // Clicking Test Config shows the results section (mocked — embedded server has no SCM).
    regressionPage.mockSourceControlValidate();
    regressionPage.testConfigButton().click();
    assertThat(regressionPage.testConfigResultsSection()).isVisible(VISIBLE_OPTS);
  }

  /**
   * Features mocked to {@code []} — both notifications and automation absent, making
   * {@code isSourceControlForSourceTileSupported = false}.
   */
  @Test
  @Tag("regression")
  public void testSourceControlUnsupported_showsErrorAlert_noFormRendered() {
    SourceControlRegressionPage regressionPage = new SourceControlRegressionPage();
    regressionPage.mockProductFeatures();

    navigateToRootOrgSourceControl();
    page.reload();
    playwrightWaitUntilUrlContains(SourceControlConfigurationPage.URL_FRAGMENT);
    assertThat(regressionPage.unsupportedAlert()).isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.unsupportedAlert()).hasText(MSG_SC_UNSUPPORTED, TEXT_OPTS);

    // No form panels rendered — provider select, token input, branch input, and submit all absent.
    SourceControlConfigurationPage editor = new SourceControlConfigurationPage();
    assertThat(editor.providerSelect()).isHidden(HIDDEN_OPTS);
    assertThat(editor.accessTokenInput()).isHidden(HIDDEN_OPTS);
    assertThat(editor.defaultBranchInput()).isHidden(HIDDEN_OPTS);
    assertThat(editor.submitButton()).isHidden(HIDDEN_OPTS);
  }

  /** SC metrics endpoint mocked — the embedded server does not generate real PR execution history. */
  @Test
  @Tag("regression")
  public void testAppSourceControl_automatedPrTable_rendersWhenMetricsPresent() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(app.getId(), REPO_URL_PR, TEST_TOKEN, SourceControlProvider.GITHUB);

    SourceControlRegressionPage regressionPage = new SourceControlRegressionPage();
    regressionPage.mockSourceControlMetrics(PR_TITLE);

    navigateToAppSourceControl(app.getPublicId());

    assertThat(page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setName(PR_TABLE_HEADING)))
            .isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.automatedPrTable()).isVisible(VISIBLE_OPTS);
    assertThat(regressionPage.automatedPrTable().getByText(PR_TITLE)).isVisible(VISIBLE_OPTS);
  }
}
