/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.MatchStateFilter;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.ProprietaryFilter;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.componentdetails.ClaimTabContent;
import com.sonatype.clm.testing.functional.elements.componentdetails.ComponentCoordinatesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.ComponentInformationTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.ComponentInformationTile.IdentificationDefinitionList;
import com.sonatype.clm.testing.functional.elements.componentdetails.EditLicensesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.LegalTabContent;
import com.sonatype.clm.testing.functional.elements.componentdetails.LicenseDetectionsTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.ManageLabelsContentTab;
import com.sonatype.clm.testing.functional.elements.componentdetails.OccurrencesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationDetailPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.SimilarMatchesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilitiesTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilityDetailsPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilityDetailsPopover.VulnerabilityOverrideForm;
import com.sonatype.clm.testing.functional.pages.AddProprietaryComponentMatchersPopover;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.AuditLogContent;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover.ComponentWaiversPopoverTable;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage;
import com.sonatype.clm.testing.functional.pages.TransitiveViolationsPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.clm.testing.functional.utils.WaiverApplierForReport;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
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
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;

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
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(Objects.requireNonNull(
                    this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json")),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/legal/legalCommentHdsResponse.json")),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(Objects.requireNonNull(this.getClass()
                    .getResourceAsStream("/legal/ApplicationAttributionReportTest-legalFileHdsResponse.json")),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/file");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");

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
      refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
      reportPage.reportTitle().shouldHave(text("ApplicationReportTest Build Report"));

      ElementsCollection violations = reportPage.resultRows();
      SelenideElement firstViolation = violations.first();
      firstViolation.click();

      waitUntilUrl(ComponentDetailsPage.url(app, SCAN_ID, HASH));
      ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
      componentDetailsPage.header().title().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));
      componentDetailsPage.tabs().shouldHaveSize(6);

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
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));

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
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reportTitle().shouldHave(text("ApplicationReportTest Build Report"));

    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.first();
    firstViolation.click();

    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, HASH));
  }

  @Test
  public void testComponentDetailsTabNavigation() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
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

    componentDetailsPage.labelsTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToLabels(app, SCAN_ID, HASH));
  }

  @Test
  public void testComponentDetailsUnknownComponentAlert() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForUnknownComponent();

    SelenideElement unknownComponentAlert = componentDetailsPage.unknownComponentAlert();
    unknownComponentAlert.shouldBe(visible);

    SelenideElement proprietaryComponentAlert = componentDetailsPage.proprietaryComponentAlert();
    proprietaryComponentAlert.shouldNotBe(visible);
  }

  @Test
  public void testComponentDetailsProprietaryComponentAlert() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForProprietaryComponent();

    SelenideElement proprietaryComponentAlert = componentDetailsPage.proprietaryComponentAlert();
    proprietaryComponentAlert.shouldBe(visible);

    SelenideElement unknownComponentAlert = componentDetailsPage.unknownComponentAlert();
    unknownComponentAlert.shouldNotBe(visible);
  }

  @Test
  public void testComponentDetailsAddProprietaryComponentMatchersPopover() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForUnknownComponent();

    SelenideElement addProprietarypComponentMatchersBtn = componentDetailsPage.addProprietarypComponentMatchersBtn();

    AddProprietaryComponentMatchersPopover addProprietaryComponentMatchersPopover =
        new AddProprietaryComponentMatchersPopover();

    addProprietarypComponentMatchersBtn.click();
    addProprietaryComponentMatchersPopover.shouldBe(visible);
    addProprietaryComponentMatchersPopover.cancelBtn().click();
    addProprietaryComponentMatchersPopover.shouldNotBe(visible);

    addProprietarypComponentMatchersBtn.click();
    addProprietaryComponentMatchersPopover.shouldBe(visible);

    eyesWatcher.eyesCheck("component details Add Proprietary Component Matchers");

    addProprietaryComponentMatchersPopover.alerts().first()
        .shouldHave(text("The following matchers will be added to the ApplicationReportTest Configuration (duplicates"
        + " will be ignored). The new matchers will be in effect for the next application analysis."));
    addProprietaryComponentMatchersPopover.matchers().shouldHaveSize(1);
    addProprietaryComponentMatchersPopover.addBtn().shouldNotHave(DISABLED);
    addProprietaryComponentMatchersPopover.matchers().get(0)
        .shouldHave(text("full.jar/WebGoat-6.0.1/WEB-INF/classes/org/owasp/webgoat/lessons/instr"));
    addProprietaryComponentMatchersPopover.matchers().get(0).click();
    addProprietaryComponentMatchersPopover.addBtn().shouldHave(DISABLED);
    addProprietaryComponentMatchersPopover.matchers().get(0).click();
    addProprietaryComponentMatchersPopover.addBtn().click();
    addProprietaryComponentMatchersPopover.shouldNotBe(visible);
  }

  @Test
  public void testComponentDetails_UnknownComponentAlert_ClaimButton() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForUnknownComponent();

    SelenideElement claimButton = componentDetailsPage.unknownComponentClaim();
    claimButton.click();

    waitUntilUrl(ComponentDetailsPage.urlToClaim(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
    componentDetailsPage.claimTabContent().shouldBe(visible);
  }

  @Test
  public void testComponentDetails_ClaimTab() {
    refreshOrOpen(ComponentDetailsPage.urlToClaim(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    componentDetailsPage.claimTabContent().shouldBe(visible);

    componentDetailsPage.violationsTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToViolations(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));

    componentDetailsPage.overviewTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
  }

  @Test
  public void testComponentDetails_ClaimTabContent() {
    mockHdsResponseForClaimedComponent();
    refreshOrOpen(ComponentDetailsPage.urlToClaim(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    ClaimTabContent claimTabContent = componentDetailsPage.claimTabContent();
    claimTabContent.shouldBe(visible);

    claimTabContent.title().shouldHave(text("Claim Component"));

    claimTabContent.cancel().scrollIntoView(true).shouldBe(disabled);
    claimTabContent.claim().shouldHave(text("Claim")).shouldBe(CLM.DISABLED);
    claimTabContent.revoke().shouldBe(hidden);

    testRequiredFormFields(claimTabContent);
    fillAllFields(claimTabContent);

    eyesWatcher.eyesCheck("component details claim tab: claim component form");

    claimTabContent.cancel().scrollIntoView(true).shouldBe(enabled);
    claimTabContent.claim().shouldBe(enabled).click();
    FormMask.seeAndWaitForDismissal();
    claimTabContent.revoke().shouldBe(enabled);

    // Reevaluate to apply the claim
    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reevaluateButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));

    reportPage.headers().componentNameFilterInput().setValue("claimed");
    reportPage.resultRow(1).shouldHave(text("claimed : claimed : claimed : claimed : claimed")).click();

    componentDetailsPage.claimTabForClaimedComponent().click();
    waitUntilUrl(ComponentDetailsPage.urlToClaim(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
    claimTabContent.shouldBe(visible);

    checkLegalLicencesForClaimedComponent(componentDetailsPage);

    componentDetailsPage.claimTabForClaimedComponent().click();
    waitUntilUrl(ComponentDetailsPage.urlToClaim(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
    claimTabContent.shouldBe(visible);

    checkFieldsValue(claimTabContent);
    claimTabContent.revoke().shouldBe(enabled).click();

    NxDeleteModal deleteModal = claimTabContent.getDeleteModal();
    deleteModal.submitButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteModal.alertContent().shouldBe(hidden);

    // Reevaluate to revoke the claim
    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reevaluateButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.headers().componentNameFilterInput().setValue("claimed");
    reportPage.resultRows().shouldHaveSize(1);
    reportPage.resultRow(1).shouldHave(text("No Results"));

    reportPage.headers().componentNameFilterInput().setValue("regexmatch.dll");
    reportPage.resultRow(1).shouldHave(text("regexmatch.dll"));
  }

  @Test
  public void testOverviewTab_componentInformationTile() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    IdentificationDefinitionList identificationDefinitionList =
        componentDetailsPage.overviewTabContent().componentInformationTile().identificationDefinitionList();
    identificationDefinitionList.shouldBe(visible);
    identificationDefinitionList.getMatchStateItem().shouldHave(text("Match State exact"));
    identificationDefinitionList.getIdentificationSourceItem().shouldHave(text("Identification Source"));
    identificationDefinitionList.getWebsiteItem().shouldHave(text("Website"));
    identificationDefinitionList.getCategoryItem().shouldHave(text("Category"));

    identificationDefinitionList.getOccurrencesItem().shouldBe(visible);
    identificationDefinitionList.getOccurrencesItem().shouldHave(text("Occurrences 1 File"));

    eyesWatcher.eyesCheck("component details overview tab component information");
  }

  @Test
  public void testOverviewTab_componentCoordinatesPopover() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    componentDetailsPage.overviewTabContent().componentInformationTile().componentCoordinatesButton().click();
    ComponentCoordinatesPopover componentCoordinatesPopover = new ComponentCoordinatesPopover();

    componentCoordinatesPopover.shouldBe(visible);
    componentCoordinatesPopover.title().shouldHave(text("Component Coordinates"));
    componentCoordinatesPopover.typeDefinition().shouldHave(text("Type maven"));
    componentCoordinatesPopover.namingDefinitions()
        .shouldHave(exactTexts("Group com.mycila", "Artifact license-maven-plugin", "Version 2.11"));

    eyesWatcher.eyesCheck("component details component coordinates popover");

    componentCoordinatesPopover.closeButton().click();
    componentCoordinatesPopover.shouldNotBe(visible);
  }

  @Test
  public void testOverviewTab_componentCoordinatesPopover_unknownComponent_hideCoordinatesButton() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForUnknownComponent();

    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    ComponentInformationTile componentInformationTile =
        componentDetailsPage.overviewTabContent().componentInformationTile();
    componentInformationTile.componentCoordinatesButton().shouldNotBe(visible);
  }

  @Test
  public void testOverviewTab_OccurrencesPopover() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    IdentificationDefinitionList identificationDefinitionList =
        componentDetailsPage.overviewTabContent().componentInformationTile().identificationDefinitionList();
    identificationDefinitionList.shouldBe(visible);
    identificationDefinitionList.getOccurrencesItem().shouldHave(text("Occurrences 1 File"));
    identificationDefinitionList.getOccurrencesLink().click();

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
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForSimilarComponent();
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    IdentificationDefinitionList identificationDefinitionList =
        componentDetailsPage.overviewTabContent().componentInformationTile().identificationDefinitionList();
    identificationDefinitionList.shouldBe(visible);
    identificationDefinitionList.getMatchStateItem().shouldHave(text("Similar (View Similar Matches)"));
    identificationDefinitionList.getSimilarMatchesLink().click();

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
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
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
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));

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
    NxSubmitMask.seeAndWaitForDismissal();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
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
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
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
    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
    String dateString = dateFormat.format(Date.from(Instant.now()));
    waiveFirstReportRow();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
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
    row1Cells.shouldHave(texts("License-Banned\nLicense not approved in any situation",
        dateString,
        "Application - ApplicationReportTest",
        "com.mycila : license-maven-plugin : 2.11",
        "Admin BuiltIn",
        "- -",
        ""));
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
  public void testPolicyViolationsTab_viewTransitiveViolations() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement violation = violations.get(15);
    violation.click();
    String innerSourceComponentHash = "952da051fc959b215c8e";
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, innerSourceComponentHash));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.violationsTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToViolations(app, SCAN_ID, innerSourceComponentHash));
    componentDetailsPage.violationsTabContent().shouldBe(visible);
    componentDetailsPage.violationsTabContent().componentTransitiveViolationsButton().click();
    waitUntilUrl(TransitiveViolationsPage.url(app.getPublicId(), SCAN_ID, innerSourceComponentHash));
    TransitiveViolationsPage transitiveViolationsPage = new TransitiveViolationsPage();
    transitiveViolationsPage.shouldBe(visible);
    transitiveViolationsPage.backButton().shouldBe(visible).click();
    waitUntilUrl(ComponentDetailsPage.urlToViolations(app, SCAN_ID, innerSourceComponentHash));
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
        "", ""));

    eyesWatcher.eyesCheck("component details security tab violation table no waiver");

    addWaiver(policyViolationsTable);

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
        "Unapplied Waiver", ""));

    eyesWatcher.eyesCheck("component details security tab violation table Unapplied waiver");

    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reevaluateButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
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
    rowCells = policyViolationsTable.getRows().last().findAll(By.tagName("td"));
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
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

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
    severityContent.shouldHave(text("Sonatype CVSS 39.1 CVE CVSS 2.00.0"));

    SelenideElement weaknessContent = vulnerabilityDetailsPopover.getSectionContentByIdx(3);
    weaknessContent.shouldHave(text("Sonatype CWE400"));

    SelenideElement sourceContent = vulnerabilityDetailsPopover.getSectionContentByIdx(4);
    sourceContent.shouldHave(text("Sonatype Data Research"));

    VulnerabilityOverrideForm vulnerabilityOverrideForm = vulnerabilityDetailsPopover.getVulnerabilityOverrideForm();
    vulnerabilityOverrideForm.shouldBe(visible);
    vulnerabilityOverrideForm.comment().shouldNotBe(visible);
    vulnerabilityOverrideForm.submitButton().shouldBe(CLM.DISABLED);

    vulnerabilityOverrideForm.status().chooseOption(new Option(3, "CONFIRMED"));
    vulnerabilityOverrideForm.comment().shouldBe(visible);
    vulnerabilityOverrideForm.comment().shouldBe(enabled);
    vulnerabilityOverrideForm.comment().setValue("vulnerability confirmed in the current code");
    vulnerabilityOverrideForm.submitButton().shouldBe(enabled).click();
    // Conditions after submitting
    vulnerabilityOverrideForm.submitButton().shouldBe(CLM.DISABLED);
    vulnerabilityOverrideForm.comment().shouldBe(enabled);

    eyesWatcher.eyesCheck("vulnerability details popover");

    SelenideElement closeButton = vulnerabilityDetailsPopover.getCloseButton();

    closeButton.click();

    vulnerabilityDetailsPopover.shouldNotBe(visible);

    vulnerabilitiesTable.getRows().first().findAll(By.tagName("td"))
        .shouldHave(exactTexts("9", "CVE-1234-56789", "Confirmed", ""));
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
        "", ""));

    eyesWatcher.eyesCheck("component details legal tab violation table no waiver");

    addWaiver(policyViolationsTable);

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
        "Unapplied Waiver", ""));

    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reevaluateButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
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
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));

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
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));

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

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
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

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
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

  @Test
  public void testLabelsTab_manageLabels() {
    // Create app level label, apply to component
    Label appLevelLabel = tempEntity.newLabel(app.getId(), "app level label", Color.dark_red);
    tempEntity.newComponentLabel(app.getId(), appLevelLabel.getId(),"fa78f54738ccf77379d1");

    // Go to details page, verify manage labels content appears
    refreshOrOpen(ComponentDetailsPage.urlToLabels(app, SCAN_ID, "fa78f54738ccf77379d1"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    ManageLabelsContentTab manageLabels = componentDetailsPage.labelsContent();
    manageLabels.shouldBe(visible);
    manageLabels.appliedLabels().shouldHaveSize(1);
    manageLabels.applicableLabels().shouldHaveSize(3);

    // Remove applied app level label
    manageLabels.appliedLabels().get(0).should(exist).click();
    manageLabels.removeLabelModal().should(exist);
    // Screenshot remove label modal
    eyesWatcher.eyesCheck("Remove Label Modal");
    // Confirm removal and verify labels count
    manageLabels.removeLabelModal().confirmRemoveButton().should(exist).click();
    manageLabels.appliedLabels().shouldHaveSize(0);
    manageLabels.applicableLabels().shouldHaveSize(4);

    // Adding first label
    manageLabels.applicableLabelText(0).shouldHave(text("Architecture-Blacklisted"));
    manageLabels.applicableLabels().get(0).should(exist).click();
    manageLabels.addLabelModal().should(exist);
    // Screenshot the add label modal
    eyesWatcher.eyesCheck("Add Label Modal");
    // Add and confirm
    manageLabels.addLabelModal().labelsScopeRadioButton(0).should(exist).click();
    manageLabels.addLabelModal().submitButton().shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    manageLabels.appliedLabelText(0).shouldHave(text("Architecture-Blacklisted"));

    // Adding app level label
    manageLabels.applicableLabelText(2).shouldHave(text("app level label"));
    manageLabels.applicableLabels().get(2).should(exist).click();
    // No modal should appear, the label should just be added right away
    manageLabels.addLabelModal().shouldNot(exist);
    manageLabels.appliedLabelText(0).shouldHave(text("app level label"));

    // Confirm additions
    manageLabels.appliedLabels().shouldHaveSize(2);
    eyesWatcher.eyesCheck("Labels Tab");
  }

  private void createAuditLogEntries() {
    // Using the CIP to create log entries.
    // Would need to change this to the form in the Component Details Page once the license tab is implemented.
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.first();
    firstViolation.click();

    mockHdsResponseForFirstComponent();

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTab().shouldBe(visible).click();
    LegalTabContent tabContent = componentDetailsPage.legalTabContent();
    tabContent.shouldBe(visible);

    SelenideElement editLicenseButton = tabContent.licenseDetectionsTile().editLicenseButton();
    editLicenseButton.shouldBe(visible).click();

    EditLicensesPopover editLicensesPopover = new EditLicensesPopover();
    editLicensesPopover.shouldBe(visible);

    //Move some licenses' status so we can have some entries in audit log
    editLicensesPopover.status().selectOption("Acknowledged");
    editLicensesPopover.comment().setValue("AAAA");
    editLicensesPopover.saveButton().shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();

    editLicensesPopover.status().selectOption("Open");
    editLicensesPopover.comment().setValue("BBBB");
    editLicensesPopover.saveButton().shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();

    editLicensesPopover.getCloseButton().click();
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

  private ComponentDetailsPage openComponentDetailsPageForProprietaryComponent() {
    reportPage.filterToggle().click();
    ProprietaryFilter proprietaryFilter = reportPage.filterPanel().proprietaryFilter();
    proprietaryFilter.click();
    proprietaryFilter.proprietary().click();

    ElementsCollection violations = reportPage.resultRows();
    SelenideElement proprietaryViolation = violations.first();
    proprietaryViolation.click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "81399f9f3278d8615a7c"));
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

  private void testRequiredFormFields(ClaimTabContent claimTabContent) {
    for (SelenideElement element : claimTabContent.requiredFields()) {
      element.sendKeys("a");
      element.sendKeys(Keys.BACK_SPACE);
      ClaimTabContent.getInputValidationElement(element).shouldHave(text("Must be non-empty"));
    }
  }

  private void fillAllFields(ClaimTabContent claimTabContent) {
    for (SelenideElement element : claimTabContent.allTextFields()) {
      element.sendKeys("claimed");
    }

    claimTabContent.createdTime().setValue("20102021"); //20.10.2021
  }

  private void checkFieldsValue(ClaimTabContent claimTabContent) {
    for (SelenideElement element : claimTabContent.allTextFields()) {
      element.shouldHave(value("claimed"));
    }
  }

  private void mockHdsResponseForClaimedComponent() {
    testCLMServer.getHdsServer().respondWith("{\"known\":false}").atUri("rest/component/summary");
  }

  private void checkLegalLicencesForClaimedComponent(ComponentDetailsPage componentDetailsPage) {
    componentDetailsPage.legalTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));

    componentDetailsPage.legalTabContent().shouldBe(visible);

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Not Provided (Claimed Component)"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(1);
    effectiveLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("Not Provided (Claimed Component)"));
  }

  /**
   * This method is a convenience method to click on a policy violation row,
   * click on manage waivers, go to the list waivers page, click on add waiver,
   * submit and return to the policy violation table page.
   * @param policyViolationsTable instance of the PolicyViolationsTable whose row needs to be clicked
   */
  private void addWaiver(PolicyViolationsTable policyViolationsTable) {
    WaiverApplierForReport.waiveViolationFromTable(policyViolationsTable, 1);
  }
}
