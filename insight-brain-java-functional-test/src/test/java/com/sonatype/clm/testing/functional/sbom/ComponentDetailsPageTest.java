/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.ComponentDetailsSummaryTile;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.CopyAnnotationModal;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.DeleteAnnotationModal;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.PolicyViolationDetailsDrawer;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.PolicyViolationsTile;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.VexAnnotationDrawer;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.VulnerabilitiesTableTile;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.VulnerabilityDetailsPopover;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.SbomManagerComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.sbom.BillOfMaterialsPageSummaryTile;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerBillOfMaterialsPage;
import com.sonatype.insight.brain.api.v2.service.ApiSbomService;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.components.SbomComponentsService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.util.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.partialText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;

public class ComponentDetailsPageTest
    extends AbstractFunctionalTest
{
  public static final String TEST_SBOM_VERSION_ID = "mockVersionId";

  public static final String TEST_COMPONENT_HASH = "mockComponentHash";

  public static final String TEST_COMPONENT_REF = "96c3fa923cd66782eb2a2747a3453200a2d78fad";

  public static final String TEST_COMPONENT_PURL = "pkg:maven/2/3@1.1";

  private final BillOfMaterialsPageSummaryTile billOfMaterialsPageSummaryTile = new BillOfMaterialsPageSummaryTile();

  private static SbomManagerComponentDetailsPage sbomManagerComponentDetailsPage;

  private Application testApplication;

  private Organization testOrganization;

  private ThirdPartySbomMetadata thirdPartySbomMetadata;

  private ThirdPartyFileCoordinate thirdPartyFileCoordinate;

  ApiSbomService apiSbomService;

  @BeforeClass
  public static void beforeClass() {
    sbomManagerComponentDetailsPage = new SbomManagerComponentDetailsPage(
        new ComponentDetailsSummaryTile(),
        new VulnerabilitiesTableTile("disclosedVulnerabilities"),
        new VulnerabilitiesTableTile("sonatypeIdentifiedVulnerabilities"),
        new VulnerabilityDetailsPopover(),
        new VexAnnotationDrawer(),
        new DeleteAnnotationModal(),
        new CopyAnnotationModal(),
        new PolicyViolationsTile(),
        new PolicyViolationDetailsDrawer());

    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void beforeEachMethod() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.APPLICATION_EVALUATION, LicensedFeature.SUCCESS_METRICS);

    // Force full page reload so the frontend's cached product license (stored in a JS module-level variable) is
    // cleared and re-fetched with the updated SBOM Manager license. Without this, hash-only SPA navigations reuse
    // the stale cached license from the initial page load (which had default Lifecycle products).
    Selenide.refresh();

    apiSbomService = lookup(ApiSbomService.class);
  }

  @Test
  public void testFeatureEnabled_showContent() {
    setTestData();

    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(true);
    refreshOrOpen(SbomManagerComponentDetailsPage.url(testApplication.getPublicId(), thirdPartySbomMetadata
        .getSbomVersion(), thirdPartyFileCoordinate.getHash()));

    // Header
    sbomManagerComponentDetailsPage.pageTitle().shouldBe(visible);
    sbomManagerComponentDetailsPage.pageTitle().shouldHave(text("2 : 3 : 1.1"));

    sbomManagerComponentDetailsPage.reportInfoItems().shouldHave(size(3));
    sbomManagerComponentDetailsPage.reportInfoItems().get(0).shouldHave(text(testOrganization.getName()));
    sbomManagerComponentDetailsPage.reportInfoItems().get(1).shouldHave(text(testApplication.getName()));
    sbomManagerComponentDetailsPage.reportInfoItems().get(2).shouldHave(text("BOM"));

    // Tags
    sbomManagerComponentDetailsPage.tags().shouldHave(size(2));
    sbomManagerComponentDetailsPage.tags().get(0).shouldHave(text("Maven"));
    sbomManagerComponentDetailsPage.tags().get(1).shouldHave(text(TEST_COMPONENT_PURL));

    // Tabs
    sbomManagerComponentDetailsPage.tabs().get(0).shouldHave(text("Vulnerability"));
    sbomManagerComponentDetailsPage.tabs().get(1).shouldHave(text("Policy Violations"));

    // Component Summary
    sbomManagerComponentDetailsPage.componentSummary().header().shouldHave(text("Component Summary"));
    sbomManagerComponentDetailsPage.componentSummary().highestScoreLabel().shouldHave(text("Highest CVSS Score"));
    sbomManagerComponentDetailsPage.componentSummary()
        .vulnerabilitiesVerifiedLabel()
        .shouldHave(text("Vulnerabilities Verified"));
    sbomManagerComponentDetailsPage.componentSummary().policyViolationsLabel().shouldHave(text("Policy Violations"));
    sbomManagerComponentDetailsPage.componentSummary().highestScoreValue().shouldHave(text("9.6"));
    sbomManagerComponentDetailsPage.componentSummary().sonatypeVerified().shouldHave(text("0 Sonatype Verified"));
    sbomManagerComponentDetailsPage.componentSummary().unVerified().shouldHave(text("3 Unverified"));
    sbomManagerComponentDetailsPage.componentSummary().severePolicyViolation().shouldHave(text("0"));
    sbomManagerComponentDetailsPage.componentSummary().criticalPolicyViolation().shouldHave(text("1"));
    // Disclosed vulnerabilities
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().header().shouldHave(text("Disclosed Vulnerabilities"));

    checkDisclosedVulnerabilitiesTableHeader();
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().tableRows().shouldHave(size(3));
    assertVulnerabilityTableRowContent(sbomManagerComponentDetailsPage.disclosedVulnerabilities(),
        "5.6", "ABC-123", "Unverified", " ", "Unannotated", " ");

    checkSonatypeVulnerabilitiesTableHeader(sbomManagerComponentDetailsPage.sonatypeVulnerabilitiesTile());
    assertVulnerabilityTableRowContent(sbomManagerComponentDetailsPage.sonatypeVulnerabilitiesTile(),
        "9.6", "sonatype-123", null, " ", "Unannotated", " ");

    eyesWatcher.eyesCheck("mockComponent");
  }

  @Test
  public void testFeatureEnabled_policyViolationsShouldBeHiddenWhenDisabled() {
    setTestData();

    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(false);
    refreshOrOpen(SbomManagerComponentDetailsPage.url(testApplication.getPublicId(), thirdPartySbomMetadata
        .getSbomVersion(), thirdPartyFileCoordinate.getHash()));

    sbomManagerComponentDetailsPage.tabs().shouldHave(size(2));
    sbomManagerComponentDetailsPage.tabs().get(0).shouldHave(text("Vulnerability"));
    sbomManagerComponentDetailsPage.tabs().get(1).shouldHave(text("Original BOM"));
  }

  @Test
  public void testFeatureEnabled_policyViolationsTile_fileCoordinateId() {
    // Use of fileCoordinateId is deprecated. But keeping this test to ensure backward compatibility
    setTestData(false);

    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(true);
    navigateToComponentDetailsPage();

    sbomManagerComponentDetailsPage.tabs().get(1).shouldHave(text("Policy Violations")).click();

    sbomManagerComponentDetailsPage.policyViolationsTile().shouldBe(visible);

    sbomManagerComponentDetailsPage.policyViolationsTile().header().shouldHave(text("Policy Violations"));
    sbomManagerComponentDetailsPage.policyViolationsTile().getColumnData(0, 0).shouldHave(text("9"));
    sbomManagerComponentDetailsPage.policyViolationsTile().getColumnData(0, 1).shouldHave(text("Security-High"));
    sbomManagerComponentDetailsPage.policyViolationsTile()
        .getColumnData(0, 2)
        .shouldHave(text("Medium risk CVSS score"));
    sbomManagerComponentDetailsPage.policyViolationsTile()
        .getColumnData(0, 3)
        .shouldHave(text(
            "Found security vulnerability CVE-4812 with severity 5.3.\n"
                + "Found security vulnerability CVE-4812 with severity 5.3.\n"
                + "Found security vulnerability CVE-4812 with status 'Open', not 'Not Applicable'."));
  }

  @Test
  public void testFeatureEnabled_policyViolationsTile_componentRef() {
    setTestData();

    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(true);
    navigateToComponentDetailsPage();

    sbomManagerComponentDetailsPage.tabs().get(1).shouldHave(text("Policy Violations")).click();

    sbomManagerComponentDetailsPage.policyViolationsTile().shouldBe(visible);

    sbomManagerComponentDetailsPage.policyViolationsTile().header().shouldHave(text("Policy Violations"));
    sbomManagerComponentDetailsPage.policyViolationsTile().getColumnData(0, 0).shouldHave(text("9"));
    sbomManagerComponentDetailsPage.policyViolationsTile().getColumnData(0, 1).shouldHave(text("Security-High"));
    sbomManagerComponentDetailsPage.policyViolationsTile()
        .getColumnData(0, 2)
        .shouldHave(text("Medium risk CVSS score"));
    sbomManagerComponentDetailsPage.policyViolationsTile()
        .getColumnData(0, 3)
        .shouldHave(text(
            "Found security vulnerability CVE-4812 with severity 5.3.\n"
                + "Found security vulnerability CVE-4812 with severity 5.3.\n"
                + "Found security vulnerability CVE-4812 with status 'Open', not 'Not Applicable'."));
  }

  @Test
  public void testFeatureEnabled_policyViolationDetailsDrawer_vulerabilityDetails() {
    setTestData();
    mockHdsResponseForVulnerabilityDetails();
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(true);

    navigateToComponentDetailsPage();

    sbomManagerComponentDetailsPage.tabs().get(1).shouldHave(text("Policy Violations")).click();

    sbomManagerComponentDetailsPage.policyViolationsTile().shouldBe(visible);

    sbomManagerComponentDetailsPage.policyViolationsTile().header().shouldHave(text("Policy Violations"));

    sbomManagerComponentDetailsPage.policyViolationsTile().getColumnData(0, 0).shouldHave(text("9")).click();

    sbomManagerComponentDetailsPage.policyViolationDetailsDrawer.shouldBe(visible);

    PolicyViolationDetailsDrawer.PolicyViolationConstraintInfo policyViolationConstraintInfo =
        PolicyViolationDetailsDrawer.policyViolationConstraintInfo();
    policyViolationConstraintInfo.title().shouldHave(text("Policy Constraint"));
    policyViolationConstraintInfo.reasons()
        .shouldHave(text(
            "Found security vulnerability CVE-4812 with severity 5.3.\n"
                + "Found security vulnerability CVE-4812 with severity 5.3.\n"
                + "Found security vulnerability CVE-4812 with status 'Open', not 'Not Applicable'."));

    PolicyViolationDetailsDrawer.VulnerabilityDetails vulnerabilityDetails =
        PolicyViolationDetailsDrawer.vulnerabilityDetails();

    assertVulnerabilityDetailsInsidePolicyViolationDetailsDrawer(vulnerabilityDetails);
  }

  @Test
  public void testFeatureEnabled_policyViolationDetailsDrawer_violationDetailsTile() {
    setTestData();
    mockHdsResponseForVulnerabilityDetails();
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(true);

    navigateToComponentDetailsPage();

    sbomManagerComponentDetailsPage.tabs().get(1).shouldHave(text("Policy Violations")).click();

    sbomManagerComponentDetailsPage.policyViolationsTile().header().shouldHave(text("Policy Violations"));

    sbomManagerComponentDetailsPage.policyViolationsTile().getColumnData(0, 0).shouldHave(text("9")).click();

    sbomManagerComponentDetailsPage.policyViolationDetailsDrawer.shouldBe(visible);

    PolicyViolationDetailsDrawer.SbomManagerViolationDetailsTile sbomManagerViolationDetailsTile =
        PolicyViolationDetailsDrawer.sbomManagerViolationDetailsTile();

    sbomManagerViolationDetailsTile.shouldBe(visible);

    sbomManagerViolationDetailsTile.threatLevelValue().shouldHave(text("9"));

    sbomManagerViolationDetailsTile.policyTypeValue().shouldHave(text("Security"));
  }

  @Test
  public void testFeatureEnabled_opensVulnerabilityDetailsPopover_issueLink_checkContent_ThirdParty() {
    setTestData();
    mockHdsResponseForVulnerabilityDetails();

    navigateToComponentDetailsPage();

    SelenideElement linkThirdRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(2, 1);
    linkThirdRowColumn.shouldBe(visible);
    linkThirdRowColumn.shouldHave(text("CVE-4812"));

    SelenideElement issueLink = linkThirdRowColumn.find("a");
    issueLink.shouldBe(visible);
    issueLink.click();

    VulnerabilityDetailsPopover vulnerabilityDetailsPopover =
        sbomManagerComponentDetailsPage.vulnerabilityDetailsPopover();
    vulnerabilityDetailsPopover.shouldBe(visible);

    assertVulnerabilityDetailsFromThirdParty(vulnerabilityDetailsPopover);
  }

  @Test
  public void testFeatureEnabled_opensVulnerabilityDetailsPopover_issueLink_checkContent_Sonatype() {
    testOrganization = tempEntity.newOrganization();
    testApplication = tempEntity.newApplication(testOrganization.getId());
    setVulnerabilityTablesData(minimumDataSet(true), true, "Sonatype", false, false);
    mockHdsResponseForVulnerabilityDetails();

    navigateToComponentDetailsPage();

    SelenideElement linkThirdRowColumn = sbomManagerComponentDetailsPage.sonatypeVulnerabilitiesTile()
        .getColumnData(4, 1);
    linkThirdRowColumn.shouldBe(visible);
    linkThirdRowColumn.shouldHave(text("CVE-4812"));

    SelenideElement issueLink = linkThirdRowColumn.find("a");
    issueLink.shouldBe(visible);
    issueLink.click();

    VulnerabilityDetailsPopover vulnerabilityDetailsPopover =
        sbomManagerComponentDetailsPage.vulnerabilityDetailsPopover();
    vulnerabilityDetailsPopover.shouldBe(visible);

    assertVulnerabilityDetails(vulnerabilityDetailsPopover);
  }

  @Test
  public void testFeatureEnabled_opensVexDrawer_addButton_checkContent() {
    setTestData();

    navigateToComponentDetailsPage();

    SelenideElement dropdownButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(0, 6);
    dropdownButtonFirstRowColumn.shouldBe(visible);

    ElementsCollection rowButtons = dropdownButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement addAnnotationButton = rowButtons.get(1);
    addAnnotationButton.shouldBe(visible);
    addAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);
    assertVexAnnotationForm(vexDrawer, "ABC-123", TEST_COMPONENT_PURL, "5.6", "Unverified",
        "test vulnerability", "SELECT", "SELECT",
        "SELECT", "Save", "");
  }

  @Test
  public void testFeatureEnabled_opensVexDrawer_addButton_checkContentWithVex_singleResponse() {
    setTestData();

    navigateToComponentDetailsPage();

    SelenideElement dropdownButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 6);
    dropdownButtonFirstRowColumn.shouldBe(visible);

    ElementsCollection rowButtons = dropdownButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement addAnnotationButton = rowButtons.get(1);
    addAnnotationButton.shouldBe(visible);
    addAnnotationButton.shouldHave(text("Edit Annotation"));
    addAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);
    assertVexAnnotationForm(vexDrawer, "DEF-456", TEST_COMPONENT_PURL, "1.6", "Unverified",
        "test vulnerability", "Exploitable", "Code not present",
        "Rollback", "Update", "");
  }

  @Test
  public void testFeatureEnabled_opensVexDrawer_addButton_checkContentWithVex_multipleResponse() {
    setTestDataWithMultipleResponsesInVexAnnotation();

    navigateToComponentDetailsPage();

    SelenideElement dropdownButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 6);
    dropdownButtonFirstRowColumn.shouldBe(visible);

    ElementsCollection rowButtons = dropdownButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement addAnnotationButton = rowButtons.get(1);
    addAnnotationButton.shouldBe(visible);
    addAnnotationButton.shouldHave(text("Edit Annotation"));
    addAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);
    assertVexAnnotationForm(vexDrawer, "DEF-456", TEST_COMPONENT_PURL, "1.6", "Unverified",
        "test vulnerability", "Exploitable", "Code not present",
        "Rollback", "Update", "");
  }

  @Test
  public void testFeatureEnabled_opensVexDrawer_addButton_submitFormSuccessfully() {
    setTestData();

    navigateToComponentDetailsPage();

    SelenideElement actionButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(0, 6);
    actionButtonFirstRowColumn.shouldBe(visible);
    ElementsCollection rowButtons = actionButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement addAnnotationButton = rowButtons.get(1);
    addAnnotationButton.shouldBe(visible);
    addAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);

    vexDrawer.analysisStatusDropdown().selectOption(1);

    vexDrawer.submitButton().click();
    vexDrawer.successModal().shouldBe(visible);
    vexDrawer.shouldNotBe(visible);

    ellipsisButton.click();
    SelenideElement editAnnotationButton = actionButtonFirstRowColumn.findAll("button").get(1);
    editAnnotationButton.shouldHave(text("Edit Annotation"));
    editAnnotationButton.click();

    vexDrawer.shouldBe(visible);
    assertVexAnnotationForm(vexDrawer, "ABC-123", TEST_COMPONENT_PURL, "5.6", "Unverified",
        "test vulnerability", "Exploitable", "SELECT",
        "SELECT", "Update", "");
  }

  @Test
  public void testFeatureEnabled_opensVexDrawer_editButton_updateFormSuccessfully() {
    setTestData();

    navigateToComponentDetailsPage();

    SelenideElement actionButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 6);
    actionButtonFirstRowColumn.shouldBe(visible);
    ElementsCollection rowButtons = actionButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement editAnnotationButton = rowButtons.get(1);
    editAnnotationButton.shouldBe(visible);
    editAnnotationButton.shouldHave(text("Edit Annotation"));

    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 0)
        .shouldHave(text("1.6"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 1)
        .shouldHave(text("DEF-456"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 4)
        .shouldHave(text("exploitable"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 5)
        .shouldHave(text("Code not present"));

    editAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);

    assertVexAnnotationForm(vexDrawer, "DEF-456", TEST_COMPONENT_PURL, "1.6",
        "Unverified", "test vulnerability2", "Exploitable",
        "Code Not Present", "Rollback", "Update",
        "test vex detail");

    vexDrawer.analysisStatusDropdown().selectOption(2);
    vexDrawer.justificationDropdown().selectOption(2);
    vexDrawer.responseDropdown().selectOption(2);
    vexDrawer.annotationDetails().setValue("XYZ edit");

    vexDrawer.submitButton().click();
    vexDrawer.successModal().shouldBe(visible);
    vexDrawer.shouldNotBe(visible);

    ellipsisButton.click();
    actionButtonFirstRowColumn.shouldHave(text("Edit"));

    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 4)
        .shouldHave(text("In triage"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 5)
        .shouldHave(text("Protected at perimeter"));

    editAnnotationButton.click();

    vexDrawer.shouldBe(visible);
    assertVexAnnotationForm(vexDrawer, "DEF-456", TEST_COMPONENT_PURL, "1.6",
        "Unverified", "test vulnerability2", "In triage",
        "Protected at perimeter", "Update", "Update",
        "XYZ edit");
  }

  @Test
  public void testFeatureEnabled_opensDeleteAnnotationModal_cancelAndSubmitButtons() {
    setTestData();

    navigateToComponentDetailsPage();

    SelenideElement actionButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 6);
    actionButtonFirstRowColumn.shouldBe(visible);
    ElementsCollection rowButtons = actionButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement deleteAnnotationButton = rowButtons.get(2);
    deleteAnnotationButton.shouldBe(visible);
    deleteAnnotationButton.shouldHave(text("Delete Annotation"));

    deleteAnnotationButton.click();

    DeleteAnnotationModal deleteModal = sbomManagerComponentDetailsPage.deleteAnnotationModal();
    deleteModal.shouldBe(visible);

    deleteModal.header().shouldBe(visible).shouldHave(text("Delete annotation for DEF-456"));
    deleteModal.body()
        .shouldBe(visible)
        .shouldHave(text("Are you sure you want to delete \"exploitable\" annotation for DEF-456?"));
    deleteModal.cancelButton().shouldBe(visible).shouldHave(text("Cancel"));
    deleteModal.submitButton().shouldBe(visible).shouldHave(text("Delete"));
    deleteModal.cancelButton().click();
    deleteModal.shouldNotBe(visible);
    ellipsisButton.click();
    deleteAnnotationButton.click();
    deleteModal.submitButton().click();
    deleteModal.successModal().shouldBe(visible);
    deleteModal.shouldNotBe(visible);
  }

  @Test
  public void testFeatureEnabled_opensCopyAnnotationModal_cancelAndSubmitButtons() {
    setTestDataWithSecondaryData(false, false);

    lookup(SbomComponentsService.class).getSbomComponentDetails(
        testApplication.getId(),
        thirdPartySbomMetadata.getSbomVersion(),
        thirdPartyFileCoordinate.getHash());

    navigateToComponentDetailsPage();

    SelenideElement actionButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 6);
    actionButtonFirstRowColumn.shouldBe(visible);
    ElementsCollection rowButtons = actionButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement deleteAnnotationButton = rowButtons.find(text("Delete Annotation"));
    deleteAnnotationButton.shouldNotBe(visible);

    SelenideElement copyAnnotationButton = rowButtons.get(2);
    copyAnnotationButton.shouldBe(visible);
    copyAnnotationButton.shouldHave(text("Copy Annotation"));

    copyAnnotationButton.click();

    CopyAnnotationModal copyModal = sbomManagerComponentDetailsPage.copyAnnotationModal();
    copyModal.shouldBe(visible);

    copyModal.header().shouldBe(visible).shouldHave(text("Copy annotation for DEF-456"));
    copyModal.body()
        .shouldBe(visible)
        .shouldHave(text(
            "Are you sure you want to copy \"Exploitable\" annotation for DEF-456 from previous version mockVersionId_2?"));
    copyModal.cancelButton().shouldBe(visible).shouldHave(text("Cancel"));
    copyModal.submitButton().shouldBe(visible).shouldHave(text("Copy"));
    copyModal.cancelButton().click();
    copyModal.shouldNotBe(visible);
    ellipsisButton.click();
    copyAnnotationButton.click();
    copyModal.submitButton().click();
    copyModal.successModal().shouldBe(visible);
    copyModal.shouldNotBe(visible);
  }

  @Test
  public void testCopyFromVexAnnotationWithNoResponse() {
    setTestDataWithSecondaryData(false, true);

    lookup(SbomComponentsService.class).getSbomComponentDetails(
        testApplication.getId(),
        thirdPartySbomMetadata.getSbomVersion(),
        thirdPartyFileCoordinate.getHash());

    navigateToComponentDetailsPage();

    SelenideElement actionButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 6);
    actionButtonFirstRowColumn.shouldBe(visible);
    ElementsCollection rowButtons = actionButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement copyAnnotationButton = rowButtons.get(2);
    copyAnnotationButton.shouldBe(visible);
    copyAnnotationButton.shouldHave(text("Copy Annotation"));

    copyAnnotationButton.click();

    CopyAnnotationModal copyModal = sbomManagerComponentDetailsPage.copyAnnotationModal();
    copyModal.shouldBe(visible);

    copyModal.header().shouldBe(visible).shouldHave(text("Copy annotation for DEF-456"));
    copyModal.body()
        .shouldBe(visible)
        .shouldHave(text(
            "Are you sure you want to copy \"Exploitable\" annotation for DEF-456 from previous version mockVersionId_2?"));
    copyModal.cancelButton().shouldBe(visible).shouldHave(text("Cancel"));
    copyModal.submitButton().shouldBe(visible).shouldHave(text("Copy"));
    copyModal.cancelButton().click();
    copyModal.shouldNotBe(visible);
    ellipsisButton.click();
    copyAnnotationButton.click();
    copyModal.submitButton().click();
    copyModal.successModal().shouldBe(visible);
    copyModal.shouldNotBe(visible);
  }

  @Test
  public void testFeatureEnabled_opensCopyAnnotationModal_multipleResponses() {
    setTestDataWithSecondaryData(true, false);

    navigateToComponentDetailsPage();

    SelenideElement actionButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 6);
    actionButtonFirstRowColumn.shouldBe(visible);
    ElementsCollection rowButtons = actionButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement deleteAnnotationButton = rowButtons.find(text("Delete Annotation"));
    deleteAnnotationButton.shouldNotBe(visible);

    SelenideElement copyAnnotationButton = rowButtons.get(2);
    copyAnnotationButton.shouldBe(visible);
    copyAnnotationButton.shouldHave(text("Copy Annotation"));

    copyAnnotationButton.click();

    CopyAnnotationModal copyModal = sbomManagerComponentDetailsPage.copyAnnotationModal();
    copyModal.shouldBe(visible);

    copyModal.header().shouldBe(visible).shouldHave(text("Copy annotation for DEF-456"));
    copyModal.body()
        .shouldBe(visible)
        .shouldHave(text(
            "Are you sure you want to copy \"Exploitable\" annotation for DEF-456 from previous version mockVersionId_2?"));
    copyModal.cancelButton().shouldBe(visible).shouldHave(text("Cancel"));
    copyModal.submitButton().shouldBe(visible).shouldHave(text("Copy"));
    copyModal.cancelButton().click();
    copyModal.shouldNotBe(visible);
    ellipsisButton.click();
    copyAnnotationButton.click();
    copyModal.submitButton().click();
    copyModal.successModal().shouldBe(visible);
    copyModal.shouldNotBe(visible);
  }

  @Test
  public void testFeatureDisabled_Error() {
    setTestData();
    setMissingFeature(LicensedFeature.SBOM_MANAGER);
    refreshOrOpen(SbomManagerComponentDetailsPage.url(testApplication.getPublicId(), thirdPartySbomMetadata
        .getSbomVersion(), thirdPartyFileCoordinate.getHash()));
    sbomManagerComponentDetailsPage.pageTitle().shouldNotBe(visible);
    eyesWatcher.eyesCheck("An error occurred loading data. The SBOM Manager license feature is not enabled.");
  }

  private void navigateToComponentDetailsPage() {
    // component details page state depends on bill of materials page state, so loading the bom page first
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(testApplication.getPublicId(), thirdPartySbomMetadata
        .getSbomVersion()));
    billOfMaterialsPageSummaryTile.componentSummaryChartAndProgress().shouldBe(visible, Duration.ofSeconds(20));

    refreshOrOpen(SbomManagerComponentDetailsPage.url(testApplication.getPublicId(), thirdPartySbomMetadata
        .getSbomVersion(), thirdPartyFileCoordinate.getHash()));
  }

  private void setTestData() {
    setTestData(true);
  }

  private void setTestData(boolean withComponentRef) {
    testOrganization = tempEntity.newOrganization();
    testApplication = tempEntity.newApplication(testOrganization.getId());
    setVulnerabilityTablesData(minimumDataSet(withComponentRef), true, "SBOM", false, false);
  }

  private void setTestDataWithMultipleResponsesInVexAnnotation() {
    testOrganization = tempEntity.newOrganization();
    testApplication = tempEntity.newApplication(testOrganization.getId());
    setVulnerabilityTablesData(minimumDataSet(true), true, "SBOM", true, false);
  }

  private void setTestDataWithSecondaryData(
      final boolean multipleResponsesInVexAnnotation,
      final boolean withNullResponseInVexAnnotation)
  {
    testOrganization = tempEntity.newOrganization();
    testApplication = tempEntity.newApplication(testOrganization.getId());

    ThirdPartyFile file = tempEntity.newThirdPartyFile();

    tempEntity.newThirdPartySbomMetadata(
        file.getId(),
        testApplication.getId(),
        TEST_SBOM_VERSION_ID + "_2",
        ACTIVE,
        file.getFilename(),
        "cycloneDX",
        "json",
        "1.5");

    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinate(
        file, "SBOM", "maven", "testComponent", "1.2", TEST_COMPONENT_HASH, TEST_COMPONENT_PURL);

    setVulnerabilityTablesData(fileCoordinate, true, "SBOM", multipleResponsesInVexAnnotation,
        withNullResponseInVexAnnotation);
    setVulnerabilityTablesData(minimumDataSet(true), false, "SBOM", multipleResponsesInVexAnnotation,
        withNullResponseInVexAnnotation);
  }

  private ThirdPartyFileCoordinate minimumDataSet(boolean withComponentRef) {
    ThirdPartyFile file = tempEntity.newThirdPartyFile();

    thirdPartySbomMetadata = tempEntity.newThirdPartySbomMetadata(
        file.getId(),
        testApplication.getId(),
        TEST_SBOM_VERSION_ID,
        ACTIVE,
        file.getFilename(),
        "cycloneDX",
        "json",
        "1.5");

    ThirdPartyScan scan = tempEntity.newThirdPartyScan(file);

    ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO = lookup(ThirdPartyFileCoordinateDAO.class);

    ThirdPartyFileCoordinate fileCoordinate = new ThirdPartyFileCoordinate(
        TEST_COMPONENT_HASH, "SBOM", "maven", "testComponent", "1.2", file.getId());
    fileCoordinate.setPackageUrl(TEST_COMPONENT_PURL);
    fileCoordinate.setId("86163fcc32524261bfd2bdbedb7eae43");
    String reportResourceName = "/sbom/ComponentDetailsTest/report";

    if (withComponentRef) {
      fileCoordinate.setComponentRef(TEST_COMPONENT_REF);
      reportResourceName = "/sbom/ComponentDetailsTest/reportWithComponentRef";
    }
    thirdPartyFileCoordinateDAO.insert(fileCoordinate);

    URL zippedReport = ReportHelper.zipReport(reportResourceName, tempDir);

    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    File reportDestination = work.getReportFile(testApplication.getId(), scan.getScanId());

    try {
      FileUtils.copyURLToFile(zippedReport, reportDestination);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    thirdPartyFileCoordinate = fileCoordinate;
    return fileCoordinate;
  }

  private void setVulnerabilityTablesData(
      ThirdPartyFileCoordinate thirdPartyFileCoordinate,
      boolean withVexAnnotations,
      String identificationSource,
      boolean withMultipleResponsesInVexAnnotation,
      boolean withNullResponseInVexAnnotation)
  {
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "ABC-123", null, "test vulnerability",
        "http://123.xyz", 5.6d, "testUser", "source", "v:1", "test severity", "123", "m1", "r1", "a1",
        identificationSource, null, null);
    ThirdPartyCoordinateSecurity vulnerabilityDEF456 = tempEntity.newThirdPartyCoordinateSecurity(
        thirdPartyFileCoordinate, "DEF-456", null,
        "test vulnerability2", "http://1234.xyz", 1.6d, "testUser", "source", "v:1", "testSeverity",
        "1234", "m1", "r1", "a1",
        identificationSource, null, null);
    if (withVexAnnotations) {
      tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityDEF456, "DEF-456", "exploitable",
          "code_not_present",
          withMultipleResponsesInVexAnnotation
              ? "rollback,update"
              : (withNullResponseInVexAnnotation ? null : "rollback"),
          "test vex detail");
    }
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "CVE-4812", null, "test vulnerability",
        "http://12345.xyz", 1.5d, "testUser", "source", "CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", "testSeverity",
        "12345", "m3", "r3", "a3", identificationSource, "DEEP_DIVE", "PRIMARY");

    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "sonatype-123", "test sonatype vulnerability",
        "http://sonatype.com", 9.6d, "testUser", "SONATYPE",
        "CVSS:1/1/1", "testSeverity", "a", "b", "c",
        "d", "Sonatype");

    ThirdPartyCoordinateSecurity vulnerabilitySona456 = tempEntity.newThirdPartyCoordinateSecurity(
        thirdPartyFileCoordinate, "sonatype-456", "test sonatype vulnerability2",
        "http://sonatype2.com", 4.6d, "testUser", "SONATYPE",
        "CVSS:1/1/1", "testSeverity", "a", "b", "c",
        "d", "Sonatype");
    if (withVexAnnotations) {
      tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilitySona456, "sonatype-456",
          "exploitable", "code_not_present", "rollback", "test vex detail");
    }
  }

  private void checkDisclosedVulnerabilitiesTableHeader() {
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().tableHeaders().shouldHave(size(7));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnHeader(0)
        .shouldHave(
            text("CVSS SCORE"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnHeader(1)
        .shouldHave(
            text("ISSUE"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnHeader(2)
        .shouldHave(
            text("VERIFICATION"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnHeader(3)
        .shouldHave(
            text("DATA ENRICHMENT"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnHeader(4)
        .shouldHave(
            text("ANALYSIS STATE"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnHeader(5)
        .shouldHave(
            text("JUSTIFICATION"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnHeader(6)
        .shouldHave(
            text("ACTIONS"));
  }

  private void checkSonatypeVulnerabilitiesTableHeader(VulnerabilitiesTableTile table) {
    table.tableHeaders().shouldHave(size(6));
    table.getColumnHeader(0)
        .shouldHave(
            text("CVSS SCORE"));
    table.getColumnHeader(1)
        .shouldHave(
            text("ISSUE"));
    table.getColumnHeader(2)
        .shouldHave(
            text("DATA ENRICHMENT"));
    table.getColumnHeader(3)
        .shouldHave(
            text("ANALYSIS STATE"));
    table.getColumnHeader(4)
        .shouldHave(
            text("JUSTIFICATION"));
    table.getColumnHeader(5)
        .shouldHave(
            text("ACTIONS"));
  }

  private void assertVulnerabilityTableRowContent(
      VulnerabilitiesTableTile table,
      String cvssScore,
      String issue,
      String verification,
      String dataEnrichment,
      String analysisStatus,
      String justification)
  {
    table.getColumnData(0, 0).shouldHave(text(cvssScore));
    table.getColumnData(0, 1).shouldHave(text(issue));
    if (verification != null) {
      table.getColumnData(0, 3).shouldHave(text(dataEnrichment));
      table.getColumnData(0, 4).shouldHave(text(analysisStatus));
      table.getColumnData(0, 5).shouldHave(text(justification));
    }
    else {
      table.getColumnData(0, 2).shouldHave(text(dataEnrichment));
      table.getColumnData(0, 3).shouldHave(text(analysisStatus));
      table.getColumnData(0, 4).shouldHave(text(justification));
    }
  }

  public void assertVexAnnotationForm(
      VexAnnotationDrawer vexAnnotationDrawer,
      String issue,
      String purl,
      String cvssScore,
      String verificationStatus,
      String vulnerabilityDescription,
      String analysisStatusDropdown,
      String justificationDropdown,
      String responseDropdown,
      String submitButtonText,
      String annotationDetails)
  {
    vexAnnotationDrawer.header().shouldBe(visible);
    vexAnnotationDrawer.closeButton().shouldBe(visible);
    vexAnnotationDrawer.header().shouldHave(text(issue));
    vexAnnotationDrawer.packageUrl().shouldHave(text(purl));
    vexAnnotationDrawer.cvssScore().shouldHave(text(cvssScore));
    vexAnnotationDrawer.verificationStatus().shouldHave(text(verificationStatus));
    vexAnnotationDrawer.vulnerabilityDescription().shouldHave(text(vulnerabilityDescription));
    vexAnnotationDrawer.analysisStatusDropdown().shouldHave(text(analysisStatusDropdown));
    vexAnnotationDrawer.justificationDropdown().shouldHave(text(justificationDropdown));
    vexAnnotationDrawer.responseDropdown().shouldHave(text(responseDropdown));
    vexAnnotationDrawer.submitButton().shouldHave(text(submitButtonText));
    if (!StringUtils.isEmptyOrNull(annotationDetails)) {
      vexAnnotationDrawer.annotationDetails().shouldHave(text(annotationDetails));
    }
  }

  public void assertVulnerabilityDetailsInsidePolicyViolationDetailsDrawer(
      PolicyViolationDetailsDrawer.VulnerabilityDetails vulnerabilityDetails)
  {
    vulnerabilityDetails.packageUrl().shouldHave(text("pkg:maven/2/3@1.1"));
    vulnerabilityDetails.vulnerabilityId().shouldHave(text("CVE-4812"));

    SelenideElement issueContent = vulnerabilityDetails.getVulnerabilityDetailsContentByFirstColumnIdx(1);
    issueContent.shouldHave(text("CVE-4812"));

    SelenideElement severityContent = vulnerabilityDetails.getVulnerabilityDetailsContentByFirstColumnIdx(2);
    severityContent.shouldHave(text("CVE CVSS 31.5 CVE CVSS 2.00.0"));

    SelenideElement kevContent = vulnerabilityDetails.getVulnerabilityDetailsContentByFirstColumnIdx(3);
    kevContent.shouldHave(text("Not listed"));

    SelenideElement epssContent = vulnerabilityDetails.getVulnerabilityDetailsContentByFirstColumnIdx(4);
    epssContent.shouldHave(text("123,444%"));

    SelenideElement weaknessContent = vulnerabilityDetails.getVulnerabilityDetailsContentByFirstColumnIdx(5);
    weaknessContent.shouldHave(text("CVE CWE400"));

    SelenideElement sourceContent = vulnerabilityDetails.getVulnerabilityDetailsContentByFirstColumnIdx(6);
    sourceContent.shouldHave(text("National Vulnerability Database"));

    SelenideElement categoryContent = vulnerabilityDetails.getVulnerabilityDetailsContentByFirstColumnIdx(7);
    categoryContent.shouldHave(text("Data"));

    SelenideElement descriptionFromCveContent =
        vulnerabilityDetails.getVulnerabilityDetailsContentBySecondColumnIdx(1);
    descriptionFromCveContent.shouldHave(text("In spring security versions prior to 5.4.11+, 5.5.7+ , 5.6.4+ " +
        "and older unsupported versions, RegexRequestMatcher can easily be misconfigured to be bypassed on some " +
        "servlet containers. Applications using RegexRequestMatcher with `.`" +
        " in the regular expression are possibly " +
        "vulnerable to an authorization bypass."));

    SelenideElement explanationContent = vulnerabilityDetails
        .getVulnerabilityDetailsContentBySecondColumnIdx(2);
    explanationContent.shouldHave(text("The spring-security-web package is vulnerable to Authorization Bypass. " +
        "The RegexRequestMatcher() function in the RegexRequestMatcher class and the addSecureUrl() function in " +
        "the DefaultFilterInvocationSecurityMetadataSource class can return an unexpected match when a . character " +
        "is used in a regular expression."));

    SelenideElement detectionContent = vulnerabilityDetails
        .getVulnerabilityDetailsContentBySecondColumnIdx(3);
    detectionContent.shouldHave(text("The application is vulnerable by using this component."));

    SelenideElement recommendationContent = vulnerabilityDetails
        .getVulnerabilityDetailsContentBySecondColumnIdx(4);
    recommendationContent.shouldHave(text("We recommend upgrading to a version of this component that is " +
        "not vulnerable to this specific issue"));

    SelenideElement rootCauseContent = vulnerabilityDetails
        .getVulnerabilityDetailsContentBySecondColumnIdx(5);
    rootCauseContent.shouldHave(text("spring-security-web-5.6.2.jar"));
    rootCauseContent.shouldHave(text("org/springframework/security/web/util/matcher/" +
        "RegexRequestMatcher.class[5.6.0.M0 , 5.6.4"));

    SelenideElement advisoriesContent = vulnerabilityDetails
        .getVulnerabilityDetailsContentBySecondColumnIdx(6);
    advisoriesContent.shouldHave(text("Third Partyhttps://issues.apache.org/jira/browse/FILEUPLOAD-250"));

    SelenideElement cvssDetailsContent = vulnerabilityDetails
        .getVulnerabilityDetailsContentBySecondColumnIdx(7);
    cvssDetailsContent.shouldHave(text("CVE CVSS 31.5"));
    cvssDetailsContent.shouldHave(text("CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"));
  }

  public void assertVulnerabilityDetailsFromThirdParty(VulnerabilityDetailsPopover vulnerabilityDetailsPopover) {
    vulnerabilityDetailsPopover.popoverTitle().shouldHave(text("Vulnerability Details CVE-4812"));
    vulnerabilityDetailsPopover.packageUrl().shouldHave(text("pkg:maven/2/3@1.1"));
    vulnerabilityDetailsPopover.vulnerabilityId().shouldHave(text("CVE-4812"));

    SelenideElement issueContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(1);
    issueContent.shouldHave(text("CVE-4812"));

    SelenideElement severityContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(2);
    severityContent.shouldHave(text("CVE CVSS 31.5 CVE CVSS 2.00.0"));

    SelenideElement kevContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(3);
    kevContent.shouldHave(text("Not listed"));

    SelenideElement epssContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(4);
    epssContent.shouldHave(text("123,444%"));

    SelenideElement weaknessContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(5);
    weaknessContent.shouldHave(text("CVE CWE400"));

    SelenideElement sourceContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(6);
    sourceContent.shouldHave(text("National Vulnerability Database"));

    SelenideElement explanationContent =
        vulnerabilityDetailsPopover.getVulnerabilityDetailsContentBySecondColumnIdx(1);
    explanationContent.shouldHave(partialText("In spring security versions prior to 5.4.11+"));

    SelenideElement recomendationContent = vulnerabilityDetailsPopover
        .getVulnerabilityDetailsContentBySecondColumnIdx(2);
    recomendationContent.shouldHave(
        partialText("The spring-security-web package is vulnerable to Authorization Bypass"));

    SelenideElement detectionContent = vulnerabilityDetailsPopover
        .getVulnerabilityDetailsContentBySecondColumnIdx(3);
    detectionContent.shouldHave(text("The application is vulnerable by using this component."));

    SelenideElement cdssDetailsContent = vulnerabilityDetailsPopover
        .getVulnerabilityDetailsContentBySecondColumnIdx(4);
    cdssDetailsContent.shouldHave(partialText("We recommend upgrading to a version of this component"));
  }

  public void assertVulnerabilityDetails(VulnerabilityDetailsPopover vulnerabilityDetailsPopover) {
    vulnerabilityDetailsPopover.popoverTitle().shouldHave(text("Vulnerability Details CVE-4812"));
    vulnerabilityDetailsPopover.packageUrl().shouldHave(text("pkg:maven/2/3@1.1"));
    vulnerabilityDetailsPopover.vulnerabilityId().shouldHave(text("CVE-4812"));
    vulnerabilityDetailsPopover.getResearchTypeTag().shouldHave(text("Deep Dive")).shouldBe(visible);

    SelenideElement issueContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(1);
    issueContent.shouldHave(text("CVE-4812"));

    SelenideElement severityContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(2);
    severityContent.shouldHave(text("CVE CVSS 31.5 CVE CVSS 2.00.0"));

    SelenideElement kevContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(3);
    kevContent.shouldHave(text("Not listed"));

    SelenideElement epssContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(4);
    epssContent.shouldHave(text("123,444%"));

    SelenideElement weaknessContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(5);
    weaknessContent.shouldHave(text("CVE CWE400"));

    SelenideElement sourceContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(6);
    sourceContent.shouldHave(text("National Vulnerability Database"));

    SelenideElement categoryContent = vulnerabilityDetailsPopover.getVulnerabilityDetailsContentByFirstColumnIdx(7);
    categoryContent.shouldHave(text("Data"));

    SelenideElement descriptionFromCveContent =
        vulnerabilityDetailsPopover.getVulnerabilityDetailsContentBySecondColumnIdx(1);
    descriptionFromCveContent.shouldHave(text("In spring security versions prior to 5.4.11+, 5.5.7+ , 5.6.4+ " +
        "and older unsupported versions, RegexRequestMatcher can easily be misconfigured to be bypassed on some " +
        "servlet containers. Applications using RegexRequestMatcher with `.` in the regular expression are possibly " +
        "vulnerable to an authorization bypass."));

    SelenideElement explanationContent = vulnerabilityDetailsPopover
        .getVulnerabilityDetailsContentBySecondColumnIdx(2);
    explanationContent.shouldHave(text("The spring-security-web package is vulnerable to Authorization Bypass. " +
        "The RegexRequestMatcher() function in the RegexRequestMatcher class and the addSecureUrl() function in " +
        "the DefaultFilterInvocationSecurityMetadataSource class can return an unexpected match when a . character " +
        "is used in a regular expression."));

    SelenideElement detectionContent = vulnerabilityDetailsPopover
        .getVulnerabilityDetailsContentBySecondColumnIdx(3);
    detectionContent.shouldHave(text("The application is vulnerable by using this component."));

    SelenideElement recommendationContent = vulnerabilityDetailsPopover
        .getVulnerabilityDetailsContentBySecondColumnIdx(4);
    recommendationContent.shouldHave(text("We recommend upgrading to a version of this component that is " +
        "not vulnerable to this specific issue"));

    SelenideElement rootCauseContent = vulnerabilityDetailsPopover
        .getVulnerabilityDetailsContentBySecondColumnIdx(5);
    rootCauseContent.shouldHave(text("spring-security-web-5.6.2.jar"));
    rootCauseContent.shouldHave(text("org/springframework/security/web/util/matcher/" +
        "RegexRequestMatcher.class[5.6.0.M0 , 5.6.4"));

    SelenideElement advisoriesContent = vulnerabilityDetailsPopover
        .getVulnerabilityDetailsContentBySecondColumnIdx(6);
    advisoriesContent.shouldHave(text("Third Partyhttps://issues.apache.org/jira/browse/FILEUPLOAD-250"));

    SelenideElement cvssDetailsContent = vulnerabilityDetailsPopover
        .getVulnerabilityDetailsContentBySecondColumnIdx(7);
    cvssDetailsContent.shouldHave(text("CVE CVSS 31.5"));
    cvssDetailsContent.shouldHave(text("CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"));

    vulnerabilityDetailsPopover.getResearchMetadataHeader()
        .shouldHave(text("Vulnerability Research Metadata"))
        .shouldBe(visible);
    vulnerabilityDetailsPopover.getResearchMetadataHeader().click();
    SelenideElement researchMetadataContent = vulnerabilityDetailsPopover.getResearchMetadataContent();
    researchMetadataContent.shouldHave(text("Vulnerability Detection Type"));
    researchMetadataContent.shouldHave(text("Primary"));
    researchMetadataContent.shouldHave(
        text("Research has validated the association between the component and the vulnerability."));
    researchMetadataContent.shouldHave(text("Data Enrichment"));
    researchMetadataContent.shouldHave(text("Sonatype Enhanced"));
    researchMetadataContent.shouldHave(text(
        "Sonatype Research has done additional research to confirm the association" +
            " of the vulnerability to the component."));
    researchMetadataContent.shouldHave(text("Detection Source"));
    researchMetadataContent.shouldHave(text("Sonatype Identified"));
    researchMetadataContent.shouldHave(text(
        "The association between this vulnerability and the component was identified in Sonatype's data catalog."));
  }

  private void mockHdsResponseForVulnerabilityDetails() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails_CVE-4812.json"))
        .atUri("rest/vulnerability/details/json/CVE-4812");
    testCLMServer.getHdsServer()
        .respondWith(Collections.emptyMap())
        .atUri("rest/vulnerability/details/json");
  }

  @Test
  public void testVexAnnotationDrawer_showsUnsavedChangesModal_whenClosingWithModifiedDetails() {
    setTestData();

    navigateToComponentDetailsPage();

    // Open the VEX annotation drawer
    SelenideElement dropdownButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(0, 6);
    ElementsCollection rowButtons = dropdownButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement addAnnotationButton = rowButtons.get(1);
    addAnnotationButton.shouldBe(visible);
    addAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);
    vexDrawer.header().shouldBe(visible).shouldHave(partialText("Annotate"));

    // Modify the details field
    vexDrawer.annotationDetails().shouldBe(visible);
    vexDrawer.annotationDetails().setValue("Test unsaved changes");

    // Try to close the drawer by clicking outside or ESC
    vexDrawer.closeButton().click();

    // Verify unsaved changes modal appears
    vexDrawer.unsavedChangesModal().shouldBe(visible);
    vexDrawer.unsavedChangesModalHeader().shouldBe(visible).shouldHave(text("Unsaved Changes"));
    vexDrawer.unsavedChangesModalBody()
        .shouldBe(visible)
        .shouldHave(partialText("The page may contain unsaved changes; continuing will discard them."));
  }

  @Test
  public void testVexAnnotationDrawer_unsavedChangesModal_cancelButton_keepsDrawerOpen() {
    setTestData();

    navigateToComponentDetailsPage();

    // Open the VEX annotation drawer
    SelenideElement dropdownButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(0, 6);
    ElementsCollection rowButtons = dropdownButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement addAnnotationButton = rowButtons.get(1);
    addAnnotationButton.shouldBe(visible);
    addAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);

    // Modify the details field
    vexDrawer.annotationDetails().setValue("Test unsaved changes");

    // Try to close
    vexDrawer.closeButton().click();

    // Verify unsaved changes modal appears
    vexDrawer.unsavedChangesModal().shouldBe(visible);

    // Click Cancel button
    vexDrawer.unsavedChangesModalCancelButton().shouldBe(visible).shouldHave(text("Cancel"));
    vexDrawer.unsavedChangesModalCancelButton().click();

    // Verify modal is closed
    vexDrawer.unsavedChangesModal().shouldNotBe(visible);

    // Verify drawer is still open
    vexDrawer.shouldBe(visible);
    vexDrawer.header().shouldBe(visible);
  }

  @Test
  public void testVexAnnotationDrawer_unsavedChangesModal_continueButton_closesDrawer() {
    setTestData();

    navigateToComponentDetailsPage();

    // Open the VEX annotation drawer
    SelenideElement dropdownButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(0, 6);
    ElementsCollection rowButtons = dropdownButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement addAnnotationButton = rowButtons.get(1);
    addAnnotationButton.shouldBe(visible);
    addAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);

    // Modify the details field
    vexDrawer.annotationDetails().setValue("Test unsaved changes");

    // Try to close
    vexDrawer.closeButton().click();

    // Verify unsaved changes modal appears
    vexDrawer.unsavedChangesModal().shouldBe(visible);

    // Click Continue button
    vexDrawer.unsavedChangesModalContinueButton().shouldBe(visible).shouldHave(text("Continue"));
    vexDrawer.unsavedChangesModalContinueButton().click();

    // Verify both modal and drawer are closed
    vexDrawer.unsavedChangesModal().shouldNotBe(visible);
    vexDrawer.shouldNotBe(visible);
  }

  @Test
  public void testVexAnnotationDrawer_noUnsavedChangesModal_whenClosingWithoutChanges() {
    setTestData();

    navigateToComponentDetailsPage();

    // Open the VEX annotation drawer
    SelenideElement dropdownButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(0, 6);
    ElementsCollection rowButtons = dropdownButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement addAnnotationButton = rowButtons.get(1);
    addAnnotationButton.shouldBe(visible);
    addAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);

    // Don't make any changes, just close
    vexDrawer.closeButton().click();

    // Verify unsaved changes modal does NOT appear
    vexDrawer.unsavedChangesModal().shouldNotBe(visible);

    // Verify drawer is closed
    vexDrawer.shouldNotBe(visible);
  }

  @Test
  public void testVexAnnotationDrawer_showsUnsavedChangesModal_whenModifyingDropdown() {
    setTestData();

    navigateToComponentDetailsPage();

    // Open the VEX annotation drawer
    SelenideElement dropdownButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(0, 6);
    ElementsCollection rowButtons = dropdownButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton = rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement addAnnotationButton = rowButtons.get(1);
    addAnnotationButton.shouldBe(visible);
    addAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);

    // Select a value in analysis status dropdown
    vexDrawer.analysisStatusDropdown().shouldBe(visible);
    vexDrawer.analysisStatusDropdown().selectOption("Resolved");
    vexDrawer.analysisStatusDropdown().shouldHave(text("Resolved"));

    // Try to close
    vexDrawer.closeButton().click();

    // Verify unsaved changes modal appears
    vexDrawer.unsavedChangesModal().shouldBe(visible);
    vexDrawer.unsavedChangesModalHeader().shouldBe(visible).shouldHave(text("Unsaved Changes"));
  }
}
