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
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
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

  private static final String PROVIDER_GITHUB = "GitHub";

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
   * "Add GitHub App" button is visible in the Authentication Method section when
   * GitHub App radio is selected and no apps have been registered yet.
   * <p>
   * Manual steps navigate via the sidebar tree; automated via direct URL since
   * root-org navigation does not require dynamic sidebar resolution.
   */
  @Test
  @Category(RegressionTest.class)
  public void testAddGitHubAppButton_visibleWhenNoAppsConfigured() {
    openSourceControlAtRoot();
    authPage.authTypeGitHubAppRadio().label().click();
    assertThat(authPage.addGitHubAppButton()).isVisible();
  }

  /**
   * Registration form required-field validation: after the org name is filled the
   * validation error clears, confirming the form accepts the required field.
   * <p>
   * Clicking "Register &amp; Create GitHub App" opens a real GitHub OAuth flow that cannot be
   * intercepted in the embedded test framework; the automatable assertion is that the
   * Organisation Name required-field error clears once a value is entered.
   */
  @Test
  @Category(RegressionTest.class)
  public void testRegistrationModal_orgNameFilledClearsValidationError() {
    openRegistrationModalAtRoot();
    authPage.registrationModalOrgAccountRadio().label().click();
    authAssertions.shouldShowOrgNameInput();
    authPage.registrationModalSubmitButton().click();
    authAssertions.shouldShowOrgNameValidationError();
    authPage.registrationModalOrgNameInput().fill("my-github-org");
    assertThat(authPage.registrationModalValidationError()).isHidden();
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
    Organization parentOrg = tempEntity.newOrganization();
    tempEntity.newSourceControl(
        parentOrg.getId(), null, GITHUB_PAT_PLACEHOLDER, SourceControlProvider.GITHUB);
    return tempEntity.newOrganization(parentOrg);
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

  /**
   * Switching auth method from PAT to GitHub App and saving persists the selection across
   * a page reload. Precondition: a GitHub App is seeded at root org so the form allows
   * saving with GitHub App selected; a PAT source-control record ensures the form loads
   * in "Update" mode with PAT as the initial active method.
   */
  @Test
  @Category(RegressionTest.class)
  public void testAuthMethodSwitch_patToGitHubApp_savesPersistsAfterReload() {
    // Seed a source control record with authenticationType=PAT explicitly so the form loads
    // with PAT as its initial auth method. Without this, the UI infers GitHub App from the
    // registered app even when a PAT token is stored, making the form non-dirty on load and
    // preventing a clean PAT→GitHub App switch.
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(Organization.ROOT_ORGANIZATION_ID)
        .setToken(GITHUB_PAT_PLACEHOLDER)
        .setProvider(SourceControlProvider.GITHUB)
        .setBaseBranch("master")
        .setAuthenticationType(SourceControl.AuthenticationType.PAT)
        .build();
    tempEntity.newSourceControl(sourceControl);
    tempEntity.newGitHubApp(Organization.ROOT_ORGANIZATION_ID);

    openSourceControlAtRoot();

    assertThat(authPage.authTypePatRadio().input()).isChecked();

    authPage.authTypeGitHubAppRadio().label().click();
    authAssertions.shouldShowGitHubAppSection();

    // Save and wait for the PUT sourceControl response to complete before reloading.
    SourceControlConfigurationPage editor = new SourceControlConfigurationPage();
    page.waitForResponse(
        r -> r.url().contains("/api/v2/sourceControl/")
            && "PUT".equalsIgnoreCase(r.request().method()),
        () -> editor.submitButton().click());

    // Reload without re-logging in (session is still active from the first login).
    playwrightRefreshOrOpen(OwnerSummaryPage.editOrganizationUrl(
        Organization.ROOT_ORGANIZATION_ID, SourceControlConfigurationPage.URL_FRAGMENT));
    new SourceControlConfigurationPage().selectProvider(PROVIDER_GITHUB);
    assertThat(authPage.authMethodSection()).isVisible();

    assertThat(authPage.authTypeGitHubAppRadio().input()).isChecked();
  }

  private void openRegistrationModalAtRoot() {
    playwrightRefreshOrOpen(OwnerSummaryPage.editOrganizationUrl(
        Organization.ROOT_ORGANIZATION_ID, SourceControlConfigurationPage.URL_FRAGMENT));
    playwrightLogin();
    new SourceControlConfigurationPage().selectProvider(PROVIDER_GITHUB);

    authPage.authTypeGitHubAppRadio().label().click();
    authPage.addGitHubAppButton().click();
    authAssertions.shouldShowRegistrationModal();
  }

  private void openSourceControlAtRoot() {
    playwrightRefreshOrOpen(OwnerSummaryPage.editOrganizationUrl(
        Organization.ROOT_ORGANIZATION_ID, SourceControlConfigurationPage.URL_FRAGMENT));
    playwrightLogin();
    new SourceControlConfigurationPage().selectProvider(PROVIDER_GITHUB);
    assertThat(authPage.authMethodSection()).isVisible();
  }

}
