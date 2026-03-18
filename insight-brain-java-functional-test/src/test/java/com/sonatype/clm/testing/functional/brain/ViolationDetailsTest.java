/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.AgeFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.ManageFiltersDropdown;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable.ListAutoWaiverTableRow;
import com.sonatype.clm.testing.functional.elements.NxPolicyThreatLevelFilter;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.NxTreeViewMultiSelect;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.DeleteWaiverModal;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.RequestWaiverPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationApplicableWaiversInfoTile;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationApplicableWaiversTab;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationConstraintInfo;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationSecurityDetailsInfoTile;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationSimilarWaiversInfoTile;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.SidebarNav;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.SidebarNavListItem;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.ViolationDetailsTile;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.textCaseSensitive;
import static com.codeborne.selenide.Condition.visible;

public class ViolationDetailsTest
    extends AbstractFunctionalTest
{
  private Application application;

  private PolicyViolation securityPolicyViolation;

  private PolicyViolation otherPolicyViolation;

  private PolicyViolation deletedPolicyViolation;

  private PolicyViolation nonSecurityPolicyViolation;

  private PolicyDAO policyDAO;

  private PolicyViolationDAO policyViolationDAO;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    policyDAO = lookup(PolicyDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);

    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
    Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);

    Organization organization = tempEntity.newOrganization("Org 1");
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    Policy securityPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.RELEASE.getId(), "scan2", false, false, Date.from(oneDayAgo));

    PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.OPERATE.getId(), "scan3", false, false, Date.from(oneDayAgo));

    securityPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation1, securityPolicy, "Group1",
        "Artifact1", "Version1", "hash", "sonatype-2017-0507");
    securityPolicyViolation.setActionTypeId(Action.ID_FAIL);
    policyViolationDAO.update(securityPolicyViolation);

    Policy otherPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 2", 3);
    otherPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation1, otherPolicy);

    // The same as securityPolicyViolation, but for a different stage
    tempEntity.newPolicyViolation(policyEvaluation2, securityPolicy, "Group1", "Artifact1", "Version1", "hash",
        "sonatype-2017-0507");

    // The same as securityPolicyViolation, but for a different stage
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation3, securityPolicy, "Group1",
        "Artifact1", "Version1", "hash", "sonatype-2017-0507");
    policyViolation3.setActionTypeId(Action.ID_WARN);
    policyViolationDAO.update(policyViolation3);

    Policy deletedPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Deleted Policy", 2);
    deletedPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation1, deletedPolicy);
    policyDAO.delete(deletedPolicy);

    Policy nonSecurityPolicy = createPolicy(Organization.ROOT_ORGANIZATION_ID, 1, "Policy 4",
        LicenseThreatGroupLevelConditionType.ID, "<=", "1");
    nonSecurityPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation2, nonSecurityPolicy);

    mockHdsResponseForVulnerabilityDetails();
    // This ensures that the redux state has the updated information at the start of the tests
    refresh();
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

  @Test
  public void testDetails() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    eyesWatcher.eyesCheck();

    tile.headerTitle().shouldHave(text("Violation of Policy 1"));
    ElementsCollection elements = tile.headerSubtitle().findAll(".iq-violation-details__subtitle-part");
    elements.shouldHave(size(3));
    elements.get(0).shouldHave(text("Org 1"));
    elements.get(1).shouldHave(text("App 1"));
    elements.get(2).shouldHave(text("Group1 : Artifact1 : Version1"));

    tile.firstReported().shouldHave(text("2 days ago"));
    tile.lastReported().shouldHave(text("1 day ago"));
    tile.policyType().shouldHave(text("Security"));
    tile.threatLevel().shouldHave(text("7"));
    tile.policyOwnerLink().shouldHave(text("Root Organization"));

    tile.stages().shouldHave(size(5));

    tile.stage(0).shouldHave(text("Source"));
    tile.stage(0).icon().should(exist);
    tile.stage(0).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(1).shouldHave(text("Build 2d"));
    tile.stage(1).icon().should(exist);
    tile.stage(1).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(2).shouldHave(text("Stage"));
    tile.stage(2).icon().should(exist);
    tile.stage(2).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(3).shouldHave(text("Release 1d"));
    tile.stage(3).icon().shouldNot(exist);
    tile.stage(3).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    // Uncomment after fixing CLM-18676
    // tile.stage(4).shouldHave(text("Operate 1d"));
    // tile.stage(4).icon().should(exist);
    // tile.stage(4).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());
  }

  @Test
  public void testDetails_PolicyNoLongerExists() {
    refreshOrOpen(ViolationDetailsPage.url(deletedPolicyViolation.getId()));
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();
    PolicyViolationApplicableWaiversTab applicableWaiversTab = new ViolationDetailsPage().applicableWaiversTab();
    tile.policyOwner().shouldHave(text("Policy no longer exists"));
    applicableWaiversTab.waiversIndicator().shouldBe(visible).shouldHave(text("Applicable Waivers"));
    tile.manageWaiversButton().shouldNotBe(visible);
  }

  @Test
  public void testStageLink() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    SidebarNavigation.closeNavigationSidebar();
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    tile.stage(1).link().shouldHave(text("Build")).click();
    waitUntilUrl(ApplicationReportPage.url(application, "scan1"));

    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    SidebarNavigation.closeNavigationSidebar();

    tile.stage(3).link().shouldHave(text("Release")).click();
    waitUntilUrl(ApplicationReportPage.url(application, "scan2"));

    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    SidebarNavigation.closeNavigationSidebar();
    tile.stage(2).link().shouldNot(exist);
    tile.stage(4).link().should(exist);
  }

  @Test
  public void testPolicyOwnerLink() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    tile.policyOwnerLink().click();
    waitUntilUrl(OwnerSummaryPage.urlToRootOrg());
  }

  @Test
  public void testPolicyViolationInfo() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    PolicyViolationConstraintInfo constraintInfo = violationDetailsPage.policyViolationConstraintInfo();
    PolicyViolationSecurityDetailsInfoTile securityDetailsInfoTile =
        violationDetailsPage.securityVulnerabilityDetailsTile();

    constraintInfo.headerTitle().shouldBe(visible).shouldHave(exactText("Policy Constraint"));
    constraintInfo.subheaderTitle()
        .shouldBe(visible)
        .shouldHave(exactText("Test Constraint is in violation for the following reason(s):"));
    constraintInfo.reasons().shouldHave(size(1));
    constraintInfo.reason(0).shouldHave(exactText("sonatype-2017-0507"));

    securityDetailsInfoTile.vulnerabilityDetailsHeader()
        .shouldBe(visible)
        .shouldHave(exactText("sonatype-2017-0507"));
  }

  @Test
  public void testSecurityPolicyViolationTabTiles() {
    // Set up a waiver for security violation
    List<ConstraintFact> constraintFacts = securityPolicyViolation.getConstraintFacts();
    String policyId = securityPolicyViolation.getPolicyId();
    String policyName = securityPolicyViolation.getPolicyName();
    String orgId = application.getParentOwnerId();

    tempEntity.newWaiver(
        securityPolicyViolation.getHash(), policyId, orgId, constraintFacts, "A waiver comment");

    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();

    SelenideElement vulnerabilityTab = violationDetailsPage.securityVulnerabilityDetailsTab();
    PolicyViolationApplicableWaiversTab waiversTab = violationDetailsPage.applicableWaiversTab();
    SelenideElement similarWaiversTab = violationDetailsPage.similarWaiversTab();

    PolicyViolationSecurityDetailsInfoTile securityDetailsInfoTile =
        violationDetailsPage.securityVulnerabilityDetailsTile();
    PolicyViolationApplicableWaiversInfoTile applicableWaiversInfoTile =
        violationDetailsPage.applicableWaiversInfoTile();
    PolicyViolationSimilarWaiversInfoTile similarWaiversInfoTile = violationDetailsPage.similarWaiversInfoTile();

    // Check tabs presence
    vulnerabilityTab.shouldBe(visible).shouldHave(exactText("Vulnerability Details"));
    waiversTab.shouldBe(visible).shouldHave(textCaseSensitive("1 Applicable Waivers"));
    similarWaiversTab.shouldBe(visible).shouldHave(textCaseSensitive("Similar Waivers"));

    // Check that default tab (security vulnerability) is displayed and that info is correct.
    securityDetailsInfoTile.vulnerabilityDetailsHeader()
        .shouldBe(visible)
        .shouldHave(exactText("sonatype-2017-0507"));
    applicableWaiversInfoTile.shouldNotBe(visible);
    similarWaiversInfoTile.shouldNotBe(visible);

    // Switch tabs, check visibility
    waiversTab.click();
    securityDetailsInfoTile.shouldNotBe(visible);
    applicableWaiversInfoTile.shouldBe(visible);
    similarWaiversInfoTile.shouldNotBe(visible);
    applicableWaiversInfoTile.waiverListHeader()
        .shouldBe(visible)
        .shouldHave(exactText("Waivers applicable to this violation of " + policyName));
    applicableWaiversInfoTile.getApplicableWaiversTable().shouldBe(visible);

    // Switch tabs, check visibility
    similarWaiversTab.click();
    similarWaiversInfoTile.shouldBe(visible);
    securityDetailsInfoTile.shouldNotBe(visible);
    applicableWaiversInfoTile.shouldNotBe(visible);
    similarWaiversInfoTile.waiverListHeader()
        .shouldBe(visible)
        .shouldHave(exactText("Waivers for similar violations of " + policyName));
    similarWaiversInfoTile.waiverListSubtitle()
        .shouldBe(visible)
        .shouldHave(exactText("Across all component versions implicated by sonatype-2017-0507"));

    // Switch tabs again
    vulnerabilityTab.click();
    securityDetailsInfoTile.shouldBe(visible);
    applicableWaiversInfoTile.shouldNotBe(visible);
  }

  @Test
  public void testNonSecurityPolicyApplicableWaiversTile() {
    // Set up a waiver for security violation
    List<ConstraintFact> constraintFacts = nonSecurityPolicyViolation.getConstraintFacts();
    String policyName = nonSecurityPolicyViolation.getPolicyName();
    String policyId = nonSecurityPolicyViolation.getPolicyId();
    String orgId = application.getParentOwnerId();

    tempEntity.newWaiver(
        nonSecurityPolicyViolation.getHash(), policyId, orgId, constraintFacts, "A waiver comment");

    refreshOrOpen(ViolationDetailsPage.url(nonSecurityPolicyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();

    SelenideElement vulnerabilityTab = violationDetailsPage.securityVulnerabilityDetailsTab();
    PolicyViolationApplicableWaiversTab waiversTab = violationDetailsPage.applicableWaiversTab();
    SelenideElement similarWaiversTab = violationDetailsPage.similarWaiversTab();

    vulnerabilityTab.shouldNotBe(visible);
    waiversTab.shouldBe(visible).shouldHave(text("Applicable Waivers"));
    similarWaiversTab.shouldBe(visible).shouldHave(textCaseSensitive("Similar Waivers"));

    PolicyViolationApplicableWaiversInfoTile applicableWaiversTile =
        violationDetailsPage.applicableWaiversInfoTile();
    PolicyViolationSimilarWaiversInfoTile similarWaiversInfoTile = violationDetailsPage.similarWaiversInfoTile();

    applicableWaiversTile.shouldBe(visible);
    similarWaiversInfoTile.shouldNotBe(visible);
    applicableWaiversTile.waiverListHeader()
        .shouldBe(visible)
        .shouldHave(exactText("Waivers applicable to this violation of " + policyName));

    // Switch tabs, check visibility
    similarWaiversTab.click();
    similarWaiversInfoTile.shouldBe(visible);
    applicableWaiversTile.shouldNotBe(visible);
    similarWaiversInfoTile.waiverListHeader()
        .shouldBe(visible)
        .shouldHave(exactText("Waivers for similar violations of " + policyName));
    similarWaiversInfoTile.waiverListSubtitle()
        .shouldBe(visible)
        .shouldHave(exactText("Across all component versions"));
  }

  @Test
  public void testPolicyViolationInfo_OtherPolicyViolation() {
    refreshOrOpen(ViolationDetailsPage.url(otherPolicyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    PolicyViolationConstraintInfo constraintInfo = violationDetailsPage.policyViolationConstraintInfo();
    PolicyViolationSecurityDetailsInfoTile securityDetailsInfoTile =
        violationDetailsPage.securityVulnerabilityDetailsTile();

    constraintInfo.headerTitle().shouldBe(visible).shouldHave(exactText("Policy Constraint"));
    constraintInfo.subheaderTitle()
        .shouldBe(visible)
        .shouldHave(exactText("Test Constraint is in violation for the following reason(s):"));
    constraintInfo.reasons().shouldHave(size(1));
    constraintInfo.reason(0).shouldHave(exactText("reason"));

    securityDetailsInfoTile.vulnerabilityDetailsHeader().shouldNotBe(visible);
  }

  @Test
  public void testBackButton() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();

    // for now, this is hard-coded to go to the dashboard
    violationDetailsPage.backButton().click();
    waitUntilUrl(DashboardPage.urlToViolations());
  }

  @Test
  public void testSidebarNav() {
    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    SidebarNav sidebarNav = violationDetailsPage.sidebarNav();
    sidebarNav.sidebarNavTitle().shouldHave(text("VIOLATIONS"));

    ElementsCollection navItems = sidebarNav.sidebarNavItems();
    navItems.shouldHave(size(3));

    SidebarNavListItem item1 = sidebarNav.navItem(0);
    item1.shouldHave(cssClass("selected"));
    item1.policyName().shouldHave(text("7 Policy 1"));
    item1.threatIndicator().shouldHave(cssClass("nx-threat-indicator--severe"));
    item1.artifactName().shouldHave(text("Artifact1"));

    SidebarNavListItem item2 = sidebarNav.navItem(1);
    item2.policyName().shouldHave(text("3 Policy 2"));
    item2.threatIndicator().shouldHave(cssClass("nx-threat-indicator--moderate"));
    item2.artifactName().shouldHave(text("Artifact1"));

    SidebarNavListItem item3 = sidebarNav.navItem(2);
    item3.policyName().shouldHave(text("2 Deleted Policy"));
    item3.threatIndicator().shouldHave(cssClass("nx-threat-indicator--moderate"));
    item3.artifactName().shouldHave(text("Artifact1"));
  }

  @Test
  public void testSidebarNavWithNoQueryParameters() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    SidebarNav sidebarNav = violationDetailsPage.sidebarNav();
    sidebarNav.sidebarNavTitle().shouldHave(text("VIOLATIONS"));

    ElementsCollection navItems = sidebarNav.sidebarNavItems();
    navItems.shouldHave(size(1));

    SidebarNavListItem item1 = sidebarNav.navItem(0);
    item1.shouldHave(cssClass("selected"));
    item1.policyName().shouldHave(text("7 Policy 1"));
    item1.artifactName().shouldHave(text("Artifact1"));
    item1.threatIndicator().shouldHave(cssClass("nx-threat-indicator--severe"));
  }

  @Test
  public void testClickingSidebarNavChangesDetailsTile() {
    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    SidebarNav sidebarNav = violationDetailsPage.sidebarNav();
    sidebarNav.sidebarNavTitle().shouldHave(text("VIOLATIONS"));

    ElementsCollection navItems = sidebarNav.sidebarNavItems();
    navItems.shouldHave(size(3));

    SidebarNavListItem item2 = sidebarNav.navItem(1);
    item2.shouldNotHave(cssClass("selected"));
    item2.click();
    item2.shouldHave(cssClass("selected"));

    ViolationDetailsTile detailsTile = violationDetailsPage.detailsTile();
    waitUntilUrl(ViolationDetailsPage.urlWithQueryParams(otherPolicyViolation.getId(), "violation", "filter"));
    detailsTile.headerTitle().shouldHave(text("Violation of Policy 2"));
  }

  @Test
  public void testScrollingToSelection() {
    Instant now = Instant.now();

    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());

    for (int i = 0; i <= 28; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
          StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID,
          "Nu Policy" + i, 6));
    }

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
        "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    PolicyViolation selectedPolicyViolation = tempEntity.newPolicyViolation(
        policyEvaluation1, tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Base Policy", 7));

    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(selectedPolicyViolation.getId(), "violation", "filter", 1));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    SidebarNav sidebarNav = violationDetailsPage.sidebarNav();
    sidebarNav.sidebarNavTitle().shouldHave(text("VIOLATIONS"));

    ElementsCollection navItems = sidebarNav.sidebarNavItems();
    navItems.shouldHave(size(33));

    SidebarNavListItem selectedItem = sidebarNav.navItem(32);
    selectedItem.shouldBe(visible);
    selectedItem.shouldHave(cssClass("selected"));
  }

  @Test
  public void sidebarReflectsDashboard() {
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

    Organization organization2 = tempEntity.newOrganization("Org 2");
    Application app2 = tempEntity.newApplication("App 2", "App2", organization2.getId());
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app2.getId(),
        StageTypes.RELEASE.getId(), "scan1", false, false, Date.from(twoDaysAgo));
    Policy policy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy I", 9);
    PolicyViolation selectedPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation1, policy1);

    for (int i = 0; i < 50; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app2.getId(),
          StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID,
          "Nu Policy" + i, 6));
    }

    refreshOrOpen(DashboardPage.urlToViolations());
    NxTreeViewMultiSelect appFilter = DashboardFilters.applicationFilter();
    DashboardPage.filterToggle().click();
    appFilter.twisty().click();
    appFilter.multiSelectList().shouldHave(size(3));
    appFilter.checkboxItem(3).click();
    AgeFilter ageFilter = DashboardFilters.ageFilter();
    ageFilter.twisty().click();
    ageFilter.radioItem(6).click();
    DashboardFilters.apply();
    DashboardFilters.closeButton().click();

    DashboardPage.violationsView().headers().threatHeader().click();
    DashboardPage.violationsView().results().violations().shouldHave(size(51));

    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(selectedPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    SidebarNav sidebarNav = violationDetailsPage.sidebarNav();
    ElementsCollection navItems = sidebarNav.sidebarNavItems();
    navItems.shouldHave(size(51));
    SidebarNavListItem selectedItem = sidebarNav.navItem(0);
    selectedItem.should(visible);
    violationDetailsPage.backButton().click();

    waitUntilUrl(DashboardPage.urlToViolations());
    DashboardPage.violationsView().results().violations().shouldHave(size(51));

    NxPolicyThreatLevelFilter threatLevelFilter = DashboardFilters.policyThreatLevelFilter();
    DashboardPage.filterToggle().click();
    threatLevelFilter.twisty().click();
    threatLevelFilter.slider().setValues(8, 10);
    DashboardFilters.apply();
    DashboardFilters.closeButton().click();
    DashboardPage.violationsView().results().violations().shouldHave(size(1));

    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(selectedPolicyViolation.getId(), "violation", "filter"));
    navItems.shouldHave(size(1));

    refreshOrOpen(DashboardPage.urlToViolations());
    ManageFiltersDropdown manage = new ManageFiltersDropdown();
    DashboardPage.filterToggle().click();
    manage.openMenuButton().click();
    manage.dropdownMenu().defaultFilterOption().click();
    DashboardFilters.closeButton().click();
  }

  private void mockHdsResponseForVulnerabilityDetails() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("vulnerabilityDetails/vulnerabilityDetails2.json"))
        .atUri("rest/vulnerability/details/json/sonatype-2017-0507");
    testCLMServer.getHdsServer()
        .respondWith(Collections.emptyMap())
        .atUri("rest/vulnerability/details/json");
  }

  @Test
  public void testGoDirectlyToRequestWaiver() {
    try {
      User developerUser = tempEntity.newUser();
      tempEntity.newMembershipMapping(
          Organization.ROOT_ORGANIZATION_ID,
          Role.DEVELOPER_ROLE_ID,
          developerUser.getUsername());
      refreshOrOpen(DashboardPage.url());
      logout();
      login(developerUser.getUsername(), developerUser.getPassword());
      refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
      ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
      ViolationDetailsTile detailsTile = violationDetailsPage.detailsTile();

      detailsTile.requestWaiverButton().shouldBe(visible);
      detailsTile.requestWaiverButton().click();

      RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
      verifyRequestWaiverPage(requestWaiverPage);
      requestWaiverPage.backButton().click();
    }
    finally {
      refreshOrOpen(DashboardPage.url());
      logout();
      loginAsAdmin();
    }
  }

  @Test
  public void testGoDirectlyToAddWaiver() {
    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    ViolationDetailsTile detailsTile = violationDetailsPage.detailsTile();

    detailsTile.addWaiverButton().shouldBe(visible);
    detailsTile.addWaiverButton().click();

    waitUntilUrl(AddWaiverPage.url(securityPolicyViolation.getId()));
    AddWaiverPage addWaiverPage = new AddWaiverPage();

    addWaiverPage.artifactName().shouldHave(text("Artifact1"));
    addWaiverPage.policyName().shouldHave(text("Policy 1"));
    addWaiverPage.constraintName().shouldHave(text("Test Constraint"));
  }

  @Test
  public void testRequestWaiverPageFromSegmentedButton() {
    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    ViolationDetailsTile detailsTile = violationDetailsPage.detailsTile();

    detailsTile.getAddWaiversSegmentedDropdownButton().shouldBe(visible);
    detailsTile.getAddWaiversSegmentedDropdownButton().click();

    detailsTile.requestWaiverButton().shouldBe(visible).click();

    waitUntilUrl(RequestWaiverPage.url(securityPolicyViolation.getId()));
    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    verifyRequestWaiverPage(requestWaiverPage);
  }

  @Test
  public void testWaivedIndicator() {
    // Set up a waiver
    List<ConstraintFact> constraintFacts = otherPolicyViolation.getConstraintFacts();
    String policyId = otherPolicyViolation.getPolicyId();
    String orgId = application.getParentOwnerId();
    tempEntity.newWaiver(otherPolicyViolation.getHash(), policyId, orgId, constraintFacts, "A waiver comment");

    refreshOrOpen(ViolationDetailsPage.url(otherPolicyViolation.getId()));

    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    ViolationDetailsTile detailsTile = violationDetailsPage.detailsTile();

    detailsTile.addWaiverButton().shouldBe(visible);
    violationDetailsPage.applicableWaiversTab().shouldBe(visible).shouldHave(text("1 Applicable Waivers"));
  }

  @Test
  public void testApplicableWaiversTable() {
    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();

    violationDetailsPage.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTable =
        violationDetailsPage.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTable.headerRow().duration().shouldHave(text("DURATION"));
    applicableWaiversTable.headerRow().waiverDetails().shouldHave(text("WAIVER DETAILS"));
    applicableWaiversTable.noWaiversMessage().shouldBe(visible);
    applicableWaiversTable.noWaiversMessage()
        .shouldHave(
            text("You don't have any waivers: to learn more about waivers you can check our help documentation."));

    violationDetailsPage.detailsTile().addWaiverButton().click();
    waitUntilUrl(AddWaiverPage.url(securityPolicyViolation.getId()));
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    final String expectedComment = "Loremipsumdolorsitametconsecteturadipiscingelitseddoeiusmodtempor" +
        "incididuntutlaboreetdoloremagnaaliquaUtenimadminimveniamquisnostrudexercitationullamco" +
        "laborisnisiutaliquipexeacommodoconsequatDuisauteiruredolorinreprehenderitinvoluptate" +
        "velitessecillumdoloreeufugiatnullapariaturExcepteursintoccaecatcupidatatnonproident" +
        "suntinculpaquiofficiadeseruntmollitanimidestlaborum";
    addWaiverPage.comments().setValue(expectedComment);
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    waitUntilUrl(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    violationDetailsPage.applicableWaiversTab().click();
    applicableWaiversTable = violationDetailsPage.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTable.noWaiversMessage().shouldNotBe(visible);
    applicableWaiversTable.rows().shouldHave(size(1));
    applicableWaiversTable.row(1).comments().shouldHave(text(expectedComment));

    eyesWatcher.eyesCheck("Applicable waivers in Violation details");
  }

  @Test
  public void testApplicableWaiversTable_delete() {
    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTable =
        violationDetailsPage.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTable.noWaiversMessage().shouldBe(visible);

    violationDetailsPage.detailsTile().addWaiverButton().click();
    waitUntilUrl(AddWaiverPage.url(securityPolicyViolation.getId()));
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    waitUntilUrl(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    violationDetailsPage.applicableWaiversTab().click();
    applicableWaiversTable.noWaiversMessage().shouldNotBe(visible);
    applicableWaiversTable.rows().shouldHave(size(1));

    applicableWaiversTable.row(1).deleteButton().click();
    DeleteWaiverModal modal = new DeleteWaiverModal();
    modal.root().shouldBe(visible);
    modal.header().shouldHave(text("Delete Waiver"));
    modal.message().shouldHave(text("Are you sure you want to delete this waiver?"));
    modal.yesButton().click();

    applicableWaiversTable.noWaiversMessage().shouldBe(visible);
  }

  @Test
  public void testApplicableWaiversTableWithAutoWaiver() {
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, false);
    securityPolicyViolation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());
    policyViolationDAO.update(securityPolicyViolation);

    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    waitUntilUrl(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTable =
        violationDetailsPage.applicableWaiversInfoTile().getApplicableWaiversTable();

    applicableWaiversTable.rows().shouldHave(size(1));
    ListAutoWaiverTableRow row = applicableWaiversTable.autoWaiverRow(1);
    row.shouldBe(visible);
    row.components().shouldHave(text("Any Component"));
    row.waiverExpiration().shouldHave(text("Auto"));
    row.waiverVersion().shouldHave(text("Current or latest non-violating"));
    row.revocationButton().shouldBe(visible);

    // Verify auto waiver appears before regular waivers
    applicableWaiversTable.rows().shouldHave(size(1));
    applicableWaiversTable.rows().first().shouldHave(cssClass("list-auto-waiver-row"));

    eyesWatcher.eyesCheck("Applicable auto waiver in Violation details");
  }

  @Test
  public void testAutoWaiverWithRegularWaivers() {
    // auto waiver
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId(), 7, false, false);
    securityPolicyViolation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());
    policyViolationDAO.update(securityPolicyViolation);

    // manual waiver
    List<ConstraintFact> constraintFacts = securityPolicyViolation.getConstraintFacts();
    String policyId = securityPolicyViolation.getPolicyId();
    String orgId = application.getParentOwnerId();
    tempEntity.newWaiver(
        securityPolicyViolation.getHash(),
        policyId,
        orgId,
        constraintFacts,
        "Regular waiver comment");

    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    waitUntilUrl(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTable =
        violationDetailsPage.applicableWaiversInfoTile().getApplicableWaiversTable();

    applicableWaiversTable.rows().shouldHave(size(2));

    ListAutoWaiverTableRow autoWaiverRow = applicableWaiversTable.autoWaiverRow(1);
    autoWaiverRow.shouldBe(visible);
    autoWaiverRow.components().shouldHave(text("Any Component"));
    autoWaiverRow.waiverExpiration().shouldHave(text("Auto"));
    autoWaiverRow.waiverVersion().shouldHave(text("Current or latest non-violating"));
    autoWaiverRow.revocationButton().shouldBe(visible);

    ListWaiversTable.ListWaiversTableRow regularWaiverRow = applicableWaiversTable.row(2);
    regularWaiverRow.comments().shouldHave(text("Regular waiver comment"));
  }

  private void verifyRequestWaiverPage(RequestWaiverPage requestWaiverPage) {
    requestWaiverPage.root().shouldBe(visible);
    requestWaiverPage.requestWaiverHeader().shouldHave(text("Request Waiver"));
    requestWaiverPage.requestWaiverTitle().shouldHave(text("Waiver Configuration"));

    requestWaiverPage.requestWaiverComponentName().shouldHave(text("Group1 : Artifact1 : Version1"));

    requestWaiverPage.requestWaiverPolicy().shouldHave(text("Policy"));
    requestWaiverPage.requestWaiverPolicy().shouldHave(text("Policy 1"));

    requestWaiverPage.requestWaiverConstraint().shouldHave(text("Constraint Name"));
    requestWaiverPage.requestWaiverConstraint().shouldHave(text("Test Constraint"));

    requestWaiverPage.requestWaiverConditions().shouldHave(text("Conditions"));
    requestWaiverPage.requestWaiverConditions().shouldHave(text("sonatype-2017-0507"));

    requestWaiverPage.requestWaiverScope().shouldHave(text("Scope"));
    requestWaiverPage.requestWaiverScopeOptions().shouldHave(size(3));
    requestWaiverPage.requestWaiverScopeOptions()
        .shouldHave(
            exactTexts("Application - App 1", "Organization - Org 1", "Organization - Root Organization"));
    requestWaiverPage.requestWaiverScopeOptions().get(0).shouldBe(selected);

    requestWaiverPage.requestWaiverComponents().shouldHave(text("Components"));
    requestWaiverPage.requestWaiverComponentsOptions().shouldHave(size(3));
    requestWaiverPage.requestWaiverComponentsOptions()
        .shouldHave(
            exactTexts("Group1 : Artifact1 : Version1", "Group1 : Artifact1 (all versions)", "All Components"));
    requestWaiverPage.requestWaiverComponentsRadios().get(0).shouldBe(checked);

    requestWaiverPage.requestWaiverExpiryTime().shouldHave(text("Waiver Expiration"));
    requestWaiverPage.requestWaiverExpiryTimeOptions().shouldHave(size(8));
    requestWaiverPage.requestWaiverExpiryTimeOptions()
        .shouldHave(exactTexts("Never", "7 Days", "14 Days", "30 Days",
            "60 Days", "90 Days", "120 Days", "Custom"));
    requestWaiverPage.requestWaiverExpiryTimeOptions().get(0).shouldBe(selected);

    requestWaiverPage.requestWaiverReason().shouldHave(text("Reason"));
    requestWaiverPage.requestWaiverReasonOptions()
        .shouldHave(
            exactTexts("Select a reason", "Acknowledged violation", "Evaluating component", "Mitigated externally",
                "No upgrade path", "Not exploitable", "Not reachable", "Researching", "Other"));
    requestWaiverPage.requestWaiverReasonOptions().get(0).shouldBe(selected);

    requestWaiverPage.requestWaiverComments().shouldBe(empty);
    requestWaiverPage.requestWaiverNoteToReviewer().shouldBe(empty);

    requestWaiverPage.saveButton().shouldBe(visible);
    requestWaiverPage.cancelButton().shouldBe(visible);
  }
}
