/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.MatchStateFilter;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.componentdetails.AddWaiverPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.ComponentInformationTile.GeneralInfoSection;
import com.sonatype.clm.testing.functional.elements.componentdetails.ComponentInformationTile.IdentificationInfoSection;
import com.sonatype.clm.testing.functional.elements.componentdetails.EditLicensesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.LicenseDetectionsTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.OccurrencesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationDetailPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.SimilarMatchesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilitiesTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilityDetailsPopover;
import com.sonatype.clm.testing.functional.elements.reports.LicenseCIP;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal;
import com.sonatype.clm.testing.functional.pages.AuditLogContent;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover.ComponentWaiversPopoverTable;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage.RequestWaiversPopover;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.clm.testing.functional.utils.WaiverApplierForReport;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentDetailsTest
    extends AbstractFunctionalTest
{
  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String HASH = "fa78f54738ccf77379d1";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  @Before
  public void start() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = new PolicyImportExport();

    Organization org = tempEntity.newOrganization("Test Organization");
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportTest", "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Test
  public void testComponentDetailsEnabled() {
    try {
      refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
      reportPage.reportTitle().shouldHave(text("ApplicationReportTest Build Report"));

      ElementsCollection violations = reportPage.resultRows();
      SelenideElement firstViolation = violations.first();
      firstViolation.click();

      waitUntilUrl(ComponentDetailsPage.url(app, SCAN_ID, HASH));
      ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
      componentDetailsPage.header().title().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));
      componentDetailsPage.tabs().shouldHaveSize(5);

      SelenideElement backButton = MainHeader.backButton();
      backButton.shouldBe(visible);
      backButton.shouldHave(text("Back to Application Report"));
      backButton.click();

      waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    }
    finally {
      if (reportPage.filterPanel().getElement().is(visible)) {
        reportPage.filterPanel().closeButton().click();
      }
    }
  }

  @Test
  public void testComponentDetailsHeaderAndFooter() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));

    ElementsCollection violations = reportPage.resultRows();
    SelenideElement directDependencyWithViolation = violations.get(4);
    directDependencyWithViolation.click();

    final String directDependencyHash = "f0776db1593e215146d2";
    waitUntilUrl(ComponentDetailsPage.url(app, SCAN_ID, directDependencyHash));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    SelenideElement title = componentDetailsPage.header().title();
    title.shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));

    // Not comparing exact texts due to dynamic information (organization uuid, rpeort date)
    ElementsCollection reportInformationElements = componentDetailsPage.header().reportInformationElements();
    reportInformationElements.shouldHave(texts("Test Organization", "ApplicationReportTest", "Build Report "));

    ElementsCollection tags = componentDetailsPage.header().tags();
    tags.shouldHave(texts("maven", "Direct Dependency"));

    componentDetailsPage.footer().paginationCounter().shouldHave(text("5 of 64"));

    eyesWatcher.eyesCheck("component details header and footer");
  }

  @Test
  public void testComponentDetailsRemediationDefaultTab() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    reportPage.reportTitle().shouldHave(text("ApplicationReportTest Build Report"));

    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.first();
    firstViolation.click();

    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, HASH));
  }

  @Test
  public void testComponentDetailsTabNavigation() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    reportPage.reportTitle().shouldHave(text("ApplicationReportTest Build Report"));

    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    componentDetailsPage.violationsTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToViolations(app, SCAN_ID, HASH));

    componentDetailsPage.securityTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToSecurity(app, SCAN_ID, HASH));

    componentDetailsPage.legalTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToLegal(app, SCAN_ID, HASH));

    componentDetailsPage.auditTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToAudit(app, SCAN_ID, HASH));

    componentDetailsPage.overviewTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, HASH));
  }

  @Test
  public void testComponentDetailsUnknownComponentAlert() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForUnknownComponent();

    SelenideElement unknownComponentAlert = componentDetailsPage.unknownComponentAlert();
    unknownComponentAlert.shouldBe(visible);
  }

  @Test
  public void testOverviewTab_componentInformationTile() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    GeneralInfoSection generalInfoSection =
        componentDetailsPage.overviewTabContent().componentInformationTile().generalInfoSection();
    generalInfoSection.shouldBe(visible);
    generalInfoSection.getTypeItem().shouldHave(text("Type maven"));
    generalInfoSection.getNamingItems()
        .shouldHave(exactTexts("Group com.mycila", "Artifact license-maven-plugin", "Version 2.11"));
    IdentificationInfoSection identificationInfoSection =
        componentDetailsPage.overviewTabContent().componentInformationTile().identificationInfoSection();
    identificationInfoSection.shouldBe(visible);
    identificationInfoSection.getCatalogedDateItem().shouldHave(text("Cataloged 6 years ago"));
    identificationInfoSection.getMatchStateItem().shouldHave(text("Match State exact"));
    identificationInfoSection.getIdentificationSourceItem().shouldHave(text("Identification Source"));
    identificationInfoSection.getCategoryItem().shouldHave(text("Category"));

    identificationInfoSection.getOccurrencesItem().shouldBe(visible);
    identificationInfoSection.getOccurrencesItem().shouldHave(text("Occurrences 1 File Matches"));

    eyesWatcher.eyesCheck("component details overview tab component information");
  }

  @Test
  public void testOverviewTab_OccurrencesPopover() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    IdentificationInfoSection identificationInfoSection =
        componentDetailsPage.overviewTabContent().componentInformationTile().identificationInfoSection();
    identificationInfoSection.shouldBe(visible);
    identificationInfoSection.getOccurrencesItem().shouldHave(text("Occurrences 1 File Matches"));
    identificationInfoSection.getOccurrencesLink().click();

    OccurrencesPopover occurrencesPopover = new OccurrencesPopover();
    occurrencesPopover.title().shouldHave(text("Occurrences"));
    occurrencesPopover.subtitle().shouldHave(text("Component Occurrences"));
    occurrencesPopover.infoMessage().shouldBe(visible);
    occurrencesPopover.externalLink().shouldBe(visible);
    eyesWatcher.eyesCheck("Occurrences Popover");

    occurrencesPopover.closeButton().click();
    occurrencesPopover.shouldNotBe(visible);
  }

  @Test
  public void testOverviewTab_SimilarMatchesPopover() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForSimilarComponent();
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    IdentificationInfoSection identificationInfoSection =
        componentDetailsPage.overviewTabContent().componentInformationTile().identificationInfoSection();
    identificationInfoSection.shouldBe(visible);
    identificationInfoSection.getMatchStateItem().shouldHave(text("similar (View Similar Matches)"));
    identificationInfoSection.getSimilarMatchesLink().click();

    SimilarMatchesPopover similarMatchesPopover = new SimilarMatchesPopover();
    similarMatchesPopover.title().shouldHave(text("Similar Matches"));
    similarMatchesPopover.componentIdentificationInformation().shouldBe(visible);
    similarMatchesPopover.bestMatchSubtitle().shouldHave(text("Best Match"));
    similarMatchesPopover.bestMatchListItem().shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));
    eyesWatcher.eyesCheck("Similar Matches Popover");

    similarMatchesPopover.closeButton().click();
    similarMatchesPopover.shouldNotBe(visible);
  }

  @Test
  public void testPolicyViolationsTab_violationTableEntries() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    SelenideElement lastViolation = reportPage.resultRows().last();
    lastViolation.click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "bd804633b9c2cf062586"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.violationsTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToViolations(app, SCAN_ID, "bd804633b9c2cf062586"));
    componentDetailsPage.violationsTabContent().shouldBe(visible);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);
    ElementsCollection rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(1);
    rowCells.get(0).shouldHave(text("No policy violations"));

    waiveFirstReportRow();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));

    componentDetailsPage = openComponentDetailsPageForFirstViolation();
    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);
    policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(6);
    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')", "Unapplied Waiver", ""));

    eyesWatcher.eyesCheck("component details violations tab violation table unapplied waiver");

    // Reevaluate to apply the waiver and apply appropriate filter to show in the report
    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reevaluateButton().click();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    reportPage.filterToggle().click();
    reportPage.filterPanel().violationStateFilter().twisty().click();
    reportPage.filterPanel().violationStateFilter().waived().click();
    reportPage.filterPanel().closeButton().click();
    componentDetailsPage = openComponentDetailsPageForFirstViolation();

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(6);
    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')", "1 Active Waiver", ""));
    eyesWatcher.eyesCheck("component details violations tab violation table active waiver");

    testGrandfatheringIndicator(componentDetailsPage);
  }

  @Test
  public void testPolicyViolationsTab_ViewDetailsPopover() {
    waiveFirstReportRow();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);
    SelenideElement row = policyViolationsTable.getRows().first();
    row.click();
    PolicyViolationDetailPopover violationDetailPopover = new PolicyViolationDetailPopover();
    violationDetailPopover.shouldBe(visible);
    SelenideElement closeButton = violationDetailPopover.getCloseButton();
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();
    eyesWatcher.eyesCheck("policy violation details popover");

    tile.headerTitle().shouldHave(text("Violation of License-Banned"));
    ElementsCollection elements = tile.headerSubtitle().findAll(".iq-violation-details__subtitle-part");
    elements.shouldHaveSize(3);
    elements.get(0).shouldHave(text("Test Organization"));
    elements.get(1).shouldHave(text("ApplicationReportTest"));
    elements.get(2).shouldHave(text("com.mycila : license-maven-plugin : 2.11"));

    tile.firstReported().shouldHave(text("Just now"));
    tile.lastReported().shouldHave(text("Just now"));
    tile.policyType().shouldHave(text("License"));
    tile.threatLevel().shouldHave(text("10"));
    tile.policyOwnerLink().shouldHave(text("Test Organization"));

    tile.stages().shouldHaveSize(5);

    tile.stage(0).shouldHave(text("Source"));
    tile.stage(0).icon().should(exist);
    tile.stage(0).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(1).shouldHave(text("Build 1min"));
    tile.stage(1).icon().shouldNot(exist);
    tile.stage(1).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(2).shouldHave(text("Stage"));
    tile.stage(2).icon().should(exist);
    tile.stage(2).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(3).shouldHave(text("Release"));
    tile.stage(3).icon().should(exist);
    tile.stage(3).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(4).shouldHave(text("Operate"));
    tile.stage(4).icon().should(exist);
    tile.stage(4).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    closeButton.click();
    violationDetailPopover.shouldNotBe(visible);

    row.click();
    violationDetailPopover.shouldBe(visible);
    SelenideElement manageWaiversButton = violationDetailPopover.getManageWaiversButton();

    manageWaiversButton.click();

    ListWaiversPage waiversForViolationPage = new ListWaiversPage();
    waiversForViolationPage.title().shouldHave(text("Waivers for Violation"));
    waiversForViolationPage.backButton().shouldHave(text("Back to Component Details"));
    waiversForViolationPage.componentName().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));
    waiversForViolationPage.waiverListTable().rows().shouldHaveSize(1);
  }

  @Test
  public void testPolicyViolationsTab_viewAllComponentWaivers() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/YYYY");
    String dateString = dateFormat.format(Date.from(Instant.now()));
    waiveFirstReportRow();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    componentDetailsPage.violationsTabContent().componentWaiversButton().click();
    ComponentWaiversPopover componentWaiversPopover = new ComponentWaiversPopover();
    componentWaiversPopover.shouldBe(visible);
    componentWaiversPopover.title().shouldHave(text("Component Waivers"));
    componentWaiversPopover.closePopoverButton().shouldBe(visible);
    componentWaiversPopover.closePopoverButton().click();
    componentWaiversPopover.shouldBe(hidden);

    componentDetailsPage.violationsTabContent().componentWaiversButton().click();
    ComponentWaiversPopoverTable componentWaiversTable = componentWaiversPopover.componentWaiversPopoverTable();
    componentWaiversTable.shouldBe(visible);
    componentWaiversTable.getRows().shouldHaveSize(1);
    SelenideElement row1 = componentWaiversTable.getRow(1);
    ElementsCollection row1Cells = row1.findAll(".nx-cell");
    row1Cells.shouldHave(texts("License-Banned", "License not approved in any situation",
        dateString, "Application - ApplicationReportTest", "com.mycila : license-maven-plugin : 2.11", "- -", ""));
    eyesWatcher.eyesCheck("component details violations tab component waivers popover");
    componentWaiversTable.deleteWaiverButton(1).click();

    ListWaiversPage.DeleteWaiverModal deleteWaiverModal = componentWaiversPopover.deleteWaiverModal();
    deleteWaiverModal.root().shouldBe(visible);
    deleteWaiverModal.yesButton().click();
    deleteWaiverModal.root().should(disappear);

    componentWaiversTable.emptyTableMessage().shouldBe(visible);
    componentWaiversTable.emptyTableMessage().shouldHave(text("No existing component waivers"));
  }

  @Test
  public void testPolicyViolationsTab_openAddWaiverPopover() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);
    policyViolationsTable.addWaiverButton(0).shouldBe(visible);
    policyViolationsTable.addWaiverButton(0).click();

    AddWaiverPopover addWaiver = new AddWaiverPopover();

    List<PolicyViolation> violations =
        new PolicyViolationDAO().getActiveByApplicationIdAndStageIdAndHash(app.getId(), "build", HASH);
    assertThat(violations).hasSize(1);

    ComponentIdentifier componentIdentifier = violations.get(0).getComponentIdentifier();
    assertThat(componentIdentifier.isMaven()).isTrue();
    String artifactId = componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID);

    addWaiver.artifactName().shouldHave(text(artifactId));

    addWaiver.policyName().shouldHave(text(violations.get(0).getPolicyName()));
    assertThat(violations.get(0).getConstraintFacts()).hasSize(1);
    ConstraintFact constraintFact = violations.get(0).getConstraintFacts().get(0);
    addWaiver.constraintName().shouldHave(text(constraintFact.getConstraintName()));
    addWaiver.conditions().get(0).shouldHave(text(constraintFact.getConditionFacts().get(0).getReason()));

    addWaiver.saveButton().shouldBe(visible, enabled).click();
    policyViolationsTable.shouldBe(visible);
  }

  @Test
  public void testPolicyViolationsTab_openRequestWaiverPopover() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);
    policyViolationsTable.waiversDropdownButton(1).shouldBe(visible);
    policyViolationsTable.waiversDropdownArrow(1).click();
    policyViolationsTable.requestWaiverDropdownButton(1).shouldBe(visible).click();
    eyesWatcher.eyesCheck("component details violations tab request waivers popover");

    List<PolicyViolation> violations =
        new PolicyViolationDAO().getActiveByApplicationIdAndStageIdAndHash(app.getId(), "build", HASH);
    assertThat(violations).hasSize(1);

    RequestWaiversPopover requestWaiver = new ListWaiversPage().requestWaiversPopover();
    requestWaiver.requestWaiverHeader().shouldHave(text("Request Waiver"));
    requestWaiver.requestWaiverPolicyViolationId().shouldHave(text(violations.get(0).getId()));

    ComponentIdentifier componentIdentifier = violations.get(0).getComponentIdentifier();
    assertThat(componentIdentifier.isMaven()).isTrue();
    String groupId = componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID);
    String artifactId = componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID);
    String version = componentIdentifier.get(ComponentIdentifier.VERSION);

    requestWaiver.requestWaiverReadOnlyData().shouldHave(text(groupId + " : " + artifactId + " : " + version));

    requestWaiver.requestWaiverReadOnlyData().shouldHave(text(violations.get(0).getPolicyName()));
    assertThat(violations.get(0).getConstraintFacts()).hasSize(1);
    ConstraintFact constraintFact = violations.get(0).getConstraintFacts().get(0);
    requestWaiver.requestWaiverReadOnlyData().shouldHave(text(constraintFact.getConstraintName()));
    requestWaiver.requestWaiverReadOnlyData().shouldHave(text(constraintFact.getConditionFacts().get(0).getReason()));

    requestWaiver.requestWaiverCancelButton().click();
    policyViolationsTable.shouldBe(visible);
  }

  @Test
  public void testSecurityTab_securityViolationTableEntries() {
    refreshOrOpen(ComponentDetailsPage.urlToSecurity(app, SCAN_ID, "197d803ab63dd3523d9d"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.securityTabContent().shouldBe(visible);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.securityTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(3);
    ElementsCollection rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(6);
    rowCells.shouldHave(exactTexts("9", "Security-High", "High risk CVSS score",
        "Found security vulnerability CVE-2016-9879 with severity >= 7 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with severity < 10 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with status 'Open', not 'Not Applicable'",
        "Add Waiver", ""));

    eyesWatcher.eyesCheck("component details security tab violation table add waiver");

    rowCells.get(4).find("button").click();
    AddWaiverPopover addWaiverPopover = new AddWaiverPopover();
    Button saveButton = addWaiverPopover.saveButton();
    saveButton.shouldBe(visible).click();

    componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.securityTabContent().shouldBe(visible);

    policyViolationsTable = componentDetailsPage.securityTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(3);
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(6);
    rowCells.shouldHave(exactTexts("9", "Security-High", "High risk CVSS score",
        "Found security vulnerability CVE-2016-9879 with severity >= 7 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with severity < 10 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with status 'Open', not 'Not Applicable'",
        "Unapplied Waiver", ""));

    eyesWatcher.eyesCheck("component details security tab violation table Unapplied waiver");

    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reevaluateButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ApplicationReportPage.AppReportHeaders reportHeaders = new ApplicationReportPage.AppReportHeaders();
    reportHeaders.componentNameFilterInput()
        .setValue("org.springframework.security : spring-security-web : 3.2.4.release");
    reportPage.resultRows().first().click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "197d803ab63dd3523d9d"));

    componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.securityTab().click();
    componentDetailsPage.securityTabContent().shouldBe(visible);

    policyViolationsTable = componentDetailsPage.securityTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(3);
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(6);
    rowCells.shouldHave(exactTexts("9", "Security-High", "High risk CVSS score",
        "Found security vulnerability CVE-2016-9879 with severity >= 7 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with severity < 10 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with status 'Open', not 'Not Applicable'",
        "1 Active Waiver", ""));

    eyesWatcher.eyesCheck("component details security tab violation table Active waiver");
  }

  @Test
  public void testSecurityTab_vulnerabilityTableEntries() {
    mockHdsResponseForFirstComponent();

    refreshOrOpen(ComponentDetailsPage.urlToSecurity(app, SCAN_ID, "1e48256a2341047e7d72"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.securityTabContent().shouldBe(visible);

    VulnerabilitiesTable vulnerabilitiesTable = componentDetailsPage.securityTabContent().vulnerabilitiesTable();
    vulnerabilitiesTable.shouldBe(visible);

    vulnerabilitiesTable.getHeaderRow().findAll(By.tagName("th"))
        .shouldHave(exactTexts("CVSS", "Problem Code", "Status", ""));

    vulnerabilitiesTable.getRows().shouldHaveSize(3);
    ElementsCollection rowCells = vulnerabilitiesTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(4);
    rowCells.shouldHave(exactTexts("9", "CVE-1234-56789", "Open", ""));
    rowCells = vulnerabilitiesTable.getRow(2).findAll(By.tagName("td"));
    rowCells.shouldHave(exactTexts("4", "OSVDB-1234", "Open", ""));
    rowCells = vulnerabilitiesTable.getRows().last().findAll(By.tagName("td"));
    rowCells.shouldHave(exactTexts("0", "OSVDB-4321", "Open", ""));

    eyesWatcher.eyesCheck("component details security tab vulnerabilities table entries");
  }

  @Test
  public void testSecurityTab_vulnerabilityDetailsPopover() {
    mockHdsResponsesForVulnerabilityDetails();

    refreshOrOpen(ComponentDetailsPage.urlToSecurity(app, SCAN_ID, "1e48256a2341047e7d72"));
    ComponentDetailsPage componentDetailsPage =  new ComponentDetailsPage();

    VulnerabilitiesTable vulnerabilitiesTable = componentDetailsPage.securityTabContent().vulnerabilitiesTable();
    vulnerabilitiesTable.shouldBe(visible);

    SelenideElement firstRow = vulnerabilitiesTable.getRows().first();
    firstRow.click();

    VulnerabilityDetailsPopover vulnerabilityDetailsPopover = new VulnerabilityDetailsPopover();
    vulnerabilityDetailsPopover.shouldBe(visible);

    vulnerabilityDetailsPopover.popoverTitle().shouldHave(text("Vulnerability Details"));
    vulnerabilityDetailsPopover.vulnerabilityTitle().shouldHave(text("CVE-1234-56789"));

    SelenideElement issueContent = vulnerabilityDetailsPopover.getSectionContentByIdx(1);
    issueContent.shouldHave(text("CVE-1234-56789"));

    SelenideElement severityContent = vulnerabilityDetailsPopover.getSectionContentByIdx(2);
    severityContent.shouldHave(text("Sonatype CVSS 3:9.1 CVE CVSS 2.0:0.0"));

    SelenideElement weaknessContent = vulnerabilityDetailsPopover.getSectionContentByIdx(3);
    weaknessContent.shouldHave(text("Sonatype CWE:400"));

    SelenideElement sourceContent = vulnerabilityDetailsPopover.getSectionContentByIdx(4);
    sourceContent.shouldHave(text("Sonatype Data Research"));

    eyesWatcher.eyesCheck("vulnerability details popover");

    SelenideElement closeButton = vulnerabilityDetailsPopover.getCloseButton();

    closeButton.click();

    vulnerabilityDetailsPopover.shouldNotBe(visible);
  }

  @Test
  public void testLegalTab_editLicensesPopover() {
    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "fa78f54738ccf77379d1"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);
    licenseDetectionsTile.editLicenseButton().click();

    EditLicensesPopover editLicensesPopover = new EditLicensesPopover();
    editLicensesPopover.shouldBe(visible);
    editLicensesPopover.popoverTitle().shouldHave(text("Edit Licenses"));

    ElementsCollection declaredLicenses = editLicensesPopover.getItems(editLicensesPopover.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection effectiveLicenses = editLicensesPopover.getItems(editLicensesPopover.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection observedLicenses = editLicensesPopover.getItems(editLicensesPopover.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("Not Provided"));

    eyesWatcher.eyesCheck("component details legal tab edit licenses popover");

    editLicensesPopover.getCloseButton().click();
    editLicensesPopover.shouldNotBe(visible);
  }

  @Test
  public void testLegalTab_licenseViolationTableEntries() {
    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "fa78f54738ccf77379d1"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.legalTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);

    ElementsCollection rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(6);

    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')",
        "Add Waiver", ""));

    eyesWatcher.eyesCheck("component details legal tab violation table add waiver");

    rowCells.get(4).find("button").click();

    AddWaiverPopover addWaiverPopover = new AddWaiverPopover();
    Button saveButton = addWaiverPopover.saveButton();
    saveButton.shouldBe(visible).click();

    componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    policyViolationsTable = componentDetailsPage.legalTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(6);
    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')",
        "Unapplied Waiver", ""));

    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reevaluateButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ApplicationReportPage.AppReportHeaders reportHeaders = new ApplicationReportPage.AppReportHeaders();
    reportHeaders.componentNameFilterInput().setValue("com.mycila : license-maven-plugin : 2.11");
    reportPage.resultRows().first().click();

    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "fa78f54738ccf77379d1"));

    componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTab().click();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    policyViolationsTable = componentDetailsPage.legalTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHaveSize(1);
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHaveSize(6);
    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')",
        "1 Active Waiver", ""));
  }

  @Test
  public void testLegalTab_licenseDetectionTile() {
    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "fa78f54738ccf77379d1"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("Not Provided"));

    licenseDetectionsTile.status().shouldHave(text("Status: Open"));
  }

  /* Part of testPolicyViolationsTab_violationTableEntries. */
  private void testGrandfatheringIndicator(final ComponentDetailsPage componentDetailsPage) {
    // Configure grandfathering indicator for the first violation in the report and reload it
    componentDetailsPage.backButton().click();
    activateGrandfathering();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));

    reportPage.aggregateByComponentToggle().click();
    SelenideElement firstGrandfatheredViolation = reportPage.resultRows().first();
    firstGrandfatheredViolation.click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, HASH));

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.getRows().shouldHaveSize(1);
    SelenideElement indicatorsCell = policyViolationsTable.getRows().first().findAll(By.tagName("td")).get(4);
    indicatorsCell.shouldHave(text("Grandfathered"));
    eyesWatcher.eyesCheck("component details violations tab violation table grandfathered row");
  }

  private void activateGrandfathering() {
    Policy licenseBannedPolicy = new PolicyDAO().getByName("License-Banned").get(0);

    app.setPolicyViolationGrandfatheringEnabled(true);
    licenseBannedPolicy.setPolicyViolationGrandfatheringAllowed(true);
    new ApplicationDAO().update(app);
    new PolicyDAO().update(licenseBannedPolicy);
    PolicyViolationGrandfatheringService policyViolationGrandfatheringService =
        testCLMServer.getCLMServer().getInstance(PolicyViolationGrandfatheringService.class);
    policyViolationGrandfatheringService.grandfather(app.getPublicId());
    try {
      evaluator.reevaluatePolicy();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testAuditLogTab_emptyMessage() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));

    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    componentDetailsPage.auditTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToAudit(app, SCAN_ID, HASH));

    AuditLogContent auditLog = componentDetailsPage.auditLogContent();
    auditLog.emptyMessage().shouldHave(text("No changes were found for this component"));
  }

  @Test
  public void testAuditLogTab_entries() {
    String dateRegex = "\\w{3} \\d{1,2}, \\d{4} \\d{1,2}:\\d{2}:\\d{2} (am|pm)";

    createAuditLogEntries();

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    componentDetailsPage.auditTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToAudit(app, SCAN_ID, HASH));

    AuditLogContent auditLog = componentDetailsPage.auditLogContent();
    SelenideElement date = auditLog.dateFromRow(0);
    date.should(matchText(dateRegex));
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));
  }

  @Test
  public void testAuditLogTab_sort() {
    createAuditLogEntries();

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID, true));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    componentDetailsPage.auditTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToAudit(app, SCAN_ID, HASH));

    AuditLogContent auditLog = componentDetailsPage.auditLogContent();
    // Sorted by time, descending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));

    ElementsCollection headers = auditLog.tableHeaders();
    headers.get(0).click();
    // Sorted by time, ascending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));

    headers.get(2).click();
    // Sorted by action, ascending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));

    headers.get(2).click();
    // Sorted by action, descending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));

    headers.get(4).click();
    // Sorted by comment, ascending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));

    headers.get(4).click();
    // Sorted by comment, descending
    auditLog.rowWithoutDate(0).shouldHave(texts("admin", "Reopened", "License Analysis", "BBBB"));
    auditLog.rowWithoutDate(1).shouldHave(texts("admin", "Acknowledged", "License Analysis", "AAAA"));
  }

  private void createAuditLogEntries() {
    // Using the CIP to create log entries.
    // Would need to change this to the form in the Component Details Page once the license tab is implemented.
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.first();
    firstViolation.click();

    mockHdsResponseForFirstComponent();
    CipModal cipModal = reportPage.cipModal();
    cipModal.tabLink(5).click();

    //Move some licenses' status so we can have some entries in audit log
    LicenseCIP.status().selectOption("Acknowledged");
    LicenseCIP.comment().setValue("AAAA");
    LicenseCIP.updateButton().shouldBe(enabled).click();
    // Navigate away and back
    LicenseCIP.status().selectOption("Open");
    LicenseCIP.comment().setValue("BBBB");
    LicenseCIP.updateButton().shouldBe(enabled).click();

    cipModal.closeButton().click();
  }

  private void mockHdsResponseForFirstComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-29.50.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
  }

  private void mockHdsResponsesForVulnerabilityDetails() {
    mockHdsResponseForFirstComponent();
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails_CVE-1234-56789.json"))
        .atUri("rest/vulnerability/details/json/CVE-1234-56789");
  }

  private ComponentDetailsPage openComponentDetailsPageForFirstViolation() {
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.first();
    firstViolation.click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, HASH));
    return new ComponentDetailsPage();
  }

  private ComponentDetailsPage openComponentDetailsPageForUnknownComponent() {
    reportPage.filterToggle().click();
    MatchStateFilter matchStateFilter = reportPage.filterPanel().matchStateFilter();
    matchStateFilter.click();
    matchStateFilter.unknown().click();

    ElementsCollection violations = reportPage.resultRows();
    SelenideElement unknownViolation = violations.first();
    unknownViolation.click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
    return new ComponentDetailsPage();
  }

  private ComponentDetailsPage openComponentDetailsPageForSimilarComponent() {
    final String similarComponentHash = "f0776db1593e215146d2";
    reportPage.filterToggle().click();
    MatchStateFilter matchStateFilter = reportPage.filterPanel().matchStateFilter();
    matchStateFilter.click();
    matchStateFilter.similar().click();

    ElementsCollection violations = reportPage.resultRows();
    SelenideElement similarComponentViolation = violations.first();
    similarComponentViolation.click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, similarComponentHash));
    return new ComponentDetailsPage();
  }

  private void navigateToComponentDetailsPageViolationsTab(final ComponentDetailsPage componentDetailsPage) {
    componentDetailsPage.violationsTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToViolations(app, SCAN_ID, HASH));
    componentDetailsPage.violationsTabContent().shouldBe(visible);
  }

  private void navigateToComponentDetailsPageSecurityTab(final ComponentDetailsPage componentDetailsPage) {
    componentDetailsPage.securityTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToSecurity(app, SCAN_ID, HASH));
    componentDetailsPage.securityTabContent().shouldBe(visible);
  }

  private void waiveFirstReportRow() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    WaiverApplierForReport.waiveReportRow(reportPage, 0);
  }
}

