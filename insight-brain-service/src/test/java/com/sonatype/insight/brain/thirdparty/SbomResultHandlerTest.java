/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Stack;

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
import com.sonatype.insight.brain.utils.Xpp3Util;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.test.LogOutput;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.cyclonedx.BomParser;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.License;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;

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
    String sbomContent = getSbomFile("sbom-multiple-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("clair-bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    assertFilteredSbomFile(filteredContent, 2);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(2);
    assertThat(coordinates).allSatisfy(coord -> {
      assertThat(coord.getSource()).isEqualTo("clair");
    });
  }

  @Test
  public void testHandleAndFilterContents_priorityPurl_Then_Sha1_Then_Coordinates() throws Exception {
    String sbomContent = getSbomFile("sbom-component-purl-hashes-coordinates-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    Bom bom = assertFilteredSbomFile(filteredContent, 3);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting("name")
        .containsOnly("tomcat-catalina", "django", "jackson-databind");
    assertThat(components).extracting("version")
        .containsOnly("9.0.14", "1.2.3", "2.9.9");
    assertThat(components).extracting("purl")
        .containsOnly("pkg:maven/org.apache.tomcat/tomcat-catalina@9.0.14", null,
            "pkg:library/com.fasterxml.jackson.core/jackson-databind@2.9.9");
    assertThat(components).extracting("hashes.size")
        .containsOnly(null, 1, null);
    assertThat(components.get(1).getHashes())
        .flatExtracting(Hash::getValue, Hash::getAlgorithm)
        .contains("e6b1000b94e835ffd37f4c6dcbdad43f4b48a02a", "SHA-1");
  }

  @Test
  public void testHandleAndFilterContents_filterContent_hashes() throws Exception {
    String sbomContent = getSbomFile("sbom-component-hashes-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("clair-bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    Bom bom = assertFilteredSbomFile(filteredContent, 2);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting("name")
        .containsOnly("tomcat-catalina", "jackson-databind");
    assertThat(components).extracting("version")
        .containsOnly("9.0.14", "2.9.9");
    assertThat(components).extracting("type.name")
        .containsOnly("LIBRARY", "LIBRARY");
    assertThat(components).extracting("purl")
        .containsOnly(null, "pkg:library/com.fasterxml.jackson.core/jackson-databind@2.9.9");
    assertThat(components).extracting("hashes.size")
        .containsOnly(1, null);
    assertThat(components.get(0).getHashes())
        .flatExtracting(Hash::getValue, Hash::getAlgorithm)
        .contains("e6b1000b94e835ffd37f4c6dcbdad43f4b48a02a", "SHA-1");
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities() throws Exception {
    String sbomContent = getSbomFile("sbom-vulnerabilities.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
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
      assertThirdPartyCoordinateSecurity(content, thirdPartyFileCoordinate.getId(), coordinatesSecurity.get(0), true);
    }
  }

  @Test
  public void testHandleAndFilterContents_withLicense() throws Exception {
    String sbomContent = getSbomFile("sbom-license.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
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
  public void testHandleAndFilterContents_lenghtFormat() throws Exception {
    String sbomContent = getSbomFile("sbom-long-purl-format.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
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
    String sbomContent = getSbomFile("sbom-license-expression.xml");
    assertNolicense(sbomContent);
  }

  @Test
  public void testHandleAndFilterContents_no_id() throws Exception {
    String sbomContent = getSbomFile("sbom-license-no-id.xml");
    assertNolicense(sbomContent);
  }

  private void assertNolicense(String sbomContent) throws Exception {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
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
    String sbomContent = getSbomFile("sbom-vulnerabilities-optional-values.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
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
      assertThirdPartyCoordinateSecurity(content, thirdPartyFileCoordinate.getId(), coordinatesSecurity.get(0), false);
    }
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_noSeverity() throws Exception {
    String sbomContent = getSbomFile("sbom-vulnerabilities-no-severity.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
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
    String sbomContent = getSbomFile("sbom-vulnerabilities-invalid-vulnerabilities.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
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
  public void testHandleAndFilterContents_withoutVulnerabilities() throws Exception {
    String sbomContent = getSbomFile("sbom-no-vulnerabilities.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
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
    String sbomContent = getSbomFile("sbom-duplicated-vulnerabilities.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
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
    String sbomContent = getSbomFile("sbom-repeated-components.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    assertFilteredSbomFile(filteredContent, 2);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
  }

  @Test
  public void testHandleAndFilterContents_nullContent() throws Exception {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, null);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNull();
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(0);
  }

  @Test
  public void testHandleAndFilterContents_emptyContent() throws Exception {
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
    String sbomContent = getSbomFile("scan-with-invalid-sbom-data-cli.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> sbomResultHandler.handleAndFilterContents(content, thirdPartyFile))
        .withMessage("Error filtering sbom file");
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(0);
  }

  @Test
  public void testHandleAndFilterContents_sbomNestedComponents() throws Exception {
    String sbomContent = getSbomFile("scan-with-sbom-nested-component.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    assertFilteredSbomFile(filteredContent, 1);
  }

  @Test
  public void testHandleAndFilterContents_sbom_coords_no_purl() throws Exception {
    String sbomContent = getSbomFile("scan-with-sbom-coords-no-purl.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    assertFilteredSbomFile(filteredContent, 2);
  }

  @Test
  public void testHandleAndFilterContents_sbom_no_name_and_version_no_purl() throws Exception {
    String sbomContent = getSbomFile("scan-with-sbom-no-name-and-version-no-purl.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    assertFilteredSbomFile(filteredContent, 0);
  }

  @Test
  public void testHandleAndFilterContents_sbom_no_name_no_purl() throws Exception {
    String sbomContent = getSbomFile("scan-with-sbom-no-name-no-purl.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    assertFilteredSbomFile(filteredContent, 0);
  }

  @Test
  public void testHandleAndFilterContents_invalidPurl_invalidCoords() throws Exception {
    String sbomContent = getSbomFile("sbom-invalid-purl-invalid-coords.xml");
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
    String sbomContent = getSbomFile("sbom-invalid-purl-valid-coords.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    assertWarnLogOutput("Fallback to coordinates due to invalid purl: pkg:pypi/@1.2.3");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
  }

  @Test
  public void testHandleAndFilterContents_validPurl_noMandatoryValue() throws Exception {
    String sbomContent = getSbomFile("sbom-invalid-valid-purl-no-mandatory-value.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    assertWarnLogOutput("PackageUrl is not valid pkg:pypi/django");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
    assertThat(coordinates).extracting("name").containsOnly("django");
    assertThat(coordinates).extracting("version").containsOnly("1.11.1");
  }

  @Test
  public void testHandleAndFilterContents_unknownFormatPurl() throws Exception {
    String sbomContent = getSbomFile("sbom-unknow-format-purl.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();

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

  private String getSbomFile(final String fileName) throws Exception {
    URL resource = getClass().getResource("/SbomResultsHandlerTest/" + fileName);
    return new String(Files.readAllBytes(Paths.get(resource.toURI())));
  }

  private Bom assertFilteredSbomFile(String content, int expectedComponentCount) throws Exception {

    Bom bom = getBom(content);
    assertThat(bom).isNotNull();
    assertThat(bom.getComponents()).hasSize(expectedComponentCount);

    for (Component component : bom.getComponents()) {
      assertThat(component.getComponents()).isNull();
      assertThat(component.getName()).isNotNull();
      assertThat(component.getVersion()).isNotNull();
      assertThat(component.getType()).isNotNull();
    }
    return bom;
  }

  private Bom getBom(String content) throws ParseException {
    BomParser parser = new BomParser();
    return parser.parse(new StringReader(content));
  }

  private void assertThirdPartyFileCoordinate(
      Component component,
      ThirdPartyFile thirdPartyFile,
      ThirdPartyFileCoordinate coordinate)
  {
    PackageUrlIdentifier packageUrl = new PackageUrlIdentifier(component.getPurl());

    String format = ThirdPartyScanResultUtils.getValidFormat(packageUrl.getFormat());
    ComponentIdentifier ci = new ComponentIdentifier(format, packageUrl.toComponentIdentifier().getCoordinates());

    assertThat(coordinate.getFormat()).isEqualTo(ThirdPartyScanResultUtils.getValidFormat(ci.getFormat()));
    assertThat(coordinate.getHash()).isNotBlank();
    assertThat(coordinate.getName()).isEqualTo(component.getName());
    assertThat(coordinate.getThirdPartyFileId()).isEqualTo(thirdPartyFile.getId());
    assertThat(coordinate.getVersion()).isEqualTo(component.getVersion());
    assertThat(coordinate.getPackageUrl()).isEqualTo(PackageUrlIdentifier.fromComponentIdentifier(ci).getPackageUrl());
  }

  private void assertThirdPartyCoordinateSecurity(
      ThirdPartyScanContent content,
      String coordinateId,
      ThirdPartyCoordinateSecurity coordinateSecurity,
      boolean optionalValuesPresent) throws XmlPullParserException, IOException
  {
    Stack<String> elementNameStack = new Stack<>();
    XmlPullParser parser = new MXParser();
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    parser.setInput(new StringReader(content.getContent()));

    int eventType = parser.getEventType();
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        String elementName = parser.getName();
        if ("component".equals(elementName)) {
          Xpp3Dom component = Xpp3Util.loadElement("component", parser);
          Xpp3Dom vulnerabilities = component.getChild("vulnerabilities");

          assertThat(vulnerabilities).isNotNull();
          for (Xpp3Dom vulnerability : vulnerabilities.getChildren()) {
            assertVulnerability(coordinateSecurity, vulnerability, coordinateId, optionalValuesPresent);
          }
        }
        else {
          elementNameStack.push(elementName);
        }
      }
      else if (eventType == XmlPullParser.END_TAG) {
        String beginName = elementNameStack.pop();
        String endName = parser.getName();
        if (!beginName.equals(endName)) {
          throw new XmlPullParserException("End tag '" + endName + "' does not match start tag '" + beginName + "'.");
        }
      }
      eventType = parser.next();
    }
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
      Xpp3Dom vulnerability,
      String coordinateId,
      boolean optionalValuesPresent)
  {
    assertThat(coordinateSecurity).isNotNull();
    assertThat(coordinateSecurity.getFileCoordinateId()).isNotNull();
    assertThat(coordinateSecurity.getFileCoordinateId()).isEqualTo(coordinateId);
    assertThat(coordinateSecurity.getFixedBy()).isNull();

    assertThat(coordinateSecurity.getRefId()).isEqualTo(vulnerability.getChild("id").getValue());

    Xpp3Dom rating = vulnerability.getChild("ratings").getChildren()[0];
    float severytyExpected = Float.parseFloat(rating.getChild("score").getChild("base").getValue());
    assertThat(coordinateSecurity.getSeverity()).isEqualTo(severytyExpected);

    if (optionalValuesPresent) {
      assertThat(coordinateSecurity.getSeverityDescription()).isEqualTo(rating.getChild("severity").getValue());
      assertThat(coordinateSecurity.getRatingMethod()).isEqualTo(rating.getChild("method").getValue());
      assertThat(coordinateSecurity.getAttackVector()).isEqualTo(rating.getChild("vector").getValue());

      Xpp3Dom source = vulnerability.getChild("source");
      assertThat(coordinateSecurity.getVulnerabilitySource()).isEqualTo(source.getAttribute("name"));
      assertThat(coordinateSecurity.getCwes()).isNotNull();
      assertThat(coordinateSecurity.getRecommendations()).isNotNull();
      assertThat(coordinateSecurity.getAdvisories()).isNotNull();
      assertThat(coordinateSecurity.getAttackVector()).isEqualTo(rating.getChild("vector").getValue());
      assertThat(coordinateSecurity.getLink()).isEqualTo(source.getChild("url").getValue());
      assertThat(coordinateSecurity.getDescription()).isEqualTo(vulnerability.getChild("description").getValue());
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

  private void assertLogOutput(final String message) {
    assertThat(logOutput.getErrorMessages(loggerName)).containsOnly(message);
  }

  private void assertWarnLogOutput(final String message) {
    assertThat(logOutput.getWarnMessages(loggerName)).containsOnly(message);
  }
}
