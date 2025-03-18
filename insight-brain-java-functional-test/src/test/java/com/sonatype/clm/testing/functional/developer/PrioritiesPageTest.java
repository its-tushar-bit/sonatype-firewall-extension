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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.PrioritiesPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.ReportEntity;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class PrioritiesPageTest
    extends AbstractFunctionalTest
{
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

    // a row with an upgrade path
    page.prioritiesTableCell(1, 0).shouldHave(text("2"));
    page.prioritiesTableCell(1, 1).shouldHave(text("Ttomcat : tomcat-util : 5.5.23"));
    page.prioritiesTableCell(1, 2).shouldHave(text("Fail"));
    page.prioritiesTableCell(1, 3).shouldHave(text("Not detected"));
    page.prioritiesTableCell(1, 4).shouldHave(text("Upgrade to 5.6.0"));

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
  }

  private ComponentDetails createComponentDetailsForNoViolation(final ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = new ComponentDetails();
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
