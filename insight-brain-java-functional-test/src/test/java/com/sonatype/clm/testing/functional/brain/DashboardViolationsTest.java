/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardComponentDetails;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.AgeFilter;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationTile;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationsHeaders;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationsResults;
import com.sonatype.clm.testing.functional.pages.ApplicationReportContainerPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.proxy.ResponseCopyHandler;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.google.common.collect.ImmutableMap;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.SEVERE;
import static com.sonatype.clm.testing.functional.pages.DashboardPage.AGE_FILTER_FEATURE_FLAG;
import static com.sonatype.clm.testing.functional.pages.DashboardPage.VIOLATIONS_URL;
import static com.sonatype.clm.testing.functional.utils.BaseUrl.uriBuilder;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DashboardViolationsTest
    extends AbstractFunctionalTest
{
  private static final String NO_DATA_MSG = "No data available in the last 30 days given the applied filters and available permissions.";

  private static final String MAX_RESULTS_MSG = "Newest 100 results shown";

  private static final String CSV_HEADERS = "Threat Level,Policy Name,Application Name,Component Name,Date First Seen,Milliseconds Since First Seen";

  private static final String NEWEST_RISK_URL = uriBuilder().fragment("/dashboard/newest-risk").build().toString();

  private final Date now = new Date();

  private final Date twoDaysAgo = now().minusDays(2).minusHours(4).toDate();

  private final Date oneWeekAgo = now().minusWeeks(1).minusHours(4).toDate();

  private final Date twoMonthsAgo = now().minusMonths(2).minusHours(4).toDate();

  private Application app1, app2;

  private Policy securityPolicy, licensePolicy;

  private PolicyEvaluation buildEvalNow, buildEval2MonthsAgo, releaseEval2DaysAgo, operateEval1WeekAgo;

  private ApplicationComponent buildComponent, releaseComponent, operateComponent;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
  }

  @After
  public void cleanup() {
    clearFilters();
    reverseProxyServer.reset();
  }

  @Before
  public void init() throws IOException {
    app1 = tempEntity.newApplicationWithParent("app1", "Violations Test App1");
    app2 = tempEntity.newApplicationWithParent("app2", "Violations Test App2");
    licensePolicy = tempEntity.newPolicy(app1.getParentOwnerId(), "DashboardViolationsTestLicensePolicy");
    securityPolicy = tempEntity.newPolicy(app2.getParentOwnerId(), "DashboardViolationsTestSecurityPolicy");
    buildEvalNow = tempEntity
        .newPolicyEvaluation(app1.getId(), BuildStageType.ID, "now", now);
    buildEval2MonthsAgo = tempEntity
        .newPolicyEvaluation(app2.getId(), BuildStageType.ID, "now", twoMonthsAgo);
    releaseEval2DaysAgo = tempEntity
        .newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "2dAgo", twoDaysAgo);
    operateEval1WeekAgo = tempEntity
        .newPolicyEvaluation(app1.getId(), OperateStageType.ID, "1yAgo", oneWeekAgo);
    buildComponent = tempEntity
        .newApplicationComponent(app1.getId(), BuildStageType.ID, "g1a1v1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    releaseComponent = tempEntity
        .newApplicationComponent(app2.getId(), ReleaseStageType.ID, randomAlphanumeric(10),
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    operateComponent = tempEntity
        .newApplicationComponent(app1.getId(), OperateStageType.ID, randomAlphanumeric(10),
            ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));

    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportZip = work.getReportFile(buildEvalNow.getApplicationId(), buildEvalNow.getScanId());
    FileUtils.copyURLToFile(getClass().getResource("/canned-reports/small-report.zip"), reportZip);
    refreshOrOpen(DashboardPage.VIOLATIONS_URL);
  }

  @Test
  public void testViolationsTable() {
    ViolationsResults table = DashboardPage.violationsView().results();

    // no results
    refresh();
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));

    // add a few violations
    tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 1,
        LICENSE, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(operateEval1WeekAgo, licensePolicy, 3,
        LICENSE, operateComponent.getComponentIdentifier(), operateComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(releaseEval2DaysAgo, securityPolicy, 10,
        SECURITY, releaseComponent.getComponentIdentifier(), releaseComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 7,
        LICENSE, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);
    tempEntity.newPolicyViolation(buildEval2MonthsAgo, licensePolicy, 7,
        LICENSE, buildComponent.getComponentIdentifier(), buildComponent.getHash(), FailActionType.ID);

    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldNotBe(visible);
    table.violations().shouldHaveSize(3);
    showLowRiskViolations();
    table.violations().shouldHaveSize(4);

    // age filter should not be displayed without url query parameter
    AgeFilter ageFilter = DashboardFilters.ageFilter();
    ageFilter.shouldNotBe(visible);
    refreshOrOpen(VIOLATIONS_URL + AGE_FILTER_FEATURE_FLAG);
    ageFilter.shouldBe(visible).counter().shouldHave(text("past 30 days"));
    ageFilter.twisty().click();
    ageFilter.past90days().click();
    ageFilter.past90days().shouldBe(selected);
    ageFilter.counter().shouldHave(text("past 90 days"));
    DashboardFilters.apply();
    table.violations().shouldHaveSize(5);
    ageFilter.past90days().shouldBe(selected);
    ageFilter.past30days().shouldNotBe(selected).click();
    ageFilter.past90days().shouldNotBe(selected);
    ageFilter.past30days().shouldBe(selected);
    DashboardFilters.apply();
    refreshOrOpen(VIOLATIONS_URL);
    table.violations().shouldHaveSize(4);

    // check the tile details
    ViolationTile firstViolation = table.firstViolation();
    firstViolation.threatBar().shouldHave(SEVERE);
    firstViolation.threatNumber().shouldHave(text("7"));
    firstViolation.policy().shouldHave(text(licensePolicy.getName())).hover();
    DashboardPage.tooltip().shouldBe(visible).shouldHave(text(licensePolicy.getName()));
    firstViolation.application().shouldHave(text(app1.getName())).hover();
    DashboardPage.tooltip().shouldBe(visible).shouldHave(text(app1.getName()));
    firstViolation.component().shouldHave(text("g1 : a1 : v1")).hover();
    DashboardPage.tooltip().shouldBe(visible).shouldHave(text("g1 : a1 : v1"));
    firstViolation.age().shouldHave(text("1min"));

    // check the report link - opens new window
    firstViolation.latestReport().shouldNotBe(DISABLED).shouldHave(text("Build")).click();
    switchToWindow(1);
    waitUntilUrl(ApplicationReportContainerPage.url(app1.getPublicId(), buildEvalNow.getScanId()));
    ApplicationReportContainerPage.getReportTitle()
        .shouldHave(text(app1.getName() + now().toString(" - YYYY-MM-dd -") + " Build Report"));
    WebDriverRunner.getWebDriver().close();
    switchToWindow(0);
    waitUntilUrl(DashboardPage.VIOLATIONS_URL);

    // open component details and back
    DashboardComponentDetails dashboardComponentDetails = new DashboardComponentDetails();
    firstViolation.component().click();
    waitUntilUrl(DashboardComponentDetails.url("g1a1v1"));
    DashboardPage.dashboardContainer().shouldNotBe(visible);
    dashboardComponentDetails.header().shouldHave(text("g1 : a1 : v1"));
    Selenide.navigator.back();
    DashboardPage.dashboardContainer().shouldBe(visible);

    ViolationsHeaders headers = DashboardPage.violationsView().headers();

    // should be sorted by age
    firstViolation.shouldHave(text("1min"));
    table.lastViolation().shouldHave(text("7d"));
    headers.ageHeader().click();
    firstViolation.shouldHave(text("7d"));
    table.lastViolation().shouldHave(text("1min"));

    // sort by threat
    headers.threatHeader().click();
    table.violations().shouldHave(texts("10", "7", "3", "1"));
    headers.threatHeader().click();
    table.violations().shouldHave(texts("1", "3", "7", "10"));

    // sort by licensePolicy name
    headers.policyHeader().click();
    firstViolation.shouldHave(text("DashboardViolationsTestLicensePolicy"));
    table.lastViolation().shouldHave(text("DashboardViolationsTestSecurityPolicy"));
    headers.policyHeader().click();
    firstViolation.shouldHave(text("DashboardViolationsTestSecurityPolicy"));
    table.lastViolation().shouldHave(text("DashboardViolationsTestLicensePolicy"));

    // sort by application name
    headers.applicationHeader().click();
    firstViolation.shouldHave(text("Violations Test App1"));
    table.lastViolation().shouldHave(text("Violations Test App2"));
    headers.applicationHeader().click();
    firstViolation.shouldHave(text("Violations Test App2"));
    table.lastViolation().shouldHave(text("Violations Test App1"));

    // sort by component name
    headers.componentHeader().click();
    firstViolation.shouldHave(text("g1 : a1 : v1"));
    table.lastViolation().shouldHave(text("g3 : a3 : v3"));
    headers.componentHeader().click();
    firstViolation.shouldHave(text("g3 : a3 : v3"));
    table.lastViolation().shouldHave(text("g1 : a1 : v1"));

    // CSV export with no filters
    ResponseCopyHandler responseCopyHandler = new ResponseCopyHandler("/rest/dashboard/export/newestRisks",
        testCLMServer.getCLMServer().getPort());
    reverseProxyServer.addHandler(responseCopyHandler);
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Violations Data")).click();
    DashboardPage.exportResultsLink().shouldNotBe(visible);
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page
    String exportCsv = new String(responseCopyHandler.consumeResponse());
    Map<String, Date> expectedResults = ImmutableMap.of(
        "1,DashboardViolationsTestLicensePolicy,Violations Test App2,g2 : a2 : v2", twoDaysAgo,   //
        "10,DashboardViolationsTestSecurityPolicy,Violations Test App2,g2 : a2 : v2", twoDaysAgo, //
        "3,DashboardViolationsTestLicensePolicy,Violations Test App1,g3 : a3 : v3", oneWeekAgo,   //
        "7,DashboardViolationsTestLicensePolicy,Violations Test App1,g1 : a1 : v1", now           //
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // CSV export - filter out threat level 1
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(2, 10);
    DashboardFilters.apply();
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = ImmutableMap.of(
        "10,DashboardViolationsTestSecurityPolicy,Violations Test App2,g2 : a2 : v2", twoDaysAgo, //
        "3,DashboardViolationsTestLicensePolicy,Violations Test App1,g3 : a3 : v3", oneWeekAgo,   //
        "7,DashboardViolationsTestLicensePolicy,Violations Test App1,g1 : a1 : v1", now           //
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // CSV export - filter out Build violations
    DashboardFilters.stageFilter().twisty().click();
    DashboardFilters.stageFilter().allItems().click();
    DashboardFilters.stageFilter().build().click();
    DashboardFilters.apply();
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = ImmutableMap.of(
        "10,DashboardViolationsTestSecurityPolicy,Violations Test App2,g2 : a2 : v2", twoDaysAgo, //
        "3,DashboardViolationsTestLicensePolicy,Violations Test App1,g3 : a3 : v3", oneWeekAgo    //
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // CSV export - filter out Security policy type violations
    DashboardFilters.policyTypeFilter().twisty().click();
    DashboardFilters.policyTypeFilter().allItems().click();
    DashboardFilters.policyTypeFilter().security().click();
    DashboardFilters.apply();
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = ImmutableMap.of(
        "3,DashboardViolationsTestLicensePolicy,Violations Test App1,g3 : a3 : v3", oneWeekAgo
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // CSV export - filter out App1
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationFilter().allItems().click();
    DashboardFilters.applicationFilter().checkboxItem(2).click();
    DashboardFilters.apply();
    DashboardPage.viewDropdown().click();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    assertEquals("Expected empty export", CSV_HEADERS, exportCsv);
  }

  @Test
  public void testNewestRiskRedirectsToViolations() {
    refreshOrOpen(NEWEST_RISK_URL);
    waitUntilUrl(DashboardPage.VIOLATIONS_URL);
  }

  @Test
  public void testShouldNotShowMaxResultsMessageWhen100Results() {
    createViolations(100, 5, buildEvalNow);
    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);
    DashboardPage.violationsView().results().maxResultsMessage().shouldNotBe(visible);
  }

  @Test
  public void testShouldShowMaxResultsMessageWhen101Results() {
    createViolations(101, 5, buildEvalNow);
    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);
    DashboardPage.violationsView().results().maxResultsMessage().shouldBe(visible).shouldHave(text(MAX_RESULTS_MSG));
  }

  private void assertViolationsCsv(String csv, Map<String, Date> expectedSortedResults) {
    String[] lines = csv.split("\r\n");

    // assert CSV header
    assertEquals(CSV_HEADERS, lines[0]);

    // assert CSV results
    String[] results = Arrays.copyOfRange(lines, 1, lines.length);
    Arrays.sort(results);
    Iterator<Map.Entry<String, Date>> it = expectedSortedResults.entrySet().iterator();
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    for (int i = 0; i < results.length && it.hasNext(); i++) {
      String result = results[i];
      Map.Entry<String, Date> expectedResult = it.next();
      String expectedResultWithoutTimestamps = expectedResult.getKey();
      Date expectedDate = expectedResult.getValue();

      Matcher matcher = Pattern.compile("^(.*),([-T:0-9]+Z),(\\d+)$").matcher(result);
      if (matcher.find()) {
        String actualResultWithoutTimestamps = matcher.group(1);
        String dateFirstSeen = matcher.group(2);
        String dateFirstSeenMillis = matcher.group(3);

        assertEquals(expectedResultWithoutTimestamps, actualResultWithoutTimestamps);
        assertEquals(dateFormat.format(expectedDate), dateFirstSeen);
        assertEquals(expectedDate.getTime(), Long.parseLong(dateFirstSeenMillis));
      } else {
        fail("The CSV line was not in expected format: " + result);
      }
    }
  }

  private void createViolations(int numViolations, int threatLevel, PolicyEvaluation policyEvaluation) {
    for (int i = 1; i <= numViolations; i++) {
      String group = "Group" + i;
      String artifact = "artifact" + i;
      String version = "version" + i;
      String hash = randomAlphanumeric(20);

      tempEntity.newPolicyViolation(policyEvaluation, licensePolicy, threatLevel,
          LICENSE, group, artifact, version, hash, FailActionType.ID);
    }
  }

  private void showLowRiskViolations() {
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(0, 10);
    DashboardFilters.apply();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
  }

  private void clearFilters() {
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    List<DashboardFilter> filters = dashboardFilterDAO.getByUsername("admin");
    for (DashboardFilter filter : filters) {
      dashboardFilterDAO.delete(filter);
    }
  }
}
