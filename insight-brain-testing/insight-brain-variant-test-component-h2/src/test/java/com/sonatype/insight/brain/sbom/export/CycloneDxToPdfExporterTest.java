/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentPolicyViolationsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.report.pdf.PdfData;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification.CYCLONEDX_15;
import static com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification.CYCLONEDX_16;
import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class CycloneDxToPdfExporterTest
    extends AbstractPdfExporterH2Test
{
  private CycloneDxToPdfExporter exporter;

  @Override
  @BeforeEach
  public void init() throws SbomExportException {
    super.init();
    exporter = new CycloneDxToPdfExporter(
        multiLicenseDAO,
        thirdPartyFileDAO,
        thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO,
        thirdPartyScanDAO,
        applicationDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO,
        migrationTrackerDAO,
        baseUrl,
        idUtils,
        versionService,
        apiReportDataServiceV2,
        licenseResolutionService,
        buildThirdPartyPersistenceService());
  }

  @Test
  public void testExportPdf_withMergedVulnerabilitiesLicenses() throws Exception {
    // Given
    File testBomFile = mockOriginalSbomFile("test-1-bom.xml");
    ThirdPartySbomMetadata sbomMetadata =
        createMetadataEntity(testBomFile.getName(), app.getId(), SBOM_VERSION, CYCLONEDX_15, SbomFormat.XML);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.COMPLIANCE.getId(), SCAN_ID, new Date());
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ReportHelper.saveMockReport(
        insightWork,
        tempDir,
        "/CycloneDxToPdfExporterTest/report-for-test-1",
        app.getId(),
        SCAN_ID);
    setupTestComponents();

    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    rawData.components = new ArrayList<>();
    rawData.components.add(setupReportRawDataLTG("pkg:maven/log4j/log4j@1.2.8?type=jar", 5));
    rawData.components.add(
        setupReportRawDataLTG("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar", 10));

    // When
    SbomExportParams exportParams = withExportParams(sbomMetadata, CYCLONEDX_15, SbomFormat.JSON);
    exportParams.withReportRawData(rawData);
    exporter.setExportParams(exportParams);
    PdfData pdfData = exporter.exportPdf();
    assertPdfData(pdfData, CYCLONEDX_15, 2);
  }

  @Test
  public void testExportPdf_withDuplicateComponentsInOriginalSbom() throws Exception {
    // Given
    File testBomFile = mockOriginalSbomFile("duplicate-components-bom.json");
    ThirdPartySbomMetadata sbomMetadata =
        createMetadataEntity(testBomFile.getName(), app.getId(), SBOM_VERSION, CYCLONEDX_16, SbomFormat.JSON);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.COMPLIANCE.getId(), SCAN_ID, new Date());
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ReportHelper.saveMockReport(
        insightWork,
        tempDir,
        "/CycloneDxToPdfExporterTest/report-for-duplicate-components",
        app.getId(),
        SCAN_ID);
    setupTestComponentsForDuplicateComponentsTest();

    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    rawData.components = new ArrayList<>();
    rawData.components.add(setupReportRawDataLTG("pkg:nuget/Microsoft.Extensions.ApiDescription.Server@3.0.0", 5));
    rawData.components.add(
        setupReportRawDataLTG("pkg:nuget/Microsoft.Extensions.ApiDescription.Server@3.0.0", 10));

    // When
    SbomExportParams exportParams = withExportParams(sbomMetadata, CYCLONEDX_16, SbomFormat.JSON);
    exportParams.withReportRawData(rawData);
    exporter.setExportParams(exportParams);
    PdfData pdfData = exporter.exportPdf();

    assertThat(pdfData.title).isEqualTo(app.getName() + REPORT_NAME);
    assertThat(pdfData.createdDate).isNotNull();
    assertThat(pdfData.analyzedDate).isNotNull();
    assertThat(pdfData.productVersion).isNotNull();
    assertThat(pdfData.components).hasSize(1);
    assertThat(pdfData.sbomMetadata.author).hasSize(1).contains("John Doe");
    assertThat(pdfData.sbomMetadata.specification).isEqualTo(CYCLONEDX_16.getSpecification().name());
    assertThat(pdfData.sbomMetadata.specVersion).isEqualTo(CYCLONEDX_16.getVersion());
    assertThat(pdfData.sbomMetadata.fileFormat).isEqualTo("json");
    assertThat(pdfData.sbomMetadata.createdAt).isNotNull();
    assertThat(pdfData.sbomMetadata.scanId).isEqualTo("sid1");

    PdfComponent c1 = pdfData.components.stream()
        .filter(c -> c.displayName.contains("Microsoft.Extensions.ApiDescription.Server 3.0.0"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("expected component not found in PDF output"));
    assertThat(c1.displayName).isEqualTo("Microsoft.Extensions.ApiDescription.Server 3.0.0");
    assertThat(c1.matchState).isEqualTo("exact");
    assertThat(c1.policyViolations).hasSize(4);
    assertThat(c1.policyViolations).filteredOn(v -> v.policyName.equals("Security-High")).hasSize(3);
    assertThat(c1.policyViolations).filteredOn(v -> v.policyName.equals("Architecture-Quality")).hasSize(1);
    assertThat(c1.effectiveLicenses).hasSize(2);
    assertThat(c1.effectiveLicenses.stream().map(c -> c.name)).containsExactlyInAnyOrder("MIT License", "Apache-2.0");
    assertThat(c1.securityIssues).hasSize(3);
    assertThat(c1.securityIssues.stream().map(s -> s.reference)).containsExactlyInAnyOrder("sonatype-2021-0713",
        "sonatype-2022-5998", "CVE-2024-21907");

    // LTGs
    assertThat(c1.effectiveLicenseThreats).hasSize(1);
    assertThat(c1.effectiveLicenseThreats.get(0).licenseThreatGroupLevel).isEqualTo(10);
  }

  @Test
  public void testExportPdf_withMissingReportData() throws Exception {
    // Given
    File testBomFile = mockOriginalSbomFile("test-1-bom.xml");
    ThirdPartySbomMetadata sbomMetadata =
        createMetadataEntity(testBomFile.getName(), app.getId(), SBOM_VERSION, CYCLONEDX_15, SbomFormat.XML);
    exporter.setExportParams(withExportParams(sbomMetadata, CYCLONEDX_15, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fc1 = setupFileCoordinateEntity("log4j", "1.2.8", "3640dd71069d7986c9a1",
        "pkg:maven/log4j/log4j@1.2.8?type=jar", "pkg:maven/log4j/log4j@1.2.8?type=jar:3640dd71069d7986c9a1");
    setupFileCoordinateEntity("jackson-databind", "2.9.9", "43482bee60d253ab70b6",
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar",
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar");
    setupCoordinateSecurityEntity(fc1, "CVE-2022-23307", "name=CVE-2022-23307", "HIGH", "502", "CVSSV3",
        "NVD", 8.8d, null);

    // When
    PdfData pdfData = exporter.exportPdf();

    // Then
    assertThat(pdfData.title).isEqualTo(app.getName() + REPORT_NAME);
    assertThat(pdfData.createdDate).isNotNull();
    assertThat(pdfData.analyzedDate).isNotNull();
    assertThat(pdfData.productVersion).isNotNull();
    assertThat(pdfData.components).hasSize(2);
    assertThat(pdfData.sbomMetadata.author).hasSize(1).contains("John Doe");
    assertThat(pdfData.sbomMetadata.specification).isEqualTo("CYCLONEDX");
    assertThat(pdfData.sbomMetadata.specVersion).isEqualTo("1.5");
    assertThat(pdfData.sbomMetadata.fileFormat).isEqualTo("xml");

    PdfComponent c1 = pdfData.components.stream()
        .filter(c -> c.displayName.contains("log4j"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("expected component not found in PDF output"));
    assertThat(c1.displayName).isEqualTo("log4j : log4j : 1.2.8");
    assertThat(c1.matchState).isEqualTo("exact");
    assertThat(c1.policyViolations).hasSize(0);
    assertThat(c1.effectiveLicenses).hasSize(1);
    assertThat(c1.effectiveLicenses.stream().map(c -> c.name)).containsExactlyInAnyOrder("MPL-2.0");
    assertThat(c1.securityIssues).hasSize(1);
    assertThat(c1.securityIssues.stream().map(s -> s.reference)).contains("CVE-2022-23307");

    PdfComponent c2 =
        pdfData.components.stream()
            .filter(c -> c.displayName.contains("jackson-databind"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("expected component not found in PDF output"));
    assertThat(c2.displayName).isEqualTo("com.fasterxml.jackson.core : jackson-databind : 2.9.9");
    assertThat(c2.matchState).isEqualTo("exact");
    assertThat(c2.policyViolations).hasSize(0);
    assertThat(c2.effectiveLicenses).hasSize(1);
    assertThat(c2.effectiveLicenses.stream().map(c -> c.name)).contains("Apache-2.0");
    assertThat(c2.securityIssues).hasSize(0);
  }

  @Test
  public void testExportPdf_withOverriddenLicenses_forLifeCycleProduct() throws Exception {
    // Given
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    testExportPdf_withOverriddenLicenses("MIT", "Aladdin");
  }

  @Test
  public void testExportPdf_withOverriddenLicenses_forSbomAndALPProduct() throws Exception {
    // Given
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK);
    testExportPdf_withOverriddenLicenses("MIT", "Aladdin");
  }

  @Test
  public void testExportPdf_withOverriddenLicenses_forSbomProduct() throws Exception {
    // Given
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    testExportPdf_withOverriddenLicenses("MPL-2.0", "Apache-2.0");
  }

  private void testExportPdf_withOverriddenLicenses(final String expected1, final String expected2) throws Exception {
    File testBomFile = mockOriginalSbomFile("test-1-bom.xml");
    String purl1 = "pkg:maven/log4j/log4j@1.2.8?type=jar";
    String purl2 = "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar";
    ThirdPartySbomMetadata sbomMetadata =
        createMetadataEntity(testBomFile.getName(), app.getId(), SBOM_VERSION, CYCLONEDX_15, SbomFormat.XML);
    exporter.setExportParams(withExportParams(sbomMetadata, CYCLONEDX_15, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    setupFileCoordinateEntity("log4j", "1.2.8", "3640dd71069d7986c9a1",
        purl1, "pkg:maven/log4j/log4j@1.2.8?type=jar:3640dd71069d7986c9a1");
    setupFileCoordinateEntity("jackson-databind", "2.9.9", "43482bee60d253ab70b6",
        purl2, "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar");
    ComponentIdentifier id1 = new PackageUrlIdentifier(purl1).toComponentIdentifier();
    id1.ensureComplete();
    tempEntity.newLicenseOverride(app.getId(), id1, LicenseOverrideStatus.OVERRIDDEN, "MIT");
    ComponentIdentifier id2 = new PackageUrlIdentifier(purl2).toComponentIdentifier();
    id2.ensureComplete();
    tempEntity.newLicenseOverride(app.getId(), id2, LicenseOverrideStatus.SELECTED, "Aladdin");
    // When
    PdfData pdfData = exporter.exportPdf();

    // Then
    assertThat(pdfData.title).isEqualTo(app.getName() + REPORT_NAME);

    PdfComponent c1 = pdfData.components.stream()
        .filter(c -> c.displayName.contains("log4j"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("expected component not found in PDF output"));
    assertThat(c1.displayName).isEqualTo("log4j : log4j : 1.2.8");
    assertThat(c1.policyViolations).hasSize(0);
    assertThat(c1.effectiveLicenses.stream().map(c -> c.name)).containsExactlyInAnyOrder(expected1);

    PdfComponent c2 =
        pdfData.components.stream()
            .filter(c -> c.displayName.contains("jackson-databind"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("expected component not found in PDF output"));
    assertThat(c2.displayName).isEqualTo("com.fasterxml.jackson.core : jackson-databind : 2.9.9");

    assertThat(c2.effectiveLicenses).hasSize(1);
    assertThat(c2.effectiveLicenses.stream().map(c -> c.name)).contains(expected2);
  }

  @Test
  public void tesExportPdf_withEmptyReportData() throws Exception {
    // Given
    File testBomFile = mockOriginalSbomFile("test-empty-bom.xml");
    ThirdPartySbomMetadata sbomMetadata =
        createMetadataEntity(testBomFile.getName(), app.getId(), SBOM_VERSION, CYCLONEDX_15, SbomFormat.XML);
    exporter.setExportParams(withExportParams(sbomMetadata, CYCLONEDX_15, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    setupTestComponents();

    // When
    PdfData pdfData = exporter.exportPdf();

    // Then
    assertThat(pdfData.title).isEqualTo(app.getName() + REPORT_NAME);
    assertThat(pdfData.createdDate).isNotNull();
    assertThat(pdfData.analyzedDate).isNotNull();
    assertThat(pdfData.productVersion).isNotNull();
    assertThat(pdfData.sbomMetadata.author).hasSize(1).contains("John Doe");
    assertThat(pdfData.sbomMetadata.specification).isEqualTo("CYCLONEDX");
    assertThat(pdfData.sbomMetadata.specVersion).isEqualTo("1.5");
    assertThat(pdfData.sbomMetadata.fileFormat).isEqualTo("xml");

    assertThat(pdfData.components).isEmpty();
  }

  @Test
  public void testExportPdf_withPrefetchedPolicyData() throws Exception {
    File testBomFile = mockOriginalSbomFile("test-1-bom.xml");
    ThirdPartySbomMetadata sbomMetadata =
        createMetadataEntity(testBomFile.getName(), app.getId(), SBOM_VERSION, CYCLONEDX_15, SbomFormat.XML);
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fc1 = setupFileCoordinateEntity("log4j", "1.2.8", "3640dd71069d7986c9a1",
        "pkg:maven/log4j/log4j@1.2.8?type=jar", "pkg:maven/log4j/log4j@1.2.8?type=jar:3640dd71069d7986c9a1");
    setupCoordinateSecurityEntity(fc1, "CVE-2022-23307", "name=CVE-2022-23307", "HIGH", "502", "CVSSV3",
        "NVD", 8.8d, null);

    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.components = new ArrayList<>();
    ApiReportComponentPolicyViolationsDTOV2 violationComponent = new ApiReportComponentPolicyViolationsDTOV2();
    violationComponent.hash = "3640dd71069d7986c9a1";
    violationComponent.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        new PackageUrlIdentifier("pkg:maven/log4j/log4j@1.2.8?type=jar").toComponentIdentifier());
    ApiReportPolicyViolationDTOV2 violation = new ApiReportPolicyViolationDTOV2();
    violation.policyName = "Prefetched-Policy";
    violation.policyThreatLevel = 9;
    violation.policyThreatCategory = "SECURITY";
    violationComponent.violations.add(violation);
    policyData.components.add(violationComponent);

    SbomExportParams exportParams = withExportParams(sbomMetadata, CYCLONEDX_15, SbomFormat.JSON);
    exportParams.withPolicyData(policyData);
    exporter.setExportParams(exportParams);

    PdfData pdfData = exporter.exportPdf();

    PdfComponent c1 = pdfData.components.stream()
        .filter(c -> c.displayName.contains("log4j"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("expected component not found in PDF output"));
    assertThat(c1.policyViolations).hasSize(1);
    assertThat(c1.policyViolations.get(0).policyName).isEqualTo("Prefetched-Policy");
    assertThat(c1.policyViolations.get(0).policyThreatLevel).isEqualTo(9);
    assertThat(c1.policyViolations.get(0).policyThreatCategory).isEqualTo("SECURITY");
  }
}
