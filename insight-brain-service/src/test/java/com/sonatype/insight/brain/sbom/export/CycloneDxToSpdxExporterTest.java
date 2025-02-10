/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.export;

import java.io.File;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.scan.file.SbomFormat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.xmlunit.assertj.XmlAssert;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.spdxDxIgnoreNodesFilter;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

public class CycloneDxToSpdxExporterTest extends AbstractSbomExporterTest
{
  private CycloneDxToSpdxExporter exporter;

  private static final String APP_ID = "webgoat";

  private static final String SBOM_VERSION = "v1";

  private static final String THIRD_PARTY_FILE = "testBom.json";

  private static final String TEST_XML_SBOM = "webgoat-bom.xml";

  private static final String TEST_JSON_SBOM = "webgoat-bom.json";

  private static final String SCAN_ID = "sid1";

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  private ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  @Inject
  private ThirdPartyFileDAO thirdPartyFileDAO;

  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Inject
  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Mock
  private BaseUrl baseUrl;

  @Inject
  private IdUtils idUtils;

  @Inject
  private VersionService versionService;

  private ThirdPartyFile thirdPartyFile;

  @Before
  public void init() {
    tempEntity.newApplicationWithParent(APP_ID);
    thirdPartyFile = tempEntity.newThirdPartyFile(THIRD_PARTY_FILE);
    exporter = new CycloneDxToSpdxExporter(
        mockInsightWork,
        thirdPartyFileDAO,
        thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO,
        baseUrl,
        idUtils,
        versionService
    );
  }

  @Test
  public void exportTest_withXmlInputFormat_toXmlOutputFormat() throws Exception {
    File testBomFile = mockSbomFileForApp(APP_ID, getGZippedSbom(TEST_XML_SBOM));
    String exportedBomStr = setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.XML);
    XmlAssert.assertThat(exportedBomStr).and(readFileToString("outputs/webgoat-from-xml-to-spdx.xml"))
        .withNodeFilter(spdxDxIgnoreNodesFilter())
        .ignoreWhitespace()
        .areIdentical();
  }

  @Test
  public void exportTest_withXmlInputFormat_toJsonOutputFormat() throws Exception {
    File testBomFile = mockSbomFileForApp(APP_ID, getGZippedSbom(TEST_XML_SBOM));
    String exportedBomStr = setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.JSON);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths("creationInfo.created", "creationInfo.creators[0]")
        .isEqualTo(readFileToString("outputs/webgoat-from-xml-to-spdx.json"));
  }

  @Test
  public void exportTest_withJsonInputFormat_toXmlOutputFormat() throws Exception {
    File testBomFile = mockSbomFileForApp(APP_ID, getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr = setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.XML);
    XmlAssert.assertThat(exportedBomStr).and(readFileToString("outputs/webgoat-from-json-to-spdx.xml"))
        .withNodeFilter(spdxDxIgnoreNodesFilter())
        .ignoreWhitespace()
        .areIdentical();
  }

  @Test
  public void exportTest_withJsonInputFormat_toJsonOutputFormat() throws Exception {
    File testBomFile = mockSbomFileForApp(APP_ID, getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr = setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.JSON);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths("creationInfo.created", "creationInfo.creators[0]")
        .isEqualTo(readFileToString("outputs/webgoat-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_SonatypeVulnerability_WithMissingCwes() throws Exception {
    File testBomFile = mockSbomFileForApp(APP_ID, getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr = setupExportSbomScenarioWithNullDbVulnerabilityField(testBomFile,
        SbomFormat.JSON, "cwes");
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths("creationInfo.created", "creationInfo.creators[0]")
        .isEqualTo(readFileToString("outputs/webgoat-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_SonatypeVulnerability_WithMissingSeverityDescription() throws Exception {
    File testBomFile = mockSbomFileForApp(APP_ID, getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr = setupExportSbomScenarioWithNullDbVulnerabilityField(testBomFile,
        SbomFormat.JSON, "severityDescription");
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths("creationInfo.created", "creationInfo.creators[0]")
        .isEqualTo(readFileToString("outputs/webgoat-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_BomMissingMetadata() throws Exception {
    File testBomFile = mockSbomFileForApp(APP_ID, getGZippedSbom("missing-metadata-bom.json"));
    String exportedBomStr = setupExportSbomScenarioWithNullDbVulnerabilityField(testBomFile,
        SbomFormat.JSON, "");
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths("creationInfo.created", "creationInfo.creators[0]")
        .isEqualTo(readFileToString("outputs/missing-metadata-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_mergeDataMatchingByComponentRef() throws Exception {
    File testBomFile = mockSbomFileForApp(APP_ID, getGZippedSbom("webgoat-with-component-ref-bom.json"));
    String exportedBomStr = setupExportSbomScenarioWithComponentRef(testBomFile,
        SbomFormat.JSON, "");
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths("creationInfo.created", "creationInfo.creators[0]")
        .isEqualTo(readFileToString("outputs/webgoat-with-component-ref-to-spdx.json"));
  }

  private String setupExportSbomScenarioWithFileAndOutputFormat(File testBomFile, SbomFormat outputFormat) {
    defineDbTestData(thirdPartyFile, "");
    ThirdPartySbomMetadata sbomMetadata = insertTestData(testBomFile.getName(),
        thirdPartyFile);

    exporter.setExportParams(SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(ExportSpecification.SPDX_23)
        .withTargetFormat(outputFormat));
    return exporter.export();
  }

  private String setupExportSbomScenarioWithNullDbVulnerabilityField(
      File testBomFile,
      SbomFormat outputFormat,
      String nullField) throws Exception
  {
    defineDbTestData(thirdPartyFile, nullField);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(testBomFile.getName(),
        thirdPartyFile);
    exporter.setExportParams(SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(ExportSpecification.SPDX_23)
        .withTargetFormat(outputFormat));
    return exporter.export();
  }

  private void defineDbTestData(ThirdPartyFile thirdPartyFile, String nullField) {
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "Sonatype",
        "JSON",
        "log4j",
        "1.2.8",
        "3640dd71069d7986c9a1",
        "pkg:maven/log4j/log4j@1.2.8?type=jar"
    );

    defineDbTestDataNullVulnerabilityField(nullField, fileCoordinate);
    tempEntity.newThirdPartyCoordinateSecurity(fileCoordinate,
        "ABC-123", "DESC 123", "http://www.source.com/abc-123", 1.5d,
        "1.2", "source",
        "v:2", "lkk", "5467", "owasp", "<dd>r1<dd/>",
        "<dd>a1<dd/>", "M");

    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "CVE-2022-23307", "DESC 123", "http://www.source.com/cve-2022-23307",
        1.5d, "1.2",
        "source", "v:2", "lkk", "5467", "owasp",
        "<dd>r1<dd/>", "<dd>a1<dd/>", "M");

    tempEntity.newThirdPartyCoordinateLicense(fileCoordinate, "MIT", "MIT",
        "http://mit", "A");
    tempEntity.newThirdPartyCoordinateLicense(fileCoordinate, "Aladdin", "Aladdin",
        "http://aladdin", "A,B");
    tempEntity.newThirdPartyCoordinateLicense(fileCoordinate, "Apache-1.1", "Apache-1.1",
        "http://apache.org", "B,C,D");
  }

  private ThirdPartySbomMetadata insertTestData(String testBomFile, ThirdPartyFile thirdPartyFile) {
    ThirdPartySbomMetadata dbRecord = tempEntity.createSbomMetadata(APP_ID, SBOM_VERSION,
        thirdPartyFile, ACTIVE);
    dbRecord.setFilename(testBomFile);
    thirdPartySbomMetadataDAO.update(dbRecord);
    return dbRecord;
  }

  private void defineDbTestDataNullVulnerabilityField(String nullField, ThirdPartyFileCoordinate fileCoordinate) {
    switch (nullField) {
      case "cwes":
        tempEntity.newThirdPartyCoordinateSecurity(
            fileCoordinate, "sonatype-2010-0053", "DESC sonatype-2010-0053",
            "http://www.sonatype.com/sonatype-2010-0053", 5.5d,
            "1.1", "source", "v:1", "Medium", null,
            "m1", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
        break;
      case "severityDescription":
        tempEntity.newThirdPartyCoordinateSecurity(
            fileCoordinate, "sonatype-2010-0053", "DESC sonatype-2010-0053",
            "http://www.sonatype.com/sonatype-2010-0053", 5.5d,
            "1.1", "source", "v:1", null, "1234",
            "m1", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
        break;
      default:
        tempEntity.newThirdPartyCoordinateSecurity(
            fileCoordinate, "sonatype-2010-0053", "DESC sonatype-2010-0053",
            "http://www.sonatype.com/sonatype-2010-0053", 5.5d,
            "1.1", "source", "v:1", "Medium", "1234",
            "m1", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
    }
  }

  private String setupExportSbomScenarioWithComponentRef(
      File testBomFile,
      SbomFormat outputFormat,
      String nullField) throws Exception
  {
    defineDbTestData(thirdPartyFile, nullField);
    mockDbRecordsWithComponentsMatchingByComponentRef(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(testBomFile.getName(),
        thirdPartyFile);
    exporter.setExportParams(SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(ExportSpecification.SPDX_23)
        .withTargetFormat(outputFormat));
    return exporter.export();
  }

  private ThirdPartyFileCoordinate mockDbRecordsWithComponentsMatchingByComponentRef(ThirdPartyFile tpFile) {
    ThirdPartyFileCoordinate componentWithComponentRef = tempEntity.newThirdPartyFileCoordinate(tpFile,
        "Third-Party", "maven", "parentApp", "1.0-SNAPSHOT", "e33c095684013cced988",
        "pkg:maven/org.example/abc-component", "fcd38b25daab77edee3dbd1122f1733d652f03b6");
    tempEntity.newThirdPartyCoordinateSecurity(componentWithComponentRef, "ABC-123",
        "Test ABC vulnerability",
        "http://cve.mitre.org/cgi-bin/cvename.cgi?name=ABC-123", 5.5d, "HIGH", "NVD",
        " CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "HIGH", "502", "", "", "", "SBOM");
    tempEntity.newThirdPartyCoordinateLicense(componentWithComponentRef, "GPL-2.0", "GPL-2.0", "",
        "SBOM");

    return componentWithComponentRef;
  }
}
