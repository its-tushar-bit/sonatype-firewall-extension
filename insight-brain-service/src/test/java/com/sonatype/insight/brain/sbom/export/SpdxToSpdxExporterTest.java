/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomSpdxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.ThirdPartyUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.spdx.library.model.SpdxDocument;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.xmlunit.assertj.XmlAssert;

import static com.sonatype.insight.brain.sbom.export.SpdxDocumentAssert.assertThatSpdx;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.when;

public class SpdxToSpdxExporterTest
    extends AbstractSbomExporterTest
{
  private static final List<String> IGNORE_NODES = Arrays.asList("created", "name", "documentNamespace", "creators");

  @Inject
  private ThirdPartyVulnerabilityExploitabilityExchangeDAO vulnerabilityExploitabilityExchangeDAO;

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

  private SpdxToSpdxExporter spdxExporter;

  private Application app;

  @Before
  public void before() {
    spdxExporter = new SpdxToSpdxExporter(mockInsightWork, thirdPartyFileDAO, thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO, thirdPartyCoordinateLicenseDAO, vulnerabilityExploitabilityExchangeDAO,
        baseUrl, idUtils, versionService);
    when(baseUrl.get()).thenReturn("http://localhost:8070/");
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
            .withExportSpecification(ExportSpecification.SPDX_23)
            .withTargetFormat(SbomFormat.JSON);
    spdxExporter.setExportParams(exportParams);
    String export = spdxExporter.export();
    ThirdPartyUtils.parseAndValidateSpdx(export, SbomFormat.JSON);
    assertThatJson(export)
        .whenIgnoringPaths("creationInfo.created", "creationInfo.creators[0]", "documentNamespace", "name")
        .isEqualTo(readFileToString("outputs/output_spdx-v2_3.json"));
  }

  @Test
  public void testExport_Xml() throws Exception {
    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("spdx-v2_3.xml"));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tempEntity.newThirdPartyFile().getId(), app.getId(), "1.0.1", "ACTIVE",
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.XML.toString(), "2.3");
    SbomExportParams exportParams = SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(ExportSpecification.SPDX_23)
        .withTargetFormat(SbomFormat.XML);
    spdxExporter.setExportParams(exportParams);
    String export = spdxExporter.export();
    ThirdPartyUtils.parseAndValidateSpdx(export, SbomFormat.XML);
    XmlAssert.assertThat(export).and(readFileToString("outputs/output_spdx-v2_3.xml"))
        .withNodeFilter(node -> !IGNORE_NODES.contains(node.getNodeName()))
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
        .withExportSpecification(ExportSpecification.SPDX_23)
        .withTargetFormat(SbomFormat.JSON);
    spdxExporter.setExportParams(exportParams);
    String export = spdxExporter.export();
    SpdxDocument sbom = SbomSpdxUtils.parseContentNoValidation(export, SbomFormat.JSON);
    SpdxDocumentAssert documentAssert = assertThatSpdx(sbom)
        .isValid()
        .hasFormat(SbomFormat.JSON)
        .nameContains(app.getPublicId())
        .creationDateCloseTo(LocalDateTime.now(ZoneOffset.UTC))
        .creatorsContaining("Tool: Sonatype SBOM Manager")
        .equalsSpecVersion("2.3")
        .equalsDataLicense("CC0-1.0")
        .hasComponentCount(4)
        .hasPackagesWithPurls("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar",
            "pkg:maven/org.example/JavaApp@1.0-SNAPSHOT?type=jar")
        .hasVulnerabilityCount(3);
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar")
        .hasVulnerabilityCount(1)
        .containsVulnerabilities("sonatype-2022-6438");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar")
        .hasVulnerabilityCount(2)
        .containsVulnerabilities("CVE-2022-42003", "CVE-2022-42004");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar")
        .hasVulnerabilityCount(0);
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
        .withExportSpecification(ExportSpecification.SPDX_23)
        .withTargetFormat(SbomFormat.XML);
    spdxExporter.setExportParams(exportParams);
    String export = spdxExporter.export();
    SpdxDocument sbom = SbomSpdxUtils.parseContentNoValidation(export, SbomFormat.XML);
    SpdxDocumentAssert documentAssert = assertThatSpdx(sbom)
        .isValid()
        .hasFormat(SbomFormat.XML)
        .nameContains(app.getPublicId())
        .creationDateCloseTo(LocalDateTime.now(ZoneOffset.UTC))
        .creatorsContaining("Tool: Sonatype SBOM Manager")
        .equalsSpecVersion("2.3")
        .equalsDataLicense("CC0-1.0")
        .hasComponentCount(4)
        .hasPackagesWithPurls("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar",
            "pkg:maven/org.example/JavaApp@1.0-SNAPSHOT?type=jar")
        .hasVulnerabilityCount(2);
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar")
        .hasConcludedLicense("Apache-2.0")
        .hasDeclaredLicense("Apache-2.0")
        .containsLicenses("Apache-2.0", "BSD-3-Clause");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar")
        .hasConcludedLicense("Apache-2.0")
        .hasDeclaredLicense("Apache-2.0")
        .containsLicenses("Apache-2.0", "MIT");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar")
        .hasConcludedLicense("Apache-2.0")
        .hasDeclaredLicense("Apache-2.0");
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
        .withExportSpecification(ExportSpecification.SPDX_23)
        .withTargetFormat(SbomFormat.XML);
    spdxExporter.setExportParams(exportParams);
    String export = spdxExporter.export();
    SpdxDocument sbom = SbomSpdxUtils.parseContentNoValidation(export, SbomFormat.XML);
    SpdxDocumentAssert documentAssert = assertThatSpdx(sbom)
        .isValid()
        .hasFormat(SbomFormat.XML)
        .nameContains(app.getPublicId())
        .creationDateCloseTo(LocalDateTime.now(ZoneOffset.UTC))
        .equalsSpecVersion("2.3")
        .equalsDataLicense("CC0-1.0")
        .hasComponentCount(4)
        .hasPackagesWithPurls("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar",
            "pkg:maven/org.example/JavaApp@1.0-SNAPSHOT?type=jar");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar")
        .hasConcludedLicense("Apache-2.0")
        .hasDeclaredLicense("Apache-2.0")
        .containsLicenses("Apache-2.0", "Sonatype-Private", "Not-Supported");
  }

  @Test
  public void testExport_VulnerabilityMissingSource() throws Exception {
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
        "http://localhost:8070/ui/links/vln/sonatype-2022-6438", 8.0, "High", null,
        "CVSS VectorCVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "High", "", "", "", "", "SONATYPE");

    SbomExportParams exportParams = SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(ExportSpecification.SPDX_23)
        .withTargetFormat(SbomFormat.JSON);
    spdxExporter.setExportParams(exportParams);
    String export = spdxExporter.export();
    SpdxDocument sbom = SbomSpdxUtils.parseContentNoValidation(export, SbomFormat.JSON);
    SpdxDocumentAssert documentAssert = assertThatSpdx(sbom)
        .isValid()
        .hasFormat(SbomFormat.JSON)
        .nameContains(app.getPublicId())
        .creationDateCloseTo(LocalDateTime.now(ZoneOffset.UTC))
        .creatorsContaining("Tool: Sonatype SBOM Manager")
        .equalsSpecVersion("2.3")
        .equalsDataLicense("CC0-1.0")
        .hasComponentCount(4)
        .hasPackagesWithPurls("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar",
            "pkg:maven/org.example/JavaApp@1.0-SNAPSHOT?type=jar")
        .hasVulnerabilityCount(3);
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar")
        .hasVulnerabilityCount(1)
        .containsVulnerabilities("sonatype-2022-6438");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar")
        .hasVulnerabilityCount(2)
        .containsVulnerabilities("CVE-2022-42003", "CVE-2022-42004");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar")
        .hasVulnerabilityCount(0);
  }

  @Test
  public void testExport_VulnerabilityMissingSourceAndLink() throws Exception {
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
        null , 8.0, "High", null,
        "CVSS VectorCVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "High", "", "", "", "", "SONATYPE");

    SbomExportParams exportParams = SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(ExportSpecification.SPDX_23)
        .withTargetFormat(SbomFormat.JSON);
    spdxExporter.setExportParams(exportParams);
    String export = spdxExporter.export();
    SpdxDocument sbom = SbomSpdxUtils.parseContentNoValidation(export, SbomFormat.JSON);
    SpdxDocumentAssert documentAssert = assertThatSpdx(sbom)
        .isValid()
        .hasFormat(SbomFormat.JSON)
        .nameContains(app.getPublicId())
        .creationDateCloseTo(LocalDateTime.now(ZoneOffset.UTC))
        .creatorsContaining("Tool: Sonatype SBOM Manager")
        .equalsSpecVersion("2.3")
        .equalsDataLicense("CC0-1.0")
        .hasComponentCount(4)
        .hasPackagesWithPurls("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar",
            "pkg:maven/org.example/JavaApp@1.0-SNAPSHOT?type=jar")
        .hasVulnerabilityCount(3);
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar")
        .hasVulnerabilityCount(1);
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar")
        .hasVulnerabilityCount(2)
        .containsVulnerabilities("CVE-2022-42003", "CVE-2022-42004");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar")
        .hasVulnerabilityCount(0);
  }

  @Test
  public void testExport_MisconfiguredBaseUrl() throws Exception {
    Map<String, Object> mockData = mockOriginalThirdPartyScan();
    ThirdPartyFile tpFile = (ThirdPartyFile) mockData.get("tpFile");
    ThirdPartyFileCoordinate core = (ThirdPartyFileCoordinate) mockData.get("core");
    when(baseUrl.get()).thenThrow(IllegalStateException.class);
    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("spdx-min.json"));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), "1.0-SNAPSHOT", "ACTIVE",
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.JSON.toString(), "2.3");
    //mock sonatype vulnerability
    tempEntity.newThirdPartyCoordinateSecurity(core, "sonatype-2022-6438",
        "Sonatype: The jackson-core package is vulnerable to a Denial of Service (DoS) attack.",
        null , 8.0, "High", null,
        "CVSS VectorCVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "High", "", "", "", "", "SONATYPE");

    SbomExportParams exportParams = SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(ExportSpecification.SPDX_23)
        .withTargetFormat(SbomFormat.JSON);
    spdxExporter.setExportParams(exportParams);
    String export = spdxExporter.export();
    SpdxDocument sbom = SbomSpdxUtils.parseContentNoValidation(export, SbomFormat.JSON);
    SpdxDocumentAssert documentAssert = assertThatSpdx(sbom)
        .isValid()
        .hasFormat(SbomFormat.JSON)
        .nameContains(app.getPublicId())
        .creationDateCloseTo(LocalDateTime.now(ZoneOffset.UTC))
        .creatorsContaining("Tool: Sonatype SBOM Manager")
        .equalsSpecVersion("2.3")
        .equalsDataLicense("CC0-1.0")
        .hasComponentCount(4)
        .hasPackagesWithPurls("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar",
            "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar",
            "pkg:maven/org.example/JavaApp@1.0-SNAPSHOT?type=jar")
        .hasVulnerabilityCount(2);
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar")
        .hasVulnerabilityCount(0);
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar")
        .hasVulnerabilityCount(2)
        .containsVulnerabilities("CVE-2022-42003", "CVE-2022-42004");
    documentAssert.hasPackageWithPurl("pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar")
        .hasVulnerabilityCount(0);
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
