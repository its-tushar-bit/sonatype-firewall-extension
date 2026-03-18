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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.sonatype.clm.testing.functional.elements.ListSimilarWaiversTable;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable.ListWaiversTableRow;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.componentdetails.EditLicensesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.FirewallPolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.LicenseDetectionsTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.ManageLabelsContentTab;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationDetailPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.RiskRemediationTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilitiesTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilityDetailsPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilityDetailsPopover.VulnerabilityOverrideForm;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover.ComponentWaiversPopoverTable;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover.WaiverRow;
import com.sonatype.clm.testing.functional.pages.CustomizeVulnerabilityDetailsPage;
import com.sonatype.clm.testing.functional.pages.DeleteWaiverModal;
import com.sonatype.clm.testing.functional.pages.FirewallComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallQuarantineTable;
import com.sonatype.clm.testing.functional.pages.RepositoryResultDetailPage;
import com.sonatype.clm.testing.functional.pages.RepositoryResultDetailPage.RepositoryResultTable;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationSimilarWaiversInfoTile;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.Label;
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
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
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
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallComponentDetailsPageTest
    extends AbstractFunctionalTest
{
  static final String dateTimeFormatMask = "yyyy-MM-dd HH:mm:ss";

  static final String dateFormatMask = "yyyy-MM-dd";

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

  private final String[] expectedLabelsTexts = {"Label 1", "Label 2", "Label 3", "Label 4"};

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

  private Policy unknownComponentPolicy;

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

  private void configureQuarantinedComponents(List<ComponentDetails> componentDetailsArrayList) {
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> dependenciesMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    ComponentDetailsList componentDetailsList = new ComponentDetailsList();
    ComponentDetails mainComponentDetail = componentDetailsArrayList.get(0);
    // the second component detail is used as a valid alternative in version explorer, so we use the third as
    // an additional quarantined component
    ComponentDetails secondaryComponentDetail = componentDetailsArrayList.get(2);

    addComponentDetailSecurityVulnerability(mainComponentDetail, (float) 9.1);
    addComponentDetailSecurityVulnerability(mainComponentDetail, (float) 4.3);
    mainComponentDetail.setCatalogDate(new Date().getTime());

    addComponentDetailSecurityVulnerability(secondaryComponentDetail, (float) 7.1);
    addComponentDetailSecurityVulnerability(secondaryComponentDetail, (float) 2.3);
    secondaryComponentDetail.setCatalogDate(new Date().getTime());

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
      // Used when componentDetails are requested
      testCLMServer.getHdsServer()
          .respondWith(secondaryComponentDetail)
          .atUri("/rest/ci/componentDetails?" + "componentIdentifier="
              + URLEncoder.encode(JsonUtils.format(secondaryComponentDetail.getComponentIdentifier()), "UTF-8")
              + "&hash="
              + secondaryComponentDetail.getHash());
      // Used when multi license details are requested
      testCLMServer.getHdsServer()
          .respondWith(secondaryComponentDetail)
          .atUri("/rest/ci/componentDetails?" + "componentIdentifier="
              + URLEncoder.encode(JsonUtils.format(secondaryComponentDetail.getComponentIdentifier()), "UTF-8"));
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

  private void createUnknownComponentPolicy() {
    unknownComponentPolicy = createPolicy(Organization.ROOT_ORGANIZATION_ID, 2, "Component-Unknown",
        MatchStateConditionType.ID, "is", MatchState.UNKNOWN.toString());
  }

  private ComponentIdentifier createComponentIdentifier(String version) {
    return ComponentIdentifier.createMavenCoordinates("com.lingocoder", "abi.cli", version, "", "jar");
  }

  private ComponentIdentifier createComponentIdentifier(String componentName, String version) {
    return ComponentIdentifier.createMavenCoordinates("com.lingocoder", componentName, version, "", "jar");
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

  private void createAllTypePolicies() {
    createSecurityPolicies();
    createLicensePolicies();
    createQualityPolicies();
    createOtherPolicies();
  }

  private ArrayList<RepositoryComponent> setupBaseTestDataFor2QuarantineComponents(String componentLicenseCondition) {
    ComponentDetails componentDetails1 =
        createComponentDetail("hash1", createComponentIdentifier("0.5.2"), componentLicenseCondition);
    ComponentDetails componentDetails2 =
        createComponentDetail("hash2", createComponentIdentifier("0.5.3"), componentLicenseCondition);
    ComponentDetails componentDetails3 =
        createComponentDetail("hash3", createComponentIdentifier("best.component.ever", "0.5.4"),
            componentLicenseCondition);
    componentDetailsArrayList.add(componentDetails1);
    componentDetailsArrayList.add(componentDetails2);
    componentDetailsArrayList.add(componentDetails3);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.DATE, -1);
    Date yesterday = calendar.getTime();

    RepositoryComponent mainRepositoryComponent =
        createRepositoryComponent(componentDetails1.getHash(), componentDetails1.getComponentIdentifier(), date, date);
    createRepositoryComponent(componentDetails2.getHash(), componentDetails2.getComponentIdentifier(), date, null);
    RepositoryComponent secondaryRepositoryComponent =
        createRepositoryComponent(componentDetails3.getHash(), componentDetails3.getComponentIdentifier(), date,
            yesterday);

    ArrayList<RepositoryComponent> repositoryComponentList = new ArrayList<>();
    repositoryComponentList.add(mainRepositoryComponent);
    repositoryComponentList.add(secondaryRepositoryComponent);
    return repositoryComponentList;
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

  private ArrayList<RepositoryComponent> setupAllTestDataFor2QuarantinedComponents() {
    ArrayList<RepositoryComponent> repositoryComponentList = setupBaseTestDataFor2QuarantineComponents(singleLicense);

    configureQuarantinedComponents(componentDetailsArrayList);
    policyViolationsTableSetup(repositoryComponentList.get(0));
    policyViolationsTableSetup(repositoryComponentList.get(1));

    return repositoryComponentList;
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

  private RepositoryComponent setupUnknownComponentTestData() {
    RepositoryComponent unknownRepositoryComponent = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.UNKNOWN, "unknownComponent", null /* componentIdentifier */, true);

    ConstraintFact unknownComponentConstraintFact =
        createConstraintFact("constraint10", "Unknown 3rd party component", "summary", "Match State is unknown");

    createRepositoryPolicyViolation(repository.getId(), 2, unknownRepositoryComponent.getPathname(),
        unknownRepositoryComponent.getHash(), Collections.singletonList(unknownComponentConstraintFact),
        false /* isWaived */, "fail", unknownComponentPolicy.getId(), unknownComponentPolicy.getName(),
        unknownRepositoryComponent.getComponentIdentifier(), date, null/* PolicyWaiverId */,
        null/* PolicyWaiverComment */, null/* PolicyWaiverCreateTime */, PolicyThreatCategory.OTHER);

    return unknownRepositoryComponent;
  }

  private RepositoryComponent setupUnknownComponentTestDataWithWaivedViolation() {
    RepositoryComponent unknownRepositoryComponent = tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.UNKNOWN, "unknownComponent", null /* componentIdentifier */, true);
    unknownRepositoryComponent.setDisplayName("unknownComponent");

    ConstraintFact unknownComponentConstraintFact =
        createConstraintFact("constraint10", "Unknown 3rd party component", "summary", "Match State is unknown");

    PolicyWaiver policyWaiver = tempEntity.newWaiver(unknownRepositoryComponent.getHash(),
        unknownComponentPolicy.getId(), Organization.ROOT_ORGANIZATION_ID,
        Collections.singletonList(unknownComponentConstraintFact), "Test comment for waiver");

    createRepositoryPolicyViolation(repository.getId(), 2, unknownRepositoryComponent.getPathname(),
        unknownRepositoryComponent.getHash(), Collections.singletonList(unknownComponentConstraintFact),
        false /* isWaived */, "fail", unknownComponentPolicy.getId(), unknownComponentPolicy.getName(),
        unknownRepositoryComponent.getComponentIdentifier(), date, policyWaiver.getId(), policyWaiver.getComment(),
        policyWaiver.getCreateTime(), PolicyThreatCategory.OTHER);

    return unknownRepositoryComponent;
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

  private RepositoryComponent openUnknownComponentDetailsPageFromFirewallDashboard() {
    createUnknownComponentPolicy();
    RepositoryComponent component = setupUnknownComponentTestData();
    FirewallPage firewallPage = new FirewallPage();
    refreshOrOpen(FirewallPage.url());
    waitUntilSpinnersGone();
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(0).get(0).shouldBe(text("2"));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(0).get(1).shouldBe(text("Component-Unknown"));
    firewallPage.firewallQuarantineTable()
        .tableBodyCellsFromRow(0)
        .get(3)
        .shouldBe(text("unknownComponent (unknownComponent)"));
    firewallPage.firewallQuarantineTable().tableBodyCellsFromRow(0).get(4).shouldBe(text("repositoryPublicId"));

    firewallPage.firewallQuarantineTable().getComponentDetailsPageLinkFromRow(0).click();
    waitUntilSpinnersGone();

    return component;
  }

  private RepositoryComponent openUnknownComponentDetailsPageFromRepositoryResults() {
    createUnknownComponentPolicy();
    RepositoryComponent component = setupUnknownComponentTestData();
    refreshOrOpen(RepositoryResultDetailPage.url(component.getRepositoryId()));
    RepositoryResultDetailPage.aggregateToggle().click();
    waitUntilSpinnersGone();
    String quarantinedDate = getDateString(component.getQuarantineTime(), "yyyy-MM-dd");
    RepositoryResultDetailPage.table().row(0).threat().shouldBe(text("2"));
    RepositoryResultDetailPage.table().row(0).policy().shouldBe(text("Component-Unknown"));
    RepositoryResultDetailPage.table().row(0).quarantined().shouldBe(text(quarantinedDate));
    RepositoryResultDetailPage.table().row(0).component().shouldBe(text("unknownComponent (unknownComponent)"));

    RepositoryResultDetailPage.table().row(0).component().click();
    waitUntilSpinnersGone();

    return component;
  }

  @Test
  public void testTitleForUnknownComponentFromFirewallDashboard() {
    openUnknownComponentDetailsPageFromFirewallDashboard();
    firewallComponentDetailsPage.title().should(exist).shouldHave(text("unknownComponent (unknownComponent)"));
  }

  @Test
  public void testTitleForUnknownComponentFromRepositoryResults() {
    openUnknownComponentDetailsPageFromRepositoryResults();
    firewallComponentDetailsPage.title().should(exist).shouldHave(text("unknownComponent (unknownComponent)"));
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
  public void testTabsFromFirewallDashboard() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    testTabs(FirewallComponentDetailsPage.defaultUrl(component), component);
  }

  @Test
  public void testTabsFromFirewallDashboardWithUnknownComponentFromFirewallDashboard() {
    RepositoryComponent component = openUnknownComponentDetailsPageFromFirewallDashboard();
    String url = getWebDriver().getCurrentUrl();
    testTabs(url, component);
  }

  @Test
  public void testTabsFromFirewallDashboardWithUnknownComponentFromRepositoryResults() {
    RepositoryComponent component = openUnknownComponentDetailsPageFromRepositoryResults();
    String url = getWebDriver().getCurrentUrl();
    testTabs(url, component);
  }

  public void testTabs(String url, RepositoryComponent component) {
    refreshOrOpen(url);
    waitUntilSpinnersGone();
    ElementsCollection tabs = firewallComponentDetailsPage.tabs();
    tabs.first().shouldHave(cssClass("active"));

    assertThat(getWebDriver().getCurrentUrl()).contains("/" + component.getMatchStateId() + "?");

    tabs.get(1).click();
    tabs.get(1).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/violations?");

    if (component.getMatchStateId() != MatchState.UNKNOWN.toString()) {
      tabs.get(2).click();
      tabs.get(2).shouldHave(cssClass("active"));
      assertThat(getWebDriver().getCurrentUrl()).contains("/security?");

      tabs.get(3).click();
      tabs.get(3).shouldHave(cssClass("active"));
      assertThat(getWebDriver().getCurrentUrl()).contains("/legal?");

      tabs.get(4).click();
      tabs.get(4).shouldHave(cssClass("active"));
      assertThat(getWebDriver().getCurrentUrl()).contains("/labels?");

      tabs.shouldHave(size(5));
    }
    else {
      tabs.shouldHave(size(2));
    }

    tabs.get(0).click();
    tabs.get(0).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/overview?");
  }

  @Test
  public void testRiskRemediationTile_FromFirewallDashboard() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    String url = FirewallComponentDetailsPage.defaultUrl(component);
    testRiskRemediationTile_VersionGraphExplorer(url);
    testRiskRemediationTile_RecommendedVersions_NoRecommendation(url);
    testCompareVersionsTable(url);
  }

  public void testRiskRemediationTile_VersionGraphExplorer(String url) {
    refreshOrOpen(url);
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = firewallComponentDetailsPage.getRiskRemediationTile();
    riskRemediation.shouldBe(visible);
    riskRemediation.getTitle().shouldHave(text("Version Explorer"));

    RiskRemediationTile.VersionExplorerSection versionExplorerSection = riskRemediation.versionExplorerSection();
    versionExplorerSection.shouldBe(visible);
    ScrollUtil.scrollIntoView(versionExplorerSection.content());
    versionExplorerSection.content().shouldBe(visible);
  }

  public void testRiskRemediationTile_RecommendedVersions_NoRecommendation(String url) {
    refreshOrOpen(url);
    waitUntilSpinnersGone();

    RiskRemediationTile riskRemediation = firewallComponentDetailsPage.getRiskRemediationTile();
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
    recommendation.text().shouldHave(text("There are no suggested versions for this component"));
  }

  public void testCompareVersionsTable(String url) {
    refreshOrOpen(url);
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
    if (recommendationIndex == 0) {
      recommendation.text().shouldHave(text("Upgrade to 0.5.3"));
    }
    else {
      recommendation.text().shouldHave(text("Version 0.5.3"));
    }

    SelenideElement compareButton = recommendedVersionsSection.getRecommendation(recommendationIndex).actions().get(1);

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
  public void testRiskRemediationTile_CompareButtons_FromFirewallDashboard() {
    createSecurityPolicies();
    RepositoryComponent component = setupAllTestData();
    testRiskRemediationTile_CompareButtons(FirewallComponentDetailsPage.defaultUrl(component));
  }

  public void testRiskRemediationTile_CompareButtons(String url) {
    refreshOrOpen(url);
    waitUntilSpinnersGone();

    // There used to be 4 recommendations, but they were of the same version.
    // After the SDEV-1534 de-duplication, there is only one left.
    testCompareButtons(0);
  }

  @Test
  public void testComponentOverviewTileFromFirewallDashboard() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    testComponentOverviewTile(FirewallComponentDetailsPage.defaultUrl(component));
  }

  @Test
  public void testUnknownComponentOverviewTileFromFirewallDashboard() {
    openUnknownComponentDetailsPageFromFirewallDashboard();
    testUnknownComponentOverviewTile();
  }

  @Test
  public void testUnknownComponentOverviewTileFromRepositoryResults() {
    openUnknownComponentDetailsPageFromRepositoryResults();
    testUnknownComponentOverviewTile();
  }

  private void testUnknownComponentOverviewTile() {
    firewallComponentDetailsPage.getComponentOverviewTile().shouldBe(visible);
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(0).shouldHave(text("Unknown"));
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(1).shouldBe(empty);
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(2).shouldBe(empty);
    firewallComponentDetailsPage.getComponentOverviewTileReadOnlyItemData(3).shouldBe(empty);
    firewallComponentDetailsPage.getViewCoordinatesButton().shouldNotBe(visible);
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
  public void testComponentPolicyViolationsClickTab() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();
    ElementsCollection tabs = firewallComponentDetailsPage.tabs();
    tabs.shouldHave(size(5));
    tabs.get(1).click();
    tabs.get(1).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/violations?");

    firewallComponentDetailsPage.getPolicyViolationsComponent().shouldBe(visible);
  }

  @Test
  public void testComponentPolicyViolationsTileFromFirewallDashboard() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    testComponentPolicyViolationsTile(FirewallComponentDetailsPage.urlViolationsTab(component));
  }

  @Test
  public void testUnknownComponentPolicyViolationsTileFromFirewallDashboard() {
    openUnknownComponentDetailsPageFromFirewallDashboard();
    testUnknownComponentPolicyViolationsTile();
  }

  @Test
  public void testUnknownComponentPolicyViolationsTileFromRepositoryResults() {
    openUnknownComponentDetailsPageFromRepositoryResults();
    testUnknownComponentPolicyViolationsTile();
  }

  private void testUnknownComponentPolicyViolationsTile() {
    ElementsCollection tabs = firewallComponentDetailsPage.tabs();
    tabs.get(1).click();
    firewallComponentDetailsPage.getPolicyViolationsComponent().shouldBe(visible);
    firewallComponentDetailsPage.getComponentPolicyViolationsTitle().shouldBe(visible);
    firewallComponentDetailsPage.getComponentPolicyViolationsTable().shouldBe(visible);
    firewallComponentDetailsPage.getComponentPolicyViolationsTableCols().first().findAll(By.tagName("td"));
    testComponentPolicyViolationsRowHeaders();

    FirewallPolicyViolationsTable policyViolationsTable =
        FirewallComponentDetailsPage.getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(1));

    ElementsCollection violationCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    violationCells.shouldHave(size(6));
    violationCells.get(0).shouldHave(text("2"));
    violationCells.get(1).shouldHave(text("Component-Unknown"));
    violationCells.get(2).shouldHave(text("Unknown 3rd party component"));
    violationCells.get(3).shouldHave(text("Match State is unknown"));
    violationCells.get(4).shouldBe(empty);
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
    violationRow1Cells.get(2).shouldHave(text(policyViolationDetailPopover.policyViolationText().getText()));
    violationRow1Cells.get(3).shouldHave(text(policyViolationDetailPopover.popoverList().getText()));
    policyViolationDetailPopover.getCloseButton().click();
  }

  @Test
  public void testPolicyViolationTabViolationRowsClickForUnknownComponentFromFirewallDashboard() {
    openUnknownComponentDetailsPageFromFirewallDashboard();
    testPolicyViolationTabViolationRowsClickForUnknownComponent();
  }

  @Test
  public void testPolicyViolationTabViolationRowsClickForUnknownComponentFromRepositoryResults() {
    openUnknownComponentDetailsPageFromRepositoryResults();
    testPolicyViolationTabViolationRowsClickForUnknownComponent();
  }

  private void testPolicyViolationTabViolationRowsClickForUnknownComponent() {
    ElementsCollection tabs = firewallComponentDetailsPage.tabs();
    tabs.get(1).click();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);
    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();
    violationRow1Cells.get(3).click();
    violationRow1Cells.get(0).shouldHave(text(policyViolationDetailPopover.popoverThreatLevel().getText()));
    violationRow1Cells.get(2).shouldHave(text(policyViolationDetailPopover.policyViolationText().getText()));
    violationRow1Cells.get(3).shouldHave(text(policyViolationDetailPopover.popoverList().getText()));
    policyViolationDetailPopover.getCloseButton().click();
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

  @Test
  public void testViolationTabWaiverTableFromFirewallDashboard() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    testViolationTabWaiverTable(FirewallComponentDetailsPage.urlViolationsTab(component));
  }

  @Test
  public void testViolationTabWaiverTableFromFirewallDashboardForUnknownComponentFromFirewallDashboard() {
    createUnknownComponentPolicy();
    setupUnknownComponentTestDataWithWaivedViolation();
    FirewallPage firewallPage = new FirewallPage();
    refreshOrOpen(FirewallPage.url());
    waitUntilSpinnersGone();
    firewallPage.firewallQuarantineTable().getComponentDetailsPageLinkFromRow(0).click();
    waitUntilSpinnersGone();

    testViolationTabWaiverTableFromFirewallDashboardForUnknownComponent();
  }

  @Test
  public void testViolationTabWaiverTableFromFirewallDashboardForUnknownComponentFromRepositoryResults() {
    createUnknownComponentPolicy();
    RepositoryComponent component = setupUnknownComponentTestDataWithWaivedViolation();
    refreshOrOpen(RepositoryResultDetailPage.url(component.getRepositoryId()));
    RepositoryResultDetailPage.aggregateToggle().click();
    waitUntilSpinnersGone();
    RepositoryResultDetailPage.table().row(0).component().click();
    waitUntilSpinnersGone();

    testViolationTabWaiverTableFromFirewallDashboardForUnknownComponent();
  }

  private void testViolationTabWaiverTableFromFirewallDashboardForUnknownComponent() {
    ElementsCollection tabs = firewallComponentDetailsPage.tabs();
    tabs.get(1).click();
    Date waiverCreateDate = date;
    String waiverCreateDateString = getDateString(waiverCreateDate, dateFormatMask);

    ComponentWaiversPopover componentWaiversPopover = new ComponentWaiversPopover();
    ComponentWaiversPopoverTable componentWaiversTable = componentWaiversPopover.componentWaiversPopoverTable();

    firewallComponentDetailsPage.getPolicyViolationsComponent().shouldBe(visible);
    firewallComponentDetailsPage.firewallWaiversButton().click();

    componentWaiversPopover.shouldBe(visible);
    componentWaiversPopover.title().shouldHave(text("Component Waivers"));
    componentWaiversTable.shouldBe(visible);
    componentWaiversTable.getRows().shouldHave(size(1));

    ElementsCollection waiversTableCells = componentWaiversTable.getCellsByNthRow(1);

    waiversTableCells.shouldHave(size(3));
    waiversTableCells.get(0)
        .shouldHave(text("Created\n" +
            waiverCreateDateString + "\n" +
            "Expiration\n" +
            "Does not expire"));
    waiversTableCells.get(1)
        .shouldHave(Condition.text("Scope\n" +
            "Organization - Root Organization\n" +
            "Component\n" +
            "unknownComponent (unknownComponent)\n" +
            "Reason\n" +
            "—\n" +
            "Comment\n" +
            "Test comment for waiver\n" +
            "Author\n" +
            "Test User"));
  }

  public void testViolationTabWaiverTable(String url) {
    refreshOrOpen(url);
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
    componentWaiversTable.getRows().shouldHave(size(1));

    ElementsCollection waiversTableCells = componentWaiversTable.getCellsByNthRow(1);

    waiversTableCells.shouldHave(size(3));
    waiversTableCells.get(0)
        .shouldHave(text("Created\n" +
            waiverCreateDateString + "\n" +
            "Expiration\n" +
            "Does not expire"));
    waiversTableCells.get(1)
        .shouldHave(text("Scope\n" +
            "Organization - Root Organization\n" +
            "Component\n" +
            "com.lingocoder : abi.cli : 0.5.2\n" +
            "Reason\n" +
            "—\n" +
            "Comment\n" +
            "Test comment for waiver\n" +
            "Author\n" +
            "Test User"));
  }

  private void testLegalTabPolicyViolationsTable() {
    PolicyViolationsTable policyViolationsTable =
        PolicyViolationsTable.getPolicyViolationsTableForParent(FirewallComponentDetailsPage.ROOT);
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(1));

    ElementsCollection policyViolationsRow1 = policyViolationsTable.getCellsByNthRow(1);

    policyViolationsRow1.shouldHave(size(6));
    policyViolationsRow1.get(0).shouldHave(text("5"));
    policyViolationsRow1.get(1).shouldHave(text("LicensePolicy Proxy Warning"));
    policyViolationsRow1.get(2).shouldHave(text("LicensePolicy constraint"));
    policyViolationsRow1.get(3).shouldHave(text("Found license threat group"));
  }

  @Test
  public void testLegalTab_singleLicenseComponent_FromFirewallDashboard() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    testLegalTab_singleLicenseComponent(FirewallComponentDetailsPage.urlLegalTab(component));
  }

  public void testLegalTab_singleLicenseComponent(String url) {
    refreshOrOpen(url);
    waitUntilSpinnersGone();
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHave(size(1));
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
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHave(size(2));
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));
    effectiveLicenses.get(1).shouldHave(text("EPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHave(size(1));
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
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHave(size(1));
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
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHave(size(1));
    observedLicenses.first().shouldHave(text("Apache-2.0"));

    licenseDetectionsTile.status().shouldHave(text("Overridden"));

    testLegalTabPolicyViolationsTable();
  }

  @Test
  public void testLegalTab_LicenseDetectionTileAlpObservedLicensesDisabled() {
    configurationService.setALPObservedLicenseDetectionEnabled(false);
    try {
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
      ComponentDetails componentDetails = createComponentDetail("hash1", componentIdentifier, singleLicense);
      componentDetailsArrayList.add(componentDetails);

      RepositoryComponent component =
          createRepositoryComponent(componentDetails.getHash(), componentDetails.getComponentIdentifier(), date, date);

      refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
      waitUntilSpinnersGone();

      ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
      LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();

      licenseDetectionsTile.shouldBe(visible)
          .observedLicenses()
          .shouldHave(text("Enable the Observed License Detection feature in the Advanced Legal Pack (ALP) add-on."));

      licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses()).isEmpty();
    }
    finally {
      configurationService.setALPObservedLicenseDetectionEnabled(true);
    }
  }

  @Test
  public void testLegalTab_LicenseDetectionTileAlpObservedLicensesWhenAlpDisabled() {
    setMissingFeature(LicensedFeature.ADVANCED_LEGAL_PACK);
    // we need to refresh the browser to load the product features again
    WebDriverRunner.getWebDriver().navigate().refresh();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    ComponentDetails componentDetails = createComponentDetail("hash1", componentIdentifier, singleLicense);
    componentDetailsArrayList.add(componentDetails);

    RepositoryComponent component =
        createRepositoryComponent(componentDetails.getHash(), componentDetails.getComponentIdentifier(), date, date);

    refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
    waitUntilSpinnersGone();

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();

    licenseDetectionsTile
        .shouldBe(visible)
        .observedLicenses()
        .shouldHave(text("Get Advanced Legal Pack (ALP) to view Observed Licenses."));

    licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses()).isEmpty();
  }

  @Test
  public void testLegalTab_LicensesPopoverAlpObservedLicensesDisabled() {
    configurationService.setALPObservedLicenseDetectionEnabled(false);
    try {
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
      ComponentDetails componentDetails = createComponentDetail("hash1", componentIdentifier, singleLicense);
      componentDetailsArrayList.add(componentDetails);

      RepositoryComponent component =
          createRepositoryComponent(componentDetails.getHash(), componentDetails.getComponentIdentifier(), date, date);

      refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
      waitUntilSpinnersGone();

      ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
      LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
      licenseDetectionsTile.editLicenseButton().click();

      EditLicensesPopover editPopover = new EditLicensesPopover();
      editPopover.observedLicenses()
          .shouldHave(text("Enable the Observed License Detection feature in the Advanced Legal Pack (ALP) add-on."));
    }
    finally {
      configurationService.setALPObservedLicenseDetectionEnabled(true);
    }
  }

  @Test
  public void testLegalTab_LicensesPopoverAlpObservedLicensesWhenAlpDisabled() {
    setMissingFeature(LicensedFeature.ADVANCED_LEGAL_PACK);
    // we need to refresh the browser to load the product features again
    WebDriverRunner.getWebDriver().navigate().refresh();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    ComponentDetails componentDetails = createComponentDetail("hash1", componentIdentifier, singleLicense);
    componentDetailsArrayList.add(componentDetails);

    RepositoryComponent component =
        createRepositoryComponent(componentDetails.getHash(), componentDetails.getComponentIdentifier(), date, date);

    refreshOrOpen(FirewallComponentDetailsPage.urlLegalTab(component));
    waitUntilSpinnersGone();

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.editLicenseButton().click();

    EditLicensesPopover editPopover = new EditLicensesPopover();
    editPopover.observedLicenses().shouldHave(text("Get Advanced Legal Pack (ALP) to view Observed Licenses."));
  }

  private void setOverriddenLicensesStatus(EditLicensesPopover editPopover, int scope, String comment) {
    waitUntilSpinnersGone();
    editPopover.licensesScopesDropdown().click();
    editPopover.scope(scope).click();
    editPopover.status().click();
    editPopover.statuses().get(EditLicensesPopover.LicensesStatuses.OVERRIDDEN.ordinal()).click();
    waitUntilSpinnersGone();
    editPopover.availableLicensesTransferListItems().shouldBe(sizeGreaterThan(1));
    editPopover.selectedLicensesTransferListItems().shouldHave(size(0));
    editPopover.availableLicensesTransferListItems().get(0).click();
    editPopover.availableLicensesTransferListItems().get(0).click();
    editPopover.selectedLicensesTransferListItems().shouldHave(size(2));

    editPopover.comment().setValue(comment);
    editPopover.saveButton().click();
    waitUntilSpinnersGone();
    NxSubmitMask.seeAndWaitForDismissal();
  }

  @Test
  public void testLegalTab_overridenLicensesStatusFromFirewallDashboard() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData(overriddenLicense);
    testLegalTab_overridenLicensesStatus(FirewallComponentDetailsPage.urlLegalTab(component));
  }

  public void testLegalTab_overridenLicensesStatus(String url) {
    refreshOrOpen(url);

    waitUntilSpinnersGone();
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHave(size(1));
    observedLicenses.first().shouldHave(text("Apache-2.0"));

    licenseDetectionsTile.status().shouldHave(text("Overridden"));

    licenseDetectionsTile.editLicenseButton().click();

    EditLicensesPopover editPopover = new EditLicensesPopover();
    String testComment = "test comment";
    setOverriddenLicensesStatus(editPopover, EditLicensesPopover.RepositoryComponentLicensesScopes.REPOSITORY.ordinal(),
        testComment);
    editPopover.getCloseButton().click();

    licenseDetectionsTile.editLicenseButton().click();
    editPopover.selectedLicensesTransferListItems().shouldHave(size(2));
    editPopover.comment().shouldHave(text(testComment));

    effectiveLicenses.shouldHave(size(2));
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
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHave(size(1));
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

    waitUntilSpinnersGone();
    editPopover.licensesScopesDropdown().shouldHave(value("ROOT_ORGANIZATION_ID"));

    Pattern scopePattern = Pattern.compile("\\((.*)\\)");
    Matcher scopeMatcher = scopePattern.matcher(editPopover.availableScopes().last().getText());
    if (scopeMatcher.find()) {
      String scope = scopeMatcher.group(1);
      licenseDetectionsTile.status().shouldHave(text(scope));
    }

    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));
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
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHave(size(1));
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
    effectiveLicenses.shouldHave(size(1));
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
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("GPL-1.0"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHave(size(1));
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

    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));
  }

  @Test
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
    policyViolationsTable.getRows().shouldHave(size(6));

    // Mock HDS response for firewall component policy evaluation
    ComponentDetails componentDetails = componentDetailsArrayList.get(0);
    ComponentEvaluationDataList hdsResponse = new ComponentEvaluationDataList();
    hdsResponse.components.add(toComponentEvaluationData(componentDetails));
    testCLMServer.getHdsServer().respondWith(hdsResponse).atUri("/rest/component/details/firewall");

    policyDAO.delete(securityLowPolicy);

    Thread.sleep(100);

    firewallComponentDetailsPage.reevaluateButton().click();

    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    // One policy was deleted, so we expect one policy violation less than before
    PolicyViolationsTable policyViolationsTableReevaluation =
        PolicyViolationsTable.getPolicyViolationsTableForParent(FirewallComponentDetailsPage.ROOT);
    policyViolationsTableReevaluation.getRows().shouldHave(size(5));

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
    componentWaiversTable.getRows().shouldHave(size(1));

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
    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.artifactName().shouldHave(text("abi.cli"));
    addWaiverPage.componentName().shouldHave(text("com.lingocoder : abi.cli : 0.5.2"));
    addWaiverPage.policyName().shouldHave(text("Security-High"));
    addWaiverPage.constraintName().shouldHave(text("Security Constraint"));
    addWaiverPage.conditions().shouldHave(size(1));
    addWaiverPage.condition(1).shouldHave(text("security vulnerability severity >= 9.1"));
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.scope(0).shouldHave(text("Repository - repositoryPublicId"));
    addWaiverPage.scope(1).shouldHave(text("Repository Manager - " + repositoryManager.getName()));
    addWaiverPage.scope(2).shouldHave(text("Repository Managers"));
    addWaiverPage.scope(3).shouldHave(text("Organization - Root Organization"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    addWaiverPage.component(0).label().shouldHave(text("com.lingocoder : abi.cli : 0.5.2"));
    addWaiverPage.component(1).label().shouldHave(text("com.lingocoder : abi.cli"));
    addWaiverPage.component(2).label().shouldHave(text("All Components"));
    addWaiverPage.currentUserName().scrollIntoView(true).shouldHave(text("Admin BuiltIn"));
  }

  @Test
  public void testOpenAddWaiverPageForUnknownComponentFromFirewallDashboard() {
    openUnknownComponentDetailsPageFromFirewallDashboard();
    testOpenAddWaiverPageForUnknownComponent();
  }

  @Test
  public void testOpenAddWaiverPageForUnknownComponentFromRepositoryResults() {
    openUnknownComponentDetailsPageFromRepositoryResults();
    testOpenAddWaiverPageForUnknownComponent();
  }

  private void testOpenAddWaiverPageForUnknownComponent() {
    ElementsCollection tabs = firewallComponentDetailsPage.tabs();
    tabs.get(1).click();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();

    violationRow1Cells.get(3).click();

    policyViolationDetailPopover.shouldBe(visible);
    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.artifactName().shouldHave(text("unknownComponent (unknownComponent)"));
    addWaiverPage.componentName().shouldHave(text("unknownComponent (unknownComponent)"));
    addWaiverPage.policyName().shouldHave(text("Component-Unknown"));
    addWaiverPage.constraintName().shouldHave(text("Unknown 3rd party component"));
    addWaiverPage.conditions().shouldHave(size(1));
    addWaiverPage.condition(1).shouldHave(text("Match State is unknown"));
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.scope(0).shouldHave(text("Repository - repositoryPublicId"));
    addWaiverPage.scope(1).shouldHave(text("Repository Manager - " + repositoryManager.getName()));
    addWaiverPage.scope(2).shouldHave(text("Repository Managers"));
    addWaiverPage.scope(3).shouldHave(text("Organization - Root Organization"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    addWaiverPage.component(0).label().shouldHave(text("unknownComponent (unknownComponent)"));
    addWaiverPage.component(1).label().shouldHave(text("All Versions"));
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
    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.comments().shouldHave(exactText(""));
    addWaiverPage.expiryTimesOptions().shouldHave(size(8));
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
  public void testAddWaiverComponent_cancelButtonClick() {
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
    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.cancelButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
  }

  @Test
  public void testAddWaiverComponent__clickingDifferentScopes_and_submitButtonClick() {
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
    policyViolationDetailPopover.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTable =
        policyViolationDetailPopover.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTable.noWaiversMessage().shouldBe(visible);

    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(0, "Repository - repositoryPublicId"));

    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.label().shouldHave(text("All Components"));
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    violationRow1Cells.get(3).click();
    policyViolationDetailPopover = new PolicyViolationDetailPopover();
    policyViolationDetailPopover.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTableAfterSubmit =
        policyViolationDetailPopover.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTableAfterSubmit.rows().shouldHave(size(1));
    applicableWaiversTableAfterSubmit.noWaiversMessage().shouldNotBe(visible);

    ListWaiversTableRow waiversTableRow = applicableWaiversTableAfterSubmit.row(1);
    waiversTableRow.comments().shouldHave(text("Some comments"));
    waiversTableRow.components().shouldHave(text("All"));
    waiversTableRow.comments().shouldHave(text("Some comments"));
    waiversTableRow.createdBy().shouldHave(text("Admin BuiltIn"));
    waiversTableRow.waiverExpiration().shouldHave(text("Does not expire"));
    waiversTableRow.components().shouldHave(text("All"));
    waiversTableRow.scope().shouldHave(text("Repository - repository"));
    waiversTableRow.dateCreated().shouldHave(text(dateCreated));
  }

  @Test
  public void testAddWaiverComponent_findBackButtonAndClick() {
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
    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(0, "Repository - repositoryPublicId"));

    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.label().shouldHave(text("All Components"));
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    waitUntilSpinnersGone();

    firewallComponentDetailsPage.getPolicyViolationsComponent().shouldBe(visible);
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
    componentWaiversTable.getRows().shouldHave(size(1));

    firewallComponentDetailsPage.getDeleteWaiverButton().click();

    firewallComponentDetailsPage.getDeleteWaiverModal().shouldBe(visible);

    firewallComponentDetailsPage.getDeleteWaiverModalButton().click();

    ComponentWaiversPopover componentWaiversPopoverRefreshed = new ComponentWaiversPopover();
    ComponentWaiversPopoverTable componentWaiversTableRefreshed =
        componentWaiversPopoverRefreshed.componentWaiversPopoverTable();
    componentWaiversTableRefreshed.getRows().get(0).shouldHave(text("No existing component waivers"));
  }

  private ArrayList<Label> generateApplicableLabels() {
    ArrayList<Label> labelsList = new ArrayList<>();
    labelsList.add(tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, expectedLabelsTexts[0], Color.dark_blue));
    labelsList.add(tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, expectedLabelsTexts[1], Color.dark_red));
    labelsList.add(tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, expectedLabelsTexts[2], Color.dark_green));
    labelsList.add(tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, expectedLabelsTexts[3], Color.dark_green));
    return labelsList;
  }

  private void setLabelsAsApplied(RepositoryComponent component, ArrayList<Label> labelsList) {
    tempEntity.newComponentLabel(Organization.ROOT_ORGANIZATION_ID, labelsList.get(0).getId(), component.getHash());
    tempEntity.newComponentLabel(RepositoryContainer.REPOSITORY_CONTAINER_ID, labelsList.get(1).getId(),
        component.getHash());
    tempEntity.newComponentLabel(repositoryManager.getId(), labelsList.get(2).getId(), component.getHash());
    tempEntity.newComponentLabel(component.getRepositoryId(), labelsList.get(3).getId(), component.getHash());
  }

  private void addAppliedLabelsFromApplicableList(ManageLabelsContentTab manageLabels, int labelIndex, int scopeIndex) {
    manageLabels.applicableLabels().get(labelIndex).should(exist).click();
    manageLabels.addLabelModal().should(exist);
    manageLabels.addLabelModal().labelsScopesDropdown().listItemWithHidden(scopeIndex).should(exist).click();
    manageLabels.addLabelModal().submitButton().shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
  }

  @Test
  public void testLabelsTab_displayApplicableLabels() {
    RepositoryComponent component = setupAllTestData();
    generateApplicableLabels();
    refreshOrOpen(FirewallComponentDetailsPage.urlLabelsTab(component));
    ManageLabelsContentTab manageLabels = firewallComponentDetailsPage.labelsContent();
    manageLabels.shouldBe(visible);

    manageLabels.applicableLabels().shouldHave(size(4));
    manageLabels.applicableLabelText(0).shouldHave(text(expectedLabelsTexts[0]));
    assertThat(manageLabels.applicableLabels().get(0).getAttribute("className")).contains("nx-selectable-color--blue");
    manageLabels.applicableLabelText(1).shouldHave(text(expectedLabelsTexts[1]));
    assertThat(manageLabels.applicableLabels().get(1).getAttribute("className")).contains("nx-selectable-color--red");
    manageLabels.applicableLabelText(2).shouldHave(text(expectedLabelsTexts[2]));
    assertThat(manageLabels.applicableLabels().get(2).getAttribute("className")).contains("nx-selectable-color--green");
    manageLabels.applicableLabelText(3).shouldHave(text(expectedLabelsTexts[3]));
    assertThat(manageLabels.applicableLabels().get(3).getAttribute("className")).contains("nx-selectable-color--green");
    manageLabels.appliedLabels().shouldHave(size(0));
  }

  private void testLabelsTab_displayAppliedLabels() {
    ManageLabelsContentTab manageLabels = firewallComponentDetailsPage.labelsContent();
    manageLabels.shouldBe(visible);
    manageLabels.applicableLabels().shouldHave(size(0));
    manageLabels.appliedLabels().shouldHave(size(4));
    manageLabels.appliedLabelText(0).shouldHave(text(expectedLabelsTexts[3]));
    assertThat(manageLabels.appliedLabels().get(0).getAttribute("className")).contains("nx-selectable-color--green");
    manageLabels.appliedLabelText(1).shouldHave(text(expectedLabelsTexts[2]));
    assertThat(manageLabels.appliedLabels().get(1).getAttribute("className")).contains("nx-selectable-color--green");
    manageLabels.appliedLabelText(2).shouldHave(text(expectedLabelsTexts[1]));
    assertThat(manageLabels.appliedLabels().get(2).getAttribute("className")).contains("nx-selectable-color--red");
    manageLabels.appliedLabelText(3).shouldHave(text(expectedLabelsTexts[0]));
    assertThat(manageLabels.appliedLabels().get(3).getAttribute("className")).contains("nx-selectable-color--blue");
  }

  @Test
  public void testLabelsTab_displayAppliedLabels_fromFirewall() {
    RepositoryComponent component = setupAllTestData();
    ArrayList<Label> labelsList = generateApplicableLabels();
    setLabelsAsApplied(component, labelsList);
    refreshOrOpen(FirewallComponentDetailsPage.urlLabelsTab(component));
    testLabelsTab_displayAppliedLabels();
  }

  @Test
  public void testLabelsTab_displayAppliedLabels_fromRepositoryResultsView() {
    RepositoryComponent component = setupAllTestData();
    ArrayList<Label> labelsList = generateApplicableLabels();
    setLabelsAsApplied(component, labelsList);
    refreshOrOpen(FirewallComponentDetailsPage.urlLabelsTabFromRepositoryResultsView(component));
    testLabelsTab_displayAppliedLabels();
  }

  private void testLabelsTab_addLabels() {
    ManageLabelsContentTab manageLabels = firewallComponentDetailsPage.labelsContent();

    manageLabels.applicableLabelText(0).shouldHave(text(expectedLabelsTexts[0]));
    addAppliedLabelsFromApplicableList(manageLabels, 0,
        ManageLabelsContentTab.RepositoryComponentLabelsScopes.ROOT_ORGANIZATION.ordinal());
    manageLabels.appliedLabelText(0).shouldHave(text(expectedLabelsTexts[0]));

    manageLabels.applicableLabelText(0).shouldHave(text(expectedLabelsTexts[1]));
    addAppliedLabelsFromApplicableList(manageLabels, 0,
        ManageLabelsContentTab.RepositoryComponentLabelsScopes.ALL_REPOSITORIES.ordinal());
    manageLabels.appliedLabelText(0).shouldHave(text(expectedLabelsTexts[1]));

    manageLabels.applicableLabelText(0).shouldHave(text(expectedLabelsTexts[2]));
    addAppliedLabelsFromApplicableList(manageLabels, 0,
        ManageLabelsContentTab.RepositoryComponentLabelsScopes.REPOSITORY_MANAGER.ordinal());
    manageLabels.appliedLabelText(0).shouldHave(text(expectedLabelsTexts[2]));

    manageLabels.applicableLabelText(0).shouldHave(text(expectedLabelsTexts[3]));
    addAppliedLabelsFromApplicableList(manageLabels, 0,
        ManageLabelsContentTab.RepositoryComponentLabelsScopes.REPOSITORY.ordinal());
    manageLabels.appliedLabelText(0).shouldHave(text(expectedLabelsTexts[3]));

    manageLabels.applicableLabels().shouldHave(size(0));
    manageLabels.appliedLabels().shouldHave(size(4));

    manageLabels.appliedLabelText(0).shouldHave(text(expectedLabelsTexts[3]));
    manageLabels.appliedLabelText(1).shouldHave(text(expectedLabelsTexts[2]));
    manageLabels.appliedLabelText(2).shouldHave(text(expectedLabelsTexts[1]));
    manageLabels.appliedLabelText(3).shouldHave(text(expectedLabelsTexts[0]));
  }

  @Test
  public void testLabelsTab_addLabels_fromFirewall() {
    RepositoryComponent component = setupAllTestData();
    generateApplicableLabels();
    refreshOrOpen(FirewallComponentDetailsPage.urlLabelsTab(component));
    testLabelsTab_addLabels();
  }

  @Test
  public void testLabelsTab_addLabels_fromRepositoryResultsView() {
    RepositoryComponent component = setupAllTestData();
    generateApplicableLabels();
    refreshOrOpen(FirewallComponentDetailsPage.urlLabelsTabFromRepositoryResultsView(component));
    testLabelsTab_addLabels();
  }

  private void removeAppliedLabel(ManageLabelsContentTab manageLabels, int labelIndex) {
    manageLabels.appliedLabels().get(labelIndex).should(exist).click();
    manageLabels.removeLabelModal().should(exist);
    manageLabels.removeLabelModal().confirmRemoveButton().should(exist).click();
    NxSubmitMask.seeAndWaitForDismissal();
  }

  private void testLabelsTab_removeLabels() {
    ManageLabelsContentTab manageLabels = firewallComponentDetailsPage.labelsContent();

    manageLabels.appliedLabelText(0).shouldHave(text(expectedLabelsTexts[3]));
    manageLabels.appliedLabelText(1).shouldHave(text(expectedLabelsTexts[2]));
    manageLabels.appliedLabelText(2).shouldHave(text(expectedLabelsTexts[1]));
    manageLabels.appliedLabelText(3).shouldHave(text(expectedLabelsTexts[0]));

    removeAppliedLabel(manageLabels, 0);

    manageLabels.applicableLabelText(0).shouldHave(text(expectedLabelsTexts[3]));
    manageLabels.appliedLabelText(0).shouldHave(text(expectedLabelsTexts[2]));
    manageLabels.appliedLabelText(1).shouldHave(text(expectedLabelsTexts[1]));
    manageLabels.appliedLabelText(2).shouldHave(text(expectedLabelsTexts[0]));

    eyesWatcher.eyesCheck("Labels Tab");

    removeAppliedLabel(manageLabels, 0);

    manageLabels.applicableLabelText(0).shouldHave(text(expectedLabelsTexts[2]));
    manageLabels.applicableLabelText(1).shouldHave(text(expectedLabelsTexts[3]));
    manageLabels.appliedLabelText(0).shouldHave(text(expectedLabelsTexts[1]));
    manageLabels.appliedLabelText(1).shouldHave(text(expectedLabelsTexts[0]));

    removeAppliedLabel(manageLabels, 0);

    manageLabels.applicableLabelText(0).shouldHave(text(expectedLabelsTexts[1]));
    manageLabels.applicableLabelText(1).shouldHave(text(expectedLabelsTexts[2]));
    manageLabels.applicableLabelText(2).shouldHave(text(expectedLabelsTexts[3]));
    manageLabels.appliedLabelText(0).shouldHave(text(expectedLabelsTexts[0]));

    removeAppliedLabel(manageLabels, 0);

    manageLabels.applicableLabels().shouldHave(size(4));
    manageLabels.appliedLabels().shouldHave(size(0));
  }

  @Test
  public void testLabelsTab_removeLabels_fromFirewall() {
    RepositoryComponent component = setupAllTestData();
    ArrayList<Label> labelsList = generateApplicableLabels();
    setLabelsAsApplied(component, labelsList);
    refreshOrOpen(FirewallComponentDetailsPage.urlLabelsTab(component));
    testLabelsTab_removeLabels();
  }

  @Test
  public void testLabelsTab_removeLabels_fromRepositoryResultsView() {
    RepositoryComponent component = setupAllTestData();
    ArrayList<Label> labelsList = generateApplicableLabels();
    setLabelsAsApplied(component, labelsList);
    refreshOrOpen(FirewallComponentDetailsPage.urlLabelsTabFromRepositoryResultsView(component));
    testLabelsTab_removeLabels();
  }

  @Test
  public void testAvailable_ActiveWaiverInTableRow() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable =
        FirewallComponentDetailsPage.getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(6));

    ElementsCollection policyViolationCells = policyViolationsTable.getRows().get(1).findAll(By.tagName("td"));
    policyViolationCells.get(0).shouldHave(text("6"));
    policyViolationCells.get(1).shouldHave(text("Security-Low"));
    policyViolationCells.get(2).shouldHave(text("Security-low constraint"));
    policyViolationCells.get(3).shouldHave(text("security vulnerability severity >= 4.3"));
    policyViolationCells.get(4).shouldBe(text("1 Active Waiver"));
  }

  @Test
  public void testOpenPageViolationDetailsFromTableRow_NotActiveWaivers() {
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

    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.detailsTile().shouldBe(visible);
  }

  @Test
  public void testOpenPageViolationDetailsFromTableRow_ActiveWaiver() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(2);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();

    violationRow1Cells.get(3).click();

    policyViolationDetailPopover.shouldBe(visible);

    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.detailsTile().shouldBe(visible);
  }

  @Test
  public void testAvailable_UnappliedWaiverInTableRow() {
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

    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(0, "Repository - repositoryPublicId"));

    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.label().shouldHave(text("All Components"));
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(6));

    ElementsCollection securityViolationCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    securityViolationCells.shouldHave(size(6));
    securityViolationCells.get(0).shouldHave(text("10"));
    securityViolationCells.get(1).shouldHave(text("Security-High"));
    securityViolationCells.get(2).shouldHave(text("Security constraint"));
    securityViolationCells.get(3).shouldHave(text("security vulnerability severity >= 9.1"));
    securityViolationCells.get(4).shouldHave(text("Unapplied Waiver"));
  }

  @Test
  public void testTraverseListWaiverAddWaiversPagesUsingBackButton() {
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
    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.artifactName().shouldHave(text("abi.cli"));
    addWaiverPage.backButton().shouldBe(text("Back to Component Details"));
    addWaiverPage.backButton().click();

    firewallComponentDetailsPage.getPolicyViolationsComponent().shouldBe(visible);
  }

  @Test
  public void testAddWaiverAllRepositoriesIsShownInExistingWaiversPopover() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();
    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);
    violationRow1Cells.get(3).click();
    policyViolationDetailPopover.shouldBe(visible);

    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(2, "Repository Managers"));

    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.label().shouldHave(text("All Components"));
    chosenComponent.click();

    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    violationRow1Cells.get(3).click();
    policyViolationDetailPopover = new PolicyViolationDetailPopover();
    policyViolationDetailPopover.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTableAfterSubmit =
        policyViolationDetailPopover.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTableAfterSubmit.rows().shouldHave(size(1));
    applicableWaiversTableAfterSubmit.noWaiversMessage().shouldNotBe(visible);

    ListWaiversTableRow waiversTableRow = applicableWaiversTableAfterSubmit.row(1);
    waiversTableRow.scope().shouldHave(text("Repository Managers"));
    policyViolationDetailPopover.getCloseButton().click();
    policyViolationDetailPopover.getElement().shouldBe(disappear, Duration.ofMillis(500));

    SelenideElement viewAllComponentWaiversButton = firewallComponentDetailsPage.getViewAllComponentWaiversButton();
    viewAllComponentWaiversButton.shouldBe(visible).click();

    ComponentWaiversPopover componentWaiversPopover = new ComponentWaiversPopover();
    ComponentWaiversPopoverTable componentWaiversTable = componentWaiversPopover.componentWaiversPopoverTable();
    WaiverRow row = componentWaiversTable.row(1);

    row.scope().shouldHave(text("Repository Managers"));
  }

  @Test
  public void testSimilarWaivers() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();
    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(2);
    violationRow1Cells.get(3).click();
    policyViolationDetailPopover.shouldBe(visible);
    policyViolationDetailPopover.similarWaiversTab().shouldBe(visible, enabled).click();
    PolicyViolationSimilarWaiversInfoTile similarWaiversTile = policyViolationDetailPopover.similarWaiversInfoTile();
    similarWaiversTile.shouldBe(visible);
    ListSimilarWaiversTable similarWaiversTable = similarWaiversTile.getSimilarWaiversTable();
    similarWaiversTable.rows().shouldHave(size(1));
    similarWaiversTable.row(1).components().shouldHave(text("com.lingocoder : abi.cli (all versions)"));
    similarWaiversTable.row(1).scope().shouldHave(text(app.getName()));
    similarWaiversTable.row(1).comments().shouldHave(text("Test comment for waiver"));
    eyesWatcher.eyesCheck("Firewall Component Details Page - Similar Waivers Tab");
  }

  @Test
  public void testAddWaiversAndRemoveThemFromExistingWaiversPopovers() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();
    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);
    violationRow1Cells.get(3).click();
    policyViolationDetailPopover.shouldBe(visible);

    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.scope(1).click();
    addWaiverPage.component(1).click();

    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    violationRow1Cells.get(3).click();
    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    addWaiverPage.scope(2).click();
    addWaiverPage.component(1).click();

    addWaiverPage.comments().setValue("test comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));

    firewallComponentDetailsPage.firewallWaiversButton().click();

    ComponentWaiversPopover componentWaiversPopover = new ComponentWaiversPopover();
    ComponentWaiversPopoverTable componentWaiversTable = componentWaiversPopover.componentWaiversPopoverTable();
    WaiverRow row = componentWaiversTable.row(2);
    row.deleteButton().click();

    firewallComponentDetailsPage.getDeleteWaiverModal().shouldBe(visible);
    firewallComponentDetailsPage.getDeleteWaiverModalButton().click();

    componentWaiversTable.getRows().shouldHave(size(2));
  }

  @Test
  public void testDeletesWaiverAfterReevaluation() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();
    policyViolationsTable.getCellsByNthRow(1).get(3).click();
    policyViolationDetailPopover.shouldBe(visible);

    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.scope(1).click();
    addWaiverPage.component(1).click();

    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    firewallComponentDetailsPage.reevaluateButton().click();
    waitUntilSpinnersGone();

    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));

    policyViolationsTable.getCellsByNthRow(1).get(3).click();
    policyViolationDetailPopover.shouldBe(visible);
    policyViolationDetailPopover.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTableAfterSubmit =
        policyViolationDetailPopover.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTableAfterSubmit.rows().shouldHave(size(1));
    applicableWaiversTableAfterSubmit.noWaiversMessage().shouldNotBe(visible);

    ListWaiversTableRow waiversTableRow = applicableWaiversTableAfterSubmit.row(1);
    waiversTableRow.deleteButton().click();

    DeleteWaiverModal modal = new DeleteWaiverModal();
    modal.root().shouldBe(visible);
    modal.header().shouldHave(text("Delete Waiver"));
    modal.message().shouldHave(text("Are you sure you want to delete this waiver?"));
    modal.yesButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    policyViolationDetailPopover.applicableWaiversInfoTile()
        .getApplicableWaiversTable()
        .noWaiversMessage()
        .shouldBe(visible);
  }

  @Test
  public void testAddWaiversAndRemoveItFromExistingWaiversPopoversInSecurityTab() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();
    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);
    violationRow1Cells.get(3).click();
    policyViolationDetailPopover.shouldBe(visible);

    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.scope(1).click();
    addWaiverPage.component(1).click();

    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    violationRow1Cells.get(3).click();
    policyViolationDetailPopover.getAddWaiversButton().shouldBe(visible, enabled).click();

    addWaiverPage.scope(2).click();
    addWaiverPage.component(1).click();

    addWaiverPage.comments().setValue("test comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    refreshOrOpen(FirewallComponentDetailsPage.urlSecurityTab(component));

    firewallComponentDetailsPage.firewallWaiversButton().click();

    ComponentWaiversPopover componentWaiversPopover = new ComponentWaiversPopover();
    ComponentWaiversPopoverTable componentWaiversTable = componentWaiversPopover.componentWaiversPopoverTable();
    WaiverRow row = componentWaiversTable.row(2);
    row.deleteButton().click();

    firewallComponentDetailsPage.getDeleteWaiverModal().shouldBe(visible);
    firewallComponentDetailsPage.getDeleteWaiverModalButton().click();

    componentWaiversTable.getRows().shouldHave(size(2));
  }

  @Test
  public void backButtonToFirewallDashboard_whenUserCameFromFirewallDashboard() {
    FirewallPage firewallPage = new FirewallPage();
    FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();
    String expectedComponentName = "com.lingocoder : abi.cli : 0.5.2";
    createAllTypePolicies();
    setupAllTestDataFor2QuarantinedComponents();
    refreshOrOpen(FirewallPage.url());
    waitUntilSpinnersGone();
    FirewallQuarantineTable firewallQuarantineTable = firewallPage.firewallQuarantineTable();
    SelenideElement policyNameSelect = firewallQuarantineTable.policyNameSelect();
    policyNameSelect.click();
    SelenideElement policyNameCheckbox = firewallQuarantineTable.policyNameCheckboxes().get(0);
    policyNameCheckbox.click();
    policyNameSelect.shouldHave(text("CoordinatesPolicy"));
    // quarantine date order: null
    waitUntilSpinnersGone();
    // quarantine date order: asc
    firewallQuarantineTable.quarantineTimeHeader().click();
    waitUntilSpinnersGone();
    // quarantine date order: desc
    firewallQuarantineTable.quarantineTimeHeader().click();
    waitUntilSpinnersGone();
    SelenideElement componentLink = firewallQuarantineTable.getComponentDetailsPageLinkFromRow(0);
    componentLink.shouldHave(text(expectedComponentName));
    componentLink.click();
    waitUntilSpinnersGone();
    firewallComponentDetailsPage.shouldBe(visible);
    firewallComponentDetailsPage.title().shouldHave(text(expectedComponentName));
    MainHeader.backButton().shouldHave(text("Back to Firewall Dashboard"));
    MainHeader.backButton().click();
    firewallPage.shouldBe(visible);
    componentLink = firewallQuarantineTable.getComponentDetailsPageLinkFromRow(0);
    componentLink.shouldHave(text(expectedComponentName));
    policyNameSelect = firewallQuarantineTable.policyNameSelect();
    policyNameSelect.shouldHave(text("1 of 6"));
  }

  @Test
  public void backButtonToFirewallDashboard_whenUserAccessByURL() {
    FirewallPage firewallPage = new FirewallPage();
    FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();
    String expectedComponentName = "com.lingocoder : abi.cli : 0.5.2";
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();
    firewallComponentDetailsPage.shouldBe(visible);
    firewallComponentDetailsPage.title().shouldHave(text(expectedComponentName));
    MainHeader.backButton().shouldHave(text("Back to Firewall Dashboard"));
    MainHeader.backButton().click();
    firewallPage.shouldBe(visible);
    SelenideElement componentLink = firewallPage.firewallQuarantineTable().getComponentDetailsPageLinkFromRow(0);
    componentLink.shouldHave(text(expectedComponentName));
  }

  @Test
  public void backButtonToRepositoryResultsView_whenUserCameFromRepositoryResultsView() {
    FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();
    String expectedComponentName = "com.lingocoder : abi.cli : 0.5.2";
    createAllTypePolicies();
    ArrayList<RepositoryComponent> repositoryComponents = setupAllTestDataFor2QuarantinedComponents();
    refreshOrOpen(RepositoryResultDetailPage.url(repositoryComponents.get(0).getRepositoryId()));
    RepositoryResultDetailPage.aggregateToggle().click();
    waitUntilSpinnersGone();
    RepositoryResultDetailPage.page().shouldBe(visible);
    RepositoryResultTable repositoryResultsTable = new RepositoryResultTable();
    repositoryResultsTable.policyName().input().setValue("Security");
    SelenideElement quarantinedHeader = repositoryResultsTable.header().quarantined();
    // sorted by quarantined date by null order
    quarantinedHeader.click();
    waitUntilSpinnersGone();
    // sorted by quarantined date by asc order
    quarantinedHeader.click();
    waitUntilSpinnersGone();
    // sorted by quarantined date by desc order
    SelenideElement componentNameCell = RepositoryResultDetailPage.table().row(1).component();
    componentNameCell.shouldHave(text(expectedComponentName));
    componentNameCell.click();
    waitUntilSpinnersGone();
    firewallComponentDetailsPage.shouldBe(visible);
    firewallComponentDetailsPage.title().shouldHave(text(expectedComponentName));
    MainHeader.backButton().shouldHave(text("Back to Repository Results"));
    MainHeader.backButton().click();
    RepositoryResultDetailPage.page().shouldBe(visible);
    RepositoryResultDetailPage.table().row(1).component().shouldHave(text(expectedComponentName));
    repositoryResultsTable.policyName().input().shouldHave(attribute("value", "Security"));
  }

  @Test
  public void backButtonToRepositoryResultsView_whenUserCameFromMalwareDefenseRepositoryResultsView() {
    FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();
    String expectedComponentName = "com.lingocoder : abi.cli : 0.5.2";
    createAllTypePolicies();
    ArrayList<RepositoryComponent> repositoryComponents = setupAllTestDataFor2QuarantinedComponents();
    refreshOrOpen(RepositoryResultDetailPage.firewallUrl(repositoryComponents.get(0).getRepositoryId()));
    RepositoryResultDetailPage.aggregateToggle().click();
    waitUntilSpinnersGone();
    RepositoryResultDetailPage.page().shouldBe(visible);
    RepositoryResultTable repositoryResultsTable = new RepositoryResultTable();
    repositoryResultsTable.policyName().input().setValue("Security");
    SelenideElement quarantinedHeader = repositoryResultsTable.header().quarantined();
    // sorted by quarantined date by null order
    quarantinedHeader.click();
    waitUntilSpinnersGone();
    // sorted by quarantined date by asc order
    quarantinedHeader.click();
    waitUntilSpinnersGone();
    // sorted by quarantined date by desc order
    SelenideElement componentNameCell = RepositoryResultDetailPage.table().row(1).component();
    componentNameCell.shouldHave(text(expectedComponentName));
    componentNameCell.click();
    waitUntilSpinnersGone();
    firewallComponentDetailsPage.shouldBe(visible);
    firewallComponentDetailsPage.title().shouldHave(text(expectedComponentName));
    MainHeader.backButton().shouldHave(text("Back to Repository Results"));
    MainHeader.backButton().click();
    RepositoryResultDetailPage.page().shouldBe(visible);
    RepositoryResultDetailPage.table().row(1).component().shouldHave(text(expectedComponentName));
    repositoryResultsTable.policyName().input().shouldHave(attribute("value", "Security"));
  }

  @Test
  public void backButtonToRepositoryResultsView_whenUserAccessByUrl() {
    FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();
    String expectedComponentName = "com.lingocoder : abi.cli : 0.5.2";
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrlFromRepositoryResultsView(component));
    waitUntilSpinnersGone();
    firewallComponentDetailsPage.shouldBe(visible);
    firewallComponentDetailsPage.title().shouldHave(text(expectedComponentName));
    MainHeader.backButton().shouldHave(text("Back to Repository Results"));
    MainHeader.backButton().click();
    RepositoryResultDetailPage.page().shouldBe(visible);
    RepositoryResultDetailPage.table().row(0).component().shouldHave(text(expectedComponentName));
  }

  private void waiversPagesFromRepositoryComponentDetailsPage_commonBackButtonsAssertions(String stringToFindInUrl) {
    FirewallPolicyViolationsTable policyViolationsTable =
        FirewallComponentDetailsPage.getFirewallPolicyViolationsTable();
    FirewallComponentDetailsPage firewallComponentDetailsPage = new FirewallComponentDetailsPage();
    String componentName = "com.lingocoder : abi.cli : 0.5.2";
    String policyName = "Security-High";

    firewallComponentDetailsPage.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(6));
    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);
    violationRow1Cells.get(1).shouldHave(text(policyName));
    violationRow1Cells.get(3).click();
    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();
    policyViolationDetailPopover.shouldBe(visible);
    policyViolationDetailPopover.headerPopoverTitle().shouldHave(text(policyName));

    // waitUntilSpinnersGone();
    policyViolationDetailPopover.getAddWaiversButton().click();
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.shouldBe(visible);
    addWaiverPage.componentName().shouldBe(text(componentName));
    addWaiverPage.policyName().shouldBe(text(policyName));
    assertThat(getWebDriver().getCurrentUrl()).contains(stringToFindInUrl);
    assertThat(getWebDriver().getCurrentUrl()).contains("/addWaiver/");
    MainHeader.backButton().shouldHave(text("Back to Component Details"));
    MainHeader.backButton().click();

    waitUntilSpinnersGone();
    firewallComponentDetailsPage.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(6));
    violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);
    violationRow1Cells.get(1).shouldHave(text(policyName));
  }

  @Test
  public void testWaiversPagesBackButton_hasBaseRouteToMatchWithFirewallComponentDetailsPage() {
    // this test is used to evaluate if the add waivers page being loaded from firewall dashboard CDP is prefixing back
    // buttons routes as a firewall/ route
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    waiversPagesFromRepositoryComponentDetailsPage_commonBackButtonsAssertions(
        "#/firewall/repository/");
  }

  @Test
  public void testWaiversPagesBackButton_hasBaseRouteToMatchWithRepositoryResultsComponentDetailsPage() {
    // this test is used to evaluate if the add waivers page being loaded from repository results CDP is prefixing back
    // buttons routes as a repository/ route
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();
    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTabFromRepositoryResultsView(component));
    waitUntilSpinnersGone();

    waiversPagesFromRepositoryComponentDetailsPage_commonBackButtonsAssertions("#/repository/");
  }

  @Test
  public void testRepositoryResultsViewStateAfterReevaluatingComponent() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();

    refreshOrOpen(RepositoryResultDetailPage.url(component.getRepositoryId()));
    RepositoryResultDetailPage.aggregateToggle().click();
    waitUntilSpinnersGone();

    RepositoryResultTable repositoryResultsTable = new RepositoryResultTable();
    repositoryResultsTable.policyName().input().setValue("Security");
    repositoryResultsTable.header().quarantined().click();
    waitUntilSpinnersGone();

    RepositoryResultDetailPage.table().policyName().input().shouldHave(attribute("value", "Security"));
    RepositoryResultDetailPage.table()
        .quarantinedHeaderSortButton()
        .shouldHave(
            attribute("aria-label", "QUARANTINE TIME ascending"));

    RepositoryResultDetailPage.table().row(1).component().click();
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.reevaluateButton().click();
    waitUntilSpinnersGone();

    MainHeader.backButton().click();
    waitUntilSpinnersGone();

    RepositoryResultDetailPage.page().shouldBe(visible);
    RepositoryResultDetailPage.table().policyName().input().shouldHave(attribute("value", "Security"));
    RepositoryResultDetailPage.table()
        .quarantinedHeaderSortButton()
        .shouldHave(
            attribute("aria-label", "QUARANTINE TIME ascending"));

    String repositoryResultsViewCDPUrl = FirewallComponentDetailsPage.defaultUrlFromRepositoryResultsView(component);

    testTabs(repositoryResultsViewCDPUrl, component);

    // overview tab
    testComponentOverviewTile(repositoryResultsViewCDPUrl);
    testRiskRemediationTile_VersionGraphExplorer(repositoryResultsViewCDPUrl);
    testRiskRemediationTile_RecommendedVersions_NoRecommendation(repositoryResultsViewCDPUrl);
    testCompareVersionsTable(repositoryResultsViewCDPUrl);

    // violations tab
    String violationsTabUrl = FirewallComponentDetailsPage.urlViolationsTabFromRepositoryResultsView(component);
    testComponentPolicyViolationsTile(violationsTabUrl);
    testViolationTabWaiverTable(violationsTabUrl);

    // security tab
    String securityTabUrl = FirewallComponentDetailsPage.urlSecurityTabFromRepositoryResultsView(component);
    testSecurityTabSecurityViolationsTable(securityTabUrl);
    testSecurityTabVulnerabilitiesTable(securityTabUrl);
    testSecurityTabVulnerabilitiesTableRowClick(securityTabUrl);
    testSecurityTabVulnerabilityOverrideForm_vulnerabilityOverride(securityTabUrl);

    // legal tab
    testLegalTab_singleLicenseComponent(FirewallComponentDetailsPage.urlLegalTabFromRepositoryResultsView(component));
  }

  @Test
  public void testRiskRemediationTile_RecommendedVersionsAfterReevaluatingComponent() {
    createSecurityPolicies();
    RepositoryComponent component = setupAllTestData();

    refreshOrOpen(RepositoryResultDetailPage.url(component.getRepositoryId()));
    RepositoryResultDetailPage.aggregateToggle().click();
    waitUntilSpinnersGone();

    RepositoryResultDetailPage.table().row(1).component().click();
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.reevaluateButton().click();
    waitUntilSpinnersGone();

    MainHeader.backButton().click();
    waitUntilSpinnersGone();

    testRiskRemediationTile_CompareButtons(FirewallComponentDetailsPage.defaultUrlFromRepositoryResultsView(component));
  }

  @Test
  public void testLegalTab_overridenLicensesStatusAfterReevaluatingComponent() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData(overriddenLicense);

    refreshOrOpen(RepositoryResultDetailPage.url(component.getRepositoryId()));
    RepositoryResultDetailPage.aggregateToggle().click();
    waitUntilSpinnersGone();

    RepositoryResultDetailPage.table().row(1).component().click();
    waitUntilSpinnersGone();

    firewallComponentDetailsPage.reevaluateButton().click();
    waitUntilSpinnersGone();

    MainHeader.backButton().click();
    waitUntilSpinnersGone();

    testLegalTab_overridenLicensesStatus(FirewallComponentDetailsPage.urlLegalTabFromRepositoryResultsView(component));
  }

  @Test
  public void testComponentReEvaluationDoesNotCauseBlankPage() {
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();

    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    // Mock HDS response for firewall component policy evaluation
    ComponentDetails componentDetails = componentDetailsArrayList.get(0);
    ComponentEvaluationDataList hdsResponse = new ComponentEvaluationDataList();
    hdsResponse.components.add(toComponentEvaluationData(componentDetails));
    testCLMServer.getHdsServer().respondWith(hdsResponse).atUri("/rest/component/details/firewall");

    firewallComponentDetailsPage.reevaluateButton().click();

    refreshOrOpen(FirewallComponentDetailsPage.urlViolationsTab(component));
    waitUntilSpinnersGone();

    FirewallPolicyViolationsTable policyViolationsTable = FirewallComponentDetailsPage
        .getFirewallPolicyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    // Create popover object before clicking to allow proper Selenide retry mechanism
    PolicyViolationDetailPopover policyViolationDetailPopover = new PolicyViolationDetailPopover();

    ElementsCollection violationRow1Cells = policyViolationsTable.getCellsByNthRow(1);
    // After page refresh, ensure the cell is clickable before attempting click
    // This prevents race condition where React event handlers may not be fully attached yet
    violationRow1Cells.get(1).shouldBe(visible, enabled).click();

    // Wait for popover to become visible before interacting with it
    policyViolationDetailPopover.shouldBe(visible).applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTableAfterSubmit =
        policyViolationDetailPopover.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTableAfterSubmit.rows().shouldHave(size(1));
    applicableWaiversTableAfterSubmit.noWaiversMessage().shouldBe(visible);
  }

  @Test
  public void testSecurityTabLoadByTabClick_customizeButton() {
    String vulnerabilityId = "sonatype-2017-0507";
    createAllTypePolicies();
    RepositoryComponent component = setupAllTestData();

    mockHdsResponsesForVulnerabilityDetails();
    refreshOrOpen(FirewallComponentDetailsPage.defaultUrl(component));
    waitUntilSpinnersGone();

    ElementsCollection tabs = firewallComponentDetailsPage.tabs();
    tabs.get(0).shouldHave(cssClass("active"));

    tabs.get(2).click();
    waitUntilSpinnersGone();
    tabs.get(2).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/security?");

    firewallComponentDetailsPage.getSecurityTabContainer().shouldBe(visible);

    VulnerabilitiesTable vulnerabilitiesTable =
        VulnerabilitiesTable.getVulnerabilitiesTableForParent(FirewallComponentDetailsPage.ROOT);
    vulnerabilitiesTable.shouldBe(visible);

    vulnerabilitiesTable.getRow(1).click();
    ElementsCollection vulnerabilityRow1Cells = vulnerabilitiesTable.getCellsByNthRow(1);
    vulnerabilityRow1Cells.shouldHave(size(5));
    vulnerabilityRow1Cells.get(0).shouldHave(text("9"));
    vulnerabilityRow1Cells.get(1).shouldHave(text(vulnerabilityId));
    vulnerabilityRow1Cells.get(2).shouldBe(empty);
    vulnerabilityRow1Cells.get(3).shouldHave(text("Open"));
    vulnerabilityRow1Cells.get(4).shouldBe(empty);
    VulnerabilityDetailsPopover vulnerabilityDetailsPopover = new VulnerabilityDetailsPopover();
    vulnerabilityDetailsPopover.shouldBe(visible);

    SelenideElement customizeButton = vulnerabilityDetailsPopover.getCustomizeButton();
    customizeButton.shouldBe(visible);
    customizeButton.click();

    CustomizeVulnerabilityDetailsPage.refIdTitle().shouldBe(visible);
    CustomizeVulnerabilityDetailsPage.refIdTitle().shouldBe(text(vulnerabilityId));

    NxBackButton backButton = CustomizeVulnerabilityDetailsPage.backButton();
    backButton.shouldBe(visible);
    backButton.shouldHave(text("Back to Firewall Vulnerability Details"));
    backButton.click();

    Duration d = Duration.ofHours(2);
    Selenide.Wait().withTimeout(d);
  }
}
