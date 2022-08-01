/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationreport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.PolicyTypeFilter;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.ViolationStateFilter;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxDropdown;
import com.sonatype.clm.testing.functional.elements.NxTooltip;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ApplicationReportContainerPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.AppReportHeaders;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQCoverageIndicator;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQGrandfatheringIndicator;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.ResultRow;
import com.sonatype.clm.testing.functional.pages.ApplicationReportRawDataPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportVulnerabilitiesPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.InputUtils;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.clm.testing.functional.utils.WaiverApplierForReport;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.joda.time.format.DateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.pages.ApplicationReportPage.DIRECT_DEPENDENCY_CLASS;
import static com.sonatype.clm.testing.functional.pages.ApplicationReportPage.INNER_SOURCE_DEPENDENCY_CLASS;
import static com.sonatype.clm.testing.functional.pages.ApplicationReportPage.TRANSITIVE_DEPENDENCY_CLASS;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationReportTest
        extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private PolicyDAO policyDAO = new PolicyDAO();

  private PolicyViolationGrandfatheringService policyViolationGrandfatheringService;

  private final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    policyViolationGrandfatheringService =
            testCLMServer.getCLMServer().getInstance(PolicyViolationGrandfatheringService.class);
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = new PolicyImportExport();

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportTest", "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @After
  public void afterEachTestEnds() {
    if (reportPage.filterPanel().getElement().is(visible)) {
      reportPage.filterPanel().closeButton().click();
    }
  }

  @Test
  public void testSummary() throws Exception {
    PolicyEvaluation policyEvaluation = new PolicyEvaluationDAO().getLastByApplicationIdAndScanId(app.getId(), SCAN_ID);
    Date policyEvaluationTime = policyEvaluation.getTime();

    String policyEvaluationTimeStr = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'Z")
            .print(policyEvaluationTime.getTime());
    reportPage.shouldBe(visible);
    reportPage.reportTitle().shouldHave(text(app.getName() + " Build Report"));
    reportPage.reportDescription()
            .shouldHave(text("Triggered by " + policyEvaluation.getScanTriggerType().getDisplayName()));
    reportPage.reportDescription().shouldNotHave(text("(Continuous Monitoring)"));
    reportPage.reportDescription().shouldNotHave(text("(Re-evaluation)"));
    reportPage.reportDescription().shouldHave(text("on " + policyEvaluationTimeStr));
    reportPage.reportDescription().shouldHave(text(policyEvaluation.getCommitHash()));

    reportPage.policyTypeFilterWarning().shouldNot(exist);

    reportPage.optionsDropdown().shouldBe(visible).menu().shouldNotBe(visible);
    reportPage.threatIndicators().critical().shouldHave(text("22"));
    reportPage.threatIndicators().severe().shouldHave(text("39"));
    reportPage.threatIndicators().moderate().shouldHave(text("4"));
    reportPage.threatIndicators().caption().shouldHave(exactText("65 Violations"));
    reportPage.threatIndicators().subCaption().shouldHave(exactText("Affecting 27 components"));

    IQCoverageIndicator coverageIndicator = reportPage.coverageIndicator();
    coverageIndicator.caption().shouldHave(exactText("64 COMPONENTS"));
    coverageIndicator.subCaption().shouldHave(exactText("97% of all components identified"));
    coverageIndicator.donutChart().shouldBe(visible);

    IQGrandfatheringIndicator grandfatheringIndicator = reportPage.grandfatheringIndicator();
    grandfatheringIndicator.caption().shouldHave(exactText("0 Grandfathered"));

    activateGrandfathering();

    grandfatheringIndicator = reportPage.grandfatheringIndicator();
    grandfatheringIndicator.caption().shouldHave(exactText("46 Grandfathered"));

    // The activateGrandfathering above re-evals and refreshes the page
    policyEvaluation = new PolicyEvaluationDAO().getLastByApplicationIdAndScanId(app.getId(), SCAN_ID);
    policyEvaluationTime = policyEvaluation.getTime();
    policyEvaluationTimeStr =
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'Z").print(policyEvaluationTime.getTime());
    refresh();
    reportPage.shouldBe(visible);
    reportPage.reportDescription()
            .shouldHave(text("Triggered by " + policyEvaluation.getScanTriggerType().getDisplayName()));
    reportPage.reportDescription().shouldNotHave(text("(Continuous Monitoring)"));
    reportPage.reportDescription().shouldHave(text("(Re-evaluation)"));
    reportPage.reportDescription().shouldHave(text("on " + policyEvaluationTimeStr));
    reportPage.reportDescription().shouldHave(text(policyEvaluation.getCommitHash()));

    // Update the policy evaluation to look like it was for monitoring
    policyEvaluation = updateForMonitoring(policyEvaluation);
    policyEvaluationTime = policyEvaluation.getTime();
    policyEvaluationTimeStr =
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'Z").print(policyEvaluationTime.getTime());
    refresh();
    reportPage.shouldBe(visible);
    reportPage.reportDescription()
            .shouldHave(text("Triggered by " + policyEvaluation.getScanTriggerType().getDisplayName()));
    reportPage.reportDescription().shouldHave(text("(Continuous Monitoring)"));
    reportPage.reportDescription().shouldNotHave(text("(Re-evaluation)"));
    reportPage.reportDescription().shouldHave(text("on " + policyEvaluationTimeStr));
    reportPage.reportDescription().shouldHave(text(policyEvaluation.getCommitHash()));
  }

  private PolicyEvaluation updateForMonitoring(PolicyEvaluation policyEvaluation) {
    policyEvaluation.setForMonitoring(true);
    policyEvaluation.setTime(new Date(policyEvaluation.getTime().getTime() + 1000));
    detachEntity(policyEvaluation);
    new PolicyEvaluationDAO().insert(policyEvaluation);
    return policyEvaluation;
  }

  private <E extends HasStringId> void detachEntity(E entity) {
    PersistenceCapable pc = (PersistenceCapable) entity;
    pc.pcSetDetachedState(null);
    pc.pcReplaceStateManager(null);
    entity.setId(null);
  }

  @Test
  public void testOptionsMenu() {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.shouldBe(visible).menu().shouldNotBe(visible);
    optionsDropdown.button().shouldHave(text("Options")).click();
    optionsDropdown.menu().shouldBe(visible).entries().shouldHaveSize(5);

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testDownloadPdf() throws Exception {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();

    long currentTimeout = Configuration.timeout;
    File downloadedPdf;
    try {
      // generating the PDF takes awhile; increase the timeout to 20 seconds
      Configuration.timeout = 20000;

      downloadedPdf = optionsDropdown.menu().entries().first().shouldHave(text("Generate PDF")).download();
    }
    finally {
      Configuration.timeout = currentTimeout;
    }

    byte[] fileBeginning = new byte[4];
    try (FileInputStream stream = new FileInputStream(downloadedPdf)) {
      stream.read(fileBeginning);
    }

    // detect PDF magic number ("%PDF")
    assertThat(fileBeginning).isEqualTo(new byte[]{0x25, 0x50, 0x44, 0x46});
  }

  @Test
  public void testViewSbom() throws Exception {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();

    File downloadedSbom = optionsDropdown.menu().entries().get(1).shouldHave(text("View SBOM")).download();

    byte[] fileBeginning = new byte[5];
    try (FileInputStream stream = new FileInputStream(downloadedSbom)) {
      stream.read(fileBeginning);
    }

    // similar to the PDF test, the content of the file is checked to see if it's XML
    assertThat(new String(fileBeginning)).isEqualTo("<?xml");
  }

  @Test
  public void testRawDataLink() {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();
    optionsDropdown.menu().entries().get(2).shouldHave(text("View raw data")).click();

    waitUntilUrl(ApplicationReportRawDataPage.url(app, SCAN_ID));
    new ApplicationReportRawDataPage().reportTitle().shouldHave(text(app.getName()));
  }

  @Test
  public void testVulnerabilitiesLink() {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();
    optionsDropdown.menu().entries().get(3).shouldHave(text("View vulnerabilities")).click();

    waitUntilUrl(ApplicationReportVulnerabilitiesPage.url(app, SCAN_ID));
    new ApplicationReportVulnerabilitiesPage().title().shouldHave(text(app.getName()));
  }

  @Test
  public void testLinkToOldReport() {
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();
    optionsDropdown.menu().entries().last().shouldHave(text("View legacy report")).click();

    ApplicationReportContainerPage.getIframe().shouldBe(visible);
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testTextIndicators() throws Exception {
    Policy licenseBanned = new PolicyDAO().getByName("License-Banned").get(0);
    reportPage.headers().policyNameFilterInput().setValue(licenseBanned.getName());
    reportPage.resultRows().shouldHaveSize(2);
    reportPage.resultRow(1).waivedIndicator().shouldBe(hidden);
    reportPage.resultRow(2).waivedIndicator().shouldBe(hidden);

    PolicyWaiver waiver = tempEntity.newWaiver(licenseBanned.getId(), app.getId());
    evaluator.reevaluatePolicy();
    refresh();

    // test that indicators are shown when aggregating
    InputUtils.clearInput(reportPage.headers().policyNameFilterInput());
    reportPage.headers().componentNameFilterInput().setValue("mycila");
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);
    reportPage.resultRow(1).grandfatheredIndicator().shouldNotBe(visible);
    reportPage.headers().componentNameFilterInput().setValue("vaadin");
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);
    reportPage.resultRow(1).grandfatheredIndicator().shouldNotBe(visible);

    // test that indicators are shown when not aggregating
    reportPage.headers().policyNameFilterInput().setValue(licenseBanned.getName());
    InputUtils.clearInput(reportPage.headers().componentNameFilterInput());
    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    reportPage.resultRows().shouldHaveSize(2);
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);
    reportPage.resultRow(2).waivedIndicator().shouldBe(visible);
    reportPage.resultRow(1).grandfatheredIndicator().shouldNotBe(visible);
    reportPage.resultRow(2).grandfatheredIndicator().shouldNotBe(visible);

    activateGrandfathering();

    // now the grandfathered indicator should appear
    reportPage.headers().componentNameFilterInput().setValue("mycila");
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);
    reportPage.resultRow(1).grandfatheredIndicator().shouldBe(visible);

    reportPage.headers().componentNameFilterInput().setValue("vaadin");
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);
    reportPage.resultRow(1).grandfatheredIndicator().shouldBe(visible);

    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    reportPage.headers().policyNameFilterInput().setValue(licenseBanned.getName());
    InputUtils.clearInput(reportPage.headers().componentNameFilterInput());
    reportPage.resultRows().shouldHaveSize(2);
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);
    reportPage.resultRow(1).grandfatheredIndicator().shouldBe(visible);

    eyesWatcher.eyesCheck();

    // a test to catch CLM-12064. When aggregating, the policy name for these rows should change back to None
    reportPage.headers().componentNameFilterInput().setValue("vaadin");
    InputUtils.clearInput(reportPage.headers().policyNameFilterInput());
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.resultRow(1).policyName().shouldHave(text(licenseBanned.getName()));
    reportPage.resultRow(1).threatNumber().shouldHave(text("10"));
    reportPage.aggregateByComponentToggle().shouldBeOff().click();
    reportPage.aggregateByComponentToggle().shouldBeOn();
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.resultRow(1).policyName().shouldHave(text("None"));
    reportPage.resultRow(1).threatNumber().shouldHave(text("0"));

    new PolicyWaiverDAO().delete(waiver);
    evaluator.reevaluatePolicy();
    refresh();

    reportPage.headers().componentNameFilterInput().setValue("mycila");
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.resultRow(1).waivedIndicator().shouldNotBe(visible);
    reportPage.resultRow(1).grandfatheredIndicator().shouldBe(visible);

    reportPage.headers().componentNameFilterInput().setValue("vaadin");
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.resultRow(1).waivedIndicator().shouldNotBe(visible);
    reportPage.resultRow(1).grandfatheredIndicator().shouldBe(visible);

    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    reportPage.headers().policyNameFilterInput().setValue(licenseBanned.getName());
    InputUtils.clearInput(reportPage.headers().componentNameFilterInput());
    reportPage.resultRows().shouldHaveSize(2);
    reportPage.resultRow(1).waivedIndicator().shouldNotBe(visible);
    reportPage.resultRow(1).grandfatheredIndicator().shouldBe(visible);
  }

  @Test
  public void testEllipsisInPolicyName() {
    reportPage.headers().policyNameFilterInput().setValue("License-Threat");
    reportPage.getColFromResultRow(1, 2).shouldHave(cssValue("text-overflow", "ellipsis"));

    // We need to ensure by visual tests that the ellipsis is shown, there is no way
    // to see if the ellipsis is applied by looking to the HTML, attributes or innerText,
    // the text look the same with or without ellipsis to selenium/selenide accessors.
    eyesWatcher.eyesCheck("Showing ellipsis in overflown policy names.");
  }

  @Test
  public void testEllipsisInComponentName() {
    reportPage.headers().componentNameFilterInput()
        .setValue("org.springframework.security : spring-security-config : 3.2.4.RELEASE");
    reportPage.getColFromResultRow(1, 3).shouldHave(cssValue("text-overflow", "ellipsis"));

    // We need to ensure by visual tests that the ellipsis is shown, there is no way
    // to see if the ellipsis is applied by looking to the HTML, attributes or innerText,
    // the text look the same with or without ellipsis to selenium/selenide accessors.
    eyesWatcher.eyesCheck("Showing ellipsis in overflown component names");
  }

  @Test
  public void testInnerSourceTransitiveViolationsCount() {
    reportPage.aggregateByComponentToggle().shouldBeOn();
    reportPage.resultRow(16).shouldHave(text("org.springframework.security : spring-security-config : 3.2.4.RELEASE"))
            .transitiveViolationsCount().shouldHaveSize(1).get(0).shouldHave(text("2 transitive violations"));
    reportPage.aggregateByComponentToggle().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    reportPage.resultRow(21).shouldHave(text("org.springframework.security : spring-security-config : 3.2.4.RELEASE"))
            .transitiveViolationsCount().shouldHaveSize(0);
  }

  @Test
  public void testDependencyIndicators() {
    reportPage.rowsWithDependencyInfo().shouldHaveSize(6);
    reportPage.resultRow(5).shouldHave(text("apache-httpclient : commons-httpclient : 3.1"))
            .dependencyIndicators().shouldHaveSize(1).get(0).shouldHave(DIRECT_DEPENDENCY_CLASS);
    ResultRow resultRow = reportPage.resultRow(6).shouldHave(text("apache-taglibs : standard : 1.1.2"));
    ElementsCollection dependencyIndicators = resultRow.dependencyIndicators().shouldHaveSize(2);
    dependencyIndicators.get(0).shouldHave(TRANSITIVE_DEPENDENCY_CLASS).shouldHave(text("T"));
    dependencyIndicators.get(1).shouldHave(INNER_SOURCE_DEPENDENCY_CLASS).shouldHave(text("IS")).hover();
    Tooltip.get().shouldBe(visible)
            .shouldHave(text("This component was brought in by the following InnerSource component:"))
            .shouldHave(text("org.springframework.security : spring-security-config : 3.2.4.RELEASE"));
    dependencyIndicators = reportPage.resultRow(16)
            .shouldHave(text("org.springframework.security : spring-security-config : 3.2.4.RELEASE"))
            .dependencyIndicators().shouldHaveSize(2);
    dependencyIndicators.get(0).shouldHave(DIRECT_DEPENDENCY_CLASS).shouldHave(text("D")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Direct Dependency"));
    dependencyIndicators.get(1).shouldHave(INNER_SOURCE_DEPENDENCY_CLASS).shouldHave(text("IS")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("InnerSource"));
    reportPage.resultRow(26).shouldHave(text("org.springframework : spring-core : 3.2.8.RELEASE"))
            .dependencyIndicators().shouldHaveSize(1).get(0).shouldHave(TRANSITIVE_DEPENDENCY_CLASS);
    reportPage.resultRow(57).shouldHave(text("org.springframework : spring-aop : 3.2.8.RELEASE"))
            .dependencyIndicators().shouldHaveSize(1).get(0).shouldHave(TRANSITIVE_DEPENDENCY_CLASS);
    reportPage.resultRow(58).shouldHave(text("org.springframework : spring-beans : 3.2.4.RELEASE"))
            .dependencyIndicators().shouldHaveSize(1).get(0).shouldHave(TRANSITIVE_DEPENDENCY_CLASS);
  }

  @Test
  public void testAggregation() {
    // Aggregate by Component toggle
    reportPage.aggregateByComponentToggle().label().shouldHave(text("Aggregate by component"));
    reportPage.aggregateByComponentToggle().label().hover();
    NxTooltip aggregateByComponentToggleToolTip = new NxTooltip();
    aggregateByComponentToggleToolTip.getElement()
            .shouldHave(text("By default the Application Report aggregates violations by component. " +
                    "To see all violations not Aggregated by Component, please switch the toggle off."));

    // By default the "Aggregate by Component" toggle should be ON
    reportPage.aggregateByComponentToggle().shouldBeOn();
    reportPage.resultRows().shouldHaveSize(64);
    reportPage.getThreatBars("critical").shouldHaveSize(17);
    reportPage.getThreatBars("severe").shouldHaveSize(9);
    reportPage.getThreatBars("moderate").shouldHaveSize(1);
    reportPage.getThreatBars("low").shouldHaveSize(1);
    reportPage.getThreatBars("none").shouldHaveSize(36);
    reportPage.headers().componentNameFilterInput().setValue("commons-fileupload");
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.getThreatBars("critical").shouldHaveSize(1);

    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();

    reportPage.resultRows().shouldHaveSize(6);
    reportPage.getThreatBars("critical").shouldHaveSize(4);
    reportPage.getThreatBars("severe").shouldHaveSize(1);
    reportPage.getThreatBars("moderate").shouldHaveSize(1);

    InputUtils.clearInput(reportPage.headers().componentNameFilterInput());
    reportPage.resultRows().shouldHaveSize(102);
    reportPage.getThreatBars("critical").shouldHaveSize(22);
    reportPage.getThreatBars("severe").shouldHaveSize(39);
    reportPage.getThreatBars("moderate").shouldHaveSize(4);
    reportPage.getThreatBars("low").shouldHaveSize(1);
    reportPage.getThreatBars("none").shouldHaveSize(36);
  }

  @Test
  public void testSorting() {
    AppReportHeaders headers = reportPage.headers();
    ElementsCollection violations = reportPage.resultRows();
    // reduce the result set so we don't need to scroll around
    headers.componentNameFilterInput().setValue("com.");
    violations.shouldHaveSize(8);

    // by threat level
    headers.threatHeader().sortArrows().shouldBeDown();
    violations.shouldHave(texts("10", "10", "9", "9", "0", "0", "0", "0"));
    // check that entries have also been sorted by component name
    checkSecondarySortByNameDescending(violations);
    // reverse threat level
    headers.threatHeader().click();
    headers.threatHeader().sortArrows().shouldBeUp();
    violations.shouldHave(texts("0", "0", "0", "0", "9", "9", "10", "10"));
    // the secondary sort should remain unchanged
    checkSecondarySortByNameDescending(violations);

    // by policy name
    headers.policyNameHeader().click();
    headers.policyNameHeader().sortArrows().shouldBeUp();
    violations.shouldHave(
        texts("License-Banned", "License-Banned", "None", "None", "None", "None", "Security-High", "Security-High"));
    checkSecondarySortByNameDescending(violations);
    headers.policyNameHeader().click();
    headers.policyNameHeader().sortArrows().shouldBeDown();
    violations.shouldHave(
        texts("Security-High", "Security-High", "None", "None", "None", "None", "License-Banned", "License-Banned"));
    checkSecondarySortByNameDescending(violations);

    // by component name
    reportPage.aggregateByComponentToggle().shouldBeOn().click(); // un-aggregate in order to check secondary sort
    reportPage.aggregateByComponentToggle().shouldBeOff();
    headers.componentNameHeader().click();
    headers.componentNameHeader().sortArrows().shouldBeUp();
    String[] componentNamesAlpha = {
        "com.adobe.acrobat", "com.adobe.pdf", "com.fasterxml.jackson.core : jackson-annotations",
        "com.fasterxml.jackson.core : jackson-core", "com.fasterxml.jackson.core : jackson-core",
        "com.fasterxml.jackson.core : jackson-databind", "com.mycila", "com.palominolabs.metrics",
        "com.vaadin.addon"
    };
    violations.shouldHave(texts(componentNamesAlpha));
    // secondary sort by threat level descending
    violations.filterBy(matchesText("jackson-core")).shouldHave(texts("9", "7"));
    headers.componentNameHeader().click();
    headers.componentNameHeader().sortArrows().shouldBeDown();
    ArrayUtils.reverse(componentNamesAlpha);
    violations.shouldHave(texts(componentNamesAlpha));
    // secondary sort should remain unchanged
    violations.filterBy(matchesText("jackson-core")).shouldHave(texts("9", "7"));
  }

  @Test
  public void testFilteringNotPersisting() {
    AppReportHeaders headers = reportPage.headers();
    ElementsCollection violations = reportPage.resultRows();

    headers.policyNameFilterInput().setValue("unk");
    violations.shouldHaveSize(1);
    violations.shouldHave(texts("Component-Unknown"));
    violations.shouldHave(texts("RegexMatch.dll"));

    SidebarNavigation.reportingNavigationButton().click();
    ReportListPage.firstRow().buildReportLink().click();

    headers.policyNameFilterInput().shouldBe(Condition.empty);
    violations.shouldHaveSize(64);

    headers.componentNameFilterInput().setValue("Reg");
    violations.shouldHaveSize(1);
    violations.shouldHave(texts("Component-Unknown"));
    violations.shouldHave(texts("RegexMatch.dll"));

    SidebarNavigation.reportingNavigationButton().click();
    ReportListPage.firstRow().buildReportLink().click();

    headers.componentNameFilterInput().shouldBe(Condition.empty);
    violations.shouldHaveSize(64);
  }

  @Test
  public void testFiltering_Scroll() {
    reportPage.filterToggle().click();

    reportPage.filterPanel().proprietaryFilter().twisty().click();
    reportPage.filterPanel().matchStateFilter().twisty().click();
    reportPage.filterPanel().violationStateFilter().twisty().click();
    reportPage.filterPanel().dependencyTypeFilter().twisty().click();

    PolicyTypeFilter policyTypeFilter = reportPage.filterPanel().policyTypeFilter();
    policyTypeFilter.twisty().click();
    ScrollUtil.scrollIntoView(policyTypeFilter.twisty());
    policyTypeFilter.other().shouldBe(visible);
  }

  @Test
  public void testFiltering_violationState() throws Exception {
    ElementsCollection violations = reportPage.resultRows();
    reportPage.filterToggle().click();

    ViolationStateFilter violationStateFilter = reportPage.filterPanel().violationStateFilter();
    violationStateFilter.multiSelectList().shouldHaveSize(5);
    violationStateFilter.counter().shouldHave(exactText("4"));
    violationStateFilter.multiSelectList().forEach(child -> child.shouldNotBe(visible));
    violationStateFilter.twisty().click();
    violationStateFilter.multiSelectList().forEach(child -> child.shouldBe(visible));

    violationStateFilter.open().click();
    violationStateFilter.open().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("1 of 4"));
    violations.shouldHaveSize(28);
    violations.first().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));

    WaiverApplierForReport.waiveReportRow(reportPage, 0);

    reportPage.shouldBe(visible);
    reportPage.reevaluateButton().click();
    FormMask.seeAndWaitForDismissal();
    violations.shouldHaveSize(64);

    reportPage.filterToggle().click();
    violationStateFilter = reportPage.filterPanel().violationStateFilter();
    violationStateFilter.multiSelectList().shouldHaveSize(5);
    violationStateFilter.multiSelectList().forEach(child -> child.shouldNotBe(visible));
    violationStateFilter.twisty().click();
    violationStateFilter.multiSelectList().shouldHaveSize(5);
    violationStateFilter.multiSelectList().forEach(child -> child.shouldBe(visible));

    violationStateFilter.open().click();
    violationStateFilter.open().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("1 of 4"));
    // waived violation filtered out
    violations.shouldHaveSize(27);

    // now add waived violations
    violationStateFilter.waived().click();
    violationStateFilter.waived().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("2 of 4"));
    violations.shouldHaveSize(28);

    // at this point, the mycila violation is visible but is way down at the "None" part of the list because we are
    // in the aggregated view
    violations.first().shouldHave(text("com.vaadin.addon : vaadin-touchkit-agpl : 3.0.0-beta1"));

    // switch to non-aggregated view to get the actual waived violation,
    // back in its original place at the top of the list
    reportPage.filterPanel().closeButton().click();
    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    violations.shouldHaveSize(66);
    violations.first().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));

    activateGrandfathering();

    // activateGrandfathering refreshes the page so we need to put the filter back how we had it
    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    reportPage.filterToggle().click();
    violationStateFilter.twisty().click();
    violationStateFilter.open().click();
    violationStateFilter.waived().click();

    // grandfathered violations not visible
    violations.shouldHaveSize(21);

    // grandfathered violations now visible
    violationStateFilter.grandfathered().click();
    violationStateFilter.grandfathered().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("3 of 4"));
    violations.shouldHaveSize(66);

    // the waived violation is also grandfathered, so no difference.
    violationStateFilter.waived().click();
    violations.shouldHaveSize(66);

    violationStateFilter.notViolating().click();
    violationStateFilter.notViolating().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("3 of 4"));
    violations.shouldHaveSize(102);

    // all boxes checked - again no difference in count because the waived violation is also grandfathered
    violationStateFilter.waived().click();
    violationStateFilter.counter().shouldHave(exactText("4 of 4"));
    violations.shouldHaveSize(102);

    // no boxes checked
    violationStateFilter.allItems().shouldBe(selected).click();
    violationStateFilter.allItems().shouldNotBe(selected);
    violationStateFilter.notViolating().shouldNotBe(selected);
    violationStateFilter.open().shouldNotBe(selected);
    violationStateFilter.waived().shouldNotBe(selected);
    violationStateFilter.grandfathered().shouldNotBe(selected);
    violationStateFilter.counter().shouldHave(exactText("4"));
    violations.shouldHaveSize(102);

    reportPage.filterPanel().closeButton().click();
  }

  @Test
  public void testReevaluate() {
    Policy licenseBanned = new PolicyDAO().getByName("License-Banned").get(0);
    tempEntity.newWaiver(licenseBanned.getId(), app.getId());

    reportPage.headers().componentNameFilterInput().setValue("mycila");
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.resultRow(1).waivedIndicator().shouldNotBe(visible);

    reportPage.reevaluateButton().click();
    FormMask.seeAndWaitForDismissal();

    reportPage.headers().componentNameFilterInput().shouldHave(value("mycila"));
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);
  }

  @Test
  public void testFilterReset() {
    AppReportHeaders headers = reportPage.headers();

    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();
    headers.policyNameHeader().click();
    headers.policyNameHeader().sortArrows().shouldBeUp();

    reportPage.filterToggle().click();
    reportPage.filterPanel().proprietaryFilter().twisty().click();
    reportPage.filterPanel().proprietaryFilter().nonProprietary().click();
    reportPage.filterPanel().proprietaryFilter().nonProprietary().shouldBe(selected);

    // navigate elsewhere and then back to this report, without triggering a full refresh
    SidebarNavigation.reportingNavigationButton().click();
    ReportListPage.firstRow().buildReportLink().click();

    reportPage.reportTitle().shouldHave(text(app.getName() + " Build Report"));
    reportPage.aggregateByComponentToggle().shouldBeOn();
    headers.policyNameHeader().sortArrows().shouldNotBeUp();
    reportPage.filterPanel().proprietaryFilter().nonProprietary().shouldNotBe(selected);
    reportPage.filterPanel().closeButton();
  }

  @Test
  public void testEmbeddable() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID) + "?embeddable");

    // test that the header is not present but that the data and sidebar are
    MainHeader.get().shouldNot(exist);
    reportPage.resultRows().shouldHaveSize(64);
    reportPage.aggregateByComponentToggle().shouldBeOn();
  }

  @Test
  public void testPolicyTypeFilterDisabledInV3Report() throws IOException {
    // Setup
    final String SCAN_ID2 = "e16caf35769f4b3186a7e3476d34c2798";
    Application app2 = tempEntity.newApplicationWithParent();
    URL zippedReport = ReportHelper.zipReport("/canned-reports/evaluated-v3-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(app2.getId(), SCAN_ID2);
    FileUtils.copyURLToFile(zippedReport, reportDestination);
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, SCAN_ID2);
    refreshOrOpen(ApplicationReportPage.url(app2, SCAN_ID2));

    // Assertions
    reportPage.filterToggle().click();
    PolicyTypeFilter policyTypeFilter = reportPage.filterPanel().policyTypeFilter();
    ElementsCollection violations = reportPage.resultRows();

    reportPage.policyTypeFilterWarning().shouldBe(visible);
    policyTypeFilter.counter().shouldHave(exactText("4"));
    policyTypeFilter.multiSelectList().forEach(child -> child.shouldNotBe(visible));
    violations.shouldHaveSize(63);
    policyTypeFilter.hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Reevaluate the report in order to enable Policy Types filter"));
    policyTypeFilter.multiSelectList().shouldHaveSize(5);
    policyTypeFilter.allItems().shouldBe(disabled);
    policyTypeFilter.security().shouldBe(disabled);
    policyTypeFilter.quality().shouldBe(disabled);
    policyTypeFilter.license().shouldBe(disabled);
    policyTypeFilter.other().shouldBe(disabled);
    // Assert no changes on click.
    policyTypeFilter.twisty().click();
    policyTypeFilter.multiSelectList().forEach(child -> child.shouldNotBe(visible));
    policyTypeFilter.allItems().shouldBe(disabled);
    policyTypeFilter.security().shouldBe(disabled);
    policyTypeFilter.quality().shouldBe(disabled);
    policyTypeFilter.license().shouldBe(disabled);
    policyTypeFilter.other().shouldBe(disabled);
    violations.shouldHaveSize(63);

    reportPage.filterPanel().closeButton().click();
  }

  @Test
  public void testViewVulnerabilitiesOptionDisabledInV4Report() throws IOException {
    // Setup
    final String SCAN_ID2 = "e16caf35769f4b3186a7e3476d34c2798";
    Application app2 = tempEntity.newApplicationWithParent();
    URL zippedReport = ReportHelper.zipReport("/canned-reports/evaluated-v4-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(app2.getId(), SCAN_ID2);
    FileUtils.copyURLToFile(zippedReport, reportDestination);
    tempEntity.newPolicyEvaluation(app2.getId(), Stage.ID_BUILD, SCAN_ID2);
    refreshOrOpen(ApplicationReportPage.url(app2, SCAN_ID2));

    reportPage.shouldBe(visible);
    // Assertions
    NxDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.button().click();
    optionsDropdown.menu().entries().get(3).shouldHave(DISABLED).click();
    // should remain on report page
    reportPage.shouldBe(visible);
  }

  @Test
  public void testBackNavigation() {
    // Test fully reopened page
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.backButton().click();
    waitUntilUrl(ReportListPage.url());

    /// Test when navigating from all reports page
    ReportListPage.firstRow().shouldBe(visible);
    ReportListPage.firstRow().buildReportLink().click();
    reportPage.backButton().click();
    waitUntilUrl(ReportListPage.url());
  }

  private void checkSecondarySortByNameDescending(final ElementsCollection violations) {
    violations.filterBy(matchesText("License-Banned")).shouldHave(texts("com.mycila", "com.vaadin"));
    violations.filterBy(matchesText("Security-High")).shouldHave(
            texts("com.fasterxml.jackson.core : jackson-core : 2.0.4",
                    "com.fasterxml.jackson.core : jackson-databind : 2.0.4"));
    violations.filterBy(matchesText("None"))
            .shouldHave(texts("com.adobe.acrobat", "com.adobe.pdf", "com.fasterxml", "com.palominolabs"));
  }

  private void activateGrandfathering() throws Exception {
    Policy licenseBanned = new PolicyDAO().getByName("License-Banned").get(0);

    app.setPolicyViolationGrandfatheringEnabled(true);
    licenseBanned.setPolicyViolationGrandfatheringAllowed(true);
    applicationDAO.update(app);
    policyDAO.update(licenseBanned);
    policyViolationGrandfatheringService.grandfather(app.getPublicId());
    evaluator.reevaluatePolicy();
    refresh();
  }

  private String getViolationForPolicyComponent(String policyName, ComponentIdentifier componentIdentifier) {
    List<PolicyViolation> policyViolations = policyViolationDAO.getByApplicationId(app.getId());

    if (CollectionUtils.isNotEmpty(policyViolations)) {
      return policyViolations.stream()
              .filter(pv -> pv.getPolicyName().equals(policyName)
                      && pv.getComponentIdentifier().equals(componentIdentifier))
              .findFirst()
              .map(PolicyViolation::getId)
              .orElse(null);
    }

    return null;
  }
}
