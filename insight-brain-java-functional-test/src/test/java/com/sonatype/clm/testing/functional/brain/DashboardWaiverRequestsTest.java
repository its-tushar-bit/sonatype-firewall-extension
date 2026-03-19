/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardReasonsFilter;
import com.sonatype.clm.testing.functional.elements.DashboardWaiverRequests.WaiverRequestTile;
import com.sonatype.clm.testing.functional.elements.DashboardWaiverRequests.WaiverRequestsResults;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.time.DateUtils;
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
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardWaiverRequestsTest
    extends AbstractFunctionalTest
{
  private Organization rootOrg;

  private Organization organization;

  private Organization parentOrganization;

  private Application application;

  private Application application2;

  private Repository repository1;

  private ArrayList<PolicyWaiverRequest> policyWaiverRequests;

  private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  private final Date now = new Date();

  private final Date twoDaysAgo = DateUtils.addDays(now, -2);

  private final Date threeDaysAgo = DateUtils.addDays(now, -3);

  private final Date fiveDaysAgo = DateUtils.addDays(now, -5);

  private final Date sixDaysAgo = DateUtils.addDays(now, -6);

  private final Date sevenDaysAgo = DateUtils.addDays(now, -7);

  private final Date eightDaysAgo = DateUtils.addDays(now, -8);

  private final Date nineDaysAgo = DateUtils.addDays(now, -9);

  private final Date fourteenDaysAgo = DateUtils.addDays(now, -14);

  private final Date thirtyDaysAgo = DateUtils.addDays(now, -30);

  private final Date fiveDaysFromNow = DateUtils.addDays(now, 5);

  private final Date sixDaysFromNow = DateUtils.addDays(now, 6);

  private final Date sevenDaysFromNow = DateUtils.addDays(now, 7);

  private final Date eightDaysFromNow = DateUtils.addDays(now, 8);

  private final Date threeDaysFromNow = DateUtils.addDays(now, 3);

  private final Date nineDaysFromNow = DateUtils.addDays(now, 9);

  private final Date fourteenDaysFromNow = DateUtils.addDays(now, 14);

  private final Date thirtyDaysFromNow = DateUtils.addDays(now, 30);

  private OrganizationDAO organizationDAO;

  private static final String NO_DATA_MSG =
      "No data available in the last 30 days given the applied filters and permissions.";

  private static final WaiverRequestsResults table = DashboardPage.waiverRequestsView().results();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.urlToWaiverRequests());
    loginAsAdmin();
  }

  @After
  public void cleanup() {
    reverseProxyServer.reset();
  }

  @Before
  public void before() {
    organizationDAO = lookup(OrganizationDAO.class);
    rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    refreshOrOpen(DashboardPage.urlToWaiverRequests());
    DashboardPage.waitUntilSpinnersGone();
  }

  @Test
  public void testWaiverRequestsTable_NoDataMessage() {
    // no data message check
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));
  }

  @Test
  public void testWaiverRequestsTable_LoadsAllWaiverRequestsWithoutFilters() {
    policyWaiverRequests = createWaiverRequests();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.dashboardContainer().shouldBe(visible);
    table.waiverRequests().shouldHave(size(9));

    // check the tile details
    // The default order is by creation/request time descending.
    WaiverRequestTile waiverRequest = table.waiverRequest(0);
    waiverRequest.threatIndicator().shouldHave(SEVERE);
    waiverRequest.threatNumber().shouldHave(text("7"));
    waiverRequest.createTime().shouldHave(text(dateFormat.format(twoDaysAgo)));
    waiverRequest.requester().shouldHave(text("Test User"));
    waiverRequest.policy().shouldHave(text("Policy 1"));
    waiverRequest.scope().shouldHave(text("Organization - Org 1"));
    waiverRequest.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiverRequest.status().shouldHave(text("Requested"));

    waiverRequest = table.waiverRequest(1);
    waiverRequest.threatIndicator().shouldHave(MODERATE);
    waiverRequest.threatNumber().shouldHave(text("3"));
    waiverRequest.createTime().shouldHave(text(dateFormat.format(threeDaysAgo)));
    waiverRequest.requester().shouldHave(text("Test User"));
    waiverRequest.policy().shouldHave(text("Policy 3"));
    waiverRequest.scope().shouldHave(text("Application - App 2"));
    waiverRequest.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiverRequest.status().shouldHave(text("Requested"));

    waiverRequest = table.waiverRequest(2);
    waiverRequest.threatIndicator().shouldHave(MODERATE);
    waiverRequest.threatNumber().shouldHave(text("3"));
    waiverRequest.createTime().shouldHave(text(dateFormat.format(fiveDaysAgo)));
    waiverRequest.requester().shouldHave(text("Test User"));
    waiverRequest.policy().shouldHave(text("Policy 3"));
    waiverRequest.scope().shouldHave(text("Application - App 1"));
    waiverRequest.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiverRequest.status().shouldHave(text("Requested"));

    waiverRequest = table.waiverRequest(3);
    waiverRequest.threatIndicator().shouldHave(CRITICAL);
    waiverRequest.threatNumber().shouldHave(text("9"));
    waiverRequest.createTime().shouldHave(text(dateFormat.format(sixDaysAgo)));
    waiverRequest.requester().shouldHave(text("Test User"));
    waiverRequest.policy().shouldHave(text("Policy 2"));
    waiverRequest.scope().shouldHave(text("Organization - Org 1"));
    waiverRequest.component().shouldHave(text("All components"));
    waiverRequest.status().shouldHave(text("Requested"));

    waiverRequest = table.waiverRequest(4);
    waiverRequest.threatIndicator().shouldHave(CRITICAL);
    waiverRequest.threatNumber().shouldHave(text("9"));
    waiverRequest.createTime().shouldHave(text(dateFormat.format(sevenDaysAgo)));
    waiverRequest.requester().shouldHave(text("Test User"));
    waiverRequest.policy().shouldHave(text("Policy 2"));
    waiverRequest.scope().shouldHave(text("Application - App 1"));
    waiverRequest.component().shouldHave(text("Group1 : Artifact1 (all versions)"));
    waiverRequest.status().shouldHave(text("Requested"));

    waiverRequest = table.waiverRequest(5);
    waiverRequest.threatIndicator().shouldHave(SEVERE);
    waiverRequest.threatNumber().shouldHave(text("4"));
    waiverRequest.createTime().shouldHave(text(dateFormat.format(eightDaysAgo)));
    waiverRequest.requester().shouldHave(text("Test User"));
    waiverRequest.policy().shouldHave(text("Policy 4"));
    waiverRequest.scope().shouldHave(text(rootOrg.getName()));
    waiverRequest.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiverRequest.status().shouldHave(text("Requested"));

    waiverRequest = table.waiverRequest(6);
    waiverRequest.threatIndicator().shouldHave(SEVERE);
    waiverRequest.threatNumber().shouldHave(text("7"));
    waiverRequest.createTime().shouldHave(text(dateFormat.format(nineDaysAgo)));
    waiverRequest.requester().shouldHave(text("Test User"));
    waiverRequest.policy().shouldHave(text("Policy 1"));
    waiverRequest.scope().shouldHave(text("Repository - Repository 1"));
    waiverRequest.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiverRequest.status().shouldHave(text("Requested"));

    waiverRequest = table.waiverRequest(7);
    waiverRequest.threatIndicator().shouldHave(CRITICAL);
    waiverRequest.threatNumber().shouldHave(text("9"));
    waiverRequest.createTime().shouldHave(text(dateFormat.format(fourteenDaysAgo)));
    waiverRequest.requester().shouldHave(text("Test User"));
    waiverRequest.policy().shouldHave(text("Policy 2"));
    waiverRequest.scope().shouldHave(text("Repository Managers"));
    waiverRequest.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiverRequest.status().shouldHave(text("Requested"));

    waiverRequest = table.waiverRequest(8);
    waiverRequest.threatIndicator().shouldHave(CRITICAL);
    waiverRequest.threatNumber().shouldHave(text("9"));
    waiverRequest.createTime().shouldHave(text(dateFormat.format(thirtyDaysAgo)));
    waiverRequest.requester().shouldHave(text("Test User"));
    waiverRequest.policy().shouldHave(text("Policy 2"));
    waiverRequest.scope().shouldHave(text("Organization - Parent Org 1"));
    waiverRequest.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    waiverRequest.status().shouldHave(text("Requested"));
  }

  @Test
  public void testShowsReasonsFilter() {
    refresh();

    DashboardPage.expandFilter();
    DashboardFilters.filterContainer().shouldBe(visible);
    DashboardFilters.iqPolicyWaiverReasonFilter().shouldBe(visible);
    DashboardReasonsFilter dashboardReasonsFilter = DashboardFilters.iqPolicyWaiverReasonFilter();
    dashboardReasonsFilter.click();

    List<String> labels = dashboardReasonsFilter.getLabels().stream().map(label -> label.getText().trim()).toList();

    assertThat(labels).containsExactly("all/none", "Acknowledged violation", "Evaluating component",
        "Mitigated externally", "No upgrade path", "Not exploitable", "Not reachable", "Researching", "Other",
        "(No reason provided)");
  }

  private ArrayList<PolicyWaiverRequest> createWaiverRequests() {
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

    // Component identifier for waiver requests
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "1.2.3", "", "jar");
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    // Default sorting: closer to expire at the top
    PolicyWaiverRequest policyWaiverRequest1 = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setHash("hash1")
            .setPolicyId(securityPolicies.get(0).getId())
            .setOwnerId(organization.getId())
            .setAssociatedPackageUrl(purl)
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment("comment org")
            .setRequestTime(twoDaysAgo)
            .setExpiryTime(threeDaysFromNow)
            .setRequesterId("testuser")
            .setRequesterName("Test User")
            .setComponentUpgradeAvailable(true));

    PolicyWaiverRequest policyWaiverRequest2 = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setHash("hash2")
            .setPolicyId(securityPolicies.get(2).getId())
            .setOwnerId(application2.getId())
            .setAssociatedPackageUrl(purl)
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment("comment app 2")
            .setRequestTime(threeDaysAgo)
            .setExpiryTime(fiveDaysFromNow)
            .setRequesterId("testuser")
            .setRequesterName("Test User")
            .setComponentUpgradeAvailable(true));

    PolicyWaiverRequest policyWaiverRequest3 = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("hash3")
        .setPolicyId(securityPolicies.get(2).getId())
        .setOwnerId(application.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment app 1")
        .setRequestTime(fiveDaysAgo)
        .setRequesterId("testuser")
        .setRequesterName("Test User"));

    PolicyWaiverRequest policyWaiverRequest4 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash4")
            .setPolicyId(securityPolicies.get(1).getId())
            .setOwnerId(organization.getId())
            .setAssociatedPackageUrl(purl)
            .setComponentMatchStrategy(ALL_COMPONENTS)
            .setComment("org all components")
            .setRequestTime(sixDaysAgo)
            .setExpiryTime(sixDaysFromNow)
            .setRequesterId("testuser")
            .setRequesterName("Test User"));

    PolicyWaiverRequest policyWaiverRequest5 = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setHash("hash5")
            .setPolicyId(securityPolicies.get(1).getId())
            .setOwnerId(application.getId())
            .setAssociatedPackageUrl(purl)
            .setComponentMatchStrategy(ALL_VERSIONS)
            .setComment("app all versions")
            .setRequestTime(sevenDaysAgo)
            .setExpiryTime(sevenDaysFromNow)
            .setRequesterId("testuser")
            .setRequesterName("Test User")
            .setComponentUpgradeAvailable(true));

    PolicyWaiverRequest policyWaiverRequest6 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash6")
            .setPolicyId(securityPolicies.get(3).getId())
            .setOwnerId(rootOrg.getId())
            .setAssociatedPackageUrl(purl)
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment("comment root org")
            .setRequestTime(eightDaysAgo)
            .setExpiryTime(eightDaysFromNow)
            .setRequesterId("testuser")
            .setRequesterName("Test User"));

    PolicyWaiverRequest policyWaiverRequest7 = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setHash("hash7")
            .setPolicyId(securityPolicies.get(0).getId())
            .setOwnerId(repository1.getId())
            .setAssociatedPackageUrl(purl)
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment("comment repo")
            .setRequestTime(nineDaysAgo)
            .setExpiryTime(nineDaysFromNow)
            .setRequesterId("testuser")
            .setRequesterName("Test User")
            .setComponentUpgradeAvailable(true));

    PolicyWaiverRequest policyWaiverRequest8 = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("hash8")
        .setPolicyId(securityPolicies.get(1).getId())
        .setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment repo container")
        .setRequestTime(fourteenDaysAgo)
        .setExpiryTime(fourteenDaysFromNow)
        .setRequesterId("testuser")
        .setRequesterName("Test User"));

    PolicyWaiverRequest policyWaiverRequest9 = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setHash("hash9")
            .setPolicyId(securityPolicies.get(1).getId())
            .setOwnerId(parentOrganization.getId())
            .setAssociatedPackageUrl(purl)
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment("comment parent org")
            .setRequestTime(thirtyDaysAgo)
            .setExpiryTime(thirtyDaysFromNow)
            .setRequesterId("testuser")
            .setRequesterName("Test User"));

    return new ArrayList<>(Arrays.asList(policyWaiverRequest1, policyWaiverRequest2, policyWaiverRequest3,
        policyWaiverRequest4, policyWaiverRequest5, policyWaiverRequest6, policyWaiverRequest7, policyWaiverRequest8,
        policyWaiverRequest9));
  }
}
