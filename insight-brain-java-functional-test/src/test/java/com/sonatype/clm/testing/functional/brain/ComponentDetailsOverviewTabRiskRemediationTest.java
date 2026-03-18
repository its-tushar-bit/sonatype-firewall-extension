/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;

import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CreatePRModal;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.elements.componentdetails.InnerSourceRepositorySourceAlert;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile.CompareVersionsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile.RecommendationElement;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile.RecommendedRemediationSection;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile.RecommendedVersionsSection;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile.VersionExplorerSection;
import com.sonatype.clm.testing.functional.elements.componentdetails.VersionGraph;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.utils.PullRequestBranchNameGenerator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.repository.client.NexusRepository3Client;
import com.sonatype.insight.brain.repository.client.NexusRepository3Client.NXRM3SearchResponse;
import com.sonatype.insight.brain.repository.client.NexusRepository3Client.NexusItem;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class ComponentDetailsOverviewTabRiskRemediationTest
    extends AbstractFunctionalTest
{
  @Rule
  public WireMockRule nxrm3MockSever = new WireMockRule(wireMockConfig().dynamicPort());

  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  public static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  public static final String FIRST_COMPONENT_HASH = "9aba4af169a1a3baa67f";

  public static final String SECOND_COMPONENT_HASH = "47b6857af4a1cc50875a";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private PullRequestBranchNameGenerator pullRequestBranchNameGenerator;

  private ApplicationDAO applicationDAO;

  private SourceControlEventDAO sourceControlEventDAO;

  private PolicyEvaluationDAO policyEvaluationDAO;

  private SourceControlDAO sourceControlDAO;

  private Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    applicationDAO = lookup(ApplicationDAO.class);
    sourceControlEventDAO = lookup(SourceControlEventDAO.class);
    policyEvaluationDAO = lookup(PolicyEvaluationDAO.class);
    pullRequestBranchNameGenerator = lookup(PullRequestBranchNameGenerator.class);
    sourceControlDAO = lookup(SourceControlDAO.class);

    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER.setEnabled(false);
    Organization org = tempEntity.newOrganization("ApplicationReportTest");
    app = tempEntity.newApplicationWithSpecificId("8bbaa746602142d9adf2de00a9ca4d4a", "ApplicationReportTest",
        "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    TestReportEvaluator evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);
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
  }

  @Test
  public void testRiskRemediationTile_RecommendedVersions_Multiple_Remediation() {
    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Version Explorer"));

    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedVersionsSection.content());
    recommendedVersionsSection.getTitle().shouldHave(text("Suggested Version Change"));
    ElementsCollection recommendedVersions = recommendedVersionsSection.contentRecommendedVersionsList();
    recommendedVersions.shouldHave(size(1));

    RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.shouldBe(visible);
    recommendation.text().shouldHave(text("Upgrade to 31.52"));
    recommendation.subText()
        .shouldHave(
            text("Next version with no policy violations for this component and its dependencies"));
    recommendation.actions().shouldHave(size(2));

    recommendation = recommendedVersionsSection.getRecommendation(1);
    recommendation.shouldBe(visible);
  }

  @Test
  public void testRiskRemediationTile_RecommendedVersions_NoRecommendation() {
    mockHdsResponseForSecondComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(1, SECOND_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Version Explorer"));

    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedVersionsSection.content());
    recommendedVersionsSection.getTitle().shouldHave(text("Suggested Version Change"));
    ElementsCollection recommendedVersions = recommendedVersionsSection.contentRecommendedVersionsList();
    recommendedVersions.shouldHave(size(1));

    RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.shouldBe(visible);
    recommendation.text()
        .shouldHave(
            text("There are no suggested versions for this component"));
  }

  @Test
  public void testRiskRemediationTile_DependencyInformation_DirectDependency() {
    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getLoadingSpinner().shouldNotBe(visible); // wait until loading complete
    ScrollUtil.scrollIntoView(riskRemediation.getTitle());
    riskRemediation.getTitle().shouldHave(text("Version Explorer"));

    riskRemediation.recommendedVersionsSections().shouldBe(visible);
    riskRemediation.compareVersionsTable().shouldBe(visible);
    riskRemediation.versionExplorerSection().shouldBe(visible);
    riskRemediation.dependencyInformationSection().shouldNotBe(visible);

    eyesWatcher.eyesCheck("component details overview tab risk remediation no dependency information");
  }

  @Test
  public void testRiskRemediationTile_RepositorySource_InnerSourceDependency() {
    NXRM3SearchResponse nxrm3SearchResponse = new NXRM3SearchResponse();
    NexusItem nexusItem = new NexusItem();
    nexusItem.format = NexusRepository3Client.REPO_MAVEN_FORMAT;
    nexusItem.maven2 = new HashMap<>();
    nexusItem.maven2.put(ComponentIdentifier.MAVEN_GROUP_ID, "org.example");
    nexusItem.maven2.put(ComponentIdentifier.MAVEN_ARTIFACT_ID, "test-business");
    nexusItem.maven2.put(ComponentIdentifier.MAVEN_EXTENSION, "jar");
    nexusItem.maven2.put(ComponentIdentifier.VERSION, "1.0-SNAPSHOT");
    nexusItem.maven2.put(ComponentIdentifier.MAVEN_CLASSIFIER, "");
    nxrm3SearchResponse.items.add(nexusItem);
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(JsonUtils.format(nxrm3SearchResponse))));
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
    testCLMServer.getHdsServer().respondWith(new ComponentDetailsList()).atUri("rest/ci/componentDetails/list");
    app.setRepositoryConnectionEnabled(true);
    applicationDAO.update(app);
    RepositoryConnection repositoryConnection =
        tempEntity.newRepositoryConnection(app.getId(), nxrm3MockSever.baseUrl(), null, null);
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(10, "cefa389a797ca9d030ef");
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent()
        .riskRemediationTile()
        .versionExplorerSection()
        .repositorySource()
        .shouldBe(visible)
        .shouldHave(text("Repository Source: " + repositoryConnection.getBaseUrl()));
  }

  @Test
  public void testOverviewTab_innerSourceRepositorySourceAlert() {
    nxrm3MockSever.stubFor(get(urlPathMatching("/service/rest/v1/search/assets"))
        .willReturn(aResponse()
            .withStatus(401)));
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
    testCLMServer.getHdsServer().respondWith(new ComponentDetailsList()).atUri("rest/ci/componentDetails/list");
    app.setRepositoryConnectionEnabled(true);
    applicationDAO.update(app);
    tempEntity.newRepositoryConnection(app.getId(), nxrm3MockSever.baseUrl(), null, null);
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(10, "cefa389a797ca9d030ef");
    componentDetailsPage.overviewTab().shouldBe(visible);

    InnerSourceRepositorySourceAlert repositorySourceAlert = new InnerSourceRepositorySourceAlert();
    repositorySourceAlert.content().shouldBe(visible);
    repositorySourceAlert.content()
        .shouldHave(
            exactText("Could not retrieve data from InnerSource repository. Check your repository configuration."));

    eyesWatcher.eyesCheck("component details overview tab InnerSource repository source alert");
  }

  @Test
  public void testRiskRemediationTile_RepositorySource_NonInnerSourceDependency() {
    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent()
        .riskRemediationTile()
        .versionExplorerSection()
        .repositorySource()
        .shouldNotBe(visible);
  }

  @Test
  public void testRiskRemediationTile_RepositorySource_InnerSourceDependency_FeatureDisabled() {
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.setEnabled(false);
    testCLMServer.getHdsServer().respondWith(new ComponentDetailsList()).atUri("/componentDetails/list");
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(10, "cefa389a797ca9d030ef");
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent()
        .riskRemediationTile()
        .versionExplorerSection()
        .repositorySource()
        .shouldNotBe(visible);
  }

  @Test
  public void testRiskRemediationTile_RecommendedRemediation_showMore() {
    mockHdsResponseForSecondComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(1, SECOND_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Version Explorer"));

    RecommendedRemediationSection recommendedRemediationSection = riskRemediation.dependencyInformationSection();
    recommendedRemediationSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedRemediationSection.content());
    recommendedRemediationSection.getTitle().shouldHave(text("Recommended Remediation"));
    recommendedRemediationSection.contentParagraph()
        .shouldHave(
            text("The direct dependencies that brought in this component are listed below. Clicking on a component" +
                " will take you to its Component Details Page."));

    ElementsCollection ancestors = recommendedRemediationSection.contentAncestorsList();
    ancestors.shouldHave(size(3));
    SelenideElement ancestor = ancestors.get(0);
    ancestor.shouldHave(text("javancss : javancss : 29.50"));

    ancestor = ancestors.get(1);
    ancestor.shouldHave(text("aopalliance : aopalliance : 1.0"));

    ancestor = ancestors.get(2);
    ancestor.shouldHave(text("java2html : j2h : 1.3.1"));

    SelenideElement showMore = recommendedRemediationSection.toggleListLink();
    showMore.shouldHave(text("Show more"));
  }

  @Test
  public void testRiskRemediationTile_RecommendedRemediation_showLess() {
    mockHdsResponseForSecondComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(1, SECOND_COMPONENT_HASH);
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Version Explorer"));

    RecommendedRemediationSection recommendedRemediationSection = riskRemediation.dependencyInformationSection();
    recommendedRemediationSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedRemediationSection.content());
    recommendedRemediationSection.getTitle().shouldHave(text("Recommended Remediation"));
    recommendedRemediationSection.contentParagraph()
        .shouldHave(
            text("The direct dependencies that brought in this component are listed below. Clicking on a component" +
                " will take you to its Component Details Page."));

    ElementsCollection ancestors = recommendedRemediationSection.contentAncestorsList();
    ancestors.shouldHave(size(3));
    SelenideElement showMore = recommendedRemediationSection.toggleListLink();
    showMore.click();

    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, SECOND_COMPONENT_HASH));
    recommendedRemediationSection = riskRemediation.dependencyInformationSection();
    recommendedRemediationSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedRemediationSection.content());
    ancestors = recommendedRemediationSection.contentAncestorsList();
    ancestors.shouldHave(size(4));
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

    showMore = recommendedRemediationSection.toggleListLink();
    showMore.shouldHave(text("Show less"));
  }

  @Test
  public void testRiskRemediationTile_DependencyInformation_withReportFiltering_showMore() {
    ApplicationReportPage.AppReportHeaders headers = reportPage.headers();
    ElementsCollection violations = reportPage.resultRows();

    headers.componentNameFilterInput().setValue("ch.qos.logback : logback-access : 0.6");

    violations.first().click();

    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, SECOND_COMPONENT_HASH));
    mockHdsResponseForSecondComponent();
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Version Explorer"));

    RecommendedRemediationSection recommendedRemediationSection = riskRemediation.dependencyInformationSection();
    recommendedRemediationSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedRemediationSection.content());
    recommendedRemediationSection.getTitle().shouldHave(text("Recommended Remediation"));
    recommendedRemediationSection.contentParagraph()
        .shouldHave(
            text("The direct dependencies that brought in this component are listed below. Clicking on a component" +
                " will take you to its Component Details Page."));

    ElementsCollection ancestors = recommendedRemediationSection.contentAncestorsList();
    ancestors.shouldHave(size(3));
    SelenideElement ancestor = ancestors.get(0);
    ancestor.shouldHave(text("javancss : javancss : 29.50"));

    ancestor = ancestors.get(1);
    ancestor.shouldHave(text("aopalliance : aopalliance : 1.0"));

    ancestor = ancestors.get(2);
    ancestor.shouldHave(text("java2html : j2h : 1.3.1"));

    SelenideElement showMore = recommendedRemediationSection.toggleListLink();
    showMore.shouldHave(text("Show more"));
  }

  @Test
  public void testCompareVersionsTable() {
    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);
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
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);
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
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);
    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();

    riskRemediation.shouldBe(visible);

    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);
    ScrollUtil.awaitEndOfScrolling(recommendedVersionsSection.content().scrollIntoView(true));

    RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);

    recommendation.shouldBe(visible);
    recommendation.text().shouldHave(text("Upgrade to 31.52"));

    SelenideElement compareButton = recommendedVersionsSection.getRecommendation(0).actions().get(1);

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

  @Test
  public void testRiskRemediationTile_PRStatus_ManualPullRequestsEnabled_SourceControlNotConfigured() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.getTitle().shouldHave(text("Suggested Version Change"));

    RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.createPullRequestButton().shouldBe(visible);
    recommendation.createPullRequestButton().shouldHave(cssClass("disabled"));

    recommendation.createPullRequestButton().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Source Control is not configured"));
  }

  @Test
  public void testRiskRemediationTile_PRStatus_ManualPullRequestsEnabled_CreatePRModal() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    final String repositoryURL = "http://test.github.com/org/repo";

    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(),
        repositoryURL,
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB);
    sourceControl.setManualPullRequestsEnabled(true);
    lookup(SourceControlDAO.class).update(sourceControl);

    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.getTitle().shouldHave(text("Suggested Version Change"));

    RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.createPullRequestButton().shouldBe(visible).click();

    CreatePRModal createPRModal = new CreatePRModal();
    createPRModal.shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPullRequestModalHeader().shouldBe(visible).shouldHave(text("Create Pull Request"));
    createPRModal.createPrModalPrTitle().shouldBe(visible).shouldHave(text("Bump javancss to 31.52"));
    createPRModal.createPrModalComponentName().shouldBe(visible).shouldHave(text("javancss : javancss : 29.50"));
    createPRModal.createPrModalCurrentVersion().shouldBe(visible).shouldHave(text("29.50"));
    createPRModal.createPrModalTargetVersion().shouldBe(visible).shouldHave(text("31.52"));
    createPRModal.createPrModalBreakingChanges().shouldBe(visible).shouldHave(text("Unknown"));
    createPRModal.createPrModalDefaultBranch().shouldBe(visible).shouldHave(text("master"));
  }

  @Test
  public void testRiskRemediationTile_PRStatus_PullRequestCreationPending() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    final String repositoryURL = "http://test.github.com/org/repo";
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("javancss", "javancss", "29.50", "", "jar");
    String branchName = pullRequestBranchNameGenerator.getBranchName(app, componentIdentifier, "31.52");

    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(),
        repositoryURL,
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB);
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    PolicyEvaluation evaluation =
        policyEvaluationDAO.getLastByApplicationIdAndScanIdNotNull(app.getId(), SCAN_ID);
    SourceControlEvent event = tempEntity.newSourceControlEvent(
        app,
        evaluation,
        "user",
        branchName,
        SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT,
        SourceControlEvent.EVENT_STATUS_IN_PROGRESS);
    sourceControlEventDAO.update(event);

    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.getTitle().shouldHave(text("Suggested Version Change"));

    RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.loadingSpinner().shouldBe(visible);
    recommendation.loadingSpinner().shouldHave(text("Creating PR…"));

    // verify that refreshing the page, will still show the spinner because of polling
    refreshOrOpen(ComponentDetailsPage.urlToOverview(app, SCAN_ID, FIRST_COMPONENT_HASH));
    recommendation.loadingSpinner().shouldBe(visible);
    recommendation.loadingSpinner().shouldHave(text("Creating PR…"));

    // verify that the spinner is not shown when the event is complete
    event.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    event.setEventStatusDetails("http://test.github.com/org/repo/pull/123");
    event.setPullRequestNumber(123);
    sourceControlEventDAO.update(event);
    componentDetailsPage = new ComponentDetailsPage();
    riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.loadingSpinner().shouldNotBe(visible);
    recommendation.prLink().shouldBe(visible, Duration.ofSeconds(5));
    recommendation.prLink().shouldHave(text("PR #123"));
    recommendation.prLink().shouldHave(attribute("href", "http://test.github.com/org/repo/pull/123"));
  }

  @Test
  public void testRiskRemediationTile_PRStatus_PullRequestCreationFailed_Retry_Complete() {
    mockHdsResponseForFirstComponent();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    final String repositoryURL = "http://test.github.com/org/repo";
    final String prUrl = "http://test.github.com/org/repo/pull/123";
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("javancss", "javancss", "29.50", "", "jar");
    String branchName = pullRequestBranchNameGenerator.getBranchName(app, componentIdentifier, "31.52");

    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(),
        repositoryURL,
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB);
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    PolicyEvaluation evaluation =
        policyEvaluationDAO.getLastByApplicationIdAndScanIdNotNull(app.getId(), SCAN_ID);
    SourceControlEvent sourceControlEvent = tempEntity.newSourceControlEvent(
        app,
        evaluation,
        "user",
        branchName,
        SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT,
        SourceControlEvent.EVENT_STATUS_ERROR);
    sourceControlEventDAO.update(sourceControlEvent);

    mockHdsResponseForFirstComponentWithRecommendedVersion();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.getTitle().shouldHave(text("Suggested Version Change"));

    RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.retryButton().shouldBe(visible);
    recommendation.retryButton().shouldHave(text("Retry"));

    recommendation.retryButton().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Failure to create PR."));

    recommendation.retryButton().click();

    recommendation.loadingSpinner().shouldBe(visible).shouldHave(text("Creating PR…"));

    sourceControlEvent.setEventStatus(SourceControlEvent.EVENT_STATUS_COMPLETE);
    sourceControlEvent.setEventStatusDetails(prUrl);
    sourceControlEvent.setPullRequestNumber(123);
    sourceControlEventDAO.update(sourceControlEvent);

    refreshOrOpen(ComponentDetailsPage.urlToOverview(app, SCAN_ID, FIRST_COMPONENT_HASH));
    componentDetailsPage = new ComponentDetailsPage();
    riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendation = recommendedVersionsSection.getRecommendation(0);

    recommendation.prLink().shouldBe(visible, Duration.ofSeconds(5));
    recommendation.prLink().shouldHave(text("PR #123"));
    recommendation.prLink().shouldHave(attribute("href", prUrl));
  }

  @Test
  public void testRiskRemediationTile_PRStatus_PullRequest() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    final String repositoryURL = "http://test.github.com/org/repo";
    final String prUrl = "http://test.github.com/org/repo/pull/123";
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("javancss", "javancss", "29.50", "", "jar");
    String branchName = pullRequestBranchNameGenerator.getBranchName(app, componentIdentifier, "31.52");

    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(),
        repositoryURL,
        lookup(PasswordHandler.class).encryptPassword("someToken"),
        SourceControlProvider.GITHUB);
    sourceControl.setManualPullRequestsEnabled(true);
    sourceControlDAO.update(sourceControl);

    PolicyEvaluation evaluation =
        policyEvaluationDAO.getLastByApplicationIdAndScanIdNotNull(app.getId(), SCAN_ID);
    SourceControlEvent sourceControlEvent = tempEntity.newSourceControlEvent(
        app,
        evaluation,
        "user",
        branchName,
        SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT,
        SourceControlEvent.EVENT_STATUS_COMPLETE);
    sourceControlEvent.setEventStatusDetails(prUrl);
    sourceControlEvent.setPullRequestNumber(123);
    sourceControlEventDAO.update(sourceControlEvent);

    mockHdsResponseForFirstComponent();
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForViolation(0, FIRST_COMPONENT_HASH);

    RiskRemediationTile riskRemediation = componentDetailsPage.overviewTabContent().riskRemediationTile();
    RecommendedVersionsSection recommendedVersionsSection = riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.getTitle().shouldHave(text("Suggested Version Change"));

    RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.prLink().shouldBe(visible);
    recommendation.prLink().shouldHave(text("PR #123"));
    recommendation.prLink().shouldHave(attribute("href", prUrl));
  }

  private ComponentDetailsPage openComponentDetailsPageForViolation(int violationIndex, String hash) {
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.get(violationIndex);
    firstViolation.click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, hash));
    return new ComponentDetailsPage();
  }

  private Policy createPolicy(
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
