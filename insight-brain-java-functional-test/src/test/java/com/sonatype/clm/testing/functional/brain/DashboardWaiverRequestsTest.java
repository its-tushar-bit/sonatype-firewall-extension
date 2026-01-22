/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardReasonsFilter;
import com.sonatype.clm.testing.functional.elements.DashboardWaiverRequests.WaiverRequestTile;
import com.sonatype.clm.testing.functional.elements.DashboardWaiverRequests.WaiverRequestsHeaders;
import com.sonatype.clm.testing.functional.elements.DashboardWaiverRequests.WaiverRequestsResults;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiverResultsPaginator;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.proxy.ResponseCopyHandler;
import com.sonatype.insight.brain.dashboard.DashboardPolicyWaiverDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
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

  private static final String CSV_HEADERS = "Waiver Request Id, Threat level, Request Date, Expiration Date, "
      + "Policy Id, Policy Name, Policy Constraints, Scope Type, Scope Id, Scope Name, Component Match Strategy, "
      + "Component Hash, Component Name, Upgrade, Requested by Id, Requested by Name, Comment, Status, "
      + "Is Expire When Remediation Available Waiver, Waiver Reason Id, Waiver Reason Text";

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

  private static final WaiverRequestsHeaders headers = DashboardPage.waiverRequestsView().headers();

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
  public void testWaiverRequestsTable_DefaultCsvExport() {
    // checks csv export when no filters are selected
    // The default order is by creation/request time descending.
    policyWaiverRequests = createWaiverRequests();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

    String exportCsvData = exportWaiverRequestsCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    // by default the waivers are ordered by expiry
    String[] expectedResults = buildExpectedCsvExportData(policyWaiverRequests);
    assertWaiversCsv(exportCsvData, expectedResults);
  }

  @Test
  public void testWaiverRequestsTable_SortsByThreat() {
    policyWaiverRequests = createWaiverRequests();
    refresh();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.waiverRequests().shouldHave(size(9));

    // sort by threat desc
    headers.threatHeader().click();
    headers.threatHeader().sortArrows().shouldBeDown();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().threatNumber().shouldHave(text("9"));
    table.lastWaiverRequest().threatNumber().shouldHave(text("3"));

    // sort by threat asc
    headers.threatHeader().click();
    headers.threatHeader().sortArrows().shouldBeUp();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().threatNumber().shouldHave(text("3"));
    table.lastWaiverRequest().threatNumber().shouldHave(text("9"));
  }

  @Test
  public void testWaiverRequestsTable_SortsByRequestTime() {
    policyWaiverRequests = createWaiverRequests();
    refresh();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.waiverRequests().shouldHave(size(9));

    // sort by request time asc
    headers.dateHeader().click();
    headers.dateHeader().sortArrows().shouldBeUp();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().createTime().shouldHave(text(dateFormat.format(thirtyDaysAgo)));
    table.lastWaiverRequest().createTime().shouldHave(text(dateFormat.format(twoDaysAgo)));

    // sort by request time desc
    headers.dateHeader().click();
    headers.dateHeader().sortArrows().shouldBeDown();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().createTime().shouldHave(text(dateFormat.format(twoDaysAgo)));
    table.lastWaiverRequest().createTime().shouldHave(text(dateFormat.format(thirtyDaysAgo)));
  }

  @Test
  public void testWaiverRequestsTable_SortsByRequesterName() {
    organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy", 7);
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "1.2.3", "", "jar");
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash1").setPolicyId(policy.getId())
        .setOwnerId(organization.getId()).setAssociatedPackageUrl(purl).setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment org").setRequestTime(twoDaysAgo).setExpiryTime(threeDaysFromNow)
        .setRequesterId("testuser1").setRequesterName("Test User 1").setComponentUpgradeAvailable(true));
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash2").setPolicyId(policy.getId())
        .setOwnerId(organization.getId()).setAssociatedPackageUrl(purl).setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment org").setRequestTime(twoDaysAgo).setExpiryTime(threeDaysFromNow)
        .setRequesterId("testuser2").setRequesterName("Test User 2").setComponentUpgradeAvailable(true));

    refresh();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.waiverRequests().shouldHave(size(2));

    // sort by requester name asc
    headers.requesterHeader().click();
    headers.requesterHeader().sortArrows().shouldBeUp();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().requester().shouldHave(text("Test User 1"));
    table.lastWaiverRequest().requester().shouldHave(text("Test User 2"));

    // sort by requester name desc
    headers.requesterHeader().click();
    headers.requesterHeader().sortArrows().shouldBeDown();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().requester().shouldHave(text("Test User 2"));
    table.lastWaiverRequest().requester().shouldHave(text("Test User 1"));
  }

  @Test
  public void testWaiverRequestsTable_SortsByStatus() {
    organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy", 7);
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "1.2.3", "", "jar");
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash1").setPolicyId(policy.getId())
        .setOwnerId(organization.getId()).setAssociatedPackageUrl(purl).setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment org").setRequestTime(twoDaysAgo).setExpiryTime(threeDaysFromNow).setRequesterId("testuser")
        .setRequesterName("Test User").setComponentUpgradeAvailable(true).setStatus(PolicyWaiverRequestStatus.REQUESTED)
    );
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash2").setPolicyId(policy.getId())
        .setOwnerId(organization.getId()).setAssociatedPackageUrl(purl).setComponentMatchStrategy(EXACT_COMPONENT)
        .setComment("comment org").setRequestTime(twoDaysAgo).setExpiryTime(threeDaysFromNow).setRequesterId("testuser")
        .setRequesterName("Test User").setComponentUpgradeAvailable(true).setStatus(PolicyWaiverRequestStatus.APPROVED)
    );

    refresh();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.waiverRequests().shouldHave(size(2));

    // sort by status asc
    headers.statusHeader().click();
    headers.statusHeader().sortArrows().shouldBeUp();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().status().shouldHave(text("APPROVED"));
    table.lastWaiverRequest().status().shouldHave(text("REQUESTED"));

    // sort by status desc
    headers.statusHeader().click();
    headers.statusHeader().sortArrows().shouldBeDown();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().status().shouldHave(text("REQUESTED"));
    table.lastWaiverRequest().status().shouldHave(text("APPROVED"));
  }

  @Test
  public void testWaiverRequestsTable_SortsByPolicy() {
    policyWaiverRequests = createWaiverRequests();
    refresh();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.waiverRequests().shouldHave(size(9));

    // sort by policy asc
    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeUp();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().policy().shouldHave(text("Policy 1"));
    table.lastWaiverRequest().policy().shouldHave(text("Policy 4"));

    // sort by policy desc
    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeDown();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().policy().shouldHave(text("Policy 4"));
    table.lastWaiverRequest().policy().shouldHave(text("Policy 1"));
  }

  @Test
  public void testWaiverRequestsTable_SortsByScope() {
    policyWaiverRequests = createWaiverRequests();
    refresh();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.waiverRequests().shouldHave(size(9));

    // sort by scope asc
    headers.scopeHeader().click();
    headers.scopeHeader().sortArrows().shouldBeUp();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().scope().shouldHave(text("Application - App 1"));
    table.lastWaiverRequest().scope().shouldHave(text("Root Organization"));

    // sort by scope desc
    headers.scopeHeader().click();
    headers.scopeHeader().sortArrows().shouldBeDown();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().scope().shouldHave(text("Root Organization"));
    table.lastWaiverRequest().scope().shouldHave(text("Application - App 1"));
  }

  private void assertWaiversCsv(String csv, String[] expectedSortedResults) {
    String[] lines = csv.split("\r\n");

    // assert CSV header
    assertThat(lines[0]).isEqualTo(CSV_HEADERS);

    // assert CSV results
    String[] results = Arrays.copyOfRange(lines, 1, lines.length);
    assertThat(results).isEqualTo(expectedSortedResults);
  }

  @Test
  public void testWaiverRequestsTable_SortsOnBackendByThreat() {
    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Date pastTime = DateUtils.addDays(now, -i);
      Policy policy =
          tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Policy Threat" + i, i % 10 + 1);

      PolicyEvaluation evaluation =
          tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(), "scan" + i, false, false, pastTime);
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Date expiration = DateUtils.addDays(now, i);
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + app.getId())
          .setPolicyId(policy.getId()).setOwnerId(app.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(fiveDaysAgo)
          .setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + app2.getId())
          .setPolicyId(policy.getId()).setOwnerId(app2.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(fiveDaysAgo)
          .setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + organization.getId())
          .setPolicyId(policy.getId()).setOwnerId(organization.getId())
          .setConstraintFacts(policyViolation.getConstraintFacts()).setComponentMatchStrategy(EXACT_COMPONENT)
          .setComment("comment").setRequestTime(fiveDaysAgo).setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + policy.getId())
          .setPolicyId(policy.getId()).setOwnerId(app.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(fiveDaysAgo)
          .setExpiryTime(expiration));
    }

    refresh();
    DashboardPage.waitUntilSpinnersGone();

    showAllWaivers();
    table.waiverRequests().shouldHave(size(100));

    // sort by threat desc
    headers.threatHeader().click();
    headers.threatHeader().sortArrows().shouldBeDown();

    table.firstWaiverRequest().threatNumber().shouldHave(text("10"));
    table.firstWaiverRequest().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiverRequest(25).threatNumber().shouldHave(text("7"));
    table.waiverRequest(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiverRequest(78).threatNumber().shouldHave(text("3"));
    table.waiverRequest(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiverRequest().threatNumber().shouldHave(text("1"));
    table.lastWaiverRequest().scope().shouldHave(text("Application - App Test Scroll"));

    // sort by threat asc
    headers.threatHeader().click();
    headers.threatHeader().sortArrows().shouldBeUp();

    table.firstWaiverRequest().threatNumber().shouldHave(text("1"));
    table.firstWaiverRequest().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiverRequest(25).threatNumber().shouldHave(text("3"));
    table.waiverRequest(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiverRequest(78).threatNumber().shouldHave(text("7"));
    table.waiverRequest(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiverRequest().threatNumber().shouldHave(text("10"));
    table.lastWaiverRequest().scope().shouldHave(text("Application - App Test Scroll"));
  }

  @Test
  public void testWaiverRequestsTable_SortsOnBackendByRequestTime() {
    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Date pastTime = DateUtils.addDays(now, -i);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Create " + i, i % 10 + 1);

      PolicyEvaluation evaluation =
          tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(), "scan" + i, false, false, pastTime);
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Date expiration = DateUtils.addDays(now, i);
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + app.getId())
          .setPolicyId(policy.getId()).setOwnerId(app.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(twoDaysAgo)
          .setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + app2.getId())
          .setPolicyId(policy.getId()).setOwnerId(app2.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(threeDaysAgo)
          .setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + organization.getId())
          .setPolicyId(policy.getId()).setOwnerId(organization.getId())
          .setConstraintFacts(policyViolation.getConstraintFacts()).setComponentMatchStrategy(EXACT_COMPONENT)
          .setComment("comment").setRequestTime(fiveDaysAgo).setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + policy.getId())
          .setPolicyId(policy.getId()).setOwnerId(app.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(fiveDaysAgo)
          .setExpiryTime(expiration));
    }

    refresh();
    DashboardPage.waitUntilSpinnersGone();

    showAllWaivers();
    table.waiverRequests().shouldHave(size(100));

    // sort by request time asc
    headers.dateHeader().click();
    headers.dateHeader().sortArrows().shouldBeUp();

    table.firstWaiverRequest().createTime().shouldHave(text(dateFormat.format(fiveDaysAgo)));
    table.waiverRequest(52).createTime().shouldHave(text(dateFormat.format(threeDaysAgo)));
    table.lastWaiverRequest().createTime().shouldHave(text(dateFormat.format(twoDaysAgo)));

    // sort by request time desc
    headers.dateHeader().click();
    headers.dateHeader().sortArrows().shouldBeDown();

    table.firstWaiverRequest().createTime().shouldHave(text(dateFormat.format(twoDaysAgo)));
    table.waiverRequest(50).createTime().shouldHave(text(dateFormat.format(threeDaysAgo)));
    table.lastWaiverRequest().createTime().shouldHave(text(dateFormat.format(fiveDaysAgo)));
  }

  @Test
  public void testWaiverRequestsTable_SortsOnBackendByStatus() {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy", 7);
    Date expiration = new Date();

    // create 100+ waivers
    for (int i = 0; i <= 50; i++) {
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i).setPolicyId(policy.getId())
          .setOwnerId(application.getId()).setComponentMatchStrategy(EXACT_COMPONENT).setExpiryTime(expiration)
          .setStatus(PolicyWaiverRequestStatus.REQUESTED));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + (i + 100))
          .setPolicyId(policy.getId()).setOwnerId(application.getId()).setComponentMatchStrategy(EXACT_COMPONENT)
          .setExpiryTime(expiration).setStatus(PolicyWaiverRequestStatus.APPROVED));
    }

    refresh();
    DashboardPage.waitUntilSpinnersGone();

    // sort by status asc
    headers.statusHeader().click();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().status().shouldHave(text("APPROVED"));
    table.lastWaiverRequest().status().shouldHave(text("REQUESTED"));

    // sort by status desc
    headers.statusHeader().click();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().status().shouldHave(text("REQUESTED"));
    table.lastWaiverRequest().status().shouldHave(text("APPROVED"));
  }

  @Test
  public void testWaiverRequestsTable_SortsOnBackendByRequesterName() {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy", 7);
    Date expiration = new Date();

    // create 100+ waivers
    for (int i = 0; i <= 50; i++) {
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i).setPolicyId(policy.getId())
          .setOwnerId(application.getId()).setComponentMatchStrategy(EXACT_COMPONENT).setExpiryTime(expiration)
          .setRequesterId("testuser1").setRequesterName("Test User 1"));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + (i + 100))
          .setPolicyId(policy.getId()).setOwnerId(application.getId()).setComponentMatchStrategy(EXACT_COMPONENT)
          .setExpiryTime(expiration).setRequesterId("testuser2").setRequesterName("Test User 2"));
    }

    refresh();
    DashboardPage.waitUntilSpinnersGone();

    // sort by requester name asc
    headers.requesterHeader().click();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().requester().shouldHave(text("Test User 1"));
    table.lastWaiverRequest().requester().shouldHave(text("Test User 2"));

    // sort by requester name desc
    headers.requesterHeader().click();

    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    table.firstWaiverRequest().requester().shouldHave(text("Test User 2"));
    table.lastWaiverRequest().requester().shouldHave(text("Test User 1"));
  }

  @Test
  public void testWaiverRequestsTable_SortsOnBackendByPolicy() {
    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Date pastTime = DateUtils.addDays(now, -i);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Policy " + i, i % 10 + 1);

      PolicyEvaluation evaluation =
          tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(), "scan" + i, false, false, pastTime);
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Date expiration = DateUtils.addDays(now, i);
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + app.getId())
          .setPolicyId(policy.getId()).setOwnerId(app.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(fiveDaysAgo)
          .setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + app2.getId())
          .setPolicyId(policy.getId()).setOwnerId(app2.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(fiveDaysAgo)
          .setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + organization.getId())
          .setPolicyId(policy.getId()).setOwnerId(organization.getId())
          .setConstraintFacts(policyViolation.getConstraintFacts()).setComponentMatchStrategy(EXACT_COMPONENT)
          .setComment("comment").setRequestTime(fiveDaysAgo).setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + policy.getId())
          .setPolicyId(policy.getId()).setOwnerId(app.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(fiveDaysAgo)
          .setExpiryTime(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(), "scan1", false, false,
        DateUtils.addDays(now, -29));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(), "scan1", false, false,
        DateUtils.addDays(now, -29));

    refresh();
    DashboardPage.waitUntilSpinnersGone();

    showAllWaivers();
    table.waiverRequests().shouldHave(size(100));

    // sort by policy asc
    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeUp();
    eyesWatcher.eyesCheck();

    table.firstWaiverRequest().policy().shouldHave(text("Dashboard Policy 0"));
    table.firstWaiverRequest().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiverRequest(25).policy().shouldHave(text("Dashboard Policy 14"));
    table.waiverRequest(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiverRequest(78).policy().shouldHave(text("Dashboard Policy 3"));
    table.waiverRequest(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiverRequest().policy().shouldHave(text("Dashboard Policy 8"));
    table.lastWaiverRequest().scope().shouldHave(text("Application - App Test Scroll"));

    // sort by policy desc
    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeDown();

    table.firstWaiverRequest().policy().shouldHave(text("Dashboard Policy 9"));
    table.firstWaiverRequest().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiverRequest(25).policy().shouldHave(text("Dashboard Policy 3"));
    table.waiverRequest(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiverRequest(78).policy().shouldHave(text("Dashboard Policy 14"));
    table.waiverRequest(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiverRequest().policy().shouldHave(text("Dashboard Policy 1"));
    table.lastWaiverRequest().scope().shouldHave(text("Application - App Test Scroll"));
  }

  @Test
  public void testWaiverRequestsTable_SortsOnBackendByScope() {
    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll A", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Date pastTime = DateUtils.addDays(now, -i);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Policy Scope" + i, i % 10 + 1);

      PolicyEvaluation evaluation =
          tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(), "scan" + i, false, false, pastTime);
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Date expiration = DateUtils.addDays(now, i);
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + app.getId())
          .setPolicyId(policy.getId()).setOwnerId(app.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(fiveDaysAgo)
          .setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + app2.getId())
          .setPolicyId(policy.getId()).setOwnerId(app2.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(fiveDaysAgo)
          .setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + organization.getId())
          .setPolicyId(policy.getId()).setOwnerId(organization.getId())
          .setConstraintFacts(policyViolation.getConstraintFacts()).setComponentMatchStrategy(EXACT_COMPONENT)
          .setComment("comment").setRequestTime(fiveDaysAgo).setExpiryTime(expiration));
      tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash" + i + policy.getId())
          .setPolicyId(policy.getId()).setOwnerId(app.getId()).setConstraintFacts(policyViolation.getConstraintFacts())
          .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment").setRequestTime(fiveDaysAgo)
          .setExpiryTime(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(), "scan1", false, false,
        DateUtils.addDays(now, -29));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(), "scan1", false, false,
        DateUtils.addDays(now, -29));

    refresh();
    DashboardPage.waitUntilSpinnersGone();

    showAllWaivers();
    table.waiverRequests().shouldHave(size(100));

    // sort by scope asc
    headers.scopeHeader().click();
    headers.scopeHeader().sortArrows().shouldBeUp();

    table.firstWaiverRequest().scope().shouldHave(text("Application - App Test Scroll A"));

    table.waiverRequest(25).scope().shouldHave(text("Application - App Test Scroll A"));
    table.waiverRequest(52).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiverRequest(77).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiverRequest(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiverRequest().scope().shouldHave(text("Organization - Org 2"));

    // sort by scope desc
    headers.scopeHeader().click();
    headers.scopeHeader().sortArrows().shouldBeDown();

    table.firstWaiverRequest().scope().shouldHave(text("Organization - Org 2"));

    table.waiverRequest(25).scope().shouldHave(text("Organization - Org 2"));
    table.waiverRequest(40).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiverRequest(51).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiverRequest(52).scope().shouldHave(text("Application - App Test Scroll A"));

    table.lastWaiverRequest().scope().shouldHave(text("Application - App Test Scroll A"));
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
        new PolicyWaiverRequest().setHash("hash1").setPolicyId(securityPolicies.get(0).getId())
            .setOwnerId(organization.getId()).setAssociatedPackageUrl(purl).setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment("comment org").setRequestTime(twoDaysAgo).setExpiryTime(threeDaysFromNow)
            .setRequesterId("testuser").setRequesterName("Test User").setComponentUpgradeAvailable(true));

    PolicyWaiverRequest policyWaiverRequest2 = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setHash("hash2").setPolicyId(securityPolicies.get(2).getId())
            .setOwnerId(application2.getId()).setAssociatedPackageUrl(purl).setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment("comment app 2").setRequestTime(threeDaysAgo).setExpiryTime(fiveDaysFromNow)
            .setRequesterId("testuser").setRequesterName("Test User").setComponentUpgradeAvailable(true));

    PolicyWaiverRequest policyWaiverRequest3 = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("hash3").setPolicyId(securityPolicies.get(2).getId()).setOwnerId(application.getId())
        .setAssociatedPackageUrl(purl).setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment app 1")
        .setRequestTime(fiveDaysAgo).setRequesterId("testuser").setRequesterName("Test User"));

    PolicyWaiverRequest policyWaiverRequest4 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash4")
            .setPolicyId(securityPolicies.get(1).getId()).setOwnerId(organization.getId()).setAssociatedPackageUrl(purl)
            .setComponentMatchStrategy(ALL_COMPONENTS).setComment("org all components").setRequestTime(sixDaysAgo)
            .setExpiryTime(sixDaysFromNow).setRequesterId("testuser").setRequesterName("Test User"));

    PolicyWaiverRequest policyWaiverRequest5 = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setHash("hash5").setPolicyId(securityPolicies.get(1).getId())
            .setOwnerId(application.getId()).setAssociatedPackageUrl(purl).setComponentMatchStrategy(ALL_VERSIONS)
            .setComment("app all versions").setRequestTime(sevenDaysAgo).setExpiryTime(sevenDaysFromNow)
            .setRequesterId("testuser").setRequesterName("Test User").setComponentUpgradeAvailable(true));

    PolicyWaiverRequest policyWaiverRequest6 =
        tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setHash("hash6")
            .setPolicyId(securityPolicies.get(3).getId()).setOwnerId(rootOrg.getId()).setAssociatedPackageUrl(purl)
            .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment root org").setRequestTime(eightDaysAgo)
            .setExpiryTime(eightDaysFromNow).setRequesterId("testuser").setRequesterName("Test User"));

    PolicyWaiverRequest policyWaiverRequest7 = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setHash("hash7").setPolicyId(securityPolicies.get(0).getId())
            .setOwnerId(repository1.getId()).setAssociatedPackageUrl(purl).setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment("comment repo").setRequestTime(nineDaysAgo).setExpiryTime(nineDaysFromNow)
            .setRequesterId("testuser").setRequesterName("Test User").setComponentUpgradeAvailable(true));

    PolicyWaiverRequest policyWaiverRequest8 = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("hash8").setPolicyId(securityPolicies.get(1).getId())
        .setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID).setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment repo container").setRequestTime(fourteenDaysAgo)
        .setExpiryTime(fourteenDaysFromNow).setRequesterId("testuser").setRequesterName("Test User"));

    PolicyWaiverRequest policyWaiverRequest9 = tempEntity.newPolicyWaiverRequest(
        new PolicyWaiverRequest().setHash("hash9").setPolicyId(securityPolicies.get(1).getId())
            .setOwnerId(parentOrganization.getId()).setAssociatedPackageUrl(purl)
            .setComponentMatchStrategy(EXACT_COMPONENT).setComment("comment parent org").setRequestTime(thirtyDaysAgo)
            .setExpiryTime(thirtyDaysFromNow).setRequesterId("testuser").setRequesterName("Test User"));

    return new ArrayList<>(Arrays.asList(policyWaiverRequest1, policyWaiverRequest2, policyWaiverRequest3,
        policyWaiverRequest4, policyWaiverRequest5, policyWaiverRequest6, policyWaiverRequest7, policyWaiverRequest8,
        policyWaiverRequest9));
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

  private String exportWaiverRequestsCSV() {
    ResponseCopyHandler handler =
        new ResponseCopyHandler("/rest/dashboard/export/policyWaiverRequests", testCLMServer.getCLMServer().getPort());
    reverseProxyServer.addHandler(handler);
    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Waiver Requests Data")).click();
    return new String(handler.consumeResponse());
  }

  private String[] buildExpectedCsvExportData(List<PolicyWaiverRequest> policyWaiverRequests) {
    DateFormat dateFormatCsv = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormatCsv.setTimeZone(TimeZone.getTimeZone("UTC"));

    String[] result = new String[9];

    PolicyWaiverRequest waiverRequest = policyWaiverRequests.get(0);
    result[0] = waiverRequest.getId() + ",7," + dateFormatCsv.format(waiverRequest.getRequestTime()) + ","
        + dateFormatCsv.format(waiverRequest.getExpiryTime()) + "," + waiverRequest.getPolicyId()
        + ",Policy 1,[],organization," + waiverRequest.getOwnerId() + ",Org 1,"
        + waiverRequest.getComponentMatchStrategy() + "," + waiverRequest.getHash() + ",Group1 : Artifact1 : 1.2.3,"
        + DashboardPolicyWaiverDTO
            .getComponentUpgradeAvailableValueCSVExport(waiverRequest.isComponentUpgradeAvailable())
        + "," + waiverRequest.getRequesterId() + "," + waiverRequest.getRequesterName() + ","
        + waiverRequest.getComment() + "," + waiverRequest.getStatus() + ","
        + waiverRequest.isExpireWhenRemediationAvailable() + ",,";

    waiverRequest = policyWaiverRequests.get(1);
    result[1] = waiverRequest.getId() + ",3," + dateFormatCsv.format(waiverRequest.getRequestTime()) + ","
        + dateFormatCsv.format(waiverRequest.getExpiryTime()) + "," + waiverRequest.getPolicyId()
        + ",Policy 3,[],application," + waiverRequest.getOwnerId() + ",App 2,"
        + waiverRequest.getComponentMatchStrategy() + "," + waiverRequest.getHash() + ",Group1 : Artifact1 : 1.2.3,"
        + DashboardPolicyWaiverDTO
            .getComponentUpgradeAvailableValueCSVExport(waiverRequest.isComponentUpgradeAvailable())
        + "," + waiverRequest.getRequesterId() + "," + waiverRequest.getRequesterName() + ","
        + waiverRequest.getComment() + "," + waiverRequest.getStatus() + ","
        + waiverRequest.isExpireWhenRemediationAvailable() + ",,";

    waiverRequest = policyWaiverRequests.get(2);
    result[2] = waiverRequest.getId() + ",3," + dateFormatCsv.format(waiverRequest.getRequestTime()) + ",,"
        + waiverRequest.getPolicyId() + ",Policy 3,[],application," + waiverRequest.getOwnerId() + ",App 1,"
        + waiverRequest.getComponentMatchStrategy() + "," + waiverRequest.getHash() + ",Group1 : Artifact1 : 1.2.3,"
        + DashboardPolicyWaiverDTO
            .getComponentUpgradeAvailableValueCSVExport(waiverRequest.isComponentUpgradeAvailable())
        + "," + waiverRequest.getRequesterId() + "," + waiverRequest.getRequesterName() + ","
        + waiverRequest.getComment() + "," + waiverRequest.getStatus() + ","
        + waiverRequest.isExpireWhenRemediationAvailable() + ",,";

    waiverRequest = policyWaiverRequests.get(3);
    result[3] = waiverRequest.getId() + ",9," + dateFormatCsv.format(waiverRequest.getRequestTime()) + ","
        + dateFormatCsv.format(waiverRequest.getExpiryTime()) + "," + waiverRequest.getPolicyId()
        + ",Policy 2,[],organization," + waiverRequest.getOwnerId() + ",Org 1,"
        + waiverRequest.getComponentMatchStrategy() + "," + waiverRequest.getHash() + ",Group1 : Artifact1 : 1.2.3,"
        + DashboardPolicyWaiverDTO
            .getComponentUpgradeAvailableValueCSVExport(waiverRequest.isComponentUpgradeAvailable())
        + "," + waiverRequest.getRequesterId() + "," + waiverRequest.getRequesterName() + ","
        + waiverRequest.getComment() + "," + waiverRequest.getStatus() + ","
        + waiverRequest.isExpireWhenRemediationAvailable() + ",,";

    waiverRequest = policyWaiverRequests.get(4);
    result[4] = waiverRequest.getId() + ",9," + dateFormatCsv.format(waiverRequest.getRequestTime()) + ","
        + dateFormatCsv.format(waiverRequest.getExpiryTime()) + "," + waiverRequest.getPolicyId()
        + ",Policy 2,[],application," + waiverRequest.getOwnerId() + ",App 1,"
        + waiverRequest.getComponentMatchStrategy() + "," + waiverRequest.getHash() + ",Group1 : Artifact1 : 1.2.3,"
        + DashboardPolicyWaiverDTO
            .getComponentUpgradeAvailableValueCSVExport(waiverRequest.isComponentUpgradeAvailable())
        + "," + waiverRequest.getRequesterId() + "," + waiverRequest.getRequesterName() + ","
        + waiverRequest.getComment() + "," + waiverRequest.getStatus() + ","
        + waiverRequest.isExpireWhenRemediationAvailable() + ",,";

    waiverRequest = policyWaiverRequests.get(5);
    result[5] = waiverRequest.getId() + ",4," + dateFormatCsv.format(waiverRequest.getRequestTime()) + ","
        + dateFormatCsv.format(waiverRequest.getExpiryTime()) + "," + waiverRequest.getPolicyId()
        + ",Policy 4,[],root_organization," + waiverRequest.getOwnerId() + ",Root Organization,"
        + waiverRequest.getComponentMatchStrategy() + "," + waiverRequest.getHash() + ",Group1 : Artifact1 : 1.2.3,"
        + DashboardPolicyWaiverDTO
            .getComponentUpgradeAvailableValueCSVExport(waiverRequest.isComponentUpgradeAvailable())
        + "," + waiverRequest.getRequesterId() + "," + waiverRequest.getRequesterName() + ","
        + waiverRequest.getComment() + "," + waiverRequest.getStatus() + ","
        + waiverRequest.isExpireWhenRemediationAvailable() + ",,";

    waiverRequest = policyWaiverRequests.get(6);
    result[6] = waiverRequest.getId() + ",7," + dateFormatCsv.format(waiverRequest.getRequestTime()) + ","
        + dateFormatCsv.format(waiverRequest.getExpiryTime()) + "," + waiverRequest.getPolicyId()
        + ",Policy 1,[],repository," + waiverRequest.getOwnerId() + ",Repository 1,"
        + waiverRequest.getComponentMatchStrategy() + "," + waiverRequest.getHash() + ",Group1 : Artifact1 : 1.2.3,"
        + DashboardPolicyWaiverDTO
            .getComponentUpgradeAvailableValueCSVExport(waiverRequest.isComponentUpgradeAvailable())
        + "," + waiverRequest.getRequesterId() + "," + waiverRequest.getRequesterName() + ","
        + waiverRequest.getComment() + "," + waiverRequest.getStatus() + ","
        + waiverRequest.isExpireWhenRemediationAvailable() + ",,";

    waiverRequest = policyWaiverRequests.get(7);
    result[7] = waiverRequest.getId() + ",9," + dateFormatCsv.format(waiverRequest.getRequestTime()) + ","
        + dateFormatCsv.format(waiverRequest.getExpiryTime()) + "," + waiverRequest.getPolicyId()
        + ",Policy 2,[],all_repositories," + waiverRequest.getOwnerId() + ",Repository Managers,"
        + waiverRequest.getComponentMatchStrategy() + "," + waiverRequest.getHash() + ",Group1 : Artifact1 : 1.2.3,"
        + DashboardPolicyWaiverDTO
            .getComponentUpgradeAvailableValueCSVExport(waiverRequest.isComponentUpgradeAvailable())
        + "," + waiverRequest.getRequesterId() + "," + waiverRequest.getRequesterName() + ","
        + waiverRequest.getComment() + "," + waiverRequest.getStatus() + ","
        + waiverRequest.isExpireWhenRemediationAvailable() + ",,";

    waiverRequest = policyWaiverRequests.get(8);
    result[8] = waiverRequest.getId() + ",9," + dateFormatCsv.format(waiverRequest.getRequestTime()) + ","
        + dateFormatCsv.format(waiverRequest.getExpiryTime()) + "," + waiverRequest.getPolicyId()
        + ",Policy 2,[],organization," + waiverRequest.getOwnerId() + ",Parent Org 1,"
        + waiverRequest.getComponentMatchStrategy() + "," + waiverRequest.getHash() + ",Group1 : Artifact1 : 1.2.3,"
        + DashboardPolicyWaiverDTO
            .getComponentUpgradeAvailableValueCSVExport(waiverRequest.isComponentUpgradeAvailable())
        + "," + waiverRequest.getRequesterId() + "," + waiverRequest.getRequesterName() + ","
        + waiverRequest.getComment() + "," + waiverRequest.getStatus() + ","
        + waiverRequest.isExpireWhenRemediationAvailable() + ",,";

    return result;
  }

  @Test
  public void testMoreThanOnePage() {
    refresh();
    DashboardPage.waitUntilSpinnersGone();

    createWaiversForPagination();

    refresh();
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.dashboardContainer().shouldBe(visible);

    WaiverResultsPaginator paginator = DashboardPage.waiversView().paginator();

    paginator.buttonBar().shouldBe(visible);
    paginator.nextPageButton().shouldBe(visible);
    paginator.previousPageButton().shouldBe(hidden);
    table.waiverRequests().shouldHave(size(100));
    table.firstWaiverRequest().policy().shouldHave(text("Policy 1"));
    table.firstWaiverRequest().threatNumber().shouldHave(text("7"));

    // click next page
    paginator.nextPageButton().click();
    newFluentWait();
    paginator.nextPageButton().shouldBe(hidden);
    paginator.previousPageButton().shouldBe(visible);
    table.waiverRequests().shouldHave(size(50));
    table.firstWaiverRequest().policy().shouldHave(text("Policy 1"));
    table.firstWaiverRequest().threatNumber().shouldHave(text("7"));

    // Click previous page
    paginator.previousPageButton().click();
    newFluentWait();
    paginator.nextPageButton().shouldBe(visible);
    paginator.previousPageButton().shouldBe(hidden);
    table.waiverRequests().shouldHave(size(100));
    table.firstWaiverRequest().policy().shouldHave(text("Policy 1"));
    table.firstWaiverRequest().threatNumber().shouldHave(text("7"));
  }

  private void newFluentWait() {
    new FluentWait<>(getWebDriver()).withTimeout(Duration.ofSeconds(10)).pollingEvery(Duration.ofSeconds(1))
        .ignoring(NoSuchElementException.class)
        .until(ExpectedConditions.visibilityOf(table.firstWaiverRequest().policy()));
  }

  private void createWaiversForPagination() {
    parentOrganization = tempEntity.newOrganization("Parent Org 1");
    organization = tempEntity.newOrganization("Org 1", parentOrganization);
    application = tempEntity.newApplication("App 1", "app1", organization.getId());

    Policy securityPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getId(),
        "scan1", false, false, twoDaysAgo);

    tempEntity.newPolicyViolation(policyEvaluation, securityPolicy, "Group1", "Artifact1", "Version1", "hash1",
        "sonatype-2017-0507");

    // Component identifier for waiver requests
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "1.2.3", "", "jar");
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    for (int i = 0; i < 150; i++) {
      tempEntity.newPolicyWaiverRequest(
          new PolicyWaiverRequest().setHash("hash1" + i).setPolicyId(securityPolicy.getId())
              .setOwnerId(organization.getId()).setAssociatedPackageUrl(purl).setComponentMatchStrategy(EXACT_COMPONENT)
              .setComment("comment org").setRequestTime(twoDaysAgo).setExpiryTime(threeDaysFromNow)
              .setRequesterId("testuser1").setRequesterName("Test User1").setComponentUpgradeAvailable(true));
    }
  }
}
