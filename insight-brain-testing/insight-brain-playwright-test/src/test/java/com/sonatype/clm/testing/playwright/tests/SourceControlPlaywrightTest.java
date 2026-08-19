/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.List;
import java.util.regex.Pattern;

import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.HeaderComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlConfigurationPageAssertions;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

/**
 * Source Control configuration smoke tests.
 *
 * <p>
 * Login + navigation are handled by {@link #openDashboardAndLoginAsAdmin()}; UI assertions live
 * on {@link OwnerSummaryPage} (tile) and {@link SourceControlConfigurationPage} (editor) per
 * {@code PLAYWRIGHT_TEST_AUTHORING_GUIDE.md} §4–§5.
 *
 */
public class SourceControlPlaywrightTest
    extends AbstractIqUiTest
{

  private record ScmToggle(String id, String title)
  {
  }

  private static final List<String> PROVIDER_LABELS =
      List.of("Azure DevOps", "Bitbucket", "GitHub", "GitLab");

  private static final List<ScmToggle> TOGGLES = List.of(
      new ScmToggle("source-control-ssh", "Use SSH for Git Operations"),
      new ScmToggle("source-control-remediation-pull-requests", "Automated Remediation with GoldenPRs\u2122"),
      new ScmToggle("inner-source-automated-updates", "Automated InnerSource Updates"),
      new ScmToggle("source-control-pull-request-commenting", "Pull Request Commenting"),
      new ScmToggle("source-control-evaluations", "Source Control Evaluations"),
      new ScmToggle("automated-commit-feedback", "Automated Commit Feedback"),
      new ScmToggle("manual-pull-requests", "Manual Pull Requests"));

  @BeforeEach
  public void openDashboardAndLoginAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  private void navigateToRootOrganizationSummary() {
    playwrightNavigateTo(OwnerSummaryPage.urlToRootOrg());
  }

  private void assertRootOrgOwnerSummaryVisible() {
    new HeaderComponentAssertions(new HeaderComponent()).shouldBeLoggedIn();
    new OwnerSummaryPageAssertions(new OwnerSummaryPage()).shouldBeVisible();
  }

  /**
   * Skips the test when the owner-summary nav pill is absent (feature/license not enabled). Pill
   * {@code data-testid} is {@code {pillTargetId}-button}.
   */
  private void assumeOwnerSummaryFeaturePillVisible(String pillTargetId, String featureLabel) {
    try {
      page.getByTestId(pillTargetId + "-button").waitFor();
    }
    catch (PlaywrightException e) {
      Assumptions.assumeTrue(false, featureLabel + " is not available for this license or configuration");
    }
  }

  /**
   * Owner summary smoke: open the Source Control nav pill and assert the tile (title, SCM subtitle,
   * Configuration heading, and edit link).
   */
  @Test
  @Tag("sanity")
  public void testOrgSourceControl() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();
    assumeOwnerSummaryFeaturePillVisible(OwnerSummaryPage.OWNER_PILL_SOURCE_CONTROL, "Source Control");

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_SOURCE_CONTROL);
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowSourceControlTile();
  }

  /**
   * Click the "Source Control not configured" summary link from the owner summary tile, then walk
   * the editor: heading, provider dropdown options, Access Token, Default Branch, every SCM toggle
   * (with copy + advanced Git options + InnerSource block), and the Create button.
   */
  @Test
  @Tag("sanity")
  public void testOrgSourceControlConfigurationLayout() {
    navigateToRootOrganizationSummary();
    playwrightWaitUntilUrlContains("/management/view/organization/");
    assertRootOrgOwnerSummaryVisible();
    assumeOwnerSummaryFeaturePillVisible(OwnerSummaryPage.OWNER_PILL_SOURCE_CONTROL, "Source Control");

    OwnerSummaryPage ownerSummary = new OwnerSummaryPage();
    ownerSummary.openOwnerSummarySectionFromNavPills(OwnerSummaryPage.OWNER_PILL_SOURCE_CONTROL);
    new OwnerSummaryPageAssertions(ownerSummary).shouldShowSourceControlTile();

    ownerSummary.sourceControlTile().getByRole(AriaRole.LINK).first().click();
    playwrightWaitUntilUrlContains(SourceControlConfigurationPage.URL_FRAGMENT);

    SourceControlConfigurationPage editor = new SourceControlConfigurationPage();
    SourceControlConfigurationPageAssertions editorAssertions = new SourceControlConfigurationPageAssertions(editor);
    editorAssertions.shouldShowEditorHeading();
    editorAssertions.shouldShowProviderDropdown();
    editorAssertions.shouldListProviderOptions(PROVIDER_LABELS);
    editor.selectGitHubPersonalAccessTokenCredentials();
    editorAssertions.shouldShowAccessTokenField();
    editorAssertions.shouldShowDefaultBranchField();
    for (ScmToggle toggle : TOGGLES) {
      editorAssertions.shouldShowToggle(toggle.id(), toggle.title());
    }
    editorAssertions.shouldShowAutomatedRemediationCopy();
    editorAssertions.shouldShowAdvancedGitOptions();
    editorAssertions.shouldShowCreateButton();
  }

  /**
   * When neither {@code notifications} nor {@code automation} is present in the
   * {@code GET /rest/product/features} response,
   * {@code selectIsSourceControlForSourceTileSupported} resolves to {@code false} and
   * {@code SourceControlConfiguration.jsx} renders
   * {@code NxErrorAlert id="source-control-not-supported"} instead of the SCM form.
   * <p>
   * A full page reload after registering the intercept resets the Redux store so
   * {@code fetchProductFeaturesIfNeeded} re-fires through the mock (the features may
   * already be cached from the {@code @Before} dashboard navigation).
   */
  @Test
  @Tag("regression")
  public void testSourceControlEditor_showsUnsupportedAlertWhenFeaturesAbsent() {
    try {
      page.route(Pattern.compile(".*/rest/product/features([?#][^/]*)?$"),
          route -> route.fulfill(new Route.FulfillOptions()
              .setStatus(200)
              .setContentType("application/json")
              .setBody("[]")));
      navigateAndWaitForUrl(
          OwnerSummaryPage.editOrganizationUrl(ROOT_ORGANIZATION_ID,
              SourceControlConfigurationPage.URL_FRAGMENT),
          SourceControlConfigurationPage.URL_FRAGMENT);

      // Full reload clears the Redux store cache; fetchProductFeaturesIfNeeded re-fires
      // through the intercepted route and the license gate activates.
      page.reload();

      SourceControlConfigurationPage scPage = new SourceControlConfigurationPage();
      new SourceControlConfigurationPageAssertions(scPage).shouldShowUnsupportedAlert();
    }
    finally {
      page.unrouteAll();
    }
  }
}
