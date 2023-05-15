/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.IqVulnerabilityModal;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.DeleteWaiverModal;
import com.sonatype.clm.testing.functional.pages.WaiverDetailsPage;
import com.sonatype.clm.testing.functional.pages.WaiverDetailsPage.SidebarNav;
import com.sonatype.clm.testing.functional.pages.WaiverDetailsPage.SidebarNavListItem;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.TestPolicyWaiverBuilder;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.codeborne.selenide.ElementsCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.CRITICAL;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.MODERATE;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.SEVERE;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;

public class WaiverDetailsTest extends AbstractFunctionalTest
{
  private Organization organization;

  private Application application;

  private ArrayList<Policy> securityPolicies;

  private ArrayList<PolicyViolation> policyViolations;

  private ArrayList<PolicyWaiver> policyWaivers;

  @Before
  public void startup() {
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
    Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);
    Instant fiveDaysFromNow = now.plus(5, ChronoUnit.DAYS);
    Instant threeDaysFromNow = now.plus(3, ChronoUnit.DAYS);

    organization = tempEntity.newOrganization("Org 1");
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    securityPolicies = new ArrayList<Policy>() {{
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7));
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 2", 9));
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 3", 3));
      }};

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    policyViolations = new ArrayList<PolicyViolation>() {{
        this.add(tempEntity.newPolicyViolation(policyEvaluation1, securityPolicies.get(0), "Group1",
            "Artifact1", "Version1", "hash1", "sonatype-2017-0507"));
        this.add(tempEntity.newPolicyViolation(policyEvaluation1, securityPolicies.get(1), "Group2",
            "Artifact2", "Version2", "hash2", "sonatype-2017-8912"));
        this.add(tempEntity.newPolicyViolation(policyEvaluation1, securityPolicies.get(2), "Group3",
            "Artifact3", "Version3", "hash3", "sonatype-2017-7848"));
      }};

    // Component identifier for waivers
    TreeMap<String, String> coordinates = new TreeMap<String, String>() {{
        this.put("artifactId", "Artifact1");
        this.put("groupId", "Group1");
        this.put("version", "1.2.3");
      }};

    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    PolicyWaiver policyWaiver1 = tempEntity.newWaiver(new TestPolicyWaiverBuilder()
        .withHash("hash1")
        .withPolicyId(securityPolicies.get(0).getId())
        .withOwnerId(application.getId())
        .withAssociatedPackageUrl(purl)
            .withConstraintFacts(policyViolations.get(0).getConstraintFacts())
        .withComponentMatcherStrategyForWaiver(EXACT_COMPONENT)
        .withComment("comment")
        .withCreateTime(Date.from(twoDaysAgo))
        .withExpiryTime(Date.from(threeDaysFromNow))
        .withCreatorName("Test User")
        .withComponentUpgradeAvailable(true)
        .build());

    PolicyWaiver policyWaiver2 = tempEntity.newWaiver(new TestPolicyWaiverBuilder()
        .withHash("hash2")
        .withPolicyId(securityPolicies.get(1).getId())
        .withOwnerId(application.getId())
        .withAssociatedPackageUrl(purl)
        .withConstraintFacts(policyViolations.get(1).getConstraintFacts())
        .withComponentMatcherStrategyForWaiver(EXACT_COMPONENT)
        .withComment("comment")
        .withCreateTime(Date.from(threeDaysAgo))
        .withExpiryTime(Date.from(fiveDaysFromNow))
        .build());

    PolicyWaiver policyWaiver3 = tempEntity.newWaiver(new TestPolicyWaiverBuilder()
        .withHash("hash3")
        .withPolicyId(securityPolicies.get(2).getId())
        .withOwnerId(application.getId())
        .withAssociatedPackageUrl(purl)
        .withConstraintFacts(policyViolations.get(2).getConstraintFacts())
        .withComponentMatcherStrategyForWaiver(EXACT_COMPONENT)
        .withComment("comment")
        .withCreateTime(Date.from(fiveDaysAgo))
        .withExpiryTime(null)
        .build());

    policyWaivers = new ArrayList<PolicyWaiver>() {{
        add(policyWaiver1);
        add(policyWaiver2);
        add(policyWaiver3);
      }};

    refreshOrOpen(WaiverDetailsPage.url("ownerTypeId", "ownerId", "waiverId"));
    loginAsAdmin();
  }

  @After
  public void after() {
    logout();
  }

  @Test
  public void testSidebarNav() {
    refreshOrOpen(WaiverDetailsPage.urlWithQueryParams(application.getType().toString().toLowerCase(Locale.ROOT),
        application.getId(), policyWaivers.get(0).getId(), "waiver", "filter"));
    WaiverDetailsPage waiverDetailsPage = new WaiverDetailsPage();
    WaiverDetailsPage.SidebarNav sidebarNav = waiverDetailsPage.sidebarNav();
    sidebarNav.sidebarNavTitle().shouldHave(text("WAIVERS"));

    ElementsCollection navItems = sidebarNav.sidebarNavItems();
    navItems.shouldHaveSize(3);

    SidebarNavListItem item1 = sidebarNav.navItem(0);
    item1.shouldHave(cssClass("selected"));
    item1.policyName().shouldHave(text("7 Policy 1"));
    item1.threatIndicator().shouldHave(SEVERE);
    item1.componentName().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    item1.organizationFullName().shouldHave(text(application.getType().toString() + " - " + application.getName()));

    SidebarNavListItem item2 = sidebarNav.navItem(1);
    item2.shouldNotHave(cssClass("selected"));
    item2.policyName().shouldHave(text("9 Policy 2"));
    item2.threatIndicator().shouldHave(CRITICAL);
    item2.componentName().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    item2.organizationFullName().shouldHave(text(application.getType().toString() + " - " + application.getName()));

    SidebarNavListItem item3 = sidebarNav.navItem(2);
    item3.shouldNotHave(cssClass("selected"));
    item3.policyName().shouldHave(text("3 Policy 3"));
    item3.threatIndicator().shouldHave(MODERATE);
    item3.componentName().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    item3.organizationFullName().shouldHave(text(application.getType().toString() + " - " + application.getName()));
  }

  @Test
  public void testSidebarNav_DeepLink() {
    refreshOrOpen(WaiverDetailsPage.url(application.getType().toString().toLowerCase(Locale.ROOT),
        application.getId(), policyWaivers.get(0).getId()));
    WaiverDetailsPage waiverDetailsPage = new WaiverDetailsPage();
    WaiverDetailsPage.SidebarNav sidebarNav = waiverDetailsPage.sidebarNav();
    sidebarNav.sidebarNavTitle().shouldHave(text("WAIVERS"));

    ElementsCollection navItems = sidebarNav.sidebarNavItems();
    navItems.shouldHaveSize(1);

    SidebarNavListItem item1 = sidebarNav.navItem(0);
    item1.shouldHave(cssClass("selected"));
    item1.policyName().shouldHave(text("7 Policy 1"));
    item1.threatIndicator().shouldHave(SEVERE);
    item1.componentName().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    item1.organizationFullName().shouldHave(text(application.getType().toString() + " - " + application.getName()));
  }

  @Test
  public void testClickingSidebarNavChangesSelected() {
    refreshOrOpen(WaiverDetailsPage.urlWithQueryParams(application.getType().toString().toLowerCase(Locale.ROOT),
        application.getId(), policyWaivers.get(0).getId(), "waiver", "filter"));
    WaiverDetailsPage waiverDetailsPage = new WaiverDetailsPage();
    SidebarNav sidebarNav = waiverDetailsPage.sidebarNav();
    sidebarNav.sidebarNavTitle().shouldHave(text("WAIVERS"));

    WaiverDetailsPage.SidebarNavListItem item2 = sidebarNav.navItem(1);
    item2.shouldNotHave(cssClass("selected"));
    item2.click();
    item2.shouldHave(cssClass("selected"));
  }

  @Test
  public void testSidebarItemOverflowTooltip() {
    Instant now = Instant.now();
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);
    Instant expiration = now.plus(1, ChronoUnit.DAYS);

    Application veryLongAppName =
        tempEntity.newApplication(
            "A Very Long Application Name - Lorem ipsum dolor sit amet consectetur adipisicing elit",
            "app-lorem",
            organization.getId());

    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash4", securityPolicies.get(0).getId(), veryLongAppName.getId(),
        policyViolations.get(1).getConstraintFacts(), EXACT_COMPONENT, "comment",
        Date.from(fiveDaysAgo), Date.from(expiration));

    refreshOrOpen(WaiverDetailsPage.urlWithQueryParams(veryLongAppName.getType().toString().toLowerCase(Locale.ROOT),
        veryLongAppName.getId(), policyWaiver.getId(), "waiver", "filter"));

    WaiverDetailsPage waiverDetailsPage = new WaiverDetailsPage();
    WaiverDetailsPage.SidebarNav sidebarNav = waiverDetailsPage.sidebarNav();
    WaiverDetailsPage.SidebarNavListItem firstItem = sidebarNav.navItem(0);

    Tooltip.get().shouldBe(hidden);

    firstItem.organizationFullName().hover();
    Tooltip.get().shouldBe(visible);
    Tooltip.get().shouldHave(text(veryLongAppName.getType().toString() + " - " + veryLongAppName.getName()));
  }

  @Test
  public void testPageLayout() {
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
    Instant threeDaysFromNow = now.plus(3, ChronoUnit.DAYS);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy").withZone(ZoneId.systemDefault());
    String createdDate = formatter.format(twoDaysAgo);
    String expirationDate = formatter.format(threeDaysFromNow);

    refreshOrOpen(WaiverDetailsPage.urlWithQueryParams(application.getType().toString().toLowerCase(Locale.ROOT),
        application.getId(), policyWaivers.get(0).getId(), "waiver", "filter"));
    WaiverDetailsPage waiverDetailsPage = new WaiverDetailsPage();

    waiverDetailsPage.detailsTileHeader().shouldHave(text("Waiver Detail View"));
    waiverDetailsPage.detailsPolicy().shouldHave(text("Policy 1"));
    waiverDetailsPage.detailsConstraint().shouldHave(text("Test Constraint"));
    waiverDetailsPage.detailsConditions().shouldHave(text("sonatype-2017-0507"));
    waiverDetailsPage.vulnerabilityDetailsLink().shouldHave(text("See Security Vulnerability Details"));
    waiverDetailsPage.detailsScope().shouldHave(text("Application - App 1"));
    waiverDetailsPage.detailsComponent().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiverDetailsPage.detailsExpiration().shouldHave(text(expirationDate));
    waiverDetailsPage.detailsComment().shouldHave(text("comment"));
    waiverDetailsPage.detailsCreatedBy().shouldHave(text("Test User"));
    waiverDetailsPage.detailsDateCreated().shouldHave(text(createdDate));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testScrollingToSelection() {
    Instant now = Instant.now();
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());

    for (int i = 0; i <= 28; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Scroll Policy " + i, i % 10);

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
          StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Instant expiration = now.plus(i, ChronoUnit.DAYS);
      tempEntity.newWaiver("hash" + i, policy.getId(), application.getId(),
          policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
          Date.from(fiveDaysAgo), Date.from(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
        "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    // Open the last waiver available in the list
    refreshOrOpen(WaiverDetailsPage.urlWithQueryParams(application.getType().toString().toLowerCase(Locale.ROOT),
        application.getId(), policyWaivers.get(2).getId(), "waiver", "filter"));
    WaiverDetailsPage waiverDetailsPage = new WaiverDetailsPage();
    WaiverDetailsPage.SidebarNav sidebarNav = waiverDetailsPage.sidebarNav();
    sidebarNav.sidebarNavTitle().shouldHave(text("Waivers"));

    ElementsCollection navItems = sidebarNav.sidebarNavItems();
    navItems.shouldHaveSize(26);

    WaiverDetailsPage.SidebarNavListItem selectedItem = sidebarNav.navItem(25);
    selectedItem.shouldBe(visible);
    selectedItem.shouldHave(cssClass("selected"));
  }

  @Test
  public void testVulnerabilityDetailsModal() {
    refreshOrOpen(WaiverDetailsPage.urlWithQueryParams(application.getType().toString().toLowerCase(Locale.ROOT),
        application.getId(), policyWaivers.get(0).getId(), "waiver", "filter"));

    WaiverDetailsPage waiverDetailsPage = new WaiverDetailsPage();
    waiverDetailsPage.vulnerabilityDetailsLink().shouldHave(text("See Security Vulnerability Details"));
    waiverDetailsPage.vulnerabilityDetailsLink().click();

    IqVulnerabilityModal detailsModal = waiverDetailsPage.detailsModal();
    detailsModal.shouldBe(visible);
    detailsModal.closeButton().shouldHave(text("Close")).click();
    detailsModal.shouldNot(exist);
  }

  @Test
  public void testDeleteWaiversModal() {
    refreshOrOpen(WaiverDetailsPage.urlWithQueryParams(application.getType().toString().toLowerCase(Locale.ROOT),
        application.getId(), policyWaivers.get(0).getId(), "waiver", "filter"));

    WaiverDetailsPage waiverDetailsPage = new WaiverDetailsPage();
    DeleteWaiverModal deleteWaiverModal = new DeleteWaiverModal();
    waiverDetailsPage.detailsPolicy().shouldHave(text("Policy 1"));
    waiverDetailsPage.deleteWaiverButton().click();
    deleteWaiverModal.yesButton().click();
    waitUntilUrl(WaiverDetailsPage.urlWithQueryParams(application.getType().toString().toLowerCase(Locale.ROOT),
        application.getId(), policyWaivers.get(0).getId(), "waiver", "filter"));
    waiverDetailsPage.detailsPolicy().shouldHave(text("Policy 2"));
  }
}
