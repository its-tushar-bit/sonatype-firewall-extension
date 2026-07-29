/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.SbomTaxonomy;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.ThirdPartyUtils;

import org.apache.shiro.util.CollectionUtils;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.Justification;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.Response;
import org.cyclonedx.model.vulnerability.Vulnerability.Analysis.State;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.sbom.SbomTestHelper.CYCLONEDX_JSON_IGNORE_FIELDS;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CycloneDxToCycloneDxExporterTest
    extends AbstractSbomExporterTest
{
  private CycloneDxToCycloneDxExporter exporter;

  private static final String SBOM_VERSION = "v1";

  private static final String THIRD_PARTY_FILE = "testBom.json";

  private static final String TEST_XML_SBOM = "webgoat-bom.xml";

  private static final String TEST_JSON_SBOM = "webgoat-bom.json";

  private static final String SCAN_ID = "sid1";

  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  @Inject
  ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Inject
  ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Inject
  ApplicationDAO applicationDAO;

  @Inject
  ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  @Inject
  ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  private IdUtils idUtils;

  @Inject
  private BaseUrl baseUrl;

  @Inject
  private VersionService versionService;

  private ThirdPartyFile thirdPartyFile;

  private Application app;

  @Before
  public void init() throws SbomExportException {
    app = tempEntity.newApplicationWithParent();
    appId = app.getId();
    thirdPartyFile = tempEntity.newThirdPartyFile(THIRD_PARTY_FILE);

    exporter = new CycloneDxToCycloneDxExporter(
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
  public void exportTest_withXmlInputFormat_toXmlOutputFormat() throws Exception {
    testExportingWebgoatAppWithInputFormatAndOutputFormat(SbomFormat.XML, SbomFormat.XML);
  }

  @Test
  public void exportTest_withXmlInputFormat_toJsonOutputFormat() throws Exception {
    testExportingWebgoatAppWithInputFormatAndOutputFormat(SbomFormat.XML, SbomFormat.JSON);
  }

  @Test
  public void exportTest_withJsonInputFormat_toXmlOutputFormat() throws Exception {
    testExportingWebgoatAppWithInputFormatAndOutputFormat(SbomFormat.JSON, SbomFormat.XML);
  }

  @Test
  public void exportTest_withJsonInputFormat_toJsonOutputFormat() throws Exception {
    testExportingWebgoatAppWithInputFormatAndOutputFormat(SbomFormat.JSON, SbomFormat.JSON);
  }

  @Test
  public void exportTest_withLicenseOverrides_forLifecycleProduct() throws Exception {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    exportTest_withLicenseOverrides("GPL-3.0", "Artistic-2.0");
  }

  @Test
  public void exportTest_withLicenseOverrides_forSbomAndALPProduct() throws Exception {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER,
        ProductLicenseDetails.PRODUCT_ADVANCED_LEGAL_PACK);
    exportTest_withLicenseOverrides("GPL-3.0", "Artistic-2.0");
  }

  @Test
  public void exportTest_withLicenseOverrides_forSbomProduct() throws Exception {
    productLicense.setProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    exportTest_withLicenseOverrides("Apache-1.1", null, "Aladdin", "MIT");
  }

  private void exportTest_withLicenseOverrides(final String... expected) throws Exception {
    String testFileName = TEST_JSON_SBOM;
    File testBomFile = prepareTestReportFile(testFileName);

    defineDbTestData(thirdPartyFile);
    String purl = "pkg:maven/log4j/log4j@1.2.8?type=jar";
    ComponentIdentifier cid = new PackageUrlIdentifier(purl).toComponentIdentifier();
    cid.ensureComplete();
    tempEntity.newLicenseOverride(appId, cid, LicenseOverrideStatus.OVERRIDDEN, Set.of("GPL-3.0", "Artistic-2.0"));
    ThirdPartySbomMetadata sbomMetadata = insertTestData(appId, SBOM_VERSION, testBomFile.getName(),
        thirdPartyFile);
    mockOriginalBom(testBomFile);

    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_15, SbomFormat.JSON));
    String exportedBomStr = exporter.export();
    assertExportedBomString(exportedBomStr);

    Bom exportedNewBom = ThirdPartyUtils.parseAndValidateCycloneDx(exportedBomStr, SbomFormat.JSON);
    List<Component> exportedComponents = exportedNewBom.getComponents();
    Component exportedLog4j = findBomComponent(purl, exportedComponents);

    assertThat(exportedLog4j.getLicenses().getLicenses()).extracting("id").containsExactlyInAnyOrder(expected);
  }

  @Test
  public void exportTest_NoVulnerabilitiesInOriginalBom() throws Exception {
    String testFileName = "test-bom.xml";
    File testBomFile = prepareTestReportFile(testFileName);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(appId, SBOM_VERSION, testBomFile.getName(), thirdPartyFile);
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_15, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "source",
        "maven",
        "log4j",
        "1.2.8",
        "abcdef",
        "pkg:maven/log4j/log4j@1.2.8?type=jar");
    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "sonatype-2010-0053", "DESC sonatype-2010-0053", "l1", 5.5d,
        "1.1", "source", "v:1", "Medium", "1234",
        "m1", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
    String export = exporter.export();
    assertThatJson(export)
        .whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS)
        .isEqualTo(readFileToString("outputs/output-test-bom.json"));
  }

  @Test
  public void exportTest_ComponentRef_MatchByBomRef() throws Exception {
    String testFileName = "test-bom-ref-as-component-ref.xml";
    File testBomFile = prepareTestReportFile(testFileName);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(appId, SBOM_VERSION, testBomFile.getName(), thirdPartyFile);
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_16, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "source",
        "maven",
        "log4j",
        "1.2.8",
        "abcdef",
        null,
        "69ea90fe09efe94a7e53e1989a3addc2decf3833");
    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "sonatype-2010-0053", "DESC sonatype-2010-0053", "l1", 5.5d,
        "1.1", "source", "v:1", "Medium", "1234",
        "m1", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
    String export = exporter.export();
    assertThatJson(export)
        .whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS)
        .isEqualTo(readFileToString("outputs/output-bom-ref-used-as-component-ref.json"));
  }

  @Test
  public void exportTest_ComponentRef_MatchByComponentIdentity() throws Exception {
    List<String> ignoreFields = new ArrayList<>(List.of(CYCLONEDX_JSON_IGNORE_FIELDS));
    ignoreFields.addAll(List.of("components[*].bom-ref", "vulnerabilities[*].affects[*].ref"));
    String testFileName = "test-component-identity-as-component-ref.xml";
    File testBomFile = prepareTestReportFile(testFileName);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(appId, SBOM_VERSION, testBomFile.getName(), thirdPartyFile);
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_15, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "source",
        "maven",
        "log4j",
        "1.2.8",
        "abcdef",
        null,
        "99de859bd6977b14fe90b8283ccd6803b34305d4");
    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "sonatype-2010-0053", "DESC sonatype-2010-0053", "l1", 5.5d,
        "1.1", "source", "v:1", "Medium", "1234",
        "m1", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
    String export = exporter.export();
    assertThatJson(export)
        .whenIgnoringPaths(ignoreFields.toArray(new String[0]))
        .isEqualTo(readFileToString("outputs/output-component-identity-used-as-component-ref.json"));
  }

  @Test
  public void exportTest_ContainerScan() throws Exception {
    String testFileName = "test-container-bom.json";
    File testBomFile = prepareTestReportFile(testFileName);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(appId, SBOM_VERSION, testBomFile.getName(), thirdPartyFile);
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.DEFAULT, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "Sonatype-Container",
        "container",
        "pam/libpam-modules-bin",
        "1.5.2-6+deb12u1",
        "abcdef",
        "pkg:generic/debian%3A12/pam%2Flibpam-modules-bin@1.5.2-6%2Bdeb12u1?nexustype=container");
    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "CVE-2024-10041", "DESC CVE-2024-10041", "l1", 4.7d,
        "1.1", "Sonatype-Container", "CVSS:3.1/AV:L/AC:H/PR:L/UI:N/S:U/C:H/I:N/A:N",
        "Medium", "1234", "other", "<dd>r1<dd/>",
        "<dd>a1<dd/>", "SBOM");
    String export = exporter.export();
    assertThatJson(export)
        .whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS)
        .isEqualTo(readFileToString("outputs/test-container-expected-bom.json"));
  }

  @Test
  public void exportTest_nonNumericCwesReturnedByHDS() throws Exception {
    String testFileName = "test-bom.xml";
    File testBomFile = prepareTestReportFile(testFileName);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(appId, SBOM_VERSION, testBomFile.getName(), thirdPartyFile);
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_15, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "source",
        "maven",
        "log4j",
        "1.2.8",
        "abcdef",
        "pkg:maven/log4j/log4j@1.2.8?type=jar");
    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "sonatype-2010-0053", "DESC sonatype-2010-0053", "l1", 5.5d,
        "1.1", "source", "v:1", "Medium", "noinfo",
        "m1", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "sonatype-2010-1234", "Some nasty vulnerability", "l1", 10d,
        "1.2", "source", "v:1", "Medium", "1973",
        "m1", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
    String export = exporter.export();
    assertThatJson(export)
        .whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS)
        .isEqualTo(readFileToString("outputs/output-test-bom-3.json"));
  }

  @Test
  public void exportTest_VulnerabilityRatingFieldsPreserved() throws Exception {
    String testFileName = "test-bom.xml";
    File testBomFile = prepareTestReportFile(testFileName);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(appId, SBOM_VERSION, testBomFile.getName(), thirdPartyFile);
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_15, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "source",
        "maven",
        "log4j",
        "1.2.8",
        "abcdef",
        "pkg:maven/log4j/log4j@1.2.8?type=jar");
    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "CVE-2022-23307", "DESC CVE-2022-23307", "NVD-link", 5.5d,
        "1.1", "NVD", "CVSS:3.1", "Medium", "1234",
        "cvSSv3", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
    String export = exporter.export();
    assertThatJson(export)
        .whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS)
        .isEqualTo(readFileToString("outputs/output-test-bom-2.json"));
  }

  @Test
  public void exportTest_componentWithSimilarMatchStateProperty() throws Exception {
    String testFileName = "test-similar-match-bom.xml";
    File testBomFile = prepareTestReportFile(testFileName);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(appId, SBOM_VERSION, testBomFile.getName(), thirdPartyFile);
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_15, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinateWithMatchState(thirdPartyFile,
        "source",
        "maven",
        "log4j",
        "1.2.8",
        "abcdef",
        "pkg:maven/log4j/log4j@1.2.8?type=jar",
        "log4j-1.2.8.jar",
        "similar");
    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "CVE-2022-23307", "DESC CVE-2022-23307", "NVD-link", 5.5d,
        "1.1", "NVD", "CVSS:3.1", "Medium", "1234",
        "cvSSv3", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
    String export = exporter.export();
    assertThatJson(export)
        .whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS)
        .isEqualTo(readFileToString("outputs/output-test-bom-4.json"));
  }

  @Test
  public void exportTest_componentWithExactMatchStateProperty() throws Exception {
    String testFileName = "test-bom.xml";
    File testBomFile = prepareTestReportFile(testFileName);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(appId, SBOM_VERSION, testBomFile.getName(), thirdPartyFile);
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_15, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinateWithMatchState(thirdPartyFile,
        "source",
        "maven",
        "log4j",
        "1.2.8",
        "abcdef",
        "pkg:maven/log4j/log4j@1.2.8?type=jar",
        "log4j-1.2.8.jar",
        "exact");
    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "CVE-2022-23307", "DESC CVE-2022-23307", "NVD-link", 5.5d,
        "1.1", "NVD", "CVSS:3.1", "Medium", "1234",
        "cvSSv3", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
    String export = exporter.export();
    assertThatJson(export)
        .whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS)
        .isEqualTo(readFileToString("outputs/output-test-bom-5.json"));
  }

  @Test
  public void testExport_VulnerabilitiesWithMultipleAffects_WithVex() throws Exception {
    // Given
    String testFileName = "test-bom-duplicate-vuln.xml";
    File testBomFile = prepareTestReportFile(testFileName);
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(appId, SBOM_VERSION, testBomFile.getName(), thirdPartyFile);
    ThirdPartyFileCoordinate cp1 =
        tempEntity.newThirdPartyFileCoordinateWithMatchState(thirdPartyFile, "SBOM", "npm", "vue", "2.2.4", "2b0949b",
            "pkg:npm/vue@2.2.4", "", "exact");
    ThirdPartyFileCoordinate cp2 =
        tempEntity.newThirdPartyFileCoordinateWithMatchState(thirdPartyFile, "SBOM", "npm", "vue", "2.2.5", "2b0949b",
            "pkg:npm/vue@2.2.5", "", "exact");
    ThirdPartyFileCoordinate cp3 =
        tempEntity.newThirdPartyFileCoordinateWithMatchState(thirdPartyFile, "SBOM", "npm", "vue", "2.2.6", "2b0949b",
            "pkg:npm/vue@2.2.6", "", "exact");

    ThirdPartyCoordinateSecurity v1cp1 = tempEntity.newThirdPartyCoordinateSecurity(
        cp1, "CVE-2018-6341", "DESC CVE-2018-6341", "NVD-link", 6.1d,
        null, "NVD", "CVSS:3.1", "MEDIUM", "79",
        "CVSSV3", null, null, "SBOM,Sonatype");
    tempEntity.newThirdPartyCoordinateSecurity(
        cp1, "sonatype-2018-0504", "DESC sonatype-2018-0504", "NVD-link", 5.4d,
        null, "SONATYPE", "CVSS:3.1", "HIGH", "400",
        "OTHER", null, null, "Sonatype");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(v1cp1,
        "CVE-2018-6341", "resolved", "code_not_present", "update",
        "VEX detail");

    tempEntity.newThirdPartyCoordinateSecurity(
        cp2, "CVE-2018-6341", "DESC CVE-2018-6341", "NVD-link", 6.1d,
        null, "NVD", "CVSS:3.1", "MEDIUM", "79",
        "CVSSV3", null, null, "SBOM,Sonatype");
    tempEntity.newThirdPartyCoordinateSecurity(
        cp2, "sonatype-2018-0504", "DESC sonatype-2018-0504", "NVD-link", 5.4d,
        null, "SONATYPE", "CVSS:3.1", "HIGH", "400",
        "OTHER", null, null, "Sonatype");

    tempEntity.newThirdPartyCoordinateSecurity(
        cp3, "CVE-2018-6341", "DESC CVE-2018-6341", "NVD-link", 6.1d,
        null, "NVD", "CVSS:3.1", "MEDIUM", "79",
        "CVSSV3", null, null, "SBOM,Sonatype");

    // when
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_16, SbomFormat.JSON));
    String export = exporter.export();

    // Then
    assertThatJson(export)
        .whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS)
        .isEqualTo(readFileToString("outputs/output-test-bom-duplicate-vuln.json"));
  }

  @Test
  public void testExport_specificSbomVersion_doNotExportOriginalSbomVexInfoWhenNoSonatypeDbRecordsFound() throws Exception {
    // Given
    String testFileName = "test-bom-with-initial-vex.json";
    File testBomFile = mockSbomFileForApp(app.getId(), getGZippedSbom(testFileName));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        insertTestData(app.getId(), SBOM_VERSION, testBomFile.getName(), thirdPartyFile);
    ThirdPartyFileCoordinate cp1 =
        tempEntity.newThirdPartyFileCoordinateWithMatchState(thirdPartyFile, "SBOM", "maven",
            "org.springframework.boot:spring-boot-autoconfigure", "2.0.3.RELEASE", "2b0949b",
            "pkg:maven/org.springframework.boot/spring-boot-autoconfigure@2.0.3.RELEASE?type=jar", "", "exact");

    // Define some new vulnerability to enforce merge
    ThirdPartyCoordinateSecurity v1cp1 = tempEntity.newThirdPartyCoordinateSecurity(
        cp1, "CVE-2018-6341", "DESC CVE-2018-6341", "NVD-link", 6.1d,
        null, "NVD", "CVSS:3.1", "MEDIUM", "79",
        "CVSSV3", null, null, "SBOM,Sonatype");

    // Insert in db record of the vulnerability coming from the original SBOM
    ThirdPartyCoordinateSecurity vulnWithOriginalVexInfoInBom = tempEntity.newThirdPartyCoordinateSecurity(
        cp1, "CVE-TEST", null, "http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-TEST", 7.5d,
        null, "NVD", "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H",
        "high", "400", "CVSSV3", null, null,
        "SBOM");

    // Define VEX records
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(v1cp1,
        "CVE-2018-6341", "resolved", "code_not_present", "update",
        "VEX detail");

    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnWithOriginalVexInfoInBom,
        "CVE-TEST", "exploitable", "code_not_present", "can_not_fix",
        "TEST-ORIGINAL-VEX-INFO");

    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_16, SbomFormat.JSON));
    String export = exporter.export();

    // Assess the exported bom with augmented data from db
    assertThatJson(export)
        .whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS)
        .isEqualTo(readFileToString("outputs/output-bom-with-vex-exported.json"));

    // Delete all vex info stored in the db associated with this bom and component
    List<ThirdPartyCoordinateSecurity> v1p1Vulnerabilities = thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(cp1
        .getId());
    List<String> vulnIds = v1p1Vulnerabilities.stream()
        .map(ThirdPartyCoordinateSecurity::getId)
        .collect(
            Collectors.toList());
    List<ThirdPartyVulnerabilityExploitabilityExchange> vexAnnotationsInDB =
        thirdPartyVulnerabilityExploitabilityExchangeDAO.getListByCoordinateSecurityIds(vulnIds);
    vexAnnotationsInDB.forEach(vex -> thirdPartyVulnerabilityExploitabilityExchangeDAO
        .delete(vex));

    // Export again after deleting all vex info in db
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_16, SbomFormat.JSON));
    export = exporter.export();

    // Check we are not exporting the original bom vex info, no vex info should be available in the exported file
    assertThatJson(export)
        .whenIgnoringPaths(CYCLONEDX_JSON_IGNORE_FIELDS)
        .isEqualTo(readFileToString("outputs/output-bom-without-vex-exported.json"));
  }

  private void testExportingWebgoatAppWithInputFormatAndOutputFormat(
      SbomFormat inputFormat,
      SbomFormat outputFormat) throws Exception
  {
    String testFileName = inputFormat.equals(SbomFormat.XML) ? TEST_XML_SBOM : TEST_JSON_SBOM;
    File testBomFile = prepareTestReportFile(testFileName);

    defineDbTestData(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(appId, SBOM_VERSION, testBomFile.getName(),
        thirdPartyFile);

    Bom originalBom = mockOriginalBom(testBomFile);

    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_15, outputFormat));
    String exportedBomStr = exporter.export();
    assertExportedBomString(exportedBomStr);

    Bom exportedNewBom = ThirdPartyUtils.parseAndValidateCycloneDx(exportedBomStr, outputFormat);
    assertExportedBom(exportedNewBom);

    List<Vulnerability> exportedVulnerabilities = exportedNewBom.getVulnerabilities();
    assertThat(exportedVulnerabilities).hasSize(13);
    testNewBomVulnerabilityDataAdded(exportedNewBom, originalBom.getVulnerabilities());
    testExistingBomVulnerabilityDataUpdated(originalBom.getVulnerabilities(),
        exportedVulnerabilities);
  }

  private void testExistingBomVulnerabilityDataUpdated(
      List<Vulnerability> originalVulnerabilities,
      List<Vulnerability> exportedVulnerabilities)
  {
    Vulnerability originalSonatype0053 = findVulnerability("sonatype-2010-0053", originalVulnerabilities);
    assertThat(originalSonatype0053).isNotNull()
        .hasFieldOrPropertyWithValue("description", null)
        .extracting(Vulnerability::getSource)
        .hasFieldOrPropertyWithValue("name", "SONATYPE")
        .hasFieldOrPropertyWithValue("url", "http://localhost:8070/ui/links/vln/sonatype-2010-0053");

    assertThat(originalSonatype0053.getCwes()).hasSize(1)
        .anyMatch(cwe -> cwe.equals(426));

    assertThat(originalSonatype0053.getRatings()).hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("vector", "CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H")
        .hasFieldOrPropertyWithValue("method", Method.OTHER)
        .hasFieldOrPropertyWithValue("severity", Severity.CRITICAL)
        .hasFieldOrPropertyWithValue("score", 7.8d)
        .hasFieldOrPropertyWithValue("justification", null)
        .extracting(Rating::getSource)
        .hasFieldOrPropertyWithValue("name", "SONATYPE")
        .hasFieldOrPropertyWithValue("url", null);

    Vulnerability originalCve23307 = findVulnerability("CVE-2022-23307", originalVulnerabilities);
    assertThat(originalCve23307).isNotNull()
        .hasFieldOrPropertyWithValue("description", null)
        .extracting(Vulnerability::getSource)
        .hasFieldOrPropertyWithValue("name", "NVD")
        .hasFieldOrPropertyWithValue("url",
            "http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-23307");

    // Vulnerabilities
    Vulnerability updatedSonatype0053 = findVulnerability("sonatype-2010-0053", exportedVulnerabilities);
    assertThat(updatedSonatype0053).isNotNull()
        .hasFieldOrPropertyWithValue("description", "DESC sonatype-2010-0053")
        .extracting(Vulnerability::getSource)
        .hasFieldOrPropertyWithValue("name", "source")
        .hasFieldOrPropertyWithValue("url", "l1");
    assertThat(updatedSonatype0053.getProperties())
        .hasSize(1)
        .anyMatch(p -> p.getName().equalsIgnoreCase(SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME) &&
            p.getValue().equals("G,F"));
    assertThat(updatedSonatype0053.getCwes()).hasSize(1)
        .anyMatch(cwe -> cwe.equals(1234));
    assertThat(updatedSonatype0053.getRatings()).hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("vector", "v:1")
        .hasFieldOrPropertyWithValue("method", Method.OTHER)
        .hasFieldOrPropertyWithValue("severity", Severity.MEDIUM)
        .hasFieldOrPropertyWithValue("score", 5.5d)
        .hasFieldOrPropertyWithValue("justification", null)
        .extracting(Rating::getSource)
        .hasFieldOrPropertyWithValue("name", "source")
        .hasFieldOrPropertyWithValue("url", "l1");

    // Vex
    assertThat(updatedSonatype0053.getAnalysis())
        .hasFieldOrPropertyWithValue("state", State.RESOLVED_WITH_PEDIGREE)
        .hasFieldOrPropertyWithValue("justification", Justification.REQUIRES_CONFIGURATION)

        .hasFieldOrPropertyWithValue("detail", "Sonatype detail");
    assertThat(updatedSonatype0053.getAnalysis().getResponses()).hasSize(1)
        .anyMatch(r -> r.getResponseName().equalsIgnoreCase(Response.WORKAROUND_AVAILABLE.toString()));
  }

  private void testNewBomVulnerabilityDataAdded(Bom exportedNewBom, List<Vulnerability> originalVulnerabilities) {
    // Original tests
    assertThatThrownBy(() -> findVulnerability("ABC-123", originalVulnerabilities))
        .isInstanceOf(NoSuchElementException.class);

    // Vulnerabilities
    List<Vulnerability> exportedVulnerabilities = exportedNewBom.getVulnerabilities();
    assertThat(exportedVulnerabilities).hasSize(13);
    Vulnerability vulnerabilityABC = findVulnerability("ABC-123", exportedVulnerabilities);
    assertThat(vulnerabilityABC).isNotNull()
        .hasFieldOrPropertyWithValue("description", "DESC 123")
        .extracting(Vulnerability::getSource)
        .hasFieldOrPropertyWithValue("name", "source")
        .hasFieldOrPropertyWithValue("url", "http//");
    assertThat(vulnerabilityABC.getProperties())
        .hasSize(1)
        .anyMatch(p -> p.getName().equalsIgnoreCase(SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME) &&
            p.getValue().equals("M"));

    // Vex
    assertThat(vulnerabilityABC.getAnalysis())
        .hasFieldOrPropertyWithValue("state", State.IN_TRIAGE)
        .hasFieldOrPropertyWithValue("justification", Justification.CODE_NOT_PRESENT)

        .hasFieldOrPropertyWithValue("detail", "ABC detail");
    assertThat(vulnerabilityABC.getAnalysis().getResponses()).hasSize(1)
        .anyMatch(r -> r.getResponseName().equalsIgnoreCase(Response.WORKAROUND_AVAILABLE.toString()));
  }

  private void defineDbTestData(ThirdPartyFile thirdPartyFile) {
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "testSource",
        "JSON",
        "test",
        "1.5",
        "abc",
        "pkg:maven/log4j/log4j@1.2.8?type=jar");

    ThirdPartyCoordinateSecurity vulnerabilitySonatype20100053 = tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "sonatype-2010-0053", "DESC sonatype-2010-0053", "l1", 5.5d,
        "1.1", "source", "v:1", "Medium", "1234",
        "m1", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");

    ThirdPartyCoordinateSecurity vulnerabilityAbc123 = tempEntity.newThirdPartyCoordinateSecurity(fileCoordinate,
        "ABC-123", "DESC 123", "http//", 1.5d, "1.2", "source",
        "v:2", "lkk", "5467", "owasp", "<dd>r1<dd/>",
        "<dd>a1<dd/>", "M");

    ThirdPartyCoordinateSecurity vulnerabilitySonatype23307 = tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "CVE-2022-23307", "DESC 123", "http//", 1.5d, "1.2",
        "source", "v:2", "lkk", "5467", "owasp",
        "<dd>r1<dd/>", "<dd>a1<dd/>", "M");

    tempEntity.newThirdPartyCoordinateLicense(fileCoordinate, "MIT", "MIT",
        "http://mit", "A");
    tempEntity.newThirdPartyCoordinateLicense(fileCoordinate, "Aladdin", "Aladdin",
        "http://aladdin", "A,B");
    tempEntity.newThirdPartyCoordinateLicense(fileCoordinate, "Apache-1.1", "Apache-1.1",
        "http://apache.org", "B,C,D");

    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityAbc123,
        "ABC-123", "in_triage", "code_not_present", "workaround_available",
        "ABC detail");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilitySonatype20100053,
        "sonatype-2010-0053", "resolved_with_pedigree", "requires_configuration",
        "workaround_available",
        "Sonatype detail");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilitySonatype23307,
        "CVE-2022-23307", "requires_configuration", "will_not_fix",
        "workaround_available",
        "Sonatype 23307");
  }

  private Bom parseGzippedReport(String path) {
    try {
      InputStream gis = new GZIPInputStream(Files.newInputStream(Paths.get(path)));
      return SbomCycloneDxUtils.parseContentStreamNoValidation(gis);
    }
    catch (IOException | ParseException e) {
      throw new SbomExportException("Unable to parse original file to create original Bom", e);
    }
  }

  private Component findBomComponent(String purl, List<Component> components) {
    return components.stream().filter(c -> c.getPurl().equalsIgnoreCase(purl)).findFirst().orElse(null);
  }

  private License findBomLicense(String licenseIdentifier, List<License> licenses) {
    return licenses.stream()
        .filter(
            l -> (l.getId() != null && l.getId().equalsIgnoreCase(licenseIdentifier))
                || (l.getName() != null && l.getName().equalsIgnoreCase(licenseIdentifier)))
        .findFirst()
        .orElse(null);
  }

  private Vulnerability findVulnerability(String refId, List<Vulnerability> vulnerabilities) {
    return vulnerabilities.stream().filter(v -> v.getId().equalsIgnoreCase(refId)).findFirst().get();
  }

  private File prepareTestReportFile(String sbomFileName) throws Exception {
    return mockSbomFileForApp(appId, getGZippedSbom(sbomFileName));
  }

  private void assertExportedBom(Bom exportedNewBom) {
    assertThat(exportedNewBom).isNotNull();
    checkExportedComponents(exportedNewBom);
  }

  private void checkExportedComponents(Bom exportedNewBom) {
    List<Component> exportedComponents = exportedNewBom.getComponents();
    assertThat(exportedComponents).hasSize(13);
    Component exportedLog4j = findBomComponent(
        "pkg:maven/log4j/log4j@1.2.8?type=jar", exportedComponents);
    assertThat(exportedLog4j).isNotNull()
        .extracting(Component::getLicenses)
        .extracting(LicenseChoice::getLicenses)
        .extracting(List::size)
        .isEqualTo(4);

    // Licenses
    List<License> log4JExportedLicenses = exportedLog4j.getLicenses().getLicenses();

    // New License added
    License exportedAladdinLicense = findBomLicense("Aladdin", log4JExportedLicenses);
    assertThat(exportedAladdinLicense).isNotNull();
    assertLicenseHasIdentificationSources(exportedAladdinLicense, "A,B");

    License exportedMitLicense = findBomLicense("MIT", log4JExportedLicenses);
    assertThat(exportedMitLicense).isNotNull();
    assertLicenseHasIdentificationSources(exportedMitLicense, "A");

    // Existing licenses updated with db data
    License exportedApache1License = findBomLicense("Apache-1.1", log4JExportedLicenses);
    assertThat(exportedApache1License).isNotNull().extracting(License::getUrl).isEqualTo("http://apache.org");
    assertLicenseHasIdentificationSources(exportedApache1License, "B,C,D");

    License exportedNonStandardLicense = findBomLicense("Non-Standard", log4JExportedLicenses);
    assertThat(exportedNonStandardLicense).extracting(License::getUrl).isNull();
    assertThat(exportedNonStandardLicense.getProperties()).isNull();
  }

  private Bom mockOriginalBom(File testBomFile) {
    Bom originalBom = parseGzippedReport(testBomFile.getAbsolutePath());
    assertThat(originalBom).isNotNull();
    assertThat(originalBom.getComponents()).hasSize(13);
    assertThat(originalBom.getVulnerabilities()).hasSize(12);
    return originalBom;
  }

  private void assertLicenseHasIdentificationSources(License license, String identificationSources) {
    assertThat(CollectionUtils.isEmpty(license.getProperties())).isFalse();
    Property foundProperty = license.getProperties()
        .stream()
        .filter(property -> property.getName()
            .equals(SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME))
        .findFirst()
        .orElse(null);
    assertThat(foundProperty).isNotNull()
        .extracting(Property::getValue)
        .isEqualTo(identificationSources);
  }

  private void assertExportedBomString(String exportedBomContent) {
    assertThat(exportedBomContent).isNotNull();
    assertThat(ThirdPartyUtils.looksLikeCycloneDX(exportedBomContent)).isTrue();
  }
}
