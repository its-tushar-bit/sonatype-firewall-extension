/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile.CompareVersionsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile.VersionExplorerSection;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class VersionGraphTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  public static final String FIRST_COMPONENT_HASH = "3dd9bcf103185593d87e";

  public static final String SECOND_COMPONENT_HASH = "7ebd60d15eec1f9e796d";

  public static final String BABEL_COMPONENT_HASH = "a983fb1aeb2ec3f6ed04";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.setEnabled(false);
    Organization org = tempEntity.newOrganization("ApplicationReportTest");
    app = tempEntity.newApplicationWithSpecificId("8bbaa746602142d9adf2de00a9ca4d4a", "ApplicationReportTest",
        "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/version-graph-test-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);

    createPolicy(app.getId(), 10, "SecurityPolicy", SecurityVulnerabilitySeverityConditionType.ID, ">", "7");

    evaluator.evaluatePolicy();

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  /**
   * tests CLM-29651
   */
  @Test
  public void testVersionGraph_debugComponent_CompareVersionButton() {
    mockHdsResponseForFirstComponent();
    mockHdsResponseForFirstComponentVersionList();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);

    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Version Explorer"));

    VersionExplorerSection versionExplorerSection = riskRemediation.versionExplorerSection();
    versionExplorerSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(versionExplorerSection.content());
    versionExplorerSection.content().shouldBe(visible);
    eyesWatcher.eyesCheck("Version Graph - debug component - initial render");

    RiskRemediationTile.RecommendedVersionsSection recommendedVersionsSection = riskRemediation
        .recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);
    SelenideElement compareButton = recommendedVersionsSection.getRecommendation(0).actions().first();
    mockHdsResponseForFirstComponentRecommendedVersion();
    compareButton.click();

    CompareVersionsTable table = riskRemediation.compareVersionsTable();
    table.shouldBe(visible);
    table.versionRow().get(1).shouldHave(text("3.0.0"));
    table.versionRow().get(2).shouldHave(text("3.2.7"));

    eyesWatcher.eyesCheck("Version Graph - debug component - recommended version selected");
  }

  /**
   * tests CLM-29546
   */
  @Test
  public void testVersionGraph_babel_plugin_syntax_showDefaultVersionBar() {
    mockHdsResponseForBabelComponent();
    mockHdsResponseForBabelComponentVersionList();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(3, BABEL_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);
    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);

    VersionExplorerSection versionExplorerSection = riskRemediation.versionExplorerSection();
    versionExplorerSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(versionExplorerSection.content());
    versionExplorerSection.content().shouldBe(visible);
    eyesWatcher.eyesCheck("Version Graph - @babel/plugin-syntax-async-generators component - initial render");
  }

  /**
   * tests CLM-29738
   */
  @Test
  public void testVersionGraph_postgresqlComponent_CompareVersionButton() {
    mockHdsResponseForSecondComponent();
    mockHdsResponseForSecondComponentVersionList();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(1, SECOND_COMPONENT_HASH);

    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Version Explorer"));

    VersionExplorerSection versionExplorerSection = riskRemediation.versionExplorerSection();
    versionExplorerSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(versionExplorerSection.content());
    versionExplorerSection.content().shouldBe(visible);
    eyesWatcher.eyesCheck("Version Graph - postgresql component - initial render");

    RiskRemediationTile.RecommendedVersionsSection recommendedVersionsSection = riskRemediation
        .recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);
    SelenideElement compareButton = recommendedVersionsSection.getRecommendation(0).actions().get(1);
    mockHdsResponseForSecondComponentRecommendedVersion();
    compareButton.click();

    CompareVersionsTable table = riskRemediation.compareVersionsTable();
    table.shouldBe(visible);
    table.versionRow().get(1).shouldHave(text("42.2.2"));
    table.versionRow().get(2).shouldHave(text("42.2.15.jre6"));

    eyesWatcher.eyesCheck("Version Graph - postgresql component - recommended version selected");
  }

  private ComponentDetailsPage openComponentDetailsPageForViolation(int violationIndex, String hash) {
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement violation = violations.get(violationIndex);
    violation.shouldBe(visible);
    violation.click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, hash));
    return new ComponentDetailsPage();
  }

  private void createPolicy(
      String ownerId,
      int threatLevel,
      String name,
      String conditionType,
      String operator,
      String value)
  {
    Policy p = new Policy(null, name);
    p.setThreatLevel(threatLevel);
    p.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, name + " constraint", LogicalOperator.AND);
    com.sonatype.insight.brain.model.policy.Condition condition = new com.sonatype.insight.brain.model.policy.Condition(
        conditionType, operator, value);
    constraint.setConditions(Collections.singletonList(condition));
    p.setConstraints(Collections.singletonList(constraint));
    tempEntity.newPolicy(p);
  }

  private void mockHdsResponseForFirstComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/debugComponentDetails-3.0.0.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
  }

  private void mockHdsResponseForBabelComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/componentDetailsPluginSyntaxAsyncGenerators-7.8.4.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");

  }

  private void mockHdsResponseForBabelComponentVersionList() {
    testCLMServer.getHdsServer()
        .respondWith(
            getClass().getResource("/componentDetails/babelComponentVersionList7.8.4.json"))
        .atUri("rest/ci/componentDetails/list");
  }

  private void mockHdsResponseForFirstComponentRecommendedVersion() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/debugComponentDetails-3.2.7.json"))
        .atUri("rest/ci/componentDetails");
  }

  private void mockHdsResponseForFirstComponentVersionList() {
    testCLMServer.getHdsServer()
        .respondWith(
            getClass().getResource("/componentDetails/debugComponentVersionsList.json"))
        .atUri("rest/ci/componentDetails/list");
  }

  private void mockHdsResponseForSecondComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/postgresqlComponentDetails-42.2.2.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
  }

  private void mockHdsResponseForSecondComponentRecommendedVersion() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/postgresqlComponentDetails-42.2.15.jre6.json"))
        .atUri("rest/ci/componentDetails");
  }

  private void mockHdsResponseForSecondComponentVersionList() {
    testCLMServer.getHdsServer()
        .respondWith(
            getClass().getResource("/componentDetails/postgresqlComponentVersionsList.json"))
        .atUri("rest/ci/componentDetails/list");
  }
}
