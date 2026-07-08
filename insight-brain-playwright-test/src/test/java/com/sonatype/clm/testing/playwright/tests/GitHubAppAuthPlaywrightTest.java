/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.GitHubAppAuthPage;
import com.sonatype.clm.testing.playwright.pages.GitHubAppAuthPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlConfigurationPage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** GitHub App registration modal + auth-method switching on the SCM editor. */
public class GitHubAppAuthPlaywrightTest
    extends AbstractIqUiTest
{
  /** Dummy PAT for seed-only source-control records; no GitHub API calls are made. */
  private static final String GITHUB_PAT_PLACEHOLDER = "test-token-pw";

  private GitHubAppAuthPage authPage;

  private GitHubAppAuthPageAssertions authAssertions;

  @Before
  public void initPageObjects() {
    authPage = new GitHubAppAuthPage();
    authAssertions = new GitHubAppAuthPageAssertions(authPage);
  }

  /** Modal blocks at validation when Organization Account submits with empty org name. */
  @Test
  @Category(RegressionTest.class)
  public void testRegistrationModal_orgNameRequiredForOrganizationAccount() {
    openRegistrationModalAtRoot();
    authPage.registrationModalOrgAccountRadio().label().click();
    authAssertions.shouldShowOrgNameInput();

    authPage.registrationModalSubmitButton().click();

    authAssertions.shouldShowRegistrationModal();
    authAssertions.shouldShowOrgNameValidationError();
  }

  /** Selecting Personal Account hides the Organization Name input. */
  @Test
  @Category(RegressionTest.class)
  public void testRegistrationModal_orgNameHiddenForPersonalAccount() {
    openRegistrationModalAtRoot();
    authPage.registrationModalPersonalAccountRadio().label().click();
    authAssertions.shouldHideOrgNameInput();
  }

  /** Cancel on the registration modal dismisses it. */
  @Test
  @Category(RegressionTest.class)
  public void testRegistrationModal_cancelDismissesModal() {
    openRegistrationModalAtRoot();
    authPage.registrationModalCancelButton().click();
    assertThat(authPage.registrationModal()).isHidden();
  }

  /**
   * Child org renders Inherit/Override radios. Seeds GitHub on the parent so the child
   * inherits the provider ({@code GitHubAppAuthenticationMethod} only renders then).
   */
  @Test
  @Category(RegressionTest.class)
  public void testChildOrg_inheritanceRadiosShown_inheritSelectedByDefault() {
    Organization childOrg = createChildOrgInheritingGitHub();

    playwrightRefreshOrOpen(OwnerSummaryPage.editOrganizationUrl(
        childOrg.getId(), SourceControlConfigurationPage.URL_FRAGMENT));
    playwrightLogin();

    authAssertions.shouldShowInheritanceRadios();
    authAssertions.shouldHaveInheritRadioChecked();
  }

  /** Switching to Override + GitHub App reveals the GitHub App status section. */
  @Test
  @Category(RegressionTest.class)
  public void testChildOrg_switchToOverride_showsGitHubAppSection() {
    Organization childOrg = createChildOrgInheritingGitHub();

    playwrightRefreshOrOpen(OwnerSummaryPage.editOrganizationUrl(
        childOrg.getId(), SourceControlConfigurationPage.URL_FRAGMENT));
    playwrightLogin();

    // Wait for the inheritance radios to mount before interacting — the child-org SCM page
    // resolves provider/auth state asynchronously after navigation.
    authAssertions.shouldShowInheritanceRadios();

    authPage.overrideRadio().label().click();
    authPage.authTypeGitHubAppRadio().label().click();

    authAssertions.shouldShowGitHubAppSection();
  }

  /** Seeds a parent org with GitHub SCM configured, plus a child org that inherits from it. */
  private Organization createChildOrgInheritingGitHub() {
    String suffix = TemporaryEntity.uuid();
    Organization parentOrg = tempEntity.newOrganization("GitHubAppParent-" + suffix);
    tempEntity.newSourceControl(
        parentOrg.getId(), null, GITHUB_PAT_PLACEHOLDER, SourceControlProvider.GITHUB);
    return tempEntity.newOrganization("GitHubAppChild-" + suffix, parentOrg);
  }

  /** GitHub App → PAT reveals the token input. */
  @Test
  @Category(RegressionTest.class)
  public void testAuthMethodSwitch_gitHubAppToPat_showsTokenInput() {
    openSourceControlAtRoot();

    authPage.authTypeGitHubAppRadio().label().click();
    authAssertions.shouldHidePatTokenInput();

    authPage.authTypePatRadio().label().click();
    authAssertions.shouldShowPatTokenInput();
  }

  /** PAT → GitHub App hides the token input and shows the status section. */
  @Test
  @Category(RegressionTest.class)
  public void testAuthMethodSwitch_patToGitHubApp_hidesTokenInput() {
    openSourceControlAtRoot();

    authPage.authTypePatRadio().label().click();
    authAssertions.shouldShowPatTokenInput();

    authPage.authTypeGitHubAppRadio().label().click();
    authAssertions.shouldHidePatTokenInput();
    authAssertions.shouldShowGitHubAppSection();
  }

  private void openRegistrationModalAtRoot() {
    playwrightRefreshOrOpen(OwnerSummaryPage.editOrganizationUrl(
        Organization.ROOT_ORGANIZATION_ID, SourceControlConfigurationPage.URL_FRAGMENT));
    playwrightLogin();
    new SourceControlConfigurationPage().selectProvider("GitHub");

    authPage.authTypeGitHubAppRadio().label().click();
    authPage.addGitHubAppButton().click();
    authAssertions.shouldShowRegistrationModal();
  }

  private void openSourceControlAtRoot() {
    playwrightRefreshOrOpen(OwnerSummaryPage.editOrganizationUrl(
        Organization.ROOT_ORGANIZATION_ID, SourceControlConfigurationPage.URL_FRAGMENT));
    playwrightLogin();
    new SourceControlConfigurationPage().selectProvider("GitHub");
    assertThat(authPage.authMethodSection()).isVisible();
  }

}
