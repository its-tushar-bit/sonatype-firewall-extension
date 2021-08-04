/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.AgeFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.ManageFiltersDropdown;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxPolicyThreatLevelFilter;
import com.sonatype.clm.testing.functional.elements.NxTreeViewMultiSelect;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationConstraintInfoTile;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationSecurityDetailsInfoTile;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.SidebarNav;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.SidebarNavListItem;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.ViolationDetailsTile;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import com.codeborne.selenide.ElementsCollection;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ViolationDetailsTest
    extends AbstractFunctionalTest
{
  private static Application application;

  private static PolicyViolation securityPolicyViolation;

  private static PolicyViolation otherPolicyViolation;

  private static PolicyViolation deletedPolicyViolation;

  @BeforeClass
  public static void startup() {
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
    Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);

    Organization organization = staticTempEntity.newOrganization("Org 1");
    application = staticTempEntity.newApplication("App 1", "app1", organization.getId());
    Policy securityPolicy = staticTempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation1 = staticTempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    PolicyEvaluation policyEvaluation2 = staticTempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.RELEASE.getId(), "scan2", false, false, Date.from(oneDayAgo));

    PolicyEvaluation policyEvaluation3 = staticTempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.OPERATE.getId(), "scan3", false, false, Date.from(oneDayAgo));

    securityPolicyViolation = staticTempEntity.newPolicyViolation(policyEvaluation1, securityPolicy, "Group1",
        "Artifact1", "Version1", "hash", "sonatype-2017-0507");
    securityPolicyViolation.setActionTypeId(Action.ID_FAIL);
    policyViolationDAO.update(securityPolicyViolation);

    Policy otherPolicy = staticTempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 2", 3);
    otherPolicyViolation = staticTempEntity.newPolicyViolation(policyEvaluation1, otherPolicy);

    staticTempEntity.newPolicyViolation(policyEvaluation2, securityPolicy);

    PolicyViolation policyViolation3 = staticTempEntity.newPolicyViolation(policyEvaluation3, securityPolicy);
    policyViolation3.setActionTypeId(Action.ID_WARN);
    policyViolationDAO.update(policyViolation3);

    Policy deletedPolicy = staticTempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Deleted Policy", 2);
    deletedPolicyViolation = staticTempEntity.newPolicyViolation(policyEvaluation1, deletedPolicy);
    new PolicyDAO().delete(deletedPolicy);

    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    mockHdsResponseForVulnerabilityDetails();
  }

  @Test
  public void testDetails() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    eyesWatcher.eyesCheck();

    tile.headerTitle().shouldHave(text("Violation of Policy 1"));
    ElementsCollection elements = tile.headerSubtitle().findAll(".iq-violation-details__subtitle-part");
    elements.shouldHaveSize(3);
    elements.get(0).shouldHave(text("Org 1"));
    elements.get(1).shouldHave(text("App 1"));
    elements.get(2).shouldHave(text("Group1 : Artifact1 : Version1"));

    tile.firstReported().shouldHave(text("2 days ago"));
    tile.lastReported().shouldHave(text("1 day ago"));
    tile.policyType().shouldHave(text("Security"));
    tile.threatLevel().shouldHave(text("7"));
    tile.policyOwnerLink().shouldHave(text("Root Organization"));

    tile.stages().shouldHaveSize(5);

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
    //tile.stage(4).shouldHave(text("Operate 1d"));
    //tile.stage(4).icon().should(exist);
    //tile.stage(4).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());
  }

  @Test
  public void testDetails_PolicyNoLongerExists() {
    refreshOrOpen(ViolationDetailsPage.url(deletedPolicyViolation.getId()));
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();
    tile.headerTitle().shouldHave(text("Deleted Policy Policy no longer exists"));
    tile.policyOwner().shouldHave(text("Policy no longer exists"));
    tile.waiversIndicator().shouldNotBe(visible);
    tile.manageWaiversButton().shouldNotBe(visible);
  }

  @Test
  public void testStageLink() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    MainHeader.closeNavigationSidebar();
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    tile.stage(1).link().shouldHave(text("Build")).click();
    waitUntilUrl(ApplicationReportPage.url(application, "scan1"));

    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    MainHeader.closeNavigationSidebar();

    tile.stage(3).link().shouldHave(text("Release")).click();
    waitUntilUrl(ApplicationReportPage.url(application, "scan2"));

    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    MainHeader.closeNavigationSidebar();
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
  public void testPolicyViolationInfoTiles() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    PolicyViolationConstraintInfoTile constraintInfoTile = violationDetailsPage.policyViolationConstraintInfoTile();
    PolicyViolationSecurityDetailsInfoTile securityDetailsInfoTile =
        violationDetailsPage.securityVulnerabilityDetailsTile();

    constraintInfoTile.headerTitle().shouldBe(visible).shouldHave(exactText("Policy Constraint"));
    constraintInfoTile.subheaderTitle().shouldBe(visible)
        .shouldHave(exactText("Test Constraint is in violation for the following reason(s):"));
    constraintInfoTile.reasons().shouldHaveSize(1);
    constraintInfoTile.reason(0).shouldHave(exactText("sonatype-2017-0507"));

    securityDetailsInfoTile.vulnerabilityDetailsHeader().shouldBe(visible)
        .shouldHave(exactText("sonatype-2017-0507"));
  }

  @Test
  public void testPolicyViolationInfoTiles_OtherPolicyViolation() {
    refreshOrOpen(ViolationDetailsPage.url(otherPolicyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    PolicyViolationConstraintInfoTile constraintInfoTile = violationDetailsPage.policyViolationConstraintInfoTile();
    PolicyViolationSecurityDetailsInfoTile securityDetailsInfoTile =
        violationDetailsPage.securityVulnerabilityDetailsTile();

    constraintInfoTile.headerTitle().shouldBe(visible).shouldHave(exactText("Policy Constraint"));
    constraintInfoTile.subheaderTitle().shouldBe(visible)
        .shouldHave(exactText("Test Constraint is in violation for the following reason(s):"));
    constraintInfoTile.reasons().shouldHaveSize(1);
    constraintInfoTile.reason(0).shouldHave(exactText("reason"));

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
    navItems.shouldHaveSize(3);

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
    navItems.shouldHaveSize(1);

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
    navItems.shouldHaveSize(3);

    SidebarNavListItem item2 = sidebarNav.navItem(1);
    item2.shouldNotHave(cssClass("selected"));
    item2.click();
    item2.shouldHave(cssClass("selected"));

    ViolationDetailsTile detailsTile = violationDetailsPage.detailsTile();
    waitUntilUrl(ViolationDetailsPage.urlWithQueryParams(otherPolicyViolation.getId(),"violation","filter"));
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

    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(selectedPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    SidebarNav sidebarNav = violationDetailsPage.sidebarNav();
    sidebarNav.sidebarNavTitle().shouldHave(text("VIOLATIONS"));

    ElementsCollection navItems = sidebarNav.sidebarNavItems();
    navItems.shouldHaveSize(33);

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
    appFilter.multiSelectList().shouldHaveSize(3);
    appFilter.checkboxItem(3).click();
    AgeFilter ageFilter = DashboardFilters.ageFilter();
    ageFilter.twisty().click();
    ageFilter.radioItem(6).click();
    DashboardFilters.apply();
    DashboardFilters.closeButton().click();

    DashboardPage.violationsView().headers().threatHeader().click();
    DashboardPage.violationsView().results().violations().shouldHaveSize(51);

    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(selectedPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    SidebarNav sidebarNav = violationDetailsPage.sidebarNav();
    ElementsCollection navItems = sidebarNav.sidebarNavItems();
    navItems.shouldHaveSize(51);
    SidebarNavListItem selectedItem = sidebarNav.navItem(0);
    selectedItem.should(visible);
    violationDetailsPage.backButton().click();

    waitUntilUrl(DashboardPage.urlToViolations());
    DashboardPage.violationsView().results().violations().shouldHaveSize(51);

    NxPolicyThreatLevelFilter threatLevelFilter = DashboardFilters.policyThreatLevelFilter();
    DashboardPage.filterToggle().click();
    threatLevelFilter.twisty().click();
    threatLevelFilter.slider().setValues(8, 10);
    DashboardFilters.apply();
    DashboardFilters.closeButton().click();
    DashboardPage.violationsView().results().violations().shouldHaveSize(1);

    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(selectedPolicyViolation.getId(), "violation", "filter"));
    navItems.shouldHaveSize(1);

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
  }

  @Test
  public void testAddWaiver() {
    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    ViolationDetailsTile detailsTile = violationDetailsPage.detailsTile();
    
    detailsTile.manageWaiversButton().shouldBe(visible);
    detailsTile.manageWaiversButton().click();

    waitUntilUrl(ListWaiversPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.waiverListTable().noWaiversMessage().shouldBe(visible);
    listWaiversPage.addWaiverButton().shouldBe(visible, enabled).click();

    waitUntilUrl(AddWaiverPage.url(securityPolicyViolation.getId()));
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.cancelButton().shouldBe(visible, enabled).click();
    // clicking cancel takes back to list waivers page
    waitUntilUrl(ListWaiversPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));

    listWaiversPage.addWaiverButton().shouldBe(visible, enabled).click();
    waitUntilUrl(AddWaiverPage.url(securityPolicyViolation.getId()));
    addWaiverPage.artifactName().shouldHave(text("Artifact1"));
    addWaiverPage.policyName().shouldHave(text("Policy 1"));
    addWaiverPage.constraintName().shouldHave(text("Test Constraint"));
    addWaiverPage.comments().setValue("Test Comment");
    addWaiverPage.saveButton().shouldBe(visible, enabled).click();
    waitUntilUrl(ListWaiversPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    // verify that it was added
    listWaiversPage.waiverListTable().noWaiversMessage().shouldNotBe(visible);
    listWaiversPage.waiverListTable().rows().shouldHaveSize(1);
    listWaiversPage.waiverListTable().row(1).comments().shouldHave(text("Test Comment"));
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

    detailsTile.manageWaiversButton().shouldBe(visible);
    detailsTile.waiversIndicator().shouldBe(visible);
    detailsTile.waiversIndicator().shouldHave(text("1 Active Waiver"));
  }
}
