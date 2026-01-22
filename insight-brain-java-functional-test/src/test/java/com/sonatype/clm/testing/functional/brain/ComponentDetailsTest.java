/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.KevData;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.MatchStateFilter;
import com.sonatype.clm.testing.functional.elements.ApplicationReportFilter.ProprietaryFilter;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.ListSimilarWaiversTable;
import com.sonatype.clm.testing.functional.elements.ListSimilarWaiversTable.ListSimilarWaiversTableRow;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable.ListWaiversTableRow;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
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
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover;
import com.sonatype.clm.testing.functional.pages.ComponentWaiversPopover.ComponentWaiversPopoverTable;
import com.sonatype.clm.testing.functional.pages.CustomizeVulnerabilityDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.DeleteWaiverModal;
import com.sonatype.clm.testing.functional.pages.LegalApplicationDetailsPage;
import com.sonatype.clm.testing.functional.pages.RequestWaiverPage;
import com.sonatype.clm.testing.functional.pages.TransitiveViolationsPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationSimilarWaiversInfoTile;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.clm.testing.functional.utils.SimilarWaiverCreator;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.clm.testing.functional.utils.WaiverApplierForReport;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.policy.LegacyViolationService;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.mock.hds.HdsMockServer;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilitySeverity;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilityWeakness;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilityWeakness.CweId;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentDetailsTest
    extends AbstractFunctionalTest
{
  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String HASH = "fa78f54738ccf77379d1";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private PolicyDAO policyDAO;

  private ApplicationDAO applicationDAO;

  private Organization parentOrg;

  private Organization org;

  private Application app;

  private Application otherApp;

  private TestReportEvaluator evaluator;

  private SimilarWaiverCreator similarWaiverCreator;

  private PolicyViolationDAO policyViolationDAO;

  private Configuration configurationService;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    policyDAO = lookup(PolicyDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    configurationService = lookup(Configuration.class);

    assertThat(configurationService.isALPObservedLicenseDetectionEnabled()).isTrue();

    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);
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

    parentOrg = tempEntity.newOrganization("Parent Organization");
    org = tempEntity.newOrganization("Test Organization", parentOrg);
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportTest", "ApplicationReportTest", org.getId());
    otherApp = tempEntity.newApplication("OtherApplicationReportTest", "OtherApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();

    similarWaiverCreator = new SimilarWaiverCreator(zippedReport, otherApp, testCLMServer,
        AbstractFunctionalTest::refreshOrOpen, baseUrlFromTest);
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
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
      componentDetailsPage.tabs().shouldHave(size(6));

      NxBackButton backButton = MainHeader.backButton();
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

    componentDetailsPage.footer().paginationCounter().shouldHave(text("5 of 65"));
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
    addProprietaryComponentMatchersPopover.matchers().shouldHave(size(1));
    FormUtils.getAlertElement(addProprietaryComponentMatchersPopover).shouldNotBe(visible);
    addProprietaryComponentMatchersPopover.matchers().get(0)
        .shouldHave(text("full.jar/WebGoat-6.0.1/WEB-INF/classes/org/owasp/webgoat/lessons/instr"));
    addProprietaryComponentMatchersPopover.matchers().get(0).click();
    addProprietaryComponentMatchersPopover.addBtn().click();
    FormUtils.getAlertElement(addProprietaryComponentMatchersPopover)
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " Unable to add: Fields with invalid or missing data."));
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
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    mockHdsResponseForClaimedComponent();
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    refreshOrOpen(ComponentDetailsPage.urlToClaim(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    ClaimTabContent claimTabContent = componentDetailsPage.claimTabContent();
    claimTabContent.shouldBe(visible);

    claimTabContent.title().shouldHave(text("Claim Component"));

    claimTabContent.cancel().scrollIntoView(true).shouldBe(disabled);
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
    reportPage.fullReevaluateButton().click();
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
    reportPage.fullReevaluateButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.headers().componentNameFilterInput().setValue("claimed");
    reportPage.resultRows().shouldHave(size(1));
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

    componentCoordinatesPopover.copyToClipboard().shouldHave(text("Copy to Clipboard\nPackage URL"));

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
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
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
    policyViolationsTable.getRows().shouldHave(size(1));
    ElementsCollection rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHave(size(1));
    rowCells.get(0).shouldHave(text("No policy violations"));

    waiveFirstReportRow();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));

    componentDetailsPage = openComponentDetailsPageForFirstViolation();
    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);
    policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(1));
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHave(size(7));

    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')", "-", "Unapplied Waiver", ""));

    eyesWatcher.eyesCheck("component details violations tab violation table unapplied waiver");

    // Reevaluate to apply the waiver and apply appropriate filter to show in the report
    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reevaluateButton().click();
    reportPage.fullReevaluateButton().click();
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
    policyViolationsTable.getRows().shouldHave(size(1));
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHave(size(7));
    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')", "-", "Waived", ""));
    testLegacyViolationIndicator(componentDetailsPage);
  }

  @Test
  public void testPolicyViolationsTab_violationTableEntries_UnappliedWaiversShouldShowForSpecificViolation() {
    refreshOrOpen(ComponentDetailsPage.urlToViolations(app, SCAN_ID, "197d803ab63dd3523d9d"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.violationsTabContent().shouldBe(visible);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    // 1st row = Level 9 - Security-High, 2nd row = Level 7 - Security-Medium, 3rd row = Level 7 - Security-Medium
    policyViolationsTable.getRows().shouldHave(size(3));

    // Apply waiver for 2nd violation of Level 7 - Security-Medium
    WaiverApplierForReport.waiveViolationFromTable(policyViolationsTable, 2);

    componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.violationsTab().click();
    componentDetailsPage.violationsTabContent().shouldBe(visible);

    policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(3));

    SelenideElement unappliedWaiverCell = policyViolationsTable.getRows().get(0).findAll(By.tagName("td")).get(5);
    unappliedWaiverCell.shouldHave(text("Open"));

    // Only the waiver applied for specific violation should have the unapplied waiver label
    unappliedWaiverCell = policyViolationsTable.getRows().get(1).findAll(By.tagName("td")).get(5);
    unappliedWaiverCell.shouldHave(text("Unapplied Waiver"));

    unappliedWaiverCell = policyViolationsTable.getRows().get(2).findAll(By.tagName("td")).get(5);
    unappliedWaiverCell.shouldHave(text("Open"));
  }

  @Test
  public void testPolicyViolationsTab_switchingFromSecurityToNonSecurityViolationDetails() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement fifthViolation = violations.get(4);
    fifthViolation.click();
    waitUntilSpinnersGone();

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    componentDetailsPage.violationsTab().shouldBe(visible).click();

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    // test switching from security to non-security while "Vulnerability Details" tab is selected
    ElementsCollection firstRow = policyViolationsTable.getCellsByNthRow(1);

    waitUntilSpinnersGone();

    firstRow.get(0).shouldBe(visible).click();
    PolicyViolationDetailPopover violationDetailPopover = new PolicyViolationDetailPopover();
    violationDetailPopover.shouldBe(visible);
    violationDetailPopover.applicableWaiversTab().shouldHave(cssClass("active"));

    ElementsCollection lastRow = policyViolationsTable.getCellsByNthRow(4);
    lastRow.get(1).shouldBe(visible).shouldHave(text("Component-Similar")).click();
    violationDetailPopover.shouldBe(visible);
    ListWaiversTable applicableWaiversTable =
        violationDetailPopover.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTable.noWaiversMessage().shouldBe(visible);

    // now test switching from security to non-security while "Applicable Waivers" tab is selected
    firstRow.get(0).shouldBe(visible).click();
    violationDetailPopover.shouldBe(visible);
    violationDetailPopover.applicableWaiversTab().shouldBe(visible).click();
    applicableWaiversTable.noWaiversMessage().shouldBe(visible);

    lastRow.get(1).shouldBe(visible).click();
    violationDetailPopover.shouldBe(visible);
    applicableWaiversTable.noWaiversMessage().shouldBe(visible);
  }

  @Test
  public void testPolicyViolationsTab_ViewDetailsPopover() {
    waiveFirstReportRow();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(1));
    SelenideElement row = policyViolationsTable.getRows().first();
    row.click();
    PolicyViolationDetailPopover violationDetailPopover = new PolicyViolationDetailPopover();
    violationDetailPopover.shouldBe(visible);
    SelenideElement closeButton = violationDetailPopover.getCloseButton();
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();
    eyesWatcher.eyesCheck("policy violation details popover");

    violationDetailPopover.headerPopoverTitle().shouldHave(text("Violation of License-Banned"));

    tile.firstReported().shouldHave(text("Just now"));
    tile.lastReported().shouldHave(text("Just now"));
    tile.policyType().shouldHave(text("License"));
    tile.threatLevel().shouldHave(text("10"));
    tile.policyOwnerLink().shouldHave(text("Test Organization"));

    tile.stages().shouldHave(size(5));

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

    violationDetailPopover.applicableWaiversTab().shouldBe(visible).click();
    ListWaiversTable applicableWaiversTable =
        violationDetailPopover.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTable.rows().shouldHave(size(1));
    ListWaiversTableRow waiversTableRow = applicableWaiversTable.row(1);
    waiversTableRow.components().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));

    violationDetailPopover.similarWaiversTab().shouldBe(visible).click();
    ListSimilarWaiversTable similarWaiversTable =
        violationDetailPopover.similarWaiversInfoTile().getSimilarWaiversTable();
    similarWaiversTable.rows().shouldHave(size(1));
    similarWaiversTable.noWaiversMessage().shouldBe(visible);

    similarWaiverCreator.createSimilarWaiver();

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));

    navigateToComponentDetailsPageViolationsTab(openComponentDetailsPageForFirstViolation());

    row.click();

    violationDetailPopover.similarWaiversTab().shouldBe(visible).click();
    similarWaiversTable.rows().shouldHave(size(1));
    ListSimilarWaiversTableRow similarWaiversTableRow = similarWaiversTable.row(1);
    similarWaiversTableRow.components().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));
  }

  @Test
  public void testPolicyViolationsTab_FilterSimilarWaivers() {
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();
    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);
    List<PolicyViolation> policyViolations = policyViolationDAO.getByApplicationId(app.getId()).stream()
        .filter(policyViolation -> policyViolation.getHash().equals(HASH)).collect(
            Collectors.toList());

    // 1 app is needed per waiver for it to show in Similar waivers
    Application app2 = tempEntity.newApplication("ApplicationReportTest1", "ApplicationReportTest1", org.getId());
    Application app3 = tempEntity.newApplication("ApplicationReportTest2", "ApplicationReportTest2", org.getId());
    Application app4 = tempEntity.newApplication("ApplicationReportTest3", "ApplicationReportTest3", org.getId());
    Application app5 = tempEntity.newApplication("ApplicationReportTest4", "ApplicationReportTest4", org.getId());
    Date futureDate = new Date(System.currentTimeMillis() + 60_000);
    Date pastDate = new Date(System.currentTimeMillis() - 60_000);
    // Create waiver with and without comments, active and expired
    ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 0, "foo", "bar");
    conditionFact.setTriggerJson("{\"conditionIndex\":1,\"trigger\":{\"refId\":\"1234\",\"severity\":5.7}}");
    ConstraintFact constraintFact = new ConstraintFact("id", "name", LogicalOperator.AND.toString());
    constraintFact.addConditionFact(conditionFact);

    tempEntity.newWaiver(HASH, policyViolations.get(0).getPolicyId(), app2.getId(),
        Collections.singletonList(constraintFact), "", new Date(), pastDate);
    tempEntity.newWaiver(HASH, policyViolations.get(0).getPolicyId(), app3.getId(),
        Collections.singletonList(constraintFact), "some comment");
    tempEntity.newWaiver(HASH, policyViolations.get(0).getPolicyId(), app4.getId(),
        Collections.singletonList(constraintFact), "");
    tempEntity.newWaiver(HASH, policyViolations.get(0).getPolicyId(), app5.getId(),
        Collections.singletonList(constraintFact), "some other comment", new Date(), futureDate);

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    navigateToComponentDetailsPageViolationsTab(openComponentDetailsPageForFirstViolation());
    PolicyViolationDetailPopover violationDetailPopover = new PolicyViolationDetailPopover();
    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    ListSimilarWaiversTable similarWaiversTable =
        violationDetailPopover.similarWaiversInfoTile().getSimilarWaiversTable();
    SelenideElement row = policyViolationsTable.getRows().first();

    row.click();

    violationDetailPopover.similarWaiversTab().shouldBe(visible).click();
    similarWaiversTable.rows().shouldHave(size(4));
    eyesWatcher.eyesCheck("Similar waivers tab");
    PolicyViolationSimilarWaiversInfoTile similarWaiversInfoTile =
        violationDetailPopover.similarWaiversInfoTile();
    similarWaiversInfoTile.filterDropdown().click();
    similarWaiversInfoTile.activeFilter().click();
    similarWaiversTable.rows().shouldHave(size(3));
    similarWaiversInfoTile.activeFilter().click();
    similarWaiversInfoTile.commentFilter().click();
    similarWaiversTable.rows().shouldHave(size(2));
    similarWaiversInfoTile.commentFilter().click();
    similarWaiversInfoTile.exactFilter().click();
    similarWaiversTable.rows().shouldHave(size(4));
  }

  @Test
  public void testPolicyViolationsTab_ClearFilterSimilarWaiversOnNavigation() {
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    navigateToComponentDetailsPageViolationsTab(openComponentDetailsPageForFirstViolation());
    PolicyViolationDetailPopover violationDetailPopover = new PolicyViolationDetailPopover();
    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    SelenideElement row = policyViolationsTable.getRows().first();

    row.click();

    violationDetailPopover.similarWaiversTab().shouldBe(visible).click();
    PolicyViolationSimilarWaiversInfoTile similarWaiversInfoTile =
        violationDetailPopover.similarWaiversInfoTile();
    similarWaiversInfoTile.filterDropdown().click();
    similarWaiversInfoTile.activeFilter().click();
    similarWaiversInfoTile.activeFilterCheckbox().shouldBe(visible);

    componentDetailsPage.overviewTab().click();
    componentDetailsPage.violationsTab().click();

    row.click();

    violationDetailPopover.similarWaiversTab().shouldBe(visible).click();
    similarWaiversInfoTile.filterDropdown().click();
    similarWaiversInfoTile = violationDetailPopover.similarWaiversInfoTile();
    similarWaiversInfoTile.filterDropdown().click();
    similarWaiversInfoTile.activeFilterCheckbox().shouldNotBe(visible);
  }

  @Test
  public void testPolicyViolationsTab_RequestWaiver() {
    waiveFirstReportRow();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(1));
    SelenideElement row = policyViolationsTable.getRows().first();
    row.click();
    PolicyViolationDetailPopover violationDetailPopover = new PolicyViolationDetailPopover();
    violationDetailPopover.shouldBe(visible);

    violationDetailPopover.getAddWaiversSegmentedDropdownButton().shouldBe(visible).click();
    violationDetailPopover.getRequestWaiversButton().shouldBe(visible).click();
    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.root().shouldBe(visible);
    requestWaiverPage.requestWaiverHeader().shouldHave(text("Request Waiver"));
    requestWaiverPage.requestWaiverTitle().shouldHave(text("Waiver Configuration"));

    requestWaiverPage.requestWaiverComponentName().shouldHave(text("com.mycila : license-maven-plugin : 2.11"));

    requestWaiverPage.requestWaiverPolicy().shouldHave(text("Policy"));
    requestWaiverPage.requestWaiverPolicy().shouldHave(text("License-Banned"));

    requestWaiverPage.requestWaiverConstraint().shouldHave(text("Constraint Name"));
    requestWaiverPage.requestWaiverConstraint().shouldHave(text("License not approved in any situation"));

    requestWaiverPage.requestWaiverConditions().shouldHave(text("Conditions"));
    requestWaiverPage.requestWaiverConditions()
        .shouldHave(text("Found licenses in the 'Banned' license threat group ('AGPL-3.0')"));

    requestWaiverPage.requestWaiverScope().shouldHave(text("Scope"));
    requestWaiverPage.requestWaiverScopeOptions().shouldHave(size(2));
    requestWaiverPage.requestWaiverScopeOptions().shouldHave(
        exactTexts("Application - ApplicationReportTest", "Organization - Test Organization"));
    requestWaiverPage.requestWaiverScopeOptions().get(0).shouldBe(selected);

    requestWaiverPage.requestWaiverComponents().shouldHave(text("Components"));
    requestWaiverPage.requestWaiverComponentsOptions().shouldHave(size(3));
    requestWaiverPage.requestWaiverComponentsOptions().shouldHave(
        exactTexts("com.mycila : license-maven-plugin : 2.11", "com.mycila : license-maven-plugin (all versions)",
            "All Components"));
    requestWaiverPage.requestWaiverComponentsRadios().get(0).shouldBe(checked);

    requestWaiverPage.requestWaiverExpiryTime().shouldHave(text("Waiver Expiration"));
    requestWaiverPage.requestWaiverExpiryTimeOptions().shouldHave(size(8));
    requestWaiverPage.requestWaiverExpiryTimeOptions().shouldHave(exactTexts("Never", "7 Days", "14 Days", "30 Days",
        "60 Days", "90 Days", "120 Days", "Custom"));
    requestWaiverPage.requestWaiverExpiryTimeOptions().get(0).shouldBe(selected);

    requestWaiverPage.requestWaiverReason().shouldHave(text("Reason"));
    requestWaiverPage.requestWaiverReasonOptions().shouldHave(
        exactTexts("Select a reason", "Acknowledged violation", "Evaluating component", "Mitigated externally",
            "No upgrade path", "Not exploitable", "Not reachable", "Researching", "Other"));
    requestWaiverPage.requestWaiverReasonOptions().get(0).shouldBe(selected);

    requestWaiverPage.requestWaiverComments().shouldBe(empty);
    requestWaiverPage.requestWaiverNoteToReviewer().shouldBe(empty);

    requestWaiverPage.saveButton().shouldBe(visible);
    requestWaiverPage.cancelButton().shouldBe(visible);
  }

  @Test
  public void testPolicyViolationsTab_ViewDetailsPopover_customizeButton() {
    String refIdForFirstClickableTableRowInPolicyViolationTable = "CVE-2016-9879";
    mockHdsResponseForVulnerabilityDetailsWithRefId(refIdForFirstClickableTableRowInPolicyViolationTable);
    tempEntity.newVulnerabilityCustomData(app.getId(), refIdForFirstClickableTableRowInPolicyViolationTable, null,
        "Test remediation", "123", "test/vector", 8.0F);
    String springSecurityWebComponentHash = "197d803ab63dd3523d9d";
    refreshOrOpen(ComponentDetailsPage.urlToViolations(app, SCAN_ID, springSecurityWebComponentHash));

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);

    SelenideElement firstRow = policyViolationsTable.getRows().first();
    firstRow.click();

    PolicyViolationDetailPopover violationDetailPopover = new PolicyViolationDetailPopover();

    SelenideElement customizeButton = violationDetailPopover.getCustomizeButton();
    customizeButton.shouldBe(visible);
    customizeButton.click();

    CustomizeVulnerabilityDetailsPage.refIdTitle().shouldBe(visible);
    CustomizeVulnerabilityDetailsPage.refIdTitle().shouldBe(
        text(refIdForFirstClickableTableRowInPolicyViolationTable));

    NxBackButton backButton = CustomizeVulnerabilityDetailsPage.backButton();
    backButton.shouldBe(visible);
    backButton.shouldHave(text("Back to Violation Details"));
    backButton.click();

    waitUntilUrl(ComponentDetailsPage.urlToViolations(app, SCAN_ID, springSecurityWebComponentHash));
  }

  @Test
  public void testPolicyViolationsTab_viewAllComponentWaivers() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
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
    componentWaiversTable.getRows().shouldHave(size(1));

    SelenideElement row1 = componentWaiversTable.getRow(1);
    ElementsCollection row1Cells = row1.findAll(".nx-cell");
    row1Cells.shouldHave(texts("Created\n" +
            dateString + "\n" +
            "Expiration\n" +
            "Does not expire",
        "Scope\n" +
            "Application - ApplicationReportTest\n" +
            "Component\n" +
            "com.mycila : license-maven-plugin : 2.11\n" +
            "Reason\n" +
            "—\n" +
            "Author\n" +
            "Admin BuiltIn",
        ""));
    eyesWatcher.eyesCheck("component details violations tab component waivers popover");
    componentWaiversTable.deleteWaiverButton(1).click();

    DeleteWaiverModal deleteWaiverModal = new DeleteWaiverModal();
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
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    refreshOrOpen(ComponentDetailsPage.urlToSecurity(app, SCAN_ID, "197d803ab63dd3523d9d"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.securityTabContent().shouldBe(visible);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.securityTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(3));
    ElementsCollection rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHave(size(7));
    rowCells.shouldHave(exactTexts("9", "Security-High", "High risk CVSS score",
        "Found security vulnerability CVE-2016-9879 with severity >= 7 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with severity < 10 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with status 'Open', not 'Not Applicable'",
        "-", "Open", ""));

    addWaiver(policyViolationsTable);

    componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.securityTab().click();
    componentDetailsPage.securityTabContent().shouldBe(visible);

    policyViolationsTable = componentDetailsPage.securityTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(3));
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHave(size(7));
    rowCells.shouldHave(exactTexts("9", "Security-High", "High risk CVSS score",
        "Found security vulnerability CVE-2016-9879 with severity >= 7 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with severity < 10 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with status 'Open', not 'Not Applicable'",
        "-", "Unapplied Waiver", ""));

    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reevaluateButton().click();
    reportPage.fullReevaluateButton().click();
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
    policyViolationsTable.getRows().shouldHave(size(3));
    rowCells = policyViolationsTable.getRows().last().findAll(By.tagName("td"));
    rowCells.shouldHave(size(7));
    rowCells.shouldHave(exactTexts("9", "Security-High", "High risk CVSS score",
        "Found security vulnerability CVE-2016-9879 with severity >= 7 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with severity < 10 (severity = 7.5) "
            + "Found security vulnerability CVE-2016-9879 with status 'Open', not 'Not Applicable'",
        "-", "Waived", ""));

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
        .shouldHave(exactTexts("CVSS", "ISSUES","DATA ENRICHMENT", "STATUS", ""));

    vulnerabilitiesTable.getRows().shouldHave(size(3));
    ElementsCollection rowCells = vulnerabilitiesTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHave(size(5));
    rowCells.shouldHave(exactTexts("9", "CVE-1234-56789", "Sonatype Enhanced", "Open", ""));
    rowCells = vulnerabilitiesTable.getRow(2).findAll(By.tagName("td"));
    rowCells.shouldHave(exactTexts("4", "OSVDB-1234", "Public Data", "Open", ""));
    rowCells = vulnerabilitiesTable.getRows().last().findAll(By.tagName("td"));
    rowCells.shouldHave(exactTexts("0", "OSVDB-4321", "", "Open", ""));
  }

  @Test
  public void testSecurityTab_vulnerabilityDetailsPopover() {
    mockHdsResponsesForVulnerabilityDetails();
    tempEntity.newVulnerabilityCustomData(app.getId(), "CVE-1234-56789", null, "Test remediation", "123", "test/vector",
        8.0F);

    refreshOrOpen(ComponentDetailsPage.urlToSecurity(app, SCAN_ID, "1e48256a2341047e7d72"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    VulnerabilitiesTable vulnerabilitiesTable = componentDetailsPage.securityTabContent().vulnerabilitiesTable();
    vulnerabilitiesTable.shouldBe(visible);

    SelenideElement firstRow = vulnerabilitiesTable.getRows().first();
    firstRow.click();

    VulnerabilityDetailsPopover vulnerabilityDetailsPopover = new VulnerabilityDetailsPopover();
    waitUntilVulnerabilityDetailsPopoverIsVisible();
    vulnerabilityDetailsPopover.shouldBe(visible);

    vulnerabilityDetailsPopover.popoverTitle().shouldHave(text("Vulnerability Details"));
    vulnerabilityDetailsPopover.vulnerabilityTitle().shouldHave(text("CVE-1234-56789"));

    SelenideElement customRemediationContent = vulnerabilityDetailsPopover.getCustomRemediationSection();
    customRemediationContent.shouldHave(text("Test remediation"));

    SelenideElement issueContent = vulnerabilityDetailsPopover.getSectionContentByIdx(1);
    issueContent.shouldHave(text("CVE-1234-56789"));

    SelenideElement severityContent = vulnerabilityDetailsPopover.getSectionContentByIdx(2);
    severityContent.shouldHave(text("Severity8.0 (Custom)"));
    severityContent.shouldHave(text("Sonatype CVSS 39.1 CVE CVSS 2.00.0"));

    SelenideElement kevContent = vulnerabilityDetailsPopover.getSectionContentByIdx(3);
    kevContent.shouldHave(text("Not listed"));

    SelenideElement epssContent = vulnerabilityDetailsPopover.getSectionContentByIdx(4);
    epssContent.shouldHave(text("0.077%"));

    SelenideElement weaknessContent = vulnerabilityDetailsPopover.getSectionContentByIdx(5);
    weaknessContent.shouldHave(text("CWE123 (Custom)"));
    weaknessContent.shouldHave(text("Sonatype CWE400"));

    SelenideElement sourceContent = vulnerabilityDetailsPopover.getSectionContentByIdx(6);
    sourceContent.shouldHave(text("Sonatype Data Research"));

    SelenideElement categoriesContent = vulnerabilityDetailsPopover.getSectionContentByIdx(7);
    categoriesContent.shouldHave(text("Data"));

    SelenideElement cvssDetailsContent = vulnerabilityDetailsPopover.getSectionContentByLabel("CVSS Details");
    cvssDetailsContent.shouldHave(text("Severity8.0 (Custom)"));
    cvssDetailsContent.shouldHave(text("Vector Stringtest/vector (Custom)"));

    // Validate the "Vulnerability Research Metadata" section
    SelenideElement researchMetadataAccordion = $("#vulnerability-research-metadata-accordion");
    researchMetadataAccordion.shouldBe(visible);
    researchMetadataAccordion.click();

    SelenideElement detectionType = researchMetadataAccordion.$("span[aria-label^='Detection Type:']");
    detectionType.shouldHave(text("Primary"));

    SelenideElement detectionSource = researchMetadataAccordion.$("span[aria-label^='Detection Source:']");
    detectionSource.shouldHave(text("Sonatype Identified"));

    VulnerabilityOverrideForm vulnerabilityOverrideForm = vulnerabilityDetailsPopover.getVulnerabilityOverrideForm();
    vulnerabilityOverrideForm.shouldBe(visible);
    vulnerabilityOverrideForm.comment().shouldNotBe(visible);
    vulnerabilityOverrideForm.submitButton().shouldBe(CLM.DISABLED);

    vulnerabilityOverrideForm.status().chooseOption(new Option(3, "CONFIRMED"));
    vulnerabilityOverrideForm.comment().shouldBe(visible);
    vulnerabilityOverrideForm.comment().shouldBe(enabled);
    String overridenVulnerabilityComment = "vulnerability confirmed in the current code";
    vulnerabilityOverrideForm.comment().setValue(overridenVulnerabilityComment);
    vulnerabilityOverrideForm.submitButton().shouldBe(enabled).click();
    // Conditions after submitting
    vulnerabilityOverrideForm.submitButton().shouldBe(CLM.DISABLED);
    vulnerabilityOverrideForm.comment().shouldBe(enabled);

    eyesWatcher.eyesCheck("vulnerability details popover");

    SelenideElement closeButton = vulnerabilityDetailsPopover.getCloseButton();

    closeButton.click();

    vulnerabilityDetailsPopover.shouldNotBe(visible);

    vulnerabilitiesTable.getRows().first().findAll(By.tagName("td"))
        .shouldHave(exactTexts("9", "CVE-1234-56789", "Sonatype Enhanced", "Confirmed", ""));

    firstRow.click();
    vulnerabilityOverrideForm.status().getElement().shouldHave(text("CONFIRMED"));
    vulnerabilityOverrideForm.comment().shouldHave(text(overridenVulnerabilityComment));
  }

  @Test
  public void testSecurityTab_vulnerabilityDetailsPopover_customizeButton() {
    String testRefId = "CVE-1234-56789";
    mockHdsResponsesForVulnerabilityDetails();
    tempEntity.newVulnerabilityCustomData(app.getId(), testRefId, null, "Test remediation",
        "123", "test/vector",
        8.0F);

    refreshOrOpen(ComponentDetailsPage.urlToSecurity(app, SCAN_ID, "1e48256a2341047e7d72"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    VulnerabilitiesTable vulnerabilitiesTable = componentDetailsPage.securityTabContent().vulnerabilitiesTable();
    vulnerabilitiesTable.shouldBe(visible);

    SelenideElement firstRow = vulnerabilitiesTable.getRows().first();
    firstRow.click();

    VulnerabilityDetailsPopover vulnerabilityDetailsPopover = new VulnerabilityDetailsPopover();
    vulnerabilityDetailsPopover.shouldBe(visible);

    SelenideElement customizeButton = vulnerabilityDetailsPopover.getCustomizeButton();
    customizeButton.shouldBe(visible);
    customizeButton.click();

    CustomizeVulnerabilityDetailsPage.refIdTitle().shouldBe(visible);
    CustomizeVulnerabilityDetailsPage.refIdTitle().shouldBe(text(testRefId));

    NxBackButton backButton = CustomizeVulnerabilityDetailsPage.backButton();
    backButton.shouldBe(visible);
    backButton.shouldHave(text("Back to Violation Details"));
    backButton.click();

    waitUntilUrl(ComponentDetailsPage.urlToSecurity(app, SCAN_ID, "1e48256a2341047e7d72"));
  }

  @Test
  public void testLegalTab_licenseViolationTableEntries() {
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "fa78f54738ccf77379d1"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.legalTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(1));

    ElementsCollection rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHave(size(6));

    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')",
        "Open", ""));

    eyesWatcher.eyesCheck("component details legal tab violation table no waiver");

    addWaiver(policyViolationsTable);

    componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTab().shouldBe(visible).click();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    policyViolationsTable = componentDetailsPage.legalTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(1));
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHave(size(6));

    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')",
        "Unapplied Waiver", ""));

    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    reportPage.reevaluateButton().click();
    reportPage.fullReevaluateButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    waitUntilUrl(ApplicationReportPage.url(app, SCAN_ID));
    ApplicationReportPage.AppReportHeaders reportHeaders = new ApplicationReportPage.AppReportHeaders();
    reportHeaders.componentNameFilterInput().setValue("com.mycila : license-maven-plugin : 2.11");
    reportPage.resultRows().first().click();

    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, "fa78f54738ccf77379d1"));

    componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTab().shouldBe(visible).click();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    policyViolationsTable = componentDetailsPage.legalTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    policyViolationsTable.getRows().shouldHave(size(1));
    rowCells = policyViolationsTable.getRows().first().findAll(By.tagName("td"));
    rowCells.shouldHave(size(6));
    rowCells.shouldHave(exactTexts("10", "License-Banned", "License not approved in any situation",
        "Found licenses in the 'Banned' license threat group ('AGPL-3.0')",
        "Waived", ""));
  }

  @Test
  public void testLegalTab_licenseDetectionTile() {
    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "fa78f54738ccf77379d1"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    ElementsCollection declaredLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.declaredLicenses());
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHave(size(1));
    observedLicenses.first().shouldHave(text("Not Provided"));

    licenseDetectionsTile.status().shouldHave(text("Status: Open"));
  }

  @Test
  public void testLegalTab_licenseDetectionTileClickReviewObligationsButton() {
    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "fa78f54738ccf77379d1"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    componentDetailsPage.legalTab().click();
    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    licenseDetectionsTile.reviewObligationsButton().shouldBe(visible);
    licenseDetectionsTile.reviewObligationsButton().shouldHave(text("Review Obligations"));
    navigateToLegalObligationsPage(licenseDetectionsTile);
  }

  @Test
  public void testLegalTab_licenseDetectionTileClickReviewObligationsButtonAndBackToCDP() {
    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "fa78f54738ccf77379d1"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);
    componentDetailsPage.legalTab().click();
    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.reviewObligationsButton().shouldBe(visible);
    navigateToLegalObligationsPage(licenseDetectionsTile);

    ComponentLegalOverviewPage.backLink().click();
    waitUntilUrl(ComponentDetailsPage.urlToLegal(app, SCAN_ID, HASH));
    licenseDetectionsTile.reviewObligationsButton().shouldBe(visible);
  }

  @Test
  public void testLegalTab_licenseDetectionTileAlpDisabled() {
    setMissingFeature(LicensedFeature.ADVANCED_LEGAL_PACK);
    // we need to refresh the browser to load the product features again
    WebDriverRunner.getWebDriver().navigate().refresh();
    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "fa78f54738ccf77379d1"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    componentDetailsPage.legalTab().click();
    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);

    licenseDetectionsTile.reviewObligationsButton().shouldNotBe(visible);
    licenseDetectionsTile.editLicenseButton().shouldBe(visible);
    licenseDetectionsTile.status().shouldHave(text("Status: Open"));
  }

  @Test
  public void testLegalTab_LicenseDetectionTileAlpObservedLicensesDisabled() {
    configurationService.setALPObservedLicenseDetectionEnabled(false);

    try {
      refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "ci6x9fypjoym3kwtai3m"));

      ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
      componentDetailsPage.legalTabContent().shouldBe(visible);

      LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();

      licenseDetectionsTile.shouldBe(visible).observedLicenses()
          .shouldHave(text("Enable the Observed License Detection feature in the Advanced Legal Pack (ALP) add-on."));

      licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses()).isEmpty();
    }
    finally {
      configurationService.setALPObservedLicenseDetectionEnabled(true);
    }
  }

  @Test
  public void testLegalTab_LicenseDetectionTileAlpObservedLicensesWhenAlpDisabled() {
    setMissingFeature(LicensedFeature.ADVANCED_LEGAL_PACK);
    // we need to refresh the browser to load the product features again
    WebDriverRunner.getWebDriver().navigate().refresh();

    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "ci6x9fypjoym3kwtai3m"));

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();

    licenseDetectionsTile
        .shouldBe(visible)
        .observedLicenses()
        .shouldHave(text("Get Advanced Legal Pack (ALP) to view Observed Licenses."));

    licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses()).isEmpty();
  }

  @Test
  public void testLegalTab_LicensesPopoverAlpObservedLicensesDisabled() {
    configurationService.setALPObservedLicenseDetectionEnabled(false);
    try {
      refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "ci6x9fypjoym3kwtai3m"));

      ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
      componentDetailsPage.legalTabContent().shouldBe(visible);

      LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
      licenseDetectionsTile.editLicenseButton().click();

      EditLicensesPopover editPopover = new EditLicensesPopover();
      editPopover.observedLicenses()
          .shouldHave(text("Enable the Observed License Detection feature in the Advanced Legal Pack (ALP) add-on."));
    }
    finally {
      configurationService.setALPObservedLicenseDetectionEnabled(true);
    }
  }

  @Test
  public void testLegalTab_LicensesPopoverAlpObservedLicensesWhenAlpDisabled() {
    setMissingFeature(LicensedFeature.ADVANCED_LEGAL_PACK);
    // we need to refresh the browser to load the product features again
    WebDriverRunner.getWebDriver().navigate().refresh();

    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, "ci6x9fypjoym3kwtai3m"));

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.editLicenseButton().click();

    EditLicensesPopover editPopover = new EditLicensesPopover();
    editPopover.observedLicenses().shouldHave(text("Get Advanced Legal Pack (ALP) to view Observed Licenses."));
  }

  /* Part of testPolicyViolationsTab_violationTableEntries. */
  private void testLegacyViolationIndicator(final ComponentDetailsPage componentDetailsPage) {
    // Configure legacy violation indicator for the first violation in the report and reload it
    componentDetailsPage.backButton().click();
    activateLegacyViolation();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));

    reportPage.aggregateByComponentToggle().click();
    SelenideElement firstLegacyViolation = reportPage.resultRows().first();
    firstLegacyViolation.click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID, HASH));

    navigateToComponentDetailsPageViolationsTab(componentDetailsPage);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.getRows().shouldHave(size(1));
    SelenideElement indicatorsCell = policyViolationsTable.getRows().first().findAll(By.tagName("td")).get(5);
    indicatorsCell.shouldHave(text("Legacy"));
  }

  private void activateLegacyViolation() {
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    Policy licenseBannedPolicy = policyDAO.getByName("License-Banned").get(0);

    app.setLegacyViolationEnabled(true);
    licenseBannedPolicy.setLegacyViolationAllowed(true);
    applicationDAO.update(app);
    policyDAO.update(licenseBannedPolicy);
    LegacyViolationService legacyViolationService =
        testCLMServer.getCLMServer().getInstance(LegacyViolationService.class);
    legacyViolationService.grantLegacyViolationStatus(app.getPublicId());
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
    tempEntity.newLabel("ROOT_ORGANIZATION_ID", "root org level label", Color.light_red);
    tempEntity.newLabel(org.getId(), "org level label", Color.dark_blue);
    tempEntity.newLabel(parentOrg.getId(), "parent org level label", Color.light_green);
    tempEntity.newComponentLabel(app.getId(), appLevelLabel.getId(), "fa78f54738ccf77379d1");

    // Go to details page, verify manage labels content appears
    refreshOrOpen(ComponentDetailsPage.urlToLabels(app, SCAN_ID, "fa78f54738ccf77379d1"));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    ManageLabelsContentTab manageLabels = componentDetailsPage.labelsContent();
    manageLabels.shouldBe(visible);
    manageLabels.appliedLabels().shouldHave(size(1));
    manageLabels.applicableLabels().shouldHave(size(6));

    // Remove applied app level label
    manageLabels.appliedLabels().get(0).should(exist).click();
    manageLabels.removeLabelModal().should(exist);
    // Confirm removal and verify labels count
    manageLabels.removeLabelModal().confirmRemoveButton().should(exist).click();
    manageLabels.appliedLabels().shouldHave(size(0));
    manageLabels.applicableLabels().shouldHave(size(7));

    // Adding first label
    manageLabels.applicableLabelText(0).shouldHave(text("Architecture-Blacklisted"));
    manageLabels.applicableLabels().get(0).should(exist).click();
    manageLabels.addLabelModal().should(exist);
    // Screenshot the add label modal
    eyesWatcher.eyesCheck("Add Label Modal");
    // Add and confirm
    manageLabels.addLabelModal().labelsScope(0).should(exist);
    manageLabels.addLabelModal().labelsScopesDropdown()
        .chooseOptionWithHidden(new Option(0, "Organization - Test Organization"));
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
    manageLabels.appliedLabels().shouldHave(size(2));
    eyesWatcher.eyesCheck("Labels Tab");

    // Checking n-level inheritance
    manageLabels.applicableLabelText(4).shouldHave(text("root org level label"));
    manageLabels.applicableLabels().get(4).should(exist).click();
    manageLabels.addLabelModal().should(exist);
    // Add and confirm
    manageLabels.addLabelModal().labelsScope(0).should(exist);
    manageLabels.addLabelModal().labelsScopesDropdown().listItems().shouldHave(size(5));
    manageLabels.addLabelModal().labelsScope(1).shouldHave(text("Organization - Root Organization"));
    manageLabels.addLabelModal().labelsScope(2).shouldHave(text("Organization - Parent Organization"));
    manageLabels.addLabelModal().labelsScope(3).shouldHave(text("Organization - Test Organization"));
    manageLabels.addLabelModal().labelsScope(4).shouldHave(text("Application - ApplicationReportTest"));
    manageLabels.addLabelModal().labelsScopesDropdown()
        .chooseOptionWithHidden(new Option(1, "Organization - Parent Organization"));
    manageLabels.addLabelModal().submitButton().shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    manageLabels.appliedLabelText(2).shouldHave(text("root org level label"));
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
    testCLMServer.getHdsServer()
        .respondWith(Collections.emptyMap())
        .atUri("rest/vulnerability/details/json");
  }

  private void mockHdsResponsesForVulnerabilityDetails() {
    mockHdsResponseForFirstComponent();
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails_CVE-1234-56789.json"))
        .atUri("rest/vulnerability/details/json/CVE-1234-56789");
  }

  private void mockHdsResponseForVulnerabilityDetailsWithRefId(String refId) {
    URI uri = UriBuilder.fromPath("rest/vulnerability/details/json/{arg1}").build(refId);

    SecurityVulnerabilityData securityVulnerabilityData = new SecurityVulnerabilityData(refId);
    securityVulnerabilityData.isAdvancedVulnerabilityDetection = true;
    securityVulnerabilityData.researchType = SecurityVulnerabilityResearchType.DEEP_DIVE;
    securityVulnerabilityData.mainSeverity =
        new SecurityVulnerabilitySeverity("source-test", "source-test-label", 7.0f, "test/vector");
    securityVulnerabilityData.weakness = new SecurityVulnerabilityWeakness();
    securityVulnerabilityData.weakness.cweIds = new ArrayList<>();
    securityVulnerabilityData.weakness.cweIds.add(new CweId("123", URI.create("http://localhost")));
    securityVulnerabilityData.kevData = new KevData(false);

    testCLMServer.getHdsServer().respondWith(securityVulnerabilityData).atUri(uri);
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

  private void navigateToLegalObligationsPage(final LicenseDetectionsTile licenseDetectionsTile) {
    licenseDetectionsTile.reviewObligationsButton().click();
    String componentIdentifier = "%7B\"format\":\"maven\",\"coordinates\":%7B\"artifactId\":\"license-maven-plugin\","
        + "\"classifier\":\"\",\"extension\":\"jar\",\"groupId\":\"com.mycila\",\"version\":\"2.11\"%7D%7D";
    waitUntilUrl(LegalApplicationDetailsPage.urlToComponentAtApplicationScopeByComponentIdentifier(
        componentIdentifier, app.getPublicId(), HASH, SCAN_ID, "legal"));
    ComponentLegalOverviewPage.editLicenseFilesButton().shouldBe(visible);
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
    declaredLicenses.shouldHave(size(1));
    declaredLicenses.first().shouldHave(text("Not Provided (Claimed Component)"));

    ElementsCollection effectiveLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.effectiveLicenses());
    effectiveLicenses.shouldHave(size(1));
    effectiveLicenses.first().shouldHave(text("Not Provided"));

    ElementsCollection observedLicenses = licenseDetectionsTile.getItems(licenseDetectionsTile.observedLicenses());
    observedLicenses.shouldHave(size(1));
    observedLicenses.first().shouldHave(text("Not Provided (Claimed Component)"));
  }

  /**
   * This method is a convenience method to click on a policy violation row, click on manage waivers, go to the list
   * waivers page, click on add waiver, submit and return to the policy violation table page.
   *
   * @param policyViolationsTable instance of the PolicyViolationsTable whose row needs to be clicked
   */
  private void addWaiver(PolicyViolationsTable policyViolationsTable) {
    WaiverApplierForReport.waiveViolationFromTable(policyViolationsTable, 1);
  }

  private static void waitUntilSpinnersGone() {
    final var pageLoadSpinner = $(".nx-loading-spinner");
    pageLoadSpinner.shouldNotBe(visible, Duration.ofSeconds(10));
  }

  private void waitUntilVulnerabilityDetailsPopoverIsVisible() {
    final var popover = $(VulnerabilityDetailsPopover.POPOVER_SELECTOR);
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.visibilityOf(popover));
  }

  private Wait<WebDriver> getWebDriverAwait() {
    return new FluentWait<>(getWebDriver()).withTimeout(Duration.ofSeconds(10)).pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class);
  }
}
