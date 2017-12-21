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
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.security.User;
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
  private static final String NO_DATA_MSG = "No data available in the last 30 days given the applied filters and permissions.";

  private static final String MAX_RESULTS_MSG = "First 100 results shown";

  private static final String CSV_HEADERS = "Threat Level,Policy Name,Application Name,Component Name,Date First Seen,Timestamp First Seen";

  private static final String NEWEST_RISK_URL = uriBuilder().fragment("/dashboard/newest-risk").build().toString();

  private static final ViolationsHeaders headers = DashboardPage.violationsView().headers();

  private static final ViolationsResults table = DashboardPage.violationsView().results();

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
    app1 = tempEntity.newApplicationWithParent("app1", "DVT App1");
    app2 = tempEntity.newApplicationWithParent("app2", "DVT App2 With A Long Name Just To Force Overflow");
    licensePolicy = tempEntity.newPolicy(app1.getParentOwnerId(), "DVTLicensePolicy");
    securityPolicy = tempEntity.newPolicy(app2.getParentOwnerId(), "DVTSecurityPolicyWithAnotherUnnecessarilyLongName");
    buildEvalNow = tempEntity
        .newPolicyEvaluation(app1.getId(), BuildStageType.ID, "now", now);
    buildEval2MonthsAgo = tempEntity
        .newPolicyEvaluation(app2.getId(), BuildStageType.ID, "2mAgo", twoMonthsAgo);
    releaseEval2DaysAgo = tempEntity
        .newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "2dAgo", twoDaysAgo);
    operateEval1WeekAgo = tempEntity
        .newPolicyEvaluation(app1.getId(), OperateStageType.ID, "1yAgo", oneWeekAgo);
    buildComponent = tempEntity
        .newApplicationComponent(app1.getId(), BuildStageType.ID, "g1a1v1",
            ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    releaseComponent = tempEntity
        .newApplicationComponent(app2.getId(), ReleaseStageType.ID, randomAlphanumeric(10),
            ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890"));
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

    // check the tile details - tooltips should not show for short names
    ViolationTile firstViolation = table.firstViolation();
    firstViolation.threatBar().shouldHave(SEVERE);
    firstViolation.threatNumber().shouldHave(text("7"));
    firstViolation.policy().shouldHave(text(licensePolicy.getName())).hover();
    DashboardPage.tooltip().shouldNotBe(visible);
    firstViolation.application().shouldHave(text(app1.getName())).hover();
    DashboardPage.tooltip().shouldNotBe(visible);
    firstViolation.component().shouldHave(text("g1 : a1 : v1")).hover();
    DashboardPage.tooltip().shouldNotBe(visible);
    firstViolation.age().shouldHave(text("1min"));

    // check that tooltips do show for long names
    ViolationTile secondViolation = table.violation(1);
    secondViolation.policy().hover();
    DashboardPage.tooltip().shouldBe(visible).shouldHave(text(securityPolicy.getName()));
    secondViolation.application().hover();
    DashboardPage.tooltip().shouldBe(visible).shouldHave(text(app2.getName()));
    secondViolation.component().hover();
    DashboardPage.tooltip().shouldBe(visible).shouldHave(text("g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890"));

    // check the report link - opens new window
    firstViolation.latestReport().shouldNotBe(DISABLED).shouldHave(text("Build")).click();
    switchToWindow(1);
    waitUntilUrl(ApplicationReportContainerPage.url(app1.getPublicId(), buildEvalNow.getScanId()));
    ApplicationReportContainerPage.getReportTitle().shouldBe(visible)
        .shouldHave(text(app1.getName() + now().toString(" - YYYY-MM-dd -") + " Build Report"));
    WebDriverRunner.getWebDriver().close();
    switchToWindow(0);
    waitUntilUrl(DashboardPage.VIOLATIONS_URL);

    // open component details and back
    DashboardComponentDetails dashboardComponentDetails = new DashboardComponentDetails();
    firstViolation.component().click(5, 5);
    waitUntilUrl(DashboardComponentDetails.url("g1a1v1"));
    DashboardPage.dashboardContainer().shouldNotBe(visible);
    dashboardComponentDetails.header().shouldHave(text("g1 : a1 : v1"));
    Selenide.back();
    DashboardPage.dashboardContainer().shouldBe(visible);

    ViolationsHeaders headers = DashboardPage.violationsView().headers();

    // should be sorted by age
    firstViolation.shouldHave(text("1min"));
    table.lastViolation().shouldHave(text("7d"));

    // check the csv export default sort order
    ResponseCopyHandler responseCopyHandler = new ResponseCopyHandler("/rest/dashboard/export/newestRisks",
        testCLMServer.getCLMServer().getPort());
    reverseProxyServer.addHandler(responseCopyHandler);
    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Violations Data")).click();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page
    String exportCsv = new String(responseCopyHandler.consumeResponse());
    Map<String, Date> expectedResults = ImmutableMap.of(
        "7,DVTLicensePolicy,DVT App1,g1 : a1 : v1", now,         //
        "10,DVTSecurityPolicyWithAnotherUnnecessarilyLongName,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo, //
        "1,DVTLicensePolicy,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo,   //
        "3,DVTLicensePolicy,DVT App1,g3 : a3 : v3", oneWeekAgo   //
    );
    assertViolationsCsv(exportCsv, expectedResults);
    
    headers.ageHeader().click();
    firstViolation.shouldHave(text("7d"));
    table.lastViolation().shouldHave(text("1min"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = ImmutableMap.of(
        "3,DVTLicensePolicy,DVT App1,g3 : a3 : v3", oneWeekAgo,   //
        "10,DVTSecurityPolicyWithAnotherUnnecessarilyLongName,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo, //
        "1,DVTLicensePolicy,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo,   //
        "7,DVTLicensePolicy,DVT App1,g1 : a1 : v1", now           //
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // sort by threat
    headers.threatHeader().click();
    table.violations().shouldHave(texts("10", "7", "3", "1"));
    headers.threatHeader().click();
    table.violations().shouldHave(texts("1", "3", "7", "10"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = ImmutableMap.of(
        "1,DVTLicensePolicy,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo,   //
        "3,DVTLicensePolicy,DVT App1,g3 : a3 : v3", oneWeekAgo,   //
        "7,DVTLicensePolicy,DVT App1,g1 : a1 : v1", now,          //
        "10,DVTSecurityPolicyWithAnotherUnnecessarilyLongName,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo //
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // sort by licensePolicy name
    headers.policyHeader().click();
    firstViolation.shouldHave(text("DVTLicensePolicy"));
    table.lastViolation().shouldHave(text("DVTSecurityPolicyWithAnotherUnnecessarilyLongName"));
    headers.policyHeader().click();
    firstViolation.shouldHave(text("DVTSecurityPolicyWithAnotherUnnecessarilyLongName"));
    table.lastViolation().shouldHave(text("DVTLicensePolicy"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = ImmutableMap.of(
        "10,DVTSecurityPolicyWithAnotherUnnecessarilyLongName,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo, //
        "7,DVTLicensePolicy,DVT App1,g1 : a1 : v1", now,         //
        "1,DVTLicensePolicy,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo,   //
        "3,DVTLicensePolicy,DVT App1,g3 : a3 : v3", oneWeekAgo   //
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // sort by application name
    headers.applicationHeader().click();
    firstViolation.shouldHave(text("DVT App1"));
    table.lastViolation().shouldHave(text("DVT App2 With A Long Name Just To Force Overflow"));
    headers.applicationHeader().click();
    firstViolation.shouldHave(text("DVT App2 With A Long Name Just To Force Overflow"));
    table.lastViolation().shouldHave(text("DVT App1"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = ImmutableMap.of(
        "10,DVTSecurityPolicyWithAnotherUnnecessarilyLongName,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo, //
        "1,DVTLicensePolicy,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo,   //
        "7,DVTLicensePolicy,DVT App1,g1 : a1 : v1", now,         //
        "3,DVTLicensePolicy,DVT App1,g3 : a3 : v3", oneWeekAgo   //
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // sort by component name
    headers.componentHeader().click();
    firstViolation.shouldHave(text("g1 : a1 : v1"));
    table.lastViolation().shouldHave(text("g3 : a3 : v3"));
    headers.componentHeader().click();
    firstViolation.shouldHave(text("g3 : a3 : v3"));
    table.lastViolation().shouldHave(text("g1 : a1 : v1"));

    // check the csv export sorting
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = ImmutableMap.of(
        "3,DVTLicensePolicy,DVT App1,g3 : a3 : v3", oneWeekAgo,   //
        "10,DVTSecurityPolicyWithAnotherUnnecessarilyLongName,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo, //
        "1,DVTLicensePolicy,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo,   //
        "7,DVTLicensePolicy,DVT App1,g1 : a1 : v1", now           //
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // CSV export - filter out threat level 1
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(2, 10);
    DashboardFilters.apply();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = ImmutableMap.of(
        "3,DVTLicensePolicy,DVT App1,g3 : a3 : v3", oneWeekAgo,   //
        "10,DVTSecurityPolicyWithAnotherUnnecessarilyLongName,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo, //
        "7,DVTLicensePolicy,DVT App1,g1 : a1 : v1", now           //
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // CSV export - filter out Build violations
    DashboardFilters.stageFilter().twisty().click();
    DashboardFilters.stageFilter().allItems().click();
    DashboardFilters.stageFilter().build().click();
    DashboardFilters.apply();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = ImmutableMap.of(
        "3,DVTLicensePolicy,DVT App1,g3 : a3 : v3", oneWeekAgo,    //
        "10,DVTSecurityPolicyWithAnotherUnnecessarilyLongName,DVT App2 With A Long Name Just To Force Overflow,g2 : a2 : v2-SNAPSHOT-TEST-RELEASE-CANDIDATE-1234567890", twoDaysAgo //
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // CSV export - filter out Security policy type violations
    DashboardFilters.policyTypeFilter().twisty().click();
    DashboardFilters.policyTypeFilter().allItems().click();
    DashboardFilters.policyTypeFilter().security().click();
    DashboardFilters.apply();
    DashboardPage.exportResultsLink().click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = ImmutableMap.of(
        "3,DVTLicensePolicy,DVT App1,g3 : a3 : v3", oneWeekAgo
    );
    assertViolationsCsv(exportCsv, expectedResults);

    // CSV export - filter out App1
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationFilter().allItems().click();
    DashboardFilters.applicationFilter().checkboxItem(2).click();
    DashboardFilters.apply();
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

  @Test
  public void testSortsOnBackendByAgeAndThreat() {

    for (int i = 1; i <= 25; i++) {
      // 50 violations 1min old: 25 with threatLevel 2, 25 with threatLevel 3
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 2, LICENSE);
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 3, LICENSE);

      // 50 violations 2d old: 25 with threatLevel 4, 25 with threatLevel 5
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 4, LICENSE);
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 5, LICENSE);
    }

    // 20 violations 2d old: 10 with threatLevel 2, 10 with threatLevel 3
    for (int i = 1; i <= 10; i++) {
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 2, LICENSE);
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 3, LICENSE);
    }

    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldBe(visible);

    // by default should be sorted by time desc, threat desc
    headers.ageHeader().sortArrowUp().shouldBeSelected();
    table.firstViolation().age().shouldHave(text("1min"));
    table.firstViolation().threatNumber().shouldHave(text("3"));

    table.violation(49).age().shouldHave(text("1min"));
    table.violation(49).threatNumber().shouldHave(text("2"));

    table.violation(50).age().shouldHave(text("2d"));
    table.violation(50).threatNumber().shouldHave(text("5"));

    table.lastViolation().age().shouldHave(text("2d"));
    table.lastViolation().threatNumber().shouldHave(text("4"));

    // sort by time asc, threat desc
    headers.ageHeader().click();
    headers.ageHeader().sortArrowDown().shouldBeSelected();
    table.firstViolation().age().shouldHave(text("2d"));
    table.firstViolation().threatNumber().shouldHave(text("5"));

    table.violation(49).age().shouldHave(text("2d"));
    table.violation(49).threatNumber().shouldHave(text("4"));

    table.violation(50).age().shouldHave(text("2d"));
    table.violation(50).threatNumber().shouldHave(text("3"));

    table.violation(69).age().shouldHave(text("2d"));
    table.violation(69).threatNumber().shouldHave(text("2"));

    table.violation(70).age().shouldHave(text("1min"));
    table.violation(70).threatNumber().shouldHave(text("3"));

    table.lastViolation().age().shouldHave(text("1min"));
    table.lastViolation().threatNumber().shouldHave(text("2"));
  }

  @Test
  public void testSortsOnBackendByThreatAndAge() {

    for (int i = 1; i <= 25; i++) {
      // 50 violations with threatLevel 3: 25 - 1min old, 25 - 2d old
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 3, LICENSE);
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 3, LICENSE);

      // 50 violations with threatLevel 4: 25 - 1min old, 25 - 2d old
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 4, LICENSE);
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 4, LICENSE);
    }

    // 20 violations with threatLevel 2: 10 - 1min old, 10 - 2d old
    for (int i = 1; i <= 10; i++) {
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 2, LICENSE);
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 2, LICENSE);
    }

    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldBe(visible);

    // sort by threat desc, time desc
    headers.threatHeader().click();
    headers.threatHeader().sortArrowDown().shouldBeSelected();
    table.firstViolation().threatNumber().shouldHave(text("4"));
    table.firstViolation().age().shouldHave(text("1min"));

    table.violation(25).threatNumber().shouldHave(text("4"));
    table.violation(25).age().shouldHave(text("2d"));

    table.violation(50).threatNumber().shouldHave(text("3"));
    table.violation(50).age().shouldHave(text("1min"));

    table.lastViolation().threatNumber().shouldHave(text("3"));
    table.lastViolation().age().shouldHave(text("2d"));

    // sort by threat asc, time desc
    headers.threatHeader().click();
    headers.threatHeader().sortArrowUp().shouldBeSelected();
    table.firstViolation().threatNumber().shouldHave(text("2"));
    table.firstViolation().age().shouldHave(text("1min"));

    table.violation(10).threatNumber().shouldHave(text("2"));
    table.violation(10).age().shouldHave(text("2d"));

    table.violation(20).threatNumber().shouldHave(text("3"));
    table.violation(20).age().shouldHave(text("1min"));

    table.violation(45).threatNumber().shouldHave(text("3"));
    table.violation(45).age().shouldHave(text("2d"));

    table.violation(70).threatNumber().shouldHave(text("4"));
    table.violation(70).age().shouldHave(text("1min"));

    table.lastViolation().threatNumber().shouldHave(text("4"));
    table.lastViolation().age().shouldHave(text("2d"));
  }

  @Test
  public void testSortsOnBackendByPolicyAndAge() {
    for (int i = 1; i <= 25; i++) {
      // 50 licensePolicy violations: 25 - 1min old, 25 - 2d old
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 5, LICENSE);
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 5, LICENSE);

      // 50 securityPolicy violations: 25 - 1min old, 25 - 2d old
      tempEntity.newPolicyViolation(buildEvalNow, securityPolicy, 5, LICENSE);
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, securityPolicy, 5, LICENSE);
    }

    // 25 licensePolicy violations 1 week old
    for (int i = 1; i <= 25; i++) {
      tempEntity.newPolicyViolation(operateEval1WeekAgo, licensePolicy, 6, LICENSE);
    }

    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldBe(visible);

    // sort by policy asc, time desc
    headers.policyHeader().click();
    headers.policyHeader().sortArrowUp().shouldBeSelected();
    table.firstViolation().policy().shouldHave(text(licensePolicy.getName()));
    table.firstViolation().age().shouldHave(text("1min"));

    table.violation(25).policy().shouldHave(text(licensePolicy.getName()));
    table.violation(25).age().shouldHave(text("2d"));

    table.violation(50).policy().shouldHave(text(licensePolicy.getName()));
    table.violation(50).age().shouldHave(text("7d"));

    table.lastViolation().policy().shouldHave(text(securityPolicy.getName()));
    table.lastViolation().age().shouldHave(text("1min"));

    // sort by policy desc, time desc
    headers.policyHeader().click();
    headers.policyHeader().sortArrowDown().shouldBeSelected();
    table.firstViolation().policy().shouldHave(text(securityPolicy.getName()));
    table.firstViolation().age().shouldHave(text("1min"));

    table.violation(25).policy().shouldHave(text(securityPolicy.getName()));
    table.violation(25).age().shouldHave(text("2d"));

    table.violation(50).policy().shouldHave(text(licensePolicy.getName()));
    table.violation(50).age().shouldHave(text("1min"));

    table.lastViolation().policy().shouldHave(text(licensePolicy.getName()));
    table.lastViolation().age().shouldHave(text("2d"));
  }

  @Test
  public void testSortsOnBackendByApplicationAndThreat() {

    for (int i = 1; i <= 25; i++) {
      // 50 violations for App1: 25 with threat 4, 25 with threat 5
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 4, LICENSE);
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 5, LICENSE);

      // 50 violations for App2: 25 with threat 4, 25 with threat 5
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 4, LICENSE);
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, licensePolicy, 5, LICENSE);
    }

    // 25 violations for App2: with threat 2
    for (int i = 1; i <= 25; i++) {
      tempEntity.newPolicyViolation(releaseEval2DaysAgo, securityPolicy, 2, LICENSE);
    }

    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldBe(visible);

    // sort by application asc, threat desc
    headers.applicationHeader().click();
    headers.applicationHeader().sortArrowUp().shouldBeSelected();
    table.firstViolation().application().shouldHave(text(app1.getName()));
    table.firstViolation().threatNumber().shouldHave(text("5"));

    table.violation(25).application().shouldHave(text(app1.getName()));
    table.violation(25).threatNumber().shouldHave(text("4"));

    table.violation(50).application().shouldHave(text(app2.getName()));
    table.violation(50).threatNumber().shouldHave(text("5"));

    table.lastViolation().application().shouldHave(text(app2.getName()));
    table.lastViolation().threatNumber().shouldHave(text("4"));

    // sort by application desc, threat desc
    headers.applicationHeader().click();
    headers.applicationHeader().sortArrowDown().shouldBeSelected();
    table.firstViolation().application().shouldHave(text(app2.getName()));
    table.firstViolation().threatNumber().shouldHave(text("5"));

    table.violation(25).application().shouldHave(text(app2.getName()));
    table.violation(25).threatNumber().shouldHave(text("4"));

    table.violation(50).application().shouldHave(text(app2.getName()));
    table.violation(50).threatNumber().shouldHave(text("2"));

    table.violation(75).application().shouldHave(text(app1.getName()));
    table.violation(75).threatNumber().shouldHave(text("5"));

    table.lastViolation().application().shouldHave(text(app1.getName()));
    table.lastViolation().threatNumber().shouldHave(text("5"));
  }

  @Test
  public void testSortsOnBackendByComponentNameAndThreat() {

    for (int i = 1; i <= 25; i++) {
      // 50 violations for 'group1' component: 25 with threatLevel 4, 25 with threatLevel 5
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 4, LICENSE, "group1", "artifact", "version");
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 5, LICENSE, "group1", "artifact", "version");

      // 50 violations for 'group2' component: 25 with threatLevel 4, 25 with threatLevel 5
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 4, LICENSE, "group2", "artifact", "version");
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 5, LICENSE, "group2", "artifact", "version");
    }

    // 25 violations for 'group2' component with threatLevel 2
    for (int i = 1; i <= 25; i++) {
      tempEntity.newPolicyViolation(buildEvalNow, licensePolicy, 2, LICENSE, "group2", "artifact", "version");
    }

    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldBe(visible);

    // sort by component name asc, threat desc
    headers.componentHeader().click();
    headers.componentHeader().sortArrowUp().shouldBeSelected();
    table.firstViolation().component().shouldHave(text("group1"));
    table.firstViolation().threatNumber().shouldHave(text("5"));

    table.violation(25).component().shouldHave(text("group1"));
    table.violation(25).threatNumber().shouldHave(text("4"));

    table.violation(50).component().shouldHave(text("group2"));
    table.violation(50).threatNumber().shouldHave(text("5"));

    table.violation(75).component().shouldHave(text("group2"));
    table.violation(75).threatNumber().shouldHave(text("4"));

    table.lastViolation().component().shouldHave(text("group2"));
    table.lastViolation().threatNumber().shouldHave(text("4"));

    // sort by component name desc, threat desc
    headers.componentHeader().click();
    headers.componentHeader().sortArrowDown().shouldBeSelected();
    table.firstViolation().component().shouldHave(text("group2"));
    table.firstViolation().threatNumber().shouldHave(text("5"));

    table.violation(25).component().shouldHave(text("group2"));
    table.violation(25).threatNumber().shouldHave(text("4"));

    table.violation(50).component().shouldHave(text("group2"));
    table.violation(50).threatNumber().shouldHave(text("2"));

    table.violation(75).component().shouldHave(text("group1"));
    table.violation(75).threatNumber().shouldHave(text("5"));

    table.lastViolation().component().shouldHave(text("group1"));
    table.lastViolation().threatNumber().shouldHave(text("5"));
  }

  private void assertViolationsCsv(String csv, Map<String, Date> expectedSortedResults) {
    String[] lines = csv.split("\r\n");

    // assert CSV header
    assertEquals(CSV_HEADERS, lines[0]);

    // assert CSV results
    String[] results = Arrays.copyOfRange(lines, 1, lines.length);
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
    new DashboardFilterDAO().deleteByUsername(User.ADMIN_USERNAME);
  }
}
