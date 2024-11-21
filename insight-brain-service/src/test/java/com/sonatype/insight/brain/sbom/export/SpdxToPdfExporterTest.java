/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.report.pdf.PdfData;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.scan.file.SbomFormat;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification.SPDX_23;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class SpdxToPdfExporterTest extends AbstractPdfExporterTest
{
  private SpdxToPdfExporter exporter;

  @Override
  @Before
  public void init() throws SbomExportException {
    super.init();
    exporter = new SpdxToPdfExporter(
        mockInsightWork,
        multiLicenseDAO,
        thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO,
        thirdPartyScanDAO,
        applicationDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO,
        baseUrl,
        idUtils,
        versionService,
        apiReportDataServiceV2
    );
  }

  @Test
  public void testExportPdf_withMergedVulnerabilitiesLicenses() throws Exception {
    //Given
    File testBomFile = mockOriginalSbomFile("spdx-bom.xml");
    ThirdPartySbomMetadata sbomMetadata =
        createMetadataEntity(testBomFile.getName(), app.getId(), SBOM_VERSION, SPDX_23);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.COMPLIANCE.getId(), SCAN_ID, new Date());
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    File reportZip = Paths.get(ReportHelper.zipReport(
        "/SpdxToPdfExporterTest/report-for-test", tempDir).toURI()).toFile();
    when(mockWork.getReportFile(app.getId(), SCAN_ID)).thenReturn(reportZip);
    setupTestComponents();

    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    rawData.components = new ArrayList<>();
    rawData.components.add(setupReportRawDataLTG("pkg:maven/log4j/log4j@1.2.8?type=jar", 5));
    rawData.components.add(
        setupReportRawDataLTG("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar", 10));

    //When
    SbomExportParams exportParams = withExportParams(sbomMetadata, SPDX_23, SbomFormat.JSON);
    exportParams.withReportRawData(rawData);
    exporter.setExportParams(exportParams);
    PdfData pdfData = exporter.exportPdf();
    assertPdfData(pdfData, SPDX_23, 3);
  }

  @Test
  public void testExportPdf_withMissingReportData() throws Exception {
    //Given
    File testBomFile = mockOriginalSbomFile("spdx-bom.xml");
    ThirdPartySbomMetadata sbomMetadata =
        createMetadataEntity(testBomFile.getName(), app.getId(), SBOM_VERSION, SPDX_23);
    exporter.setExportParams(withExportParams(sbomMetadata, SPDX_23, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fc1 = setupFileCoordinateEntity("log4j", "1.2.8", "3640dd71069d7986c9a1",
        "pkg:maven/log4j/log4j@1.2.8?type=jar", "pkg:maven/log4j/log4j@1.2.8?type=jar:3640dd71069d7986c9a1"
    );
    setupFileCoordinateEntity("jackson-databind", "2.9.9", "43482bee60d253ab70b6",
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar",
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar"
    );
    setupCoordinateSecurityEntity(fc1, "CVE-2022-23307", "name=CVE-2022-23307", "HIGH", "502", "CVSSV3",
        "NVD", 8.8d, null);

    //When
    PdfData pdfData = exporter.exportPdf();

    //Then
    assertThat(pdfData.title).isEqualTo(app.getName() + REPORT_NAME);
    assertThat(pdfData.createdDate).isNotNull();
    assertThat(pdfData.analyzedDate).isNotNull();
    assertThat(pdfData.productVersion).isNotNull();
    assertThat(pdfData.components).hasSize(3);
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
}
