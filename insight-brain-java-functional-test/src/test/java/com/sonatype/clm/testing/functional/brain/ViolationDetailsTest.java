/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationInfoTile;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.SidebarNav;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.SidebarNavListItem;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.ViolationDetailsTile;
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
    tile.headerSubtitle().shouldHave(text("Org 1App 1Group1 : Artifact1 : Version1"));
    tile.firstReported().shouldHave(text("2 days ago"));
    tile.lastReported().shouldHave(text("1 day ago"));
    tile.policyType().shouldHave(text("Security"));
    tile.threatLevel().shouldHave(text("7"));
    tile.policyOwner().shouldHave(text("Root Organization"));

    tile.stages().shouldHaveSize(4);

    tile.stage(0).shouldHave(text("Build 2d"));
    tile.stage(0).icon().should(exist);
    tile.stage(0).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(1).shouldHave(text("Stage"));
    tile.stage(1).icon().should(exist);
    tile.stage(1).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(2).shouldHave(text("Release 1d"));
    tile.stage(2).icon().shouldNot(exist);
    tile.stage(2).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(3).shouldHave(text("Operate 1d"));
    tile.stage(3).icon().should(exist);
    tile.stage(3).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());
  }

  @Test
  public void testStageLink() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    tile.stage(0).link().shouldHave(text("Build")).click();
    waitUntilUrl(ApplicationReportPage.url(application, "scan1"));

    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));

    tile.stage(2).link().shouldHave(text("Release")).click();
    waitUntilUrl(ApplicationReportPage.url(application, "scan2"));

    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    tile.stage(1).link().shouldNot(exist);
    tile.stage(3).link().should(exist);
  }

  @Test
  public void testPolicyOwnerLink() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    tile.policyOwner().click();
    waitUntilUrl(OwnerSummaryPage.urlToRootOrg());
  }

  @Test
  public void testPolicyViolationInfoTile() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    PolicyViolationInfoTile tile = new ViolationDetailsPage().policyViolationInfoTile();

    tile.headerTitle().shouldBe(visible).shouldHave(exactText("Policy Constraint - Test Constraint"));
    tile.reasons().shouldHaveSize(1);
    tile.reason(0).shouldHave(exactText("sonatype-2017-0507"));
    tile.vulnerabilityDetailsHeader().shouldBe(visible).shouldHave(exactText("VULNERABILITY ISSUE sonatype-2017-0507"));
  }

  @Test
  public void testPolicyViolationInfoTile_OtherPolicyViolation() {
    refreshOrOpen(ViolationDetailsPage.url(otherPolicyViolation.getId()));
    PolicyViolationInfoTile tile = new ViolationDetailsPage().policyViolationInfoTile();

    tile.headerTitle().shouldBe(visible).shouldHave(exactText("Policy Constraint - Test Constraint"));
    tile.reasons().shouldHaveSize(1);
    tile.reason(0).shouldHave(exactText("reason"));
    tile.vulnerabilityDetailsHeader().shouldNotBe(visible);
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
    navItems.shouldHaveSize(2);

    SidebarNavListItem item1 = sidebarNav.navItem(0);
    item1.shouldHave(cssClass("selected"));
    item1.threatNumberSpan().shouldHave(text("7"));
    item1.threatText().shouldHave(text("Policy 1"));
    item1.threatBar().shouldHave(cssClass("nx-threat-bar--severe"));

    SidebarNavListItem item2 = sidebarNav.navItem(1);
    item2.threatNumberSpan().shouldHave(text("3"));
    item2.threatText().shouldHave(text("Policy 2"));
    item2.threatBar().shouldHave(cssClass("nx-threat-bar--moderate"));
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
    item1.threatNumberSpan().shouldHave(text("7"));
    item1.threatText().shouldHave(text("Policy 1"));
    item1.threatBar().shouldHave(cssClass("nx-threat-bar--severe"));
  }

  @Test
  public void testClickingSidebarNavChangesDetailsTile() {
    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    SidebarNav sidebarNav = violationDetailsPage.sidebarNav();
    sidebarNav.sidebarNavTitle().shouldHave(text("VIOLATIONS"));

    ElementsCollection navItems = sidebarNav.sidebarNavItems();
    navItems.shouldHaveSize(2);

    SidebarNavListItem item2 = sidebarNav.navItem(1);
    item2.shouldNotHave(cssClass("selected"));
    item2.click();
    item2.shouldHave(cssClass("selected"));

    ViolationDetailsTile detailsTile = violationDetailsPage.detailsTile();
    waitUntilUrl(ViolationDetailsPage.urlWithQueryParams(otherPolicyViolation.getId(),"violation","filter"));
    detailsTile.headerTitle().shouldHave(text("Violation of Policy 2"));
  }

  private void mockHdsResponseForVulnerabilityDetails() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("vulnerabilityDetails/vulnerabilityDetails2.json"))
        .atUri("rest/vulnerability/details/json/sonatype-2017-0507");
  }
}
