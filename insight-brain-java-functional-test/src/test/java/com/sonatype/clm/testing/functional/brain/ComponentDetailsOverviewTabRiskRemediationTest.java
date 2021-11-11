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
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile.RecommendedRemediationSection;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile.RecommendationElement;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile.RecommendedVersionsSection;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile.VersionExplorerSection;
import com.sonatype.clm.testing.functional.elements.componentdetails.VersionGraph;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage.ComponentDetailsFooter;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.*;

public class ComponentDetailsOverviewTabRiskRemediationTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  public static final String FIRST_COMPONENT_HASH = "9aba4af169a1a3baa67f";

  public static final String SECOND_COMPONENT_HASH = "47b6857af4a1cc50875a";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    testCLMServer.getCLMServer().getConfiguration()
        .setFeatures(ImmutableMap.of(Feature.INNER_SOURCE_TRANSITIVE_WAIVER.getFlag(), false));
    Organization org = tempEntity.newOrganization("ApplicationReportTest");
    app = tempEntity.newApplicationWithSpecificId("8bbaa746602142d9adf2de00a9ca4d4a", "ApplicationReportTest",
        "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    // add Security policy
    createPolicy(app.getId(), 10, "SecurityPolicy", SecurityVulnerabilitySeverityConditionType.ID, "=", "9.1");
    createPolicy(app.getId(), 3, "Security-Low", SecurityVulnerabilitySeverityConditionType.ID, "=", "4.3");
    // add License policy
    createPolicy(app.getId(), 5, "LicensePolicy", LicenseThreatGroupLevelConditionType.ID, ">=", "9");
    // add Quality policy
    createPolicy(app.getId(), 2, "QualityPolicy", RelativePopularityConditionType.ID, "<=", "1");
    // add Other policy
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 1, "CoordinatesPolicy", CoordinatesConditionType.ID, "match",
        "maven:javancss*");

    evaluator.evaluatePolicy();

    setupHdsResponseForVersionList();

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @Test
  public void testRiskRemediationTile_Version_Graph_Explorer() {
    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0,FIRST_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Risk Remediation"));

    VersionExplorerSection versionExplorerSection = riskRemediation.versionExplorerSection();
    versionExplorerSection.shouldBe(visible);
    versionExplorerSection.getTitle().shouldHave(text("Version Explorer"));
    ScrollUtil.scrollIntoView(versionExplorerSection.content());
    versionExplorerSection.content().shouldBe(visible);

  }

  @Test
  public void testRiskRemediationTile_RecommendedVersions_Multiple_Remediation() {
    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0,FIRST_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Risk Remediation"));

    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedVersionsSection.content());
    recommendedVersionsSection.getTitle().shouldHave(text("Recommended Versions"));
    ElementsCollection recommendedVersions = recommendedVersionsSection.contentRecommendedVersionsList();
    recommendedVersions.shouldHaveSize(3);

    RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.shouldBe(visible);
    recommendation.text().shouldHave(text("Upgrade to 31.52"));
    recommendation.subText().shouldHave(text("Next version with no policy violation"));
    recommendation.actions().shouldHaveSize(1);

    recommendation = recommendedVersionsSection.getRecommendation(1);
    recommendation.shouldBe(visible);
    recommendation.text().shouldHave(text("Upgrade to 31.52"));
    recommendation.subText().shouldHave(
        text("Next version with no policy violations for this component and its dependencies"));
    recommendation.actions().shouldHaveSize(1);

    recommendation = recommendedVersionsSection.getRecommendation(2);
    recommendation.shouldBe(visible);
    recommendation.subText().shouldHave(
        text("The current version doesn't cause Build failure for this component and its dependencies"));

    eyesWatcher.eyesCheck("component details overview tab risk remediation recommended versions" +
        " - multiple recommendation");
  }

  @Test
  public void testRiskRemediationTile_RecommendedVersions_NoRecommendation() {
    mockHdsResponseForSecondComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(1,SECOND_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Risk Remediation"));

    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedVersionsSection.content());
    recommendedVersionsSection.getTitle().shouldHave(text("Recommended Versions"));
    ElementsCollection recommendedVersions = recommendedVersionsSection.contentRecommendedVersionsList();
    recommendedVersions.shouldHaveSize(1);

    RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.shouldBe(visible);
    recommendation.subText().shouldHave(
        text("No recommended versions are available for the current component"));

  }

  @Test
  public void testRiskRemediationTile_DependencyInformation_DirectDependency() {
    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0,FIRST_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    ScrollUtil.scrollIntoView(riskRemediation.getTitle());
    riskRemediation.getTitle().shouldHave(text("Risk Remediation"));

    riskRemediation.recommendedVersionsSections().shouldBe(visible);
    riskRemediation.compareVersionsTable().shouldBe(visible);
    riskRemediation.versionExplorerSection().shouldBe(visible);
    riskRemediation.dependencyInformationSection().shouldNotBe(visible);

    eyesWatcher.eyesCheck("component details overview tab risk remediation no dependency information");
  }

  @Test
  public void testRiskRemediationTile_DependencyInformation_showMore() {
    mockHdsResponseForSecondComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(1,SECOND_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Risk Remediation"));

    RecommendedRemediationSection recommendedRemediationSection = riskRemediation.dependencyInformationSection();
    recommendedRemediationSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedRemediationSection.content());
    recommendedRemediationSection.getTitle().shouldHave(text("Recommended Remediation"));
    recommendedRemediationSection.contentParagraph().shouldHave(
        text("The direct dependencies that brought in this component are listed below. Clicking on a component" +
            " will take you to its Component Details Page.")
    );

    ElementsCollection ancestors = recommendedRemediationSection.contentAncestorsList();
    ancestors.shouldHaveSize(3);
    SelenideElement ancestor = ancestors.get(0);
    ancestor.shouldHave(text("javancss : javancss : 29.50"));

    ancestor = ancestors.get(1);
    ancestor.shouldHave(text("aopalliance : aopalliance : 1.0"));

    ancestor = ancestors.get(2);
    ancestor.shouldHave(text("java2html : j2h : 1.3.1"));

    SelenideElement showMore = recommendedRemediationSection.toggleListLink();
    showMore.shouldHave(text("Show more"));

    eyesWatcher.eyesCheck(
        "component details overview tab risk remediation dependency information - transitive dependency show more");
  }

  @Test
  public void testRiskRemediationTile_DependencyInformation_showLess() {
    mockHdsResponseForSecondComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(1,SECOND_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Risk Remediation"));

    RecommendedRemediationSection recommendedRemediationSection = riskRemediation.dependencyInformationSection();
    recommendedRemediationSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedRemediationSection.content());
    recommendedRemediationSection.getTitle().shouldHave(text("Recommended Remediation"));
    recommendedRemediationSection.contentParagraph().shouldHave(
        text("The direct dependencies that brought in this component are listed below. Clicking on a component" +
            " will take you to its Component Details Page.")
    );

    ElementsCollection ancestors = recommendedRemediationSection.contentAncestorsList();
    ancestors.shouldHaveSize(3);
    SelenideElement showMore = recommendedRemediationSection.toggleListLink();
    showMore.click();

    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, SECOND_COMPONENT_HASH));
    recommendedRemediationSection = riskRemediation.dependencyInformationSection();
    recommendedRemediationSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedRemediationSection.content());
    ancestors = recommendedRemediationSection.contentAncestorsList();
    ancestors.shouldHaveSize(5);
    SelenideElement ancestor = ancestors.get(0);
    ancestor.find(".nx-text-link").shouldHave(text("javancss : javancss : 29.50"));
    ancestor.find(".nx-tag").shouldNot(exist);

    ancestor = ancestors.get(1);
    ancestor.find(".nx-text-link").shouldHave(text("aopalliance : aopalliance : 1.0"));
    ancestor.find(".nx-tag").shouldNot(exist);

    ancestor = ancestors.get(2);
    ancestor.find(".nx-text-link").shouldHave(text("java2html : j2h : 1.3.1"));
    ancestor.find(".nx-tag").shouldHave(text("InnerSource"));

    ancestor = ancestors.get(3);
    ancestor.find(".nx-text-link").shouldHave(text("org.apache.tiles : tiles-core : 2.2.2"));
    ancestor.find(".nx-tag").shouldNot(exist);

    ancestor = ancestors.get(4);
    ancestor.find(".nx-text-link").shouldHave(text("org.example : test-business : 1.0-snapshot"));
    ancestor.find(".nx-tag").shouldHave(text("InnerSource"));

    showMore = recommendedRemediationSection.toggleListLink();
    showMore.shouldHave(text("Show less"));

    eyesWatcher.eyesCheck(
        "component details overview tab risk remediation dependency information" +
            " - transitive dependency show less elements");
  }

  @Test
  public void testRiskRemediationTile_FooterBackButton() {
    mockHdsResponseForFirstComponent();
    mockHdsResponseForSecondComponent();

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));

    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(1,SECOND_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);

    RecommendedRemediationSection recommendedRemediationSection = riskRemediation.dependencyInformationSection();
    recommendedRemediationSection.shouldBe(visible);

    ScrollUtil.scrollIntoView(recommendedRemediationSection.content());

    SelenideElement ancestor = recommendedRemediationSection.contentClickableAncestorsList().get(0);
    ancestor.shouldHave(text("javancss : javancss : 29.50"));
    ancestor.click();

    ComponentDetailsFooter footer = componentDetailsPage.footer();

    SelenideElement footerBackButton = footer.backButton();

    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);
    footerBackButton.shouldHave(text("Back to ch.qos.logback : logback-access : 0.6 component"));
    eyesWatcher.eyesCheck();
    footerBackButton.click();

    footer.prevLink().shouldHave(text("Previous Component"));
    footer.nextLink().shouldHave(text("Next Component"));

    SelenideElement pageTitle = componentDetailsPage.header().title();
    pageTitle.shouldHave(text("ch.qos.logback : logback-access : 0.6"));
  }

  @Test
  public void testCompareVersionsTable() {
    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0,FIRST_COMPONENT_HASH);
    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.compareVersionsTitle().shouldBe(visible).shouldHave(text("Compare Versions"));
    ScrollUtil.scrollIntoView(riskRemediation.compareVersionsTitle());
    CompareVersionsTable table = riskRemediation.compareVersionsTable();
    table.shouldBe(visible);
    table.versionRow().get(1).shouldHave(text("29.50"));
    table.versionRow().get(2).shouldHave(text("-"));
    table.highestPolicyThreatRow().get(1).shouldHave(text("10 within 4 policies"));
    table.highestPolicyThreatRow().get(2).shouldBe(empty);
    table.highestSecurityThreatRow().get(1).shouldHave(text("10"));
    table.highestSecurityThreatRow().get(2).shouldBe(empty);
    table.highestCvssScoreRow().get(1).shouldHave(text("9.1"));
    table.highestCvssScoreRow().get(2).shouldBe(empty);
    table.highestLicenseThreatRow().get(1).shouldHave(text("5"));
    table.highestLicenseThreatRow().get(2).shouldBe(empty);
    table.effectiveLicenseRow().get(1).shouldHave(text("Apache-2.0, GPL-2.0"));
    table.effectiveLicenseRow().get(2).shouldBe(empty);
    table.highestQualityThreatRow().get(1).shouldHave(text("None"));
    table.highestQualityThreatRow().get(2).shouldBe(empty);
    table.highestOtherThreatRow().get(1).shouldHave(text("1"));
    table.highestOtherThreatRow().get(2).shouldBe(empty);
    table.hygieneRatingRow().get(1).shouldHave(text("Exemplar"));
    table.hygieneRatingRow().get(2).shouldBe(empty);
    table.integrityRatingRow().get(1).shouldHave(text("Normal"));
    table.integrityRatingRow().get(2).shouldBe(empty);
    table.catalogDateRow().get(1).shouldNotBe(empty);
    table.catalogDateRow().get(2).shouldBe(empty);
  }

  @Test
  public void testCompareVersionsTable_Selected() {
    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0,FIRST_COMPONENT_HASH);
    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();

    VersionGraph versionGraph = new VersionGraph();
    SelenideElement graph = versionGraph.getGraph();

    ScrollUtil.awaitEndOfScrolling(graph.scrollIntoView(true));

    mockHdsResponseForFirstComponentWithSelectedVersion();
    versionGraph.selectVersion(3).click();

    ScrollUtil.awaitEndOfScrolling(riskRemediation.compareVersionsTitle().scrollIntoView(true));

    CompareVersionsTable table = riskRemediation.compareVersionsTable();

    table.shouldBe(visible);
    table.versionRow().get(1).shouldHave(text("29.50"));
    table.versionRow().get(2).shouldHave(text("28.49"));
    table.highestPolicyThreatRow().get(2).shouldHave(text("5 within 5 policies"));
    table.highestSecurityThreatRow().get(2).shouldHave(text("3"));
    table.highestCvssScoreRow().get(2).shouldHave(text("4.3"));
    table.highestLicenseThreatRow().get(2).shouldHave(text("5"));
    table.effectiveLicenseRow().get(2).shouldHave(text("Apache-2.0, GPL-2.0"));
    table.highestQualityThreatRow().get(2).shouldHave(text("2"));
    table.highestOtherThreatRow().get(2).shouldHave(text("1"));
    table.hygieneRatingRow().get(2).shouldHave(text("Laggard"));
    table.integrityRatingRow().get(2).shouldHave(text("Normal"));
    table.catalogDateRow().get(2).shouldNotBe(empty);

    eyesWatcher.eyesCheck();

    versionGraph.selectVersion(4).click();

    table.versionRow().get(1).shouldHave(text("29.50"));
    table.versionRow().get(2).shouldHave(text("-"));
    table.highestPolicyThreatRow().get(2).shouldBe(empty);
    table.highestCvssScoreRow().get(2).shouldBe(empty);
    table.effectiveLicenseRow().get(2).shouldBe(empty);
    table.hygieneRatingRow().get(2).shouldBe(empty);
    table.integrityRatingRow().get(2).shouldBe(empty);
    table.catalogDateRow().get(2).shouldBe(empty);
  }

  @Test
  public void testRiskRemediationTile_Compare() {
    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0,FIRST_COMPONENT_HASH);
    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();

    riskRemediation.shouldBe(visible);

    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);
    ScrollUtil.awaitEndOfScrolling(recommendedVersionsSection.content().scrollIntoView(true));

    RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);

    recommendation.shouldBe(visible);
    recommendation.text().shouldHave(text("Upgrade to 31.52"));

    SelenideElement compareButton = recommendedVersionsSection.getRecommendation(0).actions().first();

    mockHdsResponseForFirstComponentWithRecommendedVersion();
    compareButton.click();

    CompareVersionsTable table = riskRemediation.compareVersionsTable();

    table.shouldBe(visible);
    table.versionRow().get(1).shouldHave(text("29.50"));
    table.versionRow().get(2).shouldHave(text("31.52"));
    table.highestPolicyThreatRow().get(2).shouldHave(text("None"));
    table.highestSecurityThreatRow().get(2).shouldHave(text("None"));
    table.highestCvssScoreRow().get(2).shouldHave(text("None"));
    table.highestLicenseThreatRow().get(2).shouldHave(text("None"));
    table.effectiveLicenseRow().get(2).shouldHave(text("BSD-3-Clause"));
    table.highestQualityThreatRow().get(2).shouldHave(text("None"));
    table.highestOtherThreatRow().get(2).shouldHave(text("None"));
    table.hygieneRatingRow().get(2).shouldBe(empty);
    table.integrityRatingRow().get(2).shouldBe(empty);
    table.catalogDateRow().get(2).shouldNotBe(empty);
  }

  private ComponentDetailsPage openComponentDetailsPageForViolation(int violationIndex, String hash) {
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.get(violationIndex);
    firstViolation.click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, hash));
    return new ComponentDetailsPage();
  }

  private Policy createPolicy(String ownerId,
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
    return tempEntity.newPolicy(p);
  }

  private void mockHdsResponseForFirstComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-29.50.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
  }

  private void mockHdsResponseForFirstComponentWithRecommendedVersion() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-31.52.json"))
        .atUri("rest/ci/componentDetails");
  }

  private void mockHdsResponseForFirstComponentWithSelectedVersion() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-28.49.json"))
        .atUri("rest/ci/componentDetails");
  }

  private void mockHdsResponseForSecondComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/logback-accessComponentDetails-0.6.json"))
        .atUri("rest/ci/componentDetails");
  }

  private void setupHdsResponseForVersionList() {
    testCLMServer.getHdsServer()
        .respondWith(
            getClass().getResource("/componentDetails/javancssComponentDetailsListWithNoViolationsVersion.json"))
        .atUri("rest/ci/componentDetails/list");
  }
}
