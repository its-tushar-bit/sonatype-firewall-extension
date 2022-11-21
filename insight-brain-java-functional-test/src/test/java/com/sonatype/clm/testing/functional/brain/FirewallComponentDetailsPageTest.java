/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.componentdetails.EditLicensesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.FirewallPolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.LicenseDetectionsTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilitiesTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilityDetailsPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationDetailPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilityDetailsPopover.VulnerabilityOverrideForm;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover.ComponentWaiversPopoverTable;
import com.sonatype.clm.testing.functional.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
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
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallComponentDetailsPageTest
    extends AbstractFunctionalTest
{
  private final List<ComponentDetails> componentDetailsArrayList = new ArrayList<>();

  private final FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();

  private Repository repository;

  private RepositoryManager repositoryManager;

  private Policy securityHighPolicy;

  private Policy securityLowPolicy;

  private Policy licensePolicy;

  private Policy popularityPolicy;

  private Policy agePolicy;

  private Policy coordinatesPolicy;

  private Date date;

  // declared and observed licenses are not the same
  private final String multiLicensed = "multiLicensed";

  // declared and observed licenses are the same
  private final String singleLicense = "singleLicense";

  // no declared or observed licenses are provided
  private final String nonLicensed = "nonLicensed";

  // overriden licenses by the user in IQ
  private final String overriddenLicense = "overriddenLicense";

  static final String dateTimeFormatMask = "yyyy-MM-dd HH:mm:ss";

  static final String dateFormatMask = "MM/dd/yyyy";

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
    wait.until(ExpectedConditions.invisibilityOf(firewallComponentDetailsPage.getAllLoadingSpinners().get(0)));
    firewallComponentDetailsPage.getAllLoadingSpinners().shouldHave(size(0));
  }

  private ComponentDetails createComponentDetail(
      String hash,
      ComponentIdentifier componentIdentifier,
      String licenseCondition)
  {
    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
    ComponentDetails componentDetails = new ComponentDetails(componentIdentifier);
    componentDetails.setHash(hash);
    componentDetails.setMatchState(MatchState.EXACT.getId());

    if (licenseCondition != nonLicensed) {
      // default license condition is singleLicense
      componentDetails
          .setDeclaredLicenses(Collections.singleton(toLicenseDTO(multiLicenseDAO.getByIdNotNull("Apache-2.0"))));

      componentDetails.setObservedLicenses(Collections.singleton(
          toLicenseDTO(multiLicenseDAO.getByIdNotNull(licenseCondition == multiLicensed ? "EPL-1.0" : "Apache-2.0"))));

      if (licenseCondition == overriddenLicense) {
        tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID, componentDetails.getComponentIdentifier(),
            LicenseOverrideStatus.OVERRIDDEN, "GPL-1.0");
      }
    }

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
    if (severity > 5) {
      secVul.setRefId("sonatype-2017-0507");
    }
    else {
      secVul.setRefId("CVE-1234-56789");
    }
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
    addComponentDetailSecurityVulnerability(mainComponentDetail, (float) 4.3);
    mainComponentDetail.setCatalogDate(new Date().getTime());

    componentDetailsList.setList(componentDetailsArrayList);
    ComponentDependenciesDTO componentDependenciesDTO = new ComponentDependenciesDTO(dependenciesMap, detailsMap);

    try {
      // Used when componentDetails are requested
      testCLMServer.getHdsServer().respondWith(mainComponentDetail)
          .atUri("/rest/ci/componentDetails?" + "componentIdentifier="
              + URLEncoder.encode(JsonUtils.format(mainComponentDetail.getComponentIdentifier()), "UTF-8") + "&hash="
              + mainComponentDetail.getHash());
      // Used when multi license details are requested
      testCLMServer.getHdsServer().respondWith(mainComponentDetail)
          .atUri("/rest/ci/componentDetails?" + "componentIdentifier="
              + URLEncoder.encode(JsonUtils.format(mainComponentDetail.getComponentIdentifier()), "UTF-8"));
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
    com.sonatype.insight.brain.model.policy.Condition condition =
        new com.sonatype.insight.brain.model.policy.Condition(conditionType, operator, value);
    constraint.setConditions(Collections.singletonList(condition));
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(ProxyStageType.ID, FailActionType.ID);
    return tempEntity.newPolicy(policy);
  }

  private void createSecurityPolicies() {
    securityHighPolicy = createPolicy(Organization.ROOT_ORGANIZATION_ID, 10, "Security-High",
        SecurityVulnerabilitySeverityConditionType.ID, ">=", "9.1");
    securityLowPolicy = createPolicy(Organization.ROOT_ORGANIZATION_ID, 6, "Security-Low",
        SecurityVulnerabilitySeverityConditionType.ID, ">=", "4.3");
  }

  private void createLicensePolicies() {
    licensePolicy = createPolicy(Organization.ROOT_ORGANIZATION_ID, 5, "LicensePolicy",
        LicenseThreatGroupLevelConditionType.ID, "<=", "5");
  }

  private void createQualityPolicies() {
    popularityPolicy = createPolicy(Organization.ROOT_ORGANIZATION_ID, 2, "QualityPolicyRelativePopularity",
        RelativePopularityConditionType.ID, "<=", "100");
    agePolicy = createPolicy(Organization.ROOT_ORGANIZATION_ID, 2, "QualityPolicyAgeInDays", AgeInDaysConditionType.ID,
        "younger than", "50");
  }

  private void createOtherPolicies() {
    coordinatesPolicy = createPolicy(Organization.ROOT_ORGANIZATION_ID, 1, "CoordinatesPolicy",
        CoordinatesConditionType.ID, "do not match", "maven:javancss*");
  }

  private ComponentIdentifier createComponentIdentifier(String version) {
    return ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", version, "", "jar");
  }

  private RepositoryComponent createRepositoryComponent(
      String hash,
      ComponentIdentifier componentIdentifier,
      Date lastEvaluationTime,
      Date quarantineTime)
  {
    String componentVersion = componentIdentifier.getCoordinates().get(ComponentIdentifier.VERSION);
    return tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        "com/lingocoder/abi.cli/" + componentVersion + "/abi.cli-" + componentVersion + ".jar", hash,
        componentIdentifier, lastEvaluationTime, quarantineTime);
  }

  private RepositoryPolicyViolation createRepositoryPolicyViolation(
      String repositoryId,
      int threatLevel,
      String pathname,
      String hash,
      List<ConstraintFact> constraintFacts,
      boolean isWaived,
      String actionId,
      String policyId,
      String policyName,
      ComponentIdentifier componentIdentifier,
      Date time,
      String policyWaiverId,
      String policyWaiverComment,
      Date waiveTime,
      PolicyThreatCategory policyThreatCategory)
  {
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repositoryId, pathname, time, policyId,
        policyName, threatLevel, policyThreatCategory, hash, componentIdentifier, constraintFacts);
    policyViolation.setWaived(isWaived);
    policyViolation.setActionTypeId(actionId);
    policyViolation.setPolicyWaiverId(policyWaiverId);
    policyViolation.setPolicyWaiverComment(policyWaiverComment);
    policyViolation.setWaiveTime(waiveTime);
    tempEntity.newRepositoryPolicyViolation(policyViolation);
    return policyViolation;
  }

  private void policyViolationsTableSetup(RepositoryComponent repositoryComponent) {
    ComponentIdentifier componentIdentifier = repositoryComponent.getComponentIdentifier();

    // Security policies violations
    if (securityHighPolicy != null) {
      ConstraintFact securityConstraintFact = createConstraintFact("constraint1", "Security constraint", "summary",
          "security vulnerability severity >= 9.1");
      createRepositoryPolicyViolation(repository.getId(), 10, repositoryComponent.getPathname(),
          repositoryComponent.getHash(), Collections.singletonList(securityConstraintFact), false /* isWaived */,
          "fail", securityHighPolicy.getId(), securityHighPolicy.getName(), componentIdentifier, date,
          null /* policyWaiverId */, null/* policyWaiverComment */, null/* waiveTime */, PolicyThreatCategory.SECURITY);
    }
    if (securityLowPolicy != null) {
      ConstraintFact securityLowConstraintFact = createConstraintFact("constraint2", "Security-low constraint",
          "summary", "security vulnerability severity >= 4.3");
      PolicyWaiver policyWaiver = tempEntity.newWaiver(repositoryComponent.getHash(), securityLowPolicy.getId(),
          Organization.ROOT_ORGANIZATION_ID, Collections.singletonList(securityLowConstraintFact),
          "Test comment for waiver");
      createRepositoryPolicyViolation(repository.getId(), 6, repositoryComponent.getPathname(),
          repositoryComponent.getHash(), Collections.singletonList(securityLowConstraintFact), true /* isWaived */,
          "warn", securityLowPolicy.getId(), securityLowPolicy.getName(), componentIdentifier, date,
          policyWaiver.getId(), policyWaiver.getComment(), date, PolicyThreatCategory.SECURITY);
    }

    // License policy violation
    if (licensePolicy != null) {
      ConstraintFact licenseConstraintFact =
          createConstraintFact("constraint3", "LicensePolicy constraint", "summary", "Found license threat group");
      createRepositoryPolicyViolation(repository.getId(), 5, repositoryComponent.getPathname(),
          repositoryComponent.getHash(), Collections.singletonList(licenseConstraintFact), true /* isWaived */, "warn",
          licensePolicy.getId(), licensePolicy.getName(), componentIdentifier, date, null /* policyWaiverId */,
          null/* policyWaiverComment */, null/* waiveTime */, PolicyThreatCategory.LICENSE);
    }

    if (agePolicy != null) {
      ConstraintFact qualityPolicyAgeInDaysConstraintFact = createConstraintFact("constraint5",
          "QualityPolicyAgeInDays constraint", "summary", "Found component younger than 50 days");
      createRepositoryPolicyViolation(repository.getId(), 4, repositoryComponent.getPathname(),
          repositoryComponent.getHash(), Collections.singletonList(qualityPolicyAgeInDaysConstraintFact),
          false /* isWaived */, "warn", agePolicy.getId(), agePolicy.getName(), componentIdentifier, date,
          null /* policyWaiverId */, null/* policyWaiverComment */, null/* waiveTime */, PolicyThreatCategory.QUALITY);
    }

    if (popularityPolicy != null) {
      // Quality policy violation
      ConstraintFact qualityRelativePopularityConstraintFact = createConstraintFact("constraint4",
          "QualityPolicyRelativePopularity constraint", "summary", "Low popularity");
      createRepositoryPolicyViolation(repository.getId(), 2, repositoryComponent.getPathname(),
          repositoryComponent.getHash(), Collections.singletonList(qualityRelativePopularityConstraintFact),
          false /* isWaived */, "warn", popularityPolicy.getId(), popularityPolicy.getName(), componentIdentifier, date,
          null /* policyWaiverId */, null/* policyWaiverComment */, null/* waiveTime */, PolicyThreatCategory.QUALITY);
    }

    // Other policy violation
    if (coordinatesPolicy != null) {
      ConstraintFact coordinatesConstraintFact = createConstraintFact("constraint6", "CoordinatesPolicy constraint",
          "summary", "Coordinates were com.lingocoder");
      createRepositoryPolicyViolation(repository.getId(), 1, repositoryComponent.getPathname(),
          repositoryComponent.getHash(), Collections.singletonList(coordinatesConstraintFact), false /* isWaived */,
          "fail", coordinatesPolicy.getId(), coordinatesPolicy.getName(), componentIdentifier, date,
          null /* policyWaiverId */, null/* policyWaiverComment */, null/* waiveTime */, PolicyThreatCategory.OTHER);
    }
  }

  private void createAllTypePolicies() {
    createSecurityPolicies();
    createLicensePolicies();
    createQualityPolicies();
    createOtherPolicies();
  }

  private RepositoryComponent setupBaseTestData(String mainComponentLicenseCondition) {
    ComponentDetails componentDetails1 =
        createComponentDetail("hash1", createComponentIdentifier("0.5.2"), mainComponentLicenseCondition);
    ComponentDetails componentDetails2 =
        createComponentDetail("hash2", createComponentIdentifier("0.5.3"), mainComponentLicenseCondition);
    componentDetailsArrayList.add(componentDetails1);
    componentDetailsArrayList.add(componentDetails2);

    RepositoryComponent mainRepositoryComponent =
        createRepositoryComponent(componentDetails1.getHash(), componentDetails1.getComponentIdentifier(), date, date);
    createRepositoryComponent(componentDetails2.getHash(), componentDetails2.getComponentIdentifier(), date, null);

    return mainRepositoryComponent;
  }

  private RepositoryComponent setupAllTestData() {
    RepositoryComponent mainRepositoryComponent = setupBaseTestData(singleLicense);

    riskRemediationSetup(componentDetailsArrayList);
    policyViolationsTableSetup(mainRepositoryComponent);

    return mainRepositoryComponent;
  }

  private RepositoryComponent setupAllTestData(String mainComponentLicenseCondition) {
    RepositoryComponent mainRepositoryComponent = setupBaseTestData(mainComponentLicenseCondition);

    riskRemediationSetup(componentDetailsArrayList);
    policyViolationsTableSetup(mainRepositoryComponent);

    return mainRepositoryComponent;
  }

  private ConstraintFact createConstraintFact(
      String constraintId,
      String constraintName,
      String summary,
      String reason)
  {
    com.sonatype.insight.brain.model.policy.Condition condition = new com.sonatype.insight.brain.model.policy.Condition(
        MatchStateConditionType.ID, "is", MatchState.EXACT.toString());
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
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();
    firewallComponentDetailsPage.title().should(exist).shouldHave(text("com.lingocoder : abi.cli : 0.5.2"));
  }

  @Test
  public void testFormatTag() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();
    firewallComponentDetailsPage.formatTag().should(exist).shouldHave(text("Maven"));
  }

  @Test
  public void testTabs() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();
    ElementsCollection tabs = firewallComponentDetailsPage.tabs();
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
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = firewallComponentDetailsPage.getRiskRemediationTile();
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
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = firewallComponentDetailsPage.getRiskRemediationTile();
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
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = firewallComponentDetailsPage.getRiskRemediationTile();
    riskRemediation.compareVersionsTitle().shouldBe(visible).shouldHave(text("Compare Versions"));
    ScrollUtil.scrollIntoView(riskRemediation.compareVersionsTitle());
    RiskRemediationTile.CompareVersionsTable table = riskRemediation.compareVersionsTable();
    table.shouldBe(visible);
    table.versionRow().get(1).shouldHave(text("0.5.2"));
    table.versionRow().get(2).shouldHave(text("-"));
    table.highestPolicyThreatRow().get(1).shouldHave(text("10 within 7 policies"));
    table.highestPolicyThreatRow().get(2).shouldBe(empty);
    table.highestSecurityThreatRow().get(1).shouldHave(text("10"));
    table.highestSecurityThreatRow().get(2).shouldBe(empty);
    table.highestCvssScoreRow().get(1).shouldHave(text("9.1"));
    table.highestCvssScoreRow().get(2).shouldBe(empty);
    table.highestLicenseThreatRow().get(1).shouldHave(text("5"));
    table.highestLicenseThreatRow().get(2).shouldBe(empty);
    table.effectiveLicenseRow().get(1).shouldHave(text("Apache-2.0"));
    table.effectiveLicenseRow().get(2).shouldBe(empty);
    table.highestQualityThreatRow().get(1).shouldHave(text("2"));
    table.highestQualityThreatRow().get(2).shouldBe(empty);
    table.highestOtherThreatRow().get(1).shouldHave(text("1"));
    table.highestOtherThreatRow().get(2).shouldBe(empty);
    table.catalogDateRow().get(1).shouldNotBe(empty);
    table.catalogDateRow().get(2).shouldBe(empty);
  }

  private String getDateString(Date date, String formatDate) {
    DateFormat dateFormat = new SimpleDateFormat(formatDate);
    dateFormat.setTimeZone(TimeZone.getDefault());
    return dateFormat.format(date);
  }

  private Date getDate(String date, String formatDate) throws ParseException {
    DateFormat dateFormat = new SimpleDateFormat(formatDate);
    dateFormat.setTimeZone(TimeZone.getDefault());
    return dateFormat.parse(date);
  }

  private RepositoryComponent setupAllUnquarantinedComponentTestData(
      Date lastEvaluationTime,
      Date quarantineTime,
      Date unquarantineTime,
      Boolean autoUnquarantined)
  {
    ComponentDetails componentDetails1 =
        createComponentDetail("hash1", createComponentIdentifier("0.5.2"), singleLicense);
    ComponentDetails componentDetails2 =
        createComponentDetail("hash2", createComponentIdentifier("0.5.3"), singleLicense);
    componentDetailsArrayList.add(componentDetails1);
    componentDetailsArrayList.add(componentDetails2);

    RepositoryComponent repositoryComponent = createRepositoryComponent(componentDetails1.getHash(),
        componentDetails1.getComponentIdentifier(), lastEvaluationTime, quarantineTime);
    RepositoryComponent selectedRepositoryComponent = createRepositoryComponent(componentDetails2.getHash(),
        componentDetails2.getComponentIdentifier(), lastEvaluationTime, quarantineTime);

    if (autoUnquarantined) {
      repositoryComponent.setUnquarantineTimeForMonitoring(unquarantineTime);
      selectedRepositoryComponent.setUnquarantineTimeForMonitoring(unquarantineTime);
    }
    else {
      repositoryComponent.setUnquarantineTimeForManualRelease(unquarantineTime);
      selectedRepositoryComponent.setUnquarantineTimeForManualRelease(unquarantineTime);
    }

    riskRemediationSetup(componentDetailsArrayList);
    policyViolationsTableSetup(repositoryComponent);
    policyViolationsTableSetup(selectedRepositoryComponent);

    RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();
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
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();
    firewallComponentDetailsPage.getClickableVersionsInVersionExplorer().get(1).hover().click();
    RiskRemediationTile riskRemediation = firewallComponentDetailsPage.getRiskRemediationTile();
    riskRemediation.compareVersionsTitle().shouldBe(visible).shouldHave(text("Compare Versions"));
    RiskRemediationTile.CompareVersionsTable table = riskRemediation.compareVersionsTable();
    table.shouldBe(visible);
    table.versionRow().get(1).shouldHave(text("0.5.2"));
    table.versionRow().get(2).shouldHave(text("0.5.3"));
    table.highestPolicyThreatRow().get(1).shouldHave(text("10 within 7 policies"));
    table.highestPolicyThreatRow().get(2).shouldHave(text("5 within 2 policies"));
    table.highestSecurityThreatRow().get(1).shouldHave(text("10"));
    table.highestSecurityThreatRow().get(2).shouldHave(text("None"));
    table.highestCvssScoreRow().get(1).shouldHave(text("9.1"));
    table.highestCvssScoreRow().get(2).shouldHave(text("None"));
    table.highestLicenseThreatRow().get(1).shouldHave(text("5"));
    table.highestLicenseThreatRow().get(2).shouldHave(text("5"));
    table.effectiveLicenseRow().get(1).shouldHave(text("Apache-2.0"));
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
    String unquarantineTimeString = getDateString(unquarantineTime, dateTimeFormatMask);
    String lastEvaluationTimeString = getDateString(lastEvaluationTime, dateTimeFormatMask);
    String quarantineTimeString = getDateString(quarantineTime, dateTimeFormatMask);
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
    String unquarantineTimeString = getDateString(unquarantineTime, dateTimeFormatMask);
    String lastEvaluationTimeString = getDateString(lastEvaluationTime, dateTimeFormatMask);
    String quarantineTimeString = getDateString(quarantineTime, dateTimeFormatMask);
    RepositoryComponent component =
        setupAllUnquarantinedComponentTestData(lastEvaluationTime, quarantineTime, unquarantineTime, true);
    testCompareVersionsValues_ForUnquarantinedComponent(component, lastEvaluationTimeString, quarantineTimeString,
        unquarantineTimeString, "Automatically");
  }

  private void testCompareButtons(int recommendationIndex) {
    RiskRemediationTile riskRemediation = firewallComponentDetailsPage.getRiskRemediationTile();

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
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();

    testCompareButtons(0);

    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();

    testCompareButtons(1);

    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();

    testCompareButtons(2);

    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();

    testCompareButtons(3);
  }

  @Test
  public void testComponentOverviewTile() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.getComponentOverviewTile().shouldBe(visible);
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(0).shouldHave(text("Exact"));
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(1).shouldHave(text("Sonatype"));
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(2).shouldHave(text("Visit Project Website"));
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(3).shouldHave(text("Other"));

    firewallComponentDetailsPage.getComponentOverviewTile().shouldBe(visible);
    firewallComponentDetailsPage.getViewCoordinatesButton().click();
    firewallComponentDetailsPage.getComponentCoordinatesPopOver().shouldBe(visible);
    firewallComponentDetailsPage.getComponentCoordinatesPopOverData(0).shouldHave(text("maven"));
    firewallComponentDetailsPage.getComponentCoordinatesPopOverData(1).shouldHave(text("com.lingocoder"));
    firewallComponentDetailsPage.getComponentCoordinatesPopOverData(2).shouldHave(text("abi.cli"));
    firewallComponentDetailsPage.getComponentCoordinatesPopOverData(3).shouldHave(text("0.5.2"));
    firewallComponentDetailsPage.getComponentCoordinatesPopOverCloseBtn().click();
    firewallComponentDetailsPage.getComponentCoordinatesPopOver().shouldNotBe(visible);
  }

  @Test
  public void testComponentDirectLinkFirewallPolicyViolations() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.getPolicyViolationsComponent().shouldBe(visible);
  }

  @Test
  public void testComponentFirewallPolicyViolationsClickTab() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();
    ElementsCollection tabs = firewallComponentDetailsPage.tabs();
    tabs.shouldHaveSize(5);
    tabs.get(1).click();
    tabs.get(1).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/violations?");

    firewallComponentDetailsPage.getPolicyViolationsComponent().shouldBe(visible);
  }

  @Test
  public void testComponentPolicyViolationsTitle() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.getComponentPolicyViolationsTitle().shouldBe(visible);
  }

  @Test
  public void testComponentPolicyViolationsTable() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.getComponentPolicyViolationsTable().shouldBe(visible);
  }

  @Test
  public void testComponentPolicyViolationsRows() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.getComponentPolicyViolationsTableCols().first().findAll(By.tagName("td"));
  }

  @Test
  public void testComponentPolicyViolationsRowHeaders() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.getComponentPolicyViolationsTableHeaders(0).shouldHave(text("Threat"));
    firewallComponentDetailsPage.getComponentPolicyViolationsTableHeaders(1).shouldHave(text("Policy/Action"));
    firewallComponentDetailsPage.getComponentPolicyViolationsTableHeaders(2).shouldHave(text("Constraint Name"));
    firewallComponentDetailsPage.getComponentPolicyViolationsTableHeaders(3).shouldHave(text("Condition"));

    eyesWatcher.eyesCheck("Firewall Component Details Page - Policy violation table ");
  }

  @Test
  public void testPolicyViolationsTableContent() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable =
        FirewallComponentDetailsPage.getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(6);

    ElementsCollection securityViolationCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    securityViolationCells.shouldHaveSize(6);
    securityViolationCells.get(0).shouldHave(text("10"));
    securityViolationCells.get(1).shouldHave(text("Security-High"));
    securityViolationCells.get(2).shouldHave(text("Security constraint"));
    securityViolationCells.get(3).shouldHave(text("security vulnerability severity >= 9.1"));
    securityViolationCells.get(4).shouldBe(empty);

    ElementsCollection securityLowViolationCells = policyViolationsTable.getRows().get(1).findAll(By.tagName("td"));
    securityLowViolationCells.get(0).shouldHave(text("6"));
    securityLowViolationCells.get(1).shouldHave(text("Security-Low"));
    securityLowViolationCells.get(2).shouldHave(text("Security-low constraint"));
    securityLowViolationCells.get(3).shouldHave(text("security vulnerability severity >= 4.3"));
    securityLowViolationCells.get(4).shouldBe(empty);

    ElementsCollection licenseViolationCells = policyViolationsTable.getRows().get(2).findAll(By.tagName("td"));
    licenseViolationCells.get(0).shouldHave(text("5"));
    licenseViolationCells.get(1).shouldHave(text("LicensePolicy"));
    licenseViolationCells.get(2).shouldHave(text("LicensePolicy constraint"));
    licenseViolationCells.get(3).shouldHave(text("Found license threat group"));
    licenseViolationCells.get(4).shouldBe(empty);

    ElementsCollection qualityAgeInDaysViolationCells =
        policyViolationsTable.getRows().get(3).findAll(By.tagName("td"));
    qualityAgeInDaysViolationCells.get(0).shouldHave(text("4"));
    qualityAgeInDaysViolationCells.get(1).shouldHave(text("QualityPolicyAgeInDays"));
    qualityAgeInDaysViolationCells.get(2).shouldHave(text("QualityPolicyAgeInDays constraint"));
    qualityAgeInDaysViolationCells.get(3).shouldHave(text("Found component younger than 50 days"));
    qualityAgeInDaysViolationCells.get(4).shouldBe(empty);

    ElementsCollection qualityPopularityViolationCells =
        policyViolationsTable.getRows().get(4).findAll(By.tagName("td"));
    qualityPopularityViolationCells.get(0).shouldHave(text("2"));
    qualityPopularityViolationCells.get(1).shouldHave(text("QualityPolicyRelativePopularity"));
    qualityPopularityViolationCells.get(2).shouldHave(text("QualityPolicyRelativePopularity constraint"));
    qualityPopularityViolationCells.get(3).shouldHave(text("Low popularity"));
    qualityPopularityViolationCells.get(4).shouldBe(empty);

    ElementsCollection otherViolationCells = policyViolationsTable.getRows().get(5).findAll(By.tagName("td"));
    otherViolationCells.get(0).shouldHave(text("1"));
    otherViolationCells.get(1).shouldHave(text("CoordinatesPolicy"));
    otherViolationCells.get(2).shouldHave(text("CoordinatesPolicy constraint"));
    otherViolationCells.get(3).shouldHave(text("Coordinates were com.lingocoder"));
    otherViolationCells.get(4).shouldBe(empty);
  }

  @Test
  public void testSecurityTabLoadByUrlChange() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlSecurityTab(component));
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.getSecurityTabContainer().shouldBe(visible);
  }

  @Test
  public void testSecurityTabLoadByTabClick() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();

    ElementsCollection tabs = firewallComponentDetailsPage.tabs();
    tabs.get(0).shouldHave(cssClass("active"));

    tabs.get(2).click();
    waitUntilSpinnersGone();
    tabs.get(2).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/security?");

    firewallComponentDetailsPage.getSecurityTabContainer().shouldBe(visible);
  }

  @Test
  public void testPolicyViolationTabVulnerabilitiesRowClick() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();

    violationRow1Cells.get(3).click();
    violationRow1Cells.get(0).shouldHave(text(policyViolationDetailPopover.popoverThreatLevel().getText()));
    violationRow1Cells.get(1).shouldHave(text(policyViolationDetailPopover.popoverHeaderTitle().getText()));
    violationRow1Cells.get(2).shouldHave(text(policyViolationDetailPopover.policyViolationText().getText()));
    violationRow1Cells.get(3).shouldHave(text(policyViolationDetailPopover.popoverList().getText()));
    policyViolationDetailPopover.getCloseButton().click();
  }

  @Test
  public void testManageWaiversPage_backButtonClick() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();

    violationRow1Cells.get(3).click();

    policyViolationDetailPopover.shouldBe(visible);
    SelenideElement manageWaiversButton = policyViolationDetailPopover.getAddWaiversButton();

    manageWaiversButton.click();

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.backButton().shouldHave(text("Back to Component Details")).click();
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.getComponentOverviewTile().shouldBe(visible);
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(0).shouldHave(text("Exact"));
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(1).shouldHave(text("Sonatype"));
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(2).shouldHave(text("Visit Project Website"));
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(3).shouldHave(text("Other"));
  }

  @Test
  public void testSecurityTabSecurityViolationsTable() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlSecurityTab(component));
    waitUntilSpinnersGone();

    PolicyViolationsTable policyViolationsTable =
        PolicyViolationsTable.getPolicyViolationsTableForParent(FirewallComponentDetailsPage.ROOT);
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(2);

    ElementsCollection policyViolationsRow1 = policyViolationsTable.getCellsByNthRow(1);
    ElementsCollection policyViolationsRow2 = policyViolationsTable.getCellsByNthRow(2);

    policyViolationsRow1.shouldHaveSize(6);
    policyViolationsRow1.get(0).shouldHave(text("10"));
    policyViolationsRow1.get(1).shouldHave(text("Security-High Proxy Failing"));
    policyViolationsRow1.get(2).shouldHave(text("Security constraint"));
    policyViolationsRow1.get(3).shouldHave(text("security vulnerability severity >= 9.1"));

    policyViolationsRow2.shouldHaveSize(6);
    policyViolationsRow2.get(0).shouldHave(text("6"));
    policyViolationsRow2.get(1).shouldHave(text("Security-Low Proxy Warning"));
    policyViolationsRow2.get(2).shouldHave(text("Security-low constraint"));
    policyViolationsRow2.get(3).shouldHave(text("security vulnerability severity >= 4.3"));
  }

  @Test
  public void testSecurityTabVulnerabilitiesTable() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlSecurityTab(component));
    waitUntilSpinnersGone();

    VulnerabilitiesTable vulnerabilitiesTable =
        VulnerabilitiesTable.getVulnerabilitiesTableForParent(FirewallComponentDetailsPage.ROOT);
    vulnerabilitiesTable.shouldBe(visible);

    ElementsCollection vulnerabilityRow1Cells = vulnerabilitiesTable.getCellsByNthRow(1);
    ElementsCollection vulnerabilityRow2Cells = vulnerabilitiesTable.getCellsByNthRow(2);

    vulnerabilityRow1Cells.shouldHaveSize(4);
    vulnerabilityRow1Cells.get(0).shouldHave(text("9"));
    vulnerabilityRow1Cells.get(1).shouldHave(text("sonatype-2017-0507"));
    vulnerabilityRow1Cells.get(2).shouldHave(text("Open"));
    vulnerabilityRow1Cells.get(3).shouldBe(empty);

    vulnerabilityRow2Cells.shouldHaveSize(4);
    vulnerabilityRow2Cells.get(0).shouldHave(text("4"));
    vulnerabilityRow2Cells.get(1).shouldHave(text("CVE-1234-56789"));
    vulnerabilityRow2Cells.get(2).shouldHave(text("Open"));
    vulnerabilityRow2Cells.get(3).shouldBe(empty);
  }

  private void mockHdsResponsesForVulnerabilityDetails() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails_CVE-1234-56789.json"))
        .atUri("rest/vulnerability/details/json/CVE-1234-56789");
    testCLMServer.getHdsServer().respondWith(getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails2.json"))
        .atUri("rest/vulnerability/details/json/sonatype-2017-0507");
  }

  @Test
  public void testSecurityTabVulnerabilitiesTableRowClick() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    mockHdsResponsesForVulnerabilityDetails();
    refreshOrOpen(FirewallComponentDetailsPage.urlSecurityTab(component));
    waitUntilSpinnersGone();

    VulnerabilitiesTable vulnerabilitiesTable =
        VulnerabilitiesTable.getVulnerabilitiesTableForParent(FirewallComponentDetailsPage.ROOT);
    vulnerabilitiesTable.shouldBe(visible);

    ElementsCollection vulnerabilityRow1Cells = vulnerabilitiesTable.getCellsByNthRow(1);
    ElementsCollection vulnerabilityRow2Cells = vulnerabilitiesTable.getCellsByNthRow(2);

    VulnerabilityDetailsPopover vulnerabilityDetailsPopover = new VulnerabilityDetailsPopover();

    vulnerabilityRow1Cells.get(3).click();
    vulnerabilityRow1Cells.get(2)
        .shouldHave(text(vulnerabilityDetailsPopover.getVulnerabilityOverrideForm().status().getElement().getText()));
    vulnerabilityRow1Cells.get(1).shouldHave(text(vulnerabilityDetailsPopover.vulnerabilityTitle().getText()));
    vulnerabilityDetailsPopover.getCloseButton().click();

    vulnerabilityRow2Cells.get(3).click();
    vulnerabilityRow2Cells.get(2)
        .shouldHave(text(vulnerabilityDetailsPopover.getVulnerabilityOverrideForm().status().getElement().getText()));
    vulnerabilityRow2Cells.get(1).shouldHave(text(vulnerabilityDetailsPopover.vulnerabilityTitle().getText()));
  }

  @Test
  public void testSecurityTabVulnerabilityOverrideForm_vulnerabilityOverride() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    mockHdsResponsesForVulnerabilityDetails();
    refreshOrOpen(FirewallComponentDetailsPage.urlSecurityTab(component));
    waitUntilSpinnersGone();

    VulnerabilitiesTable vulnerabilitiesTable =
        VulnerabilitiesTable.getVulnerabilitiesTableForParent(FirewallComponentDetailsPage.ROOT);
    vulnerabilitiesTable.shouldBe(visible);
    ElementsCollection vulnerabilityRowCells = vulnerabilitiesTable.getCellsByNthRow(2);
    VulnerabilityDetailsPopover vulnerabilityDetailsPopover = new VulnerabilityDetailsPopover();
    String overriddenVulnerabilityComment = "Vulnerability comment";
    VulnerabilityOverrideForm vulnerabilityOverrideForm = vulnerabilityDetailsPopover.getVulnerabilityOverrideForm();

    vulnerabilityRowCells.get(3).click();
    vulnerabilityRowCells.get(2)
        .shouldHave(text(vulnerabilityDetailsPopover.getVulnerabilityOverrideForm().status().getElement().getText()));
    vulnerabilityOverrideForm.comment().shouldNot(visible);
    vulnerabilityOverrideForm.status().click();
    vulnerabilityOverrideForm.status().listItem(2).click();
    vulnerabilityOverrideForm.comment().setValue(overriddenVulnerabilityComment);
    vulnerabilityOverrideForm.submitButton().click();
    waitUntilSpinnersGone();
    vulnerabilityDetailsPopover.getCloseButton().click();

    vulnerabilityRowCells.get(3).click();
    vulnerabilityRowCells.get(2)
        .shouldHave(text(vulnerabilityDetailsPopover.getVulnerabilityOverrideForm().status().getElement().getText()));
    vulnerabilityOverrideForm.comment().shouldHave(text(overriddenVulnerabilityComment));
  }

  @Test
  public void testViolationTabWaiverTable() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    Date waiverCreateDate = date;
    String waiverCreateDateString = getDateString(waiverCreateDate, dateFormatMask);

    ComponentWaiversPopover componentWaiversPopover = new ComponentWaiversPopover();
    ComponentWaiversPopoverTable componentWaiversTable = componentWaiversPopover.componentWaiversPopoverTable();

    firewallComponentDetailsPage.getPolicyViolationsComponent().shouldBe(visible);
    firewallComponentDetailsPage.firewallWaiversButton().click();

    componentWaiversPopover.shouldBe(visible);
    componentWaiversPopover.title().shouldHave(text("Component Waivers"));
    componentWaiversTable.shouldBe(visible);
    componentWaiversTable.getRows().shouldHaveSize(1);

    ElementsCollection waiversTableCells = componentWaiversTable.getCellsByNthRow(1);

    waiversTableCells.shouldHaveSize(7);
    waiversTableCells.get(0).shouldHave(text("Security-Low Security-low constraint"));
    waiversTableCells.get(1).shouldHave(text(waiverCreateDateString));
    waiversTableCells.get(2).shouldHave(text("Organization - Root Organization"));
    waiversTableCells.get(3).shouldBe(text("com.lingocoder : abi.cli : 0.5.2"));
    waiversTableCells.get(4).shouldBe(text("Test User"));
    waiversTableCells.get(5).shouldBe(text("Test comment for waiver"));
  }

  private void testLegalTabPolicyViolationsTable() {
    PolicyViolationsTable policyViolationsTable =
        PolicyViolationsTable.getPolicyViolationsTableForParent(FirewallComponentDetailsPage.ROOT);
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);

    ElementsCollection policyViolationsRow1 = policyViolationsTable.getCellsByNthRow(1);

    policyViolationsRow1.shouldHaveSize(6);
    policyViolationsRow1.get(0).shouldHave(text("5"));
    policyViolationsRow1.get(1).shouldHave(text("LicensePolicy Proxy Warning"));
    policyViolationsRow1.get(2).shouldHave(text("LicensePolicy constraint"));
    policyViolationsRow1.get(3).shouldHave(text("Found license threat group"));
  }

  @Test
  public void testLegalTab_singleLicenseComponent() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
    waitUntilSpinnersGone();
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("Apache-2.0"));

    licenseDetectionsTile.status().shouldHave(text("Open"));

    testLegalTabPolicyViolationsTable();
  }

  @Test
  public void testLegalTab_multipleLicenseComponent() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData(multiLicensed);

    refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
    waitUntilSpinnersGone();
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(2);
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));
    effectiveLicenses.get(1).shouldHave(text("EPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("EPL-1.0"));

    licenseDetectionsTile.status().shouldHave(text("Open"));

    testLegalTabPolicyViolationsTable();
  }

  @Test
  public void testLegalTab_nonLicensedComponent() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData(nonLicensed);

    refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
    waitUntilSpinnersGone();
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("Not Provided"));

    licenseDetectionsTile.status().shouldHave(text("Open"));

    testLegalTabPolicyViolationsTable();
  }

  @Test
  public void testLegalTab_overridenLicenseComponent() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData(overriddenLicense);

    refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
    waitUntilSpinnersGone();
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("Apache-2.0"));

    licenseDetectionsTile.status().shouldHave(text("Overridden"));

    testLegalTabPolicyViolationsTable();
  }

  private void setOverriddenLicensesStatus(EditLicensesPopover editPopover, int scope, String comment) {
    waitUntilSpinnersGone();
    editPopover.scope(scope).click();
    editPopover.status().click();
    editPopover.statuses().get(EditLicensesPopover.LicensesStatuses.OVERRIDDEN.ordinal()).click();
    waitUntilSpinnersGone();
    editPopover.availableLicensesTransferListItems().shouldBe(sizeGreaterThan(1));
    editPopover.selectedLicensesTransferListItems().shouldHaveSize(0);
    editPopover.availableLicensesTransferListItems().get(0).click();
    editPopover.availableLicensesTransferListItems().get(0).click();
    editPopover.selectedLicensesTransferListItems().shouldHaveSize(2);
    
    editPopover.comment().setValue(comment);
    editPopover.saveButton().click();
    waitUntilSpinnersGone();
    NxSubmitMask.seeAndWaitForDismissal();
  }

  @Test
  public void testLegalTab_overridenLicensesStatus() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData(overriddenLicense);

    refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
    waitUntilSpinnersGone();
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("Apache-2.0"));

    licenseDetectionsTile.status().shouldHave(text("Overridden"));
    
    licenseDetectionsTile.editLicenseButton().click();

    EditLicensesPopover editPopover = new EditLicensesPopover();
    String testComment = "test comment";
    setOverriddenLicensesStatus(editPopover, EditLicensesPopover.RepositoryComponentLicensesScopes.REPOSITORY.ordinal(),
        testComment);
    editPopover.getCloseButton().click();

    licenseDetectionsTile.editLicenseButton().click();
    editPopover.selectedLicensesTransferListItems().shouldHaveSize(2);
    editPopover.comment().shouldHave(text(testComment));

    effectiveLicenses.shouldHaveSize(2);
    effectiveLicenses.first().shouldHave(text("0BSD"));
    effectiveLicenses.get(1).shouldHave(text("10tec-Company-License-Agreement"));
    
    licenseDetectionsTile.status().shouldHave(text("Overridden"));
  }

  @Test
  public void testLegalTab_inheritedLicensesStatus() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData(overriddenLicense);

    refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
    waitUntilSpinnersGone();
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("Apache-2.0"));

    licenseDetectionsTile.status().shouldHave(text("Overridden")); 
    
    licenseDetectionsTile.editLicenseButton().click();

    EditLicensesPopover editPopover = new EditLicensesPopover();

    String testComment = "test comment";
    setOverriddenLicensesStatus(editPopover, EditLicensesPopover.RepositoryComponentLicensesScopes.REPOSITORY.ordinal(),
        testComment);
    
    editPopover.status().click();
    editPopover.statuses().get(EditLicensesPopover.LicensesStatuses.INHERITED.ordinal()).click();
    editPopover.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    waitUntilSpinnersGone();
    editPopover.availableScopes().get(EditLicensesPopover.RepositoryComponentLicensesScopes.ORGANIZATION.ordinal())
        .shouldHave(Condition.attribute("className", "nx-radio-checkbox nx-radio tm-checked"));
    String inherietedScope = editPopover.scopeStatuses().last().getText().replaceAll("[()]", "");
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));
    licenseDetectionsTile.status().shouldHave(text(inherietedScope));
  }

  @Test
  public void testLegalTab_selectedLicensesStatus() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData(overriddenLicense);

    refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
    waitUntilSpinnersGone();
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("Apache-2.0"));

    licenseDetectionsTile.status().shouldHave(text("Overridden")); 
    
    licenseDetectionsTile.editLicenseButton().click();

    EditLicensesPopover editPopover = new EditLicensesPopover();

    editPopover.status().click();
    editPopover.statuses().get(EditLicensesPopover.LicensesStatuses.SELECTED.ordinal()).click();
    editPopover.selectedLicensesCheckBoxElements().get(0).click();
    editPopover.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    waitUntilSpinnersGone();
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));
    licenseDetectionsTile.status().shouldHave(text("Selected"));
  }

  @Test
  public void testLegalTab_openAcknowledgeAndConfirmedLicensesStatus() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData(overriddenLicense);

    refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
    waitUntilSpinnersGone();
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("Apache-2.0"));

    licenseDetectionsTile.status().shouldHave(text("Overridden")); 
    
    licenseDetectionsTile.editLicenseButton().click();

    EditLicensesPopover editPopover = new EditLicensesPopover();

    editPopover.status().click();
    editPopover.statuses().get(EditLicensesPopover.LicensesStatuses.OPEN.ordinal()).click();
    editPopover.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    licenseDetectionsTile.status().shouldHave(text("Open"));

    editPopover.status().click();
    editPopover.statuses().get(EditLicensesPopover.LicensesStatuses.ACKNOWLEDGED.ordinal()).click();
    editPopover.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    licenseDetectionsTile.status().shouldHave(text("Acknowledged"));

    editPopover.status().click();
    editPopover.statuses().get(EditLicensesPopover.LicensesStatuses.CONFIRMED.ordinal()).click();
    editPopover.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    licenseDetectionsTile.status().shouldHave(text("Confirmed"));

    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));
  }

  @Test
  public void testPolicyViolationTabManageWaiversButtonClick() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();

    violationRow1Cells.get(3).click();

    policyViolationDetailPopover.shouldBe(visible);
    SelenideElement manageWaiversButton = policyViolationDetailPopover.getAddWaiversButton();

    manageWaiversButton.click();

    ListWaiversPage waiversForViolationPage = new ListWaiversPage();
    waiversForViolationPage.title().shouldHave(text("Waivers for Violation"));
    waiversForViolationPage.backButton().shouldHave(text("Back to Component Details"));
    waiversForViolationPage.componentName().shouldHave(text("com.lingocoder : abi.cli : 0.5.2"));
    waiversForViolationPage.waiverListTable().rows().shouldHaveSize(1);
  }
  
  public void testComponentReEvaluation() throws Exception {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();

    refreshOrOpen(FirewallComponentDetailsPage.overviewTab(component));
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = firewallComponentDetailsPage.getRiskRemediationTile();
    ScrollUtil.scrollIntoView(riskRemediation.compareVersionsTitle());
    RiskRemediationTile.CompareVersionsTable table = riskRemediation.compareVersionsTable();
    String firstEvaluation = table.latestEvaluationRow().get(1).getText();

    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    // Sanity check
    PolicyViolationsTable policyViolationsTable =
        PolicyViolationsTable.getPolicyViolationsTableForParent(FirewallComponentDetailsPage.ROOT);
    policyViolationsTable.getRows().shouldHaveSize(6);

    // Mock HDS response for firewall component policy evaluation
    ComponentDetails componentDetails = componentDetailsArrayList.get(0);
    ComponentEvaluationDataList hdsResponse = new ComponentEvaluationDataList();
    hdsResponse.components.add(toComponentEvaluationData(componentDetails));
    testCLMServer.getHdsServer().respondWith(hdsResponse).atUri("/rest/component/details/firewall");

    new PolicyDAO().delete(securityLowPolicy);

    firewallComponentDetailsPage.reevaluateButton().click();

    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    // One policy was deleted, so we expect one policy violation less than before
    PolicyViolationsTable policyViolationsTableReevaluation =
        PolicyViolationsTable.getPolicyViolationsTableForParent(FirewallComponentDetailsPage.ROOT);
    policyViolationsTableReevaluation.getRows().shouldHaveSize(5);

    refreshOrOpen(FirewallComponentDetailsPage.overviewTab(component));

    RiskRemediationTile riskRemediationRevaluation = firewallComponentDetailsPage.getRiskRemediationTile();
    ScrollUtil.scrollIntoView(riskRemediationRevaluation.compareVersionsTitle());
    RiskRemediationTile.CompareVersionsTable tableReevaluation = riskRemediationRevaluation.compareVersionsTable();
    String latestEvaluation = tableReevaluation.latestEvaluationRow().get(1).getText();

    Date firstEvaluationDate = getDate(firstEvaluation, dateTimeFormatMask);
    Date latestEvaluationDate = getDate(latestEvaluation, dateTimeFormatMask);
    assertEvaluationDates(firstEvaluationDate, latestEvaluationDate);
  }

  private void assertEvaluationDates(Date firstEvaluation, Date latestEvaluation) {
    assertThat(firstEvaluation).isBefore(latestEvaluation);
  }

  private ComponentEvaluationData toComponentEvaluationData(ComponentDetails componentDetails) {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();
    componentEvaluationData.hash = componentDetails.getHash();
    componentEvaluationData.componentIdentifier = componentDetails.getComponentIdentifier();
    componentEvaluationData.matchState = componentDetails.getMatchState();
    componentEvaluationData.declaredLicenses = componentDetails.getDeclaredLicenses();
    componentEvaluationData.observedLicenses = componentDetails.getObservedLicenses();
    componentEvaluationData.catalogDate = componentDetails.getCatalogDate();
    componentEvaluationData.relativePopularity = componentDetails.getRelativePopularity();
    componentEvaluationData.securityVulnerabilities = componentDetails.getSecurityVulnerabilities();

    return componentEvaluationData;

  }

  @Test
  public void testRemoveComponentWaiverUsingPopover() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    SelenideElement viewAllComponentWaiversButton = firewallComponentDetailsPage.getViewAllComponentWaiversButton();
    viewAllComponentWaiversButton.click();

    // Sanity Check
    ComponentWaiversPopover componentWaiversPopover = new ComponentWaiversPopover();
    ComponentWaiversPopoverTable componentWaiversTable = componentWaiversPopover.componentWaiversPopoverTable();
    componentWaiversTable.getRows().shouldHaveSize(1);

    firewallComponentDetailsPage.getDeleteWaiverButton().click();

    firewallComponentDetailsPage.getDeleteWaiverModal().shouldBe(visible);

    firewallComponentDetailsPage.getDeleteWaiverModalButton().click();

    ComponentWaiversPopover componentWaiversPopoverRefreshed = new ComponentWaiversPopover();
    ComponentWaiversPopoverTable componentWaiversTableRefreshed =
        componentWaiversPopoverRefreshed.componentWaiversPopoverTable();
    componentWaiversTableRefreshed.getRows().get(0).shouldHave(text("No existing component waivers"));
  }

  @Test
  public void testOpenAddWaiverPage() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();

    violationRow1Cells.get(3).click();

    policyViolationDetailPopover.shouldBe(visible);
    SelenideElement manageWaiversButton = policyViolationDetailPopover.getAddWaiversButton();

    manageWaiversButton.click();

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.waiverListTable().noWaiversMessage().shouldBe(visible);
    listWaiversPage.addWaiverButton().shouldBe(visible, enabled).click();
    
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.artifactName().shouldHave(text("abi.cli"));
    addWaiverPage.componentName().shouldHave(text("com.lingocoder : abi.cli : 0.5.2"));
    addWaiverPage.policyName().shouldHave(text("Security-High"));
    addWaiverPage.constraintName().shouldHave(text("Security Constraint"));
    addWaiverPage.conditions().shouldHaveSize(1);
    addWaiverPage.condition(1).shouldHave(text("security vulnerability severity >= 9.1"));
    addWaiverPage.availableScopes().shouldHaveSize(3);
    addWaiverPage.scope(0).label().shouldHave(text("Repository - repositoryPublicId"));
    addWaiverPage.scope(1).label().shouldHave(text("All Repositories"));
    addWaiverPage.scope(2).label().shouldHave(text("Organization - Root Organization"));
    addWaiverPage.availableComponents().shouldHaveSize(3);
    addWaiverPage.component(0).label().shouldHave(text("com.lingocoder : abi.cli : 0.5.2"));
    addWaiverPage.component(1).label().shouldHave(text("com.lingocoder : abi.cli"));
    addWaiverPage.component(2).label().shouldHave(text("All Components"));
    addWaiverPage.currentUserName().scrollIntoView(true).shouldHave(text("Admin BuiltIn"));
  }

  @Test
  public void testAddWaiverPage_TimeOptions() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();

    violationRow1Cells.get(3).click();

    policyViolationDetailPopover.shouldBe(visible);
    SelenideElement manageWaiversButton = policyViolationDetailPopover.getAddWaiversButton();

    manageWaiversButton.click();

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.waiverListTable().noWaiversMessage().shouldBe(visible);
    listWaiversPage.addWaiverButton().shouldBe(visible, enabled).click();
    
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.comments().shouldHave(text(""));
    addWaiverPage.expiryTimesOptions().shouldHaveSize(8);
    addWaiverPage.expiryTimesOptions().get(0).shouldHave(text("Never"));
    addWaiverPage.expiryTimesOptions().get(1).shouldHave(text("7 Days"));
    addWaiverPage.expiryTimesOptions().get(2).shouldHave(text("14 Days"));
    addWaiverPage.expiryTimesOptions().get(3).shouldHave(text("30 Days"));
    addWaiverPage.expiryTimesOptions().get(4).shouldHave(text("60 Days"));
    addWaiverPage.expiryTimesOptions().get(5).shouldHave(text("90 Days"));
    addWaiverPage.expiryTimesOptions().get(6).shouldHave(text("120 Days"));
    addWaiverPage.expiryTimesOptions().get(7).shouldHave(text("Custom"));
    addWaiverPage.expiryTimesSelect().getSelectedOption().shouldHave(text("Never"));
  }

  @Test
  public void tesAddWaiverComponent_cancelButtonClick() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();

    violationRow1Cells.get(3).click();

    policyViolationDetailPopover.shouldBe(visible);
    SelenideElement manageWaiversButton = policyViolationDetailPopover.getAddWaiversButton();

    manageWaiversButton.click();

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.waiverListTable().noWaiversMessage().shouldBe(visible);
    listWaiversPage.addWaiverButton().shouldBe(visible, enabled).click();
    
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHaveSize(3);
    addWaiverPage.cancelButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
  }

  @Test
  public void tesAddWaiverComponent__clickingDifferentScopes_and_submitButtonClick() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    Date createDate = date;
    String dateCreated = getDateString(createDate, dateFormatMask);

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();

    violationRow1Cells.get(3).click();

    policyViolationDetailPopover.shouldBe(visible);
    SelenideElement manageWaiversButton = policyViolationDetailPopover.getAddWaiversButton();

    manageWaiversButton.click();

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.waiverListTable().noWaiversMessage().shouldBe(visible);
    listWaiversPage.addWaiverButton().shouldBe(visible, enabled).click();
    
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHaveSize(3);
    NxRadio chosenScope = addWaiverPage.scope(0);
    chosenScope.label().shouldHave(text("Repository - repositoryPublicId"));
    chosenScope.click();
    addWaiverPage.availableComponents().shouldHaveSize(3);
    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.label().shouldHave(text("All Components"));
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    listWaiversPage.waiverListTable().noWaiversMessage().shouldNotBe(visible);
    listWaiversPage.waiverListTable().rows().shouldHaveSize(1);
    listWaiversPage.waiverListTable().row(1).comments().shouldHave(text("Some comments"));
    listWaiversPage.waiverListTable().row(1).createdBy().shouldHave(text("Admin BuiltIn"));
    listWaiversPage.waiverListTable().row(1).waiverExpiration().shouldHave(text("Does not expire"));
    listWaiversPage.waiverListTable().row(1).components().shouldHave(text("All"));
    listWaiversPage.waiverListTable().row(1).scope().shouldHave(text("Repository - repository"));
    listWaiversPage.waiverListTable().row(1).dateCreated().shouldHave(text(dateCreated));
  }

  @Test
  public void tesAddWaiverComponent_findBackButtonAndClick() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();

    violationRow1Cells.get(3).click();

    policyViolationDetailPopover.shouldBe(visible);
    SelenideElement manageWaiversButton = policyViolationDetailPopover.getAddWaiversButton();

    manageWaiversButton.click();

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.waiverListTable().noWaiversMessage().shouldBe(visible);
    listWaiversPage.addWaiverButton().shouldBe(visible, enabled).click();
    
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHaveSize(3);
    NxRadio chosenScope = addWaiverPage.scope(0);
    chosenScope.label().shouldHave(text("Repository - repositoryPublicId"));
    chosenScope.click();
    addWaiverPage.availableComponents().shouldHaveSize(3);
    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.label().shouldHave(text("All Components"));
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    listWaiversPage.backButton().shouldHave(text("Back to Component Details")).click();
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.getComponentOverviewTile().shouldBe(visible);
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(0).shouldHave(text("Exact"));
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(1).shouldHave(text("Sonatype"));
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(2).shouldHave(text("Visit Project Website"));
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(3).shouldHave(text("Other"));
  }

  @Test
  public void testRemoveComponentWaiverUsingPopover_legalTab() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));

    waitUntilSpinnersGone();

    SelenideElement viewAllComponentWaiversButton = firewallComponentDetailsPage.getViewAllComponentWaiversButton();
    viewAllComponentWaiversButton.click();

    // Sanity Check
    ComponentWaiversPopover componentWaiversPopover = new ComponentWaiversPopover();
    ComponentWaiversPopoverTable componentWaiversTable = componentWaiversPopover.componentWaiversPopoverTable();
    componentWaiversTable.getRows().shouldHaveSize(1);

    firewallComponentDetailsPage.getDeleteWaiverButton().click();

    firewallComponentDetailsPage.getDeleteWaiverModal().shouldBe(visible);

    firewallComponentDetailsPage.getDeleteWaiverModalButton().click();

    ComponentWaiversPopover componentWaiversPopoverRefreshed = new ComponentWaiversPopover();
    ComponentWaiversPopoverTable componentWaiversTableRefreshed =
        componentWaiversPopoverRefreshed.componentWaiversPopoverTable();
    componentWaiversTableRefreshed.getRows().get(0).shouldHave(text("No existing component waivers"));
  }
}
