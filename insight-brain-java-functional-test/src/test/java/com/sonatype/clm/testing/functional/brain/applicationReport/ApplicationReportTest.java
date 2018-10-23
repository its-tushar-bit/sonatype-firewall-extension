/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationReport;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.IQDropdown;
import com.sonatype.clm.testing.functional.elements.VersionsCIP;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQCoverageIndicator;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.WaiverCip;
import com.sonatype.clm.testing.functional.pages.WaiverCip.AddWaiverDialog;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ConfirmRemoveWaiverDialog;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ExistingWaiver;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ViewWaiversDialog;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal.ACTIVE_CLASS;

public class ApplicationReportTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  private Policy policy;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.URL);
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    app = tempEntity.newApplicationWithParent("ApplicationReportTest", "ApplicationReportTest");
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    Constraint constraint = new Constraint("C1", "All coordinates", LogicalOperator.AND);
    constraint.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:javancss*"));
    policy = tempEntity.newPolicy("ApplicationReportTest Policy", constraint);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @Test
  public void testSummary() {
    reportPage.shouldBe(visible);
    reportPage.reportTitle().shouldHave(text(app.getName() + " Build Report"));
    reportPage.reportDate().shouldHave(text(DateTime.now().toString("yyyy-MM-dd")));
    reportPage.optionsDropdown().shouldBe(visible).menu().shouldNotBe(visible);
    reportPage.threatIndicators().critical().shouldHave(text("0"));
    reportPage.threatIndicators().severe().shouldHave(text("1"));
    reportPage.threatIndicators().moderate().shouldHave(text("0"));
    reportPage.threatIndicators().caption().shouldHave(exactText("1 Violation"));
    reportPage.threatIndicators().subCaption().shouldHave(exactText("Affecting 1 component"));

    IQCoverageIndicator coverageIndicator = reportPage.coverageIndicator();
    coverageIndicator.caption().shouldHave(exactText("4 COMPONENTS"));
    coverageIndicator.subCaption().shouldHave(exactText("100% of all components identified"));
    coverageIndicator.donutChart().shouldBe(visible);
  }

  @Test
  public void testOptionsMenu() {
    IQDropdown optionsDropdown = reportPage.optionsDropdown();
    optionsDropdown.shouldBe(visible).menu().shouldNotBe(visible);
    optionsDropdown.button().shouldHave(text("Options")).click();
    optionsDropdown.menu().shouldBe(visible).entries()
        .shouldHave(texts("Re-Evaluate Report", "Generate PDF", "View raw data"));
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testResults() {
    reportPage.resultRows().shouldHaveSize(4);
    reportPage.resultRow(1).threatBar().shouldHave(cssClass("severe"));
    reportPage.resultRow(1).threatNumber().shouldHave(text("5"));
    reportPage.resultRow(1).policyName().shouldHave(text(policy.getName()));
    for (int i = 2; i <= 4; i++) {
      reportPage.resultRow(i).threatBar().shouldHave(cssClass("ignore"));
      reportPage.resultRow(i).threatNumber().shouldHave(text("0"));
      reportPage.resultRow(i).policyName().shouldHave(text("None"));
    }
    reportPage.resultRow(1).componentName().shouldHave(text("javancss : javancss : 29.50"));
    reportPage.resultRow(2).componentName().shouldHave(text("ch.qos.logback : logback-access : 0.6"));
    reportPage.resultRow(3).componentName().shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));
    reportPage.resultRow(4).componentName().shouldHave(text("org.apache.geronimo.framework : geronimo-security : 2.1"));
  }

  @Test
  public void testCIP() throws Exception {
    setupHdsResponse();
    CipModal cipModal = reportPage.cipModal();

    // Close, Prev and Next buttons
    reportPage.resultRow(1).click();
    cipModal.getElement().shouldBe(visible);

    cipModal.header().shouldHave(exactText("javancss : javancss : 29.50"));
    cipModal.previousButton().shouldBe(disabled);
    cipModal.nextButton().shouldBe(enabled).click();

    cipModal.header().shouldHave(exactText("ch.qos.logback : logback-access : 0.6"));
    cipModal.previousButton().shouldBe(enabled);
    cipModal.nextButton().shouldBe(enabled).click();
    cipModal.closeButton().click();
    cipModal.getElement().shouldBe(hidden);

    reportPage.resultRow(4).click();
    cipModal.getElement().shouldBe(visible);

    cipModal.header().shouldHave(exactText("org.apache.geronimo.framework : geronimo-security : 2.1"));
    cipModal.nextButton().shouldBe(disabled);
    cipModal.previousButton().shouldBe(enabled).click();

    cipModal.header().shouldHave(exactText("org.mortbay.jetty : jetty : 6.1.15"));
    cipModal.nextButton().shouldBe(enabled);
    cipModal.previousButton().shouldBe(enabled);
    cipModal.closeButton().click();
    cipModal.getElement().shouldBe(hidden);

    testComponentInfoTab();
    testPolicyTab();
  }

  private void testComponentInfoTab() {
    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(1).click();
    cipModal.getElement().shouldBe(visible);
    cipModal.tabLink(1).shouldHave(ACTIVE_CLASS);
    VersionsCIP.groupId().shouldHave(text("critical"));
    VersionsCIP.artifactId().shouldHave(text("threat"));
    VersionsCIP.version().shouldHave(text("1.0"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Apache-2.0"));
    VersionsCIP.observedLicenses().shouldHave(texts("GPL-2.0"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Apache-2.0", "GPL-2.0"));
    VersionsCIP.highestSecurityThreat().shouldHave(text("9.1"), cssClass("critical"));
    VersionsCIP.securityCount().shouldHave(text("3"));
    VersionsCIP.matchState().shouldHave(text("exact"));
    VersionsCIP.identificationSource().shouldHave(text("Sonatype"));
    VersionsCIP.showDetailsLink().shouldBe(visible).click();
    VersionsCIP.hideDetailsLink().shouldBe(visible);
  }

  private void testPolicyTab() throws Exception {
    CipModal cipModal = reportPage.cipModal();
    cipModal.tabLink(2).shouldNotHave(ACTIVE_CLASS).click();
    cipModal.tabLink(2).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(1).shouldNotHave(ACTIVE_CLASS);
    WaiverCip.rows().shouldHaveSize(1);
    WaiverCip.row(0).shouldBe(
        "cip-policy-orange",
        "ApplicationReportTest Policy",
        new String[] { "All coordinates" },
        new String[] { "Coordinates were javancss : javancss : 29.50" });

    // check that there are no existing waivers
    WaiverCip.viewWaivers().shouldBe(visible).click();
    ViewWaiversDialog.rows().shouldHaveSize(0);
    ViewWaiversDialog.closeButton().click();

    // Waive violation
    WaiverCip.row(0).waiveButton().shouldBe(visible).click();
    AddWaiverDialog.scopeContainer().shouldBe(visible);
    AddWaiverDialog.scope(app.getPublicId()).shouldBe(visible, selected);
    AddWaiverDialog.scope(app.getOrganizationId()).shouldBe(visible).shouldNotBe(selected);
    AddWaiverDialog.scope(Organization.ROOT_ORGANIZATION_ID).shouldBe(visible).shouldNotBe(selected);

    AddWaiverDialog.allComponents().shouldBe(visible).shouldNotBe(selected);
    AddWaiverDialog.selectedComponent().shouldBe(visible, selected);
    AddWaiverDialog.selectedComponent().parent()
        .shouldHave(exactText("Selected component (javancss : javancss : 29.50)"));

    AddWaiverDialog.comment().setValue("TEST COMMENT");
    AddWaiverDialog.saveButton().shouldBe(visible, enabled).click();

    // check that there is new waiver
    WaiverCip.viewWaivers().shouldBe(visible).click();
    ViewWaiversDialog.rows().shouldHaveSize(1);
    ExistingWaiver waiver = ViewWaiversDialog.row(0);
    waiver.policy().shouldHave(text("ApplicationReportTest Policy"));
    waiver.owner().shouldHave(text("ApplicationReportTest"));
    waiver.comment().shouldHave(text("TEST COMMENT"));
    waiver.removeButton().shouldBe(visible, enabled);

    ViewWaiversDialog.closeButton().click();
    cipModal.closeButton().click();

    // check that policy has been waived
    evaluator.reevaluatePolicy();
    reportPage.resultRow(1).click();
    cipModal.tabLink(2).click();
    WaiverCip.rows().shouldHaveSize(1);
    WaiverCip.rows().get(0).shouldHave(text("No Policy Violations"));

    // Remove waiver
    WaiverCip.viewWaivers().shouldBe(visible).click();
    ViewWaiversDialog.rows().shouldHaveSize(1);
    ViewWaiversDialog.row(0).removeButton().shouldBe(visible, enabled).click();
    ConfirmRemoveWaiverDialog.removeButton().shouldBe(visible, enabled).click();
    ViewWaiversDialog.closeButton().click();
    cipModal.closeButton().click();

    // check that violation has been un-waived
    evaluator.reevaluatePolicy();
    reportPage.resultRow(1).click();
    cipModal.tabLink(2).click();
    WaiverCip.rows().shouldHaveSize(1);
    WaiverCip.row(0).policyName().shouldHave(text("ApplicationReportTest Policy"));
  }

  @Test
  public void testWaivedIndicator() throws Exception {
    reportPage.resultRow(1).threatNumber().shouldHave(text("5"));
    reportPage.resultRow(1).waivedIndicator().shouldNotBe(visible);

    tempEntity.newWaiver(policy.getId(), app.getId());
    evaluator.reevaluatePolicy();
    refresh();

    // TODO check waived row (or lack thereof) when aggregating
    reportPage.showAllViolationsRadio().click();
    reportPage.showAllViolationsRadio().shouldBe(selected);

    reportPage.resultRows().shouldHaveSize(4);
    reportPage.resultRow(1).waivedIndicator().shouldBe(visible);

    reportPage.threatIndicators().severe().shouldHave(text("0"));
    reportPage.threatIndicators().caption().shouldHave(exactText("0 Violations"));
    reportPage.threatIndicators().subCaption().shouldHave(exactText("Affecting 0 components"));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testAggregation() {
    reportPage.showAggregatedViolationsRadio().shouldBe(selected);
    reportPage.showAllViolationsRadio().shouldNotBe(selected);

    reportPage.showAllViolationsRadio().click();

    reportPage.showAggregatedViolationsRadio().shouldNotBe(selected);
    reportPage.showAllViolationsRadio().shouldBe(selected);

    reportPage.resultRows().shouldHaveSize(4);
    reportPage.resultRow(1).threatBar().shouldHave(cssClass("severe"));
    reportPage.resultRow(1).threatNumber().shouldHave(text("5"));
    reportPage.resultRow(1).policyName().shouldHave(text(policy.getName()));
    for (int i = 2; i <= 4; i++) {
      reportPage.resultRow(i).threatBar().shouldHave(cssClass("ignore"));
      reportPage.resultRow(i).threatNumber().shouldHave(text("0"));
      reportPage.resultRow(i).policyName().shouldHave(text("None"));
    }
    reportPage.resultRow(1).componentName().shouldHave(text("javancss : javancss : 29.50"));
    reportPage.resultRow(2).componentName().shouldHave(text("ch.qos.logback : logback-access : 0.6"));
    reportPage.resultRow(3).componentName().shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));
    reportPage.resultRow(4).componentName().shouldHave(text("org.apache.geronimo.framework : geronimo-security : 2.1"));
  }

  private void setupHdsResponse() {
    testCLMServer.getHdsServer().setResponseForURI("rest/ci/componentDetails",
        getClass().getClassLoader().getResource("componentDetails/componentDetails.json"), 200);
    testCLMServer.getHdsServer().setResponseForURI("rest/ci/componentDetails/list",
        getClass().getClassLoader().getResource("componentDetails/componentDetailsList.json"), 200);
  }
}
