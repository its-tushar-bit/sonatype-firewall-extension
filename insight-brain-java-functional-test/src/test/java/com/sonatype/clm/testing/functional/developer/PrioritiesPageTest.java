/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.developer;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO.ToVersionData;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.PrioritiesPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.ScmRepoVisibilityService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.ReportEntity;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;

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
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class PrioritiesPageTest
    extends AbstractFunctionalTest
{
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

    ImmutablePair<Application, String> appAndScanId = setUpAppsWithPriorities();
    application = appAndScanId.getLeft();
    mockRemediationData();
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

    // a row with reachability detected
    page.prioritiesTableCell(0, 0).shouldHave(text("1"));
    page.prioritiesTableCell(0, 1).shouldHave(text("Dorg.openid4java : openid4java : 0.9.5"));
    page.prioritiesTableCell(0, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(0, 3).shouldHave(text("Detected"));
    page.prioritiesTableCell(0, 4).shouldHave(text("Investigate"));

    // a row with a transitive violation
    page.prioritiesTableCell(1, 0).shouldHave(text("2"));
    page.prioritiesTableCell(1, 1).shouldHave(text("Ttomcat : tomcat-util : 5.5.23"));
    page.prioritiesTableCell(1, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(1, 3).shouldHave(text("Not detected"));
    page.prioritiesTableCell(1, 4).shouldHave(text("Investigate"));

    // a row with an upgrade path
    page.prioritiesTableCell(8, 0).shouldHave(text("9"));
    page.prioritiesTableCell(8, 1).shouldHave(text("Dapache-httpclient : commons-httpclient : 3.1"));
    page.prioritiesTableCell(8, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(8, 3).shouldHave(text("Not detected"));
    page.prioritiesTableCell(8, 4).shouldHave(text("Upgrade to 3.2"));

    // a row with a Warn action
    page.prioritiesTableCell(13, 0).shouldHave(text("14"));
    page.prioritiesTableCell(13, 1).shouldHave(text("sample-application.zip"));
    page.prioritiesTableCell(13, 2).shouldHave(text("Warn"));
    page.prioritiesTableCell(13, 3).shouldHave(text("Not detected"));
    page.prioritiesTableCell(13, 4).shouldHave(text("Investigate"));

    // a row with no action
    page.prioritiesTableCell(14, 0).shouldHave(text("15"));
    page.prioritiesTableCell(14, 1).shouldHave(text("org.apache.lucene : lucene-spellchecker : 2.9.0"));
    page.prioritiesTableCell(14, 2).shouldBe(empty);
    page.prioritiesTableCell(14, 3).shouldHave(text("Not detected"));
    page.prioritiesTableCell(14, 4).shouldHave(text("Investigate"));
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
    page.prioritiesTableCell(0, 3).shouldHave(text("Not detected"));
    page.prioritiesTableCell(0, 4).shouldHave(text("Investigate"));
  }

  @Test
  public void testManualPullRequestsDisabled() {
    PrioritiesPage page = new PrioritiesPage();
    page.prioritiesTableCell(0, 4).shouldBe(visible);
    page.prioritiesTableCell(0, 5).shouldNot(exist);
  }

  @Test
  public void testManualPullRequestsEnabled() {
    SystemConfigurationPropertyFeature.MANUAL_PULL_REQUESTS.setEnabled(true);
    refresh();
    PrioritiesPage page = new PrioritiesPage();
    page.prioritiesTableCell(0, 4).shouldBe(visible);
    page.prioritiesTableCell(0, 5).shouldBe(visible);
  }

  @Test
  public void testRowData_ManualPullRequestsEnabled() throws Exception {
    SystemConfigurationPropertyFeature.MANUAL_PULL_REQUESTS.setEnabled(true);
    refresh();
    PrioritiesPage page = new PrioritiesPage();

    // a row where a manual pull request is not possible
    page.prioritiesTableCell(0, 0).shouldHave(text("1"));
    page.prioritiesTableCell(0, 1).shouldHave(text("Dorg.openid4java : openid4java : 0.9.5"));
    page.prioritiesTableCell(0, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(0, 3).shouldHave(text("Detected"));
    page.prioritiesTableCell(0, 4).shouldHave(text("Investigate"));
    page.prioritiesTableCell(0, 5).shouldHave(text("—"));
    
    // another row with a null automatedRemediationStatus where a manual PR should not be possible
    page.prioritiesTableCell(13, 0).shouldHave(text("14"));
    page.prioritiesTableCell(13, 1).shouldHave(text("sample-application.zip"));
    page.prioritiesTableCell(13, 2).shouldHave(text("Warn"));
    page.prioritiesTableCell(13, 3).shouldHave(text("Not detected"));
    page.prioritiesTableCell(13, 4).shouldHave(text("Investigate"));
    page.prioritiesTableCell(13, 5).shouldHave(text("—"));

    // a row where a manual pull request is possible but source control is not configured
    assertManualPullRequest("Source Control is not configured");

    // a row where a manual pull request is possible but manual pull requests is not configured
    SourceControl sourceControl = tempEntity.newSourceControl(
        application.getId(),
        gitService.baseUrl() + "/someOrg/someRepo",
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB
    );
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

  private void assertManualPullRequest(final String expectedTooltipText) throws Exception {
    refresh();
    PrioritiesPage page = new PrioritiesPage();
    page.prioritiesTableCell(8, 0).shouldHave(text("9"));
    page.prioritiesTableCell(8, 1).shouldHave(text("Dapache-httpclient : commons-httpclient : 3.1"));
    page.prioritiesTableCell(8, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(8, 3).shouldHave(text("Not detected"));
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
    mockReachableComponent(app, scanId);

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
  private void mockReachableComponent(Application app, String scanId) throws IOException {
    PolicyThreats policyThreats;
    ReportEntity policyThreatsReportEntity =
        applicationReportPersistenceService.getReportEntity(app.getId(), scanId, "policythreats.json");

    try (var stream = policyThreatsReportEntity.getInputStream()) {
      policyThreats = JsonUtils.parse(stream, PolicyThreats.class);
    }

    PolicyThreats.Component openId4Java = policyThreats.aaData.stream()
        .filter(component ->
            component.componentIdentifier != null &&
            component.componentIdentifier.getCoordinates().get("artifactId").equals("openid4java")
        )
        .findAny()
        .get();

    openId4Java.activeViolations.get(0).reachabilityStatus = ReachabilityStatus.REACHABLE;

    try (var stream = policyThreatsReportEntity.getOutputStream()) {
      JsonUtils.write(stream, policyThreats);
    }
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
    testCLMServer.getHdsServer().respondWith(new VersionScoringDTO[] {versionScoringDTO})
        .atUri("rest/component/version-scoring/list");
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
