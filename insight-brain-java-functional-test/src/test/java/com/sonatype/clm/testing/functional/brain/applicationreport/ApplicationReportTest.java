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

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.IQDropdown;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.PolicyThreatLevelFilter;
import com.sonatype.clm.testing.functional.pages.ApplicationReportContainerPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.AppReportHeaders;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQCoverageIndicator;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQGrandfatheringIndicator;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.MatchStateFilter;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.PolicyTypeFilter;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.ProprietaryFilter;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.ViolationStateFilter;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.WaiverCip;
import com.sonatype.clm.testing.functional.pages.WaiverCip.AddWaiverDialog;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService;
import com.sonatype.insight.brain.policy.PolicyViolationPersistenceLocks;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import org.apache.commons.lang.ArrayUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.matchesText;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.DashboardFilters.ACTIVE;
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

  private PolicyViolationGrandfatheringService policyViolationGrandfatheringService =
      new PolicyViolationGrandfatheringService(applicationDAO, new OrganizationDAO(), policyDAO,
          new PolicyViolationDAO(), new PolicyViolationPersistenceLocks(), clmLicenseManager,
          new PolicyViolationLoggerFactory(clmLicenseManager));

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
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

  @Test
  public void testSummary() throws Exception {
    reportPage.shouldBe(visible);
    reportPage.reportTitle().shouldHave(text(app.getName() + " Build Report"));
    reportPage.reportDate().shouldHave(text(DateTime.now().toString("yyyy-MM-dd")));

    reportPage.optionsDropdown().shouldBe(visible).menu().shouldNotBe(visible);
    reportPage.threatIndicators().critical().shouldHave(text("22"));
    reportPage.threatIndicators().severe().shouldHave(text("38"));
    reportPage.threatIndicators().moderate().shouldHave(text("4"));
    reportPage.threatIndicators().caption().shouldHave(exactText("64 Violations"));
    reportPage.threatIndicators().subCaption().shouldHave(exactText("Affecting 26 components"));

    IQCoverageIndicator coverageIndicator = reportPage.coverageIndicator();
    coverageIndicator.caption().shouldHave(exactText("63 COMPONENTS"));
    coverageIndicator.subCaption().shouldHave(exactText("97% of all components identified"));
    coverageIndicator.donutChart().shouldBe(visible);

    IQGrandfatheringIndicator grandfatheringIndicator = reportPage.grandfatheringIndicator();
    grandfatheringIndicator.caption().shouldHave(exactText("0 Grandfathered"));

    activateGrandfathering();

    grandfatheringIndicator = reportPage.grandfatheringIndicator();
    grandfatheringIndicator.caption().shouldHave(exactText("45 Grandfathered"));
  }

  @Test
  public void testOptionsMenu() {
    IQDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.shouldBe(visible).menu().shouldNotBe(visible);
    optionsDropdown.button().shouldHave(text("Options")).click();
    optionsDropdown.menu().shouldBe(visible).entries().shouldHaveSize(1); // increase to 2 after CLM-11857

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testDownloadPdf() throws Exception {
    IQDropdown optionsDropdown = reportPage.optionsDropdown();
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
    assertThat(fileBeginning).isEqualTo(new byte[] { 0x25, 0x50, 0x44, 0x46 });
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
    reportPage.headers().policyNameFilterInput().clear();
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
    reportPage.headers().componentNameFilterInput().clear();
    reportPage.showAllViolationsRadio().click();
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

    reportPage.showAllViolationsRadio().click();
    reportPage.headers().policyNameFilterInput().setValue(licenseBanned.getName());
    reportPage.headers().componentNameFilterInput().clear();
    reportPage.resultRows().shouldHaveSize(2);
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);
    reportPage.resultRow(1).grandfatheredIndicator().shouldBe(visible);

    eyesWatcher.eyesCheck();

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

    reportPage.showAllViolationsRadio().click();
    reportPage.headers().policyNameFilterInput().setValue(licenseBanned.getName());
    reportPage.headers().componentNameFilterInput().clear();
    reportPage.resultRows().shouldHaveSize(2);
    reportPage.resultRow(1).waivedIndicator().shouldNotBe(visible);
    reportPage.resultRow(1).grandfatheredIndicator().shouldBe(visible);
  }

  @Test
  public void testAggregation() {
    reportPage.showAggregatedViolationsRadio().shouldBe(selected);
    reportPage.showAllViolationsRadio().shouldNotBe(selected);
    reportPage.resultRows().shouldHaveSize(63);
    reportPage.getThreatBars("critical").shouldHaveSize(17);
    reportPage.getThreatBars("severe").shouldHaveSize(8);
    reportPage.getThreatBars("moderate").shouldHaveSize(1);
    reportPage.getThreatBars("low").shouldHaveSize(0);
    reportPage.getThreatBars("ignore").shouldHaveSize(36);
    reportPage.headers().componentNameFilterInput().setValue("commons-fileupload");
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.getThreatBars("critical").shouldHaveSize(1);

    reportPage.showAllViolationsRadio().click();
    reportPage.showAggregatedViolationsRadio().shouldNotBe(selected);
    reportPage.showAllViolationsRadio().shouldBe(selected);

    reportPage.resultRows().shouldHaveSize(6);
    reportPage.getThreatBars("critical").shouldHaveSize(4);
    reportPage.getThreatBars("severe").shouldHaveSize(1);
    reportPage.getThreatBars("moderate").shouldHaveSize(1);

    reportPage.headers().componentNameFilterInput().clear();
    reportPage.resultRows().shouldHaveSize(101);
    reportPage.getThreatBars("critical").shouldHaveSize(22);
    reportPage.getThreatBars("severe").shouldHaveSize(38);
    reportPage.getThreatBars("moderate").shouldHaveSize(4);
    reportPage.getThreatBars("low").shouldHaveSize(0);
    reportPage.getThreatBars("ignore").shouldHaveSize(36);
  }

  @Test
  public void testSorting() {
    AppReportHeaders headers = reportPage.headers();
    ElementsCollection violations = reportPage.resultRows();
    // reduce the result set so we don't need to scroll around
    headers.componentNameFilterInput().setValue("com.");
    violations.shouldHaveSize(8);

    // by threat level
    headers.threatHeader().sortArrowDown().shouldBeSelected();
    violations.shouldHave(texts("10", "10", "9", "9", "0", "0", "0", "0"));
    // check that entries have also been sorted by component name
    checkSecondarySortByNameDescending(violations);
    // reverse threat level
    headers.threatHeader().click();
    headers.threatHeader().sortArrowUp().shouldBeSelected();
    violations.shouldHave(texts("0", "0", "0", "0", "9", "9", "10", "10"));
    // the secondary sort should remain unchanged
    checkSecondarySortByNameDescending(violations);

    // by policy name
    headers.policyNameHeader().click();
    headers.policyNameHeader().sortArrowUp().shouldBeSelected();
    violations.shouldHave(
        texts("License-Banned", "License-Banned", "None", "None", "None", "None", "Security-High", "Security-High"));
    checkSecondarySortByNameDescending(violations);
    headers.policyNameHeader().click();
    headers.policyNameHeader().sortArrowDown().shouldBeSelected();
    violations.shouldHave(
        texts("Security-High", "Security-High", "None", "None", "None", "None", "License-Banned", "License-Banned"));
    checkSecondarySortByNameDescending(violations);

    // by component name
    reportPage.showAllViolationsRadio().click(); // un-aggregate in order to check secondary sort
    headers.componentNameHeader().click();
    headers.componentNameHeader().sortArrowUp().shouldBeSelected();
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
    headers.componentNameHeader().sortArrowDown().shouldBeSelected();
    ArrayUtils.reverse(componentNamesAlpha);
    violations.shouldHave(texts(componentNamesAlpha));
    // secondary sort should remain unchanged
    violations.filterBy(matchesText("jackson-core")).shouldHave(texts("9", "7"));
  }

  @Test
  public void testFiltering() {
    AppReportHeaders headers = reportPage.headers();
    ElementsCollection violations = reportPage.resultRows();

    headers.policyNameFilterInput().setValue("unk");

    violations.shouldHaveSize(1);
    violations.shouldHave(texts("Component-Unknown"));
    violations.shouldHave(texts("RegexMatch.dll"));

    headers.componentNameFilterInput().setValue("org.slf4j");

    violations.shouldHaveSize(1);
    violations.shouldHave(texts("No Results"));

    eyesWatcher.eyesCheck("Test Filtering No Results");

    headers.policyNameFilterInput().clear();
    violations.shouldHaveSize(3);
    violations.shouldHave(texts("None", "None", "None"));
    violations.shouldHave(texts("org.slf4j : jcl-over-slf4j", "org.slf4j : slf4j-api", "org.slf4j : slf4j-log4j12"));

    // test filtering across colon-separate fields in component name
    headers.componentNameFilterInput().setValue("org.slf4j : slf4j-");

    violations.shouldHaveSize(2);
    violations.shouldHave(texts("None", "None"));
    violations.shouldHave(texts("org.slf4j : slf4j-api", "org.slf4j : slf4j-log4j12"));

    ProprietaryFilter proprietaryFilter = reportPage.proprietaryFilter();

    proprietaryFilter.counter().shouldHave(text("2"));
    proprietaryFilter.multiSelectList().shouldBe(empty);
    proprietaryFilter.twisty().click();
    proprietaryFilter.multiSelectList().shouldHaveSize(3);
    proprietaryFilter.proprietary().click();

    proprietaryFilter.counter().shouldHave(text("1 of 2"));
    proprietaryFilter.proprietary().shouldBe(selected);
    proprietaryFilter.nonProprietary().shouldNotBe(selected);

    eyesWatcher.eyesCheck("Test Proprietary Filter");

    violations.shouldHaveSize(1);
    violations.shouldHave(texts("No Results"));

    headers.componentNameFilterInput().clear();

    violations.shouldHaveSize(3);
    violations.shouldHave(texts("full.jar", "org.apache.tiles : tiles-api", "org.apache.tiles : tiles-core"));

    proprietaryFilter.allItems().click();
    proprietaryFilter.counter().shouldHave(text("2 of 2"));
    proprietaryFilter.proprietary().shouldBe(selected);
    proprietaryFilter.nonProprietary().shouldBe(selected);

    violations.shouldHaveSize(63);

    proprietaryFilter.allItems().click();
    proprietaryFilter.counter().shouldHave(text("2"));
    proprietaryFilter.proprietary().shouldNotBe(selected);
    proprietaryFilter.nonProprietary().shouldNotBe(selected);

    violations.shouldHaveSize(63);

    proprietaryFilter.nonProprietary().click();
    proprietaryFilter.counter().shouldHave(text("1 of 2"));
    proprietaryFilter.proprietary().shouldNotBe(selected);
    proprietaryFilter.nonProprietary().shouldBe(selected);

    violations.shouldHaveSize(60);

    // match state filter
    MatchStateFilter matchStateFilter = reportPage.matchStateFilter();
    matchStateFilter.counter().shouldHave(exactText("3"));
    matchStateFilter.multiSelectList().shouldBe(empty);
    matchStateFilter.twisty().click();
    matchStateFilter.multiSelectList().shouldHaveSize(4);

    matchStateFilter.similar().click();
    matchStateFilter.similar().shouldBe(selected);
    matchStateFilter.counter().shouldHave(exactText("1 of 3"));
    violations.shouldHaveSize(1);
    violations.first().shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));

    matchStateFilter.unknown().click();
    matchStateFilter.unknown().shouldBe(selected);
    matchStateFilter.counter().shouldHave(exactText("2 of 3"));
    violations.shouldHaveSize(2);
    violations.shouldHave(texts("apache-httpclient : commons-httpclient : 3.1", "RegexMatch.dll"));
    eyesWatcher.eyesCheck("Test Component Match State Filter");

    matchStateFilter.exact().click();
    matchStateFilter.exact().shouldBe(selected);
    matchStateFilter.counter().shouldHave(exactText("3 of 3"));
    violations.shouldHaveSize(60);

    //policy type filter
    PolicyTypeFilter policyTypeFilter = reportPage.policyTypeFilter();
    policyTypeFilter.counter().shouldHave(exactText("4"));
    policyTypeFilter.multiSelectList().shouldBe(empty);
    policyTypeFilter.twisty().click();
    policyTypeFilter.multiSelectList().shouldHaveSize(5);

    policyTypeFilter.quality().click();
    policyTypeFilter.quality().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("1 of 4"));
    violations.shouldHaveSize(1);
    violations.first().shouldHave(exactText("No Results"));

    policyTypeFilter.license().click();
    policyTypeFilter.license().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("2 of 4"));
    violations.shouldHaveSize(3);
    violations.shouldHave(texts(
        "com.mycila : license-maven-plugin : 2.11",
        "com.vaadin.addon : vaadin-touchkit-agpl : 3.0.0-beta1",
        "xpp3 : xpp3_min : 1.1.4c"
    ));

    policyTypeFilter.other().click();
    policyTypeFilter.other().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("3 of 4"));
    violations.shouldHaveSize(5);
    violations.shouldHave(texts(
        "com.mycila : license-maven-plugin : 2.11",
        "com.vaadin.addon : vaadin-touchkit-agpl : 3.0.0-beta1",
        "xpp3 : xpp3_min : 1.1.4c",
        "RegexMatch.dll",
        "junit : junit : 4.8.1"
    ));
    eyesWatcher.eyesCheck("Test Policy Threat Level Filter");

    policyTypeFilter.security().click();
    policyTypeFilter.security().shouldBe(selected);
    policyTypeFilter.counter().shouldHave(exactText("4 of 4"));
    violations.shouldHaveSize(27);

    policyTypeFilter.allItems().click();
    policyTypeFilter.allItems().shouldNotBe(selected);
    violations.shouldHaveSize(60);
    
    // policy threat level filter
    PolicyThreatLevelFilter threatLevelFilter = DashboardFilters.policyThreatLevelFilter();
    threatLevelFilter.counter().shouldBe(visible).shouldBe(ACTIVE).shouldHave(text("0 – 10"));
    threatLevelFilter.slider().shouldBe(hidden);
    threatLevelFilter.twisty().click();
    threatLevelFilter.slider().shouldBe(visible);
    threatLevelFilter.slider().setValues(1, 10);
    violations.shouldHaveSize(27);
    threatLevelFilter.slider().setValues(1, 9);
    violations.shouldHaveSize(25);
    eyesWatcher.eyesCheck("Test Policy Threat Level Filter");
    threatLevelFilter.slider().setValues(2, 9);
    violations.shouldHaveSize(24);
    threatLevelFilter.slider().setValues(7, 9);
    violations.shouldHaveSize(23);
    threatLevelFilter.slider().setValues(9, 9);
    violations.shouldHaveSize(15);
    threatLevelFilter.slider().setValues(10, 10);
    violations.shouldHaveSize(2);
    threatLevelFilter.slider().setValues(3, 6);
    violations.shouldHaveSize(1);
    violations.shouldHave(texts("No Results"));
    threatLevelFilter.slider().setValues(0, 10);
    violations.shouldHaveSize(60);
    threatLevelFilter.twisty().click();
    threatLevelFilter.slider().shouldBe(hidden);
  }

  @Test
  public void testFiltering_violationState() throws Exception {
    ElementsCollection violations = reportPage.resultRows();

    ViolationStateFilter violationStateFilter = reportPage.violationStateFilter();
    violationStateFilter.counter().shouldHave(exactText("4"));
    violationStateFilter.multiSelectList().shouldBe(empty);
    violationStateFilter.twisty().click();
    violationStateFilter.multiSelectList().shouldHaveSize(5);

    violationStateFilter.open().click();
    violationStateFilter.open().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("1 of 4"));
    violations.shouldHaveSize(27);
    violations.first().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));

    // waive the first violation
    violations.first().click();
    CipModal cipModal = reportPage.cipModal();
    cipModal.shouldBe(visible);
    cipModal.tabLink(2).shouldHave(text("Policy")).click();
    WaiverCip.row(0).waiveButton().click();
    AddWaiverDialog.saveButton().click();
    cipModal.closeButton().click();
    reportPage.reevaluateButton().click();
    FormMask.seeAndWaitForDismissal();

    // waived violation filtered out
    violations.shouldHaveSize(26);

    // now add waived violations
    violationStateFilter.waived().click();
    violationStateFilter.waived().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("2 of 4"));
    violations.shouldHaveSize(27);

    // at this point, the mycila violation is visible but is way down at the "None" part of the list because we are
    // in the aggregated view
    violations.first().shouldHave(text("com.vaadin.addon : vaadin-touchkit-agpl : 3.0.0-beta1"));

    // switch to non-aggregated view to get the actual waived violation, back in its original place at the top of the
    // list
    reportPage.showAllViolationsRadio().click();
    violations.shouldHaveSize(65);
    violations.first().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));

    eyesWatcher.eyesCheck("Test Violation State Filter");

    activateGrandfathering();

    // activateGrandfathering refreshes the page so we need to put the filter back how we had it
    reportPage.showAllViolationsRadio().click();
    violationStateFilter.twisty().click();
    violationStateFilter.open().click();
    violationStateFilter.waived().click();

    // grandfathered violations not visible
    violations.shouldHaveSize(21);

    // grandfathered violations now visible
    violationStateFilter.grandfathered().click();
    violationStateFilter.grandfathered().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("3 of 4"));
    violations.shouldHaveSize(65);

    // the waived violation is also grandfathered, so no difference.
    violationStateFilter.waived().click();
    violations.shouldHaveSize(65);

    violationStateFilter.notViolating().click();
    violationStateFilter.notViolating().shouldBe(selected);
    violationStateFilter.counter().shouldHave(exactText("3 of 4"));
    violations.shouldHaveSize(101);

    // all boxes checked - again no difference in count because the waived violation is also grandfathered
    violationStateFilter.waived().click();
    violationStateFilter.counter().shouldHave(exactText("4 of 4"));
    violations.shouldHaveSize(101);

    // no boxes checked
    violationStateFilter.allItems().shouldBe(selected).click();
    violationStateFilter.allItems().shouldNotBe(selected);
    violationStateFilter.notViolating().shouldNotBe(selected);
    violationStateFilter.open().shouldNotBe(selected);
    violationStateFilter.waived().shouldNotBe(selected);
    violationStateFilter.grandfathered().shouldNotBe(selected);
    violationStateFilter.counter().shouldHave(exactText("4"));
    violations.shouldHaveSize(101);
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

    reportPage.showAllViolationsRadio().click();
    reportPage.showAllViolationsRadio().shouldBe(selected);
    headers.policyNameHeader().click();
    headers.policyNameHeader().sortArrowUp().shouldBeSelected();
    reportPage.proprietaryFilter().twisty().click();
    reportPage.proprietaryFilter().nonProprietary().click();
    reportPage.proprietaryFilter().nonProprietary().shouldBe(selected);

    // navigate elsewhere and then back to this report, without triggering a full refresh
    MainHeader.reportingNavigationButton().click();
    ReportListPage.firstRow().buildReportLink().click();
    ApplicationReportContainerPage.policyCentricAppReportPreviewLink().shouldBe(visible).click();

    reportPage.reportTitle().shouldHave(text(app.getName() + " Build Report"));
    reportPage.showAllViolationsRadio().shouldNotBe(selected);
    headers.policyNameHeader().sortArrowUp().shouldNotBeSelected();
    reportPage.proprietaryFilter().nonProprietary().shouldNotBe(selected);
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
}
