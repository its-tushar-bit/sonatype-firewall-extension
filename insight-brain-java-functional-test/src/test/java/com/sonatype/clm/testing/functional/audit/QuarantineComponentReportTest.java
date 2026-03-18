/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.audit;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Supplier;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.LicenseStatus;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.componentdetails.OtherVersionsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile;
import com.sonatype.clm.testing.functional.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.QuarantineComponentReportPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportContainerPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
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
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public class QuarantineComponentReportTest
    extends AbstractFunctionalTest
{
  private static final String EXPIRED_TOKEN = "EXPIRED_TOKEN";

  private static final String NOT_AVAILABLE_YET_TOKEN = "NOT_AVAILABLE_YET_TOKEN";

  private static final String NOT_FOUND_TOKEN = "NOT_FOUND_TOKEN";

  private static final String NOT_VALID_TOKEN = "NOT_VALID_TOKEN";

  private static final String VALID_TOKEN_CONDITION = "VALID_TOKEN_CONDITION";

  private final List<ComponentDetails> componentDetailsArrayList = new ArrayList<>();

  private MultiLicenseDAO multiLicenseDAO;

  private QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  private Repository repository;

  private RepositoryManager repositoryManager;

  private String encodedToken;

  private QuarantineComponentReportPage quarantineReportPage;

  private Date date;

  public static License toLicenseDTO(MultiLicense multiLicense) {
    return new License(multiLicense.getId(), multiLicense.getShortDisplayName());
  }

  @Before
  public void before() {
    multiLicenseDAO = lookup(MultiLicenseDAO.class);
    quarantinedComponentAccessDAO = lookup(QuarantinedComponentAccessDAO.class);

    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    repositoryManager = tempEntity.newRepositoryManager();
    repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
    quarantineReportPage = new QuarantineComponentReportPage();
    date = new Date();
  }

  @After
  public void after() {
    hardreset();
  }

  private void waitUntilSpinnersGone() {
    waitUntilSpinnersGone(() -> quarantineReportPage.getAllLoadingSpinners());
  }

  private void waitUntilSpinnersGone(Supplier<ElementsCollection> elementsCollectionSupplier) {
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.invisibilityOf(elementsCollectionSupplier.get().get(0)));
    elementsCollectionSupplier.get().shouldHave(size(0));
  }

  private ComponentDetails createComponentDetail(String hash, ComponentIdentifier componentIdentifier) {
    ComponentDetails componentDetails = new ComponentDetails(componentIdentifier);
    componentDetails.setHash(hash);
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setDeclaredLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO
        .getByIdNotNull("Apache-2.0"))));
    componentDetails
        .setObservedLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("EPL-1.0"))));
    componentDetails
        .setOverriddenLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("GPL-1.0"))));
    componentDetails
        .setEffectiveLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("GPL-1.0"))));
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

    try {
      testCLMServer.getHdsServer()
          .respondWith(mainComponentDetail)
          .atUri("/rest/ci/componentDetails?" + "componentIdentifier="
              + URLEncoder.encode(JsonUtils.format(mainComponentDetail.getComponentIdentifier()), "UTF-8")
              + "&hash=" + mainComponentDetail.getHash());
    }
    catch (UnsupportedEncodingException e) {
      throw new UncheckedIOException(e);
    }
    testCLMServer.getHdsServer().respondWith(componentDetailsList).atUri("/rest/ci/componentDetails/list");
    testCLMServer.getHdsServer().respondWith(componentDependenciesDTO).atUri("/rest/component/dependencies");
  }

  private Policy createPolicy(
      String ownerId,
      int threatLevel,
      String name,
      String conditionType,
      String operator,
      String value)
  {
    Policy policy = new Policy(null, name);
    policy.setThreatLevel(threatLevel);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, name + " constraint", LogicalOperator.AND);
    com.sonatype.insight.brain.model.policy.Condition condition = new com.sonatype.insight.brain.model.policy.Condition(
        conditionType, operator, value);
    constraint.setConditions(Collections.singletonList(condition));
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(ProxyStageType.ID, FailActionType.ID);
    return tempEntity.newPolicy(policy);
  }

  private void createSecurityPolicies() {
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 10, "SecurityPolicy",
        SecurityVulnerabilitySeverityConditionType.ID, ">=", "9.1");
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 6, "Security-Low",
        SecurityVulnerabilitySeverityConditionType.ID, ">=", "4.3");
  }

  private void createLicensePolicies() {
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 5, "LicensePolicy",
        LicenseThreatGroupLevelConditionType.ID, "<=", "5");
  }

  private void createQualityPolicies() {
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 2, "QualityPolicyRelativePopularity",
        RelativePopularityConditionType.ID, "<=", "100");
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 2, "QualityPolicyAgeInDays",
        AgeInDaysConditionType.ID, "younger than", "50");
  }

  private void createOtherPolicies() {
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 1, "CoordinatesPolicy",
        CoordinatesConditionType.ID, "do not match", "maven:javancss*");
  }

  private ComponentIdentifier createComponentIdentifier(String version) {
    return ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli",
        version, "", "jar");
  }

  private RepositoryComponent createRepositoryComponent(
      String hash,
      ComponentIdentifier componentIdentifier,
      Date quarantineTime)
  {
    String componentVersion = componentIdentifier.getCoordinates().get(ComponentIdentifier.VERSION);
    return tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/" + componentVersion + "/abi.cli-" + componentVersion + ".jar",
        hash, componentIdentifier, date, quarantineTime);
  }

  private void createAllTypePolicies() {
    createSecurityPolicies();
    createLicensePolicies();
    createQualityPolicies();
    createOtherPolicies();
  }

  private QuarantinedComponentAccess newQuarantinedComponentAccessNotAvailableYetToken(
      final String repositoryId,
      final String repositoryComponentId)
  {
    DateTime now = DateTime.now();
    return tempEntity.newQuarantinedComponentAccess(repositoryId, repositoryComponentId, now.plusDays(3).toDate());
  }

  private QuarantinedComponentAccess newQuarantinedComponentAccessExpiredToken(
      final String repositoryId,
      final String repositoryComponentId)
  {
    DateTime now = DateTime.now();
    return tempEntity.newQuarantinedComponentAccess(repositoryId, repositoryComponentId, now.minusDays(3).toDate());
  }

  private String getQuarantinedComponentToken(RepositoryComponent repositoryComponent, String tokenCondition) {
    final QuarantinedComponentAccess quarantinedComponentAccess;

    if (tokenCondition == EXPIRED_TOKEN) {
      quarantinedComponentAccess =
          newQuarantinedComponentAccessExpiredToken(repository.getId(), repositoryComponent.getId());
    }
    else if (tokenCondition == NOT_AVAILABLE_YET_TOKEN) {
      quarantinedComponentAccess =
          newQuarantinedComponentAccessNotAvailableYetToken(repository.getId(), repositoryComponent.getId());
    }
    else if (tokenCondition == NOT_FOUND_TOKEN) {
      return "YzgwZjk0NWMzY2E0NDgxODk4YTY0NmQ2ZWVjMzg4YzW";
    }
    else if (tokenCondition == NOT_VALID_TOKEN) {
      return "YzgwZjk0NWMzY2E0NDgxODk4YTY0NmQ2ZWVjMzg4Yz{";
    }
    else {
      quarantinedComponentAccess =
          tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    }
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));
  }

  private String setupAllTestDataButPolicyViolationsTable() {
    ComponentDetails componentDetails1 = createComponentDetail("hash1", createComponentIdentifier("0.5.2"));
    ComponentDetails componentDetails2 = createComponentDetail("hash2", createComponentIdentifier("0.5.3"));
    componentDetailsArrayList.add(componentDetails1);
    componentDetailsArrayList.add(componentDetails2);

    RepositoryComponent repositoryComponent = createRepositoryComponent(
        componentDetails1.getHash(), componentDetails1.getComponentIdentifier(), date);
    createRepositoryComponent(componentDetails2.getHash(), componentDetails2.getComponentIdentifier(), null);

    riskRemediationSetup(componentDetailsArrayList);

    return getQuarantinedComponentToken(repositoryComponent, VALID_TOKEN_CONDITION);
  }

  private String setupAllTestDataWithSingleComponentVersion() {
    ComponentDetails componentDetails = createComponentDetail("hash1", createComponentIdentifier("0.5.2"));
    componentDetailsArrayList.add(componentDetails);

    RepositoryComponent repositoryComponent = createRepositoryComponent(
        componentDetails.getHash(), componentDetails.getComponentIdentifier(), date);

    riskRemediationSetup(componentDetailsArrayList);
    policyViolationsTableSetup(componentDetails.getComponentIdentifier(), repositoryComponent);

    return getQuarantinedComponentToken(repositoryComponent, VALID_TOKEN_CONDITION);
  }

  private String setupAllTestData(String tokenCondition) {
    ComponentDetails componentDetails1 = createComponentDetail("hash1", createComponentIdentifier("0.5.2"));
    ComponentDetails componentDetails2 = createComponentDetail("hash2", createComponentIdentifier("0.5.3"));
    componentDetailsArrayList.add(componentDetails1);
    componentDetailsArrayList.add(componentDetails2);

    RepositoryComponent repositoryComponent =
        createRepositoryComponent(componentDetails1.getHash(), componentDetails1.getComponentIdentifier(), date);
    createRepositoryComponent(componentDetails2.getHash(), componentDetails2.getComponentIdentifier(), null);

    riskRemediationSetup(componentDetailsArrayList);
    policyViolationsTableSetup(componentDetails1.getComponentIdentifier(), repositoryComponent);

    return getQuarantinedComponentToken(repositoryComponent, tokenCondition);
  }

  private ConstraintFact createConstraintFact(
      String constraintId,
      String constraintName,
      String summary,
      String reason)
  {
    com.sonatype.insight.brain.model.policy.Condition condition =
        new com.sonatype.insight.brain.model.policy.Condition(MatchStateConditionType.ID, "is",
            MatchState.EXACT.toString());
    ConstraintFact constraintFact = new ConstraintFact(constraintId, constraintName,
        LogicalOperator.AND.name());
    ConditionFact conditionFact = new ConditionFact(condition.getConditionTypeId(), 0, summary,
        reason);
    constraintFact.addConditionFact(conditionFact);
    return constraintFact;
  }

  private void policyViolationsTableSetup(
      ComponentIdentifier componentIdentifier,
      RepositoryComponent repositoryComponent)
  {
    // Security policies violations
    ConstraintFact securityConstraintFact = createConstraintFact("constrain1",
        "Security constraint", "summary", "security vulnerability severity >= 9.1");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 10,
        repositoryComponent.getPathname(), "hash1",
        Collections.singletonList(securityConstraintFact),
        false, "fail", SecurityVulnerabilitySeverityConditionType.ID, "SecurityPolicy",
        componentIdentifier, date, "policyWaiverId1", "policy waiver comment", date);

    ConstraintFact securityLowConstraintFact = createConstraintFact("constrain2",
        "Security-low constraint", "summary", "security vulnerability severity >= 4.3");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), "hash2",
        Collections.singletonList(securityLowConstraintFact),
        false, "fail", HygieneRatingConditionType.ID, "Security-Low",
        componentIdentifier, date, "policyWaiverId2", "policy waiver comment", date);

    // License policy violation
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 5,
        repositoryComponent.getPathname(), false, "fail", LicenseThreatGroupLevelConditionType.ID,
        "LicensePolicy", componentIdentifier, date);
    // Quality policy violation
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 4,
        repositoryComponent.getPathname(), false, "fail", RelativePopularityConditionType.ID,
        "QualityPolicyRelativePopularity", componentIdentifier, date);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 2,
        repositoryComponent.getPathname(), false, "fail", AgeInDaysConditionType.ID,
        "QualityPolicyAgeInDays", componentIdentifier, date);
    // Other policy violation
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 1,
        repositoryComponent.getPathname(), false, "fail", "CoordinatesPolicy",
        CoordinatesConditionType.ID, componentIdentifier, date);
  }

  private Wait<WebDriver> getWebDriverAwait() {
    return new FluentWait<>(getWebDriver())
        .withTimeout(Duration.ofSeconds(240))
        .pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class);
  }

  @Test
  public void testReportExpirationAlert() {
    ComponentIdentifier mainComponentIdentifier = createComponentIdentifier("0.5.2");
    RepositoryComponent repositoryComponent = createRepositoryComponent("hash1", mainComponentIdentifier, date);

    QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity
            .newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(), date);

    String encodedToken = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    DbQuarantinedComponentAccessManager dbQuarantinedComponentAccessManager =
        new DbQuarantinedComponentAccessManager(quarantinedComponentAccessDAO,
            testCLMServer.getCLMServer().getInstance(Configuration.class));
    Date tokenExpiryTime = dbQuarantinedComponentAccessManager.getTokenExpiryTime(date);

    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));

    waitUntilSpinnersGone();

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'XXX");
    dateFormat.setTimeZone(TimeZone.getDefault());
    quarantineReportPage.getExpirationReportAlert()
        .shouldHave(text("This report will expire on " + dateFormat.format(tokenExpiryTime)
            .replace("Z", "+00:00")));
  }

  @Test
  public void testRiskRemediationTile_VersionGraphExplorer() {
    createAllTypePolicies();
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = quarantineReportPage.getRiskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Version Explorer"));

    RiskRemediationTile.VersionExplorerSection versionExplorerSection = riskRemediation.versionExplorerSection();
    versionExplorerSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(versionExplorerSection.content());
    versionExplorerSection.content().shouldBe(visible);
  }

  @Test
  public void testRiskRemediationTile_RecommendedVersions_NoRecommendation() {
    createAllTypePolicies();
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = quarantineReportPage.getRiskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Version Explorer"));

    RiskRemediationTile.RecommendedVersionsSection recommendedVersionsSection =
        riskRemediation.recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(recommendedVersionsSection.content());
    recommendedVersionsSection.getTitle().shouldHave(text("Suggested Version Change"));
    ElementsCollection recommendedVersions = recommendedVersionsSection.contentRecommendedVersionsList();
    recommendedVersions.shouldHave(size(1));

    RiskRemediationTile.RecommendationElement recommendation = recommendedVersionsSection.getRecommendation(0);
    recommendation.shouldBe(visible);
    recommendation.text()
        .shouldHave(
            text("There are no suggested versions for this component"));
  }

  @Test
  public void testCompareVersionsTable() {
    createAllTypePolicies();
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    eyesWatcher.eyesCheck();
    RiskRemediationTile riskRemediation = quarantineReportPage.getRiskRemediationTile();
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

  private void testCompareButtons(int recommendationIndex) {
    RiskRemediationTile riskRemediation = quarantineReportPage.getRiskRemediationTile();

    riskRemediation.shouldBe(visible);
    RiskRemediationTile.RecommendedVersionsSection recommendedVersionsSection = riskRemediation
        .recommendedVersionsSections();
    recommendedVersionsSection.shouldBe(visible);

    RiskRemediationTile.RecommendationElement recommendation = recommendedVersionsSection
        .getRecommendation(recommendationIndex);

    recommendation.shouldBe(visible);

    if (recommendationIndex == 0) {
      recommendation.text().shouldHave(text("Upgrade to 0.5.3"));
    }
    else {
      recommendation.text().shouldHave(text("Version 0.5.3"));
    }

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
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    // There used to be 4 recommendations, but they were of the same version.
    // After the SDEV-1534 de-duplication, there is only one left.
    testCompareButtons(0);
  }

  @Test
  public void testPolicyViolationsTile_NoViolationTableEntries() {
    encodedToken = setupAllTestDataButPolicyViolationsTable();
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    PolicyViolationsTable policyViolationsTable = quarantineReportPage.getViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(1));
    ElementsCollection rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHave(size(1));
    rowCells.get(0).shouldHave(text("No policy violations"));
  }

  @Test
  public void testPolicyViolationsTile_ViolationsTableEntries() {
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    PolicyViolationsTable policyViolationsTable = quarantineReportPage.getViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(6));

    policyViolationsTable.getRows().shouldHave(size(6));

    ElementsCollection securityViolationCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    securityViolationCells.shouldHave(size(4));
    securityViolationCells.get(0).shouldHave(text("10"));
    securityViolationCells.get(1).shouldHave(text("SecurityPolicy"));
    securityViolationCells.get(2).shouldHave(text("Security constraint"));
    securityViolationCells.get(3).shouldHave(text("security vulnerability severity >= 9.1"));

    ElementsCollection securityLowViolationCells = policyViolationsTable.getRows().get(1).findAll(By.tagName("td"));
    securityLowViolationCells.get(0).shouldHave(text("6"));
    securityLowViolationCells.get(1).shouldHave(text("Security-Low"));
    securityLowViolationCells.get(2).shouldHave(text("Security-low constraint"));
    securityLowViolationCells.get(3).shouldHave(text("security vulnerability severity >= 4.3"));

    ElementsCollection licenseViolationCells = policyViolationsTable.getRows().get(2).findAll(By.tagName("td"));
    licenseViolationCells.get(0).shouldHave(text("5"));
    licenseViolationCells.get(1).shouldHave(text("LicensePolicy"));

    ElementsCollection qualityPopularityViolationCells = policyViolationsTable.getRows()
        .get(3)
        .findAll(By.tagName("td"));
    qualityPopularityViolationCells.get(0).shouldHave(text("4"));
    qualityPopularityViolationCells.get(1).shouldHave(text("QualityPolicyRelativePopularity"));

    ElementsCollection qualityAgeInDaysViolationCells = policyViolationsTable.getRows()
        .get(4)
        .findAll(By.tagName("td"));
    qualityAgeInDaysViolationCells.get(0).shouldHave(text("2"));
    qualityAgeInDaysViolationCells.get(1).shouldHave(text("QualityPolicyAgeInDays"));

    ElementsCollection otherViolationCells = policyViolationsTable.getRows().get(5).findAll(By.tagName("td"));
    otherViolationCells.get(0).shouldHave(text("1"));
    otherViolationCells.get(1).shouldHave(text("Coordinates"));
  }

  @Test
  public void testOtherVersionsTable_noOtherVersionsAvailable() {
    encodedToken = setupAllTestDataWithSingleComponentVersion();
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    OtherVersionsTable otherVersionsTable = quarantineReportPage.getOtherVersionsTable();
    otherVersionsTable.getRows().shouldHave(size(1));
    otherVersionsTable.getRow(1).findAll(By.tagName("td")).get(0).shouldHave(text("No data found."));
  }

  @Test
  public void testOtherVersionsTable() {
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    OtherVersionsTable otherVersionsTable = quarantineReportPage.getOtherVersionsTable();
    otherVersionsTable.getRows().shouldHave(size(1));
    otherVersionsTable.getRow(1)
        .findAll(By.tagName("td"))
        .get(0)
        .shouldHave(text("com.lingocoder : abi.cli : 0.5.3"));
  }

  @Test
  public void testOtherVersionsTable_DefaultSorting() {
    ComponentDetails componentDetails1 = createComponentDetail("hash1", createComponentIdentifier("0.5.2"));
    ComponentDetails componentDetails2 = createComponentDetail("hash2", createComponentIdentifier("0.5.3"));
    ComponentDetails componentDetails3 = createComponentDetail("hash3", createComponentIdentifier("0.5.4"));
    ComponentDetails componentDetails4 = createComponentDetail("hash4", createComponentIdentifier("0.5.5"));

    componentDetailsArrayList.addAll(
        Arrays.asList(componentDetails1, componentDetails2, componentDetails3, componentDetails4));

    RepositoryComponent repositoryComponent =
        createRepositoryComponent(componentDetails1.getHash(), componentDetails1.getComponentIdentifier(), date);
    createRepositoryComponent(componentDetails2.getHash(), componentDetails2.getComponentIdentifier(), null);
    createRepositoryComponent(componentDetails3.getHash(), componentDetails3.getComponentIdentifier(), null);
    createRepositoryComponent(componentDetails4.getHash(), componentDetails4.getComponentIdentifier(), null);

    encodedToken = getQuarantinedComponentToken(repositoryComponent, VALID_TOKEN_CONDITION);

    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    OtherVersionsTable otherVersionsTable = quarantineReportPage.getOtherVersionsTable();
    otherVersionsTable.getRows().shouldHave(size(3));
    otherVersionsTable.getRow(1)
        .findAll(By.tagName("td"))
        .get(0)
        .shouldHave(text("com.lingocoder : abi.cli : 0.5.5"));
    otherVersionsTable.getRow(2)
        .findAll(By.tagName("td"))
        .get(0)
        .shouldHave(text("com.lingocoder : abi.cli : 0.5.4"));
    otherVersionsTable.getRow(3)
        .findAll(By.tagName("td"))
        .get(0)
        .shouldHave(text("com.lingocoder : abi.cli : 0.5.3"));
  }

  @Test
  public void testQuarantineReportComponentOverviewTile() {
    createAllTypePolicies();
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    quarantineReportPage.getQuarantineReportComponentOverviewTile().shouldBe(visible);
    quarantineReportPage.getQuarantineReportComponentOverviewTileTitle()
        .shouldHave(text("com.lingocoder : abi.cli : 0.5.2"));
    quarantineReportPage.getQuarantineReportComponentOverviewTileReadOnlyItemData(0)
        .shouldHave(text("Quarantined"));
    quarantineReportPage.getQuarantineReportComponentOverviewTileReadOnlyItemData(1)
        .shouldHave(text("6 policy violations"));
    quarantineReportPage.getQuarantineReportComponentOverviewTileReadOnlyItemData(2)
        .shouldHave(text("seconds ago"));
    quarantineReportPage.getQuarantineReportComponentOverviewTileReadOnlyItemData(3)
        .shouldHave(text("repositoryPublicId"));
  }

  @Test
  public void testQuarantineReportComponentOverviewTile_LinkToRepositoryInNewTab() {
    createAllTypePolicies();
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    quarantineReportPage.getQuarantineReportComponentOverviewTile().shouldBe(visible);
    WebDriver driver = WebDriverRunner.getWebDriver();
    String oldTab = driver.getWindowHandle();
    String newTab = null;
    try {
      quarantineReportPage.getQuarantineReportComponentOverviewTileRepositoryLink().shouldBe(visible).click();
      newTab = driver.getWindowHandles()
          .stream()
          .filter(windowHandle -> !oldTab.equals(windowHandle))
          .findFirst()
          .orElse(null);
      driver.switchTo().window(newTab);
      loginAsAdmin();
      RepositoryReportContainerPage.title().shouldHave(text("repositoryPublicId Repository Results"));
    }
    finally {
      if (oldTab != null && newTab != null && driver.getWindowHandle().equals(newTab)) {
        driver.close();
        driver.switchTo().window(oldTab);
      }
    }
  }

  @Test
  public void testQuarantineReportComponentOverviewTile_ViewComponentDetails() {
    createAllTypePolicies();
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    quarantineReportPage.getQuarantineReportComponentOverviewTile().shouldBe(visible);
    quarantineReportPage.getQuarantineReportComponentOverviewTileViewComponentDetails().shouldBe(visible).click();
    loginAsAdmin();
    FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();
    waitUntilSpinnersGone(firewallComponentDetailsPage::getAllLoadingSpinners);
    firewallComponentDetailsPage.title().shouldHave(text("com.lingocoder : abi.cli : 0.5.2"));
  }

  @Test
  public void testQuarantineReportMenus_withAnonymousAccess() {
    createAllTypePolicies();
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    // TOP NAV MENU
    // support
    MainHeader.helpMenu().dropdownToggle().shouldNotBe(visible);
    // notifications
    MainHeader.notificationsMenu().shouldNotBe(visible);
    // system
    MainHeader.systemConfigMenu().shouldNotBe(visible);
    // manage user account
    MainHeader.userMenu().dropdownToggle().shouldNotBe(visible);

    MainHeader.loginButton().shouldBe(visible);

    // LEFT NAV MENU
    SidebarNavigation.dashboardNavigationButton().shouldNotBe(visible);
    SidebarNavigation.reportingNavigationButton().shouldNotBe(visible);
    SidebarNavigation.policiesNavigationButton().shouldNotBe(visible);
    SidebarNavigation.labsNavigationButton().shouldNotBe(visible);
    SidebarNavigation.firewallNavigationButton().shouldNotBe(visible);
    SidebarNavigation.legalNavigationButton().shouldNotBe(visible);

    quarantineReportPage.getQuarantineReportComponentOverviewTile().shouldBe(visible);
    quarantineReportPage.getViolationsTable().should(visible);
    quarantineReportPage.getRiskRemediationTile().shouldBe(visible);
    quarantineReportPage.getOtherVersionsTable().shouldBe(visible);
  }

  @Test
  public void testQuarantineReportMenus_withAuthenticatedAccess() {
    createAllTypePolicies();
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    MainHeader.loginButton().click();
    loginAsAdmin();
    waitUntilSpinnersGone();

    // TOP NAV MENU
    // support
    MainHeader.helpMenu().dropdownToggle().shouldBe(visible);
    // notifications
    MainHeader.notificationsMenu().shouldBe(visible);
    // system
    MainHeader.systemConfigMenu().shouldBe(visible);
    // manage user account
    MainHeader.userMenu().dropdownToggle().shouldBe(visible);

    MainHeader.loginButton().shouldNotBe(visible);

    // LEFT NAV MENU
    SidebarNavigation.firewallDashboardNavigationButton().shouldBe(visible);
    SidebarNavigation.firewallRepositoriesNavigationButton().shouldBe(visible);

    quarantineReportPage.getQuarantineReportComponentOverviewTile().shouldBe(visible);
    quarantineReportPage.getViolationsTable().should(visible);
    quarantineReportPage.getRiskRemediationTile().shouldBe(visible);
    quarantineReportPage.getOtherVersionsTable().shouldBe(visible);
  }

  @Test
  public void testTokenExpirationWarningAlert() {
    encodedToken = setupAllTestData(EXPIRED_TOKEN);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    SelenideElement warningAlert = quarantineReportPage.getTokenWarningAlert();
    warningAlert.shouldHave(text("This report expired on"));
    warningAlert.shouldHave(text("You may generate a new report by requesting the blocked component again."));
  }

  @Test
  public void testNotAvailableYetTokenWarningAlert() {
    encodedToken = setupAllTestData(NOT_AVAILABLE_YET_TOKEN);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    SelenideElement warningAlert = quarantineReportPage.getTokenWarningAlert();
    warningAlert.shouldHave(text("The quarantined component view you are trying to access is not available yet."));
  }

  @Test
  public void testNotFoundTokenWarningAlert() {
    encodedToken = setupAllTestData(NOT_FOUND_TOKEN);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    SelenideElement warningAlert = quarantineReportPage.getTokenWarningAlert();
    warningAlert.shouldHave(
        text("The quarantined component view for the blocked component you are trying to view could not be found."));
  }

  @Test
  public void testNotValidTokenWarningAlert() {
    encodedToken = setupAllTestData(NOT_VALID_TOKEN);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    SelenideElement warningAlert = quarantineReportPage.getTokenWarningAlert();
    warningAlert.shouldHave(
        text("The quarantined component view cannot be retrieved because the URL contains invalid characters."));
  }

  @Test
  public void testLoginModalAnonymousAccessConfiguration() {
    LoginModal loginModal = new LoginModal();
    quarantinedComponentAccessDAO.setAnonymousAccess(false);
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    loginModal.shouldBe(visible);
    loginModal.cancelButton().shouldNotBe(visible);
    quarantinedComponentAccessDAO.setAnonymousAccess(true);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    loginModal.shouldNotBe(visible);
    MainHeader.loginButton().shouldBe(visible);
  }

  @Test
  public void testQuarantineReportComponent_closeLoginModalWithCancelButton() {
    createAllTypePolicies();
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    // open login modal by clicking "view component details" button
    quarantineReportPage.getQuarantineReportComponentOverviewTile().shouldBe(visible);
    quarantineReportPage.getQuarantineReportComponentOverviewTileViewComponentDetails().shouldBe(visible).click();

    LoginModal loginModal = new LoginModal();
    loginModal.shouldBe(visible);
    loginModal.cancelButton().shouldBe(visible).click();

    loginModal.shouldNotBe(visible);

    // open login modal by clicking "view component details" button
    MainHeader.loginButton().shouldBe(visible);
    MainHeader.loginButton().click();

    loginModal.shouldBe(visible);
    loginModal.cancelButton().shouldBe(visible).click();

    loginModal.shouldNotBe(visible);
  }

  @Test
  public void testQuarantineReportComponent_closeLoginModalWithESC() {
    createAllTypePolicies();
    encodedToken = setupAllTestData(VALID_TOKEN_CONDITION);
    refreshOrOpen(QuarantineComponentReportPage.url(encodedToken));
    waitUntilSpinnersGone();

    // open login modal by clicking "view component details" button
    quarantineReportPage.getQuarantineReportComponentOverviewTile().shouldBe(visible);
    quarantineReportPage.getQuarantineReportComponentOverviewTileViewComponentDetails().shouldBe(visible).click();

    LoginModal loginModal = new LoginModal();
    loginModal.shouldBe(visible);

    // Simulate pressing ESC key to close the modal
    pressEscape();

    // login modal closed
    loginModal.shouldNotBe(visible);

    // open login modal by clicking "view component details" button
    MainHeader.loginButton().shouldBe(visible);
    MainHeader.loginButton().click();

    loginModal.shouldBe(visible);

    // Simulate pressing ESC key to close the modal
    pressEscape();

    // login modal closed
    loginModal.shouldNotBe(visible);
  }

  private void pressEscape() {
    Selenide.actions().sendKeys(Keys.ESCAPE).perform();
  }
}
