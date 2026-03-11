/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.ScmOnboardingPage;
import com.sonatype.clm.testing.functional.pages.ScmOnboardingPage.OrganizationsDropdownMenu;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.CollectionCondition.textsInAnyOrder;
import static com.codeborne.selenide.Condition.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.collect.ImmutableMap.of;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.Keys.BACK_SPACE;

public class ScmOnboardingTest
    extends AbstractFunctionalTest
{
  public static final String EMPTY_JSON_ARRAY = "[]";

  private static final String ID_SELECTOR_FORMAT = "http\\:\\/\\/localhost\\:%s\\/%s";

  private static final String CI_PROJECT_1_GIT = "depshield-ci\\/ci-project-1";

  private static final String REPOSITORY_P_2_GIT = "sonatype-nexus-community\\/nexus-repository-p2";

  private static final String REPOSITORY_PUPPET_GIT = "sonatype-nexus-community\\/nexus-repository-puppet";

  private static final String REPOSITORY_TERRAFORM_GIT = "sonatype-nexus-community\\/nexus-repository-terraform";

  private static final String REPOSITORY_VGO_GIT = "sonatype-nexus-community\\/nexus-repository-vgo";

  @Rule
  public final WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Rule
  public final WireMockRule secondaryGitService = new WireMockRule(wireMockConfig().dynamicPort());

  private PasswordHandler pwHandler;

  private Organization org;

  private Organization level1ChildOrg;

  @After
  public void clearCookies() {
    Selenide.clearBrowserCookies();
  }

  @Before
  public void setup() {
    pwHandler = testCLMServer.getCLMServer().getInstance(PasswordHandler.class);

    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")));
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": false }")));

    secondaryGitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")));
    secondaryGitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": false }")));

    org = tempEntity.newOrganization("Test Org");
    level1ChildOrg = tempEntity.newOrganization("Child Organization N-Level", org);

    setFeatures(LicensedFeature.AUTOMATION, LicensedFeature.SOURCE_CONTROL);
  }

  private void setupMockRepos() throws IOException {
    mockRepoForPage(1, getResourceAsString("/ScmOnboardingTest/allRepos0.json"));
    mockRepoForPage(2, EMPTY_JSON_ARRAY);
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
            .withBody(json.replaceAll("https://localhost", gitService.baseUrl()))));
  }

  private String getResourceAsString(String filename) throws IOException {
    return IOUtils.toString(this.getClass().getResourceAsStream(filename), StandardCharsets.UTF_8);
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
    scmOnboardingPage.getPageTitleElements().shouldHave(size(1));
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
  public void testLicenseCheck() {
    // given no licensed features
    setFeatures();

    // when we open the page as admin
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // then a permission denied error is shown
    scmOnboardingPage.loadError().shouldBe(visible).shouldHave(
        text(
          "An error occurred loading data. It appears that your current licence do not support this feature(s). "
          + "If you believe this to be incorrect please contact your administrator."
        )
    );

    // and form elements are hidden
    scmOnboardingPage.hostUrl().shouldBe(hidden);
    scmOnboardingPage.resultsTable().shouldBe(hidden);

    // when the AUTOMATION feature is added
    logout();
    setFeatures(LicensedFeature.AUTOMATION);

    // and we log in as admin
    refreshOrOpen(ScmOnboardingPage.url());
    loginAsAdmin();

    // then the page is shown with no error
    scmOnboardingPage.newOrgButton().shouldBe(visible);
  }

  @Test
  public void testGenericErrorsForwardedFromIq() {
    // given an SCM with authentication failure
    setupSourceControl();
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_BAD_GATEWAY)));

    // given the onboarding page
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    // when we open the onboarding page
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then org selection is visible
    scmOnboardingPage.organizationsDropdown().shouldBe(visible);

    // then the message from IQ server is forwarded
    scmOnboardingPage.repoTableLoadError()
        .shouldHave(text("An error occurred loading data. Request failed with status code 500"));
  }

  @Test
  public void testScmDoesNotExist() {
    // given the onboarding page
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    // given an org which overrides the provider but doesn't provide a token
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN".toCharArray()));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, encryptedPwd, GITHUB);
    tempEntity.newSourceControl(org.getId(), null, null, GITLAB);

    // when we open the onboarding page as unprivileged user
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then a permission denied error is shown
    scmOnboardingPage.repoTableLoadError()
        .shouldHave(text("Source control authentication is not configured. You can configure authentication " +
            "(GitHub App or Personal Access Token) to be shared across organizations in the Root Organization's " +
            "Source Control Configuration page, or you can provide a custom configuration for the Test Org " +
            "Organization."));

    // and form elements are hidden
    scmOnboardingPage.resultsTable().shouldBe(hidden);

    eyesWatcher.eyesCheck("ScmOnboarding error token not found");
  }

  @Test
  public void testLoadAfterNewOrg() throws Exception {
    // given mock repos and source control defaults
    setupMockRepos();
    setupSourceControl();

    // given an existing app with a source control value configured
    setupAppSourceControl("/org/repo.git");

    // given an org that will return an error when we try to query
    Organization orgCustomToken = tempEntity.newOrganization("Invalid Auth");
    String encryptedPwd = new String(pwHandler.encryptPassword("foo".toCharArray()));
    tempEntity.newSourceControl(orgCustomToken.getId(), null, encryptedPwd, null);
    secondaryGitService.stubFor(get(urlMatching("/api/v3/user.*"))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_UNAUTHORIZED)));
    Application customApp = tempEntity.newApplication(orgCustomToken.getId());
    tempEntity.newSourceControl(customApp.getId(), secondaryGitService.baseUrl() + "/customorg/repo.git", null);

    // when the scm onboarding page is opened to the auth error org
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url() + "/" + orgCustomToken.getId());
    loginAsAdmin();

    // then we should receive an auth error
    scmOnboardingPage.hostUrlAuthError().shouldHave(text("Authentication Error"));
    scmOnboardingPage.hostUrlCancelButton().click();

    // when we create a new org
    scmOnboardingPage.newOrgButton().click();
    OwnerEditorDialog.addindTo().shouldHave(text("Adding to: Invalid Auth"));
    OwnerEditorDialog.name().setValue("Foo Organization");
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);

    // then it should automatically query and fully populate
    verifyAllReposLoaded(scmOnboardingPage);
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
    scmOnboardingPage.gitHostError().shouldHave(text(
        "Authentication Error. IQ Server was unable to authenticate with GitHub using the credentials " +
            "associated with the Test Org Organization. You may try a different host URL or manage your " +
            "SCM configuration in the Orgs & Policies page."
    ));

    eyesWatcher.eyesCheck("ScmOnboarding authentication error");

    // when authentication is fixed and retry button is pressed
    removeStub(stubMapping);
    setupMockRepos();

    scmOnboardingPage.hostUrlContinueButton().click();

    // page is rendered without error
    scmOnboardingPage.resultsTable().shouldBe(visible);
    scmOnboardingPage.loadError().shouldBe(hidden);
  }

  @Test
  public void testPopulatesRepositories_scmAuthorizationFailure() {
    // given an SCM with authentication failure
    setupSourceControl();
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_FORBIDDEN)));

    // when the scm onboarding page is opened
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // then an authorization error is displayed
    scmOnboardingPage.repoTableLoadError().shouldHave(text(
        "Due to an Authorization Error, IQ Server was unable to connect to GitHub " +
            "using the credentials associated with the Test Org Organization. " +
            "You may try a different host URL or manage your SCM configuration in the Orgs & Policies page."));
    scmOnboardingPage.gitHostError().shouldHave(text("Authorization Error. IQ Server was unable to connect to GitHub"));
  }

  @Test
  public void testGitHost_afterAuthFailure() {
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
    scmOnboardingPage.gitHostInfo().shouldHave(text("IQ Server was unable to identify the URL for your GitHub host."));

    // when we update the URL and attempt to load
    scmOnboardingPage.hostUrl().setValue(gitService.baseUrl());
    scmOnboardingPage.hostUrl().shouldHave(value(gitService.baseUrl()));
    scmOnboardingPage.hostUrlInvalidMessage().shouldNotBe(visible);
    scmOnboardingPage.hostUrlContinueButton().shouldBe(enabled).click();

    // then an authorization error is displayed
    scmOnboardingPage.modalDialog().shouldBe(visible);
    scmOnboardingPage.gitHostError().shouldHave(text("Authentication Error."));

    // when we cancel and switch to a new org
    scmOnboardingPage.hostUrlCancelButton().click();
    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownItems().find(exactText("Test Org")).click();

    // then we should get a git host dialog prompting us for the git host URL
    scmOnboardingPage.modalDialog().shouldBe(visible);
    scmOnboardingPage.gitHostInfo().shouldHave(text("IQ Server was unable to identify the URL for your GitHub host."));
  }

  @Test
  public void testPopulatesRepositories_scmUnableToConnect() {
    // given an endpoint which does not respond
    setupSourceControl();
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    // when the scm onboarding page is opened
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url() + "/" + org.getId());
    loginAsAdmin();

    // then an error indicating a connection failure is shown
    scmOnboardingPage.repoTableLoadError()
        .shouldHave(text("An error occurred loading data. Request failed with status code 500"));
    scmOnboardingPage.repoTableLoadError().shouldHave(text("Click here to change the git host URL."));
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
    scmOnboardingPage.resultsTableDescription().get(13).shouldHave(cssValue("text-overflow", "ellipsis"));

    // the long namespaces are trimmed
    scmOnboardingPage.resultsTableNamespace().get(13).shouldHave(cssValue("text-overflow", "ellipsis"));

    // the long projects are trimmed
    scmOnboardingPage.resultsTableProject().get(13).shouldHave(cssValue("text-overflow", "ellipsis"));

    // and the default branches are populated
    Actions actions = new Actions(WebDriverRunner.getWebDriver());
    actions.moveToElement(scmOnboardingPage.resultsTableDefaultBranch().first());
    actions.perform();
    assertThat(scmOnboardingPage.resultsTableDefaultBranch().texts()).containsExactlyInAnyOrder("master", "main",
        "prod", "golden", "boss", "shipit", "junk", "release", "ignition", "product", "liftoff", "top", "green",
        "master");

    // and there is a hover tooltip over the trimmed description
    scmOnboardingPage.resultsTableDescription().get(13).hover();
    scmOnboardingPage.descriptionTooltip().should(matchText(".{101,}")).shouldNotHave(text("..."));

    // and there is a hover tooltip over the trimmed namespace
    scmOnboardingPage.resultsTableNamespace().get(13).hover();
    scmOnboardingPage.namespaceTooltip().should(matchText(".{50,}")).shouldNotHave(text("..."));

    // and there is a hover tooltip over the trimmed project
    scmOnboardingPage.resultsTableProject().get(13).hover();
    scmOnboardingPage.projectTooltip().should(matchText(".{50,}")).shouldNotHave(text("..."));

    // when the application already exists in IQ
    Application application = tempEntity.newApplication(org.getId());
    String repositoryUrl = String.format("%s/depshield-ci/ci-project-1.git", gitService.baseUrl());
    tempEntity.newSourceControl(application.getId(), repositoryUrl, new Date());
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));

    // it is no longer displayed in the table and the UI is updated
    scmOnboardingPage.resultsTableProject().shouldHave(sizeGreaterThan(0));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).doesNotContain("ci-project-1");
    scmOnboardingPage.donutChartPercentImported().shouldHave(attribute("aria-label", "7% imported"));
    scmOnboardingPage.resultsTableAlreadyImported().shouldBe(text("1"));
  }

  @Test
  public void testPopulatesRepositories_NLevelOrganization() throws Exception {
    // given an SCM with git repos
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(level1ChildOrg.getId()));
    loginAsAdmin();

    waitUntilUrl(ScmOnboardingPage.url(level1ChildOrg.getId()));
    // then all repositories were loaded
    verifyAllReposLoaded(scmOnboardingPage);

    scmOnboardingPage.donutChartPercentImported().shouldHave(attribute("aria-label", "0% imported"));
    scmOnboardingPage.resultsTableAlreadyImported().shouldBe(text("0"));

    // the long descriptions are trimmed
    scmOnboardingPage.resultsTableDescription().get(13).shouldHave(cssValue("text-overflow", "ellipsis"));

    // the long namespaces are trimmed
    scmOnboardingPage.resultsTableNamespace().get(13).shouldHave(cssValue("text-overflow", "ellipsis"));

    // the long projects are trimmed
    scmOnboardingPage.resultsTableProject().get(13).shouldHave(cssValue("text-overflow", "ellipsis"));

    // and the default branches are populated
    Actions actions = new Actions(WebDriverRunner.getWebDriver());
    actions.moveToElement(scmOnboardingPage.resultsTableDefaultBranch().first());
    actions.perform();
    assertThat(scmOnboardingPage.resultsTableDefaultBranch().texts()).containsExactlyInAnyOrder("master", "main",
        "prod", "golden", "boss", "shipit", "junk", "release", "ignition", "product", "liftoff", "top", "green",
        "master");

    // and there is a hover tooltip over the trimmed description
    scmOnboardingPage.resultsTableDescription().get(13).hover();
    scmOnboardingPage.descriptionTooltip().should(matchText(".{101,}")).shouldNotHave(text("..."));

    // and there is a hover tooltip over the trimmed namespace
    scmOnboardingPage.resultsTableNamespace().get(13).hover();
    scmOnboardingPage.namespaceTooltip().should(matchText(".{50,}")).shouldNotHave(text("..."));

    // and there is a hover tooltip over the trimmed project
    scmOnboardingPage.resultsTableProject().get(13).hover();
    scmOnboardingPage.projectTooltip().should(matchText(".{50,}")).shouldNotHave(text("..."));

    // when the application already exists in IQ
    Application application = tempEntity.newApplication(level1ChildOrg.getId());
    String repositoryUrl = String.format("%s/depshield-ci/ci-project-1.git", gitService.baseUrl());
    tempEntity.newSourceControl(application.getId(), repositoryUrl, new Date());
    refreshOrOpen(ScmOnboardingPage.url(level1ChildOrg.getId()));

    // it is no longer displayed in the table and the UI is updated
    scmOnboardingPage.resultsTableProject().shouldHave(sizeGreaterThan(0));
    assertThat(scmOnboardingPage.resultsTableProject().texts()).doesNotContain("ci-project-1");
    scmOnboardingPage.donutChartPercentImported().shouldHave(attribute("aria-label", "7% imported"));
    scmOnboardingPage.resultsTableAlreadyImported().shouldBe(text("1"));
  }

  @Test
  public void testPopulatesRepositories_noAvailableRepositories() {
    // given an SCM without git repos
    mockRepoForPage(1, EMPTY_JSON_ARRAY);
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the table is empty
    scmOnboardingPage.repositoryCount().shouldBe(text("0"), Duration.ofMillis(2000));

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
        singletonList(of(
            "name", "test",
            "description", "",
            "private", false,
            "clone_url", cloneUrl,
            "ssh_url", "git@host" + repoUrl)
        )
    );
    mockRepoForPage(1, json);
    mockRepoForPage(2, EMPTY_JSON_ARRAY);
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

  @Test
  public void testValidatePArentOrgInNewOrganizationModal() throws Exception {
    // given an SCM with git repos
    setupMockRepos();
    setupSourceControl();

    Organization level2ChildOrg = tempEntity.newOrganization("Child 2 Organization N-Level", level1ChildOrg);
    Organization level3ChildOrg = tempEntity.newOrganization("Child 3 Organization N-Level", level2ChildOrg);

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(level1ChildOrg.getId()));
    loginAsAdmin();

    waitUntilUrl(ScmOnboardingPage.url(level1ChildOrg.getId()));

    scmOnboardingPage.newOrgButton().click();

    OwnerEditorDialog.addindTo().shouldHave(text("Adding to: Child Organization N-Level"));
    OwnerEditorDialog.cancelButton().click();

    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownItems().find(exactText("Child 2 Organization N-Level")).click();

    waitUntilUrl(ScmOnboardingPage.url(level2ChildOrg.getId()));

    scmOnboardingPage.newOrgButton().click();

    OwnerEditorDialog.addindTo().shouldHave(text("Adding to: Child 2 Organization N-Level"));
    OwnerEditorDialog.cancelButton().click();

    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownItems().find(exactText("Child 3 Organization N-Level")).click();

    waitUntilUrl(ScmOnboardingPage.url(level3ChildOrg.getId()));

    scmOnboardingPage.newOrgButton().click();

    OwnerEditorDialog.addindTo().shouldHave(text("Adding to: Child 3 Organization N-Level"));
    OwnerEditorDialog.cancelButton().click();
  }

  private void verifyAllReposLoaded(final ScmOnboardingPage scmOnboardingPage) {
    // then results are automatically displayed in the table
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);
    scmOnboardingPage.resultsTable().shouldBe(visible);
    scmOnboardingPage.repositoryCount().shouldBe(visible);
    scmOnboardingPage.selectedToImportCount().shouldBe(text("0 of 14 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(exactTexts("ci-project-1",
        "ci-project-16", "create-react-app", "missing-description", "nexus-repository-p2", "nexus-repository-puppet",
        "nexus-repository-terraform", "nexus-repository-vgo", "nexus-scripting-examples",
        "nexus-webhook-example-collection", "null-description", "oysteR",
        "prime-nexus-proxy-repos", "this-is-a-repository-with-a-really-long-name-ci-project-1"));
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
    scmOnboardingPage.repositoryCount().shouldBe(text("14"));
    scmOnboardingPage.resultsTableSelectAll().parent().shouldBe(visible);
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated
    scmOnboardingPage.selectedToImportCount().shouldBe(text("14 of 14 repositories"));

    // when select all is clicked again (delected)
    scmOnboardingPage.resultsTableSelectAll().parent().click();

    // then selected count is updated
    scmOnboardingPage.selectedToImportCount().shouldBe(text("0"));
  }

  @Test
  public void testSelection_resetAfterPageFlip() throws Exception {
    // given an SCM with enough git repos to require pagination
    mockRepoForPage(1, getResourceAsString("/ScmOnboardingTest/mixedOrderRepos.json"));
    mockRepoForPage(2, EMPTY_JSON_ARRAY);
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
    scmOnboardingPage.selectedToImportCount().shouldBe(text("3 of 14 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(exactTexts("ci-project-1",
        "ci-project-16", "this-is-a-repository-with-a-really-long-name-ci-project-1"));
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
    scmOnboardingPage.selectedToImportCount().shouldBe(text("3  of 14 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(texts("ci-project-1",
        "ci-project-16", "this-is-a-repository-with-a-really-long-name-ci-project-1"));

    // when we import the selected repos
    scmOnboardingPage.importRepoButton().click();

    // then the import spinners should not be visible
    // NB: submit is so fast, it's not possible to reliable test for 'shouldBe(visible)
    scmOnboardingPage.submitLoadingSpinner().shouldNotBe(visible);

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
    scmOnboardingPage.alreadyImportedCount().shouldBe(text("3"));

    // and the initially selected elements are no longer visible
    scmOnboardingPage.selectedToImportCount().shouldBe(text("0  of 11 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(size(0));

    // and they are not there when the filter is updated
    scmOnboardingPage.projectFilter().sendKeys(IntStream.range(0, 3)
        .mapToObj(i -> Keys.BACK_SPACE).toArray(CharSequence[]::new));
    scmOnboardingPage.resultsTableSelectAll().parent().click();
    scmOnboardingPage.repositoryCount().shouldBe(text("11"));
    scmOnboardingPage.selectedToImportCount().shouldBe(text("11 of 11 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(textsInAnyOrder(
        "create-react-app", "nexus-repository-p2", "nexus-repository-puppet",
        "nexus-repository-terraform", "nexus-repository-vgo", "nexus-scripting-examples",
        "nexus-webhook-example-collection", "null-description", "missing-description", "oysteR",
        "prime-nexus-proxy-repos"));
    assertThat(scmOnboardingPage.resultsTableNamespace().texts()).containsAnyOf("sonatype-nexus-community");

    // and the select all checkbox is checked
    scmOnboardingPage.resultsTableSelectAll().shouldBe(checked);
  }

  @Test
  public void testSelectAndImport_error() throws Exception {
    // given an SCM with git repos but with bad URLs
    mockRepoForPage(1, getResourceAsString("/ScmOnboardingTest/reposWithErrors.json"));
    mockRepoForPage(2, EMPTY_JSON_ARRAY);
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
    scmOnboardingPage.resultsTableProject().shouldHave(textsInAnyOrder("broken-url-1",
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
    scmOnboardingPage.resultsTableProject().shouldHave(texts("broken-url-1",
        "broken-url-2"));

    // and the select all checkbox is unchecked
    scmOnboardingPage.resultsTableSelectAll().shouldNotBe(checked);
  }

  @Test
  public void testSelectAndImport_successAndError() throws Exception {
    // given an SCM with git repos but with bad URLs
    mockRepoForPage(1, getResourceAsString("/ScmOnboardingTest/reposWithErrors.json"));
    mockRepoForPage(2, EMPTY_JSON_ARRAY);
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

    eyesWatcher.eyesCheck("ScmOnboarding import success / error message");

    // and can dismiss the dialog
    scmOnboardingPage.importStatusContinue().click();
    scmOnboardingPage.importStatusModal().shouldBe(hidden);

    // and the imported count is incremented
    scmOnboardingPage.alreadyImportedCount().shouldBe(text("1"));

    // and the broken elements are still visible
    scmOnboardingPage.resultsTableProject().shouldHave(texts("broken-url-1",
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
    scmOnboardingPage.selectedToImportCount().shouldBe(text("3 of 14 repositories"));

    // when a new selection is made
    scmOnboardingPage.projectFilter().setValue("nexus");
    scmOnboardingPage.resultsTableSelectAll().parent().click(); // uncheck box
    scmOnboardingPage.resultsTableSelectAll().parent().click(); // check box
    scmOnboardingPage.selectedToImportCount().shouldBe(text("7 of 14 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(texts(
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
    scmOnboardingPage.selectedToImportCount().shouldBe(text("7 of 14 repositories"));
    scmOnboardingPage.resultsTableProject().shouldHave(texts(
        "nexus-repository-p2", "nexus-repository-puppet", "nexus-repository-terraform",
        "nexus-repository-vgo", "nexus-scripting-examples", "nexus-webhook-example-collection",
        "prime-nexus-proxy-repos"));

    // when a new selection is made
    scmOnboardingPage.projectFilter().setValue("nexus-repository");

    // then the selected count is updated
    scmOnboardingPage.selectedToImportCount().shouldBe(text("4 of 14 repositories"));

    // and the result table contains exactly 4 projects
    scmOnboardingPage.resultsTableProject().shouldHave(texts(
        "nexus-repository-p2", "nexus-repository-puppet", "nexus-repository-terraform",
        "nexus-repository-vgo"));

    // and the repositories checkboxes are selected
    scmOnboardingPage.selectionCheckboxById(getIdSelector(REPOSITORY_P_2_GIT)).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(getIdSelector(REPOSITORY_PUPPET_GIT)).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(getIdSelector(REPOSITORY_TERRAFORM_GIT)).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(getIdSelector(REPOSITORY_VGO_GIT)).shouldBe(selected);

    // when the filter is changed
    scmOnboardingPage.projectFilter().setValue("i");

    // then the selections remain selected
    scmOnboardingPage.selectionCheckboxById(getIdSelector(REPOSITORY_P_2_GIT)).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(getIdSelector(REPOSITORY_PUPPET_GIT)).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(getIdSelector(REPOSITORY_TERRAFORM_GIT)).shouldBe(selected);
    scmOnboardingPage.selectionCheckboxById(getIdSelector(REPOSITORY_VGO_GIT)).shouldBe(selected);

    // and other repositories remain deselected
    scmOnboardingPage.selectionCheckboxById(getIdSelector(CI_PROJECT_1_GIT)).shouldNotBe(selected);

    // and the selected count is unchanged
    scmOnboardingPage.selectedToImportCount().shouldBe(text("4 of 14 repositories"));
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
    scmOnboardingPage.selectionCheckboxById(getIdSelector(CI_PROJECT_1_GIT)).parent().click();

    // then the checkbox is selected
    scmOnboardingPage.selectionCheckboxById(getIdSelector(CI_PROJECT_1_GIT)).shouldBe(selected);
  }

  @Test
  public void testSort() throws Exception {
    // given an SCM with git repos that start unsorted
    setupSourceControl();
    mockRepoForPage(1, getResourceAsString("/ScmOnboardingTest/mixedOrderRepos.json"));
    mockRepoForPage(2, EMPTY_JSON_ARRAY);

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
    mockRepoForPage(1, getResourceAsString("/ScmOnboardingTest/mixedOrderRepos.json"));
    mockRepoForPage(2, EMPTY_JSON_ARRAY);

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the repos list loads and has the max page size
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);
    scmOnboardingPage.resultsTableProject().shouldHave(size(15));

    // and pagination buttons are present
    scmOnboardingPage.paginationButtons().shouldHave(size(2));

    // and move viewport and perform visual test
    Actions actions = new Actions(WebDriverRunner.getWebDriver());
    actions.moveToElement(scmOnboardingPage.paginationButtons().first());
    actions.perform();
    eyesWatcher.eyesCheck("ScmOnboarding pagination visible");

    // and be on the first page
    scmOnboardingPage.paginationButtons().get(0).shouldHave(cssClass("selected"));

    // when the second pagination button is clicked
    scmOnboardingPage.paginationButtons().get(1).click();

    // then the second page of results appears
    scmOnboardingPage.resultsTableProject().shouldHave(size(5));
    scmOnboardingPage.paginationButtons().get(1).shouldHave(cssClass("selected"));
  }

  @Test
  public void testFiltersAndPagination() throws Exception {
    // given an SCM with git repos that are unsorted initially
    setupSourceControl();
    mockRepoForPage(1, getResourceAsString("/ScmOnboardingTest/mixedOrderRepos.json"));
    mockRepoForPage(2, EMPTY_JSON_ARRAY);

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
    scmOnboardingPage.paginationButtons().shouldHave(size(1));
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
    scmOnboardingPage.organizationsDropdown().click();
    OrganizationsDropdownMenu menu = scmOnboardingPage.organizationsDropdown().dropdownMenu();

    // then the org list is complete
    menu.options().shouldHave(
        texts(level1ChildOrg.getName(), org.getName(), org2.getName(), org3.getName(), org4.getName(), org5.getName()));
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
    scmOnboardingPage.resultsTableProject().shouldHave(size(14));
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
        singletonList(of(
            "name", "test",
            "description", "",
            "private", false,
            "clone_url", cloneUrl,
            "ssh_url", "git@host" + repoUrl)
        )
    );
    mockRepoForPage(secondaryGitService, 1, json);
    mockRepoForPage(secondaryGitService, 2, EMPTY_JSON_ARRAY);

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
    scmOnboardingPage.resultsTableProject().shouldHave(size(0));
    scmOnboardingPage.resultsTableBody().shouldHave(text("No matching repositories."));

    // when we pull down the list
    scmOnboardingPage.organizationsDropdown().click();
    ElementsCollection menuButtons = scmOnboardingPage.orgDropdownItems();

    // then the org list is complete. Should be sorted alphabetically
    menuButtons.shouldHave(exactTexts(
        "Child Organization N-Level",
        "Custom Host",
        "Custom Token",
        "Test Org",
        "Test Org 2"));

    // when we select an org
    menuButtons.find(exactText("Test Org")).click();

    // then we're prompted for a host URL as no SC entries exist, except those with custom tokens
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);
    scmOnboardingPage.modalDialog().shouldBe(visible);
    scmOnboardingPage.hostUrl().shouldBe(visible, enabled);

    eyesWatcher.eyesCheck("ScmOnboarding default host url modal");

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
    scmOnboardingPage.resultsTableProject().shouldHave(size(14));

    // when we reset the git service responses to have 0 entries, letting us test if a requery happens
    mockRepoForPage(gitService, 0, EMPTY_JSON_ARRAY);

    // and when select another the org without a custom token
    scmOnboardingPage.organizationsDropdown().click();
    menuButtons.find(exactText("Test Org 2")).click();

    // then it doesn't trigger a reload, repo list is unchanged
    scmOnboardingPage.resultsTableProject().shouldHave(size(14));

    // when select org with a custom token and no SC entries
    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownItems().find(exactText("Custom Token")).click();

    // then it prompts us for a git host
    scmOnboardingPage.modalDialog().shouldBe(visible);
    scmOnboardingPage.gitHostInfo().shouldHave(text("IQ Server was unable to identify the URL for your GitHub host."));

    // when we cancel
    scmOnboardingPage.hostUrlCancelButton().click();

    // then the dialog is hidden
    scmOnboardingPage.modalDialog().shouldNotBe(visible);

    // then we see an error message with a link enabling us to relaunch the dialog
    scmOnboardingPage.repoTableLoadError().shouldHave(text(
        "IQ Server was unable to identify the URL for your GitHub host. " +
            "You need to provide a SCM URL in order to proceed."));
    scmOnboardingPage.repoTableLoadErrorLink().click();

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
    scmOnboardingPage.resultsTableProject().shouldHave(size(14));

    // when we select the custom host org
    scmOnboardingPage.organizationsDropdown().click();
    menuButtons.find(exactText("Custom Host")).click();

    // then it loads the page immediately with our secondary git service results
    scmOnboardingPage.resultsTableProject().shouldHave(size(1));
  }

  @Test
  public void testOrgDropdown_filterMultiple() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();

    // given several additional organizations
    tempEntity.newOrganization("A-b");
    tempEntity.newOrganization("A-c");
    tempEntity.newOrganization("a-Bb");
    tempEntity.newOrganization("C");

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the org dropdown is shown
    scmOnboardingPage.organizationsDropdown().shouldBe(enabled);
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Test Org"));

    // when we pull down the list and filter we should see matching values
    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownFilter().setValue("a-");
    scmOnboardingPage.orgDropdownItems().shouldHave(texts("A-b", "A-c", "a-Bb"));
    clearOrgFilter(scmOnboardingPage);
    scmOnboardingPage.orgDropdownFilter().setValue("A-");
    scmOnboardingPage.orgDropdownItems().shouldHave(texts("A-b", "A-c", "a-Bb"));
    clearOrgFilter(scmOnboardingPage);
    scmOnboardingPage.orgDropdownFilter().setValue("foo");
    scmOnboardingPage.orgDropdownItems().shouldHave(size(0));
    clearOrgFilter(scmOnboardingPage);
    scmOnboardingPage.orgDropdownFilter().setValue("A-b");
    scmOnboardingPage.orgDropdownItems().shouldHave(texts("A-b", "a-Bb"));
  }

  @Test
  public void testOrgDropdown_filterRemainsVisible() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();

    // given several additional organizations
    tempEntity.newOrganization("alice");
    tempEntity.newOrganization("bob");
    Organization targetOrg = tempEntity.newOrganization("cheshire");

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the org dropdown is shown
    scmOnboardingPage.organizationsDropdown().shouldBe(enabled);
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Test Org"));

    // when we pull down the list and filter we should see matching values
    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownFilter().click();
    scmOnboardingPage.orgDropdownFilter().shouldBe(visible, focused);
    scmOnboardingPage.orgDropdownFilter().sendKeys("cheshire");
    scmOnboardingPage.orgDropdownItems().shouldHave(texts("cheshire"));
    scmOnboardingPage.orgDropdownItems().find(exactText("cheshire")).click();

    waitUntilUrl(ScmOnboardingPage.url(targetOrg.getId()));
  }

  private void clearOrgFilter(final ScmOnboardingPage scmOnboardingPage) {
    // not sure why .clear isn't working, so send a flurry of backspaces instead
    for (int i = 0; i < 15; i++) {
      scmOnboardingPage.orgDropdownFilter().sendKeys(BACK_SPACE);
    }
  }

  @Test
  public void testOrgDropdown_filterSingle() throws Exception {
    // given a mock git service
    setupMockRepos();
    setupSourceControl();

    // given SCM onboarding page with a selected organization
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url(org.getId()));
    loginAsAdmin();

    // then the org dropdown is shown
    scmOnboardingPage.organizationsDropdown().shouldBe(enabled);
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Test Org"));

    // when we pull down the list and filter we should see matching values
    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownFilter().setValue("est");
    scmOnboardingPage.orgDropdownItems().shouldHave(texts("Test Org"));
    clearOrgFilter(scmOnboardingPage);
    scmOnboardingPage.orgDropdownFilter().setValue("foo");
    scmOnboardingPage.orgDropdownItems().shouldHave(size(0));
    clearOrgFilter(scmOnboardingPage);
    scmOnboardingPage.orgDropdownFilter().setValue("st or");
    scmOnboardingPage.orgDropdownItems().shouldBe(texts("Test Org"));
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

    eyesWatcher.eyesCheck("ScmOnboarding cta modal dialog");

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
        singletonList(of(
            "name", "test",
            "description", "",
            "private", false,
            "clone_url", cloneUrl,
            "ssh_url", "git@host" + repoUrl)
        )
    );
    mockRepoForPage(1, json);
    mockRepoForPage(2, EMPTY_JSON_ARRAY);
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
    OwnerEditorDialog.name().setValue("Foo Organization");

    eyesWatcher.eyesCheck("ScmOnboarding new organization modal");

    // and pressing the create button
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);

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
    OwnerEditorDialog.name().setValue("Foo Organization");
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);
    scmOnboardingPage.newOrgButton().click();
    OwnerEditorDialog.name().setValue("Foo Organization");
    OwnerEditorDialog.saveButton().click();

    // Then the new organization is created and selected
    OwnerEditorDialog.nameInvalidMessage().shouldHave(text("Name is already in use"));
    OwnerEditorDialog.formValidationError()
        .shouldHave(text("There were validation errors. Unable to save: fields with invalid or missing data"));
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
    OwnerEditorDialog.name().setValue("  Foo Organization  ");
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);

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
    OwnerEditorDialog.name().setValue("Foo Organization");
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);
    scmOnboardingPage.newOrgButton().click();
    OwnerEditorDialog.name().setValue("Foo Organization");
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.nameInvalidMessage().shouldHave(text("Name is already in use"));
    OwnerEditorDialog.formValidationError()
        .shouldHave(text("There were validation errors. Unable to save: fields with invalid or missing data"));

    // when the organzation name is modified
    OwnerEditorDialog.name().setValue("Bar Organization");

    // then the error is cleared
    OwnerEditorDialog.nameInvalidMessage().shouldBe(hidden);
    OwnerEditorDialog.formValidationError().shouldBe(hidden);
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
    OwnerEditorDialog.name().setValue("!#$@");

    // then an form validation error is displayed
    OwnerEditorDialog.nameInvalidMessage().shouldHave(
        text("Use valid characters: alphanumeric, \"_\", \".\", \"-\", or spaces"));
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
            "clone_url", secondaryGitService.baseUrl() + "/org/repo.git",
            "ssh_url", "git@host/org/repo.git")
        )
    );
    mockRepoForPage(secondaryGitService, 1, json);
    mockRepoForPage(secondaryGitService, 2, EMPTY_JSON_ARRAY);

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
    scmOnboardingPage.resultsTableProject().shouldHave(size(1));

    // when creating a new organization
    scmOnboardingPage.newOrgButton().click();
    OwnerEditorDialog.name().setValue("Foo Organization");
    OwnerEditorDialog.saveButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);

    // Then the new organization is created and selected
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Foo Organization"));
    scmOnboardingPage.loadingSpinner().shouldNotBe(visible);
    scmOnboardingPage.resultsTableProject().shouldHave(size(14));
  }

  @Test
  public void testHostModal_scmAuthenticationFailure_rootToken() {
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
    scmOnboardingPage.repoTableLoadError().shouldHave(text(
        "Due to an Authentication Error, IQ Server was unable to authenticate with GitHub"));
    scmOnboardingPage.repoTableLoadErrorLink("Orgs & Policies").shouldHave(attribute("href", expectedUrl));

    // and an authentication error is displayed in the host URL modal
    scmOnboardingPage.hostUrlAuthError().shouldHave(text("IQ Server was unable to authenticate with GitHub"));
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
    scmOnboardingPage.repoTableLoadError().shouldHave(text(
        "Due to an Authentication Error, IQ Server was unable to authenticate with GitHub using the " +
            "credentials associated with the Custom Host Organization."));
    scmOnboardingPage.repoTableLoadErrorLink("Orgs & Policies").shouldHave(attribute("href", expectedUrl));

    // and an authentication error is displayed in the host URL modal
    scmOnboardingPage.hostUrlAuthError().shouldHave(text("Authentication Error. IQ Server was unable to authenticate " +
        "with GitHub using the credentials associated with the Custom Host Organization."));
    scmOnboardingPage.hostUrlAuthErrorLink().shouldHave(attribute("href", expectedUrl));
  }

  @Test
  public void testSelectOrganization_updatesUrl() throws Exception {
    // given an org
    setupOrgSourceControl();
    setupMockRepos();
    Organization otherOrg = tempEntity.newOrganization("Other Org");
    refreshOrOpen(ScmOnboardingPage.url(otherOrg.getId()));
    loginAsAdmin();

    // when a different org is selected
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();

    dismissScmServerNeededModal(scmOnboardingPage);

    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownItems().find(exactText("Test Org")).click();

    // then the URL is updated
    WebDriver driver = WebDriverRunner.getWebDriver();
    assertThat(driver.getCurrentUrl()).endsWith("#/onboarding/" + org.getId());

    dismissScmServerNeededModal(scmOnboardingPage);

    // when switching back to the original org
    scmOnboardingPage.organizationsDropdown().click();
    scmOnboardingPage.orgDropdownItems().find(exactText("Other Org")).click();
    // inconsistent test seems to like having the focus outside of the dropdown
    //scmOnboardingPage.orgDropdownItems().first().sendKeys(Keys.TAB);

    // then the URL is updated
    assertThat(driver.getCurrentUrl()).endsWith("#/onboarding/" + otherOrg.getId());

    dismissScmServerNeededModal(scmOnboardingPage);

    // when navigating back expect org to change to previous org
    driver.navigate().back();
    // this test fails intermittently without an explicit refresh. Not ideal, but better than the alternative
    WebDriverRunner.getWebDriver().navigate().refresh();
    scmOnboardingPage.organizationsDropdown().shouldBe(enabled);
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Test Org"));
    assertThat(driver.getCurrentUrl()).endsWith("#/onboarding/" + org.getId());

    dismissScmServerNeededModal(scmOnboardingPage);

    // when reloading the browser expect org to stay the same
    driver.navigate().refresh();
    scmOnboardingPage.organizationsDropdown().shouldBe(enabled);
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Test Org"));
    assertThat(driver.getCurrentUrl()).endsWith("#/onboarding/" + org.getId());

    dismissScmServerNeededModal(scmOnboardingPage);

    // when navigating forward expect org to change to previous org
    driver.navigate().forward();
    scmOnboardingPage.organizationsDropdown().shouldBe(enabled);
    scmOnboardingPage.organizationsDropdown().selectedOrganization().shouldHave(text("Other Org"));
    assertThat(driver.getCurrentUrl()).endsWith("#/onboarding/" + otherOrg.getId());

    dismissScmServerNeededModal(scmOnboardingPage);
  }

  private void dismissScmServerNeededModal(ScmOnboardingPage scmOnboardingPage) {
    scmOnboardingPage.hostUrlCancelButton().shouldBe(visible);
    scmOnboardingPage.hostUrlCancelButton().click();
  }

  @Test
  public void testGitHost_noPromptForCustomProviderAndNoToken() {
    // given an org with no apps
    setupOrgSourceControl();

    // given attempts to load repos results in an auth failure
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)));

    // given an org that overrides the provider but doesn't provide a new token
    Organization orgCustomProvider = tempEntity.newOrganization("Custom Provider");
    tempEntity.newSourceControl(orgCustomProvider.getId(), null, null, SourceControlProvider.GITLAB);

    // when the scm onboarding page is opened to the org with the custom provider
    ScmOnboardingPage scmOnboardingPage = new ScmOnboardingPage();
    refreshOrOpen(ScmOnboardingPage.url() + "/" + orgCustomProvider.getId());
    loginAsAdmin();

    // then the git host dialog is not loaded
    scmOnboardingPage.modalDialog().shouldNotBe(visible);

    // and an authentication error is shown
    scmOnboardingPage.loadError().shouldHave(text("Source control authentication is not configured."));
    scmOnboardingPage.loadError()
        .shouldHave(text("or you can provide a custom configuration for the Custom Provider Organization."));
  }

  private String getIdSelector(String id) {
    return String.format(ID_SELECTOR_FORMAT, gitService.port(), id);
  }
}
