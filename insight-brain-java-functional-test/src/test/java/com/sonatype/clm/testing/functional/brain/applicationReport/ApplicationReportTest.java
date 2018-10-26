/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationReport;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.IQDropdown;
import com.sonatype.clm.testing.functional.elements.LabelsCIP;
import com.sonatype.clm.testing.functional.elements.LabelsCIP.AddLabelModal;
import com.sonatype.clm.testing.functional.elements.LabelsCIP.RemoveLabelModal;
import com.sonatype.clm.testing.functional.elements.VersionsCIP;
import com.sonatype.clm.testing.functional.elements.reports.LicenseCIP;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.AppReportHeaders;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.IQCoverageIndicator;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage;
import com.sonatype.clm.testing.functional.pages.WaiverCip;
import com.sonatype.clm.testing.functional.pages.WaiverCip.AddWaiverDialog;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ConfirmRemoveWaiverDialog;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ExistingWaiver;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ViewWaiversDialog;
import com.sonatype.clm.testing.functional.utils.ReportHelper;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.service.InsightWork;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal.ACTIVE_CLASS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;

public class ApplicationReportTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private static final ComponentIdentifier JAVANCSS_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("javancss",
      "javancss", "29.50");
  private static final String JAVANCSS_HASH = "9aba4af169a1a3baa67f";

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
    reportPage.resultRow(3).componentName().shouldHave(text("org.apache.geronimo.framework : geronimo-security : 2.1"));
    reportPage.resultRow(4).componentName().shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));
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

    cipModal.header().shouldHave(exactText("org.mortbay.jetty : jetty : 6.1.15"));
    cipModal.nextButton().shouldBe(disabled);
    cipModal.previousButton().shouldBe(enabled).click();

    cipModal.header().shouldHave(exactText("org.apache.geronimo.framework : geronimo-security : 2.1"));
    cipModal.nextButton().shouldBe(enabled);
    cipModal.previousButton().shouldBe(enabled);
    cipModal.closeButton().click();
    cipModal.getElement().shouldBe(hidden);

    testComponentInfoTab();
    testPolicyTab();
    testLicensesTab();
    testLabelsTab();
  }

  private void testComponentInfoTab() {
    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(1).click();
    cipModal.getElement().shouldBe(visible);
    cipModal.tabLink(1).shouldHave(ACTIVE_CLASS);
    VersionsCIP.groupId().shouldHave(text("javancss"));
    VersionsCIP.artifactId().shouldHave(text("javancss"));
    VersionsCIP.version().shouldHave(text("29.50"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Apache-2.0"));
    VersionsCIP.observedLicenses().shouldHave(texts("GPL-2.0"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Apache-2.0", "GPL-2.0"));
    VersionsCIP.highestSecurityThreat().shouldHave(text("NA"), cssClass("unspecified"));
    VersionsCIP.matchState().shouldHave(text("exact"));
    VersionsCIP.identificationSource().shouldHave(text("Sonatype"));
    VersionsCIP.showDetailsLink().shouldBe(visible).click();
    VersionsCIP.hideDetailsLink().shouldBe(visible);
    cipModal.closeButton().click();
  }

  private void testPolicyTab() throws Exception {
    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(1).click();
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
    cipModal.closeButton().click();
  }

  private void testLicensesTab() {
    reportPage.resultRow(1).click();
    CipModal cipModal = reportPage.cipModal();
    cipModal.tabLink(5).shouldNotHave(ACTIVE_CLASS).click();
    cipModal.tabLink(5).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(1).shouldNotHave(ACTIVE_CLASS);

    // License sidebar
    LicenseCIP.declaredLicenses().shouldHave(LicenseCIP.licenseThreats(0), texts("Apache-2.0"));
    LicenseCIP.observedLicenses().shouldHave(LicenseCIP.licenseThreats(9), texts("GPL-2.0"));
    LicenseCIP.effectiveLicenses().shouldHave(LicenseCIP.licenseThreats(0, 9), texts("Apache-2.0", "GPL-2.0"));

    // Editor default state
    LicenseCIP.scopes().shouldHave(texts("ApplicationReportTest", "ApplicationReportTest", "Root Organization"));
    LicenseCIP.scope().shouldHave(value("string:ApplicationReportTest"));
    LicenseCIP.statuses().shouldHave(
        texts("Open", "Acknowledged", "Overridden", "Selected", "Confirmed", "Inherit Status (Open)"));
    LicenseCIP.status().shouldHave(value("Open"));
    LicenseCIP.licenseSelector().shouldNot(exist);
    LicenseCIP.updateButton().shouldNotBe(enabled);

    // Update to Selected state
    LicenseCIP.status().selectOption("Selected");
    LicenseCIP.licenseSelector().button().shouldBe(visible).click();
    LicenseCIP.licenseSelector().entries().shouldHave(texts("Apache-2.0", "GPL-2.0"));
    LicenseCIP.licenseSelector().entry(0).click();
    LicenseCIP.licenseSelector().button().click();
    LicenseCIP.comment().setValue("not bad");
    LicenseCIP.updateButton().shouldBe(enabled).click();

    // Check for our override
    LicenseCIP.effectiveLicenses().shouldHave(texts("Apache-2.0"));
    LicenseCIP.scope().shouldHave(value("string:ApplicationReportTest"));
    LicenseCIP.status().shouldHave(value("SELECTED"));
    LicenseCIP.licenseSelector().should(exist);
    LicenseCIP.updateButton().shouldNotBe(enabled);

    // Verify override on backend
    final LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    LicenseOverride override = licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), JAVANCSS_IDENTIFIER);
    assertThat(override.getStatus(), is(LicenseOverrideStatus.SELECTED));
    assertThat(override.getLicenseIds(), is(Collections.singleton("Apache-2.0")));

    // remove
    LicenseCIP.status().selectOption("Inherit Status (Open)");
    LicenseCIP.updateButton().shouldBe(enabled).click();

    RepositoryReportPage.waitForComponentUpdater();

    LicenseCIP.updateButton().shouldBe(disabled);
    override = licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), JAVANCSS_IDENTIFIER);
    assertNull(override);

    cipModal.closeButton().click();
  }

  private void testLabelsTab() throws Exception {
    Label elMagnifico = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "El Magnifico", Color.dark_blue);
    Label elJunko = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "El Junko", Color.dark_red);
    tempEntity.newComponentLabel(Organization.ROOT_ORGANIZATION_ID, elMagnifico.getId(), JAVANCSS_HASH);

    createPolicy(1, "Bad Label", LabelConditionType.ID, "is", elJunko.getId());

    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(1).click();
    cipModal.tabLink(7).shouldNotHave(ACTIVE_CLASS).click();
    cipModal.tabLink(7).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(1).shouldNotHave(ACTIVE_CLASS);

    LabelsCIP.appliedLabels().shouldHaveSize(1);
    LabelsCIP.appliedLabel(1).shouldHave(text("El Magnifico"), LabelsCIP.Label.color(Color.dark_blue)).action()
        .should(exist);

    LabelsCIP.availableLabels().shouldHaveSize(1);
    LabelsCIP.availableLabel(1).shouldHave(text("El Junko"), LabelsCIP.Label.color(Color.dark_red)).action()
        .click();

    // Modal
    AddLabelModal.root().shouldBe(visible);
    AddLabelModal.scopes().shouldHaveSize(3);
    AddLabelModal.saveButton().click();
    AddLabelModal.root().shouldBe(hidden);

    // label persisted
    assertThat(new ComponentLabelDAO().getByOwnerIdAndHash(app.getId(), JAVANCSS_HASH).size(), is(2));

    // Check new policy violation was added
    evaluator.reevaluatePolicy();
    cipModal.tabLink(2).click();
    WaiverCip.rows().shouldHaveSize(2);
    WaiverCip.row(1).shouldBe(
        "cip-policy-darkblue",
        "Bad Label",
        new String[] { "Bad Label constraint" },
        new String[] { "Found label 'El Junko'" });

    // Remove the label we added
    cipModal.tabLink(7).click();
    LabelsCIP.appliedLabels().shouldHaveSize(2);
    LabelsCIP.appliedLabel(1).shouldHave(text("El Junko")).action().click();

    // Confirmation modal
    RemoveLabelModal.root().should(appear);
    RemoveLabelModal.confirmButton().click();
    RemoveLabelModal.root().should(disappear);

    // backend check that it was removed
    List<ComponentLabel> appliedLabels = new ComponentLabelDAO().getByOwnerIdAndHash(app.getId(), JAVANCSS_HASH);
    assertThat(appliedLabels.size(), is(1));
    assertThat(appliedLabels.get(0).getLabelId(), is(elMagnifico.getId()));

    // Check new policy violation is gone
    evaluator.reevaluatePolicy();
    cipModal.tabLink(2).click();
    WaiverCip.rows().shouldHaveSize(1);

    cipModal.closeButton().click();
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
    reportPage.resultRow(3).componentName().shouldHave(text("org.apache.geronimo.framework : geronimo-security : 2.1"));
    reportPage.resultRow(4).componentName().shouldHave(text("org.mortbay.jetty : jetty : 6.1.15"));
  }

  @Test
  public void testSorting() {
    AppReportHeaders headers = reportPage.headers();
    ElementsCollection violations = reportPage.resultRows();
    // by threat level
    headers.threatHeader().sortArrowDown().shouldBeSelected();
    violations.shouldHave(texts("5", "0", "0", "0"));
    // check that '0' entries have also been sorted by component name
    violations.shouldHave(texts("javancss", "ch.qos.logback", "org.apache.geronimo.framework", "org.mortbay.jetty"));
    headers.threatHeader().click();
    headers.threatHeader().sortArrowUp().shouldBeSelected();
    violations.shouldHave(texts("0", "0", "0", "5"));
    violations.shouldHave(texts("ch.qos.logback", "org.apache.geronimo.framework", "org.mortbay.jetty", "javancss"));
    // by policy name
    headers.policyNameHeader().click();
    headers.policyNameHeader().sortArrowUp().shouldBeSelected();
    violations.shouldHave(texts("ApplicationReportTest Policy", "None", "None", "None"));
    violations.shouldHave(texts("javancss", "ch.qos.logback", "org.apache.geronimo.framework", "org.mortbay.jetty"));
    headers.policyNameHeader().click();
    headers.policyNameHeader().sortArrowDown().shouldBeSelected();
    violations.shouldHave(texts("None", "None", "None", "ApplicationReportTest Policy"));
    violations.shouldHave(texts("ch.qos.logback", "org.apache.geronimo.framework", "org.mortbay.jetty", "javancss"));
    // by component name
    headers.componentNameHeader().click();
    headers.componentNameHeader().sortArrowUp().shouldBeSelected();
    violations.shouldHave(texts("ch.qos.logback", "javancss", "org.apache.geronimo.framework", "org.mortbay.jetty"));
    headers.componentNameHeader().click();
    headers.componentNameHeader().sortArrowDown().shouldBeSelected();
    violations.shouldHave(texts("org.mortbay.jetty", "org.apache.geronimo.framework", "javancss", "ch.qos.logback"));
  }

  private void setupHdsResponse() {
    testCLMServer.getHdsServer().setResponseForURI("rest/ci/componentDetails",
        getClass().getClassLoader().getResource("componentDetails/javancssComponentDetails.json"), 200);
    testCLMServer.getHdsServer().setResponseForURI("rest/ci/componentDetails/list",
        getClass().getClassLoader().getResource("componentDetails/javancssComponentDetailsList.json"), 200);
  }

  private Policy createPolicy(int threatLevel, String name, String conditionType, String operator, String value) {
    Policy p = new Policy(null, name);
    p.setThreatLevel(threatLevel);
    p.setOwnerId(app.getId());
    Constraint constraint = new Constraint(null, name + " constraint", LogicalOperator.AND);
    com.sonatype.insight.brain.model.policy.Condition condition = new com.sonatype.insight.brain.model.policy.Condition(
        conditionType, operator, value);
    constraint.setConditions(Collections.singletonList(condition));
    p.setConstraints(Collections.singletonList(constraint));
    return tempEntity.newPolicy(p);
  }
}
