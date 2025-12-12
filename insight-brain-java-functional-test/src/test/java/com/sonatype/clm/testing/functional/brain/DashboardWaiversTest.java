/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiverResultsPaginator;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiverTile;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiversHeaders;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiversResults;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.WaiverDetailsPage;
import com.sonatype.clm.testing.functional.utils.proxy.ResponseCopyHandler;
import com.sonatype.insight.brain.dashboard.DashboardPolicyWaiverDTO;
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

public class DashboardWaiversTest
    extends AbstractFunctionalTest
{
  private Organization rootOrg;

  private Organization organization;

  private Organization organization2;

  private Organization parentOrganization;

  private Application application;

  private Application application2;

  private Application application3;

  private Repository repository1;

  private ArrayList<PolicyWaiver> policyWaivers;

  private static final String CSV_HEADERS = "Waiver Id, Threat level, Created Date, Expiration Date," +
      " Policy Id, Policy Name, Policy Constraints, Scope Type, Scope Id, Scope Name," +
      " Component Match Strategy, Component Hash, Component Name, Upgrade, Created by Id, Created by Name,Comment, " +
      "Is Auto Waiver, Is Expire When Remediation Available Waiver, Waiver Reason Id, Waiver Reason Text";

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

  private OrganizationDAO organizationDAO;

  private static final String NO_DATA_MSG =
      "No data available in the last 30 days given the applied filters and permissions.";

  private static ResponseCopyHandler responseCopyHandler;

  private static final WaiversResults table = DashboardPage.waiversView().results();

  private static final WaiversHeaders headers = DashboardPage.waiversView().headers();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    loginAsAdmin();
    DashboardPage.waitUntilSpinnersGone();

    responseCopyHandler = new ResponseCopyHandler("/rest/dashboard/export/policyWaivers",
        testCLMServer.getCLMServer().getPort());
    reverseProxyServer.addHandler(responseCopyHandler);

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
  public void testWaiversTable_loadsAllAutoWaiversWithoutFilters() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    policyWaivers = createAutoWaivers();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.dashboardContainer().shouldBe(visible);
    table.waivers().shouldHave(size(9));

    headers.dateHeader().click();
    headers.dateHeader().sortArrows().shouldBeUp();
    headers.dateHeader().click();
    headers.dateHeader().sortArrows().shouldBeDown();

    // check the tile details
    WaiverTile waiver1 = table.firstWaiver();
    waiver1.threatIndicator().shouldHave(SEVERE);
    waiver1.threatNumber().shouldHave(text("7"));
    waiver1.createTime().shouldHave(text(dateFormat.format(Date.from(twoDaysAgo))));
    if (waiver1.policy() != null) {
      waiver1.policy().shouldHave(
          text("Policy 1")
      );
    }
    waiver1.scope().shouldHave(text("Organization - Org 1"));
    if (waiver1.component() != null) {
      waiver1.component().shouldHave(
          text("Group1 : Artifact1 : 1.2.3")
      );
    }
    waiver1.upgradeAvailable().shouldHave(text("—"));

    WaiverTile waiver2 = table.waiver(1);
    waiver2.threatIndicator().shouldHave(SEVERE);
    waiver2.threatNumber().shouldHave(text("7"));
    waiver2.createTime().shouldHave(text(dateFormat.format(Date.from(threeDaysAgo))));
    if (waiver2.policy() != null) {
      waiver2.policy().shouldHave(
          text("Policy 1")
      );
    }
    waiver2.scope().shouldHave(text("Application - App 2"));
    if (waiver2.component() != null) {
      waiver2.component().shouldHave(
          text("Group1 : Artifact1 : 1.2.3")
      );
    }
    waiver2.upgradeAvailable().shouldHave(text("—"));

    WaiverTile waiver3 = table.waiver(2);
    waiver3.threatIndicator().shouldHave(SEVERE);
    waiver3.threatNumber().shouldHave(text("7"));
    waiver3.createTime().shouldHave(text(dateFormat.format(Date.from(fiveDaysAgo))));
    if (waiver3.policy() != null) {
      waiver3.policy().shouldHave(
          text("Policy 1")
      );
    }
    waiver3.scope().shouldHave(text("Application - App 1"));
    if (waiver3.component() != null) {
      waiver3.component().shouldHave(
          text("Group1 : Artifact1 : 1.2.3")
      );
    }
    waiver3.upgradeAvailable().shouldHave(text("—"));

    WaiverTile waiver4 = table.waiver(3);
    waiver4.threatIndicator().shouldHave(SEVERE);
    waiver4.threatNumber().shouldHave(text("7"));
    waiver4.createTime().shouldHave(text(dateFormat.format(Date.from(sixDaysAgo))));
    if (waiver4.policy() != null) {
      waiver4.policy().shouldHave(
          text("Policy 1")
      );
    }
    waiver4.scope().shouldHave(text("Organization - Org 2"));
    if (waiver4.component() != null) {
      waiver4.component().shouldHave(
          text("Group1 : Artifact1 : 1.2.3")
      );
    }
    waiver4.upgradeAvailable().shouldHave(text("—"));

    WaiverTile waiver6 = table.waiver(4);
    waiver6.threatIndicator().shouldHave(SEVERE);
    waiver6.threatNumber().shouldHave(text("7"));
    waiver6.createTime().shouldHave(text(dateFormat.format(Date.from(sevenDaysAgo))));
    if (waiver6.policy() != null) {
      waiver6.policy().shouldHave(
          text("Policy 1")
      );
    }
    waiver6.scope().shouldHave(text("Application - App 3"));
    if (waiver6.component() != null) {
      waiver6.component().shouldHave(
          text("Group1 : Artifact1 : 1.2.3")
      );
    }
    waiver6.upgradeAvailable().shouldHave(text("—"));

    WaiverTile repositoryWaiver = table.waiver(5);
    repositoryWaiver.threatIndicator().shouldHave(SEVERE);
    repositoryWaiver.threatNumber().shouldHave(text("7"));
    repositoryWaiver.createTime().shouldHave(text(dateFormat.format(Date.from(eightDaysAgo))));
    if (repositoryWaiver.policy() != null) {
      repositoryWaiver.policy().shouldHave(
          text("Policy 1")
      );
    }
    repositoryWaiver.scope().shouldHave(text("Root Organization"));
    if (repositoryWaiver.component() != null) {
      repositoryWaiver.component().shouldHave(
          text("Group1 : Artifact1 : 1.2.3")
      );
    }
    repositoryWaiver.upgradeAvailable().shouldHave(text("—"));

    WaiverTile repositoryContainerWaiver = table.waiver(6);
    repositoryContainerWaiver.threatIndicator().shouldHave(SEVERE);
    repositoryContainerWaiver.threatNumber().shouldHave(text("7"));
    repositoryContainerWaiver.createTime().shouldHave(text(dateFormat.format(Date.from(nineDaysAgo))));
    if (repositoryContainerWaiver.policy() != null) {
      repositoryContainerWaiver.policy().shouldHave(
          text("Policy 1")
      );
    }
    repositoryContainerWaiver.scope().shouldHave(text("Repository - Repository 1"));
    if (repositoryContainerWaiver.component() != null) {
      repositoryContainerWaiver.component().shouldHave(
          text("Group1 : Artifact1 : 1.2.3")
      );
    }
    repositoryContainerWaiver.upgradeAvailable().shouldHave(text("—"));

    WaiverTile waiverParentOrg = table.waiver(7);
    waiverParentOrg.threatIndicator().shouldHave(SEVERE);
    waiverParentOrg.threatNumber().shouldHave(text("7"));
    waiverParentOrg.createTime().shouldHave(text(dateFormat.format(Date.from(fourteenDaysAgo))));
    if (waiverParentOrg.policy() != null) {
      waiverParentOrg.policy().shouldHave(
          text("Policy 1")
      );
    }
    waiverParentOrg.scope().shouldHave(text("Repository Managers"));
    if (waiverParentOrg.component() != null) {
      waiverParentOrg.component().shouldHave(
          text("Group1 : Artifact1 : 1.2.3")
      );
    }

    WaiverTile waiver5 = table.waiver(8);
    waiver5.threatIndicator().shouldHave(SEVERE);
    waiver5.threatNumber().shouldHave(text("7"));
    waiver5.createTime().shouldHave(text(dateFormat.format(Date.from(thirtyDaysAgo))));
    if (waiver5.policy() != null) {
      waiver5.policy().shouldHave(
          text("Policy 1")
      );
    }
    waiver5.scope().shouldHave(text("Organization - Parent Org 1"));
    if (waiver5.component() != null) {
      waiver5.component().shouldHave(
          text("Group1 : Artifact1 : 1.2.3")
      );
    }
    waiver5.upgradeAvailable().shouldHave(text("—"));
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
  public void testWaiversTable_defaultCsvExport() {
    // checks csv export when no filters are selected
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    policyWaivers = createWaivers();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

    String exportCsvData = exportWaiversCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    // by default the waivers are ordered by expiry
    String[] expectedResults = buildExpectedCsvExportDataBySortColumn(policyWaivers, "expiry");
    assertWaiversCsv(exportCsvData, expectedResults);
  }

  @Test
  public void testAutoWaiversTable_defaultCsvExport() {
    // checks csv export when no filters are selected
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    policyWaivers = createAutoWaivers();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

    headers.dateHeader().click();

    String exportCsvData = exportWaiversCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    String[] expectedResults = buildExpectedCsvExportDataBySortColumnForAutoWaivers(policyWaivers, "createddate");
    assertWaiversCsv(exportCsvData, expectedResults);
  }

  @Test
  public void testWaiversTable_sortByThreat() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    policyWaivers = createWaivers();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

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
    DashboardPage.waitUntilSpinnersGone();

    policyWaivers = createWaivers();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

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
  public void testAutoWaiversTable_sortByCreatedDate() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    policyWaivers = createAutoWaivers();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

    // sort by creation date
    headers.dateHeader().click();

    String exportCsvData = exportWaiversCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    // assert table
    table.firstWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(thirtyDaysAgo))));
    table.lastWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(twoDaysAgo))));

    // assert csv export
    String[] expectedResults = buildExpectedCsvExportDataBySortColumnForAutoWaivers(policyWaivers, "createddate");
    assertWaiversCsv(exportCsvData, expectedResults);
  }

  @Test
  public void testWaiversTable_sortByPolicy() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    policyWaivers = createWaivers();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

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
    DashboardPage.waitUntilSpinnersGone();

    policyWaivers = createWaivers();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

    // sort by scope
    headers.scopeHeader().click();

    table.firstWaiver().scope().shouldHave(text("Application - App 1"));
    table.lastWaiver().scope().shouldHave(text("Root Organization"));

    // assert csv export
    String exportCsvData = exportWaiversCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    String[] expectedResults = buildExpectedCsvExportDataBySortColumn(policyWaivers, "scope");
    assertWaiversCsv(exportCsvData, expectedResults);
  }

  @Test
  public void testAutoWaiversTable_sortByScope() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    policyWaivers = createAutoWaivers();
    refresh();
    DashboardPage.waitUntilSpinnersGone();

    // sort by scope
    headers.scopeHeader().click();

    table.firstWaiver().scope().shouldHave(text("Application - App 1"));
    table.lastWaiver().scope().shouldHave(text("Root Organization"));

    // assert csv export
    String exportCsvData = exportWaiversCSV();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page

    String[] expectedResults = buildExpectedCsvExportDataBySortColumnForAutoWaivers(policyWaivers, "scope");
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
    DashboardPage.waitUntilSpinnersGone();

    showAllWaivers();
    table.waivers().shouldHave(size(100));

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
    DashboardPage.waitUntilSpinnersGone();

    showAllWaivers();
    table.waivers().shouldHave(size(100));

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
    DashboardPage.waitUntilSpinnersGone();

    showAllWaivers();
    table.waivers().shouldHave(size(100));

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
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Policy " + i,
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
    DashboardPage.waitUntilSpinnersGone();

    showAllWaivers();
    table.waivers().shouldHave(size(100));

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
    DashboardPage.waitUntilSpinnersGone();

    showAllWaivers();
    table.waivers().shouldHave(size(100));

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

  @Test
  public void testWaiversTabShowsReasonsFilter() {
    refreshOrOpen(DashboardPage.urlToWaivers());

    DashboardPage.expandFilter();
    DashboardFilters.filterContainer().shouldBe(visible);
    DashboardFilters.iqPolicyWaiverReasonFilter().shouldBe(visible);
    final var dashboardReasonsFilter = DashboardFilters.iqPolicyWaiverReasonFilter();
    dashboardReasonsFilter.click();

    final var labels = dashboardReasonsFilter.getLabels()
        .stream()
        .map(label -> label.getText().trim())
        .toList();

    assertThat(labels).containsExactly(
        "all/none",
        "Acknowledged violation",
        "Evaluating component",
        "Mitigated externally",
        "No upgrade path",
        "Not exploitable",
        "Not reachable",
        "Researching",
        "Other",
        "(No reason provided)");
  }

  private ArrayList<PolicyWaiver> createAutoWaivers() {
    parentOrganization = tempEntity.newOrganization("Parent Org 1");
    organization = tempEntity.newOrganization("Org 1", parentOrganization);
    organization2 = tempEntity.newOrganization("Org 2", parentOrganization);
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    application2 = tempEntity.newApplication("App 2", "app2", organization.getId());
    application3 = tempEntity.newApplication("App 3", "app3", organization.getId());
    repository1 = tempEntity.newRepository("Repository 1");

    ArrayList<Policy> securityPolicies = new ArrayList<>() {{
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7));
      }};

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(),
            StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    tempEntity.newPolicyViolation(policyEvaluation1, securityPolicies.get(0), "Group1",
            "Artifact1", "Version1", "hash1", "sonatype-2017-0507");

    TreeMap<String, String> coordinates = new TreeMap<>() {{
        this.put("artifactId", "Artifact1");
        this.put("groupId", "Group1");
        this.put("version", "1.2.3");
      }};

    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    
    PolicyWaiver policyWaiver1 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash1")
        .setPolicyId(securityPolicies.get(0).getId())
        .setOwnerId(organization.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(null)
        .setComment(null)
        .setCreateTime(Date.from(twoDaysAgo))
        .setExpiryTime(null)
        .setCreatorId(null)
        .setCreatorName(null)
        .setComponentUpgradeAvailable(null));

    PolicyWaiver policyWaiver2 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash2")
        .setPolicyId(securityPolicies.get(0).getId())
        .setOwnerId(application2.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(null)
        .setComment(null)
        .setCreateTime(Date.from(threeDaysAgo))
        .setExpiryTime(null)
        .setCreatorId(null)
        .setCreatorName(null)
        .setComponentUpgradeAvailable(null));

    PolicyWaiver policyWaiver3 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash3")
        .setPolicyId(securityPolicies.get(0).getId())
        .setOwnerId(application.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(null)
        .setComment(null)
        .setCreateTime(Date.from(fiveDaysAgo))
        .setExpiryTime(null)
        .setCreatorId(null)
        .setCreatorName(null)
        .setComponentUpgradeAvailable(null));

    PolicyWaiver policyWaiver4 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash4")
        .setPolicyId(securityPolicies.get(0).getId())
        .setOwnerId(organization2.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(null)
        .setComment(null)
        .setCreateTime(Date.from(sixDaysAgo))
        .setExpiryTime(null)
        .setCreatorId(null)
        .setCreatorName(null)
        .setComponentUpgradeAvailable(null));

    PolicyWaiver policyWaiver5 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash5")
        .setPolicyId(securityPolicies.get(0).getId())
        .setOwnerId(application3.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(null)
        .setComment(null)
        .setCreateTime(Date.from(sevenDaysAgo))
        .setExpiryTime(null)
        .setCreatorId(null)
        .setCreatorName(null)
        .setComponentUpgradeAvailable(null));

    PolicyWaiver policyWaiver6 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash6")
        .setPolicyId(securityPolicies.get(0).getId())
        .setOwnerId(rootOrg.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(null)
        .setComment(null)
        .setCreateTime(Date.from(eightDaysAgo))
        .setExpiryTime(null)
        .setCreatorId(null)
        .setCreatorName(null)
        .setComponentUpgradeAvailable(null));

    PolicyWaiver policyWaiver7 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash7")
        .setPolicyId(securityPolicies.get(0).getId())
        .setOwnerId(repository1.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(null)
        .setComment(null)
        .setCreateTime(Date.from(nineDaysAgo))
        .setExpiryTime(null)
        .setCreatorId(null)
        .setCreatorName(null)
        .setComponentUpgradeAvailable(null));

    PolicyWaiver policyWaiver8 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash8")
        .setPolicyId(securityPolicies.get(0).getId())
        .setOwnerId(RepositoryContainer.REPOSITORY_CONTAINER_ID)
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(null)
        .setComment(null)
        .setCreateTime(Date.from(fourteenDaysAgo))
        .setExpiryTime(null)
        .setCreatorId(null)
        .setCreatorName(null)
        .setComponentUpgradeAvailable(null));

    PolicyWaiver policyWaiver9 = tempEntity.newWaiver(new PolicyWaiver()
        .setHash("hash9")
        .setPolicyId(securityPolicies.get(0).getId())
        .setOwnerId(parentOrganization.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(null)
        .setComment(null)
        .setCreateTime(Date.from(thirtyDaysAgo))
        .setExpiryTime(null)
        .setCreatorId(null)
        .setCreatorName(null)
        .setComponentUpgradeAvailable(null));

    return new ArrayList<>(
        Arrays.asList(policyWaiver1, policyWaiver2, policyWaiver3, policyWaiver4, policyWaiver5, policyWaiver6,
            policyWaiver7, policyWaiver8, policyWaiver9));
  }

  private ArrayList<PolicyWaiver> createWaivers() {
    parentOrganization = tempEntity.newOrganization("Parent Org 1");
    organization = tempEntity.newOrganization("Org 1", parentOrganization);
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    application2 = tempEntity.newApplication("App 2", "app2", organization.getId());
    repository1 = tempEntity.newRepository("Repository 1");

    ArrayList<Policy> securityPolicies = new ArrayList<>() {{
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
    TreeMap<String, String> coordinates = new TreeMap<>() {{
        this.put("artifactId", "Artifact1");
        this.put("groupId", "Group1");
        this.put("version", "1.2.3");
      }};

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
        "Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver.isComponentUpgradeAvailable()) +
        ",testuser,Test User,comment org,false,false,,";

    String waiver2String = policyWaiver1.getId() + ",3," + dateFormatCsv.format(Date.from(threeDaysAgo)) + "," +
        dateFormatCsv.format(Date.from(fiveDaysFromNow)) + "," + policyWaiver1.getPolicyId() +
        ",Policy 3,,application," + policyWaiver1.getOwnerId() + ",App 2,EXACT_COMPONENT,hash2," +
        "Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver1.isComponentUpgradeAvailable()) +
        ",testuser,Test User,comment app 2,false,false,,";

    String waiver3String = policyWaiver3.getId() + ",9," + dateFormatCsv.format(Date.from(sixDaysAgo)) +
        "," + dateFormatCsv.format(Date.from(sixDaysFromNow)) + "," + policyWaiver3.getPolicyId() +
        ",Policy 2,,organization," + policyWaiver3.getOwnerId() + ",Org 1,ALL_COMPONENTS,hash4," +
        "Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver3.isComponentUpgradeAvailable()) +
        ",testuser,Test User,org all components,false,false,,";

    String waiver4String = policyWaiver4.getId() + ",9," + dateFormatCsv.format(Date.from(sevenDaysAgo)) + "," +
        dateFormatCsv.format(Date.from(sevenDaysFromNow)) + "," + policyWaiver4.getPolicyId() +
        ",Policy 2,,application," + policyWaiver4.getOwnerId() + ",App 1,ALL_VERSIONS,hash5," +
        "Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver4.isComponentUpgradeAvailable()) +
        ",testuser,Test User,app all versions,false,false,,";

    String waiverRepoString =
        policyWaiver6.getId() + ",7," + dateFormatCsv.format(Date.from(nineDaysAgo)) + "," +
            dateFormatCsv.format(Date.from(nineDaysFromNow)) + "," + policyWaiver6.getPolicyId() +
            ",Policy 1,,repository," + policyWaiver6.getOwnerId() + ",Repository 1,EXACT_COMPONENT,hash7," +
            "Group1 : Artifact1 : 1.2.3," +
            DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
                policyWaiver6.isComponentUpgradeAvailable()) +
            ",testuser,Test User,comment repo,false,false,,";

    String waiverRepoContainerString =
        policyWaiver7.getId() + ",9," + dateFormatCsv.format(Date.from(fourteenDaysAgo)) + "," +
            dateFormatCsv.format(Date.from(fourteenDaysFromNow)) + "," + policyWaiver7.getPolicyId() +
            ",Policy 2,,all_repositories," + policyWaiver7.getOwnerId() +
            ",Repository Managers,EXACT_COMPONENT,hash8," +
            "Group1 : Artifact1 : 1.2.3," +
            DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
                policyWaiver7.isComponentUpgradeAvailable()) +
            ",testuser,Test User,comment repo container,false,false,,";

    String waiver5String = policyWaiver5.getId() + ",4," + dateFormatCsv.format(Date.from(eightDaysAgo)) + "," +
        dateFormatCsv.format(Date.from(eightDaysFromNow)) + "," + policyWaiver5.getPolicyId() +
        ",Policy 4,,root_organization," + policyWaiver5.getOwnerId() +
        "," + rootOrg.getName() + ",EXACT_COMPONENT,hash6,Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver5.isComponentUpgradeAvailable()) +
        ",testuser,Test User,comment root org,false,false,,";

    String waiver6String = policyWaiver2.getId() + ",3," + dateFormatCsv.format(Date.from(fiveDaysAgo)) + ",," +
        policyWaiver2.getPolicyId() + ",Policy 3,,application," + policyWaiver2.getOwnerId() +
        ",App 1,EXACT_COMPONENT,hash3,Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver2.isComponentUpgradeAvailable()) +
        ",testuser,Test User,comment app 1,false,false,,";

    String waiverParentOrgString = policyWaiver8.getId() + ",9," + dateFormatCsv.format(Date.from(thirtyDaysAgo)) +
        "," + dateFormatCsv.format(Date.from(thirtyDaysFromNow)) + "," + policyWaiver8.getPolicyId() +
        ",Policy 2,,organization," + policyWaiver8.getOwnerId() +
        ",Parent Org 1,EXACT_COMPONENT,hash9,Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver8.isComponentUpgradeAvailable()) + ",testuser,Test User,comment parent org,false,false,,";

    switch (sortByColumn) {
      case "threat":
        return new String[]{
            waiver3String,                // threat=9, created=sixDaysAgo (NEWEST threat-9)
            waiver4String,                // threat=9, created=sevenDaysAgo
            waiverRepoContainerString,    // threat=9, created=fourteenDaysAgo
            waiverParentOrgString,        // threat=9, created=thirtyDaysAgo (OLDEST threat-9)
            waiver1String,                // threat=7, created=twoDaysAgo (NEWEST threat-7)
            waiverRepoString,             // threat=7, created=nineDaysAgo (OLDEST threat-7)
            waiver5String,                // threat=4, created=eightDaysAgo
            waiver2String,                // threat=3, created=threeDaysAgo (NEWEST threat-3)
            waiver6String
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

  private String[] buildExpectedCsvExportDataBySortColumnForAutoWaivers(
      List<PolicyWaiver> policyWaivers, String sortByColumn)
  {
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
        ",," + policyWaiver.getPolicyId() +
        ",Policy 1,,organization," + policyWaiver.getOwnerId() + "," + "Org 1,EXACT_COMPONENT,hash1," +
        "Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver.isComponentUpgradeAvailable()) +
        ",,,,false,false,,";

    String waiver2String = policyWaiver1.getId() + ",7," + dateFormatCsv.format(Date.from(threeDaysAgo)) + ",,"
        + policyWaiver1.getPolicyId() +
        ",Policy 1,,application," + policyWaiver1.getOwnerId() + ",App 2,EXACT_COMPONENT,hash2," +
        "Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver1.isComponentUpgradeAvailable()) +
        ",,,,false,false,,";

    String waiver3String = policyWaiver3.getId() + ",7," + dateFormatCsv.format(Date.from(sixDaysAgo)) +
        ",," + policyWaiver3.getPolicyId() +
        ",Policy 1,,organization," + policyWaiver3.getOwnerId() + ",Org 2,EXACT_COMPONENT,hash4," +
        "Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver3.isComponentUpgradeAvailable()) +
        ",,,,false,false,,";

    String waiver4String = policyWaiver4.getId() + ",7," + dateFormatCsv.format(Date.from(sevenDaysAgo)) + ",,"
        + policyWaiver4.getPolicyId() +
        ",Policy 1,,application," + policyWaiver4.getOwnerId() + ",App 3,EXACT_COMPONENT,hash5," +
        "Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver4.isComponentUpgradeAvailable()) +
        ",,,,false,false,,";

    String waiverRepoString =
        policyWaiver6.getId() + ",7," + dateFormatCsv.format(Date.from(nineDaysAgo)) + ",,"
            + policyWaiver6.getPolicyId() +
            ",Policy 1,,repository," + policyWaiver6.getOwnerId() + ",Repository 1,EXACT_COMPONENT,hash7," +
            "Group1 : Artifact1 : 1.2.3," +
            DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
                policyWaiver6.isComponentUpgradeAvailable()) +
            ",,,,false,false,,";

    String waiverRepoContainerString =
        policyWaiver7.getId() + ",7," + dateFormatCsv.format(Date.from(fourteenDaysAgo)) + ",,"
            + policyWaiver7.getPolicyId() +
            ",Policy 1,,all_repositories," + policyWaiver7.getOwnerId() +
            ",Repository Managers,EXACT_COMPONENT,hash8," +
            "Group1 : Artifact1 : 1.2.3," +
            DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
                policyWaiver7.isComponentUpgradeAvailable()) +
            ",,,,false,false,,";

    String waiver5String = policyWaiver5.getId() + ",7," + dateFormatCsv.format(Date.from(eightDaysAgo)) + ",,"
        + policyWaiver5.getPolicyId() +
        ",Policy 1,,root_organization," + policyWaiver5.getOwnerId() +
        "," + rootOrg.getName() + ",EXACT_COMPONENT,hash6,Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver5.isComponentUpgradeAvailable()) +
        ",,,,false,false,,";

    String waiver6String = policyWaiver2.getId() + ",7," + dateFormatCsv.format(Date.from(fiveDaysAgo)) + ",," +
        policyWaiver2.getPolicyId() + ",Policy 1,,application," + policyWaiver2.getOwnerId() +
        ",App 1,EXACT_COMPONENT,hash3,Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver2.isComponentUpgradeAvailable()) +
        ",,,,false,false,,";

    String waiverParentOrgString = policyWaiver8.getId() + ",7," + dateFormatCsv.format(Date.from(thirtyDaysAgo)) +
        ",," + policyWaiver8.getPolicyId() +
        ",Policy 1,,organization," + policyWaiver8.getOwnerId() +
        ",Parent Org 1,EXACT_COMPONENT,hash9,Group1 : Artifact1 : 1.2.3," +
        DashboardPolicyWaiverDTO.getComponentUpgradeAvailableValueCSVExport(
            policyWaiver8.isComponentUpgradeAvailable()) + ",,,,false,false,,";

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
            waiverRepoContainerString, waiver6String, waiver2String, waiver4String, waiver1String, waiver3String,
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

  @Test
  public void testMoreThanOnePage() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    createWaiversForPagination();

    refresh();
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.dashboardContainer().shouldBe(visible);

    WaiverResultsPaginator paginator = DashboardPage.waiversView().paginator();

    //DashboardPage.waiversView().paginationButtons().shouldHave(size(2));
    paginator.buttonBar().shouldBe(visible);
    paginator.nextPageButton().shouldBe(visible);
    paginator.previousPageButton().shouldBe(hidden);
    table.waivers().shouldHave(size(100));
    table.firstWaiver().policy().shouldHave(text("Policy 1"));
    table.firstWaiver().threatNumber().shouldHave(text("7"));

    // click next page
    paginator.nextPageButton().click();
    newFluentWait();
    paginator.nextPageButton().shouldBe(hidden);
    paginator.previousPageButton().shouldBe(visible);
    table.waivers().shouldHave(size(50));
    table.firstWaiver().policy().shouldHave(text("Policy 1"));
    table.firstWaiver().threatNumber().shouldHave(text("7"));

    //Click previous page
    paginator.previousPageButton().click();
    newFluentWait();
    paginator.nextPageButton().shouldBe(visible);
    paginator.previousPageButton().shouldBe(hidden);
    table.waivers().shouldHave(size(100));
    table.firstWaiver().policy().shouldHave(text("Policy 1"));
    table.firstWaiver().threatNumber().shouldHave(text("7"));
  }

  private void newFluentWait() {
    new FluentWait<>(getWebDriver())
        .withTimeout(Duration.ofSeconds(240))
        .pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class)
        .until(ExpectedConditions.visibilityOf(table.firstWaiver().policy()));
  }

  private void createWaiversForPagination() {
    parentOrganization = tempEntity.newOrganization("Parent Org 1");
    organization = tempEntity.newOrganization("Org 1", parentOrganization);
    application = tempEntity.newApplication("App 1", "app1", organization.getId());

    Policy securityPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    tempEntity.newPolicyViolation(policyEvaluation, securityPolicy, "Group1",
        "Artifact1", "Version1", "hash1", "sonatype-2017-0507");

    // Component identifier for waivers
    TreeMap<String, String> coordinates = new TreeMap<>() {{
        this.put("artifactId", "Artifact1");
        this.put("groupId", "Group1");
        this.put("version", "1.2.3");
      }};

    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    for (int i = 0; i < 150; i++) {
      tempEntity.newWaiver(new PolicyWaiver()
          .setHash("hash1" + i)
          .setPolicyId(securityPolicy.getId())
          .setOwnerId(organization.getId())
          .setAssociatedPackageUrl(purl)
          .setComponentMatchStrategy(EXACT_COMPONENT)
          .setComment("comment org")
          .setCreateTime(Date.from(twoDaysAgo))
          .setExpiryTime(Date.from(threeDaysFromNow))
          .setCreatorId("testuser1")
          .setCreatorName("Test User1")
          .setComponentUpgradeAvailable(true));
    }
  }
}
