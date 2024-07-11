/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom.manager;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.sbommanager.componentdetails.ComponentDetailsSummaryTile;
import com.sonatype.clm.testing.functional.elements.sbommanager.componentdetails.DependencyTreeTile;
import com.sonatype.clm.testing.functional.elements.sbommanager.componentdetails.VulnerabilitiesTableTile;
import com.sonatype.clm.testing.functional.elements.sbommanager.componentdetails.VexAnnotationDrawer;
import com.sonatype.clm.testing.functional.pages.SbomManagerComponentDetailsPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;
import org.eclipse.jgit.util.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class ComponentDetailsPageTest extends AbstractFunctionalTest
{
  public static final String TEST_SBOM_VERSION_ID = "mockVersionId";

  public static final String TEST_COMPONENT_HASH = "mockComponentHash";

  public static final String TEST_COMPONENT_PURL = "pkg:maven/2/3@1.1";

  private static SbomManagerComponentDetailsPage sbomManagerComponentDetailsPage;

  private Application testApplication;

  private Organization testOrganization;

  private ThirdPartySbomMetadata thirdPartySbomMetadata;

  private ThirdPartyFileCoordinate thirdPartyFileCoordinate;

  @BeforeClass
  public static void beforeClass() {
    sbomManagerComponentDetailsPage = new SbomManagerComponentDetailsPage(
        new ComponentDetailsSummaryTile(),
        new VulnerabilitiesTableTile("disclosedVulnerabilities"),
        new VulnerabilitiesTableTile("sonatypeIdentifiedVulnerabilities"),
        new DependencyTreeTile(),
        new VexAnnotationDrawer());

    Selenide.open("/#");
    loginAsAdmin();
  }

  @Before
  public void beforeEachMethod() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);
    refreshOrOpen("/#");
  }

  @Test
  public void testFeatureEnabled_showContent() {
    setTestData();

    refreshOrOpen(SbomManagerComponentDetailsPage.url(testApplication.getPublicId(), thirdPartySbomMetadata
        .getSbomVersion(), thirdPartyFileCoordinate.getHash()));

    // Header
    sbomManagerComponentDetailsPage.pageTitle().shouldBe(visible);
    sbomManagerComponentDetailsPage.pageTitle().shouldHave(text("2 : 3 : 1.1"));

    sbomManagerComponentDetailsPage.reportInfoItems().shouldHave(CollectionCondition.size(3));
    sbomManagerComponentDetailsPage.reportInfoItems().get(0).shouldHave(text(testOrganization.getName()));
    sbomManagerComponentDetailsPage.reportInfoItems().get(1).shouldHave(text(testApplication.getName()));
    sbomManagerComponentDetailsPage.reportInfoItems().get(2).shouldHave(text("BOM"));

    // Tags
    sbomManagerComponentDetailsPage.tags().shouldHave(CollectionCondition.size(2));
    sbomManagerComponentDetailsPage.tags().get(0).shouldHave(text("Maven"));
    sbomManagerComponentDetailsPage.tags().get(1).shouldHave(text(TEST_COMPONENT_PURL));

    // Component Summary
    sbomManagerComponentDetailsPage.componentSummary().header().shouldHave(text("Component Summary"));
    sbomManagerComponentDetailsPage.componentSummary().highestScoreLabel().shouldHave(text("Highest CVSS Score"));
    sbomManagerComponentDetailsPage.componentSummary().vulnerabilitiesVerifiedLabel()
        .shouldHave(text("Vulnerabilities Verified"));
    sbomManagerComponentDetailsPage.componentSummary().highestScoreValue().shouldHave(text("9.6"));
    sbomManagerComponentDetailsPage.componentSummary().sonatypeVerified().shouldHave(text("0 Sonatype Verified"));
    sbomManagerComponentDetailsPage.componentSummary().unVerified().shouldHave(text("2 Unverified"));

    // Disclosed vulnerabilities
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().header().shouldHave(text("Disclosed Vulnerabilities"));

    checkDisclosedVulnerabilitiesTableHeader();
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().tableRows().shouldHave(CollectionCondition
        .size(2));
    assertVulnerabilityTableRowContent(sbomManagerComponentDetailsPage.disclosedVulnerabilities(),
        "5.6", "ABC-123", "Unverified", "Unannotated", " ");

    checkSonatypeVulnerabilitiesTableHeader(sbomManagerComponentDetailsPage.sonatypeVulnerabilitiesTile());
    assertVulnerabilityTableRowContent(sbomManagerComponentDetailsPage.sonatypeVulnerabilitiesTile(),
        "9.6", "sonatype-123", null, "Unannotated", " ");

    sbomManagerComponentDetailsPage.dependencyTreeTile().header().shouldHave(text("Dependency Tree"));
    sbomManagerComponentDetailsPage.dependencyTreeTile().content().shouldHave(text("Dependency Tree not available"));

    eyesWatcher.eyesCheck("mockComponent");
  }

  @Test
  public void testFeatureEnabled_opensVexDrawer_addButton_checkContent() {
    setTestData();

    refreshOrOpen(SbomManagerComponentDetailsPage.url(testApplication.getPublicId(), thirdPartySbomMetadata
        .getSbomVersion(), thirdPartyFileCoordinate.getHash()));

    SelenideElement dropdownButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(0, 5);
    dropdownButtonFirstRowColumn.shouldBe(visible);

    ElementsCollection rowButtons =  dropdownButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton =  rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement addAnnotationButton =  rowButtons.get(1);
    addAnnotationButton.shouldBe(visible);
    addAnnotationButton.shouldHave(text("Add Annotation"));
    addAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);
    assertVexAnnotationForm(vexDrawer, "ABC-123", TEST_COMPONENT_PURL, "5.6", "Unverified",
        "test vulnerability", "SELECT", "SELECT",
        "SELECT", "Save", "");
  }

  @Test
  public void testFeatureEnabled_opensVexDrawer_addButton_submitFormSuccessfully() {
    setTestData();

    refreshOrOpen(SbomManagerComponentDetailsPage.url(testApplication.getPublicId(), thirdPartySbomMetadata
        .getSbomVersion(), thirdPartyFileCoordinate.getHash()));

    SelenideElement actionButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(0, 5);
    actionButtonFirstRowColumn.shouldBe(visible);
    ElementsCollection rowButtons =  actionButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton =  rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement addAnnotationButton =  rowButtons.get(1);
    addAnnotationButton.shouldBe(visible);
    addAnnotationButton.shouldHave(text("Add Annotation"));
    addAnnotationButton.click();

    VexAnnotationDrawer vexDrawer = sbomManagerComponentDetailsPage.vexAnnotationDrawer();
    vexDrawer.shouldBe(visible);

    vexDrawer.analysisStatusDropdown().selectOption(1);

    vexDrawer.submitButton().click();
    vexDrawer.successModal().shouldBe(visible);
    vexDrawer.shouldNotBe(visible);
    ellipsisButton.click();
    actionButtonFirstRowColumn.findAll("button").get(1).shouldHave(text("Edit Annotation"));
  }

  @Test
  public void testFeatureEnabled_opensVexDrawer_editButton_updateFormSuccessfully() {
    setTestData();

    refreshOrOpen(SbomManagerComponentDetailsPage.url(testApplication.getPublicId(), thirdPartySbomMetadata
        .getSbomVersion(), thirdPartyFileCoordinate.getHash()));

    SelenideElement actionButtonFirstRowColumn = sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 5);
    actionButtonFirstRowColumn.shouldBe(visible);
    ElementsCollection rowButtons =  actionButtonFirstRowColumn.findAll("button");
    SelenideElement ellipsisButton =  rowButtons.get(0);
    ellipsisButton.shouldBe(visible);
    ellipsisButton.click();

    SelenideElement editAnnotationButton =  rowButtons.get(1);
    editAnnotationButton.shouldBe(visible);
    editAnnotationButton.shouldHave(text("Edit Annotation"));

    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 0).shouldHave(text("1.6"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 1).shouldHave(text("DEF-456"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 3).shouldHave(text("exploitable"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 4).shouldHave(text("Code not present"));

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
        .getColumnData(1, 3).shouldHave(text("In triage"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities()
        .getColumnData(1, 4).shouldHave(text("Protected at perimeter"));

    editAnnotationButton.click();

    vexDrawer.shouldBe(visible);
    assertVexAnnotationForm(vexDrawer, "DEF-456", TEST_COMPONENT_PURL, "1.6",
        "Unverified", "test vulnerability2", "In triage",
        "Protected at perimeter", "Update", "Update",
        "XYZ edit");
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

  private void setTestData() {
    setVulnerabilityTablesData(minimumDataSet());
  }

  private ThirdPartyFileCoordinate minimumDataSet() {
    testOrganization = tempEntity.newOrganization();
    testApplication = tempEntity.newApplication(testOrganization.getId());
    ThirdPartyFile file = tempEntity.newThirdPartyFile();

    thirdPartySbomMetadata = tempEntity.newThirdPartySbomMetadata(
        file.getId(),
        testApplication.getId(),
        TEST_SBOM_VERSION_ID,
        "ACTIVE",
        file.getFilename(),
        "cycloneDX",
        "json",
        "1.5"
    );

    thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(file, "SBOM", "maven",
        "testComponent", "1.2", TEST_COMPONENT_HASH, TEST_COMPONENT_PURL);
    return thirdPartyFileCoordinate;
  }

  private void setVulnerabilityTablesData(ThirdPartyFileCoordinate thirdPartyFileCoordinate) {
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "ABC-123", "test vulnerability",
        "http://123.xyz", 5.6d, "testSeverity", "testUser");
    ThirdPartyCoordinateSecurity vulnerabilityDEF456 = tempEntity.newThirdPartyCoordinateSecurity(
        thirdPartyFileCoordinate, "DEF-456", "test vulnerability2",
        "http://1234.xyz", 1.6d, "testSeverity", "testUser");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityDEF456, "DEF-456", "exploitable",
        "code_not_present", "rollback", "test vex detail");

    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "sonatype-123", "test sonatype vulnerability",
        "http://sonatype.com", 9.6d, "testUser", "SONATYPE",
        "CVSS:1/1/1", "testSeverity", "a", "b", "c",
        "d", "Sonatype");

    ThirdPartyCoordinateSecurity vulnerabilitySona456 = tempEntity.newThirdPartyCoordinateSecurity(
        thirdPartyFileCoordinate, "sonatype-456", "test sonatype vulnerability2",
        "http://sonatype2.com", 4.6d, "testUser", "SONATYPE",
        "CVSS:1/1/1", "testSeverity", "a", "b", "c",
        "d", "Sonatype");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilitySona456, "sonatype-456",
        "exploitable", "code_not_present", "rollback", "test vex detail");
  }

  private void checkDisclosedVulnerabilitiesTableHeader() {
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().tableHeaders().shouldHave(CollectionCondition
        .size(6));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().getColumnHeader(0).shouldHave(
        text("CVSS SCORE"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().getColumnHeader(1).shouldHave(
        text("ISSUE"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().getColumnHeader(2).shouldHave(
        text("VERIFIED STATUS"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().getColumnHeader(3).shouldHave(
        text("ANALYSIS STATUS"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().getColumnHeader(4).shouldHave(
        text("JUSTIFICATION"));
    sbomManagerComponentDetailsPage.disclosedVulnerabilities().getColumnHeader(5).shouldHave(
        text("ACTIONS"));
  }

  private void checkSonatypeVulnerabilitiesTableHeader(VulnerabilitiesTableTile table) {
    table.tableHeaders().shouldHave(CollectionCondition
        .size(5));
    table.getColumnHeader(0).shouldHave(
        text("CVSS SCORE"));
    table.getColumnHeader(1).shouldHave(
        text("ISSUE"));
    table.getColumnHeader(2).shouldHave(
        text("ANALYSIS STATUS"));
    table.getColumnHeader(3).shouldHave(
        text("JUSTIFICATION"));
    table.getColumnHeader(4).shouldHave(
        text("ACTIONS"));
  }

  private void assertVulnerabilityTableRowContent(
      VulnerabilitiesTableTile table,
      String cvssScore, String issue, String verifiedStatus,
      String analysisStatus, String justification)
  {
    table.getColumnData(0, 0).shouldHave(text(cvssScore));
    table.getColumnData(0, 1).shouldHave(text(issue));
    if (verifiedStatus != null) {
      table.getColumnData(0, 2).shouldHave(text(verifiedStatus));
      table.getColumnData(0, 3).shouldHave(text(analysisStatus));
      table.getColumnData(0, 4).shouldHave(text(justification));
    }
    else
    {
      table.getColumnData(0, 2).shouldHave(text(analysisStatus));
      table.getColumnData(0, 3).shouldHave(text(justification));
    }

  }

  public void assertVexAnnotationForm(VexAnnotationDrawer vexAnnotationDrawer, String issue, String purl,
                                      String cvssScore, String verificationStatus, String vulnerabilityDescription,
                                      String analysisStatusDropdown, String justificationDropdown,
                                      String responseDropdown, String submitButtonText, String annotationDetails)
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
}
