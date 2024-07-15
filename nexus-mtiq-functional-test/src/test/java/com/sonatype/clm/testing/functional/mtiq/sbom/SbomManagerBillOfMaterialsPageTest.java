/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.sbom;

import java.util.Date;

import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.mtiq.pages.sbom.BillOfMaterialsPageSummaryTile;
import com.sonatype.clm.testing.functional.mtiq.pages.sbom.SbomManagerBillOfMaterialsPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.scan.file.SbomFormat;

import org.junit.Before;
import org.junit.Test;

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

  @Test
  public void testBillOfMaterial_ComponentSummaryChart() {
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(scannedFile.getId(),
            "s", "SPDX", "n1", "v1", "h1", "u1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r1", "d1", "l1", 5.5, "sd1", "f1");
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
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

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
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

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
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

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
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
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    refreshOrOpen(SbomManagerBillOfMaterialsPage.url(application.getPublicId(), sbomMetadata.getSbomVersion()));

    sbomManagerBillOfMaterialsPage.title().shouldNotBe(visible);
    sbomManagerBillOfMaterialsPage.errorAlert().shouldBe(visible);
  }
}
