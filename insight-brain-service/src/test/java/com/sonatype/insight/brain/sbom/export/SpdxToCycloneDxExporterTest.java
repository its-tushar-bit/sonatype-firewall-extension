/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.io.IOUtils;
import org.cyclonedx.model.Bom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.xmlunit.assertj.XmlAssert;

import static com.sonatype.insight.brain.sbom.export.CycloneDxDocumentAssert.assertThatCycloneDx;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.CYCLONEDX_IGNORE_METADATA_COMPONENT_PATH;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.CYCLONEDX_IGNORE_METADATA_TIMESTAMP_PATH;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

public class SpdxToCycloneDxExporterTest
    extends AbstractSbomExporterTest
{
  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  @Inject
  private ThirdPartyVulnerabilityExploitabilityExchangeDAO vulnerabilityExploitabilityExchangeDAO;

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

  private SpdxToCycloneDxExporter spdxToCycloneDxExporter;

  private Application app;

  @Before
  public void before() {
    spdxToCycloneDxExporter = new SpdxToCycloneDxExporter(mockInsightWork, multiLicenseDAO, thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO, thirdPartyCoordinateLicenseDAO, vulnerabilityExploitabilityExchangeDAO,
        baseUrl, idUtils, versionService);
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testExport_Json() throws Exception {
    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("spdx-v2_3.json"));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tempEntity.newThirdPartyFile().getId(), app.getId(), "1.0.1", "ACTIVE",
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.JSON.toString(), "2.3");
    SbomExportParams exportParams =
        SbomExportParams.newSbomExporterParams(sbomMetadata)
            .withExportSpecification(SbomExportParams.ExportSpecification.CYCLONEDX_15)
            .withTargetFormat(SbomFormat.JSON);
    spdxToCycloneDxExporter.setExportParams(exportParams);
    String expected = IOUtils.resourceToString("/" + getClass().getSimpleName() +
        "/outputs/" + "output_cdx-v1_5.json", Charset.defaultCharset());
    String actual = spdxToCycloneDxExporter.export();
    assertThatJson(actual)
        .whenIgnoringPaths(CYCLONEDX_IGNORE_METADATA_COMPONENT_PATH, CYCLONEDX_IGNORE_METADATA_TIMESTAMP_PATH)
        .isEqualTo(expected);
  }

  @Test
  public void testExport_XML() throws Exception {
    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("spdx-v2_3.xml"));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tempEntity.newThirdPartyFile().getId(), app.getId(), "1.0.1", "ACTIVE",
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.XML.toString(), "2.3");
    SbomExportParams exportParams = SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(SbomExportParams.ExportSpecification.CYCLONEDX_15)
        .withTargetFormat(SbomFormat.XML);
    spdxToCycloneDxExporter.setExportParams(exportParams);
    String actual = spdxToCycloneDxExporter.export();
    XmlAssert.assertThat(actual).and(readFileToString("outputs/output_cdx-v1_5.xml"))
        .withNodeFilter(node -> node.getParentNode().getNodeName().equals("metadata") &&
                (node.getNodeName().equals("component") || node.getNodeName().equals("timestamp")))
        .ignoreWhitespace()
        .areIdentical();
  }

  @Test
  public void testExport_MergedVulnerabilities() throws Exception {
    Map<String, Object> mockData = mockOriginalThirdPartyScan();
    ThirdPartyFile tpFile = (ThirdPartyFile) mockData.get("tpFile");
    ThirdPartyFileCoordinate core = (ThirdPartyFileCoordinate) mockData.get("core");

    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("spdx-min.json"));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), "1.0-SNAPSHOT", "ACTIVE",
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.JSON.toString(), "2.3");
    //mock sonatype vulnerability
    tempEntity.newThirdPartyCoordinateSecurity(core, "sonatype-2022-6438",
        "Sonatype: The jackson-core package is vulnerable to a Denial of Service (DoS) attack.",
        "http://localhost:8070/ui/links/vln/sonatype-2022-6438", 8.0, "High", "SONAYPE",
        "CVSS VectorCVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "High", "", "", "", "", "SONATYPE");

    SbomExportParams exportParams = SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(SbomExportParams.ExportSpecification.CYCLONEDX_15)
        .withTargetFormat(SbomFormat.JSON);
    spdxToCycloneDxExporter.setExportParams(exportParams);
    String export = spdxToCycloneDxExporter.export();
    Bom sbom = SbomCycloneDxUtils.parseContentNoValidation(export);
    CycloneDxDocumentAssert documentAssert = assertThatCycloneDx(sbom)
        .hasToolCreationInformation("Sonatype SBOM Manager", versionService.getFullVersion())
        .hasComponentDocumentDescribes(app.getPublicId())
        .creationDateCloseTo(LocalDateTime.now(ZoneOffset.UTC))
        .equalsSpecVersion("1.5")
        .hasComponentCount(4)
        .hasPackagesWithPurls("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar",
            "pkg:maven/org.example/JavaApp@1.0-SNAPSHOT?type=jar")
        .hasVulnerabilityCount(3);
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar")
        .hasVulnerabilityCount(sbom, 1)
        .containsVulnerabilities(sbom, "sonatype-2022-6438");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar")
        .hasVulnerabilityCount(sbom, 2)
        .containsVulnerabilities(sbom, "CVE-2022-42003", "CVE-2022-42004");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar")
        .hasVulnerabilityCount(sbom, 0);
  }

  @Test
  public void testExport_MergedLicenses() throws Exception {
    Map<String, Object> mockData = mockOriginalThirdPartyScan();
    ThirdPartyFile tpFile = (ThirdPartyFile) mockData.get("tpFile");
    ThirdPartyFileCoordinate core = (ThirdPartyFileCoordinate) mockData.get("core");
    ThirdPartyFileCoordinate databind = (ThirdPartyFileCoordinate) mockData.get("databind");

    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("spdx-min.json"));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), "1.0-SNAPSHOT", "ACTIVE",
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.JSON.toString(), "2.3");
    //mock sonatype vulnerability
    tempEntity.newThirdPartyCoordinateLicense(core, "BSD-3-Clause", "BSD-3-Clause", "", "SONATYPE");
    tempEntity.newThirdPartyCoordinateLicense(databind, "MIT", "MIT", "", "SONATYPE");

    SbomExportParams exportParams = SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(SbomExportParams.ExportSpecification.CYCLONEDX_15)
        .withTargetFormat(SbomFormat.XML);
    spdxToCycloneDxExporter.setExportParams(exportParams);
    String export = spdxToCycloneDxExporter.export();
    Bom sbom = SbomCycloneDxUtils.parseContentNoValidation(export);
    CycloneDxDocumentAssert documentAssert = assertThatCycloneDx(sbom)
        .hasComponentDocumentDescribes(app.getPublicId())
        .hasToolCreationInformation("Sonatype SBOM Manager", versionService.getFullVersion())
        .creationDateCloseTo(LocalDateTime.now(ZoneOffset.UTC))
        .equalsSpecVersion("1.5")
        .hasComponentCount(4)
        .hasPackagesWithPurls("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar",
            "pkg:maven/org.example/JavaApp@1.0-SNAPSHOT?type=jar")
        .hasVulnerabilityCount(2);
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar")
        .hasLicenseCount(2)
        .containsLicenses("Apache-2.0", "BSD-3-Clause");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar")
        .hasLicenseCount(2)
        .containsLicenses("Apache-2.0", "MIT");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar")
        .hasLicenseCount(1);
  }

  @Test
  public void testExport_MergedLicenses_UnsupportedLicenseIds() throws Exception {
    Map<String, Object> mockData = mockOriginalThirdPartyScan();
    ThirdPartyFile tpFile = (ThirdPartyFile) mockData.get("tpFile");
    ThirdPartyFileCoordinate databind = (ThirdPartyFileCoordinate) mockData.get("databind");

    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("spdx-min.json"));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), "1.0-SNAPSHOT", "ACTIVE",
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.JSON.toString(), "2.3");
    //mock sonatype vulnerability
    tempEntity.newThirdPartyCoordinateLicense(databind, "Not-Supported", "Not Supported", "", "SONATYPE");
    tempEntity.newThirdPartyCoordinateLicense(databind, "Sonatype-Private", "Sonatype Private", "", "SONATYPE");

    SbomExportParams exportParams = SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(SbomExportParams.ExportSpecification.CYCLONEDX_15)
        .withTargetFormat(SbomFormat.XML);
    spdxToCycloneDxExporter.setExportParams(exportParams);
    String export = spdxToCycloneDxExporter.export();
    Bom sbom = SbomCycloneDxUtils.parseContentNoValidation(export);
    CycloneDxDocumentAssert documentAssert = assertThatCycloneDx(sbom)
        .hasComponentDocumentDescribes(app.getPublicId())
        .hasToolCreationInformation("Sonatype SBOM Manager", versionService.getFullVersion())
        .creationDateCloseTo(LocalDateTime.now(ZoneOffset.UTC))
        .equalsSpecVersion("1.5")
        .hasComponentCount(4)
        .hasPackagesWithPurls("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar",
            "pkg:maven/org.example/JavaApp@1.0-SNAPSHOT?type=jar");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar")
        .hasLicenseCount(3)
        .containsLicenses("Apache-2.0", "Sonatype-Private", "Not-Supported");
  }

  private Map<String, Object> mockOriginalThirdPartyScan() {
    //scan
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(tpFile);
    //coordinates
    ThirdPartyFileCoordinate databind =
        tempEntity.newThirdPartyFileCoordinate(tpFile, "Third-Party", "maven", "jackson-databind", "2.13.3",
            "2dc096121af49cea9299", "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar");
    ThirdPartyFileCoordinate annotations =
        tempEntity.newThirdPartyFileCoordinate(tpFile, "Third-Party", "maven", "jackson-annotations", "2.13.3",
            "9644625c9e61df62e89a", "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar");
    ThirdPartyFileCoordinate core =
        tempEntity.newThirdPartyFileCoordinate(tpFile, "Third-Party", "maven", "jackson-core", "2.13.3",
            "e4d1890a31abcc38566b", "pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar");
    tempEntity.newThirdPartyFileCoordinate(tpFile, "Third-Party", "maven", "parentApp", "1.0-SNAPSHOT",
        "e33c095684013cced9f4", "pkg:maven/org.example/JavaPlay@1.0-SNAPSHOT?type=jar");
    //security
    tempEntity.newThirdPartyCoordinateSecurity(databind, "CVE-2022-42003",
        "In FasterXML jackson-databind before versions 2.13.4.1 and 2.12.17.1, resource exhaustion can occur.",
        "http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-42003", 7.5d, "HIGH", "NVD",
        " CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "HIGH", "502", "", "", "", "SBOM");
    tempEntity.newThirdPartyCoordinateSecurity(databind, "CVE-2022-42004",
        "FasterXML jackson-databind before 2.13.4, resource exhaustion can occur due to lack of a check",
        "http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-42004", 7.5d, "HIGH", "NVD",
        " CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "HIGH", "502", "", "", "", "SBOM");
    //license
    tempEntity.newThirdPartyCoordinateLicense(databind, "Apache-2.0", "Apache-2.0", "", "SBOM");
    tempEntity.newThirdPartyCoordinateLicense(annotations, "Apache-2.0", "Apache-2.0", "", "SBOM");
    tempEntity.newThirdPartyCoordinateLicense(core, "Apache-2.0", "Apache-2.0", "", "SBOM");
    return ImmutableMap.of("tpFile", tpFile, "core", core, "databind", databind);
  }
}
