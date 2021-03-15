/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.ScmOnboardingPage;
import com.sonatype.clm.testing.functional.pages.ScmOnboardingPage.OrganizationsDropdownMenu;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.codehaus.plexus.util.IOUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Condition.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.collect.ImmutableMap.of;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.service.InsightConfig.Feature.SCM_ONBOARDING;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ScmOnboardingTest
    extends AbstractFunctionalTest
{
  private static final String SCM_ROOT = "https\\:\\/\\/localhost\\/";

  private static final String CI_PROJECT_1_GIT = SCM_ROOT + "depshield-ci\\/ci-project-1";

  private static final String REPOSITORY_P_2_GIT = SCM_ROOT + "sonatype-nexus-community\\/nexus-repository-p2";

  private static final String REPOSITORY_PUPPET_GIT =
      SCM_ROOT + "sonatype-nexus-community\\/nexus-repository-puppet";

  private static final String REPOSITORY_TERRAFORM_GIT =
      SCM_ROOT + "sonatype-nexus-community\\/nexus-repository-terraform";

  private static final String REPOSITORY_VGO_GIT =
      SCM_ROOT + "sonatype-nexus-community\\/nexus-repository-vgo";

  public static final String EMPTY_JSON_ARRAY = "[]";

  private PasswordHandler pwHandler;

  private Organization org;

  @Rule
  public final WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Rule
  public final WireMockRule secondaryGitService = new WireMockRule(wireMockConfig().dynamicPort());

  @AfterClass
  public static void cleanup() {
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(SCM_ONBOARDING.getFlag(), false));
  }

  @After
  public void clearCookies() {
    Selenide.clearBrowserCookies();
    OrganizationDAO organizationDAO = new OrganizationDAO();
    organizationDAO.getByNames(singleton("Foo Organization")).forEach(organizationDAO::delete);
  }

  @Before
  public void setup() {
    pwHandler = testCLMServer.getCLMServer().getInstance(PasswordHandler.class);
    // given the feature flag is true
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(Feature.SCM_ONBOARDING.getFlag(), true));
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")));
    secondaryGitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")));

    org = tempEntity.newOrganization("Test Org");
  }

  private void setupMockRepos() throws IOException {
    mockRepoForPage(0, getResourceAsString("/ScmOnboardingTest/allRepos0.json"));
    mockRepoForPage(1, EMPTY_JSON_ARRAY);
  }

  private void setupSourceControl() {
    setupOrgSourceControl();
    setupAppSourceControl("/org/repo.git");
    setupAppSourceControl("/org/repo2.git");
  }

  private void setupAppSourceControl(final String repoUrl) {
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(app.getId(), gitService.baseUrl() + repoUrl, null);
  }

  private void setupOrgSourceControl() {
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN".toCharArray()));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, encryptedPwd, GITHUB);
  }

  private void mockRepoForPage(int page, String json) {
    mockRepoForPage(gitService, page, json);
  }

  private void mockRepoForPage(WireMockRule service, int page, String json) {
    service.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
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

    // and no page titles are visible
    scmOnboardingPage.getPageTitleElements().shouldHaveSize(0);
  }

  @Test
  public void testFeatureIsDisabled_WithOrg() {
    // given the feature flag is false and source control is configured
    setupSourceControl();
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(of(SCM_ONBOARDING.getFlag(), false));
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    // when we open the onboarding page as admin
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // then the onboarding page is disabled
    scmOnboardingPage.loadError().shouldBe(visible).shouldHave(text("This feature has not been enabled"));

    // and no page titles are visible
    scmOnboardingPage.getPageTitleElements().shouldHaveSize(0);
  }

  @Test
  public void testFeatureIsEnabled() throws IOException {
    // given the onboarding page
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    setupSourceControl();
    setupMockRepos();

    // when we open the onboarding page as admin
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the feature flag error is hidden
    scmOnboardingPage.loadError().shouldBe(hidden);

    // and page titles are visible
    scmOnboardingPage.getPageTitleElements().shouldHaveSize(1);
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

    // then org selection is visible
    scmOnboardingPage.organizationsDropdown().shouldBe(visible);

    // then the message from IQ server is forwarded
    scmOnboardingPage.loadError()
        .shouldHave(text("An error occurred loading data. Request failed with status code 500"));
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
  public void testPopulatesRepositories_scmAuthenticationFailure() throws Exception {
    // given an SCM with authentication failure
    setupSourceControl();
    StubMapping stubMapping = gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)));

    // when the scm onboarding page is opened
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // then an authentication error is displayed
    scmOnboardingPage.gitHostError().shouldHave(text("Authentication with GitHub failed. " +
            "You can update your login credentials here."));

    // when authentication is fixed and retry button is pressed
    removeStub(stubMapping);
    setupMockRepos();

    scmOnboardingPage.hostUrlContinueButton().click();

    // page is rendered without error
    scmOnboardingPage.resultsTable().shouldBe(visible);
    scmOnboardingPage.loadError().shouldBe(hidden);
  }

  @Test
  public void testPopulatesRepositories_scmAuthorizationFailure() throws Exception {
    // given an SCM with authentication failure
    setupSourceControl();
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_FORBIDDEN)));

    // when the scm onboarding page is opened
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // then an authorization error is displayed
    scmOnboardingPage.loadError().shouldHave(text("An error occurred loading data. Permission denied by GitHub."));
    scmOnboardingPage.gitHostError().shouldHave(text("Permission denied by GitHub. " +
        "You can update your login credentials here."));
  }

  @Test
  public void testGitHost_afterAuthFailure() throws Exception {
    // given an org with no apps
    setupOrgSourceControl();

    // given attempts to load repos results in an auth failure
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)));

    // given an org that overrides the token
    Organization orgCustomToken = tempEntity.newOrganization("Custom Token");
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN2".toCharArray()));
    tempEntity.newSourceControl(orgCustomToken.getId(), null, encryptedPwd, null);

    // when the scm onboarding page is opened to the org with the custom token
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url() + "/" + orgCustomToken.getId());
    loginAsAdmin();

    // then the git host dialog is loaded
    scmOnboardingPage.modalDialog().shouldBe(visible);
    scmOnboardingPage.gitHostError().shouldHave(text("IQ Server was unable to identify the URL for your GitHub host."));

    // when we update the URL and attempt to load
    scmOnboardingPage.hostUrl().setValue(gitService.baseUrl());
    scmOnboardingPage.hostUrl().shouldHave(value(gitService.baseUrl()));
    scmOnboardingPage.hostUrlInvalidMessage().shouldNotBe(visible);
    scmOnboardingPage.hostUrlContinueButton().shouldBe(enabled).click();

    // then an authorization error is displayed
    scmOnboardingPage.modalDialog().shouldBe(visible);
    scmOnboardingPage.gitHostError().shouldHave(text("Authentication with GitHub failed."));

    // when we cancel and switch to a new org
    scmOnboardingPage.hostUrlCancelButton().click();
    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownItems().find(exactText("Test Org")).click();

    // then we should get a git host dialog prompting us for the git host URL
    scmOnboardingPage.modalDialog().shouldBe(visible);
    scmOnboardingPage.gitHostError().shouldHave(text("IQ Server was unable to identify the URL for your GitHub host."));
  }

  @Test
  public void testPopulatesRepositories_scmUnableToConnect() throws Exception {
    // given an endpoint which does not respond
    setupSourceControl();
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    // when the scm onboarding page is opened
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // then an error indicating a connection failure is shown
    scmOnboardingPage.loadError()
        .shouldHave(text("An error occurred loading data. Request failed with status code 500"));
    scmOnboardingPage.loadError().shouldHave(text("Click here to change the git host URL."));
    scmOnboardingPage.gitHostError().shouldNotBe(visible);
  }

  @Test
  public void testPopulatesRepositories_inferredHostUrl() throws Exception {
    // given an SCM with git repos
    setupMockRepos();
    setupSourceControl();

    // given an existing app with a source control value configured
    setupAppSourceControl("/org/repo.git");

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

    // then all repositories were loaded
    verifyAllReposLoaded(scmOnboardingPage);

    scmOnboardingPage.donutChartPercentImported().shouldHave(attribute("aria-label", "0% imported"));
    scmOnboardingPage.resultsTableAlreadyImported().shouldBe(text("0"));

    // the long descriptions are trimmed
    scmOnboardingPage.resultsTableDescription().get(0).shouldHave(cssValue("text-overflow", "ellipsis"));

    // and there is a hover tooltip over the trimmed description
    scmOnboardingPage.resultsTableDescription().get(0).hover();
    scmOnboardingPage.descriptionTooltip().should(matchText(".{101,}")).shouldNotHave(text("..."));

    // when the application already exists in IQ
    Application application = tempEntity.newApplication(org.getId());
    tempEntity.newSourceControl(application.getId(), "https://localhost/depshield-ci/ci-project-1.git", new Date());
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));

    // it is no longer displayed in the table and the UI is updated
    scmOnboardingPage.resultsTableProject().shouldHave(sizeGreaterThan(0));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).doesNotContain("ci-project-1");
    scmOnboardingPage.donutChartPercentImported().shouldHave(attribute("aria-label", "8% imported"));
    scmOnboardingPage.resultsTableAlreadyImported().shouldBe(text("1"));
  }

  @Test
  public void testPopulatesRepositories_noAvailableRepositories() throws Exception {
    // given an SCM without git repos
    mockRepoForPage(0, EMPTY_JSON_ARRAY);
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the table is empty
    scmOnboardingPage.repositoryCount().waitUntil(text("0"), 2000);

    // the statistics are shown and indicate no available repositories (and none already imported)
    scmOnboardingPage.donutChartPercentImported().shouldHave(attribute("aria-label", "0% imported"));
    scmOnboardingPage.resultsTableAlreadyImported().shouldBe(text("0"));
  }

  @Test
  public void testPopulatesRepositories_allRepositoriesAlreadyImported() throws Exception {
    // given an SCM with a single, already imported, git repo
    ObjectMapper mapper = new ObjectMapper();
    String repoUrl = "/org/repo.git";
    String cloneUrl = gitService.baseUrl() + repoUrl;
    String json = mapper.writeValueAsString(
        Arrays.asList(of(
            "name", "test",
            "description", "",
            "private", false,
            "clone_url", cloneUrl)
        )
    );
    mockRepoForPage(0, json);
    mockRepoForPage(1, EMPTY_JSON_ARRAY);
    setupOrgSourceControl();
    setupAppSourceControl(repoUrl);

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the table is empty
    scmOnboardingPage.repositoryCount().shouldBe(text("0"));

    // the statistics are shown and indicate no available repositories (and one already imported)
    scmOnboardingPage.donutChartPercentImported().shouldHave(attribute("aria-label", "100% imported"));
    scmOnboardingPage.resultsTableAlreadyImported().shouldBe(text("1"));
  }

  private void verifyAllReposLoaded(final ScmOnboardingPage scmOnboardingPage) {
    // then results are automatically displayed in the table
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);
    scmOnboardingPage.resultsTable().shouldBe(visible);
    scmOnboardingPage.repositoryCount().shouldBe(visible);
    scmOnboardingPage.selectedToImportCount().shouldBe(text("0 of 13 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(exactTexts("ci-project-1",
        "ci-project-16", "create-react-app", "nexus-repository-p2", "nexus-repository-puppet",
        "nexus-repository-terraform", "nexus-repository-vgo", "nexus-scripting-examples",
        "nexus-webhook-example-collection", "nxrm-cli", "ossindex-gradle-plugin", "oysteR", "prime-nexus-proxy-repos"));
    assertThat(scmOnboardingPage.resultsTableNamespace().texts()).containsAnyOf("depshield-ci",
        "sonatype-nexus-community");
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
        .shouldBe(text("Import Applications from GitHub"));
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

    // when select all is clicked
    scmOnboardingPage.repositoryCount().shouldBe(text("13"));
    scmOnboardingPage.resultsTableSelectAll().parent().shouldBe(visible);
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated
    scmOnboardingPage.selectedToImportCount().shouldBe(text("13 of 13 repositories"));

    // when select all is clicked again (delected)
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated
    scmOnboardingPage.selectedToImportCount().shouldBe(text("0"));
  }

  @Test
  public void testSelection_resetAfterPageFlip() throws Exception {
    // given an SCM with enough git repos to require pagination
    mockRepoForPage(0, getResourceAsString("/ScmOnboardingTest/mixedOrderRepos.json"));
    mockRepoForPage(1, EMPTY_JSON_ARRAY);
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // when select all is clicked
    scmOnboardingPage.repositoryCount().shouldBe(text("20"));
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated to be the max # of repos/page, not all repos
    scmOnboardingPage.selectedToImportCount().shouldBe(text("15"));

    // when we flip pages
    scmOnboardingPage.paginationButtons().get(1).click();

    // then selections are reset
    scmOnboardingPage.selectedToImportCount().shouldBe(text("0"));

    // and the Select All state is reset
    scmOnboardingPage.resultsTableSelectAll().shouldNotBe(checked);
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

    // when select all is clicked while a filter is active
    scmOnboardingPage.resultsTableSelectAll().parent().shouldBe(visible);
    scmOnboardingPage.projectFilter().setValue("ci-");
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated with the number of filtered repositories
    scmOnboardingPage.selectedToImportCount().shouldBe(text("2 of 13 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(exactTexts("ci-project-1",
        "ci-project-16"));

  }

  @Test
  public void testSelectAndImport_success() throws Exception {
    // given an SCM with git repos
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // when select all is clicked while a filter is active
    scmOnboardingPage.resultsTableSelectAll().parent().shouldBe(visible);
    scmOnboardingPage.projectFilter().setValue("ci-");
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated with the number of filtered repositories
    scmOnboardingPage.selectedToImportCount().shouldBe(text("2  of 13 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(CollectionCondition.texts("ci-project-1",
        "ci-project-16"));

    // when we import the selected repos
    scmOnboardingPage.importRepoButton().click();

    // then we see a success message in the status dialog
    scmOnboardingPage.importStatusModal().shouldBe(visible);
    scmOnboardingPage.successMessage().shouldBe(visible);
    scmOnboardingPage.errorMessage().shouldBe(hidden);
    scmOnboardingPage.successMessage().shouldBe(text(
        "All repositories were successfully imported. See details below."));

    // and can dismiss the dialog
    scmOnboardingPage.importStatusContinue().click();
    scmOnboardingPage.importStatusModal().shouldBe(hidden);

    // and the imported count is incremented
    scmOnboardingPage.alreadyImportedCount().shouldBe(text("2"));

    // and the initially selected elements are no longer visible
    scmOnboardingPage.selectedToImportCount().shouldBe(text("0  of 11 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(CollectionCondition.size(0));

    // and they are not there when the filter is updated
    scmOnboardingPage.projectFilter().sendKeys(IntStream.range(0, 3)
        .mapToObj(i -> Keys.BACK_SPACE).toArray(CharSequence[]::new));
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.repositoryCount().shouldBe(text("11"));
    scmOnboardingPage.selectedToImportCount().shouldBe(text("11 of 11 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(CollectionCondition.textsInAnyOrder(
        "create-react-app", "nexus-repository-p2", "nexus-repository-puppet",
        "nexus-repository-terraform", "nexus-repository-vgo", "nexus-scripting-examples",
        "nexus-webhook-example-collection", "nxrm-cli", "ossindex-gradle-plugin", "oysteR", "prime-nexus-proxy-repos"));
    assertThat(scmOnboardingPage.resultsTableNamespace().texts()).containsAnyOf("sonatype-nexus-community");

    // and the select all checkbox is checked
    scmOnboardingPage.resultsTableSelectAll().shouldBe(checked);
  }

  @Test
  public void testSelectAndImport_error() throws Exception {
    // given an SCM with git repos but with bad URLs
    mockRepoForPage(0, getResourceAsString("/ScmOnboardingTest/reposWithErrors.json"));
    mockRepoForPage(1, EMPTY_JSON_ARRAY);
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // select project which will fail import
    scmOnboardingPage.resultsTableSelectAll().parent().shouldBe(visible);
    scmOnboardingPage.projectFilter().setValue("broken");
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated with the number of filtered repositories
    scmOnboardingPage.selectedToImportCount().shouldBe(text("2 of 3 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(CollectionCondition.textsInAnyOrder("broken-url-1",
        "broken-url-2"));

    // when we import the selected repos
    scmOnboardingPage.importRepoButton().click();

    // then we see an error message
    scmOnboardingPage.successMessage().shouldNotBe(visible);
    scmOnboardingPage.errorMessage().shouldBe(text("2 repositories had an error. See details below."));

    // and can dismiss the dialog
    scmOnboardingPage.importStatusContinue().click();
    scmOnboardingPage.importStatusModal().shouldBe(hidden);

    // and the imported count is unchanged
    scmOnboardingPage.alreadyImportedCount().shouldBe(text("0"));

    // and the initially selected elements are still visible
    scmOnboardingPage.resultsTableProject().shouldHave(CollectionCondition.texts("broken-url-1",
        "broken-url-2"));

    // and the select all checkbox is unchecked
    scmOnboardingPage.resultsTableSelectAll().shouldNotBe(checked);
  }

  @Test
  public void testSelectAndImport_successAndError() throws Exception {
    // given an SCM with git repos but with bad URLs
    mockRepoForPage(0, getResourceAsString("/ScmOnboardingTest/reposWithErrors.json"));
    mockRepoForPage(1, EMPTY_JSON_ARRAY);
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // select all projects, a mix of good & bad
    scmOnboardingPage.resultsTableSelectAll().parent().shouldBe(visible);
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.selectedToImportCount().shouldBe(text("3 of 3 repositories"));

    // when we import the selected repos
    scmOnboardingPage.importRepoButton().click();

    // then we see an import message
    scmOnboardingPage.importStatusModal().shouldBe(visible);
    scmOnboardingPage.errorMessage().shouldBe(text("2 repositories had an error. See details below."));
    scmOnboardingPage.importSuccessDetailMsg().shouldBe(text("1 repository was successfully imported to IQ Server " +
            "as applications under the Test Org Organization."));
    scmOnboardingPage.importErrorDetailMsg().shouldBe(text("2 repositories had an error"));
    scmOnboardingPage.importErrorDetails().shouldHave(exactTexts(
        "org2/broken-url-1 failed with Unsupported repository URL format: `h://localhost/org2/broken-url-1.git`",
        "org2/broken-url-2 failed with Unsupported repository URL format: `ht://host/org2/broken-url-2.git`"
    ));

    // and can dismiss the dialog
    scmOnboardingPage.importStatusContinue().click();
    scmOnboardingPage.importStatusModal().shouldBe(hidden);

    // and the imported count is incremented
    scmOnboardingPage.alreadyImportedCount().shouldBe(text("1"));

    // and the broken elements are still visible
    scmOnboardingPage.resultsTableProject().shouldHave(CollectionCondition.texts("broken-url-1",
        "broken-url-2"));

    // and the select all checkbox is unchecked
    scmOnboardingPage.resultsTableSelectAll().shouldNotBe(checked);
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

    // and given that select all is clicked while a filter is active
    scmOnboardingPage.resultsTableSelectAll().parent().shouldBe(visible);
    scmOnboardingPage.projectFilter().setValue("ci-");
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.selectedToImportCount().shouldBe(text("2 of 13 repositories"));

    // when a new selection is made
    scmOnboardingPage.projectFilter().setValue("nexus");
    scmOnboardingPage.resultsTableSelectAll().parent().click(); // uncheck box
    scmOnboardingPage.resultsTableSelectAll().parent().click(); // check box
    scmOnboardingPage.selectedToImportCount().shouldBe(text("7 of 13 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(CollectionCondition.texts(
        "nexus-repository-p2", "nexus-repository-puppet", "nexus-repository-terraform",
        "nexus-repository-vgo", "nexus-scripting-examples", "nexus-webhook-example-collection",
        "prime-nexus-proxy-repos"));
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

    // and given that select all is clicked while a filter is active
    scmOnboardingPage.resultsTableSelectAll().parent().shouldBe(visible);
    scmOnboardingPage.projectFilter().setValue("nexus");
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.selectedToImportCount().shouldBe(text("7 of 13 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(CollectionCondition.texts(
        "nexus-repository-p2", "nexus-repository-puppet", "nexus-repository-terraform",
        "nexus-repository-vgo", "nexus-scripting-examples", "nexus-webhook-example-collection",
        "prime-nexus-proxy-repos"));

    // when a new selection is made
    scmOnboardingPage.projectFilter().setValue("nexus-repository");

    // then the selected count is updated
    scmOnboardingPage.selectedToImportCount().shouldBe(text("4 of 13 repositories"));

    // and the result table contains exactly 4 projects
    scmOnboardingPage.resultsTableProject().shouldHave(CollectionCondition.texts(
        "nexus-repository-p2", "nexus-repository-puppet", "nexus-repository-terraform",
        "nexus-repository-vgo"));

    // and the repositories checkboxes are selected
    scmOnboardingPage.selectionCheckboxById(REPOSITORY_P_2_GIT).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(REPOSITORY_PUPPET_GIT).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(REPOSITORY_TERRAFORM_GIT).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(REPOSITORY_VGO_GIT).shouldBe(selected);

    // when the filter is changed
    scmOnboardingPage.projectFilter().setValue("i");

    // then the selections remain selected
    scmOnboardingPage.selectionCheckboxById(REPOSITORY_P_2_GIT).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(REPOSITORY_PUPPET_GIT).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(REPOSITORY_TERRAFORM_GIT).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(REPOSITORY_VGO_GIT).shouldBe(selected);

    // and other repositories remain deselected
    scmOnboardingPage.selectionCheckboxById(CI_PROJECT_1_GIT).shouldNotBe(selected);

    // and the selected count is unchanged
    scmOnboardingPage.selectedToImportCount().shouldBe(text("4 of 13 repositories"));
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

    // when a repository is clicked
    scmOnboardingPage.resultsTableSelectAll().parent().shouldBe(visible);
    scmOnboardingPage.selectionCheckboxById(CI_PROJECT_1_GIT).parent().click();

    // then the checkbox is selected
    scmOnboardingPage.selectionCheckboxById(CI_PROJECT_1_GIT).shouldBe(selected);
  }

  @Test
  public void testSort() throws Exception {
    // given an SCM with git repos that start unsorted
    setupSourceControl();
    mockRepoForPage(0, getResourceAsString("/ScmOnboardingTest/mixedOrderRepos.json"));
    mockRepoForPage(1, EMPTY_JSON_ARRAY);

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the repos are initially sorted by namespace first and project second
    scmOnboardingPage.namespaceHeader().shouldHave(attribute("aria-sort", "ascending"));
    scmOnboardingPage.resultsTableNamespace().shouldHave(sizeGreaterThan(0));
    List<String> namespaceTexts = scmOnboardingPage.resultsTableNamespace().texts();
    assertThat(namespaceTexts).isSorted();

    // and project are sorted within their namespace
    scmOnboardingPage.resultsTableProject().shouldHave(exactTexts(
        // org-1
        "dupe-prj", "prj-1", "prj-2", "prj-3",
        // org-2
        "dupe-prj", "name-7", "name-8", "name-9",
        // org-3
        "a", "b", "c", "d", "dupe-prj", "e", "f"));

    // when namespace is clicked
    scmOnboardingPage.namespaceHeaderSort().click();

    // then UI shows the sort order changed
    scmOnboardingPage.namespaceHeader().shouldHave(attribute("aria-sort", "descending"));

    // and the sort is reversed
    namespaceTexts = scmOnboardingPage.resultsTableNamespace().texts();
    assertThat(namespaceTexts).isSortedAccordingTo(Comparator.reverseOrder());

    // and project keeps the same order within their project
    scmOnboardingPage.resultsTableProject().shouldHave(exactTexts(
        // org-3
        "a", "b", "c", "d", "dupe-prj", "e", "f", "g", "m", "n", "v", "z",
        // org-2
        "dupe-prj", "name-7", "name-8"));

    // when project is clicked
    scmOnboardingPage.projectHeader().click();

    // then it is sorted descending
    List<String> projectTexts = scmOnboardingPage.resultsTableProject().texts();
    assertThat(projectTexts).isSorted();

    // when project is clicked again
    scmOnboardingPage.projectHeader().click();

    // then sort order is reversed
    projectTexts = scmOnboardingPage.resultsTableProject().texts();
    assertThat(projectTexts).isSortedAccordingTo(Comparator.reverseOrder());

    // when description is clicked
    scmOnboardingPage.descriptionHeader().click();

    // then it is sorted
    List<String> descriptionTexts = scmOnboardingPage.resultsTableDescription().texts();
    assertThat(descriptionTexts).isSorted();

    // when description is clicked again
    scmOnboardingPage.descriptionHeader().click();

    // then it is sorted ascending
    descriptionTexts = scmOnboardingPage.resultsTableDescription().texts();
    assertThat(descriptionTexts).isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  public void testValidation() throws Exception {
    // given a mock git service
    setupMockRepos();

    // given org has SC setup but no apps created, so no default host URL
    setupOrgSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // empty field
    scmOnboardingPage.hostUrl().clear();
    scmOnboardingPage.hostUrlInvalidMessage().shouldBe(hidden);

    // invalid data
    scmOnboardingPage.hostUrl().setValue("f");
    scmOnboardingPage.hostUrlInvalidMessage().shouldBe(text("Not a valid URL"));

    // invalid protocol
    scmOnboardingPage.hostUrl().setValue("tp://h");
    scmOnboardingPage.hostUrlInvalidMessage().shouldBe(text("Protocol must be http or https"));

    // valid URL
    scmOnboardingPage.hostUrl().setValue("http://host");
    scmOnboardingPage.hostUrlInvalidMessage().shouldBe(hidden);

    // server side validation (server calls are async so wait is needed)
    scmOnboardingPage.hostUrl().sendKeys(Keys.SPACE);
    scmOnboardingPage.hostUrlInvalidMessage().shouldBe(hidden);
    scmOnboardingPage.hostUrlInvalidMessage().shouldBe(text("Unable to parse repository URL: " +
        "java.net.URISyntaxException: Illegal character in authority at index 7: http://host"));
  }

  @Test
  public void testPagination() throws Exception {
    // given an SCM with git repos that are unsorted initially
    setupSourceControl();
    mockRepoForPage(0, getResourceAsString("/ScmOnboardingTest/mixedOrderRepos.json"));
    mockRepoForPage(1, EMPTY_JSON_ARRAY);

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the repos list loads and has the max page size
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);
    scmOnboardingPage.resultsTableProject().shouldHaveSize(15);

    // and pagination buttons are present
    scmOnboardingPage.paginationButtons().shouldHaveSize(2);

    // and be on the first page
    scmOnboardingPage.paginationButtons().get(0).shouldHave(cssClass("selected"));

    // when the second pagination button is clicked
    scmOnboardingPage.paginationButtons().get(1).click();

    // then the second page of results appears
    scmOnboardingPage.resultsTableProject().shouldHaveSize(5);
    scmOnboardingPage.paginationButtons().get(1).shouldHave(cssClass("selected"));
  }

  @Test
  public void testFiltersAndPagination() throws Exception {
    // given an SCM with git repos that are unsorted initially
    setupSourceControl();
    mockRepoForPage(0, getResourceAsString("/ScmOnboardingTest/mixedOrderRepos.json"));
    mockRepoForPage(1, EMPTY_JSON_ARRAY);

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the pagination buttons shows the first is selected
    scmOnboardingPage.paginationButtons().get(0).shouldHave(cssClass("selected"));

    // when the second pagination button is clicked
    scmOnboardingPage.paginationButtons().get(1).click();

    // then the second shows as selected
    scmOnboardingPage.paginationButtons().get(1).shouldHave(cssClass("selected"));

    // when the filters are updated
    scmOnboardingPage.projectFilter().setValue("p");

    // then the page is reset to the first one
    scmOnboardingPage.paginationButtons().get(0).shouldHave(cssClass("selected"));
    scmOnboardingPage.paginationButtons().shouldHaveSize(1);
  }

  @Test
  public void testOrgDropdown() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();

    // given several additional organizations, created in non-alphabetic order
    Organization org5 = tempEntity.newOrganization("Test Org 5");
    Organization org2 = tempEntity.newOrganization("Test Org 2");
    Organization org4 = tempEntity.newOrganization("Test Org 4");
    Organization org3 = tempEntity.newOrganization("Test Org 3");

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the org dropdown is shown
    scmOnboardingPage.organizationsDropdown().shouldBe(enabled);
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Test Org"));

    // and the tooltip will show
    scmOnboardingPage.importLabelQuestionIcon()
        .shouldNotHave(attribute("aria-describedby"));
    scmOnboardingPage.importLabelQuestionIcon().hover();
    Tooltip.get().shouldHave(text("IQ Server will attempt to connect to GitHub using the credentials " +
        "associated with the target organization"));

    // when we pull down the list
    OrganizationsDropdownMenu menu = scmOnboardingPage.organizationsDropdown().dropdownMenu();

    // then the org list is complete
    menu.options().containsAll(Arrays.asList(org, org2, org3, org4, org5));
  }

  @Test
  public void testOrgDropdown_noOrgSelected() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // then the org dropdown is shown with no org selected
    scmOnboardingPage.organizationsDropdown().shouldBe(enabled);
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Select"));

    // table is rendered, and shows no entries
    scmOnboardingPage.resultsTableBody().shouldHave(text("No matching repositories."));

    // when select the org without a custom token
    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownItems().find(exactText("Test Org")).click();

    // then it triggers a reload, repo list is unchanged
    scmOnboardingPage.resultsTableProject().shouldHaveSize(13);
  }

  @Test
  public void testOrgDropdown_requeryOnSelection() throws Exception {
    // given a mock git service
    setupMockRepos();

    // given source control at the root
    setupOrgSourceControl();

    // given another org that does not override the token
    tempEntity.newOrganization("Test Org 2");

    // given an org that overrides the token
    Organization orgCustomToken = tempEntity.newOrganization("Custom Token");
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN2".toCharArray()));
    tempEntity.newSourceControl(orgCustomToken.getId(), null, encryptedPwd, null);

    // given a second git server
    ObjectMapper mapper = new ObjectMapper();
    String repoUrl = "/org/repo.git";
    String cloneUrl = secondaryGitService.baseUrl() + repoUrl;
    String json = mapper.writeValueAsString(
        Arrays.asList(of(
            "name", "test",
            "description", "",
            "private", false,
            "clone_url", cloneUrl)
        )
    );
    mockRepoForPage(secondaryGitService, 0, json);
    mockRepoForPage(secondaryGitService, 1, EMPTY_JSON_ARRAY);

    // given an org that overrides the token and uses the second git service
    Organization orgCustomHost = tempEntity.newOrganization("Custom Host");
    tempEntity.newSourceControl(orgCustomHost.getId(), null, encryptedPwd, null);
    Application appCustomHost = tempEntity.newApplication(orgCustomHost.getId());
    tempEntity.newSourceControl(appCustomHost.getId(), secondaryGitService.baseUrl() + "/org/existingrepo", null);

    // given SCM onboarding page with no selected org (from cog menu)
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // then the org dropdown is shown with no current selection
    scmOnboardingPage.organizationsDropdown().shouldBe(enabled);
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Select"));

    // and the repo list is empty with no errors
    scmOnboardingPage.resultsTableProject().shouldHaveSize(0);
    scmOnboardingPage.resultsTableBody().shouldHave(text("No matching repositories."));

    // when we pull down the list
    scmOnboardingPage.organizationsDropdown().click();
    ElementsCollection menuButtons = scmOnboardingPage.orgDropdownItems();

    // then the org list is complete. Should be sorted alphabetically with the current option at the top, duped
    menuButtons.shouldHave(exactTexts("Select", "Custom Host", "Custom Token", "Test Org", "Test Org 2"));

    // when we select an org
    menuButtons.find(exactText("Test Org")).click();

    // then we're prompted for a host URL as no SC entries exist, except those with custom tokens
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);
    scmOnboardingPage.modalDialog().shouldBe(visible);
    scmOnboardingPage.hostUrl().shouldBe(visible, enabled);

    // and the git host URL field should default to the cloud provider
    scmOnboardingPage.hostUrl().shouldHave(value("https://github.com/"));

    // when we set the host URL to our local git service and continue
    scmOnboardingPage.hostUrl().setValue(gitService.baseUrl());
    scmOnboardingPage.hostUrl().shouldHave(value(gitService.baseUrl()));
    scmOnboardingPage.hostUrlInvalidMessage().shouldNotBe(visible);
    scmOnboardingPage.hostUrlContinueButton().shouldBe(enabled).click();

    // then the repository list gets populated
    scmOnboardingPage.modalDialog().shouldNotBe(visible);
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);
    scmOnboardingPage.resultsTableProject().shouldHaveSize(13);

    // when we reset the git service responses to have 0 entries, letting us test if a requery happens
    mockRepoForPage(gitService, 0, EMPTY_JSON_ARRAY);

    // and when select another the org without a custom token
    scmOnboardingPage.organizationsDropdown().click();
    menuButtons.find(exactText("Test Org 2")).click();

    // then it doesn't trigger a reload, repo list is unchanged
    scmOnboardingPage.resultsTableProject().shouldHaveSize(13);

    // when select org with a custom token and no SC entries
    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownItems().find(exactText("Custom Token")).click();

    // then it prompts us for a git host
    scmOnboardingPage.modalDialog().shouldBe(visible);
    scmOnboardingPage.gitHostError().shouldHave(text("IQ Server was unable to identify the URL for your GitHub host."));

    // when we cancel
    scmOnboardingPage.hostUrlCancelButton().click();

    // then the dialog is hidden
    scmOnboardingPage.modalDialog().shouldNotBe(visible);

    // then we see an error message with a link enabling us to relaunch the dialog
    scmOnboardingPage.loadError().shouldHave(text(
        "An error occurred loading data. IQ Server was unable to identify the URL for your GitHub host.\n" +
        "Click here to change the git host URL.\n" +
        "Retry"));
    scmOnboardingPage.loadErrorAnchor().click();

    // then the dialog is visible again
    scmOnboardingPage.modalDialog().shouldBe(visible);

    // when we cancel
    scmOnboardingPage.hostUrlCancelButton().click();

    // and click the 'retry' button instead
    scmOnboardingPage.retry().click();

    // then it launches the git host dialog (instead of just trying to query)
    scmOnboardingPage.modalDialog().shouldBe(visible);
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);

    // when we restore the list of repos
    setupMockRepos();

    // when we set the host URL to our local git service and continue
    scmOnboardingPage.hostUrl().setValue(gitService.baseUrl());
    scmOnboardingPage.hostUrl().shouldHave(value(gitService.baseUrl()));
    scmOnboardingPage.hostUrlInvalidMessage().shouldNotBe(visible);
    scmOnboardingPage.hostUrlContinueButton().shouldBe(enabled).click();

    // then the repository list gets populated
    scmOnboardingPage.modalDialog().shouldNotBe(visible);
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);
    scmOnboardingPage.resultsTableProject().shouldHaveSize(13);

    // when we select the custom host org
    scmOnboardingPage.organizationsDropdown().click();
    menuButtons.find(exactText("Custom Host")).click();

    // then it loads the page immediately with our secondary git service results
    scmOnboardingPage.resultsTableProject().shouldHaveSize(1);
  }

  @Test
  public void testReportsCta_modal() throws Exception {
    // given SCM onboarding page with a selected organization
    setupMockRepos();
    setupSourceControl();

    // and loading scm onboarding page with given org id
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the "Go to Reports" button should not appear
    scmOnboardingPage.titleReportsCta().shouldNotBe(visible);

    // when we import several repositories
    scmOnboardingPage.resultsTableSelectAll().parent().shouldBe(visible);
    scmOnboardingPage.projectFilter().setValue("ci-");
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.importRepoButton().click();

    // then the status modal should appear
    scmOnboardingPage.importStatusModal().shouldBe(visible);

    // when we click the reports button
    scmOnboardingPage.importStatusCta().click();

    // then we are taken to the reports page
    waitUntilUrl(ReportListPage.url());
  }

  @Test
  public void testReportsCta_header() throws Exception {
    // given SCM onboarding page with a selected organization
    setupMockRepos();
    setupSourceControl();

    // and loading scm onboarding page with given org id
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the "Go to Reports" button should not appear
    scmOnboardingPage.titleReportsCta().shouldNotBe(visible);

    // when we import several repositories
    scmOnboardingPage.resultsTableSelectAll().parent().shouldBe(visible);
    scmOnboardingPage.projectFilter().setValue("ci-");
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.importRepoButton().click();

    // when we dismiss the status dialog
    scmOnboardingPage.importStatusContinue().click();

    // then the CTA should appear
    scmOnboardingPage.titleReportsCta().shouldBe(visible);

    // when we click the reports button
    scmOnboardingPage.titleReportsCta().click();

    // then we are taken to the reports page
    waitUntilUrl(ReportListPage.url());
  }

  @Test
  public void testReportsCta_alreadyImported() throws Exception {
    // given an SCM with a single, already imported, git repo
    ObjectMapper mapper = new ObjectMapper();
    String repoUrl = "/org/repo.git";
    String cloneUrl = gitService.baseUrl() + repoUrl;
    String json = mapper.writeValueAsString(
        Arrays.asList(of(
            "name", "test",
            "description", "",
            "private", false,
            "clone_url", cloneUrl)
        )
    );
    mockRepoForPage(0, json);
    mockRepoForPage(1, EMPTY_JSON_ARRAY);
    setupOrgSourceControl();
    setupAppSourceControl(repoUrl);

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the CTA should appear
    scmOnboardingPage.titleReportsCta().shouldBe(visible);
  }

  @Test
  public void testNewOrgCreation() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the org dropdown is shown
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Test Org"));

    // when creating a new organization
    scmOnboardingPage.newOrgButton().click();
    scmOnboardingPage.createOrgButton().shouldHave(cssClass("disabled"));
    scmOnboardingPage.newOrgName().setValue("Foo Organization");
    scmOnboardingPage.createOrgButton().shouldNotHave(cssClass("disabled"));
    scmOnboardingPage.createOrgButton().click();
    scmOnboardingPage.newOrgModal().shouldBe(hidden);

    // Then the new organization is created and selected
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Foo Organization"));
  }

  @Test
  public void testNewOrgCreation_error() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // when creating a new organization that already exists
    scmOnboardingPage.newOrgButton().click();
    scmOnboardingPage.newOrgName().setValue("Foo Organization");
    scmOnboardingPage.createOrgButton().click();
    scmOnboardingPage.newOrgModal().shouldBe(hidden);
    scmOnboardingPage.newOrgButton().click();
    scmOnboardingPage.newOrgName().setValue("Foo Organization");
    scmOnboardingPage.createOrgButton().click();

    // Then the new organization is created and selected
    scmOnboardingPage.newOrgModalError().shouldHave(text("Failed to create organization. Foo Organization is already" +
        " used as a name."));
  }

  @Test
  public void testNewOrgCreation_trimsWhitespace() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // when creating a new organization with whitespace in its name
    scmOnboardingPage.newOrgButton().click();
    scmOnboardingPage.createOrgButton().shouldHave(cssClass("disabled"));
    scmOnboardingPage.newOrgName().setValue("  Foo Organization  ");
    scmOnboardingPage.createOrgButton().shouldNotHave(cssClass("disabled"));
    scmOnboardingPage.createOrgButton().click();
    scmOnboardingPage.newOrgModal().shouldBe(hidden);

    // Then the new organization is created and selected
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Foo Organization"));
  }

  @Test
  public void testNewOrgCreation_clearsError() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // and an organization that already exists
    scmOnboardingPage.newOrgButton().click();
    scmOnboardingPage.newOrgName().setValue("Foo Organization");
    scmOnboardingPage.createOrgButton().click();
    scmOnboardingPage.newOrgModal().shouldBe(hidden);
    scmOnboardingPage.newOrgButton().click();
    scmOnboardingPage.newOrgName().setValue("Foo Organization");
    scmOnboardingPage.createOrgButton().click();
    scmOnboardingPage.newOrgModalError().shouldHave(text("Failed to create organization. Foo Organization is already" +
        " used as a name."));

    // when the organzation name is modified
    scmOnboardingPage.newOrgName().setValue("Bar Organization");

    // then the error is cleared
    scmOnboardingPage.newOrgModalError().shouldBe(hidden);
  }

  @Test
  public void testNewOrgCreation_invalidChars() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // when creating a new organization that already exists
    scmOnboardingPage.newOrgButton().click();
    scmOnboardingPage.newOrgName().setValue("!#$@");

    // then an form validation error is displayed
    scmOnboardingPage.newOrganizationInvalidMessage().shouldHave(
        text("Organization name contains an invalid character"));
  }

  @Test
  public void testNewOrgCreation_reloadTriggered() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();
    String json = new ObjectMapper().writeValueAsString(
        singletonList(ImmutableMap.of(
            "name", "test",
            "description", "",
            "private", false,
            "clone_url", secondaryGitService.baseUrl() + "/org/repo.git")
        )
    );
    mockRepoForPage(secondaryGitService, 0, json);
    mockRepoForPage(secondaryGitService, 1, EMPTY_JSON_ARRAY);

    // given an org that overrides the token and uses the second git service
    Organization orgCustomHost = tempEntity.newOrganization("Custom Host");
    String encryptedPwd = new String(pwHandler.encryptPassword("password".toCharArray()));
    tempEntity.newSourceControl(orgCustomHost.getId(), null, encryptedPwd, null);
    Application appCustomHost = tempEntity.newApplication(orgCustomHost.getId());
    tempEntity.newSourceControl(appCustomHost.getId(), secondaryGitService.baseUrl() + "/org/existingrepo", null);

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(orgCustomHost.getId()));
    loginAsAdmin();

    // then the org dropdown is shown
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Custom Host"));
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);
    scmOnboardingPage.resultsTableProject().shouldHaveSize(1);

    // when creating a new organization
    scmOnboardingPage.newOrgButton().click();
    scmOnboardingPage.createOrgButton().shouldHave(cssClass("disabled"));
    scmOnboardingPage.newOrgName().setValue("Foo Organization");
    scmOnboardingPage.createOrgButton().shouldNotHave(cssClass("disabled"));
    scmOnboardingPage.createOrgButton().click();
    scmOnboardingPage.newOrgModal().shouldBe(hidden);

    // Then the new organization is created and selected
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Foo Organization"));
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);
    scmOnboardingPage.resultsTableProject().shouldHaveSize(13);
  }

  @Test
  public void testHostModal_scmAuthenticationFailure_rootToken() throws Exception {
    // given an SCM with authentication failure
    setupSourceControl();
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)));

    // when the scm onboarding page is opened
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then an authentication error is displayed inside the results table
    String expectedUrl = SourceControlEditorPage.url("organization", ROOT_ORGANIZATION_ID);
    scmOnboardingPage.loadError().shouldHave(text("An error occurred loading data. Authentication with github failed." +
        " You can update your login credentials here."));
    scmOnboardingPage.loadErrorLink().shouldHave(attribute("href", expectedUrl));

    // and an authentication error is displayed in the host URL modal
    scmOnboardingPage.hostUrlAuthError().shouldHave(text("Authentication with github failed." +
        " You can update your login credentials here."));
    scmOnboardingPage.hostUrlAuthErrorLink().shouldHave(attribute("href", expectedUrl));
  }

  @Test
  public void testHostModal_scmAuthenticationFailure_tokenOverride() throws Exception {
    // given an SCM with authentication failure
    setupSourceControl();
    setupMockRepos();
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)));

    // given an org that overrides the token
    Organization orgCustomToken = tempEntity.newOrganization("Custom Host");
    String encryptedPwd = new String(pwHandler.encryptPassword("password".toCharArray()));
    tempEntity.newSourceControl(orgCustomToken.getId(), null, encryptedPwd, null);

    // when the scm onboarding page is opened
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(orgCustomToken.getId()));
    loginAsAdmin();
    scmOnboardingPage.hostUrl().setValue(gitService.baseUrl());
    scmOnboardingPage.hostUrl().shouldHave(value(gitService.baseUrl()));
    scmOnboardingPage.hostUrlInvalidMessage().shouldNotBe(visible);
    scmOnboardingPage.hostUrlContinueButton().shouldBe(enabled).click();

    // then an authentication error is displayed inside the results table
    String expectedUrl = SourceControlEditorPage.url("organization", orgCustomToken.getId());
    scmOnboardingPage.loadError().shouldHave(text("An error occurred loading data. Authentication with github failed." +
        " You can update your login credentials here."));
    scmOnboardingPage.loadErrorLink().shouldHave(attribute("href", expectedUrl));

    // and an authentication error is displayed in the host URL modal
    scmOnboardingPage.hostUrlAuthError().shouldHave(text("Authentication with github failed." +
        " You can update your login credentials here."));
    scmOnboardingPage.hostUrlAuthErrorLink().shouldHave(attribute("href", expectedUrl));

  }
}
