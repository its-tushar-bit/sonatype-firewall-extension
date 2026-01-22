/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.export;

import java.io.File;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.license.ThirdPartyComponentLicenseResolutionService;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.scan.file.SbomFormat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.xmlunit.assertj.XmlAssert;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.SPDX_JSON_IGNORE_FIELDS;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.spdxDxIgnoreNodesFilter;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

public class CycloneDxToSpdxExporterTest
    extends AbstractSbomExporterTest
{
  private CycloneDxToSpdxExporter exporter;

  private static final String APP_NAME = "webgoat";

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

  @Inject
  private ThirdPartyComponentLicenseResolutionService thirdPartyLicenseResolver;

  private ThirdPartyFile thirdPartyFile;

  private Application app;

  @Before
  public void init() {
    app = tempEntity.newApplicationWithParent(APP_NAME, APP_NAME);
    thirdPartyFile = tempEntity.newThirdPartyFile(THIRD_PARTY_FILE);
    exporter = new CycloneDxToSpdxExporter(
        thirdPartyFileDAO,
        thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO,
        baseUrl,
        idUtils,
        versionService,
        thirdPartyLicenseResolver,
        buildThirdPartyPersistenceService()
    );
  }

  @Test
  public void exportTest_withXmlInputFormat_toXmlOutputFormat_23() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_XML_SBOM));
    String exportedBomStr =
        setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.XML, ExportSpecification.SPDX_23);
    XmlAssert.assertThat(exportedBomStr).and(readFileToString("outputs/2_3/webgoat-from-xml-to-spdx.xml"))
        .withNodeFilter(spdxDxIgnoreNodesFilter())
        .withNodeMatcher(new IgnoreXmlListOrderMatcher())
        .ignoreWhitespace()
        .areSimilar();
  }

  @Test
  public void exportTest_withXmlInputFormat_toXmlOutputFormat_22() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_XML_SBOM));
    String exportedBomStr =
        setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.XML, ExportSpecification.SPDX_22);
    XmlAssert.assertThat(exportedBomStr).and(readFileToString("outputs/2_2/webgoat-from-xml-to-spdx.xml"))
        .withNodeFilter(spdxDxIgnoreNodesFilter())
        .withNodeMatcher(new IgnoreXmlListOrderMatcher())
        .ignoreWhitespace()
        .areSimilar();
  }

  @Test
  public void exportTest_withXmlInputFormat_toJsonOutputFormat_23() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_XML_SBOM));
    String exportedBomStr =
        setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.JSON, ExportSpecification.SPDX_23);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_3/webgoat-from-xml-to-spdx.json"));
  }

  @Test
  public void exportTest_withXmlInputFormat_toJsonOutputFormat_22() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_XML_SBOM));
    String exportedBomStr =
        setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.JSON, ExportSpecification.SPDX_22);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_2/webgoat-from-xml-to-spdx.json"));
  }

  @Test
  public void exportTest_withJsonInputFormat_toXmlOutputFormat_23() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr =
        setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.XML, ExportSpecification.SPDX_23);
    XmlAssert.assertThat(exportedBomStr).and(readFileToString("outputs/2_3/webgoat-from-json-to-spdx.xml"))
        .withNodeFilter(spdxDxIgnoreNodesFilter())
        .withNodeMatcher(new IgnoreXmlListOrderMatcher())
        .ignoreWhitespace()
        .areSimilar();
  }

  @Test
  public void exportTest_withJsonInputFormat_toXmlOutputFormat_22() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr =
        setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.XML, ExportSpecification.SPDX_22);
    XmlAssert.assertThat(exportedBomStr).and(readFileToString("outputs/2_2/webgoat-from-json-to-spdx.xml"))
        .withNodeFilter(spdxDxIgnoreNodesFilter())
        .withNodeMatcher(new IgnoreXmlListOrderMatcher())
        .ignoreWhitespace()
        .areSimilar();
  }

  @Test
  public void exportTest_withJsonInputFormat_toJsonOutputFormat_23() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr =
        setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.JSON, ExportSpecification.SPDX_23);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_3/webgoat-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_withJsonInputFormat_toJsonOutputFormat_22() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr =
        setupExportSbomScenarioWithFileAndOutputFormat(testBomFile, SbomFormat.JSON, ExportSpecification.SPDX_22);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_2/webgoat-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_SonatypeVulnerability_WithMissingCwes_23() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr = setupExportSbomScenarioWithNullDbVulnerabilityField(testBomFile,
        SbomFormat.JSON, "cwes", ExportSpecification.SPDX_23);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_3/webgoat-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_SonatypeVulnerability_WithMissingCwes_22() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr = setupExportSbomScenarioWithNullDbVulnerabilityField(testBomFile,
        SbomFormat.JSON, "cwes", ExportSpecification.SPDX_22);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_2/webgoat-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_SonatypeVulnerability_WithMissingSeverityDescription_23() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr = setupExportSbomScenarioWithNullDbVulnerabilityField(testBomFile,
        SbomFormat.JSON, "severityDescription", ExportSpecification.SPDX_23);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_3/webgoat-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_SonatypeVulnerability_WithMissingSeverityDescription_22() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(TEST_JSON_SBOM));
    String exportedBomStr = setupExportSbomScenarioWithNullDbVulnerabilityField(testBomFile,
        SbomFormat.JSON, "severityDescription", ExportSpecification.SPDX_22);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_2/webgoat-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_BomMissingMetadata_23() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("missing-metadata-bom.json"));
    String exportedBomStr = setupExportSbomScenarioWithNullDbVulnerabilityField(testBomFile,
        SbomFormat.JSON, "", ExportSpecification.SPDX_23);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_3/missing-metadata-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_BomMissingMetadata_22() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("missing-metadata-bom.json"));
    String exportedBomStr = setupExportSbomScenarioWithNullDbVulnerabilityField(testBomFile,
        SbomFormat.JSON, "", ExportSpecification.SPDX_22);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_2/missing-metadata-from-json-to-spdx.json"));
  }

  @Test
  public void exportTest_mergeDataMatchingByComponentRef_23() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("webgoat-with-component-ref-bom.json"));
    String exportedBomStr = setupExportSbomScenarioWithComponentRef(testBomFile,
        SbomFormat.JSON, "", ExportSpecification.SPDX_23);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_3/webgoat-with-component-ref-to-spdx.json"));
  }

  @Test
  public void exportTest_mergeDataMatchingByComponentRef_22() throws Exception {
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("webgoat-with-component-ref-bom.json"));
    String exportedBomStr = setupExportSbomScenarioWithComponentRef(testBomFile,
        SbomFormat.JSON, "", ExportSpecification.SPDX_22);
    assertThatJson(exportedBomStr)
        .whenIgnoringPaths(SPDX_JSON_IGNORE_FIELDS)
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(readFileToString("outputs/2_2/webgoat-with-component-ref-to-spdx.json"));
  }

  private String setupExportSbomScenarioWithFileAndOutputFormat(
      File testBomFile,
      SbomFormat outputFormat,
      ExportSpecification exportSpec)
  {
    defineDbTestData(thirdPartyFile, "");
    ThirdPartySbomMetadata sbomMetadata = insertTestData(testBomFile.getName(),
        thirdPartyFile);

    exporter.setExportParams(SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(exportSpec)
        .withTargetFormat(outputFormat));
    return exporter.export();
  }

  private String setupExportSbomScenarioWithNullDbVulnerabilityField(
      File testBomFile,
      SbomFormat outputFormat,
      String nullField,
      ExportSpecification exportSpec)
  {
    defineDbTestData(thirdPartyFile, nullField);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(testBomFile.getName(),
        thirdPartyFile);
    exporter.setExportParams(SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(exportSpec)
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
    ThirdPartySbomMetadata dbRecord = tempEntity.createSbomMetadata(app.getId(), SBOM_VERSION,
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
      String nullField,
      ExportSpecification exportSpec)
  {
    defineDbTestData(thirdPartyFile, nullField);
    mockDbRecordsWithComponentsMatchingByComponentRef(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(testBomFile.getName(),
        thirdPartyFile);
    exporter.setExportParams(SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(exportSpec)
        .withTargetFormat(outputFormat));
    return exporter.export();
  }

  private ThirdPartyFileCoordinate mockDbRecordsWithComponentsMatchingByComponentRef(ThirdPartyFile tpFile) {
    ThirdPartyFileCoordinate componentWithComponentRef = tempEntity.newThirdPartyFileCoordinate(tpFile,
        "Third-Party", "maven", "parentApp", "1.0-SNAPSHOT", "e33c095684013cced988",
        "pkg:maven/org.example/abc-component", "652faea012fd8be1371fbea623164ecc343c37df");
    tempEntity.newThirdPartyCoordinateSecurity(componentWithComponentRef, "ABC-123",
        "Test ABC vulnerability",
        "http://cve.mitre.org/cgi-bin/cvename.cgi?name=ABC-123", 5.5d, "HIGH", "NVD",
        " CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "HIGH", "502", "", "", "", "SBOM");
    tempEntity.newThirdPartyCoordinateLicense(componentWithComponentRef, "GPL-2.0", "GPL-2.0", "",
        "SBOM");

    return componentWithComponentRef;
  }
}
