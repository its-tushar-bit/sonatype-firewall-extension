/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.model.ProjectScanItem;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.insight.util.SbomUtils;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.ExtensibleType;
import org.cyclonedx.model.Extension;
import org.cyclonedx.model.Extension.ExtensionType;
import org.cyclonedx.model.License;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method;
import org.cyclonedx.model.vulnerability.Vulnerability10;
import org.cyclonedx.model.vulnerability.Vulnerability10.ScoreSource;
import org.cyclonedx.model.vulnerability.Vulnerability10.Severity;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.parsers.XmlParser;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.ATTACK_VECTOR_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.FORMAT_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.IDENTIFICATION_SOURCE_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.LINK_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.NAME_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.PURL_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.REFID_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.VERSION_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.VULNERABILITY_SOURCE_MAX_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SbomResultHandlerTest
    extends AbstractComponentTest
{
  @Spy
  private SbomResultHandler sbomResultHandler;

  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  private LicenseDAO licenseDAO;

  @Spy
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Spy
  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private final String loggerName = SbomResultHandler.class.getName();

  @Rule
  public LogOutput logOutput = new LogOutput(loggerName);

  @Rule
  public LogOutput logOutputUtils = new LogOutput(ThirdPartyUtils.class.getName());

  @Test
  public void testHandleAndFilterContents_filterContent_newThirdPartyFileMultipleEntries() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-multiple-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("clair-bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 2);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(2);
    assertThat(coordinates).allSatisfy(coord -> assertThat(coord.getSource()).isEqualTo("clair"));
  }

  @Test
  public void testHandleAndFilterContents_veryLongIdentificationSource() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-multiple-components.xml");
    String identificationSource = "identification-source-very-long";
    ThirdPartyScanContent content =
        new ThirdPartyScanContent(identificationSource + "-bom.xml", null, null, null,
            sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 2);
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).allSatisfy(coord -> assertThat(coord.getSource())
        .isEqualTo(StringUtils.truncate(identificationSource, IDENTIFICATION_SOURCE_MAX_LENGTH)));
  }

  @Test
  public void testHandleAndFilterContents_priorityPurl_Then_Sha1_Then_Coordinates() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-purl-hashes-coordinates-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = assertFilteredSbomFile(filteredContent, 4);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName)
        .containsOnly("tomcat-catalina", "django", "jackson-databind", "joda-time");
    assertThat(components).extracting(Component::getVersion)
        .containsOnly("9.0.14", "1.2.3", "2.9.9", "2.1.0");
    assertThat(components).extracting(Component::getPurl)
        .containsExactlyInAnyOrder("pkg:maven/org.apache.tomcat/tomcat-catalina@9.0.14?type=jar", null,
            "pkg:library/com.fasterxml.jackson.core/jackson-databind@2.9.9", null);
    assertThat(components).extracting("properties.size")
        .containsOnly(null, 1, null, 1);
    assertThat(components.get(1).getProperties())
        .flatExtracting(Property::getValue)
        .contains("e6b1000b94e835ffd37f");
    assertThat(components.get(3).getProperties())
        .flatExtracting(Property::getValue)
        .contains("9188560f22e0b73070d2");
  }

  @Test
  public void testHandleAndFilterContents_filterContent_hashes() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-hashes-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("clair-bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = assertFilteredSbomFile(filteredContent, 2);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName)
        .containsOnly("tomcat-catalina", "jackson-databind");
    assertThat(components).extracting(Component::getVersion)
        .containsOnly("9.0.14", "2.9.9");
    assertThat(components).extracting(component -> component.getType().getTypeName())
        .containsOnly("library", "library");
    assertThat(components).extracting(Component::getPurl)
        .containsOnly(null, "pkg:library/com.fasterxml.jackson.core/jackson-databind@2.9.9");
    assertThat(components).extracting("properties.size")
        .containsOnly(1, null);
    assertThat(components.get(0).getProperties())
        .flatExtracting(Property::getValue)
        .contains("e6b1000b94e835ffd37f");
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 7);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(7);

    List<ThirdPartyCoordinateSecurity> actualVuln = new ArrayList<>();
    for (ThirdPartyFileCoordinate coordinate : coordinates) {
      List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(coordinate.getId());
      actualVuln.addAll(coordinatesSecurity);
      assertThat(coordinatesSecurity).hasSize(1);
    }
    assertThirdPartyCoordinateSecurities(sbomContent, actualVuln);
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilitiesAndNoPurl() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-no-purl.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-no-purl.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 1);

    List<Component> components = filteredSbom.getComponents();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
    assertThirdPartyFileCoordinate(components.get(0), thirdPartyFile, thirdPartyFileCoordinate);
    List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(coordinatesSecurity).hasSize(1);
    assertThirdPartyCoordinateSecurity(sbomContent, thirdPartyFileCoordinate.getId(), coordinatesSecurity.get(0),
        true);
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilitiesAndNoPurl_withHash() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-no-purl-with-hash.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-no-purl-with-hash.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 1);

    List<Component> components = filteredSbom.getComponents();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
    assertThirdPartyFileCoordinate(components.get(0), thirdPartyFile, thirdPartyFileCoordinate, true);
    List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(coordinatesSecurity).hasSize(1);
    assertThirdPartyCoordinateSecurity(sbomContent, thirdPartyFileCoordinate.getId(), coordinatesSecurity.get(0),
        true);
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilitiesAndNoPurlNotCoordinates_withHash() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-no-purl-no-coordinates-with-hash.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-no-purl-no-coordinates-with-hash.xml", null, null, null,
            sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1, true);
    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId())).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_withLicense() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-license.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-license.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom unfilteredSbom = getBom(sbomContent);
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 1);

    List<Component> components = filteredSbom.getComponents();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    try (TransactionContext tx = thirdPartyCoordinateLicenseDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
      assertThirdPartyFileCoordinate(components.get(0), thirdPartyFile, thirdPartyFileCoordinate);
      List<ThirdPartyCoordinateLicense> coordinatesLicense =
          thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(coordinatesLicense).hasSize(2);
      assertThirdPartyCoordinateLicense(unfilteredSbom.getComponents().get(0).getLicenseChoice().getLicenses().get(0),
          thirdPartyFileCoordinate.getId(),
          coordinatesLicense.get(0));
      assertThirdPartyCoordinateLicense(unfilteredSbom.getComponents().get(0).getLicenseChoice().getLicenses().get(1),
          thirdPartyFileCoordinate.getId(),
          coordinatesLicense.get(1));
    }
  }
  
  @Test
  public void testHandleAndFilterContents_withComponentDuplicatedLicenseAndVulnerability() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-duplicated-component-license-vulnerability.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-duplicated-component-license-vulnerability.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 1);

    List<Component> components = filteredSbom.getComponents();
    assertThat(components).hasSize(1);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    try (TransactionContext tx = thirdPartyCoordinateLicenseDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
      assertThirdPartyFileCoordinate(components.get(0), thirdPartyFile, thirdPartyFileCoordinate);
      List<ThirdPartyCoordinateLicense> coordinatesLicense =
          thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(coordinatesLicense).hasSize(1);
      List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(coordinatesSecurity).hasSize(1);
    }
  }

  @Test
  public void testHandleAndFilterContents_lengthFormat() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-long-purl-format.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-long-purl-format.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 1);

    List<Component> components = filteredSbom.getComponents();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    try (TransactionContext tx = thirdPartyCoordinateLicenseDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
      assertThirdPartyFileCoordinate(components.get(0), thirdPartyFile, thirdPartyFileCoordinate);
    }
  }
 
  @Test
  public void testHandleAndFilterContents_ignoreUnsupportedLicenseExpressions() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-license-expression.xml");
    assertNoLicense(sbomContent, "sbom-license-expression.xml");
  }

  @Test
  public void testHandleAndFilterContents_licenseMissingId() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-license-no-id.xml");
    assertNoLicense(sbomContent, "sbom-license-no-id.xml");
  }

  @Test
  public void testHandleAndFilterContents_missingLicenses() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-missing-licenses.xml");
    assertNoLicense(sbomContent, "sbom-missing-licenses.xml");
  }

  @Test
  public void testHandleAndFilterContents_emptyLicenses() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-empty-licenses.xml");
    assertNoLicense(sbomContent, "sbom-empty-licenses.xml");
  }

  private void assertNoLicense(String sbomContent, String name) throws Exception {
    ThirdPartyScanContent content = new ThirdPartyScanContent(name, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 1);

    List<Component> components = filteredSbom.getComponents();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    try (TransactionContext tx = thirdPartyCoordinateLicenseDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
      assertThirdPartyFileCoordinate(components.get(0), thirdPartyFile, thirdPartyFileCoordinate);
      List<ThirdPartyCoordinateLicense> coordinatesLicense =
          thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(coordinatesLicense).isEmpty();
    }
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_optionalValues() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-optional-values.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-optional-values.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 1);

    List<Component> components = filteredSbom.getComponents();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    try (TransactionContext tx = thirdPartyCoordinateSecurityDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
      assertThirdPartyFileCoordinate(components.get(0), thirdPartyFile, thirdPartyFileCoordinate);
      List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(coordinatesSecurity).hasSize(1);
      assertThirdPartyCoordinateSecurity(sbomContent, thirdPartyFileCoordinate.getId(), coordinatesSecurity.get(0),
          false);
    }
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_xml_14() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-v1_4.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-v1_4.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 1);

    List<Component> components = filteredSbom.getComponents();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    try (TransactionContext tx = thirdPartyCoordinateSecurityDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
      assertThirdPartyFileCoordinate(components.get(0), thirdPartyFile, thirdPartyFileCoordinate);
      List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(coordinatesSecurity).hasSize(1);
      assertThirdPartyCoordinateSecurity(sbomContent, thirdPartyFileCoordinate.getId(), coordinatesSecurity.get(0),
          true, false);
    }
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_json_14() throws Exception {
    String sbomContent = getSbomJsonFile("sbom-vulnerabilities-v1-4.json");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-v1-4.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 1);

    List<Component> components = filteredSbom.getComponents();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    try (TransactionContext tx = thirdPartyCoordinateSecurityDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
      assertThirdPartyFileCoordinate(components.get(0), thirdPartyFile, thirdPartyFileCoordinate);
      List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(coordinatesSecurity).hasSize(1);
      assertThirdPartyCoordinateSecurity(sbomContent, thirdPartyFileCoordinate.getId(), coordinatesSecurity.get(0),
          true, false);
    }
  }

  @Test
  public void testHandleAndFilterContents_withExtensionVulnerabilities_xml_14() throws Exception {
    assertVulnerabilityInformation("sbom-ext-vulnerabilities-v1_4.xml");
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_noSeverity() throws Exception {
    assertVulnerabilityInformation("sbom-vulnerabilities-no-severity.xml");
  }

  private void assertVulnerabilityInformation(final String filename) throws Exception {
    String sbomContent = getSbomXmlFile(filename);
    ThirdPartyScanContent content = new ThirdPartyScanContent(filename, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 1);

    List<Component> components = filteredSbom.getComponents();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    try (TransactionContext tx = thirdPartyCoordinateSecurityDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
      assertThirdPartyFileCoordinate(components.get(0), thirdPartyFile, thirdPartyFileCoordinate);
      List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(coordinatesSecurity).isEmpty();
    }
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_invalidVulnerabilities() throws Exception {
    assertVulnerabilityInformation("sbom-vulnerabilities-invalid-vulnerabilities.xml");
  }

  @Test
  public void testHandleAndFilterContents_Sbom_Version_1_0() throws Exception {
    String sbomContent = getSbomXmlFile("sbom_1_0.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom_1_0.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> sbomResultHandler.handleAndFilterContents(content, thirdPartyFile))
        .withMessage("Error filtering sbom file sbom_1_0.xml");
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_missingFields() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-missing-fields.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-missing-fields.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 5);

    List<Component> components = filteredSbom.getComponents();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(5);

    try (TransactionContext tx = thirdPartyCoordinateSecurityDAO.createTransactionContext()) {
      assertSecurityCoordinates(coordinates.get(0), components.get(0), thirdPartyFile, tx, 0);
      assertSecurityCoordinates(coordinates.get(1), components.get(1), thirdPartyFile, tx, 0);
      assertSecurityCoordinates(coordinates.get(2), components.get(2), thirdPartyFile, tx, 1);
      assertSecurityCoordinates(coordinates.get(3), components.get(3), thirdPartyFile, tx, 1);
      assertSecurityCoordinates(coordinates.get(4), components.get(4), thirdPartyFile, tx, 1);
    }
  }

  private void assertSecurityCoordinates(
      ThirdPartyFileCoordinate thirdPartyFileCoordinate,
      Component component,
      ThirdPartyFile thirdPartyFile,
      TransactionContext tx,
      int coordinatesSecuritySize)
  {
    assertThirdPartyFileCoordinate(component, thirdPartyFile, thirdPartyFileCoordinate);
    List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
    assertThat(coordinatesSecurity).hasSize(coordinatesSecuritySize);
  }

  @Test
  public void testHandleAndFilterContents_withoutVulnerabilities() throws Exception {
    assertVulnerabilityInformation("sbom-no-vulnerabilities.xml");
  }

  @Test
  public void testHandleAndFilterContents_duplicatedVulnerabilities() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-duplicated-vulnerabilities.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-duplicated-vulnerabilities.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    try (TransactionContext tx = thirdPartyCoordinateSecurityDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
      List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(coordinatesSecurity).hasSize(1);
    }
  }

  @Test
  public void testHandleAndFilterContents_repeatedComponents() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-repeated-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-repeated-components.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
  }

  @Test
  public void testHandleAndFilterContents_nullContent() {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, null);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isNull();
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_emptyContent() {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, "");
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isBlank();
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_invalidSbom() throws Exception {
    testHandleAndFilterContents_invalid_sbom(getSbomXmlFile("scan-with-invalid-sbom-data-cli.xml"),
        "scan-with-invalid-sbom-data-cli.xml");
  }

  @Test
  public void testHandleAndFilterContents_invalidSbom_content() throws Exception {
    testHandleAndFilterContents_invalid_sbom(getSbomXmlFile("scan-with-invalid-license-id.xml"),
        "scan-with-invalid-sbom-data-cli.xml");
    assertThat(logOutputUtils.getErrorMessages(ThirdPartyUtils.class.getName()))
        .contains("The sbom is not valid. There were 2 errors.");
  }

  @Test
  public void testHandleAndFilterContents_invalidSbom_Xml_v1_2() throws Exception {
    testHandleAndFilterContents_invalid_sbom(getSbomXmlFile("scan-with-invalid-sbom-v1_2.xml"),
        "scan-with-invalid-sbom-v1_2.xml");
  }

  @Test
  public void testHandleAndFilterContents_invalidSbom_Json() throws Exception {
    testHandleAndFilterContents_invalid_sbom(getSbomJsonFile("sbom-invalid.json"), "sbom-invalid.json");
  }

  private void testHandleAndFilterContents_invalid_sbom(String sbomContent, String path) {
    ThirdPartyScanContent content = new ThirdPartyScanContent(path, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> sbomResultHandler.handleAndFilterContents(content, thirdPartyFile))
        .withMessage("Error filtering sbom file " + path);

    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId())).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_sbomNestedComponents() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-nested-component.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("scan-with-sbom-nested-component.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);
  }

  @Test
  public void testHandleAndFilterContents_v1_2() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_2.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_2.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    FilteredThirdPartyContent filteredContent =
        sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    Bom bom = assertFilteredSbomFile(sbomXml, 2);
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/Acme%20Application@9.1.1");
  }

  @Test
  public void testHandleAndFilterContents_v1_3() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_3.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_3.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = assertFilteredSbomFile(filteredContent, 2);
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/Acme%20Application@9.1.1");
  }

  @Test
  public void testHandleAndFilterContents_v1_4() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_4.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_4.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = assertFilteredSbomFile(filteredContent, 2);
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/Acme/Acme%20Application@9.1.1");
  }

  @Test
  public void testHandleAndFilterContents_v1_4_json() throws Exception {
    String sbomContent = getSbomJsonFile("sbom-simple-v1-4.json");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_4.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);
  }

  @Test
  public void testProcessDependencyGraph() {
    Bom sourceBom = new Bom();
    Bom targetBom = new Bom();
    targetBom.addComponent(new Component());
    Metadata metadata = new Metadata();
    Component rootComponent = new Component();
    rootComponent.setName("root");
    rootComponent.setBomRef("root");
    rootComponent.setVersion("1.0");
    rootComponent.setPurl("pkg:npm/root@1.0");
    metadata.setComponent(rootComponent);
    targetBom.setMetadata(metadata);
    Dependency root = createDependencyList("root", "pkg:npm/direct1@1.0", "pkg:npm/direct2@2.0");
    Dependency d1 = createDependencyList("pkg:npm/direct1@1.0", "pkg:npm/d1t1@1.1");
    Dependency d2 = createDependencyList("pkg:npm/direct2@2.0", "pkg:npm/d2t1@1.1");
    Dependency d1t1 = d1.getDependencies().get(0);
    Dependency d2t1 = d2.getDependencies().get(0);
    sourceBom.setDependencies(Arrays.asList(root, d1, d2, d1t1, d2t1));

    List<ProjectScanItem> result = new ArrayList<>();

    sbomResultHandler.processDependencyGraph(sourceBom, targetBom, result,
        new ThirdPartyFile("test-bom.xml", new Date()));

    assertThat(result).hasSize(1).allSatisfy(projectItem -> {
      assertThat(projectItem.getKind()).isEqualTo("sbom");
      assertThat(projectItem.getId()).isEqualTo("pkg:npm/root@1.0");
      assertThat(projectItem.getPath()).isEqualTo("test-bom.xml");
      List<com.sonatype.insight.scan.model.Dependency> rootDependencies = projectItem.getDependencies();
      assertThat(rootDependencies).hasSize(4)
          .extracting(com.sonatype.insight.scan.model.Dependency::getId)
          .containsExactlyInAnyOrder("pkg:npm/direct1@1.0", "pkg:npm/direct2@2.0",
              "pkg:npm/d1t1@1.1", "pkg:npm/d2t1@1.1");
      assertParentAndChildDependency(rootDependencies, "pkg:npm/direct1@1.0", "pkg:npm/d1t1@1.1");
      assertParentAndChildDependency(rootDependencies, "pkg:npm/direct2@2.0", "pkg:npm/d2t1@1.1");
    });
    assertIdentityMetadata(targetBom, metadata);
  }

  @Test
  public void testProcessDependencyGraph_WithDuplicatedDeps() {
    Bom sourceBom = new Bom();
    Bom targetBom = new Bom();
    targetBom.addComponent(new Component());
    Metadata metadata = new Metadata();
    Component rootComponent = new Component();
    rootComponent.setName("root");
    rootComponent.setBomRef("root");
    rootComponent.setVersion("1.0");
    rootComponent.setPurl("pkg:npm/root@1.0");
    metadata.setComponent(rootComponent);
    targetBom.setMetadata(metadata);
    Dependency root = createDependencyList("root", "pkg:npm/direct1@1.0", "pkg:npm/direct2@2.0");
    Dependency d1 = createDependencyList("pkg:npm/direct1@1.0", "pkg:npm/d1t1@1.1");
    Dependency d2 = createDependencyList("pkg:npm/direct2@2.0");
    Dependency d1t1 = d1.getDependencies().get(0);
    Dependency duplicatedDep = d1.getDependencies().get(0);
    sourceBom.setDependencies(Arrays.asList(root, d1, d2, d1t1, duplicatedDep));

    List<ProjectScanItem> result = new ArrayList<>();

    sbomResultHandler.processDependencyGraph(sourceBom, targetBom, result,
        new ThirdPartyFile("test-bom.xml", new Date()));

    assertThat(result).hasSize(1).allSatisfy(projectItem -> {
      assertThat(projectItem.getKind()).isEqualTo("sbom");
      assertThat(projectItem.getId()).isEqualTo("pkg:npm/root@1.0");
      assertThat(projectItem.getPath()).isEqualTo("test-bom.xml");
      List<com.sonatype.insight.scan.model.Dependency> rootDependencies = projectItem.getDependencies();
      assertThat(rootDependencies).hasSize(3)
          .extracting(com.sonatype.insight.scan.model.Dependency::getId)
          .containsExactlyInAnyOrder("pkg:npm/direct1@1.0", "pkg:npm/direct2@2.0", "pkg:npm/d1t1@1.1");
      assertParentAndChildDependency(rootDependencies, "pkg:npm/direct1@1.0", "pkg:npm/d1t1@1.1");
      assertParentAndChildDependency(rootDependencies, "pkg:npm/direct2@2.0", null);
    });
    assertIdentityMetadata(targetBom, metadata);
  }

  @Test
  public void testProcessDependencyGraph_WithMessyDependencies() {
    Bom sourceBom = new Bom();
    Bom targetBom = new Bom();
    targetBom.addComponent(new Component());
    Metadata metadata = new Metadata();
    Component rootComponent = new Component();
    rootComponent.setName("NugetProject");
    rootComponent.setBomRef("NugetProject@0.0.0");
    rootComponent.setVersion("0.0.0");
    rootComponent.setPurl("NugetProject@0.0.0");
    metadata.setComponent(rootComponent);
    targetBom.setMetadata(metadata);
    Dependency root = createDependencyList("NugetProject@0.0.0", "pkg:nuget/NUnit3TestAdapter@3.11.2");
    Dependency d1 = createDependencyList(
        "pkg:nuget/Microsoft.DotNet.InternalAbstractions@1.0.0", "pkg:nuget/System.AppContext@4.1.0");
    Dependency d2 = createDependencyList(
        "pkg:nuget/NUnit3TestAdapter@3.11.2", "pkg:nuget/Microsoft.DotNet.InternalAbstractions@1.0.0");
    Dependency d3 = createDependencyList("pkg:nuget/System.AppContext@4.1.0");
    sourceBom.setDependencies(Arrays.asList(root, d1, d2, d3));

    List<ProjectScanItem> result = new ArrayList<>();

    sbomResultHandler.processDependencyGraph(sourceBom, targetBom, result,
        new ThirdPartyFile("messy-bom.xml", new Date()));

    assertThat(result).hasSize(1).allSatisfy(projectItem -> {
      assertThat(projectItem.getKind()).isEqualTo("sbom");
      assertThat(projectItem.getId()).isEqualTo("NugetProject@0.0.0");
      assertThat(projectItem.getPath()).isEqualTo("messy-bom.xml");
      List<com.sonatype.insight.scan.model.Dependency> rootDependencies = projectItem.getDependencies();
      assertThat(rootDependencies).hasSize(3)
          .extracting(com.sonatype.insight.scan.model.Dependency::getId)
          .containsExactlyInAnyOrder(
              "pkg:nuget/Microsoft.DotNet.InternalAbstractions@1.0.0", "pkg:nuget/System.AppContext@4.1.0",
              "pkg:nuget/NUnit3TestAdapter@3.11.2");
      assertParentAndChildDependency(rootDependencies,
          "pkg:nuget/NUnit3TestAdapter@3.11.2", "pkg:nuget/Microsoft.DotNet.InternalAbstractions@1.0.0");
      assertParentAndChildDependency(rootDependencies,
          "pkg:nuget/Microsoft.DotNet.InternalAbstractions@1.0.0", "pkg:nuget/System.AppContext@4.1.0");
      assertThat(rootDependencies.stream().filter(dependency -> dependency.isDirect())).hasSize(1);
      assertThat(rootDependencies.stream().filter(dependency -> !dependency.isDirect())).hasSize(2);
    });
    assertIdentityMetadata(targetBom, metadata);
  }

  @Test
  public void testProcessDependencyGraph_WithModulePurl_NoMetadata() {
    Bom sourceBom = new Bom();
    Bom targetBom = new Bom();
    targetBom.addComponent(new Component());
    Dependency root = createDependencyList("pkg:npm/root@1.0", "pkg:npm/direct1@1.0", "pkg:npm/direct2@2.0");
    Dependency d1 = createDependencyList("pkg:npm/direct1@1.0", "pkg:npm/d1t1@1.1");
    Dependency d2 = createDependencyList("pkg:npm/direct2@2.0", "pkg:npm/d2t1@1.1");
    Dependency d1t1 = d1.getDependencies().get(0);
    Dependency d2t1 = d2.getDependencies().get(0);
    sourceBom.setDependencies(Arrays.asList(root, d1, d2, d1t1, d2t1));

    List<ProjectScanItem> result = new ArrayList<>();

    sbomResultHandler.processDependencyGraph(sourceBom, targetBom, result,
        new ThirdPartyFile("test-bom.xml", new Date()));

    assertThat(result).isEmpty();
  }

  @Test
  public void testProcessDependencyGraph_MissingRootDependency() {
    Bom sourceBom = new Bom();
    Bom targetBom = new Bom();
    Metadata metadata = new Metadata();
    Component rootComponent = new Component();
    rootComponent.setName("root");
    rootComponent.setBomRef("root");
    rootComponent.setVersion("1.0");

    targetBom.addComponent(new Component());

    metadata.setComponent(rootComponent);
    targetBom.setMetadata(metadata);

    Dependency d1 = createDependencyList("pkg:maven/com.fasterxml.jackson.core/jackson-annotations@2.9.10?type=jar");
    Dependency d2 = createDependencyList("pkg:maven/com.google.guava/guava@24.1.1-jre?type=jar",
        "pkg:maven/org.checkerframework/checker-compat-qual@2.0.0?type=jar",
        "pkg:maven/com.google.errorprone/error_prone_annotations@2.1.3?type=jar");
    Dependency d2t1 = d2.getDependencies().get(0);
    Dependency d2t2 = d2.getDependencies().get(1);
    sourceBom.setDependencies(Arrays.asList(d1, d2, d2t1, d2t2));

    List<ProjectScanItem> result = new ArrayList<>();

    sbomResultHandler.processDependencyGraph(sourceBom, targetBom, result,
        new ThirdPartyFile("test-bom.xml", new Date()));

    assertThat(result).isEmpty();
  }

  @Test
  public void testProcessDependencyGraph_NoSourceDependencies() {
    Bom sourceBom = new Bom();
    Bom targetBom = new Bom();
    Metadata metadata = new Metadata();
    Component rootComponent = new Component();
    rootComponent.setName("root");
    rootComponent.setBomRef("root");
    rootComponent.setVersion("1.0");
    rootComponent.setPurl("pkg:npm/root@1.0");
    metadata.setComponent(rootComponent);
    targetBom.setMetadata(metadata);

    targetBom.addComponent(new Component());

    List<ProjectScanItem> result = new ArrayList<>();

    sbomResultHandler.processDependencyGraph(sourceBom, targetBom, result,
        new ThirdPartyFile("test-bom.xml", new Date()));

    assertThat(result).isEmpty();
    assertIdentityMetadata(targetBom, metadata);
  }

  @Test
  public void testProcessDependencyGraph_SourceGraphMissingLeafNodes_StillWorks() {
    Bom sourceBom = new Bom();
    Bom targetBom = new Bom();
    targetBom.addComponent(new Component());
    Metadata metadata = new Metadata();
    Component rootComponent = new Component();
    rootComponent.setName("root");
    rootComponent.setBomRef("root");
    rootComponent.setVersion("1.0");
    rootComponent.setPurl("pkg:npm/root@1.0");
    metadata.setComponent(rootComponent);
    targetBom.setMetadata(metadata);
    Dependency root = createDependencyList("root", "pkg:npm/direct1@1.0", "pkg:npm/direct2@2.0");
    Dependency d1 = createDependencyList("pkg:npm/direct1@1.0", "pkg:npm/d1t1@1.1");
    Dependency d2 = createDependencyList("pkg:npm/direct2@2.0", "pkg:npm/d2t1@1.1");
    sourceBom.setDependencies(Arrays.asList(root, d1, d2));

    List<ProjectScanItem> result = new ArrayList<>();

    sbomResultHandler.processDependencyGraph(sourceBom, targetBom, result,
        new ThirdPartyFile("test-bom.xml", new Date()));

    assertThat(result).hasSize(1).allSatisfy(projectItem -> {
      assertThat(projectItem.getKind()).isEqualTo("sbom");
      assertThat(projectItem.getId()).isEqualTo("pkg:npm/root@1.0");
      assertThat(projectItem.getPath()).isEqualTo("test-bom.xml");
      List<com.sonatype.insight.scan.model.Dependency> rootDependencies = projectItem.getDependencies();
      assertThat(rootDependencies).hasSize(4)
          .extracting(com.sonatype.insight.scan.model.Dependency::getId)
          .containsExactlyInAnyOrder("pkg:npm/direct1@1.0", "pkg:npm/direct2@2.0",
              "pkg:npm/d1t1@1.1", "pkg:npm/d2t1@1.1");
      assertParentAndChildDependency(rootDependencies,"pkg:npm/direct1@1.0", "pkg:npm/d1t1@1.1");
      assertParentAndChildDependency(rootDependencies,"pkg:npm/direct2@2.0", "pkg:npm/d2t1@1.1");
    });
    assertIdentityMetadata(targetBom, metadata);
  }

  private void assertIdentityMetadata(final Bom targetBom, final Metadata metadata) {
    assertThat(metadata.getTimestamp()).isEqualTo(targetBom.getMetadata().getTimestamp());
    Component projectComponent = targetBom.getMetadata().getComponent();
    Component expectedComponent = metadata.getComponent();
    assertThat(projectComponent.getBomRef()).isEqualTo(expectedComponent.getBomRef());
    assertThat(projectComponent.getPurl()).isNotNull().isEqualTo(expectedComponent.getPurl());
    assertThat(projectComponent.getGroup()).isEqualTo(expectedComponent.getGroup());
    assertThat(projectComponent.getName()).isEqualTo(expectedComponent.getName());
    assertThat(projectComponent.getVersion()).isEqualTo(expectedComponent.getVersion());
    assertThat(projectComponent.getType()).isEqualTo(expectedComponent.getType());
  }

  private void assertParentAndChildDependency(
      final List<com.sonatype.insight.scan.model.Dependency> rootDependencies,
      final String parentPurl,
      final String childPurl)
  {
    com.sonatype.insight.scan.model.Dependency parent =
        rootDependencies.stream().filter(d -> d.getId().equals(parentPurl)).findFirst().get();
    if (childPurl != null) {
      assertThat(parent.getDependencies().get(0).getId()).isEqualTo(childPurl);
    }
  }

  @Test
  public void testProcessDependencyGraph_UnsortedGraph() {
    //given
    Bom sourceBom = new Bom();
    Bom targetBom = new Bom();
    targetBom.addComponent(new Component());
    Metadata metadata = new Metadata();
    Component rootComponent = new Component();
    rootComponent.setName("root");
    rootComponent.setBomRef("root");
    rootComponent.setVersion("1.0");
    rootComponent.setPurl("pkg:npm/root@1.0");
    metadata.setComponent(rootComponent);
    targetBom.setMetadata(metadata);
    Dependency root = createDependencyList("root", "pkg:npm/direct1@1.0");
    Dependency d2 = createDependencyList("pkg:npm/direct2@2.0", "pkg:npm/d2t1@1.1");
    Dependency d2t1 = d2.getDependencies().get(0);
    sourceBom.setDependencies(Arrays.asList(root, d2, d2t1));
    List<ProjectScanItem> result = new ArrayList<>();

    //when
    sbomResultHandler.processDependencyGraph(sourceBom, targetBom, result,
        new ThirdPartyFile("test-bom.xml", new Date()));

    //then
    assertThat(result).hasSize(1).allSatisfy(projectItem -> {
      assertThat(projectItem.getKind()).isEqualTo("sbom");
      assertThat(projectItem.getId()).isEqualTo("pkg:npm/root@1.0");
      assertThat(projectItem.getPath()).isEqualTo("test-bom.xml");
      List<com.sonatype.insight.scan.model.Dependency> resultDependencies = projectItem.getDependencies();
      assertThat(resultDependencies).hasSize(3) // can resolve direct dependencies from root and transitive dependencies
          .extracting(com.sonatype.insight.scan.model.Dependency::getId)
          .containsExactlyInAnyOrder("pkg:npm/direct2@2.0", "pkg:npm/direct1@1.0", "pkg:npm/d2t1@1.1");
    });
    assertIdentityMetadata(targetBom, metadata);
  }

  private Dependency createDependencyList(String parentPurl, String... childPurls) {
    Dependency parent = new Dependency(parentPurl);
    Stream.of(childPurls).forEach(childPurl -> parent.addDependency(new Dependency(childPurl)));
    return parent;
  }

  @Test
  public void testHandleAndFilterContents_sbom_coords_no_purl() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-coords-no-purl.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("scan-with-sbom-coords-no-purl.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 2);
  }

  @Test
  public void testHandleAndFilterContents_sbom_no_name_and_version_no_purl() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-no-name-and-version-no-purl.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("scan-with-sbom-no-name-and-version-no-purl.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1, true);
  }

  @Test
  public void testHandleAndFilterContents_sbom_no_name_no_purl() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-no-name-no-purl.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("scan-with-sbom-no-name-no-purl.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 2, true);
  }

  @Test
  public void testHandleAndFilterContents_invalidPurl_invalidCoords() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-invalid-purl-invalid-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-invalid-purl-invalid-coords.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isNotNull();
    assertDebugLogOutput("Fallback to coordinates due to invalid purl: pkg:pypi/@1.2.3");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_invalidPurl_validCoords() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-invalid-purl-valid-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-invalid-purl-valid-coords.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);
    assertDebugLogOutput("Fallback to coordinates due to invalid purl: pkg:pypi/@1.2.3");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
  }

  @Test
  public void testHandleAndFilterContents_validPurl_noMandatoryValue() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-invalid-valid-purl-no-mandatory-value.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-invalid-valid-purl-no-mandatory-value.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);
    assertDebugLogOutput("PackageUrl is not valid pkg:pypi/django");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
    assertThat(coordinates).extracting(ThirdPartyFileCoordinate::getName).containsOnly("django");
    assertThat(coordinates).extracting(ThirdPartyFileCoordinate::getVersion).containsOnly("1.11.1");
  }

  @Test
  public void testHandleAndFilterContents_unknownFormatPurl() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-unknow-format-purl.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-unknow-format-purl.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
  }

  @Test
  public void testDetermineIdentificationSource() {
    assertThat(sbomResultHandler.determineIdentificationSource("abcd-bom.xml")).isEqualTo("abcd");
    assertThat(sbomResultHandler.determineIdentificationSource("ABCD123-BOM.XmL")).isEqualTo("ABCD123");
    assertThat(sbomResultHandler.determineIdentificationSource("sub/dir/abcd-bom.xml")).isEqualTo("abcd");

    assertThat(sbomResultHandler.determineIdentificationSource("ABCD-SBOM.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineIdentificationSource("abcdbom.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineIdentificationSource("bom.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineIdentificationSource("BOM.XML")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineIdentificationSource("-bom.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineIdentificationSource("sub/dir/bom.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineIdentificationSource("")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineIdentificationSource(null)).isEqualTo("Third-Party");
  }

  @Test
  public void testHandleAndFilterContents_truncate() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-truncate-coordinates-for-hds.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-truncate-coordinates-for-hds.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);

    // check filtered content (will be sent to HDS) has been truncated
    Bom filteredSbom = getBom(filteredContent);
    Component component = filteredSbom.getComponents().get(0);
    assertThat(component.getName()).hasSize(NAME_MAX_LENGTH);
    assertThat(component.getVersion()).hasSize(VERSION_MAX_LENGTH);

    assertThat(component.getPurl()).hasSize(PURL_MAX_LENGTH);
    PackageUrlIdentifier packageUrlIdentifier = new PackageUrlIdentifier(component.getPurl());
    assertThat(packageUrlIdentifier.getFormat()).hasSize(FORMAT_MAX_LENGTH);
    assertThat(packageUrlIdentifier.getName()).hasSize(NAME_MAX_LENGTH);
    assertThat(packageUrlIdentifier.getVersion()).hasSize(VERSION_MAX_LENGTH);

    // check third party coordinates (stored in IQ) has been truncated
    ThirdPartyFileCoordinate coordinate =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId()).get(0);
    assertThat(coordinate.getFormat()).hasSize(FORMAT_MAX_LENGTH);
    assertThat(coordinate.getName()).hasSize(NAME_MAX_LENGTH);
    assertThat(coordinate.getVersion()).hasSize(VERSION_MAX_LENGTH);
    assertThat(coordinate.getPackageUrl()).hasSize(PURL_MAX_LENGTH);

    ThirdPartyCoordinateSecurity coordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(coordinate.getId()).get(0);
    assertThat(coordinateSecurity.getSeverityDescription()).isNotNull();
    assertThat(coordinateSecurity.getRatingMethod()).isNotNull();
    assertThat(coordinateSecurity.getLink()).hasSize(LINK_MAX_LENGTH);
    assertThat(coordinateSecurity.getRefId()).hasSize(REFID_MAX_LENGTH);
    assertThat(coordinateSecurity.getVulnerabilitySource()).hasSize(VULNERABILITY_SOURCE_MAX_LENGTH);
    assertThat(coordinateSecurity.getAttackVector()).hasSize(ATTACK_VECTOR_MAX_LENGTH);
    assertThat(coordinateSecurity.getRefId()).hasSize(REFID_MAX_LENGTH);
  }

  private String getSbomFile(final String fileType, final String fileName) throws Exception {
    URL resource = getClass().getResource("/SbomResultsHandlerTest/" + fileType + "/" + fileName);
    return new String(Files.readAllBytes(Paths.get(resource.toURI())), StandardCharsets.UTF_8);
  }

  private String getSbomXmlFile(final String fileName) throws Exception {
    return getSbomFile("xml", fileName);
  }

  private String getSbomJsonFile(final String fileName) throws Exception {
    return getSbomFile("json", fileName);
  }

  private Bom assertFilteredSbomFile(final String content, final int expectedComponentCount) throws Exception {
    return assertFilteredSbomFile(content, expectedComponentCount, false);
  }

  private Bom assertFilteredSbomFile(final String content, final int expectedComponentCount, final boolean optional)
      throws Exception
  {
    assertThat(content).isNotNull();
    Bom bom = getBom(content);
    assertThat(bom).isNotNull();
    assertThat(bom.getComponents()).hasSize(expectedComponentCount);

    for (Component component : bom.getComponents()) {
      assertThat(component.getComponents()).isNull();
      if (!optional) {
        assertThat(component.getName()).isNotNull();
        assertThat(component.getVersion()).isNotNull();
      }
      assertThat(component.getType()).isNotNull();

      assertThat(component.getLicenseChoice()).isNull();
      assertThat(component.getAuthor()).isNull();
      assertThat(component.getCopyright()).isNull();
      assertThat(component.getEvidence()).isNull();
      assertThat(component.getPedigree()).isNull();
      assertThat(component.getHashes()).isNull();
      assertThat(component.getExternalReferences()).isNull();
      assertThat(component.getSwid()).isNull();
      assertThat(component.getExtensibleTypes()).isNull();
      assertThat(component.getExtensions()).isNull();
    }
    assertThat(bom.getCompositions()).isNull();
    assertThat(bom.getServices()).isNull();
    assertThat(bom.getExternalReferences()).isNull();
    assertThat(bom.getProperties()).isNull();
    assertThat(bom.getExtensibleTypes()).isNull();
    assertThat(bom.getExtensions()).isNull();
    return bom;
  }

  @Test
  public void testParseFilesV11AndV12AndV13And14() throws Exception {
    ThirdPartyScanContent contentV11 =
        new ThirdPartyScanContent("sbom-simple-v1-1.xml", null, null, null, getSbomXmlFile("sbom-simple-v1-1.xml"));
    Bom bomV11 = sbomResultHandler.parseBom(contentV11);
    ThirdPartyScanContent contentV12 =
        new ThirdPartyScanContent("sbom-simple-v1-2.xml", null, null, null, getSbomXmlFile("sbom-simple-v1-2.xml"));
    Bom bomV12 = sbomResultHandler.parseBom(contentV12);
    ThirdPartyScanContent contentV13 =
        new ThirdPartyScanContent("sbom-simple-v1_3.xml", null, null, null, getSbomXmlFile("sbom-simple-v1_3.xml"));
    Bom bomV13 = sbomResultHandler.parseBom(contentV13);
    ThirdPartyScanContent contentV14 =
        new ThirdPartyScanContent("sbom-simple-v1_4.xml", null, null, null, getSbomXmlFile("sbom-simple-v1_4.xml"));
    Bom bomV14 = sbomResultHandler.parseBom(contentV14);
    ThirdPartyScanContent contentV14json =
        new ThirdPartyScanContent("sbom-simple-v1-4.json", null, null, null, getSbomJsonFile("sbom-simple-v1-4.json"));
    Bom bomV14json = sbomResultHandler.parseBom(contentV14json);
    assertThat(bomV11).isNotNull();
    assertThat(bomV11.getSpecVersion()).isEqualTo("1.1");
    assertThat(bomV12).isNotNull();
    assertThat(bomV12.getSpecVersion()).isEqualTo("1.2");
    assertThat(bomV13).isNotNull();
    assertThat(bomV13.getSpecVersion()).isEqualTo("1.3");
    assertThat(bomV14).isNotNull();
    assertThat(bomV14.getSpecVersion()).isEqualTo("1.4");
    assertThat(bomV14json).isNotNull();
    assertThat(bomV14json.getSpecVersion()).isEqualTo("1.4");

    assertThat(bomV11.getComponents()).hasSameElementsAs(bomV12.getComponents())
        .hasSameElementsAs(bomV13.getComponents()).hasSameElementsAs(bomV14.getComponents())
        .hasSameElementsAs(bomV14json.getComponents());
  }

  @Test
  public void testParseInvalidJsonVersion() throws Exception {
    ThirdPartyScanContent contentJson =
        new ThirdPartyScanContent("sbom-simple.json", null, null, null, getSbomJsonFile("sbom-simple.json"));
    assertThatExceptionOfType(InvalidSbomException.class)
        .isThrownBy(() ->  sbomResultHandler.parseBom(contentJson))
        .withMessage("CycloneDX JSON 1.2 version is not supported");
  }

  @Test
  public void testParseFilesV11AndV12WithLicenses() throws Exception {
    ThirdPartyScanContent contentV11 =
        new ThirdPartyScanContent("sbom-licenses-v1-1.xml", null, null, null, getSbomXmlFile("sbom-licenses-v1-1.xml"));
    Bom bomV11 = sbomResultHandler.parseBom(contentV11);
    ThirdPartyScanContent contentV12 =
        new ThirdPartyScanContent("sbom-licenses-v1-2.xml", null, null, null, getSbomXmlFile("sbom-licenses-v1-2.xml"));
    Bom bomV12 = sbomResultHandler.parseBom(contentV12);
    assertThat(bomV11).isNotNull();
    assertThat(bomV12).isNotNull();
    assertThat(bomV11.getComponents()).hasSameElementsAs(bomV12.getComponents());
  }

  @Test
  public void testParseXmlFilesV11AndV12andV13WithVulnerabilities() throws Exception {
    ThirdPartyScanContent contentV11 = new ThirdPartyScanContent("sbom-vulnerabilities-v1_1.xml", null, null, null,
        getSbomXmlFile("sbom-vulnerabilities-v1_1.xml"));
    Bom bomV11 = sbomResultHandler.parseBom(contentV11);
    ThirdPartyScanContent contentV12 = new ThirdPartyScanContent("sbom-vulnerabilities-v1_2.xml", null, null, null,
        getSbomXmlFile("sbom-vulnerabilities-v1_2.xml"));
    Bom bomV12 = sbomResultHandler.parseBom(contentV12);
    ThirdPartyScanContent contentV13 = new ThirdPartyScanContent("sbom-vulnerabilities-v1_3.xml", null, null, null,
        getSbomXmlFile("sbom-vulnerabilities-v1_3.xml"));
    Bom bomV13 = sbomResultHandler.parseBom(contentV13);
    assertThat(bomV11.getComponents()).isNotEmpty().allSatisfy(this::assertExtensionVulnerabilities);
    assertThat(bomV12.getComponents()).isNotEmpty().allSatisfy(this::assertExtensionVulnerabilities);
    assertThat(bomV13.getComponents()).isNotEmpty().allSatisfy(this::assertExtensionVulnerabilities);
  }

  @Test
  public void testParseXmlFilesV14WithVulnerabilities() throws Exception {
    ThirdPartyScanContent contentV14 = new ThirdPartyScanContent("sbom-vulnerabilities-v1_4.xml", null, null, null,
        getSbomXmlFile("sbom-vulnerabilities-v1_4.xml"));
    Bom bomV14 = sbomResultHandler.parseBom(contentV14);
    assertThat(bomV14.getVulnerabilities()).isNotEmpty().allSatisfy(this::assertVulnerability);
  }

  @Test
  public void testHandleAndFilterContents_only_coordinates_hash_purl() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-license-vulnerability.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-component-license-vulnerability.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();

    // check filtered content (will be sent to HDS) has only coordinates, hash or purl
    Bom filteredSbom = getBom(filteredContent);
    assertFilteredSbomFile(filteredContent, 3);
    List<Component> components = filteredSbom.getComponents();
    assertThat(components).hasSize(3)
        .allSatisfy(component -> {
          assertThat(component.getExtensions()).isNull();
          assertThat(component.getLicenseChoice()).isNull();
          assertThat(component.getName()).isNotNull();
          assertThat(component.getVersion()).isNotNull();
          assertThat(component.getType()).isNotNull();
        });
    Component component1 = components.get(0);
    Component component2 = components.get(1);
    Component component3 = components.get(2);
    assertThat(component1.getName()).isEqualTo("jackson-databind");
    assertThat(component2.getName()).isEqualTo("tomcat-catalina");
    assertThat(component3.getName()).isEqualTo("sample-library");
    assertThat(component1.getVersion()).isEqualTo("2.9.9");
    assertThat(component2.getVersion()).isEqualTo("9.0.14");
    assertThat(component3.getVersion()).isEqualTo("1.0.0");
    assertThat(component1.getPurl()).isEqualTo("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar");
    assertThat(component2.getPurl()).isNull();
    assertThat(component3.getPurl()).isNull();
    assertThat(component1.getHashes()).isNull();
    assertThat(component2.getHashes()).isNull();
    assertThat(component3.getHashes()).isNull();
    assertThat(component1.getProperties()).isNull();
    assertThat(component2.getProperties()).hasSize(1);
    assertThat(component2.getProperties().get(0).getName()).isEqualTo(SbomUtils.SONATYPE_HASH_PROPERTY_NAME);
    assertThat(component2.getProperties().get(0).getValue()).isEqualTo("e6b1000b94e835ffd37f");
    assertThat(component3.getProperties()).hasSize(1);
    assertThat(component3.getProperties().get(0).getName()).isEqualTo(SbomUtils.SONATYPE_HASH_PROPERTY_NAME);
    assertThat(component3.getProperties().get(0).getValue()).isEqualTo("716e4909ac2db42da409");
  }

  @Test
  public void testHandleAndFilterContents_invalidPurl_missingCoords() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-invalid-purl-missing-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-invalid-purl-missing-coords.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);
    assertDebugLogOutput("Component jackson-databind 2.9.9 is missing coordinates." +
        " The following coordinates are missing: [type]");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
  }

  private void assertExtensionVulnerabilities(Component component) {
    Map<String, Extension> extensions = component.getExtensions();
    assertThat(extensions).isNotEmpty().containsKey(ExtensionType.VULNERABILITIES.getTypeName());
    Extension vulnerabilityExtension = extensions.get(ExtensionType.VULNERABILITIES.getTypeName());
    assertThat(vulnerabilityExtension.getExtensionType()).isEqualTo(ExtensionType.VULNERABILITIES);
    assertThat(vulnerabilityExtension.getNamespaceURI()).isEqualTo(Vulnerability10.NAMESPACE_URI);
    assertThat(vulnerabilityExtension.getPrefix()).isEqualTo(Vulnerability10.PREFIX);
    assertThat(vulnerabilityExtension.getExtensions()).isNotEmpty().hasSize(1);
    Consumer<Vulnerability10> vulnerabilitiesRequirement = vulnerability -> {
      assertThat(vulnerability.getId()).isEqualTo("CVE-2018-7489");
      assertThat(vulnerability.getRef()).isEqualTo(
          "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar");
      assertThat(vulnerability.getCwes()).hasSize(2);
      assertThat(vulnerability.getCwes().get(0).getText()).isEqualTo(184);
      assertThat(vulnerability.getCwes().get(1).getText()).isEqualTo(502);
      assertThat(vulnerability.getRatings()).hasSize(1);
      Rating rating = vulnerability.getRatings().get(0);
      assertThat(rating.getMethod()).isEqualTo(ScoreSource.CVSSv3);
      assertThat(rating.getSeverity()).isEqualTo(Severity.CRITICAL);
      assertThat(rating.getVector()).isEqualTo("AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");
      assertThat(rating.getScore().getBase()).isEqualTo(9.8);
      assertThat(rating.getScore().getExploitability()).isEqualTo(3.0);
      assertThat(rating.getScore().getImpact()).isEqualTo(5.9);
      assertThat(vulnerability.getDescription()).isNotEmpty();
      assertThat(vulnerability.getAdvisories()).hasSize(4);
      assertThat(vulnerability.getRecommendations()).hasSize(1);
    };
    assertThat(vulnerabilityExtension.getExtensions()).allSatisfy(extensibleType ->
        assertThat(extensibleType).isInstanceOfSatisfying(Vulnerability10.class, vulnerabilitiesRequirement)
    );
  }

  private void assertVulnerability(
      ThirdPartyCoordinateSecurity coordinateSecurity,
      Vulnerability vulnerability,
      String coordinateId,
      boolean optionalValuesPresent)
  {
    assertThat(coordinateSecurity).isNotNull();
    assertThat(coordinateSecurity.getFileCoordinateId()).isEqualTo(coordinateId);
    assertThat(coordinateSecurity.getFixedBy()).isNull();

    assertThat(coordinateSecurity.getRefId()).isEqualTo(vulnerability.getId());

    Vulnerability.Rating rating = vulnerability.getRatings().get(0);
    Float severityExpected = new Float(rating.getScore());
    assertThat(coordinateSecurity.getSeverity()).isEqualTo(severityExpected);

    if (optionalValuesPresent) {
      assertThat(coordinateSecurity.getSeverityDescription()).isEqualTo(rating.getSeverity().getSeverityName());
      assertThat(coordinateSecurity.getRatingMethod()).isEqualTo(rating.getMethod().getMethodName());
      assertThat(coordinateSecurity.getAttackVector()).isEqualTo(rating.getVector());

      Vulnerability.Source source = vulnerability.getSource();
      assertThat(coordinateSecurity.getVulnerabilitySource()).isEqualTo(source.getName());
      assertThat(coordinateSecurity.getCwes()).isNotNull();
      assertThat(coordinateSecurity.getRecommendations()).isNotNull();
      assertThat(coordinateSecurity.getAdvisories()).isNotNull();
      assertThat(coordinateSecurity.getLink()).isEqualTo(source.getUrl());
      assertThat(coordinateSecurity.getDescription()).isEqualTo(vulnerability.getDescription());
    }
    else {
      assertThat(coordinateSecurity.getCwes()).isNull();
      assertThat(coordinateSecurity.getRecommendations()).isNull();
      assertThat(coordinateSecurity.getAdvisories()).isNull();
      assertThat(coordinateSecurity.getAttackVector()).isNull();
      assertThat(coordinateSecurity.getLink()).isNull();
      assertThat(coordinateSecurity.getVulnerabilitySource()).isNull();

      assertThat(coordinateSecurity.getSeverityDescription()).isNull();
      assertThat(coordinateSecurity.getRatingMethod()).isNull();
      assertThat(coordinateSecurity.getSeverityDescription()).isNull();
      assertThat(coordinateSecurity.getAttackVector()).isNull();
      assertThat(coordinateSecurity.getDescription()).isNull();
    }
  }

  private void assertVulnerability(Vulnerability vulnerability) {
    assertThat(vulnerability.getId()).isEqualTo("CVE-2018-7489");
    assertThat(vulnerability.getBomRef()).isNull();

    //Cwes
    assertThat(vulnerability.getCwes()).hasSize(2);
    assertThat(vulnerability.getCwes().get(0)).isEqualTo(184);
    assertThat(vulnerability.getCwes().get(1)).isEqualTo(502);

    //Source
    assertThat(vulnerability.getSource()).isNotNull();
    assertThat(vulnerability.getSource().getName()).isNotNull();
    assertThat(vulnerability.getSource().getUrl()).isNotNull();

    //Rating
    assertThat(vulnerability.getRatings()).hasSize(1);
    Vulnerability.Rating rating = vulnerability.getRatings().get(0);
    assertThat(rating.getMethod()).isEqualTo(Method.CVSSV3);
    assertThat(rating.getSeverity()).isEqualTo(Vulnerability.Rating.Severity.CRITICAL);

    assertThat(rating.getSource().getUrl()).isNotEmpty();
    assertThat(rating.getSource().getName()).isEqualTo("NVD");
    assertThat(rating.getVector()).isEqualTo("AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");
    assertThat(rating.getJustification()).isNull();

    assertThat(rating.getScore()).isEqualTo(9.8);

    assertThat(vulnerability.getDescription()).isNotEmpty();
    assertThat(vulnerability.getAdvisories()).hasSize(4);
    assertThat(vulnerability.getRecommendation()).isNotEmpty();

    assertThat(vulnerability.getDetail()).isNull();
    assertThat(vulnerability.getCreated()).isNull();
    assertThat(vulnerability.getPublished()).isNull();
    assertThat(vulnerability.getUpdated()).isNull();

    //Affects
    assertThat(vulnerability.getAffects()).hasSize(1);
    assertThat(vulnerability.getAffects().get(0).getRef()).isEqualTo(
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar");
  }

  private Bom getBom(String content) throws ParseException {
    Parser parser = new XmlParser();
    return parser.parse(new StringReader(content));
  }

  private void assertThirdPartyFileCoordinate(
      Component component,
      ThirdPartyFile thirdPartyFile,
      ThirdPartyFileCoordinate coordinate)
  {
    assertThirdPartyFileCoordinate(component, thirdPartyFile, coordinate, false);
  }

  private void assertThirdPartyFileCoordinate(
      Component component,
      ThirdPartyFile thirdPartyFile,
      ThirdPartyFileCoordinate coordinate,
      boolean noPurl)
  {
    if (!noPurl) {
      PackageUrlIdentifier packageUrl = new PackageUrlIdentifier(component.getPurl());

      String format = ThirdPartyScanResultUtils.getValidFormat(packageUrl.getFormat());
      ComponentIdentifier ci = new ComponentIdentifier(format, packageUrl.toComponentIdentifier().getCoordinates());
      assertThat(coordinate.getFormat()).isEqualTo(ThirdPartyScanResultUtils.getValidFormat(ci.getFormat()));
      assertThat(coordinate.getPackageUrl())
          .isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(ci).getPackageUrl());
    }
    else {
      assertThat(coordinate.getFormat()).isEqualTo(component.getType().getTypeName());
      assertThat(coordinate.getPackageUrl()).isNull();
    }

    assertThat(coordinate.getHash()).isNotBlank();
    assertThat(coordinate.getName()).isEqualTo(component.getName());
    assertThat(coordinate.getThirdPartyFileId()).isEqualTo(thirdPartyFile.getId());
    assertThat(coordinate.getVersion()).isEqualTo(component.getVersion());
  }

  private void assertThirdPartyCoordinateSecurity(
      String content,
      String coordinateId,
      ThirdPartyCoordinateSecurity coordinateSecurity,
      boolean optionalValuesPresent) throws ParseException, RuntimeException
  {
    assertThirdPartyCoordinateSecurity(content, coordinateId, coordinateSecurity, optionalValuesPresent, true);
  }

  private void assertThirdPartyCoordinateSecurity(
      String content,
      String coordinateId,
      ThirdPartyCoordinateSecurity coordinateSecurity,
      boolean optionalValuesPresent, boolean extensionVulnerability) throws ParseException, RuntimeException
  {
    Bom expectedBom = ThirdPartyUtils.parseBom(content);

    if (extensionVulnerability) {
      assertExtensionVulnerability(coordinateSecurity,
          (Vulnerability10) expectedBom.getComponents().get(0).getExtensions().get("vulnerabilities").getExtensions()
              .get(0), coordinateId, optionalValuesPresent);
    }
    else {
      assertVulnerability(coordinateSecurity, expectedBom.getVulnerabilities().get(0), coordinateId,
          optionalValuesPresent);
    }
  }

  private void assertThirdPartyCoordinateSecurities(
      String content,
      List<ThirdPartyCoordinateSecurity> actualVulnerabilities) throws ParseException, RuntimeException
  {
    Bom expectedBom = ThirdPartyUtils.parseBom(content);

    List<ThirdPartyCoordinateSecurity> expectedVulnerabilities = new ArrayList<>();

    for (Component component : expectedBom.getComponents()) {
      List<ExtensibleType> vulnerabilitiesSbom = component.getExtensions().get("vulnerabilities").getExtensions();
      for (ExtensibleType vulnerabilities : vulnerabilitiesSbom) {
        expectedVulnerabilities.add(
            sbomResultHandler.parseVulnerabilityExtension((Vulnerability10) vulnerabilities, null));
      }
    }
    assertThat(expectedVulnerabilities)
        .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
            .withIgnoredFields("id", "fileCoordinateId", "pcStateManager", "pcDetachedState").build())
        .containsExactlyInAnyOrderElementsOf(actualVulnerabilities);
  }

  private void assertThirdPartyCoordinateLicense(
      License licenseSbom,
      String coordinateId,
      ThirdPartyCoordinateLicense coordinateLicense)
  {
    com.sonatype.insight.brain.model.license.License sonatypeLicense = null;
    if (StringUtils.isNotEmpty(licenseSbom.getId())) {
      sonatypeLicense = licenseDAO.getById(licenseSbom.getId());
    }
    else if (StringUtils.isNotEmpty(licenseSbom.getName())) {
      sonatypeLicense = licenseDAO.getByName(licenseSbom.getName());
    }
    if (sonatypeLicense != null) {
      assertThat(coordinateLicense.getLicenseId()).isEqualTo(sonatypeLicense.getId());
      assertThat(coordinateLicense.getName()).isEqualTo(sonatypeLicense.getShortDisplayName());
    }
    else {
      assertThat(coordinateLicense.getLicenseId()).isEqualTo(licenseSbom.getId());
      assertThat(coordinateLicense.getName()).isEqualTo(licenseSbom.getName());
    }
    assertThat(coordinateLicense.getUrl()).isEqualTo(licenseSbom.getUrl());
    assertThat(coordinateLicense.getFileCoordinateId()).isEqualTo(coordinateId);
  }

  private void assertExtensionVulnerability(
      ThirdPartyCoordinateSecurity coordinateSecurity,
      Vulnerability10 vulnerability,
      String coordinateId,
      boolean optionalValuesPresent)
  {
    assertThat(coordinateSecurity).isNotNull();
    assertThat(coordinateSecurity.getFileCoordinateId()).isNotNull();
    assertThat(coordinateSecurity.getFileCoordinateId()).isEqualTo(coordinateId);
    assertThat(coordinateSecurity.getFixedBy()).isNull();

    assertThat(coordinateSecurity.getRefId()).isEqualTo(vulnerability.getId());

    Rating rating = vulnerability.getRatings().get(0);
    Float severityExpected = new Float(rating.getScore().getBase());
    assertThat(coordinateSecurity.getSeverity()).isEqualTo(severityExpected);

    if (optionalValuesPresent) {
      assertThat(coordinateSecurity.getSeverityDescription()).isEqualTo(rating.getSeverity().getSeverityName());
      assertThat(coordinateSecurity.getRatingMethod()).isEqualTo(rating.getMethod().getScoreSourceName());
      assertThat(coordinateSecurity.getAttackVector()).isEqualTo(rating.getVector());

      Vulnerability10.Source source = vulnerability.getSource();
      assertThat(coordinateSecurity.getVulnerabilitySource()).isEqualTo(source.getName());
      assertThat(coordinateSecurity.getCwes()).isNotNull();
      assertThat(coordinateSecurity.getRecommendations()).isNotNull();
      assertThat(coordinateSecurity.getAdvisories()).isNotNull();
      assertThat(coordinateSecurity.getLink()).isEqualTo(source.getUrl().toString());
      assertThat(coordinateSecurity.getDescription()).isEqualTo(vulnerability.getDescription());
    }
    else {
      assertThat(coordinateSecurity.getCwes()).isNull();
      assertThat(coordinateSecurity.getRecommendations()).isNull();
      assertThat(coordinateSecurity.getAdvisories()).isNull();
      assertThat(coordinateSecurity.getAttackVector()).isNull();
      assertThat(coordinateSecurity.getLink()).isNull();
      assertThat(coordinateSecurity.getVulnerabilitySource()).isNull();

      assertThat(coordinateSecurity.getSeverityDescription()).isNull();
      assertThat(coordinateSecurity.getRatingMethod()).isNull();
      assertThat(coordinateSecurity.getSeverityDescription()).isNull();
      assertThat(coordinateSecurity.getAttackVector()).isNull();
      assertThat(coordinateSecurity.getDescription()).isNull();
    }
  }

  private void assertDebugLogOutput(final String message) {
    assertThat(logOutput.getDebugMessages(loggerName)).contains(message);
  }
}
