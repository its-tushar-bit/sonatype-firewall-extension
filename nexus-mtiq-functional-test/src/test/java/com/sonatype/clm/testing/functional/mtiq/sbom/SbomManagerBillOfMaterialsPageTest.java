/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.sbom;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.mtiq.pages.sbom.BillOfMaterialsPageSummaryTile;
import com.sonatype.clm.testing.functional.mtiq.elements.sbom.ComponentsTile;
import com.sonatype.clm.testing.functional.mtiq.pages.sbom.SbomManagerBillOfMaterialsPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;

import com.codeborne.selenide.ElementsCollection;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class SbomManagerBillOfMaterialsPageTest
    extends AbstractMtiqFunctionalTest
{
  private final SbomManagerBillOfMaterialsPage sbomManagerBillOfMaterialsPage = new SbomManagerBillOfMaterialsPage();

  private final BillOfMaterialsPageSummaryTile billOfMaterialsPageSummaryTile = new BillOfMaterialsPageSummaryTile();

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private Organization organization;

  private Application application;

  private ThirdPartySbomMetadata sbomMetadata;

  private ThirdPartyFile scannedFile;

  @Before
  public void init() {
    thirdPartySbomMetadataDAO = lookup(ThirdPartySbomMetadataDAO.class);
    organization = tempEntity.newOrganization("test-organization");
    application = tempEntity.newApplication("Test Application", "test-application", organization.getId());

    scannedFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(scannedFile);
    sbomMetadata = tempEntity.newThirdPartySbomMetadata(
      scannedFile.getId(),
      application.getId(),
      "test-version",
      "ACTIVE",
      scannedFile.getFilename(),
      SbomSpecification.CYCLONEDX.name(),
      SbomFormat.XML.name(),
      "0.0"
    );
  }

  private void setLicenseAndLogin() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
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
    setLicenseAndLogin();
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
    setLicenseAndLogin();
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
  public void testBillOfMaterial_AnnotatedVulnerabilitiesSummaryDescription() {
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(scannedFile.getId(),
            "s", "SPDX", "n1", "v1", "h1", "u1");
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    setLicenseAndLogin();
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
    setLicenseAndLogin();
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
        "CYCLONEDX\n" +
        "Spec Version\n" +
        "0.0\n" +
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
        "CYCLONEDX\n" +
        "Spec Version\n" +
        "0.0\n" +
        "File Format\n" +
        "XML")).shouldBe(visible);
  }

  @Test
  public void testBillOfMaterial_SbomManagerDisabled() {
    setLicenseAndLogin();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    sbomManagerBillOfMaterialsPage.title().shouldNotBe(visible);
    sbomManagerBillOfMaterialsPage.errorAlert().shouldBe(visible);
  }

  @Test
  public void testBillOfMaterial_ComponentsTileRendered() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    componentsTile.shouldBe(visible);
    componentsTile.header().shouldHave(text("Components"));
    componentsTile.tableHeaders().shouldHave(size(5));
    componentsTile.columnHeader(0).shouldHave(
        text("TYPE"));
    componentsTile.columnHeader(1).shouldHave(
        text("NAME"));
    componentsTile.columnHeader(2).shouldHave(
        text("VULNERABILITIES"));
    componentsTile.columnHeader(3).shouldHave(
        text("PERCENTAGE ANNOTATED"));
    componentsTile.columnHeader(4).shouldHave(
        text("LICENSE"));
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
  public void testBillOfMaterial_ComponentsTileSortByType() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    //default sorting is by vulnerabilities
    componentsTile.columnHeader(0).shouldHave(
        text("TYPE")).click();
    for (int i = 0; i < 30; i++) {
      componentsTile.tableBodyRowsColumns(i).get(0).shouldHave(text("D"));
    }
    for (int i = 30; i < 50; i++) {
      componentsTile.tableBodyRowsColumns(i).get(0).shouldHave(text("T"));
    }
    componentsTile.columnHeader(0).shouldHave(
        text("TYPE")).click();
    for (int i = 0; i < 30; i++) {
      componentsTile.tableBodyRowsColumns(i).get(0).shouldHave(text("T"));
    }
    for (int i = 30; i < 50; i++) {
      componentsTile.tableBodyRowsColumns(i).get(0).shouldHave(text("D"));
    }
  }

  @Test
  public void testBillOfMaterial_ComponentsTileSortByVulnerabilities() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    //default sorting is by vulnerabilities
    componentsTile.columnHeader(2).shouldHave(
        text("VULNERABILITIES")).click();
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

    for (int i = 29; i < 49; i++) {
      componentsTile.vulnerabilitiesColumns(i).get(0).shouldHave(text("0"));
      componentsTile.vulnerabilitiesColumns(i).get(1).shouldHave(text("0"));
      componentsTile.vulnerabilitiesColumns(i).get(2).shouldHave(text("0"));
      componentsTile.vulnerabilitiesColumns(i).get(3).shouldHave(text("0"));
    }
    componentsTile.vulnerabilitiesColumns(0).get(0).shouldHave(text("1"));
  }

  @Test
  public void testBillOfMaterial_ComponentsTileSortByPercentageAnnotated() {
    insertComponentsTileSbomData();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    ComponentsTile componentsTile = SbomManagerBillOfMaterialsPage.componentsTile();
    componentsTile.columnHeader(3).shouldHave(
        text("PERCENTAGE ANNOTATED")).click();
    verifySortOrder(true, componentsTile);
    componentsTile.columnHeader(3).shouldHave(
        text("PERCENTAGE ANNOTATED")).click();
    verifySortOrder(false, componentsTile);
  }

  @Test
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
    componentsTile.tableBodyRows().shouldHave(size(7));
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(0).click();
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(1).click();
    componentsTile.tableBodyRows().shouldHave(size(5));
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(1).click();
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(2).click();
    componentsTile.tableBodyRows().shouldHave(size(8));
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(2).click();
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(3).click();
    componentsTile.tableBodyRows().shouldHave(size(9));
    sbomManagerBillOfMaterialsPage.vulnerabilityThreatLevelFilterCheckboxes().get(3).click();

    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(0).click();
    componentsTile.tableBodyRows().shouldHave(size(30));
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(0).click();
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(1).click();
    componentsTile.tableBodyRows().shouldHave(size(33));
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(1).click();
    sbomManagerBillOfMaterialsPage.dependencyTypeFilterChecboxes().get(2).click();
    componentsTile.tableBodyRows().get(0).shouldHave(text("No components found"));
  }

  private void insertComponentsTileSbomData() {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(scannedFile);
    sbomMetadata = tempEntity.newThirdPartySbomMetadata(
        thirdPartyScan.getThirdPartyFileId(),
        application.getId(),
        "t-version",
        "ACTIVE",
        scannedFile.getFilename(),
        SbomSpecification.CYCLONEDX.name(),
        SbomFormat.XML.name(),
        "0.0"
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
    setLicenseAndLogin();
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
}
