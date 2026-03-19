/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiverTile;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiversResults;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.WaiverDetailsPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.CRITICAL;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.MODERATE;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.SEVERE;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;

public class DashboardWaiversTest
    extends AbstractFunctionalTest
{
  private Organization rootOrg;

  private Organization organization;

  private Organization parentOrganization;

  private Application application;

  private Application application2;

  private Repository repository1;

  private ArrayList<PolicyWaiver> policyWaivers;

  private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  private final Instant now = Instant.now();

  private final Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

  private final Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);

  private final Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

  private final Instant sixDaysAgo = now.minus(6, ChronoUnit.DAYS);

  private final Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);

  private final Instant eightDaysAgo = now.minus(8, ChronoUnit.DAYS);

  private final Instant nineDaysAgo = now.minus(9, ChronoUnit.DAYS);

  private final Instant fourteenDaysAgo = now.minus(14, ChronoUnit.DAYS);

  private final Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

  private final Instant fiveDaysFromNow = now.plus(5, ChronoUnit.DAYS);

  private final Instant sixDaysFromNow = now.plus(6, ChronoUnit.DAYS);

  private final Instant sevenDaysFromNow = now.plus(7, ChronoUnit.DAYS);

  private final Instant eightDaysFromNow = now.plus(8, ChronoUnit.DAYS);

  private final Instant threeDaysFromNow = now.plus(3, ChronoUnit.DAYS);

  private final Instant nineDaysFromNow = now.plus(9, ChronoUnit.DAYS);

  private final Instant fourteenDaysFromNow = now.plus(14, ChronoUnit.DAYS);

  private final Instant thirtyDaysFromNow = now.plus(30, ChronoUnit.DAYS);

  private OrganizationDAO organizationDAO;

  private static final String NO_DATA_MSG =
      "No data available in the last 30 days given the applied filters and permissions.";

  private static final WaiversResults table = DashboardPage.waiversView().results();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    loginAsAdmin();
    DashboardPage.waitUntilSpinnersGone();
  }

  @After
  public void cleanup() {
    reverseProxyServer.reset();
  }

  @Before
  public void before() {
    organizationDAO = lookup(OrganizationDAO.class);
    rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWaiversTable_noDataMessage() {
    // no data message check
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));
  }

  @Test
  public void testWaiversTable_loadsAllWaiversWithoutFilters() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    policyWaivers = createWaivers();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.dashboardContainer().shouldBe(visible);
    table.waivers().shouldHave(size(9));

    // check the tile details
    WaiverTile waiver1 = table.firstWaiver();
    waiver1.threatIndicator().shouldHave(SEVERE);
    waiver1.threatNumber().shouldHave(text("7"));
    waiver1.createTime().shouldHave(text(dateFormat.format(Date.from(twoDaysAgo))));
    waiver1.expiryTime().shouldHave(text(dateFormat.format(Date.from(threeDaysFromNow))));
    waiver1.policy().shouldHave(text("Policy 1"));
    waiver1.scope().shouldHave(text("Organization - Org 1"));
    waiver1.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiver1.upgradeAvailable().shouldHave(text("Available"));

    WaiverTile waiver2 = table.waiver(1);
    waiver2.threatIndicator().shouldHave(MODERATE);
    waiver2.threatNumber().shouldHave(text("3"));
    waiver2.createTime().shouldHave(text(dateFormat.format(Date.from(threeDaysAgo))));
    waiver2.expiryTime().shouldHave(text(dateFormat.format(Date.from(fiveDaysFromNow))));
    waiver2.policy().shouldHave(text("Policy 3"));
    waiver2.scope().shouldHave(text("Application - App 2"));
    waiver2.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiver2.upgradeAvailable().shouldHave(text("Available"));

    WaiverTile waiver3 = table.waiver(2);
    waiver3.threatIndicator().shouldHave(CRITICAL);
    waiver3.threatNumber().shouldHave(text("9"));
    waiver3.createTime().shouldHave(text(dateFormat.format(Date.from(sixDaysAgo))));
    waiver3.expiryTime().shouldHave(text(dateFormat.format(Date.from(sixDaysFromNow))));
    waiver3.policy().shouldHave(text("Policy 2"));
    waiver3.scope().shouldHave(text("Organization - Org 1"));
    waiver3.component().shouldHave(text("All Components"));
    waiver3.upgradeAvailable().shouldHave(text("—"));

    WaiverTile waiver4 = table.waiver(3);
    waiver4.threatIndicator().shouldHave(CRITICAL);
    waiver4.threatNumber().shouldHave(text("9"));
    waiver4.createTime().shouldHave(text(dateFormat.format(Date.from(sevenDaysAgo))));
    waiver4.expiryTime().shouldHave(text(dateFormat.format(Date.from(sevenDaysFromNow))));
    waiver4.policy().shouldHave(text("Policy 2"));
    waiver4.scope().shouldHave(text("Application - App 1"));
    waiver4.component().shouldHave(text("Group1 : Artifact1 (all versions)"));
    waiver4.upgradeAvailable().shouldHave(text("—"));

    WaiverTile waiver6 = table.waiver(4);
    waiver6.threatIndicator().shouldHave(SEVERE);
    waiver6.threatNumber().shouldHave(text("4"));
    waiver6.createTime().shouldHave(text(dateFormat.format(Date.from(eightDaysAgo))));
    waiver6.expiryTime().shouldHave(text(dateFormat.format(Date.from(eightDaysFromNow))));
    waiver6.policy().shouldHave(text("Policy 4"));
    waiver6.scope().shouldHave(text(rootOrg.getName()));
    waiver6.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiver6.upgradeAvailable().shouldHave(text("—"));

    WaiverTile repositoryWaiver = table.waiver(5);
    repositoryWaiver.threatIndicator().shouldHave(SEVERE);
    repositoryWaiver.threatNumber().shouldHave(text("7"));
    repositoryWaiver.createTime().shouldHave(text(dateFormat.format(Date.from(nineDaysAgo))));
    repositoryWaiver.expiryTime().shouldHave(text(dateFormat.format(Date.from(nineDaysFromNow))));
    repositoryWaiver.policy().shouldHave(text("Policy 1"));
    repositoryWaiver.scope().shouldHave(text(repository1.getType().toString() + " - " + "repository"));
    repositoryWaiver.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    repositoryWaiver.upgradeAvailable().shouldHave(text("Available"));

    WaiverTile repositoryContainerWaiver = table.waiver(6);
    repositoryContainerWaiver.threatIndicator().shouldHave(CRITICAL);
    repositoryContainerWaiver.threatNumber().shouldHave(text("9"));
    repositoryContainerWaiver.createTime().shouldHave(text(dateFormat.format(Date.from(fourteenDaysAgo))));
    repositoryContainerWaiver.expiryTime().shouldHave(text(dateFormat.format(Date.from(fourteenDaysFromNow))));
    repositoryContainerWaiver.policy().shouldHave(text("Policy 2"));
    repositoryContainerWaiver.scope().shouldHave(text("Repository Managers"));
    repositoryContainerWaiver.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    repositoryContainerWaiver.upgradeAvailable().shouldHave(text("—"));

    WaiverTile waiverParentOrg = table.waiver(7);
    waiverParentOrg.threatIndicator().shouldHave(CRITICAL);
    waiverParentOrg.threatNumber().shouldHave(text("9"));
    waiverParentOrg.createTime().shouldHave(text(dateFormat.format(Date.from(thirtyDaysAgo))));
    waiverParentOrg.expiryTime().shouldHave(text(dateFormat.format(Date.from(thirtyDaysFromNow))));
    waiverParentOrg.policy().shouldHave(text("Policy 2"));
    waiverParentOrg.scope().shouldHave(text("Organization - Parent Org 1"));
    waiverParentOrg.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));

    WaiverTile waiver5 = table.waiver(8);
    waiver5.threatIndicator().shouldHave(MODERATE);
    waiver5.threatNumber().shouldHave(text("3"));
    waiver5.createTime().shouldHave(text(dateFormat.format(Date.from(fiveDaysAgo))));
    waiver5.expiryTime().shouldHave(text("Never"));
    waiver5.policy().shouldHave(text("Policy 3"));
    waiver5.scope().shouldHave(text("Application - App 1"));
    waiver5.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiver5.upgradeAvailable().shouldHave(text("—"));
  }

  @Test
  public void testWaiversTableRowClick() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));

    policyWaivers = createWaivers();
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    showAllWaivers();
    table.waivers().shouldHave(size(7));

    // get first waiver row in table
    table.firstWaiver().click();
    waitUntilUrl(WaiverDetailsPage.urlWithQueryParams("organization",
        organization.getId(), policyWaivers.get(0).getId(), "waiver", "filter", 1));

    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    // get second waiver row in table
    table.waiver(1).click();
    waitUntilUrl(WaiverDetailsPage.urlWithQueryParams("application",
        application2.getId(), policyWaivers.get(1).getId(), "waiver", "filter", 1));

    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    // get third waiver row in table
    table.waiver(2).click();
    waitUntilUrl(WaiverDetailsPage.urlWithQueryParams("organization",
        organization.getId(), policyWaivers.get(3).getId(), "waiver", "filter", 1));

    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
  }

  private ArrayList<PolicyWaiver> createWaivers() {
    parentOrganization = tempEntity.newOrganization("Parent Org 1");
    organization = tempEntity.newOrganization("Org 1", parentOrganization);
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    application2 = tempEntity.newApplication("App 2", "app2", organization.getId());
    repository1 = tempEntity.newRepository("Repository 1");

    ArrayList<Policy> securityPolicies = new ArrayList<>()
    {
      {
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7));
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 2", 9));
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 3", 3));
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 4", 4));
      }
    };

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    tempEntity.newPolicyViolation(policyEvaluation1, securityPolicies.get(0), "Group1",
        "Artifact1", "Version1", "hash1", "sonatype-2017-0507");
    tempEntity.newPolicyViolation(policyEvaluation1, securityPolicies.get(1), "Group2",
        "Artifact2", "Version2", "hash2", "sonatype-2017-8912");
    tempEntity.newPolicyViolation(policyEvaluation2, securityPolicies.get(2), "Group3",
        "Artifact3", "Version3", "hash3", "sonatype-2017-7848");

    // Component identifier for waivers
    TreeMap<String, String> coordinates = new TreeMap<>()
    {
      {
        this.put("artifactId", "Artifact1");
        this.put("groupId", "Group1");
        this.put("version", "1.2.3");
      }
    };

    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    // Default sorting: closer to expire at the top
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash1")
        .setPolicyId(securityPolicies.get(0).getId())
        .setOwnerId(organization.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment org")
        .setCreateTime(Date.from(twoDaysAgo))
        .setExpiryTime(Date.from(threeDaysFromNow))
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(true));

    PolicyWaiver policyWaiver2 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash2")
        .setPolicyId(securityPolicies.get(2).getId())
        .setOwnerId(application2.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment app 2")
        .setCreateTime(Date.from(threeDaysAgo))
        .setExpiryTime(Date.from(fiveDaysFromNow))
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(true));

    PolicyWaiver policyWaiver3 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash3")
        .setPolicyId(securityPolicies.get(2).getId())
        .setOwnerId(application.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment app 1")
        .setCreateTime(Date.from(fiveDaysAgo))
        .setCreatorId("testuser")
        .setCreatorName("Test User"));

    PolicyWaiver policyWaiver4 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash4")
        .setPolicyId(securityPolicies.get(1).getId())
        .setOwnerId(organization.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(ALL_COMPONENTS)
        .setComment("org all components")
        .setCreateTime(Date.from(sixDaysAgo))
        .setExpiryTime(Date.from(sixDaysFromNow))
        .setCreatorId("testuser")
        .setCreatorName("Test User"));

    PolicyWaiver policyWaiver5 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash5")
        .setPolicyId(securityPolicies.get(1).getId())
        .setOwnerId(application.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(ALL_VERSIONS)
        .setComment("app all versions")
        .setCreateTime(Date.from(sevenDaysAgo))
        .setExpiryTime(Date.from(sevenDaysFromNow))
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(true));

    PolicyWaiver policyWaiver6 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash6")
        .setPolicyId(securityPolicies.get(3).getId())
        .setOwnerId(rootOrg.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment root org")
        .setCreateTime(Date.from(eightDaysAgo))
        .setExpiryTime(Date.from(eightDaysFromNow))
        .setCreatorId("testuser")
        .setCreatorName("Test User"));

    PolicyWaiver policyWaiver7 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash7")
        .setPolicyId(securityPolicies.get(0).getId())
        .setOwnerId(repository1.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment repo")
        .setCreateTime(Date.from(nineDaysAgo))
        .setExpiryTime(Date.from(nineDaysFromNow))
        .setCreatorId("testuser")
        .setCreatorName("Test User")
        .setComponentUpgradeAvailable(true));

    PolicyWaiver policyWaiver8 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash8")
        .setPolicyId(securityPolicies.get(1).getId())
        .setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment repo container")
        .setCreateTime(Date.from(fourteenDaysAgo))
        .setExpiryTime(Date.from(fourteenDaysFromNow))
        .setCreatorId("testuser")
        .setCreatorName("Test User"));

    PolicyWaiver policyWaiver9 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash9")
        .setPolicyId(securityPolicies.get(1).getId())
        .setOwnerId(parentOrganization.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment parent org")
        .setCreateTime(Date.from(thirtyDaysAgo))
        .setExpiryTime(Date.from(thirtyDaysFromNow))
        .setCreatorId("testuser")
        .setCreatorName("Test User"));

    return new ArrayList<>(
        Arrays.asList(policyWaiver1, policyWaiver2, policyWaiver3, policyWaiver4, policyWaiver5, policyWaiver6,
            policyWaiver7, policyWaiver8, policyWaiver9));
  }

  private void showAllWaivers() {
    DashboardPage.expandFilter();

    DashboardFilters.organizationFilter().twisty().click();
    DashboardFilters.organizationFilter().allItems().click();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(1, 10);
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.apply();
    DashboardFilters.closeFilter();
  }
}
