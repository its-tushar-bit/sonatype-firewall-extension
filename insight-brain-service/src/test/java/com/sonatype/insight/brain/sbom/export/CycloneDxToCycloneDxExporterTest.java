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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportOption;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.thirdparty.SbomStatus;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CycloneDxToCycloneDxExporterTest
    extends AbstractSbomExporterTest
{
  private CycloneDxToCycloneDxExporter exporter;

  private static final String APP_ID = "webgoat";

  private static final String SBOM_VERSION = "v1";

  private static final String THIRD_PARTY_FILE = "testBom.json";

  private static final String TEST_XML_SBOM = "webgoat-bom.xml";

  private static final String TEST_JSON_SBOM = "webgoat-bom.json";

  private static final String SCAN_ID = "sid1";

  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Inject
  ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Inject
  ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  @Inject
  private IdUtils idUtils;

  @Inject
  private BaseUrl baseUrl;

  @Inject
  private VersionService versionService;

  private ThirdPartyFile thirdPartyFile;

  @Before
  public void init() throws SbomExportException {
    tempEntity.newApplicationWithParent(APP_ID);
    thirdPartyFile = tempEntity.newThirdPartyFile(THIRD_PARTY_FILE);
    exporter = new CycloneDxToCycloneDxExporter(
        mockInsightWork,
        multiLicenseDAO,
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
  public void exportTest_NoVulnerabilitiesInOriginalBom() throws Exception {
    String testFileName = "test-bom.xml";
    File testBomFile = prepareTestReportFile(testFileName);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(testBomFile.getName(), thirdPartyFile);
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_15, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "source",
        "maven",
        "log4j",
        "1.2.8",
        "abcdef",
        "pkg:maven/log4j/log4j@1.2.8?type=jar"
    );
    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "sonatype-2010-0053", "DESC sonatype-2010-0053", "l1", 5.5d,
        "1.1", "source", "v:1", "Medium", "1234",
        "m1", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
    String export = exporter.export();
    assertThatJson(export)
        .whenIgnoringPaths("metadata.timestamp",
            "metadata.tools.components[0].version", "metadata.component.bom-ref")
        .isEqualTo(readFileToString("outputs/output-test-bom.json"));
  }

  @Test
  public void exportTest_VulnerabilityRatingFieldsPreserved() throws Exception {
    String testFileName = "test-bom.xml";
    File testBomFile = prepareTestReportFile(testFileName);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(testBomFile.getName(), thirdPartyFile);
    exporter.setExportParams(withExportParams(sbomMetadata, ExportSpecification.CYCLONEDX_15, SbomFormat.JSON));
    tempEntity.newThirdPartyScan("srid1", SCAN_ID, thirdPartyFile);
    ThirdPartyFileCoordinate fileCoordinate = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile,
        "source",
        "maven",
        "log4j",
        "1.2.8",
        "abcdef",
        "pkg:maven/log4j/log4j@1.2.8?type=jar"
    );
    tempEntity.newThirdPartyCoordinateSecurity(
        fileCoordinate, "CVE-2022-23307", "DESC CVE-2022-23307", "NVD-link", 5.5d,
        "1.1", "NVD", "CVSS:3.1", "Medium", "1234",
        "cvSSv3", "<dd>r1<dd/>", "<dd>a1<dd/>", "G,F");
    String export = exporter.export();
    assertThatJson(export)
        .whenIgnoringPaths("metadata.timestamp", "metadata.tools.components[0].version",
            "metadata.component.bom-ref")
        .isEqualTo(readFileToString("outputs/output-test-bom-2.json"));
  }

  private void testExportingWebgoatAppWithInputFormatAndOutputFormat(SbomFormat inputFormat, SbomFormat outputFormat)
      throws Exception
  {
    String testFileName = inputFormat.equals(SbomFormat.XML) ? TEST_XML_SBOM : TEST_JSON_SBOM;
    File testBomFile = prepareTestReportFile(testFileName);

    defineDbTestData(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata = insertTestData(testBomFile.getName(),
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
        .anyMatch(p ->
            p.getName().equalsIgnoreCase("identificationSources") &&
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
        .anyMatch(p ->
            p.getName().equalsIgnoreCase("identificationSources") &&
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
        "pkg:maven/log4j/log4j@1.2.8?type=jar"
    );

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
    return licenses.stream().filter(
        l -> (l.getId() != null && l.getId().equalsIgnoreCase(licenseIdentifier))
            || (l.getName() != null && l.getName().equalsIgnoreCase(licenseIdentifier))
    ).findFirst().orElse(null);
  }

  private Vulnerability findVulnerability(String refId, List<Vulnerability> vulnerabilities) {
    return vulnerabilities.stream().filter(v -> v.getId().equalsIgnoreCase(refId)).findFirst().get();
  }

  private File prepareTestReportFile(String sbomFileName) throws Exception {
    return mockSbomFileForApp(APP_ID, getGZippedSbom(sbomFileName));
  }

  private ThirdPartySbomMetadata insertTestData(String testBomFile, ThirdPartyFile thirdPartyFile) {
    ThirdPartySbomMetadata dbRecord = tempEntity.createSbomMetadata(APP_ID, SBOM_VERSION,
        thirdPartyFile);
    dbRecord.setFilename(testBomFile);
    dbRecord.setStatus(SbomStatus.ACTIVE.toString());
    thirdPartySbomMetadataDAO.update(dbRecord);
    return dbRecord;
  }

  private SbomExportParams withExportParams(
      ThirdPartySbomMetadata sbomMetadata,
      ExportSpecification specification,
      SbomFormat targetFormat
  )
  {
    return SbomExportParams.newSbomExporterParams(sbomMetadata)
        .withExportOptions(ExportOption.NO_VULNERABILITIES)
        .withExportSpecification(specification)
        .withTargetFormat(targetFormat);
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
        .extracting(List::size).isEqualTo(4);

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
    Property foundProperty = license.getProperties().stream().filter( property -> property.getName()
        .equals("identificationSources")).findFirst().orElse(null);
    assertThat(foundProperty).isNotNull()
        .extracting(Property::getValue)
        .isEqualTo(identificationSources);
  }

  private void assertExportedBomString(String exportedBomContent) {
    assertThat(exportedBomContent).isNotNull();
    assertThat(ThirdPartyUtils.looksLikeCycloneDX(exportedBomContent)).isTrue();
  }
}
