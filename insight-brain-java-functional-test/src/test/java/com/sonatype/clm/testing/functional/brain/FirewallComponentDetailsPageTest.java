/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.componentdetails.FirewallPolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilitiesTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilityDetailsPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilityDetailsPopover.VulnerabilityOverrideForm;
import com.sonatype.clm.testing.functional.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
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
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.codeborne.selenide.ElementsCollection;
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
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallComponentDetailsPageTest
    extends AbstractFunctionalTest
{
  private final List<ComponentDetails> componentDetailsArrayList = new ArrayList<>();

  private final FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();

  // declared and observed licenses are not the same
  private final String multiLicensed = "multiLicensed";

  // declared and observed licenses are the same
  private final String singleLicense = "singleLicense";

  // no declared or observed licenses are provided
  private final String nonLicensed = "nonLicensed";

  // overridden licenses by the user in IQ
  private final String overriddenLicense = "overriddenLicense";

  private MultiLicenseDAO multiLicenseDAO;

  private RepositoryComponentDAO repositoryComponentDAO;

  private PolicyDAO policyDAO;

  private Repository repository;

  private RepositoryManager repositoryManager;

  private Policy securityHighPolicy;

  private Policy securityLowPolicy;

  private Policy licensePolicy;

  private Policy popularityPolicy;

  private Policy agePolicy;

  private Policy coordinatesPolicy;

  private Application app;

  private Date date;

  private Configuration configurationService;

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
    multiLicenseDAO = lookup(MultiLicenseDAO.class);
    repositoryComponentDAO = lookup(RepositoryComponentDAO.class);
    policyDAO = lookup(PolicyDAO.class);

    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    repositoryManager = tempEntity.newRepositoryManager();
    repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
    date = new Date();
    app = tempEntity.newApplication("publicId", tempEntity.newOrganization().getId());

    configurationService = lookup(Configuration.class);
    assertThat(configurationService.isALPObservedLicenseDetectionEnabled()).isTrue();
  }

  private void waitUntilSpinnersGone() {
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.invisibilityOf(firewallComponentDetailsPage.getAllLoadingSpinners().get(0)));
  }

  private ComponentDetails createComponentDetail(
      String hash,
      ComponentIdentifier componentIdentifier,
      String licenseCondition)
  {
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
      testCLMServer.getHdsServer()
          .respondWith(mainComponentDetail)
          .atUri("/rest/ci/componentDetails?" + "componentIdentifier="
              + URLEncoder.encode(JsonUtils.format(mainComponentDetail.getComponentIdentifier()), "UTF-8") + "&hash="
              + mainComponentDetail.getHash());
      // Used when multi license details are requested
      testCLMServer.getHdsServer()
          .respondWith(mainComponentDetail)
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

  private void createAllTypePolicies() {
    createSecurityPolicies();
    createLicensePolicies();
    createQualityPolicies();
    createOtherPolicies();
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
      // Creating similar waiver
      tempEntity.newWaiver(repositoryComponent.getHash(), securityLowPolicy.getId(),
          app.getId(), Collections.singletonList(securityLowConstraintFact),
          PackageUrlIdentifier.toPackageUrl(repositoryComponent.getComponentIdentifier()),
          ComponentMatcherStrategyForWaiver.ALL_VERSIONS,
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
    return new FluentWait<>(getWebDriver()).withTimeout(Duration.ofSeconds(240))
        .pollingEvery(Duration.ofSeconds(2))
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
  public void testComponentOverviewTileFromFirewallDashboard() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    testComponentOverviewTile(FirewallComponentDetailsPage.defaultUrl(component));
  }

  public void testComponentOverviewTile(String url) {
    refreshOrOpen(url);
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
  public void testComponentPolicyViolationsTileFromFirewallDashboard() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    testComponentPolicyViolationsTile(FirewallComponentDetailsPage.urlViolationsTab(component));
  }

  public void testComponentPolicyViolationsTile(String url) {
    refreshOrOpen(url);
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.getPolicyViolationsComponent().shouldBe(visible);
    firewallComponentDetailsPage.getComponentPolicyViolationsTitle().shouldBe(visible);
    firewallComponentDetailsPage.getComponentPolicyViolationsTable().shouldBe(visible);
    firewallComponentDetailsPage.getComponentPolicyViolationsTableCols().first().findAll(By.tagName("td"));
    testComponentPolicyViolationsRowHeaders();
    testPolicyViolationsTableContent();
  }

  public void testComponentPolicyViolationsRowHeaders() {
    firewallComponentDetailsPage.getComponentPolicyViolationsTableHeaders(0).shouldHave(text("Threat"));
    firewallComponentDetailsPage.getComponentPolicyViolationsTableHeaders(1).shouldHave(text("Policy/Action"));
    firewallComponentDetailsPage.getComponentPolicyViolationsTableHeaders(2).shouldHave(text("Constraint Name"));
    firewallComponentDetailsPage.getComponentPolicyViolationsTableHeaders(3).shouldHave(text("Condition"));

    eyesWatcher.eyesCheck("Firewall Component Details Page - Policy violation table ");
  }

  public void testPolicyViolationsTableContent() {
    FirewallPolicyViolationsTable policyViolationsTable =
        FirewallComponentDetailsPage.getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(6));

    ElementsCollection securityViolationCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    securityViolationCells.shouldHave(size(6));
    securityViolationCells.get(0).shouldHave(text("10"));
    securityViolationCells.get(1).shouldHave(text("Security-High Proxy Failing"));
    securityViolationCells.get(2).shouldHave(text("Security constraint"));
    securityViolationCells.get(3).shouldHave(text("security vulnerability severity >= 9.1"));
    securityViolationCells.get(4).shouldBe(empty);

    ElementsCollection securityLowViolationCells = policyViolationsTable.getRows().get(1).findAll(By.tagName("td"));
    securityLowViolationCells.get(0).shouldHave(text("6"));
    securityLowViolationCells.get(1).shouldHave(text("Security-Low Proxy Warning"));
    securityLowViolationCells.get(2).shouldHave(text("Security-low constraint"));
    securityLowViolationCells.get(3).shouldHave(text("security vulnerability severity >= 4.3"));
    securityLowViolationCells.get(4).shouldHave(text("1 Active Waiver"));

    ElementsCollection licenseViolationCells = policyViolationsTable.getRows().get(2).findAll(By.tagName("td"));
    licenseViolationCells.get(0).shouldHave(text("5"));
    licenseViolationCells.get(1).shouldHave(text("LicensePolicy Proxy Warning"));
    licenseViolationCells.get(2).shouldHave(text("LicensePolicy constraint"));
    licenseViolationCells.get(3).shouldHave(text("Found license threat group"));
    licenseViolationCells.get(4).shouldBe(empty);

    ElementsCollection qualityAgeInDaysViolationCells =
        policyViolationsTable.getRows().get(3).findAll(By.tagName("td"));
    qualityAgeInDaysViolationCells.get(0).shouldHave(text("4"));
    qualityAgeInDaysViolationCells.get(1).shouldHave(text("QualityPolicyAgeInDays Proxy Warning"));
    qualityAgeInDaysViolationCells.get(2).shouldHave(text("QualityPolicyAgeInDays constraint"));
    qualityAgeInDaysViolationCells.get(3).shouldHave(text("Found component younger than 50 days"));
    qualityAgeInDaysViolationCells.get(4).shouldBe(empty);

    ElementsCollection qualityPopularityViolationCells =
        policyViolationsTable.getRows().get(4).findAll(By.tagName("td"));
    qualityPopularityViolationCells.get(0).shouldHave(text("2"));
    qualityPopularityViolationCells.get(1).shouldHave(text("QualityPolicyRelativePopularity Proxy Warning"));
    qualityPopularityViolationCells.get(2).shouldHave(text("QualityPolicyRelativePopularity constraint"));
    qualityPopularityViolationCells.get(3).shouldHave(text("Low popularity"));
    qualityPopularityViolationCells.get(4).shouldBe(empty);

    ElementsCollection otherViolationCells = policyViolationsTable.getRows().get(5).findAll(By.tagName("td"));
    otherViolationCells.get(0).shouldHave(text("1"));
    otherViolationCells.get(1).shouldHave(text("CoordinatesPolicy Proxy Failing"));
    otherViolationCells.get(2).shouldHave(text("CoordinatesPolicy constraint"));
    otherViolationCells.get(3).shouldHave(text("Coordinates were com.lingocoder"));
    otherViolationCells.get(4).shouldBe(empty);
  }

  @Test
  public void testSecurityTabFromFirewallDashboard() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    String url = FirewallComponentDetailsPage.urlSecurityTab(component);
    testSecurityTabSecurityViolationsTable(url);
    testSecurityTabVulnerabilitiesTable(url);
    testSecurityTabVulnerabilitiesTableRowClick(url);
    testSecurityTabVulnerabilityOverrideForm_vulnerabilityOverride(url);
  }

  public void testSecurityTabSecurityViolationsTable(String url) {
    refreshOrOpen(url);
    waitUntilSpinnersGone();

    PolicyViolationsTable policyViolationsTable =
        PolicyViolationsTable.getPolicyViolationsTableForParent(FirewallComponentDetailsPage.ROOT);
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(2));

    ElementsCollection policyViolationsRow1 = policyViolationsTable.getCellsByNthRow(1);
    ElementsCollection policyViolationsRow2 = policyViolationsTable.getCellsByNthRow(2);

    policyViolationsRow1.shouldHave(size(6));
    policyViolationsRow1.get(0).shouldHave(text("10"));
    policyViolationsRow1.get(1).shouldHave(text("Security-High Proxy Failing"));
    policyViolationsRow1.get(2).shouldHave(text("Security constraint"));
    policyViolationsRow1.get(3).shouldHave(text("security vulnerability severity >= 9.1"));

    policyViolationsRow2.shouldHave(size(6));
    policyViolationsRow2.get(0).shouldHave(text("6"));
    policyViolationsRow2.get(1).shouldHave(text("Security-Low Proxy Warning"));
    policyViolationsRow2.get(2).shouldHave(text("Security-low constraint"));
    policyViolationsRow2.get(3).shouldHave(text("security vulnerability severity >= 4.3"));
  }

  public void testSecurityTabVulnerabilitiesTable(String url) {
    refreshOrOpen(url);
    waitUntilSpinnersGone();

    VulnerabilitiesTable vulnerabilitiesTable =
        VulnerabilitiesTable.getVulnerabilitiesTableForParent(FirewallComponentDetailsPage.ROOT);
    vulnerabilitiesTable.shouldBe(visible);

    ElementsCollection vulnerabilityRow1Cells = vulnerabilitiesTable.getCellsByNthRow(1);
    ElementsCollection vulnerabilityRow2Cells = vulnerabilitiesTable.getCellsByNthRow(2);

    vulnerabilityRow1Cells.shouldHave(size(5));
    vulnerabilityRow1Cells.get(0).shouldHave(text("9"));
    vulnerabilityRow1Cells.get(1).shouldHave(text("sonatype-2017-0507"));
    vulnerabilityRow1Cells.get(2).shouldBe(empty);
    vulnerabilityRow1Cells.get(3).shouldHave(text("Open"));
    vulnerabilityRow1Cells.get(4).shouldBe(empty);

    vulnerabilityRow2Cells.shouldHave(size(5));
    vulnerabilityRow2Cells.get(0).shouldHave(text("4"));
    vulnerabilityRow2Cells.get(1).shouldHave(text("CVE-1234-56789"));
    vulnerabilityRow2Cells.get(2).shouldBe(empty);
    vulnerabilityRow2Cells.get(3).shouldHave(text("Open"));
    vulnerabilityRow2Cells.get(4).shouldBe(empty);
  }

  private void mockHdsResponsesForVulnerabilityDetails() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails_CVE-1234-56789.json"))
        .atUri("rest/vulnerability/details/json/CVE-1234-56789");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails2.json"))
        .atUri("rest/vulnerability/details/json/sonatype-2017-0507");
    testCLMServer.getHdsServer()
        .respondWith(Collections.emptyMap())
        .atUri("rest/vulnerability/details/json");
  }

  public void testSecurityTabVulnerabilitiesTableRowClick(String url) {
    mockHdsResponsesForVulnerabilityDetails();
    refreshOrOpen(url);
    waitUntilSpinnersGone();

    VulnerabilitiesTable vulnerabilitiesTable =
        VulnerabilitiesTable.getVulnerabilitiesTableForParent(FirewallComponentDetailsPage.ROOT);
    vulnerabilitiesTable.shouldBe(visible);

    ElementsCollection vulnerabilityRow1Cells = vulnerabilitiesTable.getCellsByNthRow(1);
    ElementsCollection vulnerabilityRow2Cells = vulnerabilitiesTable.getCellsByNthRow(2);

    VulnerabilityDetailsPopover vulnerabilityDetailsPopover = new VulnerabilityDetailsPopover();

    vulnerabilityRow1Cells.get(4).click();
    waitUntilSpinnersGone();
    vulnerabilityRow1Cells.get(3)
        .shouldHave(text(vulnerabilityDetailsPopover.getVulnerabilityOverrideForm().status().getElement().getText()));
    vulnerabilityRow1Cells.get(1).shouldHave(text(vulnerabilityDetailsPopover.vulnerabilityTitle().getText()));
    vulnerabilityDetailsPopover.getCloseButton().click();

    vulnerabilityRow2Cells.get(4).click();
    waitUntilSpinnersGone();
    vulnerabilityRow2Cells.get(3)
        .shouldHave(text(vulnerabilityDetailsPopover.getVulnerabilityOverrideForm().status().getElement().getText()));
    vulnerabilityRow2Cells.get(1).shouldHave(text(vulnerabilityDetailsPopover.vulnerabilityTitle().getText()));
    vulnerabilityDetailsPopover.getCloseButton().click();
  }

  public void testSecurityTabVulnerabilityOverrideForm_vulnerabilityOverride(String url) {
    mockHdsResponsesForVulnerabilityDetails();
    refreshOrOpen(url);
    waitUntilSpinnersGone();

    VulnerabilitiesTable vulnerabilitiesTable =
        VulnerabilitiesTable.getVulnerabilitiesTableForParent(FirewallComponentDetailsPage.ROOT);
    vulnerabilitiesTable.shouldBe(visible);
    ElementsCollection vulnerabilityRowCells = vulnerabilitiesTable.getCellsByNthRow(2);
    VulnerabilityDetailsPopover vulnerabilityDetailsPopover = new VulnerabilityDetailsPopover();
    String overriddenVulnerabilityComment = "Vulnerability comment";
    VulnerabilityOverrideForm vulnerabilityOverrideForm = vulnerabilityDetailsPopover.getVulnerabilityOverrideForm();

    vulnerabilityRowCells.get(4).click();
    waitUntilSpinnersGone();
    vulnerabilityRowCells.get(3)
        .shouldHave(text(vulnerabilityDetailsPopover.getVulnerabilityOverrideForm().status().getElement().getText()));
    vulnerabilityOverrideForm.comment().shouldNot(visible);
    vulnerabilityOverrideForm.status().click();
    vulnerabilityOverrideForm.status().listItem(2).click();
    vulnerabilityOverrideForm.comment().setValue(overriddenVulnerabilityComment);
    vulnerabilityOverrideForm.submitButton().click();
    waitUntilSpinnersGone();
    vulnerabilityDetailsPopover.getCloseButton().click();

    vulnerabilityRowCells.get(4).click();
    waitUntilSpinnersGone();
    vulnerabilityRowCells.get(3)
        .shouldHave(text(vulnerabilityDetailsPopover.getVulnerabilityOverrideForm().status().getElement().getText()));
    vulnerabilityOverrideForm.comment().shouldHave(text(overriddenVulnerabilityComment));
    vulnerabilityDetailsPopover.getCloseButton().click();
  }
}
