/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import java.io.IOException;
import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ScmOnboardingPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

import com.codeborne.selenide.Selenide;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpHeaders;
import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.collect.ImmutableMap.of;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.service.InsightConfig.Feature.SCM_ONBOARDING;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;

public class ScmOnboardingTest
    extends AbstractFunctionalTest
{
  private static final String GITHUB_ROOT = "https\\:\\/\\/github\\.com\\/";

  private static final String CI_PROJECT_1_GIT = GITHUB_ROOT + "depshield-ci\\/ci-project-1";

  private static final String REPOSITORY_P_2_GIT = GITHUB_ROOT + "sonatype-nexus-community\\/nexus-repository-p2";

  private static final String REPOSITORY_PUPPET_GIT =
      GITHUB_ROOT + "sonatype-nexus-community\\/nexus-repository-puppet";

  private static final String REPOSITORY_TERRAFORM_GIT =
      GITHUB_ROOT + "sonatype-nexus-community\\/nexus-repository-terraform";

  private static final String REPOSITORY_VGO_GIT =
      GITHUB_ROOT + "sonatype-nexus-community\\/nexus-repository-vgo";

  private Organization org;

  @Rule
  public final WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @AfterClass
  public static void cleanup() {
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(SCM_ONBOARDING.getFlag(), false));
  }

  @After
  public void clearCookies() {
    Selenide.clearBrowserCookies();
  }

  @Before
  public void setup() {
    // given the feature flag is true
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(Feature.SCM_ONBOARDING.getFlag(), true));
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")));

    org = tempEntity.newOrganization("Test Org");
  }

  private void setupMockRepos() throws IOException {
    mockRepoForPage(0, getResourceAsString("/ScmOnboardingTest/allRepos0.json"));
    mockRepoForPage(1, getResourceAsString("/ScmOnboardingTest/emptyResponse.json"));
  }

  private void setupSourceControl() {
    PasswordHandler pwHandler = testCLMServer.getCLMServer().getInstance(PasswordHandler.class);
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN".toCharArray()));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, encryptedPwd, GITHUB);

    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(app.getId(), gitService.baseUrl() + "/org/repo.git", null);
    Application app2 = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(app2.getId(), gitService.baseUrl() + "/org/repo2.git", null);
  }

  private void mockRepoForPage(int page, String json) {
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .withQueryParam("per_page", equalTo("100"))
        .withQueryParam("page", equalTo(Integer.toString(page)))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(json)));
  }

  private String getResourceAsString(String filename) throws IOException {
    return IOUtil.toString(this.getClass().getResourceAsStream(filename));
  }

  @Test
  public void testFeatureIsDisabled() {
    // given the feature flag is false
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(SCM_ONBOARDING.getFlag(), false));
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    // when we open the onboarding page as admin
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // then the onboarding page is disabled
    scmOnboardingPage.loadError().shouldBe(visible).shouldHave(text("This feature has not been enabled"));
  }

  @Test
  public void testFeatureIsEnabled() {
    // given the onboarding page
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    PasswordHandler pwHandler = testCLMServer.getCLMServer().getInstance(PasswordHandler.class);
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN".toCharArray()));
    tempEntity.newSourceControl(org.getParentOwnerId(), null, encryptedPwd, GITHUB);

    // when we open the onboarding page as admin
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the feature flag error is hidden
    scmOnboardingPage.loadError().shouldBe(hidden);
  }

  @Test
  public void testFeatureIsNotAllowed() {
    // given the onboarding page and a new user
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    User user = tempEntity.newUser();

    // when we open the onboarding page as unprivileged user
    refreshOrOpen(ScmOnboardingPage.url());
    login(user.getUsername(), user.getPassword());

    // then a permission denied error is shown
    scmOnboardingPage.loadError().shouldBe(visible).shouldHave(text("you do not have permission to access this page"));

    // and form elements are hidden
    scmOnboardingPage.hostUrl().shouldBe(hidden);
    scmOnboardingPage.resultsTable().shouldBe(hidden);
  }

  @Test
  public void testGenericErrorsForwardedFromIq() {
    // given the onboarding page
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    // and an invalid configuration
    PasswordHandler pwHandler = testCLMServer.getCLMServer().getInstance(PasswordHandler.class);
    String encryptedPwd = new String(pwHandler.encryptPassword("".toCharArray()));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, encryptedPwd, GITHUB);

    // when we open the onboarding page as unprivileged user
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the message from IQ server is forwarded
    scmOnboardingPage.loadError().shouldHave(text("An error occurred loading data. Internal Server Error"));
  }

  @Test
  public void testScmDoesNotExist() {
    // given the onboarding page
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    // when we open the onboarding page as unprivileged user
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then a permission denied error is shown
    scmOnboardingPage.loadError().shouldHave(text("An error occurred loading data. The selected Organization does not" +
            " have SCM configured. You can configure it here. Retry"));

    // and form elements are hidden
    scmOnboardingPage.hostUrl().shouldBe(hidden);
    scmOnboardingPage.resultsTable().shouldBe(hidden);
  }

  @Test
  public void testPopulatesRepositories_inferredHostUrl() throws Exception {
    // given an SCM with git repos
    setupMockRepos();
    setupSourceControl();

    // given an existing app with a source control value configured
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(app.getId(), gitService.baseUrl() + "/org/repo.git", null);

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // then all repositories were loaded
    verifyAllReposLoaded(scmOnboardingPage);
  }

  @Test
  public void testPopulatesRepositories() throws Exception {
    // given an SCM with git repos
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // update the default host URL
    scmOnboardingPage.hostUrl().waitUntil(value(gitService.baseUrl()), 2000);
    updateHostUrl(scmOnboardingPage);
    scmOnboardingPage.reloadRepoButton().click();
    updateHostUrl(scmOnboardingPage);
    scmOnboardingPage.reloadRepoButton().waitUntil(enabled, 2000).click();

    // then all repositories were loaded
    verifyAllReposLoaded(scmOnboardingPage);

    scmOnboardingPage.resultsTablePercentageImported().shouldBe(text("0%"));
    scmOnboardingPage.resultsTableAlreadyImported().shouldBe(text("0"));
    
    // the long descriptions are trimmed
    assertThat(scmOnboardingPage.resultsTableDescription().get(0).getCssValue("text-overflow")).isEqualTo("ellipsis");

    // and there is a hover tooltip over the trimmed description
    scmOnboardingPage.resultsTableDescription().get(0).hover();
    String tooltipText = scmOnboardingPage.descriptionTooltip().text();
    assertThat(tooltipText.length() > 100).isTrue();
    assertThat(tooltipText).doesNotContain("...");

    Application application = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(application.getId(), "https://github.com/depshield-ci/ci-project-1.git", new Date());
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).doesNotContain("ci-project-1");
    scmOnboardingPage.resultsTablePercentageImported().shouldBe(text("8%"));
    scmOnboardingPage.resultsTableAlreadyImported().shouldBe(text("1"));
  }

  private void updateHostUrl(final ScmOnboardingPage scmOnboardingPage) {
    scmOnboardingPage.hostUrl().sendKeys(Keys.CONTROL, "a");
    scmOnboardingPage.hostUrl().sendKeys(Keys.BACK_SPACE);
    scmOnboardingPage.hostUrl().setValue(gitService.baseUrl());
    scmOnboardingPage.hostUrl().waitUntil(value(gitService.baseUrl()), 2000);
  }

  private void verifyAllReposLoaded(final ScmOnboardingPage scmOnboardingPage) {
    // then results are automatically displayed in the table
    scmOnboardingPage.loadingSpinner().waitWhile(visible, 5000);
    scmOnboardingPage.resultsTable().waitUntil(visible, 5000);
    scmOnboardingPage.repositoryCount().waitUntil(visible, 5000);
    scmOnboardingPage.repositoryCount().shouldBe(text("13"));
    scmOnboardingPage.selectedTotalCount().shouldBe(text("OF 13 REPOSITORIES"));
    scmOnboardingPage.resultsTableProject().shouldHaveSize(13);
    assertThat(scmOnboardingPage.resultsTableNamespace().texts()).containsAnyOf("depshield-ci",
        "sonatype-nexus-community");
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder("ci-project-1",
        "ci-project-16", "create-react-app", "nexus-repository-p2", "nexus-repository-puppet",
        "nexus-repository-terraform", "nexus-repository-vgo", "nexus-scripting-examples",
        "nexus-webhook-example-collection", "nxrm-cli", "ossindex-gradle-plugin", "oysteR", "prime-nexus-proxy-repos");
  }

  @Test
  public void testPageTitleElements() throws Exception {
    // given SCM onboarding page with a selected organization
    setupMockRepos();
    setupSourceControl();

    // and loading scm onboarding page with given org id
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the page title block is populated
    scmOnboardingPage.backButton().shouldBe(text("Back to Organization Management"));

    // NOTE the missing space before the org name is deliberate. In the UI there is an icon there with
    // appropriate margins.
    scmOnboardingPage.onboardingPageTitle().shouldBe(visible)
        .waitUntil(text("Import Applications toTest Org"), 5000);
  }

  @Test
  public void testSelection_all() throws Exception {
    // given an SCM with git repos
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // update the default host URL
    updateHostUrl(scmOnboardingPage);
    scmOnboardingPage.reloadRepoButton().waitUntil(enabled, 2000).click();

    // when select all is clicked
    scmOnboardingPage.repositoryCount().waitUntil(text("13"), 5000);
    scmOnboardingPage.resultsTableSelectAll().parent().waitUntil(visible, 5000);
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("13"));

    // when select all is clicked again (delected)
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("0"));
  }

  @Test
  public void testSelection_subset() throws Exception {
    // given an SCM with git repos
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // update the default host URL
    updateHostUrl(scmOnboardingPage);
    scmOnboardingPage.reloadRepoButton().waitUntil(enabled, 2000).click();

    // when select all is clicked while a filter is active
    scmOnboardingPage.resultsTableSelectAll().parent().waitUntil(visible, 5000);
    scmOnboardingPage.projectFilter().setValue("ci-");
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated with the number of filtered repositories
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("2"));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder("ci-project-1",
        "ci-project-16");

  }

  @Test
  public void testSelectAndImport() throws Exception {
    // given an SCM with git repos
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // update the default host URL
    updateHostUrl(scmOnboardingPage);
    scmOnboardingPage.reloadRepoButton().waitUntil(enabled, 2000).click();

    // when select all is clicked while a filter is active
    scmOnboardingPage.resultsTableSelectAll().parent().waitUntil(visible, 5000);
    scmOnboardingPage.projectFilter().setValue("ci-");
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated with the number of filtered repositories
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("2"));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder("ci-project-1",
        "ci-project-16");

    // when we import the selected repos
    scmOnboardingPage.importRepoButton().click();

    // then we see a success message
    scmOnboardingPage.successMessage().waitUntil(visible, 5000);
    scmOnboardingPage.successMessage().shouldBe(text(
        "2 repositories were successfully imported to IQ Server as applications under the Test Org Organization."));

    // and the imported count is incremented
    scmOnboardingPage.alreadyImportedCount().shouldBe(text("2"));

    // and the initially selected elements are no longer visible
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("0"));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).isEmpty();

    // and they are not there when the filter is updated
    scmOnboardingPage.projectFilter().sendKeys(Keys.CONTROL, "a");
    scmOnboardingPage.projectFilter().sendKeys(Keys.BACK_SPACE);
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.repositoryCount().shouldBe(text("11"));
    scmOnboardingPage.selectedTotalCount().shouldBe(text("OF 11 REPOSITORIES"));
    scmOnboardingPage.resultsTableProject().shouldHaveSize(11);
    assertThat(scmOnboardingPage.resultsTableNamespace().texts()).containsAnyOf("sonatype-nexus-community");
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder(
        "create-react-app", "nexus-repository-p2", "nexus-repository-puppet",
        "nexus-repository-terraform", "nexus-repository-vgo", "nexus-scripting-examples",
        "nexus-webhook-example-collection", "nxrm-cli", "ossindex-gradle-plugin", "oysteR", "prime-nexus-proxy-repos");
  }

  @Test
  public void testSelection_subset_replaces_previous_selection() throws Exception {
    // given an SCM with git repos
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // update the default host URL
    updateHostUrl(scmOnboardingPage);
    scmOnboardingPage.reloadRepoButton().waitUntil(enabled, 2000).click();

    // and given that select all is clicked while a filter is active
    scmOnboardingPage.resultsTableSelectAll().parent().waitUntil(visible, 5000);
    scmOnboardingPage.projectFilter().setValue("ci-");
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("2"));

    // when a new selection is made
    scmOnboardingPage.projectFilter().setValue("nexus");
    scmOnboardingPage.resultsTableSelectAll().parent().click(); // uncheck box
    scmOnboardingPage.resultsTableSelectAll().parent().click(); // check box
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("7"));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder(
        "nexus-repository-p2", "nexus-repository-puppet", "nexus-repository-terraform",
        "nexus-repository-vgo", "nexus-scripting-examples", "nexus-webhook-example-collection",
        "prime-nexus-proxy-repos");
  }

  @Test
  public void testSelection_filter_change_replaces_previous_selection() throws Exception {
    // given an SCM with git repos
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // update the default host URL
    updateHostUrl(scmOnboardingPage);
    scmOnboardingPage.reloadRepoButton().waitUntil(enabled, 2000).click();

    // and given that select all is clicked while a filter is active
    scmOnboardingPage.resultsTableSelectAll().parent().waitUntil(visible, 5000);
    scmOnboardingPage.projectFilter().setValue("nexus");
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("7"));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder(
        "nexus-repository-p2", "nexus-repository-puppet", "nexus-repository-terraform",
        "nexus-repository-vgo", "nexus-scripting-examples", "nexus-webhook-example-collection",
        "prime-nexus-proxy-repos");

    // when a new selection is made
    scmOnboardingPage.projectFilter().setValue("nexus-repository");

    // then the selected count is updated
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("4"));

    // and the result table contains exactly 4 projects
    assertThat(scmOnboardingPage.resultsTableProject().texts()).containsExactlyInAnyOrder(
        "nexus-repository-p2", "nexus-repository-puppet", "nexus-repository-terraform",
        "nexus-repository-vgo");

    // and the repositories checkboxes are selected
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_P_2_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_PUPPET_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_TERRAFORM_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_VGO_GIT).isSelected()).isTrue();

    // when the filter is changed
    scmOnboardingPage.projectFilter().setValue("i");

    // then the selections remain selected
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_P_2_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_PUPPET_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_TERRAFORM_GIT).isSelected()).isTrue();
    assertThat(scmOnboardingPage.selectionCheckboxById(REPOSITORY_VGO_GIT).isSelected()).isTrue();

    // and other repositories remain deselected
    assertThat(scmOnboardingPage.selectionCheckboxById(CI_PROJECT_1_GIT).isSelected()).isFalse();

    // and the selected count is unchanged
    scmOnboardingPage.selectedRepositoryCount().shouldBe(text("4"));
  }

  @Test
  public void testSelection_select_individual_row() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // update the default host URL
    updateHostUrl(scmOnboardingPage);
    scmOnboardingPage.reloadRepoButton().waitUntil(enabled, 2000).click();

    // when a repository is clicked
    scmOnboardingPage.resultsTableSelectAll().parent().waitUntil(visible, 5000);
    scmOnboardingPage.selectionCheckboxById(CI_PROJECT_1_GIT).parent().click();

    // then the checkbox is selected
    assertThat(scmOnboardingPage.selectionCheckboxById(CI_PROJECT_1_GIT).isSelected()).isTrue();
  }
}
