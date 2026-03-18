/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.report.pdf.PdfData;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.scan.file.SbomFormat;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification.SPDX_22;
import static com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification.SPDX_23;
import static org.assertj.core.api.Assertions.assertThat;

public class SpdxToPdfExporterTest
    extends AbstractPdfExporterTest
{
  private SpdxToPdfExporter exporter;

  @Override
  @Before
  public void init() throws SbomExportException {
    super.init();
    exporter = new SpdxToPdfExporter(
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
  public void testExportPdf_withMergedVulnerabilitiesLicenses_2_3() throws Exception {
    assertPdfExportData("spdx2.3-bom.xml", SPDX_23);
  }

  @Test
  public void testExportPdf_withMergedVulnerabilitiesLicenses_2_2() throws Exception {
    assertPdfExportData("spdx2.2-bom.xml", SPDX_22);
  }

  private void assertPdfExportData(String fileName, ExportSpecification spec) throws Exception {
    File testBomFile = mockOriginalSbomFile(fileName);
    ThirdPartySbomMetadata sbomMetadata =
        createMetadataEntity(testBomFile.getName(), app.getId(), SBOM_VERSION, spec, SbomFormat.XML);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.COMPLIANCE.getId(), SCAN_ID, new Date());
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ReportHelper.saveMockReport(insightWork, tempDir, "/SpdxToPdfExporterTest/report-for-test", app.getId(), SCAN_ID);
    setupTestComponents();

    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    rawData.components = new ArrayList<>();
    rawData.components.add(setupReportRawDataLTG("pkg:maven/log4j/log4j@1.2.8?type=jar", 5));
    rawData.components.add(
        setupReportRawDataLTG("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar", 10));

    // When
    SbomExportParams exportParams = withExportParams(sbomMetadata, spec, SbomFormat.JSON);
    exportParams.withReportRawData(rawData);
    exporter.setExportParams(exportParams);
    PdfData pdfData = exporter.exportPdf();
    assertPdfData(pdfData, spec, 2);
  }

  @Test
  public void testExportPdf_withMissingReportData() throws Exception {
    // Given
    File testBomFile = mockOriginalSbomFile("spdx2.3-bom.xml");
    ThirdPartySbomMetadata sbomMetadata =
        createMetadataEntity(testBomFile.getName(), app.getId(), SBOM_VERSION, SPDX_23, SbomFormat.XML);
    exporter.setExportParams(withExportParams(sbomMetadata, SPDX_23, SbomFormat.JSON));
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
    assertThat(pdfData.sbomMetadata.specification).isEqualTo("SPDX");
    assertThat(pdfData.sbomMetadata.specVersion).isEqualTo("2.3");
    assertThat(pdfData.sbomMetadata.fileFormat).isEqualTo("xml");

    PdfComponent c1 = pdfData.components.stream().filter(c -> c.displayName.contains("log4j")).findFirst().get();
    assertThat(c1.displayName).isEqualTo("log4j : log4j : 1.2.8");
    assertThat(c1.matchState).isEqualTo("exact");
    assertThat(c1.policyViolations).isEmpty();
    assertThat(c1.effectiveLicenses).isEmpty();
    assertThat(c1.securityIssues).hasSize(1);
    assertThat(c1.securityIssues.stream().map(s -> s.reference)).contains("CVE-2022-23307");

    PdfComponent c2 =
        pdfData.components.stream().filter(c -> c.displayName.contains("jackson-databind")).findFirst().get();
    assertThat(c2.displayName).isEqualTo("com.fasterxml.jackson.core : jackson-databind : 2.9.9");
    assertThat(c2.matchState).isEqualTo("exact");
    assertThat(c2.policyViolations).hasSize(0);
    assertThat(c2.securityIssues).hasSize(0);
  }

  @Test
  public void testExportPdf_validateVulnerabilitiesArePrinted() throws Exception {
    // Given
    File testBomFile = mockOriginalSbomFile("spdx2.3-validate-vulnerabilities-are-printed.spdx.json");
    ThirdPartySbomMetadata sbomMetadata =
        createMetadataEntityJson(testBomFile.getName(), app.getId(), SBOM_VERSION, SPDX_23);
    exporter.setExportParams(withExportParams(sbomMetadata, SPDX_23, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fc1 = setupFileCoordinateEntity("org.apache.logging.log4j:log4j-core", "2.13.2", "12345",
        "pkg:maven/org.apache.logging.log4j/log4j-core@2.13.2?type=jar",
        "pkg:maven/org.apache.logging.log4j/log4j-core@2.13.2?type=jar:12345");
    ThirdPartyFileCoordinate fc2 = setupFileCoordinateEntity("junit:junit", "4.12", "67890",
        "pkg:maven/junit/junit@4.12?type=jar",
        "pkg:maven/junit/junit@4.12?type=jar");
    setupCoordinateSecurityEntity(fc1, "CVE-2021-45046", "name=CVE-2021-45046", "HIGH", "502", "CVSSV3",
        "NVD", 8.8d, null);
    setupCoordinateSecurityEntity(fc2, "CVE-2020-15250", "name=CVE-2020-15250", "HIGH", "502", "CVSSV3",
        "NVD", 1.0d, null);

    // When
    PdfData pdfData = exporter.exportPdf();

    // Then
    assertThat(pdfData.title).isEqualTo(app.getName() + REPORT_NAME);
    assertThat(pdfData.createdDate).isNotNull();
    assertThat(pdfData.analyzedDate).isNotNull();
    assertThat(pdfData.productVersion).isNotNull();
    assertThat(pdfData.components).hasSize(2);
    assertThat(pdfData.sbomMetadata.author).hasSize(1).contains("John Doe");
    assertThat(pdfData.sbomMetadata.specification).isEqualTo("SPDX");
    assertThat(pdfData.sbomMetadata.specVersion).isEqualTo("2.3");
    assertThat(pdfData.sbomMetadata.fileFormat).isEqualTo("json");

    PdfComponent c1 = pdfData.components.stream().filter(c -> c.displayName.contains("log4j")).findFirst().get();
    assertThat(c1.displayName).isEqualTo("org.apache.logging.log4j : log4j-core : 2.13.2");
    assertThat(c1.matchState).isEqualTo("exact");
    assertThat(c1.policyViolations).hasSize(0);
    assertThat(c1.effectiveLicenses).hasSize(1);
    assertThat(c1.effectiveLicenses.get(0).name).isEqualTo("Apache-2.0");
    // Verify vulnerabilities are printed
    assertThat(c1.securityIssues).hasSize(1);
    assertThat(c1.securityIssues.stream().map(s -> s.reference)).contains("CVE-2021-45046");

    PdfComponent c2 = pdfData.components.stream().filter(c -> c.displayName.contains("junit")).findFirst().get();
    assertThat(c2.displayName).isEqualTo("junit : junit : 4.12");
    assertThat(c2.matchState).isEqualTo("exact");
    assertThat(c2.policyViolations).hasSize(0);
    assertThat(c2.effectiveLicenses).hasSize(1);
    assertThat(c2.effectiveLicenses.get(0).name).isEqualTo("EPL-1.0");
    // Verify vulnerabilities are printed
    assertThat(c2.securityIssues).hasSize(1);
    assertThat(c2.securityIssues.stream().map(s -> s.reference)).contains("CVE-2020-15250");
  }

  private ThirdPartySbomMetadata createMetadataEntityJson(
      String filename,
      String applicationId,
      String version,
      SbomExportParams.ExportSpecification spec)
  {
    ThirdPartySbomMetadata entity = tempEntity.createSbomMetadata(applicationId, version,
        thirdPartyFile, ACTIVE);
    entity.setFilename(filename);
    entity.setSpec(spec.getSpecification().name());
    entity.setSpecFormat(SbomFormat.JSON.toString());
    entity.setSpecVersion(spec.getVersion());
    entity.setScanType("SBOM");
    entity.setMetadataJson("{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"creators\":[{\"type\":" +
        "\"Author\" ,\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"}]}");
    thirdPartySbomMetadataDAO.update(entity);
    return entity;
  }
}
