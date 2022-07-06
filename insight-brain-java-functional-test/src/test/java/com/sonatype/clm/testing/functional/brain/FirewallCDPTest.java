/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile;
import com.sonatype.clm.testing.functional.pages.FirewallCDPPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallCDPTest
    extends AbstractFunctionalTest
{
  private final List<ComponentDetails> componentDetailsArrayList = new ArrayList<>();

  private final RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();

  FirewallCDPPage firewallCDPPage = new FirewallCDPPage();

  private Repository repository;

  private RepositoryManager repositoryManager;

  private Date date;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  public static License toLicenseDTO(MultiLicense multiLicense) {
    return new License(multiLicense.getId(), multiLicense.getShortDisplayName());
  }

  @Before
  public void before() {
    repositoryManager = tempEntity.newRepositoryManager();
    repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
    date = new Date();
  }

  private void waitUntilSpinnersGone() {
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.invisibilityOf(firewallCDPPage.getAllLoadingSpinners().get(0)));
    firewallCDPPage.getAllLoadingSpinners().shouldHave(size(0));
  }

  private ComponentDetails createComponentDetail(ComponentIdentifier componentIdentifier) {
    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
    ComponentDetails componentDetails = new ComponentDetails(componentIdentifier);
    componentDetails.setHash("somehash" + Math.floor(Math.random() * 100));
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setDeclaredLicenses(
        Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("Apache-2.0"))));
    componentDetails.setObservedLicenses(
        Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("EPL-1.0"))));
    componentDetails.setOverriddenLicenses(
        Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("GPL-1.0"))));
    componentDetails.setEffectiveLicenses(
        Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("GPL-1.0"))));
    componentDetails.setEffectiveLicenseStatus(LicenseStatus.Overridden);
    componentDetails.setCatalogDate(new Date().getTime());
    componentDetails.setWebsite("http://www.example.com");
    componentDetails.setLicenseThreatLevel(2);
    componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
    componentDetails.setIdentificationSourceComment("No comments");
    componentDetails.setRelativePopularity(100);
    return componentDetails;
  }

  private void addComponentDetailSecurityVulnerability(ComponentDetails componentDetail, float severity) {
    SecurityVulnerability secVul = new SecurityVulnerability();
    secVul.setRefId("SecurityVulnerability-lvl" + severity);
    secVul.setSeverity(severity);
    secVul.setSource("cve");
    componentDetail.addSecurityVulnerability(secVul);
  }

  private void riskRemediationSetup(List<ComponentDetails> componentDetailsArrayList) {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    ComponentDetails mainComponentDetail = componentDetailsArrayList.get(0);

    addComponentDetailSecurityVulnerability(mainComponentDetail, (float) 9.1);

    Set<License> effectiveLicenses = new HashSet<>();
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID, mainComponentDetail.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "MIT");
    effectiveLicenses.add(new License("MIT", "MIT"));
    mainComponentDetail.setEffectiveLicenses(effectiveLicenses);

    mainComponentDetail.setLicenseThreatLevel(10);
    mainComponentDetail.setCatalogDate(new Date().getTime());

    componentDetailsList.setList(componentDetailsArrayList);
    ComponentDependenciesDTO componentDependenciesDTO = new ComponentDependenciesDTO(dependenciesMap, detailsMap);

    testCLMServer.getHdsServer().respondWith(mainComponentDetail).atUri("/rest/ci/componentDetails/");
    testCLMServer.getHdsServer().respondWith(mainComponentDetail).atUri("/rest/ci/componentDetails?" +
        "componentIdentifier=%7B%22format%22%3A%22maven%22%2C%22coordinates%22%3A%7B%22artifactId" +
        "%22%3A%22abi.cli%22%2C%22classifier%22%3A%22%22%2C%22extension%22%3A%22jar%22%2C%22groupId" +
        "%22%3A%22com.lingocoder%22%2C%22version%22%3A%220.5.2%22%7D%7D&hash=hash");
    testCLMServer.getHdsServer().respondWith(componentDetailsList).atUri("/rest/ci/componentDetails/list");
    testCLMServer.getHdsServer().respondWith(componentDependenciesDTO).atUri("/rest/component/dependencies");
  }

  private Policy createPolicy(
      String ownerId, int threatLevel, String name, String conditionType, String operator, String value)
  {
    Policy policy = new Policy(null, name);
    policy.setThreatLevel(threatLevel);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, name + " constraint", LogicalOperator.AND);
    com.sonatype.insight.brain.model.policy.Condition condition =
        new com.sonatype.insight.brain.model.policy.Condition(conditionType, operator, value);
    constraint.setConditions(Collections.singletonList(condition));
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(ProxyStageType.ID, FailActionType.ID);
    return tempEntity.newPolicy(policy);
  }

  private void createSecurityPolicies() {
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 10, "SecurityPolicy", SecurityVulnerabilitySeverityConditionType.ID,
        ">=", "9.1");
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 6, "Security-Low", SecurityVulnerabilitySeverityConditionType.ID,
        ">=", "4.3");
  }

  private void createLicensePolicies() {
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 5, "LicensePolicy", LicenseThreatGroupLevelConditionType.ID, "<=",
        "5");
  }

  private void createQualityPolicies() {
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 2, "QualityPolicyRelativePopularity",
        RelativePopularityConditionType.ID, "<=", "100");
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 2, "QualityPolicyAgeInDays", AgeInDaysConditionType.ID,
        "younger than", "50");
  }

  private void createOtherPolicies() {
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 1, "CoordinatesPolicy", CoordinatesConditionType.ID, "do not match",
        "maven:javancss*");
  }

  private ComponentIdentifier createComponentIdentifier(String version) {
    return ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", version, "", "jar");
  }

  private RepositoryComponent createRepositoryComponent(
      ComponentIdentifier componentIdentifier, Date lastEvaluationTime, Date quarantineTime)
  {
    String componentVersion = componentIdentifier.getCoordinates().get(ComponentIdentifier.VERSION);
    return tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/" + componentVersion + "/abi.cli-" + componentVersion + ".jar", "hash",
        componentIdentifier, lastEvaluationTime, quarantineTime);
  }

  private void policyViolationsTableSetup(
      ComponentIdentifier componentIdentifier, RepositoryComponent repositoryComponent)
  {
    // Security policies violations
    ConstraintFact securityConstraintFact =
        createConstraintFact("constrain1", "Security constraint", "summary", "security vulnerability severity >= 9.1");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 10, repositoryComponent.getPathname(), "hash1",
        Collections.singletonList(securityConstraintFact), false, "fail", SecurityVulnerabilitySeverityConditionType.ID,
        "SecurityPolicy", componentIdentifier, date, "policyWaiverId1", "policy waiver comment", date);

    ConstraintFact securityLowConstraintFact = createConstraintFact("constrain2", "Security-low constraint", "summary",
        "security vulnerability severity >= 4.3");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6, repositoryComponent.getPathname(), "hash2",
        Collections.singletonList(securityLowConstraintFact), false, "fail", HygieneRatingConditionType.ID,
        "Security-Low", componentIdentifier, date, "policyWaiverId2", "policy waiver comment", date);

    // License policy violation
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5, repositoryComponent.getPathname(), false, "fail",
        LicenseThreatGroupLevelConditionType.ID, "LicensePolicy", componentIdentifier, date);
    // Quality policy violation
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 4, repositoryComponent.getPathname(), false, "fail",
        RelativePopularityConditionType.ID, "QualityPolicyRelativePopularity", componentIdentifier, date);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 2, repositoryComponent.getPathname(), false, "fail",
        AgeInDaysConditionType.ID, "QualityPolicyAgeInDays", componentIdentifier, date);
    // Other policy violation
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1, repositoryComponent.getPathname(), false, "fail",
        "CoordinatesPolicy", CoordinatesConditionType.ID, componentIdentifier, date);
  }

  private void createAllTypePolicies() {
    createSecurityPolicies();
    createLicensePolicies();
    createQualityPolicies();
    createOtherPolicies();
  }

  private RepositoryComponent setupAllTestData() {
    ComponentIdentifier mainComponentIdentifier = createComponentIdentifier("0.5.2");
    componentDetailsArrayList.add(createComponentDetail(mainComponentIdentifier));
    componentDetailsArrayList.add(createComponentDetail(createComponentIdentifier("0.5.3")));

    RepositoryComponent repositoryComponent = createRepositoryComponent(mainComponentIdentifier, date, date);
    createRepositoryComponent(componentDetailsArrayList.get(1).getComponentIdentifier(), date, null);

    riskRemediationSetup(componentDetailsArrayList);
    policyViolationsTableSetup(mainComponentIdentifier, repositoryComponent);

    return repositoryComponent;
  }

  private ConstraintFact createConstraintFact(
      String constraintId, String constraintName, String summary, String reason)
  {
    com.sonatype.insight.brain.model.policy.Condition condition =
        new com.sonatype.insight.brain.model.policy.Condition(MatchStateConditionType.ID, "is",
            MatchState.EXACT.toString());
    ConstraintFact constraintFact = new ConstraintFact(constraintId, constraintName, LogicalOperator.AND.name());
    ConditionFact conditionFact = new ConditionFact(condition.getConditionTypeId(), 0, summary, reason);
    constraintFact.addConditionFact(conditionFact);
    return constraintFact;
  }

  private Wait<WebDriver> getWebDriverAwait() {
    return new FluentWait<>(getWebDriver()).withTimeout(Duration.ofSeconds(240)).pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class);
  }

  @Test
  public void testTitle() {
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();
    firewallCDPPage.title().should(exist).shouldHave(text("com.lingocoder : abi.cli : 0.5.2"));
  }

  @Test
  public void testFormatTag() {
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();
    firewallCDPPage.formatTag().should(exist).shouldHave(text("Maven"));
  }

  @Test
  public void testTabs() {
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();
    ElementsCollection tabs = firewallCDPPage.tabs();
    tabs.shouldHaveSize(5);
    tabs.first().shouldHave(cssClass("active"));

    assertThat(getWebDriver().getCurrentUrl()).contains("/" + component.getMatchStateId() + "?");

    tabs.get(1).click();
    tabs.get(1).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/violations?");

    tabs.get(2).click();
    tabs.get(2).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/security?");

    tabs.get(3).click();
    tabs.get(3).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/legal?");

    tabs.get(4).click();
    tabs.get(4).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/labels?");

    tabs.get(0).click();
    tabs.get(0).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/overview?");
  }

  @Test
  public void testRiskRemediationTile_VersionGraphExplorer() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = firewallCDPPage.getRiskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Risk Remediation"));

    RiskRemediationTile.VersionExplorerSection versionExplorerSection = riskRemediation.versionExplorerSection();
    versionExplorerSection.shouldBe(visible);
    versionExplorerSection.getTitle().shouldHave(text("Version Explorer"));
    ScrollUtil.scrollIntoView(versionExplorerSection.content());
    versionExplorerSection.content().shouldBe(visible);
  }

  @Test
  public void testRiskRemediationTile_RecommendedVersions_NoRecommendation() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = firewallCDPPage.getRiskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Risk Remediation"));

    RiskRemediationTile.RecommendedVersionsSection recommendedVersionsSection =
        riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedVersionsSection.content());
    recommendedVersionsSection.getTitle().shouldHave(text("Recommended Versions"));
    ElementsCollection recommendedVersions = recommendedVersionsSection.contentRecommendedVersionsList();
    recommendedVersions.shouldHaveSize(1);

    RiskRemediationTile.RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.shouldBe(visible);
    recommendation.subText().shouldHave(text("No recommended versions are available for the current component"));
  }

  @Test
  public void testCompareVersionsTable() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = firewallCDPPage.getRiskRemediationTile();
    riskRemediation.compareVersionsTitle().shouldBe(visible).shouldHave(text("Compare Versions"));
    ScrollUtil.scrollIntoView(riskRemediation.compareVersionsTitle());
    RiskRemediationTile.CompareVersionsTable table = riskRemediation.compareVersionsTable();
    table.shouldBe(visible);
    table.versionRow().get(1).shouldHave(text("0.5.2"));
    table.versionRow().get(2).shouldHave(text("-"));
    table.highestPolicyThreatRow().get(1).shouldHave(text("10 within 6 policies"));
    table.highestPolicyThreatRow().get(2).shouldBe(empty);
    table.highestSecurityThreatRow().get(1).shouldHave(text("10"));
    table.highestSecurityThreatRow().get(2).shouldBe(empty);
    table.highestCvssScoreRow().get(1).shouldHave(text("9.1"));
    table.highestCvssScoreRow().get(2).shouldBe(empty);
    table.highestLicenseThreatRow().get(1).shouldHave(text("5"));
    table.highestLicenseThreatRow().get(2).shouldBe(empty);
    table.effectiveLicenseRow().get(1).shouldHave(text("MIT, GPL-1.0 Overridden"));
    table.effectiveLicenseRow().get(2).shouldBe(empty);
    table.highestQualityThreatRow().get(1).shouldHave(text("2"));
    table.highestQualityThreatRow().get(2).shouldBe(empty);
    table.highestOtherThreatRow().get(1).shouldHave(text("1"));
    table.highestOtherThreatRow().get(2).shouldBe(empty);
    table.catalogDateRow().get(1).shouldNotBe(empty);
    table.catalogDateRow().get(2).shouldBe(empty);
  }

  private String getDateString(Date date) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getDefault());
    return dateFormat.format(date);
  }

  private RepositoryComponent setupAllUnquarantinedComponentTestData(
      Date lastEvaluationTime, Date quarantineTime, Date unquarantineTime, Boolean autoUnquarantined)
  {
    ComponentIdentifier mainComponentIdentifier = createComponentIdentifier("0.5.2");
    componentDetailsArrayList.add(createComponentDetail(mainComponentIdentifier));
    ComponentIdentifier selectedComponentIdentifier = createComponentIdentifier("0.5.3");
    componentDetailsArrayList.add(createComponentDetail(selectedComponentIdentifier));

    RepositoryComponent repositoryComponent =
        createRepositoryComponent(mainComponentIdentifier, lastEvaluationTime, quarantineTime);
    RepositoryComponent selectedRepositoryComponent =
        createRepositoryComponent(selectedComponentIdentifier, lastEvaluationTime, quarantineTime);

    if (autoUnquarantined) {
      repositoryComponent.setUnquarantineTimeForMonitoring(unquarantineTime);
      selectedRepositoryComponent.setUnquarantineTimeForMonitoring(unquarantineTime);
    }
    else {
      repositoryComponent.setUnquarantineTimeForManualRelease(unquarantineTime);
      selectedRepositoryComponent.setUnquarantineTimeForManualRelease(unquarantineTime);
    }

    riskRemediationSetup(componentDetailsArrayList);
    policyViolationsTableSetup(mainComponentIdentifier, repositoryComponent);
    policyViolationsTableSetup(selectedComponentIdentifier, selectedRepositoryComponent);

    repositoryComponentDAO.update(repositoryComponent);
    repositoryComponentDAO.update(selectedRepositoryComponent);

    return repositoryComponent;
  }

  private void testCompareVersionsValues_ForUnquarantinedComponent(
      RepositoryComponent component,
      String lastEvaluationTimeString,
      String quarantineTimeString,
      String unquarantineTimeString,
      String unquarantineType)
  {
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();
    firewallCDPPage.getNextVersionInVersionExplorer().click();
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = firewallCDPPage.getRiskRemediationTile();
    riskRemediation.compareVersionsTitle().shouldBe(visible).shouldHave(text("Compare Versions"));
    ScrollUtil.scrollIntoView(riskRemediation.compareVersionsTitle());
    RiskRemediationTile.CompareVersionsTable table = riskRemediation.compareVersionsTable();
    table.shouldBe(visible);
    table.versionRow().get(1).shouldHave(text("0.5.2"));
    table.versionRow().get(2).shouldHave(text("0.5.3"));
    table.highestPolicyThreatRow().get(1).shouldHave(text("10 within 6 policies"));
    table.highestPolicyThreatRow().get(2).shouldHave(text("5 within 2 policies"));
    table.highestSecurityThreatRow().get(1).shouldHave(text("10"));
    table.highestSecurityThreatRow().get(2).shouldHave(text("None"));
    table.highestCvssScoreRow().get(1).shouldHave(text("9.1"));
    table.highestCvssScoreRow().get(2).shouldHave(text("None"));
    table.highestLicenseThreatRow().get(1).shouldHave(text("5"));
    table.highestLicenseThreatRow().get(2).shouldHave(text("5"));
    table.effectiveLicenseRow().get(1).shouldHave(text("MIT, GPL-1.0 Overridden"));
    table.effectiveLicenseRow().get(2).shouldHave(text("Not Provided"));
    table.highestQualityThreatRow().get(1).shouldHave(text("2"));
    table.highestQualityThreatRow().get(2).shouldHave(text("None"));
    table.highestOtherThreatRow().get(1).shouldHave(text("1"));
    table.highestOtherThreatRow().get(2).shouldHave(text("1"));
    table.catalogDateRow().get(1).shouldHave(text("Less than a day ago"));
    table.catalogDateRow().get(2).shouldHave(text("-"));

    table.firstEvaluationRow().get(1).shouldHave(text(lastEvaluationTimeString));
    table.firstEvaluationRow().get(2).shouldHave(text(lastEvaluationTimeString));
    table.latestEvaluationRow().get(1).shouldHave(text(lastEvaluationTimeString));
    table.latestEvaluationRow().get(2).shouldHave(text(lastEvaluationTimeString));
    table.quarantinedRow().get(1).shouldHave(text(quarantineTimeString));
    table.quarantinedRow().get(2).shouldHave(text(quarantineTimeString));
    table.releasedFromQuarantineRow().get(1).shouldHave(text(unquarantineType + " on " + unquarantineTimeString));
    table.releasedFromQuarantineRow().get(2).shouldHave(text(unquarantineType + " on " + unquarantineTimeString));
  }

  @Test
  public void testCompareVersionsTable_WithManuallyUnquarantinedComponent() {
    createAllTypePolicies();
    Date quarantineTime = date;
    Date lastEvaluationTime = new Date(date.getTime() + 10000);
    Date unquarantineTime = new Date(date.getTime() + 20000);
    String unquarantineTimeString = getDateString(unquarantineTime);
    String lastEvaluationTimeString = getDateString(lastEvaluationTime);
    String quarantineTimeString = getDateString(quarantineTime);
    eyesWatcher.eyesCheck(
        "Firewall Component Details Page - Compare Versions table with a component manually released from quarantine");
    RepositoryComponent component =
        setupAllUnquarantinedComponentTestData(lastEvaluationTime, quarantineTime, unquarantineTime, false);
    testCompareVersionsValues_ForUnquarantinedComponent(component, lastEvaluationTimeString, quarantineTimeString,
        unquarantineTimeString, "Manually");
  }

  @Test
  public void testCompareVersionsTable_WithAutoUnquarantinedComponent() {
    createAllTypePolicies();
    Date quarantineTime = date;
    Date lastEvaluationTime = new Date(date.getTime() + 10000);
    Date unquarantineTime = new Date(date.getTime() + 20000);
    String unquarantineTimeString = getDateString(unquarantineTime);
    String lastEvaluationTimeString = getDateString(lastEvaluationTime);
    String quarantineTimeString = getDateString(quarantineTime);
    RepositoryComponent component =
        setupAllUnquarantinedComponentTestData(lastEvaluationTime, quarantineTime, unquarantineTime, true);
    testCompareVersionsValues_ForUnquarantinedComponent(component, lastEvaluationTimeString, quarantineTimeString,
        unquarantineTimeString, "Automatically");
  }

  private void testCompareButtons(int recommendationIndex) {
    RiskRemediationTile riskRemediation = firewallCDPPage.getRiskRemediationTile();

    riskRemediation.shouldBe(visible);
    RiskRemediationTile.RecommendedVersionsSection recommendedVersionsSection =
        riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);

    RiskRemediationTile.RecommendationElement recommendation =
        recommendedVersionsSection.getRecommendation(recommendationIndex);

    recommendation.shouldBe(visible);
    recommendation.text().shouldHave(text("Upgrade to 0.5.3"));

    SelenideElement compareButton = recommendedVersionsSection.getRecommendation(recommendationIndex).actions().first();

    compareButton.click();
    RiskRemediationTile.CompareVersionsTable table = riskRemediation.compareVersionsTable();

    waitUntilSpinnersGone();
    table.shouldBe(visible);
    table.versionRow().get(1).shouldHave(text("0.5.2"));
    table.versionRow().get(2).shouldHave(text("0.5.3"));
    table.highestPolicyThreatRow().get(2).shouldHave(text("None"));
    table.highestSecurityThreatRow().get(2).shouldHave(text("None"));
    table.highestCvssScoreRow().get(2).shouldHave(text("None"));
    table.highestLicenseThreatRow().get(2).shouldHave(text("None"));
    table.effectiveLicenseRow().get(2).shouldHave(text("Not Provided"));
    table.highestQualityThreatRow().get(2).shouldHave(text("None"));
    table.highestOtherThreatRow().get(2).shouldHave(text("None"));
    table.catalogDateRow().get(2).shouldNotBe(empty);
  }

  @Test
  public void testRiskRemediationTile() {
    createSecurityPolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();

    testCompareButtons(0);

    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();

    testCompareButtons(1);

    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();

    testCompareButtons(2);

    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();

    testCompareButtons(3);
  }

  @Test
  public void testComponentOverviewTile() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();

    firewallCDPPage.getComponentOverviewTile().shouldBe(visible);
    firewallCDPPage.getComponentOverviewTileReadOnlyItemData(0).shouldHave(text("Exact"));
    firewallCDPPage.getComponentOverviewTileReadOnlyItemData(1).shouldHave(text("Sonatype"));
    firewallCDPPage.getComponentOverviewTileReadOnlyItemData(2).shouldHave(text("Visit Project Website"));
    firewallCDPPage.getComponentOverviewTileReadOnlyItemData(3).shouldHave(text("Other"));

    firewallCDPPage.getComponentOverviewTile().shouldBe(visible);
    firewallCDPPage.getViewCoordinatesButton().click();
    firewallCDPPage.getComponentCoordinatesPopOver().shouldBe(visible);
    firewallCDPPage.getComponentCoordinatesPopOverData(0).shouldHave(text("maven"));
    firewallCDPPage.getComponentCoordinatesPopOverData(1).shouldHave(text("com.lingocoder"));
    firewallCDPPage.getComponentCoordinatesPopOverData(2).shouldHave(text("abi.cli"));
    firewallCDPPage.getComponentCoordinatesPopOverData(3).shouldHave(text("0.5.2"));
    firewallCDPPage.getComponentCoordinatesPopOverCloseBtn().click();
    firewallCDPPage.getComponentCoordinatesPopOver().shouldNotBe(visible);
  }
}
