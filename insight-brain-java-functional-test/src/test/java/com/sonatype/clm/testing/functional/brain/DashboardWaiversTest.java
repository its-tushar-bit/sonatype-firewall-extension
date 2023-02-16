/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiverTile;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiversHeaders;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiversResults;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.WaiverDetailsPage;
import com.sonatype.clm.testing.functional.utils.proxy.ResponseCopyHandler;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.CRITICAL;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.MODERATE;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.SEVERE;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;

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

  private static final String CSV_HEADERS = "Waiver Id, Threat level, Created Date, Expiration Date," +
          " Policy Id, Policy Name, Policy Constraints, Scope Type, Scope Id, Scope Name," +
          " Component Match Strategy, Component Hash, Component Name, Created by Id, Created by Name,Comment";

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

  private final Instant fourteenDaysFromNow = now.plus(14,ChronoUnit.DAYS);

  private final Instant thirtyDaysFromNow = now.plus(30, ChronoUnit.DAYS);

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private static final String NO_DATA_MSG =
      "No data available in the last 30 days given the applied filters and permissions.";

  private static ResponseCopyHandler responseCopyHandler;

  private static final WaiversResults table = DashboardPage.waiversView().results();

  private static final WaiversHeaders headers = DashboardPage.waiversView().headers();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    loginAsAdmin();

    responseCopyHandler = new ResponseCopyHandler("/rest/dashboard/export/policyWaivers",
        testCLMServer.getCLMServer().getPort());
    reverseProxyServer.addHandler(responseCopyHandler);

  }

  @Before
  public void before() {
    rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWaiversTable_noDataMessage() {
    // no data message check
    refreshOrOpen(DashboardPage.urlToWaivers());
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));
  }

  @Test
  public void testWaiversTable_loadsAllWaiversWithoutFilters() {
    refreshOrOpen(DashboardPage.urlToWaivers());

    policyWaivers = createWaivers();
    refresh();

    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldBe(hidden);
    table.waivers().shouldHaveSize(9);

    // check the tile details
    WaiverTile waiver1 = table.firstWaiver();
    waiver1.threatIndicator().shouldHave(SEVERE);
    waiver1.threatNumber().shouldHave(text("7"));
    waiver1.createTime().shouldHave(text(dateFormat.format(Date.from(twoDaysAgo))));
    waiver1.expiryTime().shouldHave(text(dateFormat.format(Date.from(threeDaysFromNow))));
    waiver1.policy().shouldHave(text("Policy 1"));
    waiver1.scope().shouldHave(text("Organization - Org 1"));
    waiver1.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));

    WaiverTile waiver2 = table.waiver(1);
    waiver2.threatIndicator().shouldHave(MODERATE);
    waiver2.threatNumber().shouldHave(text("3"));
    waiver2.createTime().shouldHave(text(dateFormat.format(Date.from(threeDaysAgo))));
    waiver2.expiryTime().shouldHave(text(dateFormat.format(Date.from(fiveDaysFromNow))));
    waiver2.policy().shouldHave(text("Policy 3"));
    waiver2.scope().shouldHave(text("Application - App 2"));
    waiver2.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));

    WaiverTile waiver3 = table.waiver(2);
    waiver3.threatIndicator().shouldHave(CRITICAL);
    waiver3.threatNumber().shouldHave(text("9"));
    waiver3.createTime().shouldHave(text(dateFormat.format(Date.from(sixDaysAgo))));
    waiver3.expiryTime().shouldHave(text(dateFormat.format(Date.from(sixDaysFromNow))));
    waiver3.policy().shouldHave(text("Policy 2"));
    waiver3.scope().shouldHave(text("Organization - Org 1"));
    waiver3.component().shouldHave(text("All Components"));

    WaiverTile waiver4 = table.waiver(3);
    waiver4.threatIndicator().shouldHave(CRITICAL);
    waiver4.threatNumber().shouldHave(text("9"));
    waiver4.createTime().shouldHave(text(dateFormat.format(Date.from(sevenDaysAgo))));
    waiver4.expiryTime().shouldHave(text(dateFormat.format(Date.from(sevenDaysFromNow))));
    waiver4.policy().shouldHave(text("Policy 2"));
    waiver4.scope().shouldHave(text("Application - App 1"));
    waiver4.component().shouldHave(text("Group1 : Artifact1 (all versions)"));

    WaiverTile waiver6 = table.waiver(4);
    waiver6.threatIndicator().shouldHave(SEVERE);
    waiver6.threatNumber().shouldHave(text("4"));
    waiver6.createTime().shouldHave(text(dateFormat.format(Date.from(eightDaysAgo))));
    waiver6.expiryTime().shouldHave(text(dateFormat.format(Date.from(eightDaysFromNow))));
    waiver6.policy().shouldHave(text("Policy 4"));
    waiver6.scope().shouldHave(text(rootOrg.getName()));
    waiver6.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));

    WaiverTile repositoryWaiver = table.waiver(5);
    repositoryWaiver.threatIndicator().shouldHave(SEVERE);
    repositoryWaiver.threatNumber().shouldHave(text("7"));
    repositoryWaiver.createTime().shouldHave(text(dateFormat.format(Date.from(nineDaysAgo))));
    repositoryWaiver.expiryTime().shouldHave(text(dateFormat.format(Date.from(nineDaysFromNow))));
    repositoryWaiver.policy().shouldHave(text("Policy 1"));
    repositoryWaiver.scope().shouldHave(text(repository1.getType().toString() + " - " + "repository"));
    repositoryWaiver.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));

    WaiverTile repositoryContainerWaiver = table.waiver(6);
    repositoryContainerWaiver.threatIndicator().shouldHave(CRITICAL);
    repositoryContainerWaiver.threatNumber().shouldHave(text("9"));
    repositoryContainerWaiver.createTime().shouldHave(text(dateFormat.format(Date.from(fourteenDaysAgo))));
    repositoryContainerWaiver.expiryTime().shouldHave(text(dateFormat.format(Date.from(fourteenDaysFromNow))));
    repositoryContainerWaiver.policy().shouldHave(text("Policy 2"));
    repositoryContainerWaiver.scope().shouldHave(text("All Repositories"));
    repositoryContainerWaiver.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));

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
  }

  @Test
  public void testWaiversTable_defaultCsvExport() {
    // checks csv export when no filters are selected
    refreshOrOpen(DashboardPage.urlToWaivers());

    policyWaivers = createWaivers();
    refresh();

    String exportCsvData = exportWaiversCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    // by default the waivers are ordered by expiry
    String[] expectedResults = buildExpectedCsvExportDataBySortColumn(policyWaivers, "expiry");
    assertWaiversCsv(exportCsvData, expectedResults);
  }

  @Test
  public void testWaiversTable_sortByThreat() {
    refreshOrOpen(DashboardPage.urlToWaivers());

    policyWaivers = createWaivers();
    refresh();

    // sort by threat
    headers.threatHeader().click();

    String exportCsvData = exportWaiversCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    // assert table
    table.firstWaiver().threatNumber().shouldHave(text("9"));
    table.lastWaiver().threatNumber().shouldHave(text("3"));

    // assert csv export
    String[] expectedResults = buildExpectedCsvExportDataBySortColumn(policyWaivers, "threat");
    assertWaiversCsv(exportCsvData, expectedResults);
  }

  @Test
  public void testWaiversTable_sortByCreatedDate() {
    refreshOrOpen(DashboardPage.urlToWaivers());

    policyWaivers = createWaivers();
    refresh();

    // sort by creation date
    headers.dateHeader().click();

    String exportCsvData = exportWaiversCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    // assert table
    table.firstWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(thirtyDaysAgo))));
    table.lastWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(twoDaysAgo))));

    // assert csv export
    String[] expectedResults = buildExpectedCsvExportDataBySortColumn(policyWaivers, "createddate");
    assertWaiversCsv(exportCsvData, expectedResults);
  }

  @Test
  public void testWaiversTable_sortByPolicy() {
    refreshOrOpen(DashboardPage.urlToWaivers());

    policyWaivers = createWaivers();
    refresh();

    // sort by policy
    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeUp();
    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeDown();

    String exportCsvData = exportWaiversCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    // assert table
    table.firstWaiver().policy().shouldHave(text("Policy 4"));
    table.lastWaiver().policy().shouldHave(text("Policy 1"));

    // assert csv export
    String[] expectedResults = buildExpectedCsvExportDataBySortColumn(policyWaivers, "policy");
    assertWaiversCsv(exportCsvData, expectedResults);
  }

  @Test
  public void testWaiversTable_sortByScope() {
    refreshOrOpen(DashboardPage.urlToWaivers());

    policyWaivers = createWaivers();
    refresh();

    // sort by scope
    headers.scopeHeader().click();

    table.firstWaiver().scope().shouldHave(text("All Repositories"));
    table.lastWaiver().scope().shouldHave(text("Root Organization"));

    // assert csv export
    String exportCsvData = exportWaiversCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    String[] expectedResults = buildExpectedCsvExportDataBySortColumn(policyWaivers, "scope");
    assertWaiversCsv(exportCsvData, expectedResults);
  }

  @Test
  public void testWaiversTable_sortByComponent() {
    refreshOrOpen(DashboardPage.urlToWaivers());

    policyWaivers = createWaivers();
    refresh();

    // sort by component
    headers.componentHeader().click();

    String exportCsvData = exportWaiversCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    // assert table
    table.firstWaiver().component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    table.lastWaiver().component().shouldHave(text("All Components"));

    // assert csv export
    String[] expectedResults = buildExpectedCsvExportDataBySortColumn(policyWaivers, "component");
    assertWaiversCsv(exportCsvData, expectedResults);
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
  public void testSortsOnBackendByThreat() {
    Instant now = Instant.now();
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Policy Threat" + i,
              i % 10 + 1);

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
              StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Instant expiration = now.plus(i, ChronoUnit.DAYS);
      tempEntity.newWaiver("hash" + i + app.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + app2.getId(), policy.getId(), app2.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + organization.getId(), policy.getId(), organization.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + policy.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.maxResultsMessage().shouldBe(visible);
    table.waivers().shouldHaveSize(100);

    // sort by threat desc
    headers.threatHeader().click();
    headers.threatHeader().sortArrows().shouldBeDown();

    table.firstWaiver().threatNumber().shouldHave(text("10"));
    table.firstWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiver(25).threatNumber().shouldHave(text("7"));
    table.waiver(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiver(78).threatNumber().shouldHave(text("3"));
    table.waiver(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiver().threatNumber().shouldHave(text("1"));
    table.lastWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    // sort by threat asc
    headers.threatHeader().click();
    headers.threatHeader().sortArrows().shouldBeUp();

    table.firstWaiver().threatNumber().shouldHave(text("1"));
    table.firstWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiver(25).threatNumber().shouldHave(text("3"));
    table.waiver(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiver(78).threatNumber().shouldHave(text("7"));
    table.waiver(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiver().threatNumber().shouldHave(text("10"));
    table.lastWaiver().scope().shouldHave(text("Application - App Test Scroll"));
  }

  @Test
  public void testSortsOnBackendByCreatedDate() {
    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Create " + i, i % 10 + 1);

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
              StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Instant expiration = now.plus(i, ChronoUnit.DAYS);
      tempEntity.newWaiver("hash" + i + app.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(twoDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + app2.getId(), policy.getId(), app2.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(threeDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + organization.getId(), policy.getId(), organization.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + policy.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.maxResultsMessage().shouldBe(visible);
    table.waivers().shouldHaveSize(100);

    // sort by creation date asc
    headers.dateHeader().click();
    headers.dateHeader().sortArrows().shouldBeUp();

    table.firstWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(fiveDaysAgo))));
    table.waiver(52).createTime().shouldHave(text(dateFormat.format(Date.from(threeDaysAgo))));
    table.lastWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(twoDaysAgo))));

    // sort by creation date desc
    headers.dateHeader().click();
    headers.dateHeader().sortArrows().shouldBeDown();

    table.firstWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(twoDaysAgo))));
    table.waiver(50).createTime().shouldHave(text(dateFormat.format(Date.from(threeDaysAgo))));
    table.lastWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(fiveDaysAgo))));
  }

  @Test
  public void testSortsOnBackendByExpirationDate() {
    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    Instant lastExpiryDate = null;
    Instant firstExpiryDate = null;

    // create 101 waivers
    for (int i = 0; i < 25; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Expiry " + i, i % 10 + 1);

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
              StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Instant expiration = now.plus(i, ChronoUnit.DAYS);
      if (i == 0) {
        firstExpiryDate = expiration;
      }
      if (i == 24) {
        tempEntity.newWaiver("hash" + i + 1 + policy.getId(), policy.getId(), app.getId(),
                policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
                Date.from(fiveDaysAgo), Date.from(expiration));
        lastExpiryDate = expiration;
      }
      tempEntity.newWaiver("hash" + i + app.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(twoDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + app2.getId(), policy.getId(), app2.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(threeDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + organization.getId(), policy.getId(), organization.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), null);
      tempEntity.newWaiver("hash" + i + policy.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.maxResultsMessage().shouldBe(visible);
    table.waivers().shouldHaveSize(100);

    // sort by expiration date desc
    headers.expirationHeader().click();
    headers.expirationHeader().sortArrows().shouldBeDown();

    table.firstWaiver().expiryTime().shouldHave(text("Never"));
    table.waiver(26).expiryTime().shouldHave(text(dateFormat.format(Date.from(lastExpiryDate))));
    table.lastWaiver().expiryTime().shouldHave(text(dateFormat.format(Date.from(firstExpiryDate))));

    // sort by expiration date asc
    headers.expirationHeader().click();
    headers.expirationHeader().sortArrows().shouldBeUp();

    table.firstWaiver().expiryTime().shouldHave(text(dateFormat.format(Date.from(firstExpiryDate))));
    table.waiver(75).expiryTime().shouldHave(text(dateFormat.format(Date.from(lastExpiryDate))));
    table.lastWaiver().expiryTime().shouldHave(text("Never"));
  }

  @Test
  public void testSortsOnBackendByPolicy() {
    Instant now = Instant.now();
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      Policy policy = staticTempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Policy " + i,
              i % 10 + 1);

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
              StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Instant expiration = now.plus(i, ChronoUnit.DAYS);
      tempEntity.newWaiver("hash" + i + app.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + app2.getId(), policy.getId(), app2.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + organization.getId(), policy.getId(), organization.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + policy.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.maxResultsMessage().shouldBe(visible);
    table.waivers().shouldHaveSize(100);

    // sort by policy asc
    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeUp();
    eyesWatcher.eyesCheck();

    table.firstWaiver().policy().shouldHave(text("Dashboard Policy 0"));
    table.firstWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiver(25).policy().shouldHave(text("Dashboard Policy 14"));
    table.waiver(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiver(78).policy().shouldHave(text("Dashboard Policy 3"));
    table.waiver(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiver().policy().shouldHave(text("Dashboard Policy 8"));
    table.lastWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    // sort by policy desc
    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeDown();

    table.firstWaiver().policy().shouldHave(text("Dashboard Policy 9"));
    table.firstWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiver(25).policy().shouldHave(text("Dashboard Policy 3"));
    table.waiver(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiver(78).policy().shouldHave(text("Dashboard Policy 14"));
    table.waiver(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiver().policy().shouldHave(text("Dashboard Policy 1"));
    table.lastWaiver().scope().shouldHave(text("Application - App Test Scroll"));
  }

  @Test
  public void testSortsOnBackendByScope() {
    Instant now = Instant.now();
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll A", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Policy Scope" + i, i % 10 + 1);

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
              StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Instant expiration = now.plus(i, ChronoUnit.DAYS);
      tempEntity.newWaiver("hash" + i + app.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + app2.getId(), policy.getId(), app2.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + organization.getId(), policy.getId(), organization.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + policy.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.maxResultsMessage().shouldBe(visible);
    table.waivers().shouldHaveSize(100);

    // sort by scope asc
    headers.scopeHeader().click();
    headers.scopeHeader().sortArrows().shouldBeUp();

    table.firstWaiver().scope().shouldHave(text("Application - App Test Scroll A"));

    table.waiver(25).scope().shouldHave(text("Application - App Test Scroll A"));
    table.waiver(52).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiver(77).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiver(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiver().scope().shouldHave(text("Organization - Org 2"));

    // sort by scope desc
    headers.scopeHeader().click();
    headers.scopeHeader().sortArrows().shouldBeDown();

    table.firstWaiver().scope().shouldHave(text("Organization - Org 2"));

    table.waiver(25).scope().shouldHave(text("Organization - Org 2"));
    table.waiver(40).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiver(51).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiver(52).scope().shouldHave(text("Application - App Test Scroll A"));

    table.lastWaiver().scope().shouldHave(text("Application - App Test Scroll A"));
  }

  @Test
  public void testWaiversTableRowClick() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));

    policyWaivers = createWaivers();
    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.waivers().shouldHaveSize(7);

    // get first waiver row in table
    table.firstWaiver().click();
    waitUntilUrl(WaiverDetailsPage.urlWithQueryParams("organization",
        organization.getId(), policyWaivers.get(0).getId(), "waiver", "filter"));

    refreshOrOpen(DashboardPage.urlToWaivers());

    // get second waiver row in table
    table.waiver(1).click();
    waitUntilUrl(WaiverDetailsPage.urlWithQueryParams("application",
            application2.getId(), policyWaivers.get(1).getId(), "waiver", "filter"));

    refreshOrOpen(DashboardPage.urlToWaivers());

    // get third waiver row in table
    table.waiver(2).click();
    waitUntilUrl(WaiverDetailsPage.urlWithQueryParams("organization",
        organization.getId(), policyWaivers.get(3).getId(), "waiver", "filter"));

    refreshOrOpen(DashboardPage.urlToWaivers());
  }

  private ArrayList<PolicyWaiver> createWaivers() {
    parentOrganization = tempEntity.newOrganization("Parent Org 1");
    organization = tempEntity.newOrganization("Org 1", parentOrganization);
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    application2 = tempEntity.newApplication("App 2", "app2", organization.getId());
    repository1 = tempEntity.newRepository("Repository 1");

    ArrayList<Policy> securityPolicies = new ArrayList<Policy>() {{
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7));
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 2", 9));
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 3", 3));
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 4", 4));
      }};

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
    TreeMap<String, String> coordinates = new TreeMap<String, String>() {{
        this.put("artifactId", "Artifact1");
        this.put("groupId", "Group1");
        this.put("version", "1.2.3");
      }};

    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    // Default sorting: closer to expire at the top
    return new ArrayList<PolicyWaiver>() {{
        this.add(tempEntity.newWaiver("hash1", securityPolicies.get(0).getId(), organization.getId(),
            null, purl, EXACT_COMPONENT, "comment org",
            Date.from(twoDaysAgo), Date.from(threeDaysFromNow)));
        this.add(tempEntity.newWaiver("hash2", securityPolicies.get(2).getId(), application2.getId(),
            null, purl, EXACT_COMPONENT, "comment app 2",
            Date.from(threeDaysAgo), Date.from(fiveDaysFromNow)));
        this.add(tempEntity.newWaiver("hash3", securityPolicies.get(2).getId(), application.getId(),
            null, purl, EXACT_COMPONENT, "comment app 1",
            Date.from(fiveDaysAgo), null));
        this.add(tempEntity.newWaiver("hash4", securityPolicies.get(1).getId(), organization.getId(),
            null, purl, ALL_COMPONENTS, "org all components",
            Date.from(sixDaysAgo), Date.from(sixDaysFromNow)));
        this.add(tempEntity.newWaiver("hash5", securityPolicies.get(1).getId(), application.getId(),
            null, purl, ALL_VERSIONS, "app all versions",
            Date.from(sevenDaysAgo), Date.from(sevenDaysFromNow)));
        this.add(tempEntity.newWaiver("hash6", securityPolicies.get(3).getId(), rootOrg.getId(),
            null, purl, EXACT_COMPONENT, "comment root org",
            Date.from(eightDaysAgo), Date.from(eightDaysFromNow)));
        this.add(tempEntity.newWaiver("hash7", securityPolicies.get(0).getId(), repository1.getId(),
            null, purl, EXACT_COMPONENT, "comment repo",
            Date.from(nineDaysAgo), Date.from(nineDaysFromNow)));
        this.add(tempEntity.newWaiver("hash8", securityPolicies.get(1).getId(),
              RepositoryContainer.REPOSITORY_CONTAINER_ID,
            null, purl, EXACT_COMPONENT, "comment repo container",
            Date.from(fourteenDaysAgo), Date.from(fourteenDaysFromNow)));
        this.add(tempEntity.newWaiver("hash9", securityPolicies.get(1).getId(),
              parentOrganization.getId(),
            null, purl, EXACT_COMPONENT, "comment parent org",
            Date.from(thirtyDaysAgo), Date.from(thirtyDaysFromNow)));
      }};
  }

  private void showAllWaivers() {
    DashboardPage.filterToggle().click();
    DashboardFilters.organizationFilter().twisty().click();
    DashboardFilters.organizationFilter().allItems().click();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(1, 10);
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.apply();
    DashboardFilters.closeButton().click();
  }

  private String exportWaiversCSV() {
    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Waivers Data")).click();
    return new String(responseCopyHandler.consumeResponse());
  }

  private String[] buildExpectedCsvExportDataBySortColumn(List<PolicyWaiver> policyWaivers, String sortByColumn) {
    DateFormat dateFormatCsv = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormatCsv.setTimeZone(TimeZone.getTimeZone("UTC"));

    PolicyWaiver policyWaiver = policyWaivers.get(0);
    PolicyWaiver policyWaiver1 = policyWaivers.get(1);
    PolicyWaiver policyWaiver2 = policyWaivers.get(2);
    PolicyWaiver policyWaiver3 = policyWaivers.get(3);
    PolicyWaiver policyWaiver4 = policyWaivers.get(4);
    PolicyWaiver policyWaiver5 = policyWaivers.get(5);
    PolicyWaiver policyWaiver6 = policyWaivers.get(6);
    PolicyWaiver policyWaiver7 = policyWaivers.get(7);
    PolicyWaiver policyWaiver8 = policyWaivers.get(8);

    String waiver1String = policyWaiver.getId() + ",7," + dateFormatCsv.format(Date.from(twoDaysAgo)) +
        "," + dateFormatCsv.format(Date.from(threeDaysFromNow)) + "," + policyWaiver.getPolicyId() +
        ",Policy 1,,organization," + policyWaiver.getOwnerId() + "," + "Org 1,EXACT_COMPONENT,hash1," +
        "Group1 : Artifact1 : 1.2.3,testuser,Test User,comment org";

    String waiver2String = policyWaiver1.getId() + ",3," + dateFormatCsv.format(Date.from(threeDaysAgo)) + "," +
        dateFormatCsv.format(Date.from(fiveDaysFromNow)) + "," + policyWaiver1.getPolicyId() +
        ",Policy 3,,application," + policyWaiver1.getOwnerId() + ",App 2,EXACT_COMPONENT,hash2," +
        "Group1 : Artifact1 : 1.2.3,testuser,Test User,comment app 2";

    String waiver3String = policyWaiver3.getId() + ",9," + dateFormatCsv.format(Date.from(sixDaysAgo)) +
        "," + dateFormatCsv.format(Date.from(sixDaysFromNow)) + "," + policyWaiver3.getPolicyId() +
        ",Policy 2,,organization," + policyWaiver3.getOwnerId() + ",Org 1,ALL_COMPONENTS,hash4," +
        "Group1 : Artifact1 : 1.2.3,testuser,Test User,org all components";

    String waiver4String = policyWaiver4.getId() + ",9," + dateFormatCsv.format(Date.from(sevenDaysAgo)) + "," +
        dateFormatCsv.format(Date.from(sevenDaysFromNow)) + "," + policyWaiver4.getPolicyId() +
        ",Policy 2,,application," + policyWaiver4.getOwnerId() + ",App 1,ALL_VERSIONS,hash5," +
        "Group1 : Artifact1 : 1.2.3,testuser,Test User,app all versions";

    String waiverRepoString =
        policyWaiver6.getId() + ",7," + dateFormatCsv.format(Date.from(nineDaysAgo)) + "," +
            dateFormatCsv.format(Date.from(nineDaysFromNow)) + "," + policyWaiver6.getPolicyId() +
            ",Policy 1,,repository," + policyWaiver6.getOwnerId() + ",Repository 1,EXACT_COMPONENT,hash7," +
            "Group1 : Artifact1 : 1.2.3,testuser,Test User,comment repo";

    String waiverRepoContainerString =
        policyWaiver7.getId() + ",9," + dateFormatCsv.format(Date.from(fourteenDaysAgo)) + "," +
            dateFormatCsv.format(Date.from(fourteenDaysFromNow)) + "," + policyWaiver7.getPolicyId() +
            ",Policy 2,,all_repositories," + policyWaiver7.getOwnerId() +
            ",All Repositories,EXACT_COMPONENT,hash8," +
            "Group1 : Artifact1 : 1.2.3,testuser,Test User,comment repo container";

    String waiver5String = policyWaiver5.getId() + ",4," + dateFormatCsv.format(Date.from(eightDaysAgo)) + "," +
        dateFormatCsv.format(Date.from(eightDaysFromNow)) + "," + policyWaiver5.getPolicyId() +
        ",Policy 4,,root_organization," + policyWaiver5.getOwnerId() +
        "," + rootOrg.getName() +
        ",EXACT_COMPONENT,hash6,Group1 : Artifact1 : 1.2.3,testuser,Test User,comment root org";

    String waiver6String = policyWaiver2.getId() + ",3," + dateFormatCsv.format(Date.from(fiveDaysAgo)) + ",," +
        policyWaiver2.getPolicyId() + ",Policy 3,,application," + policyWaiver2.getOwnerId() +
        ",App 1,EXACT_COMPONENT,hash3,Group1 : Artifact1 : 1.2.3,testuser,Test User,comment app 1";

    String waiverParentOrgString = policyWaiver8.getId() + ",9," + dateFormatCsv.format(Date.from(thirtyDaysAgo)) +
        "," + dateFormatCsv.format(Date.from(thirtyDaysFromNow)) + "," + policyWaiver8.getPolicyId() +
        ",Policy 2,,organization," + policyWaiver8.getOwnerId() +
        ",Parent Org 1,EXACT_COMPONENT,hash9,Group1 : Artifact1 : 1.2.3,testuser,Test User,comment parent org";

    switch (sortByColumn) {
      case "threat":
        return new String[]{
            waiver3String, waiver4String, waiverRepoContainerString, waiverParentOrgString, waiver1String,
            waiverRepoString, waiver5String, waiver2String, waiver6String
        };
      case "createddate":
        return new String[]{
            waiverParentOrgString, waiverRepoContainerString, waiverRepoString, waiver5String, waiver4String,
            waiver3String, waiver6String, waiver2String, waiver1String
        };
      case "policy":
        return new String[]{
            waiver5String, waiver2String, waiver6String, waiver3String, waiver4String, waiverRepoContainerString,
            waiverParentOrgString, waiver1String, waiverRepoString
        };
      case "scope":
        return new String[]{
            waiverRepoContainerString, waiver4String, waiver6String, waiver2String, waiver1String, waiver3String,
            waiverParentOrgString, waiverRepoString, waiver5String
        };
      case "component":
      default:
        // sort by expiry
        return new String[]{
            waiver1String, waiver2String, waiver3String, waiver4String, waiver5String, waiverRepoString,
            waiverRepoContainerString, waiverParentOrgString, waiver6String
        };
    }
  }
}
