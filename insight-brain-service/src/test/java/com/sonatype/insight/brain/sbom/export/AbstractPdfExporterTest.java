/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.File;
import java.util.ArrayList;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.report.pdf.PdfData;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.scan.file.SbomFormat;

import com.google.inject.Binder;
import org.junit.Before;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

abstract class AbstractPdfExporterTest
    extends AbstractSbomExporterTest
{
  protected static final String THIRD_PARTY_FILE = "testBom.json";

  protected static final String APP_ID = "webgoat";

  protected static final String SCAN_ID = "sid1";

  protected static final String SBOM_VERSION = "v1";

  protected static final String REPORT_NAME = " Compliance Report";

  @Inject
  protected MultiLicenseDAO multiLicenseDAO;

  @Inject
  protected ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  protected ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  protected ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Inject
  protected ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Inject
  protected ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  @Inject
  protected ApplicationDAO applicationDAO;

  @Inject
  protected ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  protected IdUtils idUtils;

  @Mock
  protected BaseUrl baseUrl;

  @Inject
  protected InsightWork insightWork;

  @Inject
  protected VersionService versionService;

  protected ThirdPartyFile thirdPartyFile;

  protected Application app;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
  }

  @Before
  public void init() throws SbomExportException {
    app = tempEntity.newApplicationWithParent(APP_ID);
    thirdPartyFile = tempEntity.newThirdPartyFile(THIRD_PARTY_FILE);
    when(baseUrl.get()).thenReturn("http://localhost:8080");
  }

  protected ApiReportComponentDTOV2 setupReportRawDataLTG(final String purl, final int ltgLevel) {
    ApiReportComponentDTOV2 rawCp1 = new ApiReportComponentDTOV2();
    rawCp1.packageUrl = purl;
    rawCp1.licenseData = new ApiLicenseDataDTOV2();
    rawCp1.licenseData.effectiveLicenseThreats = new ArrayList<>();
    ApiLicenseThreatDTOV2 lt1 = new ApiLicenseThreatDTOV2();
    lt1.licenseThreatGroupLevel = ltgLevel;
    rawCp1.licenseData.effectiveLicenseThreats.add(lt1);
    return rawCp1;
  }

  protected void setupTestComponents() {
    ThirdPartyFileCoordinate fc1 = setupFileCoordinateEntity("log4j", "1.2.8", "3640dd71069d7986c9a1",
        "pkg:maven/log4j/log4j@1.2.8?type=jar", "pkg:maven/log4j/log4j@1.2.8?type=jar:3640dd71069d7986c9a1");
    ThirdPartyFileCoordinate fc2 =
        setupFileCoordinateEntity("jackson-databind", "2.9.9", "43482bee60d253ab70b6",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar");

    setupCoordinateSecurityEntity(fc1, "CVE-2022-23307", "name=CVE-2022-23307", "HIGH", "502", "CVSSV3",
        "NVD", 8.8d, "exploitable");
    setupCoordinateSecurityEntity(fc1, "sonatype-2010-0053", null, "HIGH", "426", "OTHER", "SONATYPE",
        7.8d, "resolved");
    setupCoordinateSecurityEntity(fc1, "CVE-2022-23305", "name=CVE-2022-23305", "CRITICAL", "89", "CVSSV3",
        "NVD", 9.8d, "resolved_with_pedigree");
    setupCoordinateSecurityEntity(fc1, "CVE-2023-26464", "name=CVE-2023-26464", "HIGH", "400", "CVSSV3",
        "NVD", 7.5d, "in_triage");
    setupCoordinateSecurityEntity(fc1, "CVE-2021-4104", "name=CVE-2021-4104", "HIGH", "502", "CVSSV3",
        "NVD", 7.5d, "false_positive");
    setupCoordinateSecurityEntity(fc1, "CVE-2019-17571", "name=CVE-2019-17571", "CRITICAL", "502",
        "CVSSV3", "NVD", 9.8d, "not_affected");
    setupCoordinateSecurityEntity(fc1, "CVE-2022-23302", "name=CVE-2022-23302", "HIGH", "502", "CVSSV3",
        "NVD", 8.8d, null);

    setupCoordinateSecurityEntity(fc2, "CVE-2022-42003", "name=CVE-2022-42003", "HIGH", "502", "CVSSV3",
        "NVD", 7.5d, null);
    setupCoordinateSecurityEntity(fc2, "CVE-2019-12384", "name=CVE-2019-12384", "MEDIUM", "502", "CVSSV3",
        "NVD", 5.9d, null);

    tempEntity.newThirdPartyCoordinateLicense(fc1, "MPL-2.0", "MPL-2.0", null, "SBOM");
    tempEntity.newThirdPartyCoordinateLicense(fc1, "Apache-1.1", null, null, "Sonatype");
    tempEntity.newThirdPartyCoordinateLicense(fc2, "Apache-2.0", null, null, "Sonatype");
  }

  protected void setupTestComponentsForDuplicateComponentsTest() {
    ThirdPartyFileCoordinate fc1 = setupFileCoordinateEntity("Microsoft.Extensions.ApiDescription.Server", "3.0.0",
        "85e25187b46727232561", "pkg:nuget/Microsoft.Extensions.ApiDescription.Server@3.0.0",
        "pkg:nuget/Microsoft.Extensions.ApiDescription.Server@3.0.0");
    setupCoordinateSecurityEntity(fc1, "sonatype-2021-0713", "link=sonatype-2021-0713", "HIGH", "400", "OTHER",
        "SONATYPE", 7.5d, null);
    setupCoordinateSecurityEntity(fc1, "sonatype-2022-5998", null, "HIGH", "755", "OTHER", "SONATYPE",
        7.5d, null);
    setupCoordinateSecurityEntity(fc1, "CVE-2024-21907", "link=CVE-2024-21907", "CRITICAL", "755", "CVSSV3",
        "NVD", 7.5d, null);
    tempEntity.newThirdPartyCoordinateLicense(fc1, "Apache-2.0", "Apache-2.0", null, "Sonatype");
  }

  protected void setupCoordinateSecurityEntity(
      ThirdPartyFileCoordinate fc1,
      String refId,
      String link,
      String severityDesc,
      String cwes,
      String ratingMethod,
      String vulnSource,
      double severity,
      String analysisState)
  {
    ThirdPartyCoordinateSecurity cs1 = tempEntity.newThirdPartyCoordinateSecurity(fc1, refId, null,
        link, severity, null, vulnSource, "CVSS:3.1", severityDesc, cwes,
        ratingMethod, null, null, "Sonatype");

    if (analysisState != null) {
      tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(cs1, refId, analysisState,
          "code_not_reachable", "response", "details");
    }
  }

  protected ThirdPartyFileCoordinate setupFileCoordinateEntity(
      String name,
      String version,
      String hash,
      String purl,
      String filenames)
  {
    return tempEntity.newThirdPartyFileCoordinateWithMatchState(thirdPartyFile,
        "SBOM", "maven", name, version, hash, purl, filenames, "exact");
  }

  protected ThirdPartySbomMetadata createMetadataEntity(
      String filename,
      String applicationId,
      String version,
      SbomExportParams.ExportSpecification spec,
      SbomFormat sbomFormat)
  {
    ThirdPartySbomMetadata entity = tempEntity.createSbomMetadata(applicationId, version,
        thirdPartyFile, ACTIVE);
    entity.setFilename(filename);
    entity.setSpec(spec.getSpecification().name());
    entity.setSpecFormat(sbomFormat.toString());
    entity.setSpecVersion(spec.getVersion());
    entity.setScanType("SBOM");
    entity.setMetadataJson("{\"type\":\"application\",\"created\":\"2024-02-29T23:41:22Z\",\"creators\":[{\"type\":" +
        "\"Author\" ,\"name\":\"John Doe\",\"email\":\"john.doe@example.com\",\"phone\":\"1-800-111-1111\"}]}");
    thirdPartySbomMetadataDAO.update(entity);
    return entity;
  }

  protected File mockOriginalSbomFile(String sbomFileName) throws Exception {
    return mockSbomFileForApp(app.getId(), getGZippedSbom(sbomFileName));
  }

  protected void assertPdfData(PdfData pdfData, SbomExportParams.ExportSpecification spec, int componentsSize) {
    // Then
    assertThat(pdfData.title).isEqualTo(app.getName() + REPORT_NAME);
    assertThat(pdfData.createdDate).isNotNull();
    assertThat(pdfData.analyzedDate).isNotNull();
    assertThat(pdfData.productVersion).isNotNull();
    assertThat(pdfData.components).hasSize(componentsSize);
    assertThat(pdfData.sbomMetadata.author).hasSize(1).contains("John Doe");
    assertThat(pdfData.sbomMetadata.specification).isEqualTo(spec.getSpecification().name());
    assertThat(pdfData.sbomMetadata.specVersion).isEqualTo(spec.getVersion());
    assertThat(pdfData.sbomMetadata.fileFormat).isEqualTo("xml");
    assertThat(pdfData.sbomMetadata.createdAt).isNotNull();
    assertThat(pdfData.sbomMetadata.scanId).isEqualTo("sid1");

    PdfComponent c1 = pdfData.components.stream().filter(c -> c.displayName.contains("log4j")).findFirst().get();
    assertThat(c1.displayName).isEqualTo("log4j : log4j : 1.2.8");
    assertThat(c1.matchState).isEqualTo("exact");
    assertThat(c1.policyViolations).hasSize(9);
    assertThat(c1.policyViolations).filteredOn(v -> v.policyName.equals("Architecture-Quality")).hasSize(2);
    assertThat(c1.policyViolations).filteredOn(v -> v.policyName.equals("Security-Critical")).hasSize(2);
    assertThat(c1.policyViolations).filteredOn(v -> v.policyName.equals("Security-High")).hasSize(5);
    assertThat(c1.effectiveLicenses).hasSize(2);
    assertThat(c1.effectiveLicenses.stream().map(c -> c.name)).containsExactlyInAnyOrder("MPL-2.0", "Apache-1.1");
    assertThat(c1.securityIssues).hasSize(7);
    assertThat(c1.securityIssues.stream().map(s -> s.reference)).containsExactlyInAnyOrder("CVE-2022-23307",
        "sonatype-2010-0053", "CVE-2022-23305", "CVE-2023-26464", "CVE-2021-4104", "CVE-2019-17571", "CVE-2022-23302");
    assertThat(c1.securityIssues.stream().map(s -> s.analysisState)).containsExactlyInAnyOrder("Resolved",
        "Exploitable", "False Positive", "Resolved With Pedigree", "In Triage", "Unannotated", "Not Affected");

    PdfComponent c2 =
        pdfData.components.stream().filter(c -> c.displayName.contains("jackson-databind")).findFirst().get();
    assertThat(c2.displayName).isEqualTo("com.fasterxml.jackson.core : jackson-databind : 2.9.9");
    assertThat(c2.matchState).isEqualTo("exact");
    assertThat(c2.policyViolations).hasSize(7);
    assertThat(c2.policyViolations).filteredOn(v -> v.policyName.equals("Architecture-Quality")).hasSize(1);
    assertThat(c2.policyViolations).filteredOn(v -> v.policyName.equals("Security-Medium")).hasSize(2);
    assertThat(c2.policyViolations).filteredOn(v -> v.policyName.equals("Security-High")).hasSize(4);
    assertThat(c2.effectiveLicenses).hasSize(1);
    assertThat(c2.effectiveLicenses.stream().map(c -> c.name)).contains("Apache-2.0");
    assertThat(c2.securityIssues).hasSize(2);
    assertThat(c2.securityIssues.stream().map(s -> s.reference)).containsExactlyInAnyOrder("CVE-2022-42003",
        "CVE-2019-12384");

    // LTGs
    assertThat(c1.effectiveLicenseThreats).hasSize(1);
    assertThat(c1.effectiveLicenseThreats.get(0).licenseThreatGroupLevel).isEqualTo(5);
    assertThat(c2.effectiveLicenseThreats).hasSize(1);
    assertThat(c2.effectiveLicenseThreats.get(0).licenseThreatGroupLevel).isEqualTo(10);
  }
}
