/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.List;

import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.HeaderComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.pages.SourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlConfigurationPageAssertions;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

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

  @Before
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
      Assume.assumeTrue(featureLabel + " is not available for this license or configuration", false);
    }
  }

  /**
   * Owner summary smoke: open the Source Control nav pill and assert the tile (title, SCM subtitle,
   * Configuration heading, and edit link).
   */
  @Test
  @Category(SanityTest.class)
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
  @Category(SanityTest.class)
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
}
