/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationreport;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DatePicker;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.LabelsCIP;
import com.sonatype.clm.testing.functional.elements.LabelsCIP.AddLabelModal;
import com.sonatype.clm.testing.functional.elements.LabelsCIP.RemoveLabelModal;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.VersionsCIP;
import com.sonatype.clm.testing.functional.elements.VulnerabilityCIP;
import com.sonatype.clm.testing.functional.elements.VulnerabilityCIP.SVDetailModal;
import com.sonatype.clm.testing.functional.elements.VulnerabilityCIP.SVTableRow;
import com.sonatype.clm.testing.functional.elements.reports.ClaimComponentCIP;
import com.sonatype.clm.testing.functional.elements.reports.ClaimComponentCIP.ConfirmRevokeClaimDialog;
import com.sonatype.clm.testing.functional.elements.reports.LicenseCIP;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipAuditTab;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipOccurrencesTab;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipSimilarTab;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.InnerSourceProducerReportModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.RepositoryReportPage;
import com.sonatype.clm.testing.functional.pages.TransitiveViolationsPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.functional.pages.WaiverCip;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ConfirmRemoveWaiverDialog;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ExistingWaiver;
import com.sonatype.clm.testing.functional.pages.WaiverCip.RequestWaiverDialog;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ViewWaiversDialog;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.testing.functional.elements.reports.ClaimComponentCIP.ERROR_CLASS;
import static com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal.ACTIVE_CLASS;
import static com.sonatype.insight.brain.model.security.MemberType.USER;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationReportCipTest
    extends AbstractFunctionalTest
{
  private static final ComponentIdentifier JAVANCSS_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("javancss",
      "javancss", "29.50", "", "jar");

  private static final String JAVANCSS_HASH = "9aba4af169a1a3baa67f";

  public static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  private InsightWork insightWork;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(ImmutableMap.of(Feature.INNER_SOURCE_TRANSITIVE_WAIVER.getFlag(), false));
    Organization org = tempEntity.newOrganization("ApplicationReportTest");
    app = tempEntity.newApplicationWithSpecificId("8bbaa746602142d9adf2de00a9ca4d4a", "ApplicationReportTest",
        "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    // add Security policy
    createPolicy(app.getId(), 10, "SecurityPolicy", SecurityVulnerabilitySeverityConditionType.ID, "=", "9.1");
    // add License policy
    createPolicy(app.getId(), 5, "LicensePolicy", LicenseThreatGroupLevelConditionType.ID, ">=", "9");
    // add Quality policy
    createPolicy(app.getId(), 2, "QualityPolicy", RelativePopularityConditionType.ID, "<=", "1");
    // add Other policy
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 1, "CoordinatesPolicy", CoordinatesConditionType.ID, "match",
        "maven:javancss*");

    evaluator.evaluatePolicy();
    insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @Test
  public void testCIP() throws Exception {
    setupHdsResponses();
    mockHdsResponseForRemediation();
    CipModal cipModal = reportPage.cipModal();

    // Close, Prev and Next buttons
    reportPage.resultRow(1).click();
    cipModal.getElement().shouldBe(visible);

    cipModal.header().shouldHave(text("javancss : javancss : 29.50"));
    cipModal.previousButton().shouldBe(disabled);
    cipModal.dependencyIndicator().shouldBe(visible).shouldHave(cssClass("direct"))
        .shouldHave(exactText("Direct Dependency"));
    cipModal.nextButton().shouldBe(enabled).click();

    cipModal.header().shouldHave(text("ch.qos.logback : logback-access : 0.6"));
    cipModal.previousButton().shouldBe(enabled);
    cipModal.dependencyIndicator().shouldBe(visible).shouldHave(cssClass("transitive"))
        .shouldHave(exactText("Transitive Dependency"));
    cipModal.nextButton().shouldBe(enabled).click();
    cipModal.closeButton().click();
    cipModal.getElement().shouldBe(hidden);

    reportPage.resultRow(7).click();
    cipModal.getElement().shouldBe(visible);

    cipModal.header().shouldHave(exactText("unknown.jar"));
    cipModal.nextButton().shouldBe(enabled);
    cipModal.dependencyIndicator().shouldNot(exist);
    cipModal.previousButton().shouldBe(enabled).click();

    cipModal.header().shouldHave(text("org.apache.tiles : tiles-core : 2.2.2"));
    cipModal.nextButton().shouldBe(enabled);
    cipModal.previousButton().shouldBe(enabled);
    cipModal.dependencyIndicator().shouldBe(visible).shouldHave(cssClass("direct"))
        .shouldHave(exactText("Direct Dependency"));
    cipModal.closeButton().click();
    cipModal.getElement().shouldBe(hidden);

    // test tab state while navigating with Next/Prev
    reportPage.resultRow(5).click();
    cipModal.getElement().shouldBe(visible);
    cipModal.tabLink(6).shouldHave(text("Vulnerabilities")).click();

    // navigate to another exact-matched component - should stay on Vulnerabilities tab
    cipModal.nextButton().click();
    cipModal.tabLink(6).shouldHave(ACTIVE_CLASS).shouldHave(exactText("Vulnerabilities"));

    // navigate to unknown component while on vulnerabilities tab - should go back to Component Info tab
    cipModal.nextButton().click();
    cipModal.tabLink(1).shouldHave(ACTIVE_CLASS).shouldHave(exactText("Component Info"));

    cipModal.tabLink(5).shouldHave(exactText("Claim")).click();

    // navigate to exact-match component while on Claim tab - should go back to Component Info tab
    cipModal.previousButton().click();
    cipModal.tabLink(1).shouldHave(ACTIVE_CLASS).shouldHave(exactText("Component Info"));
    cipModal.closeButton().click();

    testInnerSourceDependencyComponentHeader();
    testInnerSourceComponentHeader();
    testComponentInfoTab();
    testPolicyTab();
    testLicensesTab();
    testLabelsTab();
    testVulnerabilitiesTab();
    testOccurrencesTab();
    testSimilarTab();
    testAuditTab();
  }

  @Test
  public void testCip_ViewTransitiveViolations_FeatureEnabled_InnerSource() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(ImmutableMap.of(Feature.INNER_SOURCE_TRANSITIVE_WAIVER.getFlag(), true));
    setupHdsResponses();
    mockHdsResponseForRemediation();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(8).click();
    cipModal.tabLink(2).shouldNotHave(ACTIVE_CLASS).click();
    SelenideElement viewTransitiveViolations = WaiverCip.viewTransitiveViolations();
    viewTransitiveViolations.shouldBe(visible);
    viewTransitiveViolations.click();
    waitUntilUrl(TransitiveViolationsPage.url(app.getPublicId(), SCAN_ID, "18d393ad345b03b49c62"));
    TransitiveViolationsPage transitiveViolationsPage = new TransitiveViolationsPage();
    transitiveViolationsPage.shouldBe(visible);
    transitiveViolationsPage.backButton().shouldBe(visible).click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.shouldBe(visible);
  }
  
  @Test
  public void testCip_ViewTransitiveViolations_FeatureEnabled_NonInnerSource() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(ImmutableMap.of(Feature.INNER_SOURCE_TRANSITIVE_WAIVER.getFlag(), true));
    setupHdsResponses();
    mockHdsResponseForRemediation();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(7).click();
    cipModal.tabLink(2).shouldNotHave(ACTIVE_CLASS).click();
    SelenideElement viewTransitiveViolations = WaiverCip.viewTransitiveViolations();
    viewTransitiveViolations.shouldNotBe(visible);
  }

  @Test
  public void testCip_ViewTransitiveViolations_FeatureDisabled() {
    setupHdsResponses();
    mockHdsResponseForRemediation();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(8).click();
    cipModal.tabLink(2).shouldNotHave(ACTIVE_CLASS).click();
    SelenideElement viewTransitiveViolations = WaiverCip.viewTransitiveViolations();
    viewTransitiveViolations.shouldNotBe(visible);
  }

  private void testInnerSourceComponentHeader() {
    String packageUrl = InnerSourceUtils
        .getVersionlessPackageUrl(ComponentIdentifier.createMavenCoordinates("java2html", "j2h", "1.3.1"))
        .getPackageUrl();
    tempEntity.newInnerSourceComponent(packageUrl, app, "0.0.0");

    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(8).click();

    cipModal.getElement().shouldBe(visible);
    cipModal.header().shouldHave(text("java2html : j2h : 1.3.1"));
    cipModal.nextButton().shouldBe(enabled);
    cipModal.previousButton().shouldBe(enabled);
    cipModal.dependencyIndicator().shouldBe(visible).shouldHave(cssClass("inner-source"))
        .shouldHave(exactText("InnerSource"));
    cipModal.ownerApplication().shouldHave(text(app.getName()));
    cipModal.latestReportLink().shouldHave(exactText("View Latest Report"));
    cipModal.innerSourceAlertInfo().shouldHave(exactText("InnerSource components are software components that are " +
        "developed internally and shared with other internal projects."));

    cipModal.latestReportLink().click();
    InnerSourceProducerReportModal innerSourceProducerReportModal = cipModal.innerSourceProducerReportModal();
    innerSourceProducerReportModal.shouldBe(visible);
    innerSourceProducerReportModal.header().shouldHave(exactText("Newer Component Version Found in Report"));
    innerSourceProducerReportModal.content()
        .shouldHave(exactText("A newer version of the InnerSource component is being used in the latest report."));
    innerSourceProducerReportModal.continueToReportButton().shouldBe(visible);
    innerSourceProducerReportModal.cancelButton().click();
    innerSourceProducerReportModal.shouldNotBe(visible);

    cipModal.closeButton().click();
  }

  private void testComponentInfoTab() {
    mockHdsResponseForFirstComponent();
    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(1).click();
    cipModal.getElement().shouldBe(visible);
    cipModal.tabLink(1).shouldHave(ACTIVE_CLASS);
    VersionsCIP.componentType().shouldHave(text("maven"));
    VersionsCIP.groupId().shouldHave(text("javancss"));
    VersionsCIP.artifactId().shouldHave(text("javancss"));
    VersionsCIP.version().shouldHave(text("29.50"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Apache-2.0"));
    VersionsCIP.observedLicenses().shouldHave(texts("GPL-2.0"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Apache-2.0", "GPL-2.0"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("10"), cssClass("critical"));
    VersionsCIP.policyCount().shouldHave(exactText("within 3 policies"));
    VersionsCIP.highestSecurityThreat().shouldHave(text("9.1"));
    VersionsCIP.securityCount().shouldHave(exactText("within 3 security issues"));
    VersionsCIP.hygieneRating().shouldHave(text("Exemplar"));
    VersionsCIP.integrityRating().shouldHave(text("Normal"));
    VersionsCIP.integrityRating().shouldNotHave(cssClass("cip-color-suspicious"));
    VersionsCIP.matchState().shouldHave(text("exact"));
    VersionsCIP.identificationSource().shouldHave(text("Sonatype"));
    VersionsCIP.componentCategory().shouldHave(text("Programming Language Utilites"));
    VersionsCIP.recommendedVersionsHeader().shouldBe(visible);
    VersionsCIP.nextNoViolationVersionLink().shouldBe(visible);
    VersionsCIP.nextNoViolationVersionLink().shouldHave(text("Select 31.52"));
    VersionsCIP.nextNoFailVersionLink().shouldNotBe(visible);
    VersionsCIP.rootAncestorsHeader().shouldNotBe(visible);

    VersionsCIP.showDetailsLink().shouldBe(visible).click();
    VersionsCIP.hideDetailsLink().shouldBe(visible);
    eyesWatcher.eyesCheck("Component Info Tab");

    // test hovering over version bar shows version number
    VersionsCIP.versionBar(1).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(1).shouldHave(text("21.41"));
    VersionsCIP.versionBar(2).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(2).shouldHave(text("25.45"));
    VersionsCIP.versionBar(3).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(3).shouldHave(text("26.46"));
    VersionsCIP.versionBar(4).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(4).shouldHave(text("28.49"));
    VersionsCIP.versionBar(5).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(5).shouldHave(text("29.50"));
    VersionsCIP.versionBar(6).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(6).shouldHave(text("30.51"));

    // mock request for version 21.41
    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("componentDetails/javancssComponentDetails-21.41.json"))
        .atUri("rest/ci/componentDetails");

    VersionsCIP.versionBar(1).shouldBe(visible).click();
    VersionsCIP.version().shouldHave(text("21.41"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Not Declared"));
    VersionsCIP.observedLicenses().shouldHave(texts("No Sources"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Not Declared", "No Sources"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("1"), cssClass("none"));
    VersionsCIP.policyCount().shouldNotBe(visible);
    VersionsCIP.highestSecurityThreat().shouldHave(text("NA"));
    VersionsCIP.securityCount().shouldNotBe(visible);
    VersionsCIP.hygieneRating().shouldNotBe(visible);

    // mock request for version 31.52
    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("componentDetails/javancssComponentDetails-31.52.json"))
        .atUri("rest/ci/componentDetails");

    VersionsCIP.selectNoViolation().shouldBe(visible).click();

    VersionsCIP.version().shouldHave(text("31.52"));
    VersionsCIP.declaredLicenses().shouldHave(texts("BSD-3-Clause"));
    VersionsCIP.observedLicenses().shouldHave(texts("BSD-3-Clause"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("BSD-3-Clause"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("NA"), cssClass("unspecified"));
    VersionsCIP.policyCount().shouldNotBe(visible);
    VersionsCIP.highestSecurityThreat().shouldHave(text("NA"));
    VersionsCIP.securityCount().shouldNotBe(visible);
    VersionsCIP.hygieneRating().shouldNotBe(visible);

    // check that tab loads next component when using Next button
    mockHdsResponseForSecondComponent();
    cipModal.nextButton().shouldBe(enabled).click();
    VersionsCIP.artifactId().shouldHave(text("logback-access"));

    // Check root ancestors list (Recommended Remediation)
    VersionsCIP.rootAncestorsHeader().shouldBe(visible).shouldHave(exactText("Recommended Remediation"));
    VersionsCIP.rootAncestorLinks().shouldHaveSize(3);
    eyesWatcher.eyesCheck("Recommended Remediation");
    VersionsCIP.showMoreRootAncestorsToggle().shouldBe(visible).shouldHave(exactText("Show more")).click();
    VersionsCIP.rootAncestorLinks().shouldHaveSize(4);
    VersionsCIP.showMoreRootAncestorsToggle().shouldBe(visible).shouldHave(exactText("Show less"));
    // navigate to root ancestor and back
    VersionsCIP.rootAncestorLink(1).shouldHave(exactText("javancss : javancss : 29.50")).click();
    cipModal.header().shouldHave(text("javancss : javancss : 29.50"));
    cipModal.nextButton().shouldNotBe(visible);
    cipModal.previousButton().shouldNotBe(visible);
    cipModal.backButton().shouldBe(visible).shouldHave(exactText("Back to ch.qos.logback : logback-access : 0.6"))
        .click();
    cipModal.header().shouldHave(text("ch.qos.logback : logback-access : 0.6"));
    cipModal.nextButton().shouldBe(visible);
    cipModal.previousButton().shouldBe(visible);
    cipModal.backButton().shouldNotBe(visible);
    VersionsCIP.rootAncestorsHeader().shouldBe(visible);

    cipModal.closeButton().click();
  }

  private void testPolicyTab() throws Exception {
    mockHdsResponseForFirstComponent();

    String policyCssClass = "cip-policy-darkblue";
    String policyName = "CoordinatesPolicy";
    String constraintName = "CoordinatesPolicy constraint";
    String conditions = "Coordinates were javancss : javancss : 29.50 (match javancss* : * : * : * : *)";

    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(1).click();
    cipModal.tabLink(2).shouldNotHave(ACTIVE_CLASS).click();
    cipModal.tabLink(2).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(1).shouldNotHave(ACTIVE_CLASS);
    WaiverCip.rows().shouldHaveSize(2);
    WaiverCip.row(1).shouldBe(
        policyCssClass,
        policyName,
        new String[]{constraintName},
        new String[]{conditions});

    // check that there are no existing waivers
    WaiverCip.viewWaivers().shouldBe(visible).click();
    ViewWaiversDialog.rows().shouldHaveSize(0);
    ViewWaiversDialog.closeButton().click();

    // Request Waiver
    WaiverCip.row(1).requestWaiverButton().shouldBe(visible).click();
    RequestWaiverDialog.explanatoryText().shouldNotBe(empty);
    RequestWaiverDialog.policyName().shouldHave(exactText(policyName));
    RequestWaiverDialog.constraintName().shouldHave(exactText(constraintName));
    RequestWaiverDialog.waiverConditions().shouldHave(exactText(conditions));
    RequestWaiverDialog.policyViolationId().shouldNotBe(empty);
    RequestWaiverDialog.policyViolationPageLink().shouldNotBe(empty);

    String policyViolationId = RequestWaiverDialog.policyViolationId().getText();
    String requestWaiverUrl = Configuration.baseUrl + "api/v2/policyWaiver/" + policyViolationId + "/application";
    String policyViolationPageURL = ViolationDetailsPage.url(policyViolationId);

    assertThat(RequestWaiverDialog.policyViolationPageLink().attr("href")).contains(policyViolationPageURL);
    assertThat(RequestWaiverDialog.policyCurlExample().getText()).contains(requestWaiverUrl);

    RequestWaiverDialog.closeButton().click();

    // Waive violation
    WaiverCip.row(1).waiveButton().shouldBe(visible).click();

    // loads the new add waiver page
    waitUntilUrl(AddWaiverPage.url(policyViolationId));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.policyName().shouldHave(exactText(policyName));
    addWaiverPage.constraintName().shouldHave(exactText(constraintName));
    addWaiverPage.conditions().shouldHaveSize(1);
    addWaiverPage.condition(1).shouldHave(exactText(conditions));
    addWaiverPage.availableScopes().shouldHaveSize(3);
    addWaiverPage.scope(0).label().shouldHave(text("Application - " + app.getName()));
    addWaiverPage.scope(1).label().shouldHave(text("Organization - " + app.getName()));
    addWaiverPage.scope(2).label().shouldHave(text("Organization - Root Organization"));

    addWaiverPage.scope(0).click();

    addWaiverPage.comments().setValue("TEST COMMENT");
    // click save
    addWaiverPage.saveButton().shouldBe(visible, enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    // should redirect back to report, with the cip modal open
    reportPage.shouldBe(visible);

    cipModal.tabLink(2).shouldNotHave(ACTIVE_CLASS).click();
    cipModal.tabLink(2).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(1).shouldNotHave(ACTIVE_CLASS);
    WaiverCip.rows().shouldHaveSize(2);
    WaiverCip.row(1).shouldBe(
        policyCssClass,
        policyName,
        new String[]{constraintName},
        new String[]{conditions});

    eyesWatcher.eyesCheck("Policy Tab");

    // check that there is new waiver
    WaiverCip.viewWaivers().shouldBe(visible).click();
    ViewWaiversDialog.rows().shouldHaveSize(1);
    ExistingWaiver waiver = ViewWaiversDialog.row(0);
    waiver.policy().shouldHave(text("CoordinatesPolicy"));
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
    WaiverCip.rows().get(0).shouldHave(text("LicensePolicy"));

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
    WaiverCip.rows().shouldHaveSize(2);
    WaiverCip.row(1).policyName().shouldHave(text("CoordinatesPolicy"));

    // check that tab loads next component when using Next button
    mockHdsResponseForSecondComponent();
    cipModal.nextButton().shouldBe(enabled).click();
    WaiverCip.rows().shouldHaveSize(1);
    cipModal.closeButton().click();
  }

  private void testLicensesTab() {
    mockHdsResponseForFirstComponent();

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
    assertThat(override.getStatus()).isEqualTo(LicenseOverrideStatus.SELECTED);
    assertThat(override.getLicenseIds()).isEqualTo(Collections.singleton("Apache-2.0"));

    // open full-size license selector dropdown before eyes check
    LicenseCIP.status().selectOption("Overridden");
    LicenseCIP.licenseSelector().button().shouldBe(visible).click();

    eyesWatcher.eyesCheck("Licenses Tab");

    // remove
    LicenseCIP.status().selectOption("Inherit Status (Open)");
    LicenseCIP.updateButton().shouldBe(enabled).click();

    RepositoryReportPage.waitForComponentUpdater();

    LicenseCIP.updateButton().shouldBe(disabled);
    override = licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), JAVANCSS_IDENTIFIER);
    assertThat(override).isNull();

    // check that tab loads next component when using Next button
    mockHdsResponseForSecondComponent();
    cipModal.nextButton().shouldBe(enabled).click();
    LicenseCIP.declaredLicenses().shouldHave(LicenseCIP.licenseThreats(5), texts("Not Declared"));

    cipModal.closeButton().click();
  }

  private void testAuditTab() {
    String dateRegex = "\\w{3} \\d{1,2} \\d{4}, \\d{1,2}:\\d{2}:\\d{2} (AM|PM)";

    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(1).click();

    cipModal.tabLink(8).shouldNotHave(ACTIVE_CLASS).click();
    cipModal.tabLink(8).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(1).shouldNotHave(ACTIVE_CLASS);

    CipAuditTab auditTab = cipModal.getAuditTab();

    auditTab.emptyMessage().shouldNot(exist);

    // check that the audit table contains the expected results from the testing done on the other tabs
    auditTab.rowWithoutDate(0).shouldHave(texts("admin", "Deleted", "Vulnerability CVE-1234-56789", "woot"));
    auditTab.dateFromRow(0).should(matchText(dateRegex));
    auditTab.rowWithoutDate(1).shouldHave(texts("admin", "Deleted", "License as Apache-2.0", ""));
    auditTab.dateFromRow(1).should(matchText(dateRegex));
    auditTab.rowWithoutDate(2).shouldHave(texts("admin", "Selected", "License as Apache-2.0", "not bad"));
    auditTab.dateFromRow(2).should(matchText(dateRegex));

    // sorting
    auditTab.dateHeader().sortArrowDown().shouldBeSelected().click();

    auditTab.rowWithoutDate(0).shouldHave(texts("admin", "Selected", "License as Apache-2.0", "not bad"));
    auditTab.rowWithoutDate(1).shouldHave(texts("admin", "Deleted", "License as Apache-2.0", ""));
    auditTab.rowWithoutDate(2).shouldHave(texts("admin", "Deleted", "Vulnerability CVE-1234-56789", "woot"));

    auditTab.actionHeader().click();
    auditTab.actionHeader().sortArrowUp().shouldBeSelected();

    // first two rows sort the same on this column and could come out either way
    auditTab.rowWithoutDate(2).shouldHave(texts("admin", "Selected", "License as Apache-2.0", "not bad"));

    auditTab.actionHeader().click();
    auditTab.actionHeader().sortArrowDown().shouldBeSelected();

    auditTab.rowWithoutDate(0).shouldHave(texts("admin", "Selected", "License as Apache-2.0", "not bad"));
    // second and third rows sort the same on this column and could come out either way

    auditTab.detailHeader().click();
    auditTab.detailHeader().sortArrowUp().shouldBeSelected();

    // first two rows sort the same on this column and could come out either way
    auditTab.rowWithoutDate(2).shouldHave(texts("admin", "Deleted", "Vulnerability CVE-1234-56789", "woot"));

    auditTab.detailHeader().click();
    auditTab.detailHeader().sortArrowDown().shouldBeSelected();

    auditTab.rowWithoutDate(0).shouldHave(texts("admin", "Deleted", "Vulnerability CVE-1234-56789", "woot"));
    // second and third rows sort the same on this column and could come out either way

    auditTab.commentHeader().click();
    auditTab.commentHeader().sortArrowUp().shouldBeSelected();

    auditTab.rowWithoutDate(0).shouldHave(texts("admin", "Deleted", "License as Apache-2.0", ""));
    auditTab.rowWithoutDate(1).shouldHave(texts("admin", "Selected", "License as Apache-2.0", "not bad"));
    auditTab.rowWithoutDate(2).shouldHave(texts("admin", "Deleted", "Vulnerability CVE-1234-56789", "woot"));

    auditTab.commentHeader().click();
    auditTab.commentHeader().sortArrowDown().shouldBeSelected();

    auditTab.rowWithoutDate(0).shouldHave(texts("admin", "Deleted", "Vulnerability CVE-1234-56789", "woot"));
    auditTab.rowWithoutDate(1).shouldHave(texts("admin", "Selected", "License as Apache-2.0", "not bad"));
    auditTab.rowWithoutDate(2).shouldHave(texts("admin", "Deleted", "License as Apache-2.0", ""));

    cipModal.nextButton().click();

    auditTab.emptyMessage().shouldBe(visible);
    auditTab.table().shouldNot(exist);
  }

  private void testLabelsTab() throws Exception {
    mockHdsResponseForFirstComponent();

    Label elMagnifico = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "El Magnifico", Color.dark_blue);
    Label elJunko = tempEntity.newLabel(Organization.ROOT_ORGANIZATION_ID, "El Junko", Color.dark_red);
    tempEntity.newComponentLabel(Organization.ROOT_ORGANIZATION_ID, elMagnifico.getId(), JAVANCSS_HASH);

    createPolicy(app.getId(), 2, "Bad Label", LabelConditionType.ID, "is", elJunko.getId());

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

    eyesWatcher.eyesCheck("Labels Tab");

    AddLabelModal.saveButton().click();
    AddLabelModal.root().shouldBe(hidden);

    // label persisted
    assertThat(new ComponentLabelDAO().getByOwnerIdAndHashWithHierarchy(app.getId(), JAVANCSS_HASH)).hasSize(2);

    // Check new policy violation was added
    evaluator.reevaluatePolicy();
    cipModal.tabLink(2).click();
    WaiverCip.rows().shouldHaveSize(3);
    WaiverCip.row(1).shouldBe(
        "cip-policy-yellow",
        "Bad Label",
        new String[] { "Bad Label constraint" },
        new String[] { "Found label 'El Junko'" });

    cipModal.tabLink(7).click();
    LabelsCIP.appliedLabel(1).shouldHave(text("El Junko")).action().click();
    RemoveLabelModal.confirmButton().click();
    cipModal.closeButton().click();

    // Removing without proper permissions should display an error
    logout();
    User user = tempEntity.newUser("username", "john", "doe", "john@doe");
    tempEntity.newMembershipMapping(app.getId(), DEVELOPER_ROLE_ID, user.getUsername(), USER);

    // login with developer role
    login(user.getUsername(), user.getPassword());

    // create report for this user
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, insightWork);

    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));

    reportPage.resultRow(1).click();
    cipModal.tabLink(7).click();

    // verify existing labels
    LabelsCIP.availableLabels().shouldHaveSize(1);
    LabelsCIP.availableLabel(1).shouldHave(text("El Junko"));
    LabelsCIP.appliedLabels().shouldHaveSize(1);
    LabelsCIP.appliedLabel(1).shouldHave(text("El Magnifico"));

    LabelsCIP.availableLabel(1).shouldHave(text("El Junko")).action().click();
    AddLabelModal.root().should(appear);
    AddLabelModal.error().shouldBe(visible).shouldHave(text("Insufficient Permissions"));

    AddLabelModal.closeButton().shouldBe(visible).click();
    AddLabelModal.root().should(disappear);

    LabelsCIP.appliedLabel(1).shouldHave(text("El Magnifico")).action().click();
    RemoveLabelModal.root().should(appear);
    RemoveLabelModal.error().shouldBe(visible).shouldHave(text("Insufficient Permissions"));
    RemoveLabelModal.closeButton().shouldBe(visible).click();
    RemoveLabelModal.root().should(disappear);

    // verify labels remain unchanged
    LabelsCIP.availableLabels().shouldHaveSize(1);
    LabelsCIP.availableLabel(1).shouldHave(text("El Junko"));
    LabelsCIP.appliedLabels().shouldHaveSize(1);
    LabelsCIP.appliedLabel(1).shouldHave(text("El Magnifico"));
    cipModal.closeButton().click();

    // Remove the label we added with proper permissions
    logout();
    loginAsAdmin();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.resultRow(1).click();
    cipModal.tabLink(7).click();
    LabelsCIP.availableLabel(1).shouldHave(text("El Junko")).action().click();
    AddLabelModal.saveButton().click();
    LabelsCIP.appliedLabels().shouldHaveSize(2);
    LabelsCIP.appliedLabel(1).shouldHave(text("El Junko")).action().click();

    // Confirmation modal
    RemoveLabelModal.root().should(appear);
    RemoveLabelModal.confirmButton().click();
    RemoveLabelModal.root().should(disappear);

    // backend check that it was removed
    List<ComponentLabel> appliedLabels =
        new ComponentLabelDAO().getByOwnerIdAndHashWithHierarchy(app.getId(), JAVANCSS_HASH);
    assertThat(appliedLabels).extracting(ComponentLabel::getLabelId).containsExactlyInAnyOrder(elMagnifico.getId());

    // Check new policy violation is gone
    evaluator.reevaluatePolicy();
    cipModal.tabLink(2).click();
    WaiverCip.rows().shouldHaveSize(2);

    // check that tab loads next component when using Next button
    cipModal.tabLink(7).click();
    LabelsCIP.appliedLabels().shouldHaveSize(1);
    mockHdsResponseForSecondComponent();
    cipModal.nextButton().shouldBe(enabled).click();
    LabelsCIP.appliedLabels().shouldHaveSize(0);

    cipModal.closeButton().click();
  }

  private void testVulnerabilitiesTab() throws Exception {
    mockHdsResponseForFirstComponent();

    tempEntity.newSecurityVulnerabilityOverride(app.getId(), JAVANCSS_HASH, "cve", "CVE-1234-56789",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);

    reportPage.resultRow(1).click();
    CipModal cipModal = reportPage.cipModal();
    cipModal.tabLink(6).shouldNotHave(ACTIVE_CLASS).click();
    cipModal.tabLink(6).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(1).shouldNotHave(ACTIVE_CLASS);

    VulnerabilityCIP.root().shouldBe(visible);

    VulnerabilityCIP.rows().shouldHaveSize(3);

    assertRow(VulnerabilityCIP.row(0), 9, "CVE-1234-56789");
    assertRow(VulnerabilityCIP.row(1), 4, "OSVDB-1234");
    assertRow(VulnerabilityCIP.row(2), null, "OSVDB-4321");

    String componentIdentifier = URLEncoder.encode(ComponentIdentifierAdapter.toJson(JAVANCSS_IDENTIFIER), "UTF-8");

    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("vulnerabilityDetails/vulnerabilityDetails.json"))
        .atUri("rest/vulnerability/details/json/CVE-1234-56789?componentIdentifier=" + componentIdentifier);

    SVTableRow row = VulnerabilityCIP.row(0);
    row.info().click();

    SVDetailModal.root().shouldBe(visible);

    // data from the json response we send above
    SVDetailModal.contents().shouldHave(text("CVE-1234-56789"));

    SVDetailModal.closeButton().shouldBe(enabled).click();

    row.identifier().click();
    row.shouldBe(SVTableRow.ROW_SELECTED);

    VulnerabilityCIP.Editor.status().shouldBe(visible).shouldHave(text("Acknowledged"))
        .selectOption(SecurityVulnerabilityOverrideStatus.OPEN.getName());

    eyesWatcher.eyesCheck("Vulnerabilities Tab");

    VulnerabilityCIP.Editor.comment().val("woot");
    VulnerabilityCIP.Editor.saveButton().click();

    row.status().shouldHave(text("Open"));
    assertThat(new SecurityVulnerabilityOverrideDAO().getByOwnerIdHashSourceAndReferenceId(app.getId(),
        JAVANCSS_HASH, "cve", "CVE-1234-56789")).isNull();

    ArrayNode allLogJsonData = JsonUtils.read(new File(insightWork.getAuditDir(app.getId()), "security.json"));
    assertThat(allLogJsonData).hasSize(1);
    assertThat(allLogJsonData.get(0).get("data").get("comment").asText()).isEqualTo("woot");

    // check that tab loads next component when using Next button
    mockHdsResponseForSecondComponent();
    cipModal.nextButton().shouldBe(enabled).click();
    VulnerabilityCIP.rows().shouldHaveSize(0);

    cipModal.closeButton().click();
  }

  private void testOccurrencesTab() {
    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(1).click();

    cipModal.tabLink(4).shouldNotHave(ACTIVE_CLASS).click();
    cipModal.tabLink(4).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(1).shouldNotHave(ACTIVE_CLASS);

    CipOccurrencesTab occurrencesTab = cipModal.getOccurrencesTab();
    occurrencesTab.occurrences().shouldHaveSize(1);
    occurrencesTab.occurrences().shouldHave(exactTexts("Dependency javancss:javancss:jar:29.50 located at Module " +
        "com.sonatype.insight.example:sample-small-application:jar:1.0.0"));

    cipModal.nextButton().click();

    occurrencesTab.occurrences().shouldHaveSize(3);
    occurrencesTab.occurrences().shouldHave(exactTexts(
        "Dependency ch.qos.logback:logback-access:jar:0.6 located at Module " +
        "com.sonatype.insight.example:sample-small-application:jar:1.0.0",
        "logback-access-0.6.jar located at deps",
        "logback-access-0.6.jar"
    ));

    eyesWatcher.eyesCheck("Occurrences Tab");

    cipModal.closeButton().click();
  }

  private void testSimilarTab() {
    reportPage.resultRow(2).shouldHave(text("logback")).click();

    CipModal cipModal = reportPage.cipModal();
    cipModal.tabLink(3).shouldNotHave(ACTIVE_CLASS).click();
    cipModal.tabLink(3).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(1).shouldNotHave(ACTIVE_CLASS);

    CipSimilarTab similarTab = cipModal.getSimilarTab();
    similarTab.emptyMessage().shouldNot(exist);
    similarTab.mostSimilarComponent().shouldHave(text("ch.qos.logback : logback-access : 0.6-a"));
    similarTab.otherSimilarComponents().shouldHave(texts("ch.qos.logback : logback-access : 0.6-b"));

    eyesWatcher.eyesCheck("Similar Tab");

    cipModal.nextButton().click();

    similarTab.mostSimilarComponent().shouldNot(exist);
    similarTab.otherSimilarComponents().shouldHaveSize(0);
    similarTab.emptyMessage().shouldBe(visible);

    cipModal.closeButton().click();
  }

  @Test
  public void testClaimComponentTab() {
    mockHdsResponseForClaimedComponent();
    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(7).shouldHave(text("unknown.jar")).click();
    cipModal.tabLink(5).shouldNotHave(ACTIVE_CLASS).click();
    cipModal.tabLink(5).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(1).shouldNotHave(ACTIVE_CLASS);

    ClaimComponentCIP claimComponentTab = new ClaimComponentCIP();
    claimComponentTab.shouldBe(visible);
    claimComponentTab.allFormInputs().forEach(i -> i.shouldBe(empty));
    claimComponentTab.revokeBtn().shouldBe(hidden);
    claimComponentTab.cancelBtn().shouldBe(hidden);
    claimComponentTab.updateBtn().shouldBe(hidden);
    claimComponentTab.claimBtn().shouldBe(visible, disabled);
    claimComponentTab.comment().input().val("comment");
    claimComponentTab.claimBtn().shouldBe(visible, enabled).click();
    claimComponentTab.validationErrors()
        .shouldHave(exactText("Group ID, Artifact ID, Version and Extension are required"));
    eyesWatcher.eyesCheck("Form and validation message");

    claimComponentTab.group().shouldHave(ERROR_CLASS).input().val("groupId");
    claimComponentTab.group().shouldNotHave(ERROR_CLASS);

    claimComponentTab.artifactId().shouldHave(ERROR_CLASS).input().val("artifactId");
    claimComponentTab.artifactId().shouldNotHave(ERROR_CLASS);

    claimComponentTab.version().shouldHave(ERROR_CLASS).input().val("version");
    claimComponentTab.version().shouldNotHave(ERROR_CLASS);

    claimComponentTab.extension().shouldHave(ERROR_CLASS).input().val("extension");
    claimComponentTab.extension().shouldNotHave(ERROR_CLASS);
    claimComponentTab.validationErrors().shouldBe(empty);

    claimComponentTab.created().shouldNotHave(ERROR_CLASS).input().val("foo");
    claimComponentTab.created().shouldHave(ERROR_CLASS);
    claimComponentTab.validationErrors().shouldHave(exactText("Date format is MM/DD/YYYY"));
    claimComponentTab.created().click();
    new DatePicker().today().click();
    claimComponentTab.validationErrors().shouldBe(empty);
    claimComponentTab.created().shouldNotHave(ERROR_CLASS);

    claimComponentTab.classifier().shouldNotHave(ERROR_CLASS).input().val("classifier");

    claimComponentTab.claimBtn().click();

    claimComponentTab.group().input().shouldHave(value("groupId"));
    claimComponentTab.artifactId().input().shouldHave(value("artifactId"));
    claimComponentTab.version().input().shouldHave(value("version"));
    claimComponentTab.extension().input().shouldHave(value("extension"));
    claimComponentTab.created().input().shouldNotBe(empty);
    claimComponentTab.comment().input().shouldHave(value("comment"));
    claimComponentTab.claimBtn().shouldNotBe(visible);
    claimComponentTab.revokeBtn().shouldBe(visible);
    claimComponentTab.cancelBtn().shouldBe(visible, disabled);
    claimComponentTab.updateBtn().shouldBe(visible, disabled);

    // close and reopen CIP to ensure claim persists/comes back
    // Close CIP, re-eval policies and re-open the CIP
    cipModal.closeButton().click();
    reportPage.resultRow(7).shouldHave(text("unknown.jar")).click();
    cipModal.tabLink(5).shouldNotHave(ACTIVE_CLASS).click();

    claimComponentTab.group().input().shouldHave(value("groupId"));
    claimComponentTab.artifactId().input().shouldHave(value("artifactId"));
    claimComponentTab.version().input().shouldHave(value("version"));
    claimComponentTab.extension().input().shouldHave(value("extension"));
    claimComponentTab.created().input().shouldNotBe(empty);
    claimComponentTab.comment().input().shouldHave(value("comment"));
    claimComponentTab.claimBtn().shouldNotBe(visible);
    claimComponentTab.revokeBtn().shouldBe(visible);
    claimComponentTab.cancelBtn().shouldBe(visible, disabled);
    claimComponentTab.updateBtn().shouldBe(visible, disabled);

    // Close CIP, re-eval policies and re-open the CIP
    cipModal.closeButton().click();
    reportPage.reevaluateButton().click();
    FormMask.seeAndWaitForDismissal();
    cipModal = reportPage.cipModal();

    // the new name pushes the component up in the results page
    reportPage.resultRow(7).shouldHave(text("org.apache.tiles : tiles-core : 2.2.2"));
    reportPage.resultRow(5).shouldHave(text("groupId : artifactId : extension : classifier : version")).click();
    cipModal.tabLink(7).shouldHave(exactText("CLAIM")).click(); // a few extra tabs have been added
    cipModal.header().shouldHave(text("groupId : artifactId : extension : classifier : version"));
    claimComponentTab.revokeBtn().shouldBe(enabled);
    claimComponentTab.cancelBtn().shouldBe(disabled);

    HashComponentIdentifier claimedComponent = new HashComponentIdentifierDAO().getByComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version", "classifier", "extension"));

    assertThat(claimedComponent).isNotNull();
    assertThat(claimedComponent.getComment()).isEqualTo("comment");
    assertThat(claimedComponent.getCreateTime()).isEqualTo(DateTime.now().withTimeAtStartOfDay().toDate());

    claimComponentTab.group().input().val("groupie");
    claimComponentTab.cancelBtn().shouldBe(enabled).click();
    claimComponentTab.group().input().shouldHave(value("groupId"));
    claimComponentTab.cancelBtn().shouldBe(disabled);

    claimComponentTab.revokeBtn().click();
    ConfirmRevokeClaimDialog confirmRevokeClaimDialog = new ConfirmRevokeClaimDialog();
    confirmRevokeClaimDialog.shouldBe(visible);
    eyesWatcher.eyesCheck("Revoke Claim dialog");
    confirmRevokeClaimDialog.revokeClaimButton().click();
    confirmRevokeClaimDialog.shouldBe(hidden);

    // Revoke doesn't take effect in the report immediately, but does after re-eval
    cipModal.closeButton().click();
    reportPage.resultRow(5).shouldHave(text("groupId : artifactId : extension : classifier : version"));
    reportPage.reevaluateButton().click();
    FormMask.seeAndWaitForDismissal();
    reportPage.resultRow(7).shouldHave(text("unknown.jar"));
  }

  @Test
  public void testClaimComponentTabDisplayRules() {
    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(6).click();
    cipModal.tabLink(1).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(5).shouldNotHave(ACTIVE_CLASS).shouldHave(exactText("LICENSES"));
    cipModal.nextButton().click();
    cipModal.tabLink(5).shouldHave(exactText("CLAIM")).click();
    cipModal.tabLink(5).shouldHave(ACTIVE_CLASS);
    cipModal.previousButton().click();
    cipModal.tabLink(1).shouldHave(ACTIVE_CLASS);
    cipModal.tabLink(5).shouldNotHave(ACTIVE_CLASS);
  }

  private void testInnerSourceDependencyComponentHeader() {
    CipModal cipModal = reportPage.cipModal();
    reportPage.resultRow(9).click();

    cipModal.getElement().shouldBe(visible);
    cipModal.header().shouldHave(text("joda-time : joda-time : 1.3.1"));
    cipModal.previousButton().shouldBe(enabled);
    cipModal.dependencyInnerSourceIndicator().shouldBe(visible);
    cipModal.dependencyIndicator().shouldBe(visible).shouldHave(cssClass("transitive"))
        .shouldHave(exactText("Transitive Dependency"));
    cipModal.closeButton().click();

    reportPage.resultRow(12).click();

    cipModal.getElement().shouldBe(visible);
    cipModal.header().shouldHave(text("javax.inject : javax.inject : 1"));
    cipModal.previousButton().shouldBe(enabled);
    cipModal.dependencyInnerSourceIndicator().shouldBe(visible);
    cipModal.dependencyIndicator().shouldBe(visible).shouldHave(cssClass("direct"))
        .shouldHave(exactText("Direct Dependency"));
    cipModal.closeButton().click();
  }

  private Policy createPolicy(String ownerId,
                              int threatLevel,
                              String name,
                              String conditionType,
                              String operator,
                              String value)
  {
    Policy p = new Policy(null, name);
    p.setThreatLevel(threatLevel);
    p.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, name + " constraint", LogicalOperator.AND);
    com.sonatype.insight.brain.model.policy.Condition condition = new com.sonatype.insight.brain.model.policy.Condition(
        conditionType, operator, value);
    constraint.setConditions(Collections.singletonList(condition));
    p.setConstraints(Collections.singletonList(constraint));
    return tempEntity.newPolicy(p);
  }

  private void mockHdsResponseForFirstComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-29.50.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
  }

  private void mockHdsResponseForSecondComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/logback-accessComponentDetails-0.6.json"))
        .atUri("rest/ci/componentDetails");
  }

  private void setupHdsResponses() {
    mockHdsResponseForFirstComponent();
    testCLMServer.getHdsServer()
        .respondWith(
            getClass().getResource("/componentDetails/javancssComponentDetailsListWithNoViolationsVersion.json"))
        .atUri("rest/ci/componentDetails/list");
  }

  private void mockHdsResponseForRemediation() {
    testCLMServer.getHdsServer().respondWith("{\"known\":true}").atUri("rest/component/summary");
  }

  private void mockHdsResponseForClaimedComponent() {
    testCLMServer.getHdsServer().respondWith("{\"known\":false}").atUri("rest/component/summary");
  }

  private static void assertRow(SVTableRow actualRow, Integer threatLevel, String identifier) {
    actualRow.identifier().shouldHave(text(identifier));
    actualRow.info().shouldBe(visible);
    actualRow.threatLevel().shouldHave(text(threatLevel == null ? "Unscored" : threatLevel.toString().substring(0, 1)),
        SVTableRow.color(threatLevel));
  }
}
