/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
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
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExtensibleType;
import org.cyclonedx.model.Extension;
import org.cyclonedx.model.Extension.ExtensionType;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.License;
import org.cyclonedx.model.Source;
import org.cyclonedx.model.vulnerability.Rating;
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

  @Spy
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Spy
  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private final String loggerName = SbomResultHandler.class.getName();

  @Rule
  public LogOutput logOutput = new LogOutput(loggerName);

  @Test
  public void testHandleAndFilterContents_filterContent_newThirdPartyFileMultipleEntries() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-multiple-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("clair-bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertFilteredSbomFile(filteredContent, 2);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(2);
    assertThat(coordinates).allSatisfy(coord -> {
      assertThat(coord.getSource()).isEqualTo("clair");
    });
  }

  @Test
  public void testHandleAndFilterContents_veryLongIdentificationSource() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-multiple-components.xml");
    String identificationSource = "identification-source-very-long";
    ThirdPartyScanContent content =
        new ThirdPartyScanContent(identificationSource + "-bom.xml", null, null, null,
            sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    Bom bom = assertFilteredSbomFile(filteredContent, 4);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName)
        .containsOnly("tomcat-catalina", "django", "jackson-databind", "joda-time");
    assertThat(components).extracting(Component::getVersion)
        .containsOnly("9.0.14", "1.2.3", "2.9.9", "2.1.0");
    assertThat(components).extracting(Component::getPurl)
        .containsExactlyInAnyOrder("pkg:maven/org.apache.tomcat/tomcat-catalina@9.0.14?packaging=jar", null,
            "pkg:library/com.fasterxml.jackson.core/jackson-databind@2.9.9", null);
    assertThat(components).extracting("hashes.size")
        .containsOnly(null, 1, null, 1);
    assertThat(components.get(1).getHashes())
        .flatExtracting(Hash::getValue, Hash::getAlgorithm)
        .contains("e6b1000b94e835ffd37f", "SHA-1");
    assertThat(components.get(3).getHashes())
        .flatExtracting(Hash::getValue, Hash::getAlgorithm)
        .contains("f498a8ff2dd00", "SHA-1");
  }

  @Test
  public void testHandleAndFilterContents_filterContent_hashes() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-hashes-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("clair-bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
    assertThat(components).extracting("hashes.size")
        .containsOnly(1, null);
    assertThat(components.get(0).getHashes())
        .flatExtracting(Hash::getValue, Hash::getAlgorithm)
        .contains("e6b1000b94e835ffd37f", "SHA-1");
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertFilteredSbomFile(filteredContent, 1, true);
    thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId()).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_withLicense() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-license.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
      assertThat(coordinatesLicense).hasSize(1);
      assertThirdPartyCoordinateLicense(unfilteredSbom.getComponents().get(0), thirdPartyFileCoordinate.getId(),
          coordinatesLicense.get(0));
    }
  }
  
  @Test
  public void testHandleAndFilterContents_withComponentDuplicatedLicenseAndVulnerability() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-duplicated-component-license-vulnerability.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
    assertNoLicense(sbomContent);
  }

  @Test
  public void testHandleAndFilterContents_licenseMissingId() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-license-no-id.xml");
    assertNoLicense(sbomContent);
  }

  @Test
  public void testHandleAndFilterContents_missingLicenses() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-missing-licenses.xml");
    assertNoLicense(sbomContent);
  }

  @Test
  public void testHandleAndFilterContents_emptyLicenses() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-empty-licenses.xml");
    assertNoLicense(sbomContent);
  }

  private void assertNoLicense(String sbomContent) throws Exception {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
  public void testHandleAndFilterContents_withVulnerabilities_noSeverity() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-no-severity.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
      assertThat(coordinatesSecurity).hasSize(0);
    }
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_invalidVulnerabilities() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-invalid-vulnerabilities.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
      assertThat(coordinatesSecurity).hasSize(0);
    }
  }

  @Test
  public void testHandleAndFilterContents_Sbom_Version_1_0() throws Exception {
    String sbomContent = getSbomXmlFile("sbom_1_0.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom_1_0.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
      sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    }).withMessage("Error filtering sbom file sbom_1_0.xml");
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_missingFields() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-missing-fields.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
    String sbomContent = getSbomXmlFile("sbom-no-vulnerabilities.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
      assertThat(coordinatesSecurity).hasSize(0);
    }
  }

  @Test
  public void testHandleAndFilterContents_duplicatedVulnerabilities() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-duplicated-vulnerabilities.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertFilteredSbomFile(filteredContent, 1);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
  }

  @Test
  public void testHandleAndFilterContents_nullContent() {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, null);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNull();
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(0);
  }

  @Test
  public void testHandleAndFilterContents_emptyContent() {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, "");
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isBlank();
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(0);
  }

  @Test
  public void testHandleAndFilterContents_invalidSbom() throws Exception {
    testHandleAndFilterContents_invalid_sbom(getSbomXmlFile("scan-with-invalid-sbom-data-cli.xml"),
        "scan-with-invalid-sbom-data-cli.xml");
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

  private void testHandleAndFilterContents_invalid_sbom(String sbomContent, String path) throws Exception {
    ThirdPartyScanContent content = new ThirdPartyScanContent(path, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> sbomResultHandler.handleAndFilterContents(content, thirdPartyFile))
        .withMessage("Error filtering sbom file " + path);
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(0);
  }

  @Test
  public void testHandleAndFilterContents_sbomNestedComponents() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-nested-component.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertFilteredSbomFile(filteredContent, 1);
  }

  @Test
  public void testHandleAndFilterContents_v1_2() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_2.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertFilteredSbomFile(filteredContent, 2);
  }

  @Test
  public void testHandleAndFilterContents_v1_3() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_3.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertFilteredSbomFile(filteredContent, 2);
  }

  @Test
  public void testHandleAndFilterContents_sbom_coords_no_purl() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-coords-no-purl.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertFilteredSbomFile(filteredContent, 2);
  }

  @Test
  public void testHandleAndFilterContents_sbom_no_name_and_version_no_purl() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-no-name-and-version-no-purl.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertFilteredSbomFile(filteredContent, 1, true);
  }

  @Test
  public void testHandleAndFilterContents_sbom_no_name_no_purl() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-no-name-no-purl.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertFilteredSbomFile(filteredContent, 2, true);
  }

  @Test
  public void testHandleAndFilterContents_invalidPurl_invalidCoords() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-invalid-purl-invalid-coords.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    assertWarnLogOutput("Fallback to coordinates due to invalid purl: pkg:pypi/@1.2.3");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(0);
  }

  @Test
  public void testHandleAndFilterContents_invalidPurl_validCoords() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-invalid-purl-valid-coords.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertFilteredSbomFile(filteredContent, 1);
    assertWarnLogOutput("Fallback to coordinates due to invalid purl: pkg:pypi/@1.2.3");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
  }

  @Test
  public void testHandleAndFilterContents_validPurl_noMandatoryValue() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-invalid-valid-purl-no-mandatory-value.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertFilteredSbomFile(filteredContent, 1);
    assertWarnLogOutput("PackageUrl is not valid pkg:pypi/django");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
    assertThat(coordinates).extracting(ThirdPartyFileCoordinate::getName).containsOnly("django");
    assertThat(coordinates).extracting(ThirdPartyFileCoordinate::getVersion).containsOnly("1.11.1");
  }

  @Test
  public void testHandleAndFilterContents_unknownFormatPurl() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-unknow-format-purl.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
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
    assertThat(coordinateSecurity.getSeverityDescription()).isNull();
    assertThat(coordinateSecurity.getRatingMethod()).isNull();
    assertThat(coordinateSecurity.getLink()).hasSize(LINK_MAX_LENGTH);
    assertThat(coordinateSecurity.getRefId()).hasSize(REFID_MAX_LENGTH);
    assertThat(coordinateSecurity.getVulnerabilitySource()).hasSize(VULNERABILITY_SOURCE_MAX_LENGTH);
    assertThat(coordinateSecurity.getAttackVector()).hasSize(ATTACK_VECTOR_MAX_LENGTH);
    assertThat(coordinateSecurity.getRefId()).hasSize(REFID_MAX_LENGTH);
  }

  private String getSbomFile(final String fileType, final String fileName) throws Exception {
    URL resource = getClass().getResource("/SbomResultsHandlerTest/" + fileType + "/" + fileName);
    return new String(Files.readAllBytes(Paths.get(resource.toURI())));
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
      assertThat(component.getProperties()).isNull();
      assertThat(component.getExternalReferences()).isNull();
      assertThat(component.getSwid()).isNull();
      assertThat(component.getExtensibleTypes()).isNull();
      assertThat(component.getExtensions()).isNull();
    }
    assertThat(bom.getMetadata()).isNull();
    assertThat(bom.getDependencies()).isNull();
    assertThat(bom.getCompositions()).isNull();
    assertThat(bom.getServices()).isNull();
    assertThat(bom.getExternalReferences()).isNull();
    assertThat(bom.getProperties()).isNull();
    assertThat(bom.getExtensibleTypes()).isNull();
    assertThat(bom.getExtensions()).isNull();
    return bom;
  }

  @Test
  public void testParseFilesV11AndV12AndV13() throws Exception {
    ThirdPartyScanContent contentV11 =
        new ThirdPartyScanContent(null, null, null, null, getSbomXmlFile("sbom-simple-v1-1.xml"));
    Bom bomV11 = sbomResultHandler.parseBom(contentV11);
    ThirdPartyScanContent contentV12 =
        new ThirdPartyScanContent(null, null, null, null, getSbomXmlFile("sbom-simple-v1-2.xml"));
    Bom bomV12 = sbomResultHandler.parseBom(contentV12);
    ThirdPartyScanContent contentJson =
        new ThirdPartyScanContent(null, null, null, null, getSbomJsonFile("sbom-simple.json"));
    Bom bomJson = sbomResultHandler.parseBom(contentJson);
    ThirdPartyScanContent contentV13 =
        new ThirdPartyScanContent(null, null, null, null, getSbomXmlFile("sbom-simple-v1_3.xml"));
    Bom bomV13 = sbomResultHandler.parseBom(contentV13);
    assertThat(bomV11).isNotNull();
    assertThat(bomV11.getSpecVersion()).isEqualTo("1.1");
    assertThat(bomV12).isNotNull();
    assertThat(bomV12.getSpecVersion()).isEqualTo("1.2");
    assertThat(bomJson).isNotNull();
    assertThat(bomJson.getSpecVersion()).isEqualTo("1.2");
    assertThat(bomV13).isNotNull();
    assertThat(bomV13.getSpecVersion()).isEqualTo("1.3");
    assertThat(bomV11.getComponents()).hasSameElementsAs(bomV12.getComponents())
        .hasSameElementsAs(bomJson.getComponents()).hasSameElementsAs(bomV13.getComponents());
  }

  @Test
  public void testParseFilesV11AndV12WithLicenses() throws Exception {
    ThirdPartyScanContent contentV11 =
        new ThirdPartyScanContent(null, null, null, null, getSbomXmlFile("sbom-licenses-v1-1.xml"));
    Bom bomV11 = sbomResultHandler.parseBom(contentV11);
    ThirdPartyScanContent contentV12 =
        new ThirdPartyScanContent(null, null, null, null, getSbomXmlFile("sbom-licenses-v1-2.xml"));
    Bom bomV12 = sbomResultHandler.parseBom(contentV12);
    ThirdPartyScanContent contentJson =
        new ThirdPartyScanContent(null, null, null, null, getSbomJsonFile("sbom-licenses.json"));
    Bom bomJson = sbomResultHandler.parseBom(contentJson);
    assertThat(bomV11).isNotNull();
    assertThat(bomV12).isNotNull();
    assertThat(bomJson).isNotNull();
    assertThat(bomV11.getComponents()).hasSameElementsAs(bomV12.getComponents())
        .hasSameElementsAs(bomJson.getComponents());
  }

  @Test
  public void testParseXmlFilesV11AndV12andV13WithVulnerabilities() throws Exception {
    ThirdPartyScanContent contentV11 =
        new ThirdPartyScanContent(null, null, null, null, getSbomXmlFile("sbom-vulnerabilities-v1_1.xml"));
    Bom bomV11 = sbomResultHandler.parseBom(contentV11);
    ThirdPartyScanContent contentV12 =
        new ThirdPartyScanContent(null, null, null, null, getSbomXmlFile("sbom-vulnerabilities-v1_2.xml"));
    Bom bomV12 = sbomResultHandler.parseBom(contentV12);
    ThirdPartyScanContent contentV13 =
        new ThirdPartyScanContent(null, null, null, null, getSbomXmlFile("sbom-vulnerabilities-v1_3.xml"));
    Bom bomV13 = sbomResultHandler.parseBom(contentV13);
    assertThat(bomV11.getComponents()).isNotEmpty().allSatisfy(this::assertVulnerabilities);
    assertThat(bomV12.getComponents()).isNotEmpty().allSatisfy(this::assertVulnerabilities);
    assertThat(bomV13.getComponents()).isNotEmpty().allSatisfy(this::assertVulnerabilities);
  }

  @Test
  public void testHandleAndFilterContents_only_coordinates_hash_purl() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-license-vulnerability.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    // check filtered content (will be sent to HDS) has only coordinates, hash or purl
    Bom filteredSbom = getBom(filteredContent);
    assertFilteredSbomFile(filteredContent, 2);
    List<Component> components = filteredSbom.getComponents();
    assertThat(components).hasSize(2)
        .allSatisfy(component -> {
          assertThat(component.getExtensions()).isNull();
          assertThat(component.getLicenseChoice()).isNull();
          assertThat(component.getName()).isNotNull();
          assertThat(component.getVersion()).isNotNull();
          assertThat(component.getType()).isNotNull();
        });
    Component component1 = components.get(0);
    Component component2 = components.get(1);
    assertThat(component1.getName()).isEqualTo("jackson-databind");
    assertThat(component2.getName()).isEqualTo("tomcat-catalina");
    assertThat(component1.getVersion()).isEqualTo("2.9.9");
    assertThat(component2.getVersion()).isEqualTo("9.0.14");
    assertThat(component1.getPurl()).isEqualTo("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar");
    assertThat(component2.getPurl()).isNull();
    assertThat(component2.getHashes()).hasSize(1);
    assertThat(component2.getHashes().get(0).getAlgorithm()).isEqualTo(Algorithm.SHA1.getSpec());
    assertThat(component2.getHashes().get(0).getValue()).isEqualTo("e6b1000b94e835ffd37f");

  }

  private void assertVulnerabilities(Component component) {
    Map<String, Extension> extensions = component.getExtensions();
    assertThat(extensions).isNotEmpty().containsKey(ExtensionType.VULNERABILITIES.getTypeName());
    Extension vulnerabilityExtension = extensions.get(ExtensionType.VULNERABILITIES.getTypeName());
    assertThat(vulnerabilityExtension.getExtensionType()).isEqualTo(ExtensionType.VULNERABILITIES);
    assertThat(vulnerabilityExtension.getNamespaceURI()).isEqualTo(Vulnerability10.NAMESPACE_URI);
    assertThat(vulnerabilityExtension.getPrefix()).isEqualTo(Vulnerability10.PREFIX);
    assertThat(vulnerabilityExtension.getExtensions()).isNotEmpty().hasSize(1);
    Consumer<Vulnerability10> vulnerabilitiesRequirement = vulnerability -> {
      assertThat(vulnerability.getId()).isEqualTo("CVE-2018-7489");
      assertThat(vulnerability.getRef()).isEqualTo("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9");
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
    Bom expectedBom = ThirdPartyUtils.parseBom(content);
    assertVulnerability(coordinateSecurity,
        (Vulnerability10) expectedBom.getComponents().get(0).getExtensions().get("vulnerabilities").getExtensions()
            .get(0), coordinateId, optionalValuesPresent);
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
        expectedVulnerabilities.add(sbomResultHandler.parseVulnerability((Vulnerability10) vulnerabilities, null));
      }
    }
    assertThat(expectedVulnerabilities)
        .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
            .withIgnoredFields("id", "fileCoordinateId", "pcStateManager", "pcDetachedState").build())
        .containsExactlyInAnyOrderElementsOf(actualVulnerabilities);
  }

  private void assertThirdPartyCoordinateLicense(
      Component component,
      String coordinateId,
      ThirdPartyCoordinateLicense coordinateLicense)
  {
    License licenseSbom = component.getLicenseChoice().getLicenses().get(0);
    assertThat(coordinateLicense.getLicenseId()).isEqualTo(licenseSbom.getId());
    assertThat(coordinateLicense.getName()).isEqualTo(licenseSbom.getName());
    assertThat(coordinateLicense.getUrl()).isEqualTo(licenseSbom.getUrl());
    assertThat(coordinateLicense.getFileCoordinateId()).isEqualTo(coordinateId);
  }

  private void assertVulnerability(
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

      Source source = vulnerability.getSource();
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

  private void assertWarnLogOutput(final String message) {
    assertThat(logOutput.getWarnMessages(loggerName)).containsOnly(message);
  }
}
