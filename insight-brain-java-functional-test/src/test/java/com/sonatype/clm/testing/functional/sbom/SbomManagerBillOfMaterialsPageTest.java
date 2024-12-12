/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.sbom.ComponentsTile;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.sbom.BillOfMaterialsPageSummaryTile;
import com.sonatype.clm.testing.functional.pages.sbom.LearnMoreSbomManagerPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerBillOfMaterialsPage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SbomManagerBillOfMaterialsPageTest
    extends AbstractFunctionalTest
{
  private final SbomManagerBillOfMaterialsPage sbomManagerBillOfMaterialsPage = new SbomManagerBillOfMaterialsPage();

  private final BillOfMaterialsPageSummaryTile billOfMaterialsPageSummaryTile = new BillOfMaterialsPageSummaryTile();

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private Organization organization;

  private Application application;

  private ThirdPartySbomMetadata sbomMetadata;

  private ThirdPartyFile scannedFile;

  private final InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  @Before
  public void init() throws Exception {
    thirdPartySbomMetadataDAO = lookup(ThirdPartySbomMetadataDAO.class);
    organization = tempEntity.newOrganization("test-organization");
    application = tempEntity.newApplication("Test Application", "test-application", organization.getId());
    Path zippedBom = mockOriginalSbom(SbomManagerBillOfMaterialsPageTest.class, "simple-bom.xml",
        insightWork.getSbomDir(application.getId()).toPath());

    scannedFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(scannedFile);

    File reportFile = insightWork.getReportFile(application.getId(), scan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
            .zipReport("/SbomManagerBillOfMaterialsPageTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, scan.getScanId());

    sbomMetadata = tempEntity.newThirdPartySbomMetadata(
      scannedFile.getId(),
      application.getId(),
      "test-version",
      ACTIVE,
        zippedBom.getFileName().toString(),
      SbomSpecification.CYCLONEDX.toString(),
      SbomFormat.XML.name(),
        "1.6"
    );
  }

  @BeforeClass
  public static void initialLogin() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void setLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
  }

  @Test
  public void testBillOfMaterial_ComponentSummaryChart() {
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(scannedFile.getId(),
            "s", "SPDX", "n1", "v1", "h1", "u1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r1", "d1", "l1", 5.5, "sd1", "f1");
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    billOfMaterialsPageSummaryTile.componentSummaryChartAndProgress().shouldBe(visible);
    billOfMaterialsPageSummaryTile.componentSummaryChartAndProgress().shouldHave(text("Component Summary\n" +
        "1\n" +
        "0 Direct\n" +
        "0 Transitive\n" +
        "1 Unspecified")).shouldBe(visible);
  }

  @Test
  public void testBillOfMaterial_VulnerabilitySummaryChart() {
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(scannedFile.getId(),
            "s", "SPDX", "n1", "v1", "h1", "u1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r1", "d1", "l1", 5.5, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r2", "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r3", "d3", "l3", 3.5, "sd3", "f3");
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    billOfMaterialsPageSummaryTile.vulnerabilitySummaryChartAndProgress().shouldBe(visible);
    billOfMaterialsPageSummaryTile.vulnerabilitySummaryChartAndProgress().shouldHave(text("Vulnerabilities Summary\n" +
        "3\n" +
        "0 Critical\n" +
        "1 High\n" +
        "1 Medium\n" +
        "1 Low")).shouldBe(visible);
  }

  @Test
  public void testBillOfMaterial_PolicyViolationSummaryChart() {
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(scannedFile.getId(),
            "s", "SPDX", "n1", "v1", "h1", "u1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r1", "d1", "l1", 5.5, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r2", "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r3", "d3", "l3", 3.5, "sd3", "f3");
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    billOfMaterialsPageSummaryTile.policyViolationSummaryChartAndProgress().shouldBe(visible);
    billOfMaterialsPageSummaryTile.policyViolationSummaryChartAndProgress()
        .shouldHave(text("Policy Violation Summary\n" +
        "4\n" +
        "2 Critical\n" +
        "1 Severe\n" +
        "1 Moderate\n" +
        "0 Low")).shouldBe(visible);
  }

  @Test
  public void testBillOfMaterial_PolicyViolationSummaryChartHidden() {
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(scannedFile.getId(),
            "s", "SPDX", "n1", "v1", "h1", "u1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r1", "d1", "l1", 5.5, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r2", "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r3", "d3", "l3", 3.5, "sd3", "f3");
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(false);
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    billOfMaterialsPageSummaryTile.policyViolationSummaryChartAndProgress().shouldNotBe(visible);
  }

  @Test
  public void testBillOfMaterial_AnnotatedVulnerabilitiesSummaryDescription() {
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(scannedFile.getId(),
            "s", "SPDX", "n1", "v1", "h1", "u1");
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    billOfMaterialsPageSummaryTile.annotatedVulnerabilitiesSummaryDescription().shouldBe(visible);
    billOfMaterialsPageSummaryTile.annotatedVulnerabilitiesSummaryDescription()
        .shouldHave(text("No vulnerabilities to annotate")).shouldBe(visible);

    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r1", "d1", "l1", 5.5, "sd1", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r2", "d2", "l2", 7.5, "sd2", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r3", "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity1,
        "r1", "s1", "j1", "r1", "d1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity2,
        "r1", "s1", "j1", "r1", "d1");

    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    billOfMaterialsPageSummaryTile.annotatedVulnerabilitiesSummaryDescription().shouldHave(text("66.7% of " +
        "vulnerabilities annotated with exploitability information")).shouldBe(visible);
  }

  @Test
  public void testBillOfMaterial_MetadataAccordion() {
    String metadataJson =
        "{\"created\":\"2020-01-01T00:00:00Z\",\"creators\":[{\"type\":\"Author\",\"name\":\"John Doe\"," +
            "\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"},{\"type\":\"Manufacturer\"," +
            "\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"," +
            "\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Manufacturer\"," +
            "\"name\":\"Jane Doe\",\"email\":\"jane.doe@example.com\",\"phone\":\"1-800-222-2222\"," +
            "\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Supplier\",\"name\":\"John Doe\"," +
            "\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"," +
            "\"url\":\"example.com,example2.com,example3.com\"},{\"type\":\"Supplier\",\"name\":\"Jane Doe\"," +
            "\"email\":\"jane.doe@example.com\",\"phone\":\"1-800-222-2222\"," +
            "\"url\":\"example.com,example2.com,example3.com\"}],\"tools\":[{\"type\":\"application\"," +
            "\"name\":\"Tool\",\"version\":\"1.0-RELEASE\"}]}";
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(scannedFile.getId(),
            "s", "SPDX", "n1", "v1", "h1", "u1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r1", "d1", "l1", 5.5, "sd1", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r2", "d2", "l2", 7.5, "sd2", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r3", "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity1,
        "r1", "s1", "j1", "r1", "d1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity2,
        "r1", "s1", "j1", "r1", "d1");
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    billOfMaterialsPageSummaryTile.summaryTileMetadataAccordion().shouldBe(visible);
    billOfMaterialsPageSummaryTile.summaryTileMetadataAccordion().click();
    billOfMaterialsPageSummaryTile.summaryTileMetadataAccordion().shouldHave(text("Show metadata\n" +
        "Author\n" +
        "NONE\n" +
        "Manufacturer\n" +
        "NONE\n" +
        "Supplier\n" +
        "NONE\n" +
        "Specification\n" +
        "CycloneDx\n" +
        "Spec Version\n" +
        "1.6\n" +
        "File Format\n" +
        "XML")).shouldBe(visible);

    sbomMetadata.setMetadataJson(metadataJson);
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    billOfMaterialsPageSummaryTile.summaryTileMetadataAccordion().click();
    billOfMaterialsPageSummaryTile.summaryTileMetadataAccordion().shouldHave(text("Show metadata\n" +
        "Author\n" +
        "John Doe\n" +
        "Manufacturer\n" +
        "John Doe\n" +
        "Supplier\n" +
        "John Doe\n" +
        "Specification\n" +
        "CycloneDx\n" +
        "Spec Version\n" +
        "1.6\n" +
        "File Format\n" +
        "XML")).shouldBe(visible);
  }

  @Test
  public void testBillOfMaterial_SbomManagerDisabledRedirectsToLearnMorePage() {
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    waitUntilUrl(LearnMoreSbomManagerPage.url());
    
    LearnMoreSbomManagerPage learnMoreSbomManagerPage = new LearnMoreSbomManagerPage();
    learnMoreSbomManagerPage.infoAlert().shouldHave(text("SBOM Manager is currently not enabled for your " +
        "organization. Learn more about SBOM Manager."));
  }

  @Test
  @Ignore
  // SBOM-1143
  public void testBillOfMaterial_ComponentsTileRendered() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    waitUntilComponentsTileSpinnerGone();

    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    componentsTile.shouldBe(visible);
    componentsTile.header().shouldHave(text("Components"));
    componentsTile.tableHeaders().shouldHave(size(6));
    componentsTile.columnHeader(0).shouldHave(
        text("TYPE"));
    componentsTile.columnHeader(1).shouldHave(
        text("NAME"));
    componentsTile.columnHeader(2).shouldHave(
        text("VULNERABILITIES"));
    componentsTile.columnHeader(3).shouldHave(
        text("VIOLATIONS"));
    componentsTile.columnHeader(4).shouldHave(
        text("PERCENTAGE ANNOTATED"));
    componentsTile.columnHeader(5).shouldHave(
        text("LICENSE"));
    ElementsCollection tableRows = componentsTile.tableBodyRows();
    tableRows.shouldHave(sizeGreaterThan(49));
    componentsTile.footer().shouldBe(visible);
    componentsTile.paginationStatus().shouldHave(visible);
  }

  @Test
  @Ignore
  // SBOM-1143
  public void testBillOfMaterial_ComponentsTileRenderedWithoutViolations() {
    insertComponentsTileSbomData();
    SystemConfigurationPropertyFeature.SBOM_POLICIES.setEnabled(false);
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    componentsTile.shouldBe(visible);
    componentsTile.header().shouldHave(text("Components"));
    componentsTile.tableHeaders().shouldHave(size(5));
    componentsTile.columnHeader(0).shouldHave(text("TYPE"));
    componentsTile.columnHeader(1).shouldHave(text("NAME"));
    componentsTile.columnHeader(2).shouldHave(text("VULNERABILITIES"));
    componentsTile.columnHeader(3).shouldHave(text("PERCENTAGE ANNOTATED"));
    componentsTile.columnHeader(4).shouldHave(text("LICENSE"));
    ElementsCollection tableRows = componentsTile.tableBodyRows();
    tableRows.shouldHave(sizeGreaterThan(49));
    componentsTile.footer().shouldBe(visible);
    componentsTile.paginationStatus().shouldHave(visible);
  }

  @Ignore
  @Test
  public void testBillOfMaterial_ComponentsTilePagination() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    ElementsCollection paginationButtons = componentsTile.paginationButtons();
    paginationButtons.get(0).shouldHave(text("1"));
    paginationButtons.get(1).shouldHave(text("2"));
    componentsTile.tableBodyRowsColumns(0).get(1).shouldHave(text("cxf-rt-transports-http-jetty : v-58"));
    componentsTile.paginationStatus().shouldHave(text("Showing 50 of 60 components"));
    paginationButtons.get(1).shouldHave(text("2")).click();
    componentsTile.tableBodyRowsColumns(0).get(1).shouldHave(text("cxf-rt-transports-http-jetty : v-25"));
    componentsTile.paginationStatus().shouldHave(text("Showing 60 of 60 components"));
  }

  @Test
  @Ignore
  // SBOM-1143
  public void testBillOfMaterial_ComponentsTileSortByType() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    waitUntilComponentsTileSpinnerGone();

    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    //default sorting is by vulnerabilities
    componentsTile.columnHeader(0).shouldHave(
        text("TYPE")).click();

    waitUntilComponentsTileSpinnerGone();

    for (int i = 0; i < 30; i++) {
      componentsTile.tableBodyRowsColumns(i).get(0).shouldHave(text("D"));
    }
    for (int i = 30; i < 50; i++) {
      componentsTile.tableBodyRowsColumns(i).get(0).shouldHave(text("T"));
    }
    componentsTile.columnHeader(0).shouldHave(
        text("TYPE")).click();

    waitUntilComponentsTileSpinnerGone();

    for (int i = 0; i < 30; i++) {
      componentsTile.tableBodyRowsColumns(i).get(0).shouldHave(text("T"));
    }
    for (int i = 30; i < 50; i++) {
      componentsTile.tableBodyRowsColumns(i).get(0).shouldHave(text("D"));
    }
  }

  @Test
  @Ignore
  // SBOM-1143
  public void testBillOfMaterial_ComponentsTileSortByVulnerabilities() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();

    waitUntilComponentsTileSpinnerGone();

    // default sorting is by vulnerabilities
    componentsTile.columnHeader(2).shouldHave(text("VULNERABILITIES")).click();

    waitUntilComponentsTileSpinnerGone();

    for (int i = 0; i < 31; i++) {
      componentsTile.vulnerabilitiesColumns(i).get(0).shouldHave(text("0"));
      componentsTile.vulnerabilitiesColumns(i).get(1).shouldHave(text("0"));
      componentsTile.vulnerabilitiesColumns(i).get(2).shouldHave(text("0"));
      componentsTile.vulnerabilitiesColumns(i).get(3).shouldHave(text("0"));
    }
    //-9 low, 8 medium, 5 high, 7 critical
    for (int i = 31; i < 49; i++) {
      if (i < 40) {
        componentsTile.vulnerabilitiesColumns(i).get(0).shouldHave(text("0"));
        componentsTile.vulnerabilitiesColumns(i).get(1).shouldHave(text("0"));
        componentsTile.vulnerabilitiesColumns(i).get(2).shouldHave(text("0"));
        componentsTile.vulnerabilitiesColumns(i).get(3).shouldHave(text("1"));
      }
      else if (i < 46) {
        componentsTile.vulnerabilitiesColumns(i).get(0).shouldHave(text("0"));
        componentsTile.vulnerabilitiesColumns(i).get(1).shouldHave(text("0"));
        componentsTile.vulnerabilitiesColumns(i).get(2).shouldHave(text("1"));
        componentsTile.vulnerabilitiesColumns(i).get(3).shouldHave(text("0"));
      }
    }
    componentsTile.paginationButtons().get(1).shouldHave(text("2")).click();
    componentsTile.vulnerabilitiesColumns(0).get(1).shouldHave(text("1"));
    componentsTile.vulnerabilitiesColumns(9).get(0).shouldHave(text("1"));
    componentsTile.paginationButtons().get(0).shouldHave(text("1")).click();

    componentsTile.columnHeader(2).shouldHave(
        text("VULNERABILITIES")).click();

    waitUntilComponentsTileSpinnerGone();

    for (int i = 29; i < 49; i++) {
      componentsTile.vulnerabilitiesColumns(i).get(0).shouldHave(text("0"));
      componentsTile.vulnerabilitiesColumns(i).get(1).shouldHave(text("0"));
      componentsTile.vulnerabilitiesColumns(i).get(2).shouldHave(text("0"));
      componentsTile.vulnerabilitiesColumns(i).get(3).shouldHave(text("0"));
    }
    componentsTile.vulnerabilitiesColumns(0).get(0).shouldHave(text("1"));
  }

  @Test
  @Ignore
  // SBOM-1143
  public void testBillOfMaterial_ComponentsTileSortByPercentageAnnotated() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    waitUntilComponentsTileSpinnerGone();

    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    componentsTile.columnHeader(4).shouldHave(
        text("PERCENTAGE ANNOTATED")).click();

    waitUntilComponentsTileSpinnerGone();

    verifySortOrder(true, componentsTile);
    componentsTile.columnHeader(4).shouldHave(
        text("PERCENTAGE ANNOTATED")).click();

    waitUntilComponentsTileSpinnerGone();

    verifySortOrder(false, componentsTile);
  }

  @Test
  @Ignore
  // SBOM-1143
  public void testBillOfMaterial_ComponentsTileFilterBy() {
    insertComponentsTileSbomData();
    for (int i = 0; i < 3; i++) {
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates(
          "cxf-rt-transports-http-jetty", "v-t" + i);
      PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);

      tempEntity.newThirdPartyFileCoordinate(
          sbomMetadata.getThirdPartyFileId(), "s", "SPDX", "n", "v1", "h1",
          packageUrlIdentifier.getPackageUrl(), ThirdPartyDependencyType.TRANSITIVE);
    }
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    waitUntilComponentsTileSpinnerGone();

    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    componentsTile.filterByButton().click();

    sbomManagerBillOfMaterialsPage.filterDialog().shouldBe(visible);
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(0).shouldHave(text("Critical"));
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(1).shouldHave(text("High"));
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(2).shouldHave(text("Medium"));
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(3).shouldHave(text("Low"));
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(0).shouldHave(text("Direct"));
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(1).shouldHave(text("Transitive"));
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(2).shouldHave(text("Unspecified"));
    //-9 low, 8 medium, 5 high, 7 critical
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(0).click();
    waitUntilComponentsTileSpinnerGone();

    componentsTile.tableBodyRows().shouldHave(size(7));
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(0).click();
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(1).click();
    waitUntilComponentsTileSpinnerGone();

    componentsTile.tableBodyRows().shouldHave(size(5));
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(1).click();
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(2).click();
    waitUntilComponentsTileSpinnerGone();

    componentsTile.tableBodyRows().shouldHave(size(8));
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(2).click();
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(3).click();
    waitUntilComponentsTileSpinnerGone();

    componentsTile.tableBodyRows().shouldHave(size(9));
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(3).click();
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(0).click();
    waitUntilComponentsTileSpinnerGone();

    componentsTile.tableBodyRows().shouldHave(size(30));
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(0).click();
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(1).click();
    waitUntilComponentsTileSpinnerGone();

    componentsTile.tableBodyRows().shouldHave(size(33));
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(1).click();
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(2).click();
    waitUntilComponentsTileSpinnerGone();

    componentsTile.tableBodyRows().get(0).shouldHave(text("No components found"));
  }

  @Test
  public void testBillOfMaterial_ComponentsTilePolicyViolation() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    componentsTile.shouldBe(visible);
    componentsTile.header().shouldHave(text("Components"));
    componentsTile.tableHeaders().shouldHave(size(6));
    componentsTile.columnHeader(0).shouldHave(
            text("TYPE"));
    componentsTile.columnHeader(1).shouldHave(
            text("NAME"));
    componentsTile.columnHeader(2).shouldHave(
            text("VULNERABILITIES"));
    componentsTile.columnHeader(3).shouldHave(
            text("VIOLATIONS"));
    componentsTile.columnHeader(4).shouldHave(
            text("PERCENTAGE ANNOTATED"));
    componentsTile.columnHeader(5).shouldHave(
            text("LICENSE"));
  }

  @Test
  public void testBillOfMaterial_InvalidSbomWarnings_hiddenWhenValid() {
    insertComponentsTileSbomData();

    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    var page = new SbomManagerBillOfMaterialsPage();
    page.invalidSbomAlert().shouldNotBe(visible);
    page.invalidSbomIndicator().shouldNotBe(visible);
  }

  @Test
  public void testBillOfMaterial_InvalidSbomWarnings() {
    insertInvalidSbomData();

    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    var page = new SbomManagerBillOfMaterialsPage();

    page.invalidSbomIndicator().shouldNotBe(visible);

    page.invalidSbomAlert().shouldBe(visible);
    page.invalidSbomAlertCloseBtn().shouldBe(visible).click();

    page.invalidSbomAlert().shouldNotBe(visible);
    page.invalidSbomIndicator().shouldBe(visible);
  }

  private void insertComponentsTileSbomData() {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(scannedFile);
    Path zippedBom = null;
    try {
      zippedBom = mockOriginalSbom(SbomManagerBillOfMaterialsPageTest.class, "simple-bom.xml",
          insightWork.getSbomDir(application.getId()).toPath());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    sbomMetadata = tempEntity.newThirdPartySbomMetadata(
        thirdPartyScan.getThirdPartyFileId(),
        application.getId(),
        "t-version",
        ACTIVE,
        zippedBom.getFileName().toString(),
        SbomSpecification.CYCLONEDX.toString(),
        SbomFormat.XML.name(),
        "1.6"
    );
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    for (int i = 0; i < 60; i++) {
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates(
          "cxf-rt-transports-http-jetty", "v-" + i);
      PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
      if (i % 2 != 0) {
        tempEntity.newThirdPartyFileCoordinate(
            thirdPartyScan.getThirdPartyFileId(), "s", "SPDX", "n", "v1", "h1",
            packageUrlIdentifier.getPackageUrl(), ThirdPartyDependencyType.DIRECT);
      }
      //insert some vex -9 low, 8 medium, 5 high, 3 critical
      else {
        ThirdPartyFileCoordinate thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(
            thirdPartyScan.getThirdPartyFileId(), "s", "SPDX", "n", "v1", "h1",
            packageUrlIdentifier.getPackageUrl(), ThirdPartyDependencyType.TRANSITIVE);
        double severity = 0.1 * i * 2;
        ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity(
            thirdPartyFileCoordinate, "r1", "d1", "l1", severity <= 10 ? severity : 10, "sd1",
            "f1");
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity, coordinateSecurity.getRefId(),
            "state", "justification", "response", "detail");
      }
    }
    refreshOrOpen(IndexPage.url());
  }

  private void insertInvalidSbomData() {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(scannedFile);
    Path zippedBom = null;
    try {
      zippedBom = mockOriginalSbom(SbomManagerBillOfMaterialsPageTest.class, "simple-bom.xml",
          insightWork.getSbomDir(application.getId()).toPath());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    sbomMetadata = tempEntity.newThirdPartySbomMetadata(
        thirdPartyScan.getThirdPartyFileId(),
        application.getId(),
        "t-version",
        ACTIVE,
        zippedBom.getFileName().toString(),
        SbomSpecification.CYCLONEDX.toString(),
        SbomFormat.XML.name(),
        "1.6",
        new Date(0),
        false
    );
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    refreshOrOpen(IndexPage.url());
  }

  private void verifySortOrder(boolean ascending, ComponentsTile componentsTile) {
    String ascendingExpected = ascending ? "0%" : "100%";
    String descendingExpected = ascending ? "100%" : "0%";
    for (int i = 0; i < 50; i++) {
      if (i < 30) {
        componentsTile.percentageAnnotatedColumn(i).shouldHave(text(ascendingExpected));
      }
      else {
        componentsTile.percentageAnnotatedColumn(i).shouldHave(text(descendingExpected));
      }
    }
  }

  @Test
  public void testBillOfMaterial_ExportSbomButton() throws Exception {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    sbomManagerBillOfMaterialsPage.exportButton().shouldHave(visible);
    sbomManagerBillOfMaterialsPage.exportButton().shouldHave(text("Export SBOM")).click();
    NxSubmitMask.seeAndWaitForDismissal();
    File downloadedSbom = sbomManagerBillOfMaterialsPage.exportButton().shouldHave(text("Export SBOM"))
        .download(3000L);
    byte[] fileBeginning = new byte[5];
    try (FileInputStream stream = new FileInputStream(downloadedSbom)) {
      stream.read(fileBeginning);
    }

    assertThat(new String(fileBeginning)).isEqualTo("<?xml");
  }

  @Test
  public void testBillOfMaterial_ExportOriginalSbomOption() throws Exception {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    sbomManagerBillOfMaterialsPage.exportButton().shouldHave(visible);
    sbomManagerBillOfMaterialsPage.exportButtonMenu().click();
    sbomManagerBillOfMaterialsPage.exportButtonMenuItems().shouldBe(size(3));
    File downloadedSbom = sbomManagerBillOfMaterialsPage.exportButtonMenuItems().get(0)
        .shouldHave(text("Export Original SBOM"))
        .download();
    byte[] fileBeginning = new byte[5];
    try (FileInputStream stream = new FileInputStream(downloadedSbom)) {
      stream.read(fileBeginning);
    }

    assertThat(new String(fileBeginning)).isEqualTo("<?xml");
  }

  @Test
  public void testBillOfMaterial_ExportModal_RendersCorrectly() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    sbomManagerBillOfMaterialsPage.exportButton().shouldHave(visible);
    sbomManagerBillOfMaterialsPage.exportButtonMenu().click();
    sbomManagerBillOfMaterialsPage.exportButtonMenuItems().get(1).shouldHave(text("Additional Export Options")).click();

    sbomManagerBillOfMaterialsPage.additionalExportOptionsModal().shouldBe(visible);
    sbomManagerBillOfMaterialsPage.sbomModalOptions().shouldBe(size(2));
    sbomManagerBillOfMaterialsPage.sbomSpecificationOptions().shouldHave(size(2))
        .get(0).shouldHave(text("CycloneDX"));
    sbomManagerBillOfMaterialsPage.sbomSpecificationOptions().shouldHave(size(2))
        .get(1).shouldHave(text("SPDX"));
    sbomManagerBillOfMaterialsPage.sbomsFormatOptions().shouldHave(size(2))
        .get(0).shouldHave(text("JSON"));
    sbomManagerBillOfMaterialsPage.sbomsFormatOptions().shouldHave(size(2))
        .get(1).shouldHave(text("XML"));
    sbomManagerBillOfMaterialsPage.exportSbomButtonModal().shouldBe(visible);
    sbomManagerBillOfMaterialsPage.cancelButtonModal().shouldBe(visible);

    SelenideElement spdxRadioButton =  sbomManagerBillOfMaterialsPage.sbomSpecificationOptions().get(1);
    spdxRadioButton.click();
    spdxRadioButton.shouldHave(cssClass("tm-checked"));
    SelenideElement cycloneDxRadioButton =  sbomManagerBillOfMaterialsPage.sbomSpecificationOptions().get(0);
    cycloneDxRadioButton.click();
    spdxRadioButton.shouldHave(cssClass("tm-unchecked"));
    cycloneDxRadioButton.shouldHave(cssClass("tm-checked"));

    SelenideElement jsonRadioButton =  sbomManagerBillOfMaterialsPage.sbomsFormatOptions().get(0);
    jsonRadioButton.click();
    jsonRadioButton.shouldHave(cssClass("tm-checked"));
    SelenideElement xmlRadioButton =  sbomManagerBillOfMaterialsPage.sbomsFormatOptions().get(1);
    xmlRadioButton.click();
    jsonRadioButton.shouldHave(cssClass("tm-unchecked"));
    xmlRadioButton.shouldHave(cssClass("tm-checked"));
  }

  @Test
  public void testBillOfMaterial_ExportModal_DownloadOptions() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    sbomManagerBillOfMaterialsPage.exportButtonMenu().click();
    sbomManagerBillOfMaterialsPage.exportButtonMenuItems().get(1).shouldHave(text("Additional Export Options")).click();
    sbomManagerBillOfMaterialsPage.additionalExportOptionsModal().shouldBe(visible);

    SelenideElement jsonRadioButton =  sbomManagerBillOfMaterialsPage.sbomsFormatOptions().get(0);
    jsonRadioButton.click();
    jsonRadioButton.shouldHave(cssClass("tm-checked"));

    File downloadedSbom = sbomManagerBillOfMaterialsPage.exportSbomButtonModal().shouldHave(text("Export SBOM"))
        .download(3000L);
    assertThat(downloadedSbom.getName()).endsWith(".json");

    sbomManagerBillOfMaterialsPage.exportButtonMenu().click();
    sbomManagerBillOfMaterialsPage.exportButtonMenuItems().get(1).shouldHave(text("Additional Export Options")).click();
    sbomManagerBillOfMaterialsPage.additionalExportOptionsModal().shouldBe(visible);

    SelenideElement xmlRadioButton =  sbomManagerBillOfMaterialsPage.sbomsFormatOptions().get(1);
    xmlRadioButton.click();
    xmlRadioButton.shouldHave(cssClass("tm-checked"));
    downloadedSbom = sbomManagerBillOfMaterialsPage.exportSbomButtonModal().shouldHave(text("Export SBOM"))
        .download(3000L);
    assertThat(downloadedSbom.getName()).endsWith(".xml");
  }

  @Test
  public void testBillOfMaterial_ExportOptions_ValidSbom() throws Exception {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    sbomManagerBillOfMaterialsPage.exportButton().shouldBe(enabled).shouldHave(text("Export SBOM"));
    sbomManagerBillOfMaterialsPage.exportButtonMenu().click();
    sbomManagerBillOfMaterialsPage.exportButtonMenuItems().get(0).shouldBe(enabled)
        .shouldHave(text("Export Original SBOM"));
    sbomManagerBillOfMaterialsPage.exportButtonMenuItems().get(1).shouldBe(enabled)
        .shouldHave(text("Additional Export Options"));
    sbomManagerBillOfMaterialsPage.exportButtonMenuItems().get(2).shouldNotHave(cssClass("disabled"))
        .shouldHave(text("Export PDF"));

    String cdxExportVersion = getCDXExportVersion(Files.readString(sbomManagerBillOfMaterialsPage.exportButton()
        .download(3000L)
        .toPath()));

    sbomManagerBillOfMaterialsPage.exportButtonMenu().click();
    String cdxOriginalExportVersion =
        getCDXExportVersion(Files.readString(sbomManagerBillOfMaterialsPage.exportButtonMenuItems().get(0)
            .download(3000L)
            .toPath()));
    assertThat(cdxOriginalExportVersion).isEqualTo("1.4");
    
    GenericVersionScheme scheme = new GenericVersionScheme();
    Version parsedCdxExportVersion = scheme.parseVersion(cdxExportVersion);
    Version parsedCdxOriginalExportVersion = scheme.parseVersion(cdxOriginalExportVersion);
    assertThat(parsedCdxExportVersion.compareTo(parsedCdxOriginalExportVersion)).isGreaterThan(0);
  }

  @Test
  public void testBillOfMaterial_ExportOptions_InvalidSbom() throws Exception {
    insertInvalidSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    sbomManagerBillOfMaterialsPage.exportButton().shouldBe(enabled).shouldHave(text("Export Original SBOM"));
    sbomManagerBillOfMaterialsPage.exportButtonMenu().click();
    sbomManagerBillOfMaterialsPage.exportButtonMenuItems().get(0).shouldBe(disabled)
        .shouldHave(text("Export SBOM"));
    sbomManagerBillOfMaterialsPage.exportButtonMenuItems().get(1).shouldBe(disabled)
        .shouldHave(text("Additional Export Options"));
    sbomManagerBillOfMaterialsPage.exportButtonMenuItems().get(2).shouldHave(cssClass("disabled"))
        .shouldHave(text("Export PDF"));

    String cdxOriginalExportVersion = getCDXExportVersion(Files.readString(sbomManagerBillOfMaterialsPage.exportButton()
        .download(3000L)
        .toPath()));
    assertThat(cdxOriginalExportVersion).isEqualTo("1.4");
  }

  private String getCDXExportVersion(String content) {
    Pattern pattern = Pattern.compile("http://cyclonedx.org/schema/bom/([\\d.]+)");
    Matcher matcher = pattern.matcher(content);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  @Test
  public void testBillOfMaterial_ComponentSearch() {
    insertComponentsTileSbomDataForSearchName();
    refreshOrOpen(IndexPage.url());
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    ElementsCollection tableRows = componentsTile.tableBodyRows();
    componentsTile.inputComponentSearch().shouldBe(visible);
    componentsTile.inputComponentSearch().setValue("insight-scanner-manifest-model");
    tableRows.shouldHave(size(1));
    componentsTile.nameColum(0).shouldHave(text("insight-scanner-manifest-model"));
    componentsTile.inputComponentSearch().setValue("");
    componentsTile.inputComponentSearch().setValue("jackson");
    tableRows.shouldHave(size(2));
    componentsTile.nameColum(0).shouldHave(text("nexus-rest-jackson2"));
    componentsTile.nameColum(1).shouldHave(text("jackson-annotations"));
    componentsTile.inputComponentSearch().setValue("insight");
    tableRows.shouldHave(size(3));
    componentsTile.nameColum(0).shouldHave(text("insight-scanner-tools"));
    componentsTile.nameColum(1).shouldHave(text("insight-scanner-manifest-model"));
    componentsTile.nameColum(2).shouldHave(text("insight-archive-utils"));
    componentsTile.inputComponentSearch().setValue("org.apache.");
    tableRows.shouldHave(size(2));
    componentsTile.nameColum(0).shouldHave(text("org.apache.karaf"));
    componentsTile.nameColum(1).shouldHave(text("org.apache.felix.converter"));
    componentsTile.inputComponentSearch().setValue("geronimo-jpa_2.2_");
    tableRows.shouldHave(size(1));
    componentsTile.nameColum(0).shouldHave(text("geronimo-jpa_2.2_spec"));
  }

  private void insertComponentsTileSbomDataForSearchName() {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(scannedFile);
    Path zippedBom = null;
    try {
      zippedBom = mockOriginalSbom(SbomManagerBillOfMaterialsPageTest.class, "simple-bom.xml",
          insightWork.getSbomDir(application.getId()).toPath());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    sbomMetadata = tempEntity.newThirdPartySbomMetadata(
        thirdPartyScan.getThirdPartyFileId(),
        application.getId(),
        "t-version",
        ACTIVE,
        zippedBom.getFileName().toString(),
        SbomSpecification.CYCLONEDX.toString(),
        SbomFormat.XML.name(),
        "1.6"
    );
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    List<String> componentNames = List.of("insight-scanner-manifest-model", "jackson-annotations",
        "nexus-rest-jackson2", "insight-archive-utils", "org.apache.felix.converter", "org.apache.karaf",
        "jss-plugin-global", "insight-scanner-tools", "glibc/libc6", "geronimo-jpa_2.2_spec");
    for (int i = 0; i < 10; i++) {
      ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates(
          componentNames.get(i), "v-" + i);
      PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
      tempEntity.newThirdPartyFileCoordinate(
          thirdPartyScan.getThirdPartyFileId(), "s", "SPDX", componentNames.get(i), "v1", "h1",
          packageUrlIdentifier.getPackageUrl(), ThirdPartyDependencyType.DIRECT);
    }
  }

  private void waitUntilComponentsTileSpinnerGone() {
    new FluentWait<>(getWebDriver())
        .withTimeout(Duration.ofSeconds(240))
        .pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class)
        .until(ExpectedConditions.visibilityOf(SbomManagerBillOfMaterialsPage.componentsTile().getLoadingSpinner()));

    new FluentWait<>(getWebDriver())
        .withTimeout(Duration.ofSeconds(240))
        .pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class)
        .until(ExpectedConditions.invisibilityOf(SbomManagerBillOfMaterialsPage.componentsTile().getLoadingSpinner()));
  }
}
