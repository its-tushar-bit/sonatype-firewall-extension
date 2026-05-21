/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.List;

import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardApplicationsComponent;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPageAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.InternalRealm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Each test follows a Given/When/Then shape:
 * <ul>
 * <li>{@link #openDashboardApplicationsAsAdmin()} seeds the per-test {@link Organization} +
 * {@link Policy} and lands on the dashboard logged-in as admin.</li>
 * <li>The test body seeds applications via {@link #seedTableTestApps(List)} or
 * {@link #seedPaginationGroups(List)} and exercises the UI via
 * {@link DashboardApplicationsComponent}.</li>
 * <li>{@link #clearFiltersAndResetProxy()} resets per-test state so the next test starts clean.</li>
 * </ul>
 *
 * <p>
 * UI behaviour and snapshot-on-failure logging live in {@link DashboardApplicationsComponent};
 * the per-environment numbers live in
 * {@code src/test/resources/test-data/dashboard-applications.json}.
 */
public class DashboardApplicationsPlaywrightTest
    extends AbstractIqUiTest
{
  // Passed by reference into appsTable.runWithSnapshotOnFailure(log, ...) so failure
  // diagnostics are logged under this test class's category, not the page object's.
  private static final Logger log = LoggerFactory.getLogger(DashboardApplicationsPlaywrightTest.class);

  private static final DashboardApplicationsData DATA =
      TestDataManager.load("dashboard-applications", DashboardApplicationsData.class);

  private DashboardFilterDAO dashboardFilterDAO;

  private Organization org;

  private Policy policy;

  private int componentCounter;

  @Before
  public void openDashboardApplicationsAsAdmin() {
    dashboardFilterDAO = lookup(DashboardFilterDAO.class);
    org = tempEntity.newOrganization(DATA.organizationName());
    policy = tempEntity.newPolicy(org);
    componentCounter = 0;

    playwrightRefreshOrOpen(DashboardPage.urlToApplications());
    playwrightLogin();
  }

  @After
  public void clearFiltersAndResetProxy() {
    dashboardFilterDAO.deleteByUsernameAndRealmId(User.ADMIN_USERNAME, InternalRealm.ID);
    reverseProxyServer.reset();
  }

  @Test
  @Category(SanityTest.class)
  public void testResultsMessageNoData() {
    playwrightRefresh();
    loadDashboard();

    DashboardApplicationsComponent appsTable = new DashboardApplicationsComponent();
    appsTable.waitForNoDataMessage();
    assertThat(appsTable.noDataMessage()).containsText(DATA.noDataMessage());

  }

  @Test
  @Category(SanityTest.class)
  public void testApplicationsTable() {
    seedTableTestApps(DATA.tableTestApps());

    playwrightRefresh();
    loadDashboard();

    DashboardApplicationsComponent appsTable = new DashboardApplicationsComponent();
    appsTable.applyPolicyThreatLevelFilter(DATA.threatLevelRangeMin(), DATA.threatLevelRangeMax());

    appsTable.runWithSnapshotOnFailure(log, "testApplicationsTable.assertions", () -> {
      List<String> defaultOrderNames = DATA.tableTestDefaultSortIds()
          .stream()
          .map(this::appName)
          .toList();
      appsTable.assertDefaultSortOrder(defaultOrderNames);

      appsTable.assertTotalRiskCellContains(DATA.totalRiskRowIndex(), DATA.tableTestExpectedTotalRisk());

      appsTable.assertAllSortColumns(
          appName(DATA.tableTestNameAscFirstId()),
          appName(DATA.tableTestNameAscLastId()),
          appName(DATA.tableTestLowRiskSortId()),
          appName(DATA.tableTestModerateRiskSortId()),
          appName(DATA.tableTestSevereRiskSortId()),
          appName(DATA.tableTestCriticalRiskSortId()));
    });

  }

  @Test
  @Category(SanityTest.class)
  public void testApplicationsTableMultiplePages() {
    int totalApps = seedPaginationGroups(DATA.paginationGroups());

    playwrightRefresh();
    DashboardPage dashboard = loadDashboard();

    DashboardApplicationsComponent appsTable = new DashboardApplicationsComponent();
    appsTable.waitForResults(DATA.paginationWaitForResultsMs());
    appsTable.applyPolicyThreatLevelFilter(DATA.threatLevelRangeMin(), DATA.threatLevelRangeMax());

    appsTable.runWithSnapshotOnFailure(log, "testApplicationsTableMultiplePages.assertions", () -> {
      assertThat(dashboard.dashboardContainer()).isVisible();
      appsTable.walkPaginationFlow(
          DATA.totalRiskRowIndex(),
          DATA.paginationExpectedPage1Risk(),
          DATA.paginationExpectedPage2Risk(),
          DATA.paginationExpectedAscPage1Risk());
    });

  }

  // --------------- DB seeding (test-local) ---------------

  /**
   * Seeds one {@link Application} per entry in {@code apps}, plus one violation per entry in
   * {@link TableTestApp#allViolations()}.
   */
  private void seedTableTestApps(List<TableTestApp> apps) {
    for (TableTestApp appData : apps) {
      Application app = createApp(appData.id());
      for (AppViolation v : appData.allViolations()) {
        createViolation(app, v.stageType(), v.threatLevel());
      }
    }
  }

  /**
   * Seeds {@code sum(group.count)} apps total, in order, each with one violation matching the
   * stage/threat of its group.
   *
   * @return total number of apps seeded
   */
  private int seedPaginationGroups(List<PaginationGroup> groups) {
    int appIndex = 0;
    for (PaginationGroup group : groups) {
      for (int i = 0; i < group.count(); i++) {
        createViolation(createApp(String.valueOf(appIndex++)), group.stageType(), group.threatLevel());
      }
    }
    return appIndex;
  }

  private String appName(String id) {
    return DATA.appNamePrefix() + id;
  }

  private Application createApp(String id) {
    return tempEntity.newApplication(appName(id), id, org.getId());
  }

  private PolicyViolation createViolation(Application app, String stageType, int threatLevel) {
    String scanId = app.getName() + stageType;
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), stageType, scanId);
    int idx = componentCounter++;
    return tempEntity.newPolicyViolation(evaluation, policy, threatLevel,
        PolicyThreatCategory.LICENSE,
        "Group" + idx, "Artifact" + idx, "Version" + idx, "hash" + idx,
        FailActionType.ID);
  }

  // --------------- helpers ---------------

  private DashboardPage loadDashboard() {
    DashboardPage dashboard = new DashboardPage();
    new DashboardPageAssertions(dashboard).shouldBeLoaded();
    assertThat(dashboard.dashboardContainer()).isVisible();
    return dashboard;
  }

  // --------------- Test-local nested types ---------------

  public record AppViolation(String stageType, int threatLevel)
  {
  }

  public record TableTestApp(String id, String stageType, Integer threatLevel, List<AppViolation> violations)
  {
    public List<AppViolation> allViolations() {
      return violations != null ? violations : List.of(new AppViolation(stageType, threatLevel));
    }
  }

  public record PaginationGroup(int count, String stageType, int threatLevel)
  {
  }

  /**
   * Typed view of {@code src/test/resources/test-data/dashboard-applications.json}.
   */
  public record DashboardApplicationsData(
      String organizationName,
      String appNamePrefix,
      String noDataMessage,
      List<TableTestApp> tableTestApps,
      int tableTestExpectedCount,
      String tableTestExpectedTotalRisk,
      List<String> tableTestDefaultSortIds,
      String tableTestNameAscFirstId,
      String tableTestNameAscLastId,
      String tableTestLowRiskSortId,
      String tableTestModerateRiskSortId,
      String tableTestSevereRiskSortId,
      String tableTestCriticalRiskSortId,
      int threatLevelRangeMin,
      int threatLevelRangeMax,
      int totalRiskRowIndex,
      List<PaginationGroup> paginationGroups,
      int paginationWaitForResultsMs,
      String paginationExpectedPage1Risk,
      String paginationExpectedPage2Risk,
      String paginationExpectedAscPage1Risk)
  {
  }
}
