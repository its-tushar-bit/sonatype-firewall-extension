/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.componentdetails.ClaimTabContent;
import com.sonatype.clm.testing.functional.elements.componentdetails.ComponentCoordinatesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.ComponentInformationTile.IdentificationDefinitionList;
import com.sonatype.clm.testing.functional.elements.componentdetails.EditLicensesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.LicenseDetectionsTile;
import com.sonatype.clm.testing.functional.elements.componentdetails.OccurrencesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationDetailPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.elements.componentdetails.VulnerabilitiesTable;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.AuditLogContent;
import com.sonatype.clm.testing.functional.pages.ContainerComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.utils.SimilarWaiverCreator;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.api.v2.dto.ApiVerifyOrCreateApplicationForContainerImageFirewallDTO;
import com.sonatype.insight.brain.integration.ApplicationForContainerImageFirewallService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.mock.hds.HdsMockServer;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallContainerComponentDetailsPageTest
    extends AbstractFunctionalTest
{
  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String HASH = "dc810b3d25f9e8c930f5";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private Application otherApp;

  private TestReportEvaluator evaluator;

  private Configuration configurationService;

  @Before
  public void start() throws IOException {
    setFeatures(
            LicensedFeature.CONTAINER_IMAGES_EVALUATION,
            LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
            LicensedFeature.APPLICATION_EVALUATION,
            LicensedFeature.APPLICATION_REPORTS,
            LicensedFeature.SUCCESS_METRICS,
            LicensedFeature.COMPONENT_LABELS,
            LicensedFeature.POLICY_MANAGEMENT,
            LicensedFeature.POLICY_WAIVERS,
            LicensedFeature.POLICY_VIOLATIONS,
            LicensedFeature.COMPONENT_EVALUATION
    );
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    hardreset();

    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();

    configurationService = lookup(Configuration.class);

    assertThat(configurationService.isALPObservedLicenseDetectionEnabled()).isTrue();

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

    RepositoryManager repositoryManager = tempEntity.newRepositoryManagerWithBaseUrl("base-url");
    Repository repository =
        tempEntity.newRepository(repositoryManager, "proxy-docker-repository", RepositoryType.proxy, "docker");
    Organization organizationForRepository =
        tempEntity.createOrganizationHierarchyForContainers(repositoryManager, repository);

    app = tempEntity.newApplication("ContainerReportTest", "ContainerReportTest", organizationForRepository.getId());
    otherApp = tempEntity.newApplication("OtherApplicationReportTest", "OtherApplicationReportTest",
        organizationForRepository.getId());
    tempEntity.createPolicyEvaluationForContainerEvaluation(repository);

    ApplicationForContainerImageFirewallService containerService =
        lookup(ApplicationForContainerImageFirewallService.class);
    ApiVerifyOrCreateApplicationForContainerImageFirewallDTO apiDTO =
        new ApiVerifyOrCreateApplicationForContainerImageFirewallDTO(repositoryManager.getInstanceId(),
            repository.getPublicId(),
            repositoryManager.getBaseUrl(),
            "containerImageNamespace",
            "containerImageName",
            "containerImageVersion");
    containerService.verifyOrCreateApplicationForContainerImage(repository, apiDTO);

    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);

    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work, Stage.ID_PROXY);
    evaluator.evaluatePolicyForScanIdWithScanTriggerType(ScanTriggerType.CLI);

    new SimilarWaiverCreator(zippedReport, otherApp, testCLMServer,
        AbstractFunctionalTest::refreshOrOpen, baseUrlFromTest);
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
  }

  @Test
  public void testComponentDetailsHeaderAndFooter() {
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));

    ElementsCollection violations = reportPage.resultRows();
    SelenideElement directDependencyWithViolation = violations.get(2);
    directDependencyWithViolation.click();

    final String directDependencyHash = "f0776db1593e215146d2";
    waitUntilUrl(ContainerComponentDetailsPage.url(app, SCAN_ID, directDependencyHash));
    ContainerComponentDetailsPage componentDetailsPage = new ContainerComponentDetailsPage();

    SelenideElement title = componentDetailsPage.header().title();
    title.shouldHave(text("apache-httpclient : commons-httpclient : 3.1"));

    // Not comparing exact texts due to dynamic information (organization uuid, report date)
    ElementsCollection reportInformationElements = componentDetailsPage.header().reportInformationElements();
    reportInformationElements.shouldHave(texts("repository-organization", "ContainerReportTest", "Proxy Report "));

    ElementsCollection tags = componentDetailsPage.header().tags();
    tags.shouldHave(texts("maven", "Direct Dependency"));

    componentDetailsPage.footer().paginationCounter().shouldHave(text("3 of 65"));
  }

  @Test
  public void testComponentDetailsRemediationDefaultTab() {
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    reportPage.reportTitle().shouldHave(text("ContainerReportTest Proxy Report"));

    ElementsCollection violations = reportPage.resultRows();
    SelenideElement violation = violations.get(0);
    violation.click();

    waitUntilUrl(ContainerComponentDetailsPage.urlToOverview(app, SCAN_ID, HASH));
  }

  @Test
  public void testComponentDetailsTabNavigation() {
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    reportPage.reportTitle().shouldHave(text("ContainerReportTest Proxy Report"));

    ContainerComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    componentDetailsPage.violationsTab().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToViolations(app, SCAN_ID, HASH));

    componentDetailsPage.securityTab().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToSecurity(app, SCAN_ID, HASH));

    componentDetailsPage.legalTab().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToLegal(app, SCAN_ID, HASH));

    componentDetailsPage.auditTab().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToAudit(app, SCAN_ID, HASH));

    componentDetailsPage.overviewTab().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToOverview(app, SCAN_ID, HASH));

    componentDetailsPage.labelsTab().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToLabels(app, SCAN_ID, HASH));
  }

  @Test
  public void testComponentDetails_ClaimTab() {
    refreshOrOpen(ContainerComponentDetailsPage.urlToClaim(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
    ContainerComponentDetailsPage componentDetailsPage = new ContainerComponentDetailsPage();

    componentDetailsPage.claimTabContent().shouldBe(visible);

    componentDetailsPage.violationsTab().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToViolations(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));

    componentDetailsPage.overviewTab().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToOverview(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
  }

  @Test
  public void testComponentDetails_ClaimTabContent() {
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    mockHdsResponseForClaimedComponent();
    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    refreshOrOpen(ContainerComponentDetailsPage.urlToClaim(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
    ContainerComponentDetailsPage componentDetailsPage = new ContainerComponentDetailsPage();

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
    waitUntilUrl(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    reportPage.reevaluateButton().click();
    reportPage.fullReevaluateButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    waitUntilUrl(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));

    reportPage.headers().componentNameFilterInput().setValue("claimed");
    reportPage.resultRow(1).shouldHave(text("claimed : claimed : claimed : claimed : claimed")).click();

    componentDetailsPage.claimTabForClaimedComponent().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToClaim(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
    claimTabContent.shouldBe(visible);

    checkLegalLicencesForClaimedComponent(componentDetailsPage);

    componentDetailsPage.claimTabForClaimedComponent().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToClaim(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));
    claimTabContent.shouldBe(visible);

    checkFieldsValue(claimTabContent);
    claimTabContent.revoke().shouldBe(enabled).click();

    NxDeleteModal deleteModal = claimTabContent.getDeleteModal();
    deleteModal.submitButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteModal.alertContent().shouldBe(hidden);

    // Reevaluate to revoke the claim
    MainHeader.backButton().click();
    waitUntilUrl(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    reportPage.reevaluateButton().click();
    reportPage.fullReevaluateButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    waitUntilUrl(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    reportPage.headers().componentNameFilterInput().setValue("claimed");
    reportPage.resultRows().shouldHave(size(1));
    reportPage.resultRow(1).shouldHave(text("No Results"));

    reportPage.headers().componentNameFilterInput().setValue("regexmatch.dll");
    reportPage.resultRow(1).shouldHave(text("regexmatch.dll"));
  }

  @Test
  public void testOverviewTab_componentInformationTile() {
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    ContainerComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();
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
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    ContainerComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();
    componentDetailsPage.overviewTab().shouldBe(visible);
    componentDetailsPage.overviewTabContent().shouldBe(visible);

    componentDetailsPage.overviewTabContent().componentInformationTile().componentCoordinatesButton().click();
    ComponentCoordinatesPopover componentCoordinatesPopover = new ComponentCoordinatesPopover();

    componentCoordinatesPopover.shouldBe(visible);
    componentCoordinatesPopover.title().shouldHave(text("Component Coordinates"));
    componentCoordinatesPopover.typeDefinition().shouldHave(text("Type a-name"));
    componentCoordinatesPopover.namingDefinitions()
        .shouldHave(exactTexts("Name angular", "Version 1.2.17"));

    componentCoordinatesPopover.copyToClipboard().shouldHave(text("Copy to Clipboard\nPackage URL"));

    eyesWatcher.eyesCheck("component details component coordinates popover");

    componentCoordinatesPopover.closeButton().click();
    componentCoordinatesPopover.shouldNotBe(visible);
  }

  @Test
  public void testOverviewTab_OccurrencesPopover() {
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    ContainerComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();
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
  public void testPolicyViolationsTab_switchingFromSecurityToNonSecurityViolationDetails() {
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement fifthViolation = violations.get(4);
    fifthViolation.click();
    waitUntilSpinnersGone();

    ContainerComponentDetailsPage componentDetailsPage = new ContainerComponentDetailsPage();

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

    ElementsCollection lastRow = policyViolationsTable.getCellsByNthRow(1);
    lastRow.get(1).shouldBe(visible).shouldHave(text("Security-High")).click();
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
  public void testSecurityTab_vulnerabilityTableEntries() {
    mockHdsResponseForFirstComponent();

    refreshOrOpen(ContainerComponentDetailsPage.urlToSecurity(app, SCAN_ID, "1e48256a2341047e7d72"));
    ContainerComponentDetailsPage componentDetailsPage = new ContainerComponentDetailsPage();
    componentDetailsPage.securityTabContent().shouldBe(visible);

    VulnerabilitiesTable vulnerabilitiesTable = componentDetailsPage.securityTabContent().vulnerabilitiesTable();
    vulnerabilitiesTable.shouldBe(visible);

    vulnerabilitiesTable.getHeaderRow().findAll(By.tagName("th"))
        .shouldHave(exactTexts("CVSS", "ISSUES", "DATA ENRICHMENT", "STATUS", ""));

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
  public void testLegalTab_licenseDetectionTile() {
    refreshOrOpen(ContainerComponentDetailsPage.urlToLegal(app, SCAN_ID, "fa78f54738ccf77379d1"));
    ContainerComponentDetailsPage componentDetailsPage = new ContainerComponentDetailsPage();
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
  public void testLegalTab_licenseDetectionTileAlpDisabled() {
    setMissingFeature(LicensedFeature.ADVANCED_LEGAL_PACK);
    // we need to refresh the browser to load the product features again
    WebDriverRunner.getWebDriver().navigate().refresh();
    refreshOrOpen(ContainerComponentDetailsPage.urlToLegal(app, SCAN_ID, HASH));
    ContainerComponentDetailsPage componentDetailsPage = new ContainerComponentDetailsPage();
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
      refreshOrOpen(ContainerComponentDetailsPage.urlToLegal(app, SCAN_ID, "ci6x9fypjoym3kwtai3m"));

      ContainerComponentDetailsPage componentDetailsPage = new ContainerComponentDetailsPage();
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

    refreshOrOpen(ContainerComponentDetailsPage.urlToLegal(app, SCAN_ID, "ci6x9fypjoym3kwtai3m"));

    ContainerComponentDetailsPage componentDetailsPage = new ContainerComponentDetailsPage();
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
      refreshOrOpen(ContainerComponentDetailsPage.urlToLegal(app, SCAN_ID, "ci6x9fypjoym3kwtai3m"));

      ContainerComponentDetailsPage componentDetailsPage = new ContainerComponentDetailsPage();
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

    refreshOrOpen(ContainerComponentDetailsPage.urlToLegal(app, SCAN_ID, "ci6x9fypjoym3kwtai3m"));

    ContainerComponentDetailsPage componentDetailsPage = new ContainerComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.editLicenseButton().click();

    EditLicensesPopover editPopover = new EditLicensesPopover();
    editPopover.observedLicenses().shouldHave(text("Get Advanced Legal Pack (ALP) to view Observed Licenses."));
  }

  @Test
  public void testAuditLogTab_emptyMessage() {
    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));

    ContainerComponentDetailsPage componentDetailsPage = openComponentDetailsPageForFirstViolation();

    componentDetailsPage.auditTab().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToAudit(app, SCAN_ID, HASH));

    AuditLogContent auditLog = componentDetailsPage.auditLogContent();
    auditLog.emptyMessage().shouldHave(text("No changes were found for this component"));
  }

  private void mockHdsResponseForFirstComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-29.50.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
  }

  private ContainerComponentDetailsPage openComponentDetailsPageForFirstViolation() {
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement violation = violations.get(0);
    violation.click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToOverview(app, SCAN_ID, HASH));
    return new ContainerComponentDetailsPage();
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

  private void checkLegalLicencesForClaimedComponent(ContainerComponentDetailsPage componentDetailsPage) {
    componentDetailsPage.legalTab().click();
    waitUntilUrl(ContainerComponentDetailsPage.urlToLegal(app, SCAN_ID, "6d0684d8acf85cd6e7f2"));

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

  private static void waitUntilSpinnersGone() {
    final var pageLoadSpinner = $(".nx-loading-spinner");
    pageLoadSpinner.shouldNotBe(visible, Duration.ofSeconds(10));
  }
}
