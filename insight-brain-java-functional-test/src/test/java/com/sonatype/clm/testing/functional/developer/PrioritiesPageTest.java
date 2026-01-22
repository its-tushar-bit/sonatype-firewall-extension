/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.developer;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO.ToVersionData;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CreatePRModal;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.PrioritiesPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.ScmRepoVisibilityService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.report.ReportEntity;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.SourceControlPullRequestService;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.href;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.Wait;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PrioritiesPageTest
    extends AbstractFunctionalTest
{
  private static final Duration TIMEOUT = Duration.ofSeconds(3);

  private static final Duration POLL_FREQUENCY = Duration.ofMillis(200);

  @Rule
  public final WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Before
  public void setup() {
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")));
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": true }")));
  }

  private Application application;

  private PolicyDAO policyDAO;

  private InnerSourceApplicationDAO innerSourceApplicationDAO;

  private ApplicationReportPersistenceService applicationReportPersistenceService;

  @BeforeClass
  public static void initialLogin() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() throws Exception {
    policyDAO = lookup(PolicyDAO.class);
    applicationReportPersistenceService = lookup(ApplicationReportPersistenceService.class);
    innerSourceApplicationDAO = lookup(InnerSourceApplicationDAO.class);

    ImmutablePair<Application, String> appAndScanId = setUpAppsWithPriorities();
    application = appAndScanId.getLeft();
    mockRemediationData();
    //add inner source data
    ComponentIdentifier innersourceDirectComponent =
        ComponentIdentifier.createMavenCoordinates("org.jclouds.driver", "jclouds-enterprise", "1.3.1", "", "jar");
    PackageUrlIdentifier versionlessPurl = InnerSourceUtils.getVersionlessPackageUrl(innersourceDirectComponent);
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(versionlessPurl.getPackageUrl(), application);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.4.0", StageTypes.BUILD.getId());

    refreshOrOpen(PrioritiesPage.url(application.getPublicId(), appAndScanId.getRight()));
  }

  @Test
  public void testLoad() {
    PrioritiesPage page = new PrioritiesPage();
    page.title().shouldHave(text(application.getName() + " - Priorities"));
    page.summaryTile().shouldBe(visible);
    page.backLink().shouldBe(visible);
    page.prioritiesTable().shouldBe(visible);
    page.prioritiesTableRows().shouldHave(size(15));
  }

  @Test
  public void testRowData() {
    PrioritiesPage page = new PrioritiesPage();

    // a row with a reachable vulnerability
    page.prioritiesTableCell(0, 0).shouldHave(text("1"));
    page.prioritiesTableCell(0, 1).shouldHave(text("Dorg.openid4java : openid4java : 0.9.5"));
    page.prioritiesTableCell(0, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(0, 3).shouldHave(text("Reachable"));
    page.prioritiesTableCell(0, 4).shouldHave(text("Investigate"));

    // a row with a transitive violation and a non-reachable vulnerability
    page.prioritiesTableCell(1, 0).shouldHave(text("2"));
    page.prioritiesTableCell(1, 1).shouldHave(text("Ttomcat : tomcat-util : 5.5.23"));
    page.prioritiesTableCell(1, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(1, 3).shouldHave(text("Not Reachable"));
    page.prioritiesTableCell(1, 4).shouldHave(text("Waive violations"));

    // a row with an upgrade path
    page.prioritiesTableCell(8, 0).shouldHave(text("9"));
    page.prioritiesTableCell(8, 1).shouldHave(text("Dapache-httpclient : commons-httpclient : 3.1"));
    page.prioritiesTableCell(8, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(8, 3).shouldHave(text("-"));
    page.prioritiesTableCell(8, 4).shouldHave(text("Upgrade to 3.2"));

    // a row with a Warn action
    page.prioritiesTableCell(13, 0).shouldHave(text("14"));
    page.prioritiesTableCell(13, 1).shouldHave(text("sample-application.zip"));
    page.prioritiesTableCell(13, 2).shouldHave(text("Warn"));
    page.prioritiesTableCell(13, 3).shouldHave(text("-"));
    page.prioritiesTableCell(13, 4).shouldHave(text("Investigate"));

    // a row with no action
    page.prioritiesTableCell(14, 0).shouldHave(text("15"));
    page.prioritiesTableCell(14, 1).shouldHave(text("Dorg.apache.lucene : lucene-spellchecker : 2.9.0"));
    page.prioritiesTableCell(14, 2).shouldBe(empty);
    page.prioritiesTableCell(14, 3).shouldHave(text("-"));
    page.prioritiesTableCell(14, 4).shouldHave(text("Waive violations"));
  }

  @Test
  public void testComponentLink() {
    PrioritiesPage page = new PrioritiesPage();
    page.rowComponentLink(0).click();
    ComponentDetailsPage.title().shouldHave(text("org.openid4java : openid4java : 0.9.5"));
  }

  @Test
  public void testPagination() {
    PrioritiesPage page = new PrioritiesPage();
    page.lastPageLink().shouldBe(visible).click();
    page.prioritiesTableCell(0, 0).shouldHave(text("16"));
    page.prioritiesTableCell(0, 1).shouldHave(text("org.slf4j : slf4j-api : 1.6.1"));
    page.prioritiesTableCell(0, 2).shouldBe(empty);
    page.prioritiesTableCell(0, 3).shouldHave(text("-"));
    page.prioritiesTableCell(0, 4).shouldHave(text("Waive Violations"));
    page.prioritiesTableCell(0, 5).shouldHave(text("View Violations"));
  }

  @Test
  public void testRowData_ManualPullRequestButtonStates() throws Exception {
    refresh();
    PrioritiesPage page = new PrioritiesPage();

    // a row where a manual pull request is not possible
    page.prioritiesTableCell(0, 0).shouldHave(text("1"));
    page.prioritiesTableCell(0, 1).shouldHave(text("Dorg.openid4java : openid4java : 0.9.5"));
    page.prioritiesTableCell(0, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(0, 3).shouldHave(text("Reachable"));
    page.prioritiesTableCell(0, 4).shouldHave(text("Investigate"));
    page.prioritiesTableCell(0, 5).shouldHave(text("View Violations"));

    // another row with a null automatedRemediationStatus where a manual PR should not be possible
    page.prioritiesTableCell(13, 0).shouldHave(text("14"));
    page.prioritiesTableCell(13, 1).shouldHave(text("sample-application.zip"));
    page.prioritiesTableCell(13, 2).shouldHave(text("Warn"));
    page.prioritiesTableCell(13, 3).shouldHave(text("-"));
    page.prioritiesTableCell(13, 4).shouldHave(text("Investigate"));
    page.prioritiesTableCell(13, 5).shouldHave(text("View Violations"));

    // a row where a manual pull request is possible but source control is not configured
    assertManualPullRequest("Source Control is not configured");

    // a row where a manual pull request is possible but manual pull requests is not configured
    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(false);
    lookup(SourceControlDAO.class).update(sourceControl);
    assertManualPullRequest("Manual Pull Requests are disabled");

    // a row where a manual pull request is possible and enabled
    sourceControl.setManualPullRequestsEnabled(true);
    lookup(SourceControlDAO.class).update(sourceControl);
    assertManualPullRequest(null);

    // a row where a manual pull request is possible but the license feature is missing
    setMissingFeature(LicensedFeature.AUTOMATION);
    assertManualPullRequest("Manual Pull Requests are disabled");

    // a row where a manual pull request is possible and enabled
    setMissingFeature(LicensedFeature.ALLOW_SCM_ON_PUBLIC_REPOS);
    assertManualPullRequest(null);

    // a row where a manual pull request is possible but the repository is public
    ScmRepoVisibilityService scmRepoVisibilityServiceSpy = spy(lookup(ScmRepoVisibilityService.class));
    doReturn(false).when(scmRepoVisibilityServiceSpy).isInternalRepository(any());
    mocks.put(ScmRepoVisibilityService.class, scmRepoVisibilityServiceSpy);
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": false }")));
    assertManualPullRequest("Manual Pull Requests are disabled");

    // a row where a manual pull request is possible and enabled
    mocks.clear();
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": true }")));
    assertManualPullRequest(null);
  }

  @Test
  public void testRowData_PollPRStatusOnInitialLoad_Success() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    SourceControlPullRequestService pullRequestServiceSpy = spy(lookup(SourceControlPullRequestService.class));
    mocks.put(SourceControlPullRequestService.class, pullRequestServiceSpy);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    String branchName = application.getId().substring(0, 6) + "/apache-httpclient/commons-httpclient/3.1-to-3.2";
    SourceControlEvent pullRequestEvent = new SourceControlEvent();
    pullRequestEvent.setApplicationId(application.getId());
    pullRequestEvent.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    pullRequestEvent.setEventStatusDetails("https://example.com/pull/123");
    pullRequestEvent.setPullRequestNumber(123);
    pullRequestEvent.setBranchName(branchName);
    sourceControlEventDAO.insert(pullRequestEvent);

    // loading the page
    refresh();
    PrioritiesPage page = new PrioritiesPage();

    SelenideElement prLink = page.viewPullRequestLink(8).shouldBe(visible).shouldHave(text("PR #123"));
    prLink.shouldBe(enabled);
    prLink.shouldHave(href("https://example.com/pull/123"));
  }

  @Test
  public void testRowData_PollPRStatusOnInitialLoad_ExistingEventAndNonDirectDependency_Success() throws Exception {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    SourceControlPullRequestService pullRequestServiceSpy = spy(lookup(SourceControlPullRequestService.class));
    mocks.put(SourceControlPullRequestService.class, pullRequestServiceSpy);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    String branchName = application.getId().substring(0, 6) + "/tomcat/tomcat-util/5.5.23-to-5.6.0";
    SourceControlEvent pullRequestEvent = new SourceControlEvent();
    pullRequestEvent.setApplicationId(application.getId());
    pullRequestEvent.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    pullRequestEvent.setEventStatusDetails("https://example.com/pull/123");
    pullRequestEvent.setPullRequestNumber(123);
    pullRequestEvent.setBranchName(branchName);
    sourceControlEventDAO.insert(pullRequestEvent);

    // loading the page
    mockTransitiveRemediationData();
    refresh();
    PrioritiesPage page = new PrioritiesPage();

    page.prioritiesTableCell(1, 1).shouldHave(text("Ttomcat : tomcat-util : 5.5.23"));
    page.viewViolationsLink(1).shouldBe(visible).shouldHave(text("View Violations"));
  }

  @Test
  public void testRowData_PollPRStatusOnInitialLoad_Failure() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    SourceControlPullRequestService pullRequestServiceSpy = spy(lookup(SourceControlPullRequestService.class));
    mocks.put(SourceControlPullRequestService.class, pullRequestServiceSpy);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    String branchName = application.getId().substring(0, 6) + "/apache-httpclient/commons-httpclient/3.1-to-3.2";
    SourceControlEvent pullRequestEvent = new SourceControlEvent();
    pullRequestEvent.setApplicationId(application.getId());
    pullRequestEvent.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    pullRequestEvent.setEventStatusDetails("Branch already exists.");
    pullRequestEvent.setBranchName(branchName);
    sourceControlEventDAO.insert(pullRequestEvent);

    // loading the page
    refresh();
    PrioritiesPage page = new PrioritiesPage();

    page.retryCreatePullRequestButton(8).shouldBe(visible).shouldHave(text("Retry"));
  }

  @Test
  public void testRowData_PollPRStatusOnInitialLoad_PendingAndSucceed() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    SourceControlPullRequestService pullRequestServiceSpy = spy(lookup(SourceControlPullRequestService.class));
    mocks.put(SourceControlPullRequestService.class, pullRequestServiceSpy);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    String branchName = application.getId().substring(0, 6) + "/apache-httpclient/commons-httpclient/3.1-to-3.2";
    SourceControlEvent pullRequestEvent = new SourceControlEvent();
    pullRequestEvent.setApplicationId(application.getId());
    pullRequestEvent.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_NEW);
    pullRequestEvent.setBranchName(branchName);
    sourceControlEventDAO.insert(pullRequestEvent);

    // loading the page
    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // polling should start
    createPRModal.shouldNotBe(visible);
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the pull request event from new to in progress
    pullRequestEvent = sourceControlEventDAO.getAll().get(0);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.update(pullRequestEvent);

    // wait for a new polling call
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> isPullRequestStatusCalled(pullRequestServiceSpy, 2));

    // polling should continue
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the pull request event from in progress to complete
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    pullRequestEvent.setEventStatusDetails("https://example.com/pull/123");
    pullRequestEvent.setPullRequestNumber(123);
    sourceControlEventDAO.update(pullRequestEvent);

    // polling should end
    SelenideElement prLink =
        page.viewPullRequestLink(8)
            .shouldBe(visible, TIMEOUT).shouldHave(text("PR #123"));
    prLink.shouldBe(enabled);
    prLink.shouldHave(href("https://example.com/pull/123"));
  }

  @Test
  public void testRowData_PollPRStatusOnInitialLoad_PendingAndFails() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    String branchName = application.getId().substring(0, 6) + "/apache-httpclient/commons-httpclient/3.1-to-3.2";
    SourceControlEvent pullRequestEvent = new SourceControlEvent();
    pullRequestEvent.setApplicationId(application.getId());
    pullRequestEvent.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_NEW);
    pullRequestEvent.setBranchName(branchName);
    sourceControlEventDAO.insert(pullRequestEvent);

    // loading the page
    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // polling should start
    createPRModal.shouldNotBe(visible);
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the pull request event to error
    pullRequestEvent = sourceControlEventDAO.getAll().get(0);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    pullRequestEvent.setEventStatusDetails("Branch already exists.");
    sourceControlEventDAO.update(pullRequestEvent);

    // polling should end
    page.retryCreatePullRequestButton(8).shouldBe(visible).shouldHave(text("Retry"));
  }

  @Test
  public void testRowData_PollPRStatusOnInitialLoad_AbortIfNavigatingAway() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    SourceControlPullRequestService pullRequestServiceSpy = spy(lookup(SourceControlPullRequestService.class));
    mocks.put(SourceControlPullRequestService.class, pullRequestServiceSpy);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    String branchName = application.getId().substring(0, 6) + "/apache-httpclient/commons-httpclient/3.1-to-3.2";
    SourceControlEvent pullRequestEvent = new SourceControlEvent();
    pullRequestEvent.setApplicationId(application.getId());
    pullRequestEvent.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_NEW);
    pullRequestEvent.setBranchName(branchName);
    sourceControlEventDAO.insert(pullRequestEvent);

    // loading the page
    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // polling should start
    createPRModal.shouldNotBe(visible);
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));
    verify(pullRequestServiceSpy, times(1)).getPullRequestStatus(any());

    // change the status of the pull request event from new to in progress
    pullRequestEvent = sourceControlEventDAO.getAll().get(0);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.update(pullRequestEvent);

    // wait for a new polling call
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> isPullRequestStatusCalled(pullRequestServiceSpy, 2));

    // polling should continue
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // navigates away from the page
    SidebarNavigation.productLogo().click();

    // wait to check that the polling has stopped
    Selenide.sleep(1000);

    // check no more calls were made to the pull request service
    verify(pullRequestServiceSpy, times(2)).getPullRequestStatus(any());
  }

  @Test
  public void testRowData_PollPRStatusOnInitialLoad_Retry_Success() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    String branchName = application.getId().substring(0, 6) + "/apache-httpclient/commons-httpclient/3.1-to-3.2";
    SourceControlEvent pullRequestEvent = new SourceControlEvent();
    pullRequestEvent.setApplicationId(application.getId());
    pullRequestEvent.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_NEW);
    pullRequestEvent.setBranchName(branchName);
    sourceControlEventDAO.insert(pullRequestEvent);

    // loading the page
    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // polling should start
    createPRModal.shouldNotBe(visible);
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the pull request event to error
    pullRequestEvent = sourceControlEventDAO.getAll().get(0);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    pullRequestEvent.setEventStatusDetails("Branch already exists.");
    sourceControlEventDAO.update(pullRequestEvent);

    // polling should end
    SelenideElement retryButton =
        page.retryCreatePullRequestButton(8).shouldBe(visible).shouldHave(text("Retry"));

    // retry button should create a new PR event and poll again
    retryButton.click();
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the new PR event from new to complete
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> sourceControlEventDAO.getAll().size() == 2);
    pullRequestEvent = sourceControlEventDAO.getAll().get(1);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    pullRequestEvent.setEventStatusDetails("https://example.com/pull/123");
    pullRequestEvent.setPullRequestNumber(123);
    sourceControlEventDAO.update(pullRequestEvent);

    // polling should end
    SelenideElement prLink =
        page.viewPullRequestLink(8).shouldBe(visible).shouldHave(text("PR #123"));
    prLink.shouldBe(enabled);
    prLink.shouldHave(href("https://example.com/pull/123"));
  }

  @Test
  public void testRowData_PollPRStatusOnInitialLoad_Retry_Failure() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    SourceControlPullRequestService pullRequestServiceSpy = spy(lookup(SourceControlPullRequestService.class));
    mocks.put(SourceControlPullRequestService.class, pullRequestServiceSpy);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    String branchName = application.getId().substring(0, 6) + "/apache-httpclient/commons-httpclient/3.1-to-3.2";
    SourceControlEvent pullRequestEvent = new SourceControlEvent();
    pullRequestEvent.setApplicationId(application.getId());
    pullRequestEvent.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_NEW);
    pullRequestEvent.setBranchName(branchName);
    sourceControlEventDAO.insert(pullRequestEvent);

    // loading the page
    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // polling should start
    createPRModal.shouldNotBe(visible);
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the pull request event to error
    pullRequestEvent = sourceControlEventDAO.getById(pullRequestEvent.getId());
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    pullRequestEvent.setEventStatusDetails("Branch already exists.");
    sourceControlEventDAO.update(pullRequestEvent);

    // polling should end
    SelenideElement retryButton =
        page.retryCreatePullRequestButton(8).shouldBe(visible).shouldHave(text("Retry"));

    // retry button should create a new PR event and poll again
    retryButton.click();
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // wait for the new PR event to be created
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> sourceControlEventDAO.getAll().size() == 2);

    // change the status of the pull request event from new to in progress
    pullRequestEvent = sourceControlEventDAO.getAll().stream()
        .filter(event -> SourceControlEvent.EVENT_STATUS_NEW.equals(event.getEventStatus()))
        .findFirst()
        .orElseThrow();
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.update(pullRequestEvent);

    // wait for a new polling call
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> isPullRequestStatusCalled(pullRequestServiceSpy, 3));

    // polling should continue
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the new PR event from new to error
    pullRequestEvent = sourceControlEventDAO.getById(pullRequestEvent.getId());
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    pullRequestEvent.setEventStatusDetails("Branch already exists.");
    sourceControlEventDAO.update(pullRequestEvent);

    // wait for a new polling call
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> isPullRequestStatusCalled(pullRequestServiceSpy, 4));

    // polling should end
    page.retryCreatePullRequestButton(8).shouldBe(visible).shouldHave(text("Retry"));
  }

  @Test
  public void testRowData_PollPRStatusOnInitialLoad_Retry_AbortIfNavigatingAway() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    SourceControlPullRequestService pullRequestServiceSpy = spy(lookup(SourceControlPullRequestService.class));
    mocks.put(SourceControlPullRequestService.class, pullRequestServiceSpy);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    String branchName = application.getId().substring(0, 6) + "/apache-httpclient/commons-httpclient/3.1-to-3.2";
    SourceControlEvent pullRequestEvent = new SourceControlEvent();
    pullRequestEvent.setApplicationId(application.getId());
    pullRequestEvent.setEventType(SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_NEW);
    pullRequestEvent.setBranchName(branchName);
    sourceControlEventDAO.insert(pullRequestEvent);

    // loading the page
    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // polling should start
    createPRModal.shouldNotBe(visible);
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the pull request event to error
    pullRequestEvent = sourceControlEventDAO.getAll().get(0);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    pullRequestEvent.setEventStatusDetails("Branch already exists.");
    sourceControlEventDAO.update(pullRequestEvent);

    // polling should end
    SelenideElement retryButton =
        page.retryCreatePullRequestButton(8).shouldBe(visible).shouldHave(text("Retry"));

    // retry button should create a new PR event and poll again
    retryButton.click();
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // wait for the new PR event to be created
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> sourceControlEventDAO.getAll().size() == 2);

    // change the status of the pull request event from new to in progress
    pullRequestEvent = sourceControlEventDAO.getAll().get(1);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.update(pullRequestEvent);

    // wait for a new polling call
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> isPullRequestStatusCalled(pullRequestServiceSpy, 3));

    // polling should continue
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // navigates away from the page
    SidebarNavigation.productLogo().click();

    // wait to check that the polling has stopped
    Selenide.sleep(1000);

    // check no more calls were made to the pull request service
    verify(pullRequestServiceSpy, times(3)).getPullRequestStatus(any());
  }

  @Test
  public void testCreatePRModal() {
    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    lookup(SourceControlDAO.class).update(sourceControl);

    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // click the create pull request button
    SelenideElement createPullRequestButton =
        page.createPullRequestButton(8).shouldBe(visible).shouldHave(text("Create PR"));
    createPullRequestButton.click();

    // check the modal
    createPRModal.shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPullRequestModalHeader().shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPrModalPrTitle().shouldBe(visible).shouldHave(text("Bump commons-httpclient to 3.2"));
    createPRModal.createPrModalComponentName().shouldBe(visible)
        .shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));
    createPRModal.createPrModalCurrentVersion().shouldBe(visible).shouldHave(text("3.1"));
    createPRModal.createPrModalTargetVersion().shouldBe(visible).shouldHave(text("3.2"));
    createPRModal.createPrModalBreakingChanges().shouldBe(visible).shouldHave(text("None"));
    createPRModal.createPrModalDefaultBranch().shouldBe(visible).shouldHave(text("master"));
  }

  @Test
  public void testCreatePRModal_loadBranchName() {
    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB,
        null,
        null,
        "my-branch"
    );
    sourceControl.setManualPullRequestsEnabled(true);
    lookup(SourceControlDAO.class).update(sourceControl);

    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // click the create pull request button
    SelenideElement createPullRequestButton =
        page.createPullRequestButton(8).shouldBe(visible).shouldHave(text("Create PR"));
    createPullRequestButton.click();

    // check the modal with the proper branch name
    createPRModal.shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPullRequestModalHeader().shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPrModalPrTitle().shouldBe(visible).shouldHave(text("Bump commons-httpclient to 3.2"));
    createPRModal.createPrModalComponentName().shouldBe(visible)
        .shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));
    createPRModal.createPrModalCurrentVersion().shouldBe(visible).shouldHave(text("3.1"));
    createPRModal.createPrModalTargetVersion().shouldBe(visible).shouldHave(text("3.2"));
    createPRModal.createPrModalBreakingChanges().shouldBe(visible).shouldHave(text("None"));
    createPRModal.createPrModalDefaultBranch().shouldBe(visible).shouldHave(text("my-branch"));
  }

  @Test
  public void testCreatePRModal_Cancel() {
    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    lookup(SourceControlDAO.class).update(sourceControl);

    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // click the create pull request button
    SelenideElement createPullRequestButton =
        page.createPullRequestButton(8).shouldBe(visible).shouldHave(text("Create PR"));
    createPullRequestButton.click();

    // check the modal
    createPRModal.shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPullRequestModalHeader().shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPrModalPrTitle().shouldBe(visible).shouldHave(text("Bump commons-httpclient to 3.2"));
    createPRModal.createPrModalComponentName().shouldBe(visible)
        .shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));
    createPRModal.createPrModalCurrentVersion().shouldBe(visible).shouldHave(text("3.1"));
    createPRModal.createPrModalTargetVersion().shouldBe(visible).shouldHave(text("3.2"));
    createPRModal.createPrModalBreakingChanges().shouldBe(visible).shouldHave(text("None"));
    createPRModal.createPrModalDefaultBranch().shouldBe(visible).shouldHave(text("master"));

    // click the modal's cancel button
    SelenideElement cancelButton =
        createPRModal.createPullRequestModalCancelButton().shouldBe(visible).shouldHave(text("Cancel"));
    cancelButton.click();

    // check the modal
    createPRModal.shouldNotBe(visible);
  }

  @Test
  public void testCreatePRModal_loadBranchName_ParentValue() {
    SourceControl parentSourceControl = tempEntity.newSourceControl(
        application.getParentOwnerId(),
        null,
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB,
        null,
        null,
        "my-parent-branch"
    );
    parentSourceControl.setManualPullRequestsEnabled(true);
    lookup(SourceControlDAO.class).update(parentSourceControl);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB,
        null,
        null,
        null
    );
    sourceControl.setManualPullRequestsEnabled(true);
    lookup(SourceControlDAO.class).update(sourceControl);

    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // click the create pull request button
    SelenideElement createPullRequestButton =
        page.createPullRequestButton(8).shouldBe(visible).shouldHave(text("Create PR"));
    createPullRequestButton.click();

    // check the modal with the proper branch name
    createPRModal.shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPullRequestModalHeader().shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPrModalPrTitle().shouldBe(visible).shouldHave(text("Bump commons-httpclient to 3.2"));
    createPRModal.createPrModalComponentName().shouldBe(visible)
        .shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));
    createPRModal.createPrModalCurrentVersion().shouldBe(visible).shouldHave(text("3.1"));
    createPRModal.createPrModalTargetVersion().shouldBe(visible).shouldHave(text("3.2"));
    createPRModal.createPrModalBreakingChanges().shouldBe(visible).shouldHave(text("None"));
    createPRModal.createPrModalDefaultBranch().shouldBe(visible).shouldHave(text("my-parent-branch"));
  }

  @Test
  public void testCreatePRModal_PollOnCreation_Success() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    SourceControlPullRequestService pullRequestServiceSpy = spy(lookup(SourceControlPullRequestService.class));
    mocks.put(SourceControlPullRequestService.class, pullRequestServiceSpy);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // click the create pull request button
    SelenideElement createPullRequestButton =
        page.createPullRequestButton(8).shouldBe(visible).shouldHave(text("Create PR"));
    createPullRequestButton.click();

    // check the modal
    createPRModal.shouldBe(visible).shouldHave(text("Create Pull Request"));

    // click create pr button inside the modal
    SelenideElement createPullRequestModalButton = createPRModal.createPullRequestModalCreateButton();
    createPullRequestModalButton.shouldBe(visible).shouldHave(text("Create"));
    createPullRequestModalButton.click();

    // polling should start
    createPRModal.shouldNotBe(visible);
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the pull request event from new to in progress
    SourceControlEvent pullRequestEvent = sourceControlEventDAO.getAll().get(0);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.update(pullRequestEvent);

    // wait for a new polling call
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> isPullRequestStatusCalled(pullRequestServiceSpy, 2));

    // polling should continue
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the pull request event from in progress to complete
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    pullRequestEvent.setEventStatusDetails("https://example.com/pull/123");
    pullRequestEvent.setPullRequestNumber(123);
    sourceControlEventDAO.update(pullRequestEvent);

    // polling should end
    SelenideElement prLink =
        page.viewPullRequestLink(8).shouldBe(visible).shouldHave(text("PR #123"));
    prLink.shouldBe(enabled);
    prLink.shouldHave(href("https://example.com/pull/123"));
  }

  @Test
  public void testCreatePRModal_PollOnCreation_Success_PRNumberNotSet() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    SourceControlPullRequestService pullRequestServiceSpy = spy(lookup(SourceControlPullRequestService.class));
    mocks.put(SourceControlPullRequestService.class, pullRequestServiceSpy);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // click the create pull request button
    SelenideElement createPullRequestButton =
        page.createPullRequestButton(8).shouldBe(visible).shouldHave(text("Create PR"));
    createPullRequestButton.click();

    // check the modal
    createPRModal.shouldBe(visible).shouldHave(text("Create Pull Request"));

    // click create pr button inside the modal
    SelenideElement createPullRequestModalButton = createPRModal.createPullRequestModalCreateButton();
    createPullRequestModalButton.shouldBe(visible).shouldHave(text("Create"));
    createPullRequestModalButton.click();

    // polling should start
    createPRModal.shouldNotBe(visible);
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the pull request event from new to in progress
    SourceControlEvent pullRequestEvent = sourceControlEventDAO.getAll().get(0);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.update(pullRequestEvent);

    // wait for a new polling call
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> isPullRequestStatusCalled(pullRequestServiceSpy, 2));

    // polling should continue
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the pull request event from in progress to complete
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    // the PR number is not set
    pullRequestEvent.setEventStatusDetails("https://example.com/pull/123");
    sourceControlEventDAO.update(pullRequestEvent);

    // polling should end
    // the link should still be present, but with text "View PR" instead of "PR #123"
    SelenideElement prLink =
        page.viewPullRequestLink(8).shouldBe(visible).shouldHave(text("View PR"));
    prLink.shouldBe(enabled);
    prLink.shouldHave(href("https://example.com/pull/123"));
  }

  @Test
  public void testCreatePRModal_PollOnCreation_FailureAndRetry() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    SourceControlPullRequestService pullRequestServiceSpy = spy(lookup(SourceControlPullRequestService.class));
    mocks.put(SourceControlPullRequestService.class, pullRequestServiceSpy);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // click the create pull request button
    SelenideElement createPullRequestButton =
        page.createPullRequestButton(8).shouldBe(visible).shouldHave(text("Create PR"));
    createPullRequestButton.click();

    // check the modal
    createPRModal.shouldBe(visible).shouldHave(text("Create Pull Request"));

    // click create pr button inside the modal
    SelenideElement createPullRequestModalButton = createPRModal.createPullRequestModalCreateButton();
    createPullRequestModalButton.shouldBe(visible).shouldHave(text("Create"));
    createPullRequestModalButton.click();

    // polling should start
    createPRModal.shouldNotBe(visible);
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the pull request event to error
    SourceControlEvent pullRequestEvent = sourceControlEventDAO.getAll().stream()
        .filter(event -> SourceControlEvent.EVENT_STATUS_NEW.equals(event.getEventStatus()))
        .findFirst()
        .orElseThrow();
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_ERROR);
    pullRequestEvent.setEventStatusDetails("Branch already exists.");
    sourceControlEventDAO.update(pullRequestEvent);

    // polling should end
    SelenideElement retryButton =
        page.retryCreatePullRequestButton(8).shouldBe(visible).shouldHave(text("Retry"));

    // retry button should create a new PR event and poll again
    retryButton.click();
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // change the status of the new PR event from new to complete
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> sourceControlEventDAO.getAll().size() == 2);
    pullRequestEvent = sourceControlEventDAO.getAll().stream()
        .filter(event -> SourceControlEvent.EVENT_STATUS_NEW.equals(event.getEventStatus()))
        .findFirst()
        .orElseThrow();
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    pullRequestEvent.setEventStatusDetails("https://example.com/pull/123");
    pullRequestEvent.setPullRequestNumber(123);
    sourceControlEventDAO.update(pullRequestEvent);

    // wait for a new polling call
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> isPullRequestStatusCalled(pullRequestServiceSpy, 4));

    // polling should end
    SelenideElement prLink =
        page.viewPullRequestLink(8).shouldBe(visible).shouldHave(text("PR #123"));
    prLink.shouldBe(enabled);
    prLink.shouldHave(href("https://example.com/pull/123"));
  }

  @Test
  public void testCreatePRModal_PollOnCreation_AbortIfNavigatingAway() {
    SourceControlDAO sourceControlDAO = lookup(SourceControlDAO.class);
    SourceControlEventDAO sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    SourceControlPullRequestService pullRequestServiceSpy = spy(lookup(SourceControlPullRequestService.class));
    mocks.put(SourceControlPullRequestService.class, pullRequestServiceSpy);

    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    // click the create pull request button
    SelenideElement createPullRequestButton =
        page.createPullRequestButton(8).shouldBe(visible).shouldHave(text("Create PR"));
    createPullRequestButton.click();

    // check the modal
    createPRModal.shouldBe(visible).shouldHave(text("Create Pull Request"));

    // click create pr button inside the modal
    SelenideElement createPullRequestModalButton = createPRModal.createPullRequestModalCreateButton();
    createPullRequestModalButton.shouldBe(visible).shouldHave(text("Create"));
    createPullRequestModalButton.click();

    // polling should start
    createPRModal.shouldNotBe(visible);
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));
    verify(pullRequestServiceSpy, times(1)).getPullRequestStatus(any());

    // change the status of the pull request event from new to in progress
    SourceControlEvent pullRequestEvent = sourceControlEventDAO.getAll().get(0);
    pullRequestEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.update(pullRequestEvent);

    // wait for a new polling call
    Wait()
        .withTimeout(TIMEOUT)
        .pollingEvery(POLL_FREQUENCY)
        .until(webDriver -> isPullRequestStatusCalled(pullRequestServiceSpy, 2));

    // polling should continue
    page.pullRequestCreationLoadingSpinner(8).shouldBe(visible).shouldHave(text("Creating PR…"));

    // navigates away from the page
    SidebarNavigation.productLogo().click();

    // wait to check that the polling has stopped
    Selenide.sleep(1000);

    // check no more calls were made to the pull request service
    verify(pullRequestServiceSpy, times(2)).getPullRequestStatus(any());
  }

  @Test
  public void testViewViolationsLink() {
    refresh();
    PrioritiesPage page = new PrioritiesPage();

    page.prioritiesTableCell(0, 5).shouldBe(visible);
    page.viewViolationsLink(0).shouldBe(visible).shouldHave(text("View Violations"));

    page.viewViolationsLink(0).click();

    ComponentDetailsPage.title().shouldHave(text("org.openid4java : openid4java : 0.9.5"));

    ComponentDetailsPage detailsPage = new ComponentDetailsPage();
    detailsPage.violationsTab().shouldHave(cssClass("active"));
  }

  @Test
  public void testRowData_InnerSourceDirectComponentWithRemediation() {
    ComponentIdentifier innersourceDirectComponent =
        ComponentIdentifier.createMavenCoordinates("org.jclouds.driver", "jclouds-enterprise", "1.3.1", "", "jar");

    PackageUrlIdentifier versionlessPurl = InnerSourceUtils.getVersionlessPackageUrl(innersourceDirectComponent);
    InnerSourceApplication innerSourceApplication = innerSourceApplicationDAO.getByPackageUrl(versionlessPurl);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.4.0", StageTypes.RELEASE.getId());

    ComponentIdentifier innersourceTransitiveComponent =
        ComponentIdentifier.createMavenCoordinates("com.sonatype.insight.scan", "insight-scanner-archive", "1.0.0", "",
            "jar");

    PackageUrlIdentifier transitiveVersionlessPurl =
        InnerSourceUtils.getVersionlessPackageUrl(innersourceTransitiveComponent);
    InnerSourceApplication transitiveInnerSourceApplication =
        tempEntity.newInnerSourceApplication(transitiveVersionlessPurl.getPackageUrl(), application);
    tempEntity.newInnerSourceVersion(transitiveInnerSourceApplication, "1.1.0", StageTypes.RELEASE.getId());

    refresh();
    PrioritiesPage page = new PrioritiesPage();
    page.lastPageLink().shouldBe(visible).click();
    //the transitive component should not be shown in the priorities table, so it has only 2 rows
    page.prioritiesTableRows().shouldHave(size(2));
    page.prioritiesTableCell(1, 0).shouldHave(text("17"));
    page.prioritiesTableCell(1, 1).shouldHave(text("org.jclouds.driver : jclouds-enterprise : 1.3.1"));
    page.directDependencyIndicator(1).shouldBe(visible);
    page.innerSourceDependencyIndicator(1).shouldBe(visible);
    page.prioritiesTableCell(1, 4).shouldHave(text("Upgrade to 1.4.0"));
  }

  @Test
  public void testCreatePRModal_InnerSourceDirectComponentWithRemediation_CreatePR() throws Exception {
    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
    sourceControl.setManualPullRequestsEnabled(true);
    lookup(SourceControlDAO.class).update(sourceControl);
    mockInnerSourceRemediationData();
    refresh();
    PrioritiesPage page = new PrioritiesPage();
    CreatePRModal createPRModal = new CreatePRModal();

    page.lastPageLink().shouldBe(visible).click();

    //verify the direct component is shown in the priorities table
    page.prioritiesTableCell(1, 0).shouldHave(text("17"));
    page.prioritiesTableCell(1, 1).shouldHave(text("org.jclouds.driver : jclouds-enterprise : 1.3.1"));
    page.directDependencyIndicator(1).shouldBe(visible);
    page.innerSourceDependencyIndicator(1).shouldBe(visible);
    page.prioritiesTableCell(1, 4).shouldHave(text("Upgrade to 1.4.0"));
    //click the create pull request button
    SelenideElement createPullRequestButton =
        page.createPullRequestButton(1).shouldBe(visible).shouldHave(text("Create PR"));
    createPullRequestButton.click();

    //check the modal content
    createPRModal.shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPullRequestModalHeader().shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPrModalPrTitle().shouldBe(visible).shouldHave(text("Bump jclouds-enterprise to 1.4.0"));
    createPRModal.createPrModalComponentName().shouldBe(visible)
        .shouldHave(text("org.jclouds.driver : jclouds-enterprise : 1.3.1"));
    createPRModal.createPrModalCurrentVersion().shouldBe(visible).shouldHave(text("1.3.1"));
    createPRModal.createPrModalTargetVersion().shouldBe(visible).shouldHave(text("1.4.0"));
    createPRModal.createPrModalBreakingChanges().shouldBe(visible).shouldHave(text("None"));
    createPRModal.createPrModalDefaultBranch().shouldBe(visible).shouldHave(text("master"));
  }

  private boolean isPullRequestStatusCalled(final SourceControlPullRequestService pullRequestServiceSpy, int times) {
    try {
      verify(pullRequestServiceSpy, times(times)).getPullRequestStatus(any());
      return true;
    }
    catch (AssertionError e) {
      return false;
    }
  }

  private void assertManualPullRequest(final String expectedTooltipText) throws Exception {
    refresh();
    PrioritiesPage page = new PrioritiesPage();
    page.prioritiesTableCell(8, 0).shouldHave(text("9"));
    page.prioritiesTableCell(8, 1).shouldHave(text("Dapache-httpclient : commons-httpclient : 3.1"));
    page.prioritiesTableCell(8, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(8, 3).shouldHave(text("-"));
    page.prioritiesTableCell(8, 4).shouldHave(text("Upgrade to 3.2"));
    page.prioritiesTableCell(8, 5).shouldHave(text("Create PR"));
    SelenideElement createPullRequestButton =
        page.createPullRequestButton(8).shouldBe(visible).shouldHave(text("Create PR"));
    if (expectedTooltipText == null) {
      createPullRequestButton.shouldNotHave(cssClass("disabled"));
      createPullRequestButton.hover();
      Thread.sleep(500);
      Tooltip.get().shouldNot(exist);
    }
    else {
      createPullRequestButton.shouldHave(cssClass("disabled"));
      createPullRequestButton.hover();
      Tooltip.get().shouldBe(visible).shouldHave(text(expectedTooltipText));
    }
  }

  private ImmutablePair<Application, String> setUpAppsWithPriorities() throws IOException {
    return setupMainApp();
  }

  private ImmutablePair<Application, String> setupMainApp() throws IOException {
    String scanId = "scanId";

    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication("appName", "appId", org.getId());

    setupPolicies(org, app);
    evaluateScan(app, scanId);

    ComponentIdentifier c1 =
        ComponentIdentifier.createMavenCoordinates("org.openid4java", "openid4java", "0.9.5", "", "jar");
    mockReachability(app, scanId, c1, ReachabilityStatus.REACHABLE);

    ComponentIdentifier c2 =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar");
    mockReachability(app, scanId, c2, ReachabilityStatus.NON_REACHABLE);

    return ImmutablePair.of(app, scanId);
  }

  private void setupPolicies(Organization org, Application app) throws IOException {
    PolicyExportResult referencePolicies;
    try (var referencePolicyStream = getClass().getResourceAsStream("/reference-policies-v3-with-build-fail.json")) {
      referencePolicies = JsonUtils.parse(referencePolicyStream, PolicyExportResult.class);
    }

    lookup(PolicyImportExport.class).importOrganization(org, referencePolicies);

    // set up the Component-Unknown policy to warn on build
    Policy componentUnknownPolicy = policyDAO.getByName("Component-Unknown").get(0);
    componentUnknownPolicy.setPolicyActionsOverrideAllowed(true);
    componentUnknownPolicy.addPolicyActionsOverride(app.getId(), Map.of("build", "warn"));
    policyDAO.update(componentUnknownPolicy);

    // set up the Component-Similar policy to have no action on build
    Policy componentSimilarPolicy = policyDAO.getByName("Component-Similar").get(0);
    componentSimilarPolicy.setPolicyActionsOverrideAllowed(true);
    componentSimilarPolicy.addPolicyActionsOverride(app.getId(), Map.of());
    policyDAO.update(componentSimilarPolicy);
  }

  private void evaluateScan(Application app, String scanId) throws IOException {
    URL zippedReport = ReportHelper.zipReport("/canned-reports/report-with-dependency-tree", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator evaluator =
        new TestReportEvaluator(app, scanId, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
  }

  /**
   * there's no way to submit reachability analysis through TestReportEvaluator, so instead we just hack up the policy
   * results to include a positive reachability status afterwards
   */
  private void mockReachability(
      Application app,
      String scanId,
      ComponentIdentifier componentIdentifier,
      ReachabilityStatus reachabilityStatus) throws IOException
  {
    PolicyThreats policyThreats;
    ReportEntity policyThreatsReportEntity =
        applicationReportPersistenceService.getReportEntity(app.getId(), scanId, "policythreats.json");

    try (var stream = policyThreatsReportEntity.getInputStream()) {
      policyThreats = JsonUtils.parse(stream, PolicyThreats.class);
    }

    PolicyThreats.Component component = policyThreats.aaData.stream()
        .filter(c ->
            componentIdentifier == null ?
                c.componentIdentifier == null : componentIdentifier.equals(c.componentIdentifier)
        )
        .findAny()
        .get();

    component.activeViolations.forEach(v -> v.reachabilityStatus = reachabilityStatus);

    try (var stream = policyThreatsReportEntity.getOutputStream()) {
      JsonUtils.write(stream, policyThreats);
    }
  }

  private void mockTransitiveRemediationData() throws Exception {
    ComponentIdentifier tomcatUtilCoordFromReport =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.5.23", "", "jar");
    ComponentIdentifier tomcatUtilCoordNonFailing =
        ComponentIdentifier.createMavenCoordinates("tomcat", "tomcat-util", "5.6.0", "", "jar");

    ComponentDetails fromReport = createComponentDetailsForSecurityViolation(tomcatUtilCoordFromReport);
    ComponentDetails nonFailing = createComponentDetailsForNoViolation(tomcatUtilCoordNonFailing);
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(List.of(fromReport, nonFailing));

    testCLMServer.getHdsServer().respondWith(detailsList).atUri("rest/ci/componentDetails/list");

    testCLMServer.getHdsServer().respondWith(List.of()).atUri(HDS_BULK_SCORE_VERSIONING_PATH);

    testCLMServer.getHdsServer().respondWith(ComponentSummary.create(true)).atUri(
        UriBuilder.fromPath("rest/component/summary")
            .queryParam("componentIdentifier",
                URLEncoder.encode(new ObjectMapper().writeValueAsString(tomcatUtilCoordFromReport), "UTF-8"))
            .build()
    );

    testCLMServer.getHdsServer().respondWith(new ComponentDependenciesDTO(Map.of(), Map.of()))
        .atUri("rest/component/dependencies");

    VersionScoringDTO versionScoringDTO = new VersionScoringDTO();
    versionScoringDTO.setComponentIdentifier(tomcatUtilCoordFromReport);
    versionScoringDTO.setVersionScore(0);
    versionScoringDTO.setMaxSeverity(5.0d);
    VersionScoringDTO.ToVersionData toVersionData = new ToVersionData();
    toVersionData.setBreakingChangeCount(0);
    versionScoringDTO.setToVersionsNonBreaking(Map.of("5.6.0", toVersionData));
    testCLMServer.getHdsServer().respondWith(new VersionScoringDTO[]{versionScoringDTO})
        .atUri("rest/component/version-scoring/list");
  }

  private void mockRemediationData() throws Exception {
    ComponentIdentifier logbackAccessCoordFromReport =
        ComponentIdentifier.createMavenCoordinates("apache-httpclient", "commons-httpclient", "3.1", "", "jar");
    ComponentIdentifier logbackAccessCoordNonFailing =
        ComponentIdentifier.createMavenCoordinates("apache-httpclient", "commons-httpclient", "3.2", "", "jar");

    ComponentDetails fromReport = createComponentDetailsForSecurityViolation(logbackAccessCoordFromReport);
    ComponentDetails nonFailing = createComponentDetailsForNoViolation(logbackAccessCoordNonFailing);
    ComponentDetailsList detailsList = new ComponentDetailsList();
    detailsList.setList(List.of(fromReport, nonFailing));

    testCLMServer.getHdsServer().respondWith(detailsList).atUri("rest/ci/componentDetails/list");

    testCLMServer.getHdsServer().respondWith(List.of()).atUri(HDS_BULK_SCORE_VERSIONING_PATH);

    testCLMServer.getHdsServer().respondWith(ComponentSummary.create(true)).atUri(
        UriBuilder.fromPath("rest/component/summary")
            .queryParam("componentIdentifier",
                URLEncoder.encode(new ObjectMapper().writeValueAsString(logbackAccessCoordFromReport), "UTF-8"))
            .build()
    );

    testCLMServer.getHdsServer().respondWith(new ComponentDependenciesDTO(Map.of(), Map.of()))
        .atUri("rest/component/dependencies");

    VersionScoringDTO versionScoringDTO = new VersionScoringDTO();
    versionScoringDTO.setComponentIdentifier(logbackAccessCoordFromReport);
    versionScoringDTO.setVersionScore(0);
    versionScoringDTO.setMaxSeverity(5.0d);
    VersionScoringDTO.ToVersionData toVersionData = new ToVersionData();
    toVersionData.setBreakingChangeCount(0);
    versionScoringDTO.setToVersionsNonBreaking(Map.of("3.2", toVersionData));
    testCLMServer.getHdsServer().respondWith(new VersionScoringDTO[]{versionScoringDTO})
        .atUri("rest/component/version-scoring/list");
  }

  private void mockInnerSourceRemediationData() throws Exception {
    ComponentIdentifier innersourceDirectComponent =
        ComponentIdentifier.createMavenCoordinates("org.jclouds.driver", "jclouds-enterprise", "1.3.1", "", "jar");

    PackageUrlIdentifier versionlessPurl = InnerSourceUtils.getVersionlessPackageUrl(innersourceDirectComponent);
    InnerSourceApplication innerSourceApplication = innerSourceApplicationDAO.getByPackageUrl(versionlessPurl);
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.4.0", StageTypes.RELEASE.getId());

    // Add remediation data for the inner source component
    try {
      ComponentIdentifier innerSourceCoordNonFailing =
          ComponentIdentifier.createMavenCoordinates("org.jclouds.driver", "jclouds-enterprise", "1.4.0", "", "jar");

      ComponentDetails fromReport = createComponentDetailsForSecurityViolation(innersourceDirectComponent);
      ComponentDetails nonFailing = createComponentDetailsForNoViolation(innerSourceCoordNonFailing);
      ComponentDetailsList detailsList = new ComponentDetailsList();
      detailsList.setList(List.of(fromReport, nonFailing));

      testCLMServer.getHdsServer().respondWith(detailsList).atUri("rest/ci/componentDetails/list");

      testCLMServer.getHdsServer().respondWith(ComponentSummary.create(true)).atUri(
          UriBuilder.fromPath("rest/component/summary")
              .queryParam("componentIdentifier",
                  URLEncoder.encode(new ObjectMapper().writeValueAsString(innersourceDirectComponent), "UTF-8"))
              .build()
      );

      VersionScoringDTO versionScoringDTO = new VersionScoringDTO();
      versionScoringDTO.setComponentIdentifier(innersourceDirectComponent);
      versionScoringDTO.setVersionScore(0);
      versionScoringDTO.setMaxSeverity(5.0d);
      VersionScoringDTO.ToVersionData toVersionData = new ToVersionData();
      toVersionData.setBreakingChangeCount(0);
      versionScoringDTO.setToVersionsNonBreaking(Map.of("1.4.0", toVersionData));
      testCLMServer.getHdsServer().respondWith(new VersionScoringDTO[]{versionScoringDTO})
          .atUri("rest/component/version-scoring/list");
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ComponentDetails createComponentDetailsForNoViolation(final ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = new ComponentDetails();
    componentDetails.setBreakingChangesCount(0);
    componentDetails.setComponentIdentifier(componentIdentifier);
    return componentDetails;
  }

  private ComponentDetails createComponentDetailsForSecurityViolation(final ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = createComponentDetailsForNoViolation(componentIdentifier);
    componentDetails.setLicenseThreatLevel(5);
    componentDetails
        .setSecurityVulnerabilities(List.of(new SecurityVulnerability("ref", "source", 5f)));
    return componentDetails;
  }
}
