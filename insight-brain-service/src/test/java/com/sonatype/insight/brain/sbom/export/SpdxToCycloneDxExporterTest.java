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
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.io.IOUtils;
import org.cyclonedx.model.Bom;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import com.google.common.collect.ImmutableMap;
import org.xmlunit.assertj.XmlAssert;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.cycloneDxIgnoreAttributesFilter;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.cycloneDxIgnoreNodesFilter;
import static com.sonatype.insight.brain.sbom.export.CycloneDxDocumentAssert.assertThatCycloneDx;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

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

  @Inject
  ApplicationDAO applicationDAO;

  @Mock
  private BaseUrl baseUrl;

  @Inject
  private IdUtils idUtils;

  @Inject
  private VersionService versionService;

  @Inject
  ApiReportDataServiceV2 apiReportDataServiceV2;

  private SpdxToCycloneDxExporter spdxToCycloneDxExporter;

  private Application app;

  @Before
  public void before() {
    spdxToCycloneDxExporter = new SpdxToCycloneDxExporter(
        multiLicenseDAO,
        thirdPartyFileDAO,
        thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO,
        thirdPartyScanDAO,
        applicationDAO,
        vulnerabilityExploitabilityExchangeDAO,
        migrationTrackerDAO,
        baseUrl,
        idUtils,
        versionService,
        apiReportDataServiceV2,
        licenseResolutionService,
        buildThirdPartyPersistenceService());
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testExport_Json_2_3() throws Exception {
    assertJson("spdx-v2_3.json", ExportSpecification.CYCLONEDX_15, "2.3", "output_cdx-v1_5.json");
  }

  @Test
  public void testExport_Json_2_2() throws Exception {
    assertJson("spdx-v2_2.json", ExportSpecification.CYCLONEDX_16, "2.2", "output_cdx-v1_6.json");
  }

  private void assertJson(
      String fileName,
      ExportSpecification exportSpecification,
      String importVersion,
      String outputFileName) throws Exception
  {
    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(fileName));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tempEntity.newThirdPartyFile().getId(), app.getId(), "1.0.1", ACTIVE,
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.JSON.toString(), importVersion);
    SbomExportParams exportParams =
        SbomExportParams.newSbomExporterParams(sbomMetadata)
            .withExportSpecification(exportSpecification)
            .withTargetFormat(SbomFormat.JSON);
    spdxToCycloneDxExporter.setExportParams(exportParams);
    String expected = IOUtils.resourceToString("/" + getClass().getSimpleName() +
        "/outputs/" + outputFileName, Charset.defaultCharset());
    String actual = spdxToCycloneDxExporter.export();
    assertThatJson(actual)
        .whenIgnoringPaths("components[*].licenses[*].license.bom-ref")
        .withOptions(IGNORING_ARRAY_ORDER)
        .isEqualTo(expected);
  }

  @Test
  public void testExport_XML_2_2() throws Exception {
    assertXml("spdx-v2_2.xml", ExportSpecification.CYCLONEDX_16, "2.2", "output_cdx-v1_6.xml");
  }

  @Test
  public void testExport_XML_2_3() throws Exception {
    assertXml("spdx-v2_3.xml", ExportSpecification.CYCLONEDX_15, "2.3", "output_cdx-v1_5.xml");
  }

  private void assertXml(
      String fileName,
      ExportSpecification exportSpecification,
      String importVersion,
      String outputFileName) throws Exception
  {
    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(fileName));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tempEntity.newThirdPartyFile().getId(), app.getId(), "1.0.1", ACTIVE,
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.XML.toString(), importVersion);
    SbomExportParams exportParams = SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(exportSpecification)
        .withTargetFormat(SbomFormat.XML);
    spdxToCycloneDxExporter.setExportParams(exportParams);
    String actual = spdxToCycloneDxExporter.export();
    XmlAssert.assertThat(actual)
        .and(readFileToString("outputs/" + outputFileName))
        .withNodeFilter(cycloneDxIgnoreNodesFilter())
        .withAttributeFilter(cycloneDxIgnoreAttributesFilter())
        .withNodeMatcher(new DefaultNodeMatcher(
            ElementSelectors.conditionalBuilder()
                .whenElementIsNamed("vulnerability")
                .thenUse(ElementSelectors.and(
                    ElementSelectors.byName,
                    ElementSelectors.byXPath("./*[local-name()='id']", ElementSelectors.byNameAndText)))
                .elseUse(ElementSelectors.byName)
                .build()))
        .ignoreWhitespace()
        .areSimilar();
  }

  @Test
  public void testExport_componentWithSimilarMatchStateProperty() throws Exception {
    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("spdx-v2_3.xml"));

    ThirdPartyFile tpf = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpf.getId(), app.getId(), "1.0.1", ACTIVE,
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.XML.toString(), "2.3");

    tempEntity.newThirdPartyFileCoordinateWithMatchState(tpf,
        "source",
        "maven",
        "log4j-core",
        "2.13.2",
        "abcdef",
        "pkg:maven/org.apache.logging.log4j/log4j-core@2.13.2?type=jar",
        "log4j-core-2.13.2.jar",
        "similar");

    tempEntity.newThirdPartyFileCoordinateWithMatchState(tpf,
        "source",
        "maven",
        "junit",
        "4.12",
        "1111111",
        "pkg:maven/junit/junit@4.12?type=jar",
        "junit-4.12.jar",
        "exact");

    SbomExportParams exportParams = SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(SbomExportParams.ExportSpecification.CYCLONEDX_15)
        .withTargetFormat(SbomFormat.XML);
    spdxToCycloneDxExporter.setExportParams(exportParams);
    String actual = spdxToCycloneDxExporter.export();
    XmlAssert.assertThat(actual)
        .and(readFileToString("outputs/output_cdx-v_1_5-similar-components.xml"))
        .withNodeFilter(cycloneDxIgnoreNodesFilter())
        .withNodeFilter(n -> n.getNodeName().equals("vulnerabilities"))
        .withAttributeFilter(cycloneDxIgnoreAttributesFilter())
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
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), "1.0-SNAPSHOT", ACTIVE,
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.JSON.toString(), "2.3");
    // mock sonatype vulnerability
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
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), "1.0-SNAPSHOT", ACTIVE,
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.JSON.toString(), "2.3");
    // mock sonatype vulnerability
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
  public void testExport_withLicenseOverrides_forLifeCycleProduct() throws Exception {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    testExport_withLicenseOverrides("GPL-3.0", "Aladdin");
  }

  @Test
  public void testExport_withLicenseOverrides_forSbomAndALPProduct() throws Exception {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK);
    testExport_withLicenseOverrides("GPL-3.0", "Aladdin");
  }

  @Test
  public void testExport_withLicenseOverrides_forSbomProduct() throws Exception {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    testExport_withLicenseOverrides("Apache-2.0");
  }

  private void testExport_withLicenseOverrides(final String... expected) throws Exception {
    Map<String, Object> mockData = mockOriginalThirdPartyScan();
    ThirdPartyFile tpFile = (ThirdPartyFile) mockData.get("tpFile");
    ThirdPartyFileCoordinate core = (ThirdPartyFileCoordinate) mockData.get("core");

    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("spdx-min.json"));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), "1.0-SNAPSHOT", ACTIVE,
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.JSON.toString(), "2.3");
    ComponentIdentifier id = new PackageUrlIdentifier(core.getPackageUrl()).toComponentIdentifier();
    id.ensureComplete();
    // mock license override
    tempEntity.newLicenseOverride(app.getId(), id, LicenseOverrideStatus.OVERRIDDEN, Set.of("GPL-3.0", "Aladdin"));

    SbomExportParams exportParams = SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportSpecification(SbomExportParams.ExportSpecification.CYCLONEDX_15)
        .withTargetFormat(SbomFormat.XML);
    spdxToCycloneDxExporter.setExportParams(exportParams);
    String export = spdxToCycloneDxExporter.export();
    Bom sbom = SbomCycloneDxUtils.parseContentNoValidation(export);
    assertThatCycloneDx(sbom).hasPackageWithPurl(core.getPackageUrl())
        .hasLicenseCount(expected.length)
        .containsLicenses(expected);
  }

  @Test
  public void testExport_MergedLicenses_UnsupportedLicenseIds() throws Exception {
    Map<String, Object> mockData = mockOriginalThirdPartyScan();
    ThirdPartyFile tpFile = (ThirdPartyFile) mockData.get("tpFile");
    ThirdPartyFileCoordinate databind = (ThirdPartyFileCoordinate) mockData.get("databind");

    File sbomFile = mockSbomFileForApp(app.getId(), getGZippedSbom("spdx-min.json"));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), "1.0-SNAPSHOT", ACTIVE,
            sbomFile.getName(), SbomSpecification.SPDX.toString(), SbomFormat.JSON.toString(), "2.3");
    // mock sonatype vulnerability
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
        .containsLicenses("Apache-2.0")
        .containsNotListedLicenses("Sonatype-Private", "Not-Supported");

  }

  private Map<String, Object> mockOriginalThirdPartyScan() {
    // scan
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(tpFile);
    // coordinates
    ThirdPartyFileCoordinate databind =
        tempEntity.newThirdPartyFileCoordinate(tpFile, "Third-Party", "maven", "jackson-databind", "2.13.3",
            "2dc096121af49cea9299", "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.3?type=jar",
            "0d4c0224ba6b5abe2f7c9368771bde469a7c73ae");
    ThirdPartyFileCoordinate annotations =
        tempEntity.newThirdPartyFileCoordinate(tpFile, "Third-Party", "maven", "jackson-annotations", "2.13.3",
            "9644625c9e61df62e89a", "pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.13.3?type=jar",
            "f8f51717d2a3fdc7e4972f081d6d4ba3148eb7c1");
    ThirdPartyFileCoordinate core =
        tempEntity.newThirdPartyFileCoordinate(tpFile, "Third-Party", "maven", "jackson-core", "2.13.3",
            "e4d1890a31abcc38566b", "pkg:maven/com.fasterxml.jackson.core/jackson-core@2.13.3?type=jar",
            "ab4b99bf70ab45d4e745165b2d2d8f81fe53ff7e");
    tempEntity.newThirdPartyFileCoordinate(tpFile, "Third-Party", "maven", "parentApp", "1.0-SNAPSHOT",
        "e33c095684013cced9f4", "pkg:maven/org.example/JavaPlay@1.0-SNAPSHOT?type=jar");
    // security
    tempEntity.newThirdPartyCoordinateSecurity(databind, "CVE-2022-42003",
        "In FasterXML jackson-databind before versions 2.13.4.1 and 2.12.17.1, resource exhaustion can occur.",
        "http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-42003", 7.5d, "HIGH", "NVD",
        " CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "HIGH", "502", "", "", "", "SBOM");
    tempEntity.newThirdPartyCoordinateSecurity(databind, "CVE-2022-42004",
        "FasterXML jackson-databind before 2.13.4, resource exhaustion can occur due to lack of a check",
        "http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-42004", 7.5d, "HIGH", "NVD",
        " CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "HIGH", "502", "", "", "", "SBOM");
    // license
    tempEntity.newThirdPartyCoordinateLicense(databind, "Apache-2.0", "Apache-2.0", "", "SBOM");
    tempEntity.newThirdPartyCoordinateLicense(annotations, "Apache-2.0", "Apache-2.0", "", "SBOM");
    tempEntity.newThirdPartyCoordinateLicense(core, "Apache-2.0", "Apache-2.0", "", "SBOM");
    return ImmutableMap.of("tpFile", tpFile, "core", core, "databind", databind);
  }
}
