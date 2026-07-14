/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.GitHubAppAuthPage;
import com.sonatype.clm.testing.playwright.pages.ManageGitHubAppsPage;
import com.sonatype.clm.testing.playwright.pages.ManageGitHubAppsPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlRegressionPage;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Manage GitHub Applications admin page (ManageGitHubApps.jsx, /manage-github-apps). */
public class ManageGitHubAppsPlaywrightTest
    extends AbstractIqUiTest
{
  private Organization org;

  private GitHubApp seededApp;

  private ManageGitHubAppsPage manageAppsPage;

  private ManageGitHubAppsPageAssertions manageAppsAssertions;

  @Before
  public void seedAndOpen() {
    org = tempEntity.newOrganization();
    seededApp = tempEntity.newGitHubApp(org.getId());

    openManageGitHubAppsPage();

    manageAppsPage = new ManageGitHubAppsPage();
    manageAppsAssertions = new ManageGitHubAppsPageAssertions(manageAppsPage);
  }

  /** The seeded GitHub App appears in the list with its slug rendered in the row. */
  @Test
  @Category(RegressionTest.class)
  public void testListRendering_showsSeededApp() {
    manageAppsAssertions.shouldBeLoaded();
    manageAppsAssertions.shouldHaveAppCount(1);
    assertThat(manageAppsPage.rowForSlug(seededApp.getSlug())).isVisible();
  }

  /** Add GitHub App button opens the registration modal (same modal used elsewhere). */
  @Test
  @Category(RegressionTest.class)
  public void testAddApp_opensRegistrationForm() {
    manageAppsAssertions.shouldBeLoaded();
    manageAppsPage.addGitHubAppButton().click();

    GitHubAppAuthPage authPage = new GitHubAppAuthPage();
    assertThat(authPage.registrationModal()).isVisible();
  }

  @Test
  @Category(RegressionTest.class)
  public void testDeleteApp_clickDeleteOpensConfirmationModal() {
    manageAppsAssertions.shouldBeLoaded();
    manageAppsAssertions.shouldHaveAppCount(1);
    manageAppsPage.deleteButtonForApp(seededApp.getSlug()).click();
    manageAppsAssertions.shouldShowDeleteConfirmModal();
  }

  @Test
  @Category(RegressionTest.class)
  public void testDeleteApp_cancelLeavesAppInList() {
    manageAppsAssertions.shouldBeLoaded();
    manageAppsPage.deleteButtonForApp(seededApp.getSlug()).click();
    manageAppsAssertions.shouldShowDeleteConfirmModal();

    manageAppsPage.deleteCancelButton().click();
    manageAppsAssertions.shouldHideDeleteConfirmModal();
    manageAppsAssertions.shouldHaveAppCount(1);
  }

  @Test
  @Category(RegressionTest.class)
  public void testDeleteApp_confirmRemovesApp() {
    manageAppsAssertions.shouldBeLoaded();
    manageAppsPage.deleteButtonForApp(seededApp.getSlug()).click();
    manageAppsAssertions.shouldShowDeleteConfirmModal();

    manageAppsPage.deleteConfirmButton().click();
    manageAppsAssertions.shouldHideDeleteConfirmModal();
    manageAppsAssertions.shouldShowEmptyState();
  }

  /**
   * After the sole GitHub App is deleted, the Source Control Configuration page still offers
   * PAT as an accessible authentication method. No residual GitHub App config blocks switching to PAT.
   * <p>
   * Manual steps navigate via the sidebar tree; automated via direct URL since org-level SC
   * config does not require dynamic sidebar resolution.
   */
  @Test
  @Category(RegressionTest.class)
  public void testDeleteApp_afterConfirm_patAuthAccessibleInScConfig() {
    manageAppsAssertions.shouldBeLoaded();
    manageAppsPage.deleteButtonForApp(seededApp.getSlug()).click();
    manageAppsAssertions.shouldShowDeleteConfirmModal();
    manageAppsPage.deleteConfirmButton().click();
    manageAppsAssertions.shouldShowEmptyState();

    playwrightRefreshOrOpen(OwnerSummaryPage.editOrganizationUrl(
        org.getId(), SourceControlConfigurationPage.URL_FRAGMENT));

    SourceControlRegressionPage scRegressionPage = new SourceControlRegressionPage();
    scRegressionPage.providerOverrideRadio().click();

    SourceControlConfigurationPage scPage = new SourceControlConfigurationPage();
    scPage.selectGitHubPersonalAccessTokenCredentials();
    assertThat(scPage.accessTokenInput()).isVisible();
  }

  private void openManageGitHubAppsPage() {
    playwrightRefreshOrOpen(OwnerSummaryPage.editOrganizationUrl(
        org.getId(), ManageGitHubAppsPage.URL_FRAGMENT));
    playwrightLogin();
  }
}
