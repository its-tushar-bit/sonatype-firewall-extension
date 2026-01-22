/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.SbomTaxonomy;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.sbom.SbomComponentInfoTelemetry;
import com.sonatype.insight.brain.sbom.SbomTestHelper;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.SbomProcessingException;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.file.UnsupportedSbomException;
import com.sonatype.insight.scan.model.ProjectScanItem;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.insight.util.SbomUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Extension;
import org.cyclonedx.model.Extension.ExtensionType;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.License;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Swid;
import org.cyclonedx.model.vulnerability.Rating;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method;
import org.cyclonedx.model.vulnerability.Vulnerability10;
import org.cyclonedx.model.vulnerability.Vulnerability10.ScoreSource;
import org.cyclonedx.model.vulnerability.Vulnerability10.Severity;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.parsers.XmlParser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.ATTACK_VECTOR_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.FORMAT_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.LINK_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.NAME_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.PURL_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.THIRD_PARTY_IDENTIFICATION_SOURCE_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.VERSION_MAX_LENGTH;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.VULNERABILITY_SOURCE_MAX_LENGTH;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.OTHER;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.PUBLIC_RESEARCH;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.VENDOR_RESEARCH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SbomResultHandlerTest
    extends AbstractComponentTest
{
  @Inject
  private ThirdPartyFileDAO thirdPartyFileDAO;

  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  private DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister;

  @Inject
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Inject
  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Inject
  private ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  private TelemetryUtils telemetryUtils;

  @Mock
  private TelemetrySender telemetrySender;

  @Mock
  private ThirdPartyScanContext thirdPartyScanContext;

  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  private SbomResultHandler sbomResultHandler;

  private final String loggerName = SbomResultHandler.class.getName();

  @Rule
  public LogOutput logOutput = new LogOutput(loggerName);

  @Rule
  public LogOutput logOutputUtils = new LogOutput(ThirdPartyUtils.class.getName());

  @Before
  public void before() {
    // Mockito mocks with Boolean values return false by default instead of null
    // Change it to return null since we rely on this value
    lenient().when(thirdPartyScanContext.isValid()).thenReturn(null);
    sbomResultHandler =
        new SbomResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, thirdPartyScanContext);
  }

  @Test
  public void testHandleAndFilterContents_filterContent_newThirdPartyFileMultipleEntries() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-multiple-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("clair-bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true, false);
    assertFilteredSbomFile(filteredContent, 2, false, hasHashes);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(2);
    assertThat(coordinates).allSatisfy(coord -> assertThat(coord.getSource()).isEqualTo("clair"))
        .allSatisfy(coord -> assertThat(coord.getIdentificationSources()).isEqualTo(
            SbomMetadataUtils.SBOM_IDENTIFICATION_SOURCE));
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
    List<Boolean> hasHashes = List.of(true, false);
    assertFilteredSbomFile(filteredContent, 2, false, hasHashes);
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).allSatisfy(coord -> assertThat(coord.getSource())
            .isEqualTo(StringUtils.truncate(identificationSource, THIRD_PARTY_IDENTIFICATION_SOURCE_MAX_LENGTH)))
        .allSatisfy(coord -> assertThat(coord.getIdentificationSources()).isEqualTo(
            SbomMetadataUtils.SBOM_IDENTIFICATION_SOURCE));
  }

  @Test
  public void testHandleAndFilterContents_xmlContentWithoutSBomContent() throws Exception {
    String sbomContent = getSbomXmlFile("bom-file-name-without-bom.xml");
    String identificationSource = "identification-source-very-long";
    ThirdPartyScanContent content =
        new ThirdPartyScanContent(identificationSource + "-bom.xml", null, null, null,
            sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isEqualTo(sbomContent);
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).allSatisfy(coord -> assertThat(coord.getSource())
        .isEqualTo(StringUtils.truncate(identificationSource, THIRD_PARTY_IDENTIFICATION_SOURCE_MAX_LENGTH)));
  }

  @Test
  public void testHandleAndFilterContents_priorityPurl_Then_Sha1_Then_Coordinates() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-purl-hashes-coordinates-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true, true, true, true);
    Bom bom = assertFilteredSbomFile(filteredContent, 4, false, hasHashes);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName)
        .containsOnly("tomcat-catalina", "django", "jackson-databind", "joda-time");
    assertThat(components).extracting(Component::getVersion)
        .containsOnly("9.0.14", "1.2.3", "2.9.9", "2.1.0");
    assertThat(components).extracting(Component::getPurl)
        .containsExactlyInAnyOrder(
            "pkg:maven/org.apache.tomcat/tomcat-catalina@9.0.14?type=jar",
            "pkg:generic/django@1.2.3?sbom_type=library",
            "pkg:generic/com.fasterxml.jackson.core/jackson-databind@2.9.9?sbom_type=library",
            "pkg:generic/joda-time/joda-time@2.1.0?sbom_type=library");
    assertThat(components).extracting("properties.size")
        .containsOnly(3, 3, 1, 2);
    assertThat(components.get(1).getProperties())
        .flatExtracting(Property::getValue)
        .contains("e6b1000b94e835ffd37f");
    assertThat(components.get(3).getProperties())
        .flatExtracting(Property::getValue)
        .contains("9188560f22e0b73070d2");
  }

  @Test
  public void testHandleAndFilterContents_priority_Sha1_Then_Coordinates() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-hash-coordinates-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true);
    Bom bom = assertFilteredSbomFile(filteredContent, 1, false, hasHashes);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName).containsOnly("tomcat-catalina");
    assertThat(components).extracting(Component::getVersion).containsOnly("9.0.14");
    assertThat(components).extracting(Component::getPurl)
        .containsExactly("pkg:generic/org.apache.tomcat/tomcat-catalina@9.0.14?sbom_type=library");
    assertThat(components).extracting("properties.size").containsOnly(2);
    assertThat(components.get(0).getProperties())
        .flatExtracting(Property::getValue)
        .contains("e7b1000b94e835ffd37f");
  }

  @Test
  public void testHandleAndFilterContents_Purl_Then_Cpe_Then_Coordinates_Then_Sha1() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-purl-cpe-hash-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true, true, true, true, true, true);
    Bom bom = assertFilteredSbomFile(filteredContent, 6, false, hasHashes);

    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName)
        .containsOnly("tomcat-catalina", "django", "log4j", "log4j", "jackson-databind", "joda-time");
    assertThat(components).extracting(Component::getVersion)
        .containsOnly("9.0.14", "1.2.3", "2.11.2", "2.12.2", "2.9.9", "2.1.0");

    // 4 purls were collected: 2 original purls, 2 from cpe
    assertThat(components).extracting(Component::getPurl).containsExactlyInAnyOrder(
        "pkg:maven/org.apache.tomcat/tomcat-catalina@9.0.14?type=jar",
        "pkg:generic/com.fasterxml.jackson.core/jackson-databind@2.9.9?sbom_type=library",
        "pkg:generic/apache/log4j@2.11.2?part=a&update=rc3",
        "pkg:generic/apache/log4j@2.12.2?language=en&part=a&update=rc1",
        "pkg:generic/django@1.2.3?sbom_type=library",
        "pkg:generic/joda-time/joda-time@2.1.0?sbom_type=library");
    assertThat(components).extracting("properties.size")
        .containsOnly(3, 3, 2, 2, 1, 2);
    assertThat(components.get(1).getProperties())
        .flatExtracting(Property::getValue)
        .contains("e6b1000b94e835ffd37f");
    assertThat(components.get(5).getProperties())
        .flatExtracting(Property::getValue)
        .contains("9188560f22e0b73070d2");
  }

  @Test
  public void testHandleAndFilterContents_Purl_Then_Swid_Then_Coordinates_Then_Sha1() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-purl-swid-hash-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true, true, true, true, true, true);
    Bom bom = assertFilteredSbomFile(filteredContent, 5, false, hasHashes);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName)
        .containsOnly("tomcat-catalina", "django", "Apache Log4J", "jackson-databind", "joda-time");
    assertThat(components).extracting(Component::getVersion)
        .containsOnly("9.0.14", "1.2.3", "2.11.2", "2.9.9", "2.1.0");

    // 4 purls were collected: 2 original purls, 2 from cpe
    assertThat(components).extracting(Component::getPurl).containsExactlyInAnyOrder(
        "pkg:maven/org.apache.tomcat/tomcat-catalina@9.0.14?type=jar",
        "pkg:generic/com.fasterxml.jackson.core/jackson-databind@2.9.9?sbom_type=library",
        "pkg:swid/Apache%20Log4J@2.11.2?tag_creator_name=Acme%2C%20Inc.&tag_creator_regid=example.com&" +
            "tag_id=swidgen-242eb18a-503e-ca37-393b-cf156ef09691_2.11.2",
        "pkg:generic/joda-time/joda-time@2.1.0?sbom_type=library",
        "pkg:generic/django@1.2.3?sbom_type=library");
    assertThat(components).extracting("properties.size")
        .containsOnly(3, 3, 2, 1, 2);
    assertThat(components.get(1).getProperties())
        .flatExtracting(Property::getValue)
        .contains("e6b1000b94e835ffd37f");
    assertThat(components.get(4).getProperties())
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
    List<Boolean> hasHashes = List.of(true, false);
    Bom bom = assertFilteredSbomFile(filteredContent, 2, false, hasHashes);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName)
        .containsOnly("tomcat-catalina", "jackson-databind");
    assertThat(components).extracting(Component::getVersion)
        .containsOnly("9.0.14", "2.9.9");
    assertThat(components).extracting(component -> component.getType().getTypeName())
        .containsOnly("library", "library");
    assertThat(components).extracting(Component::getPurl)
        .containsOnly("pkg:generic/org.apache.tomcat/tomcat-catalina@9.0.14?publisher=Apache&sbom_type=library",
            "pkg:generic/com.fasterxml.jackson.core/jackson-databind@2.9.9?sbom_type=library");
    assertThat(components.get(0).getHashes()).extracting(Hash::getAlgorithm)
        .containsOnly("MD5", "SHA-1", "SHA-256", "SHA-512");
    assertThat(components.get(0).getHashes()).extracting(Hash::getValue)
        .containsOnly("3942447fac867ae5cdb3229b658f4d48", "e6b1000b94e835ffd37f4c6dcbdad43f4b48a02a",
            "f498a8ff2dd007e29c2074f5e4b01a9a01775c3ff3aeaf6906ea503bc5791b7b",
            "e8f33e424f3f4ed6db76a482fde1a5298970e442c531729119e37991884bdffab4f9426" +
                "b7ee11fccd074eeda0634d71697d6f88a460dce0ac8d627a29f7d1282");
    assertThat(components).extracting("properties.size")
        .containsOnly(2, 1);
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
    ThirdPartyScanContext thirdPartyScanContext = new ThirdPartyScanContext(null, null, null, null, null);
    thirdPartyScanContext.setSbomMetadataId("someSbomMetadataId");
    thirdPartyScanContext.setIsValid(true);
    sbomResultHandler =
        new SbomResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, thirdPartyScanContext);

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(false, false, false, true, true, false, true);
    assertFilteredSbomFile(filteredContent, 7, false, hasHashes);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(5);

    List<ThirdPartyCoordinateSecurity> actualVuln = new ArrayList<>();
    for (ThirdPartyFileCoordinate coordinate : coordinates) {
      List<ThirdPartyCoordinateSecurity> coordinatesSecurity =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(coordinate.getId());
      actualVuln.addAll(coordinatesSecurity);
      assertThat(coordinatesSecurity).hasSize(1);
    }
    assertThat(actualVuln).hasSize(5);
    assertThat(
        actualVuln.stream().map(ThirdPartyCoordinateSecurity::getFileCoordinateId)).containsExactlyInAnyOrderElementsOf(
        coordinates.stream().map(ThirdPartyFileCoordinate::getId).toList());
    assertThat(actualVuln).allSatisfy(vuln -> assertThat(vuln.getRefId()).isEqualTo("CVE-2018-7489"));
  }

  @Test
  public void testHandleAndFilterContents_withSha1PresentInInput() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-purl-hashes-coordinates-components.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-component-purl-hashes-coordinates-components.xml",
        null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true, true, true, true);
    assertFilteredSbomFile(filteredContent, 4, false, hasHashes);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(4);

    ThirdPartyFileCoordinate coordinate = coordinates.get(0);
    assertThat(coordinate.getName()).isEqualTo("tomcat-catalina");
    // the saved hash matches the one in the input
    assertThat(coordinate.getHash()).isEqualTo("e7b1000b94e835ffd37f");
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilitiesAndNoPurl() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-no-purl.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-no-purl.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScanContext thirdPartyScanContext = new ThirdPartyScanContext(null, null, null, null, null);
    thirdPartyScanContext.setSbomMetadataId("someSbomMetadataId");
    thirdPartyScanContext.setIsValid(true);
    sbomResultHandler =
        new SbomResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, thirdPartyScanContext);

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
        SbomFormat.XML, true, thirdPartyScanContext.getSbomMetadataId(), VENDOR_RESEARCH.name());
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilitiesAndNoPurl_withHash() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-no-purl-with-hash.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-no-purl-with-hash.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScanContext thirdPartyScanContext = new ThirdPartyScanContext(null, null, null, null, null);
    thirdPartyScanContext.setSbomMetadataId("someSbomMetadataId");
    thirdPartyScanContext.setIsValid(true);
    sbomResultHandler =
        new SbomResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, thirdPartyScanContext);

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true);
    Bom filteredSbom = assertFilteredSbomFile(filteredContent, 1, false, hasHashes);

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
        SbomFormat.XML, true, thirdPartyScanContext.getSbomMetadataId(), VENDOR_RESEARCH.name());
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilitiesAndNoPurlNotCoordinates_withHash() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-no-purl-no-coordinates-with-hash.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-no-purl-no-coordinates-with-hash.xml", null, null, null,
            sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true);
    assertFilteredSbomFile(filteredContent, 1, true, hasHashes);
    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId())).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_componentRefValueGetsInsertedInDb() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_4.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-v1_4.xml", null, null, null,
            sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();

    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId())).isNotEmpty();
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(2);
    assertThat(coordinates).extracting(ThirdPartyFileCoordinate::getComponentRef)
        .containsExactlyInAnyOrder("621aa65ba425b9713382367e8491dc8f43f0d72d",
            "ec9df88dda81aefdf115081b9b297eebbaa142fd");
  }

  @Test
  public void testHandleAndFilterContents_withLicense() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-license.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-license.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

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
      assertThirdPartyCoordinateLicense(unfilteredSbom.getComponents().get(0).getLicenses().getLicenses().get(0),
          thirdPartyFileCoordinate.getId(),
          coordinatesLicense.get(0));
      assertThirdPartyCoordinateLicense(unfilteredSbom.getComponents().get(0).getLicenses().getLicenses().get(1),
          thirdPartyFileCoordinate.getId(),
          coordinatesLicense.get(1));
    }
  }

  @Test
  public void testHandleAndFilterContents_withLicenseExpressions() throws Exception {
    String sbom = getSbomJsonFile("license-expression-bom.json");
    ThirdPartyScanContent content = new ThirdPartyScanContent("license-expression-bom.json", null, null, null, sbom);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    try (TransactionContext tx = thirdPartyCoordinateLicenseDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate;
      List<ThirdPartyCoordinateLicense> licenses;

      thirdPartyFileCoordinate = coordinates.stream().filter(c -> c.getVersion().equals("2.9.4")).findFirst().get();
      licenses = thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(licenses).extracting(ThirdPartyCoordinateLicense::getLicenseId)
          .containsExactlyInAnyOrder("MIT", "Apache-2.0");
      assertThat(licenses).extracting(ThirdPartyCoordinateLicense::getName)
          .containsExactlyInAnyOrder("MIT", "Apache-2.0");

      thirdPartyFileCoordinate = coordinates.stream().filter(c -> c.getVersion().equals("2.9.5")).findFirst().get();
      licenses = thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(licenses).extracting(ThirdPartyCoordinateLicense::getLicenseId)
          .containsExactlyInAnyOrder("Apache-2.0");
      assertThat(licenses).extracting(ThirdPartyCoordinateLicense::getName)
          .containsExactlyInAnyOrder("Apache-2.0");

      thirdPartyFileCoordinate = coordinates.stream().filter(c -> c.getVersion().equals("2.9.6")).findFirst().get();
      licenses = thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(licenses).extracting(ThirdPartyCoordinateLicense::getLicenseId)
          .containsExactlyInAnyOrder("Apache-2.0-MIT");
      assertThat(licenses).extracting(ThirdPartyCoordinateLicense::getName)
          .containsExactlyInAnyOrder("Apache-2.0 or MIT");
    }
  }

  @Test
  public void testHandleAndFilterContents_withDuplicateLicense() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-duplicate-license.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-duplicate-license.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

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
      assertThat(coordinatesLicense).hasSize(1);
      assertThirdPartyCoordinateLicense(unfilteredSbom.getComponents().get(0).getLicenses().getLicenses().get(0),
          thirdPartyFileCoordinate.getId(),
          coordinatesLicense.get(0));
    }
  }

  @Test
  public void testHandleAndFilterContents_withComponentDuplicatedLicenseAndVulnerability() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-duplicated-component-license-vulnerability.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-duplicated-component-license-vulnerability.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

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
    when(thirdPartyScanContext.isValid()).thenReturn(true);

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
    String sbom = getSbomXmlFile("sbom-license-expression.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-license-expression.xml", null, null, null, sbom);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    try (TransactionContext tx = thirdPartyCoordinateLicenseDAO.createTransactionContext()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinate;
      List<ThirdPartyCoordinateLicense> licenses;

      thirdPartyFileCoordinate = coordinates.stream().iterator().next();
      licenses = thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tx, thirdPartyFileCoordinate.getId());
      assertThat(licenses).extracting(ThirdPartyCoordinateLicense::getName)
          .containsExactlyInAnyOrder("BSD-3-Clause", "GPL-2.0", "MIT");
    }

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes).isNotNull();
    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");
    assertThat(componentInfoTelemetry.getValidLicensesCount()).isEqualTo(1);
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
    ThirdPartyScanContext thirdPartyScanContext = new ThirdPartyScanContext(null, null, null, null, null);
    thirdPartyScanContext.setSbomMetadataId("someSbomMetadataId");
    thirdPartyScanContext.setIsValid(true);

    sbomResultHandler =
        new SbomResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, thirdPartyScanContext);

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
          SbomFormat.XML, false, thirdPartyScanContext.getSbomMetadataId(), VENDOR_RESEARCH.name());
    }
  }

  @Test
  public void testHandleAndFilterContents_NoComponentsSaved() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-components-none-saved.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-components-none-saved.xml",
        null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String actualFilteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(actualFilteredContent).isNotNull();
    Bom actualFilteredBom = SbomTestHelper.parseToCycloneDxBom(actualFilteredContent);
    assertThat(actualFilteredBom).isNotNull();
    assertThat(actualFilteredBom.getComponents()).hasSize(1);
    Component actualComponent = actualFilteredBom.getComponents().get(0);
    assertThat(actualComponent.getName()).isEqualTo("django");
    assertThat(actualComponent.getProperties()).size().isEqualTo(2);
    assertThat(actualComponent.getProperties().get(0).getName()).isEqualTo(
        SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME);
    assertThat(actualComponent.getProperties().get(0).getValue()).isEqualTo("e6b1000b94e835ffd37f");
    assertThat(actualComponent.getVersion()).isNull();
    assertThat(actualComponent.getHashes().get(0).getAlgorithm()).isEqualTo("SHA-1");
    assertThat(actualComponent.getHashes().get(0).getValue()).isEqualTo(
        "e6b1000b94e835ffd37f4c6dcbdad43f4b48a02a");
    assertThat(actualComponent.getAuthor()).isNull();
    assertThat(actualComponent.getCpe()).isNull();
    assertThat(actualComponent.getPurl()).isNull();
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(0);
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_xml_14() throws Exception {
    testHandleFilterContents(getSbomXmlFile("sbom-vulnerabilities-v1_4.xml"), "sbom-vulnerabilities-v1_4.xml",
        SbomFormat.XML, PUBLIC_RESEARCH.name());
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_xml_15() throws Exception {
    testHandleFilterContents(getSbomXmlFile("sbom-vulnerabilities-v1_5.xml"), "sbom-vulnerabilities-v1_5.xml",
        SbomFormat.XML, VENDOR_RESEARCH.name());
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_xml_15_withCVSSv4() throws Exception {
    testHandleFilterContents(getSbomXmlFile("sbom-vulnerabilities-v1_5_cvssv4.xml"),
        "sbom-vulnerabilities-v1_5_cvssv4.xml", SbomFormat.XML, VENDOR_RESEARCH.name());
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_xml_14_withCVSSv4_shouldFail() throws Exception {
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> testHandleFilterContents(getSbomXmlFile("sbom-vulnerabilities-v1_4_cvssv4.xml"),
            "sbom-vulnerabilities-v1_4_cvssv4.xml", SbomFormat.XML, PUBLIC_RESEARCH.name()))
        .withStackTraceContaining("cvc-enumeration-valid: Value 'CVSSv4' is not facet-valid with respect " +
            "to enumeration '[CVSSv2, CVSSv3, CVSSv31, OWASP, other]'. It must be a value from the enumeration");
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilitiesRatings_json_14() throws Exception {
    testHandleFilterContents(getSbomJsonFile("sbom-vulnerabilities-ratings-v1-4.json"),
        "sbom-vulnerabilities-ratings-v1-4.json", SbomFormat.JSON, PUBLIC_RESEARCH.name());
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_json_14() throws Exception {
    testHandleFilterContents(getSbomJsonFile("sbom-vulnerabilities-v1-4.json"), "sbom-vulnerabilities-v1-4.json",
        SbomFormat.JSON, PUBLIC_RESEARCH.name());
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_json_15() throws Exception {
    testHandleFilterContents(getSbomJsonFile("sbom-vulnerabilities-v1-5.json"), "sbom-vulnerabilities-v1-5.json",
        SbomFormat.JSON, VENDOR_RESEARCH.name());
  }

  @Test
  public void testHandleAndFilterContents_withExtensionVulnerabilities_xml_14() throws Exception {
    assertVulnerabilityInformation("sbom-ext-vulnerabilities-v1_4.xml");
  }

  @Test
  public void testHandleAndFilterContents_withVulnerabilities_noSeverity() throws Exception {
    assertVulnerabilityInformation("sbom-vulnerabilities-no-severity.xml");
  }

  @Test
  public void testHandleAndFilterContents_withComponentProperties() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-properties.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-component-properties.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
    assertThat(thirdPartyFileCoordinate.getMatchStateId()).isEqualTo("exact");
    assertThat(thirdPartyFileCoordinate.getFilenamesList()).containsExactlyInAnyOrder("f1", "f2", "f3");
    assertThat(thirdPartyFileCoordinate.getOccurrencesList()).containsExactlyInAnyOrder(
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/js-lib/jquery-ui-1.10.3/jquery-1.9.1.js",
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/js-lib/jquery-ui-1.10.3/tests/jquery-1.9.1.js",
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/js-lib/jquery/jquery-migrate-1.2.1.js",
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/rest/js/vendor/jquery-1.9.1.min.js"
    );
  }

  @Test
  public void testHandleAndFilterContents_withComponentPropertiesLegacyMatchState() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-properties-legacy-match-state.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-component-properties.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
    assertThat(thirdPartyFileCoordinate.getMatchStateId()).isEqualTo("exact");
    assertThat(thirdPartyFileCoordinate.getFilenamesList()).containsExactlyInAnyOrder("f1", "f2", "f3");
    assertThat(thirdPartyFileCoordinate.getOccurrencesList()).containsExactlyInAnyOrder(
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/js-lib/jquery-ui-1.10.3/jquery-1.9.1.js",
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/js-lib/jquery-ui-1.10.3/tests/jquery-1.9.1.js",
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/js-lib/jquery/jquery-migrate-1.2.1.js",
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/rest/js/vendor/jquery-1.9.1.min.js"
    );
  }

  @Test
  public void testHandleAndFilterContents_withNoComponentProperties() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-no-properties.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-component-no-properties.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
    assertThat(thirdPartyFileCoordinate.getMatchStateId()).isNull();
    assertThat(thirdPartyFileCoordinate.getFilenamesList()).isEmpty();
    assertThat(thirdPartyFileCoordinate.getOccurrencesList()).containsExactlyInAnyOrder(
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/js-lib/jquery-ui-1.10.3/jquery-1.9.1.js",
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/js-lib/jquery-ui-1.10.3/tests/jquery-1.9.1.js",
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/js-lib/jquery/jquery-migrate-1.2.1.js",
        "OWF-bundle-7.17.1.zip/apache-tomcat/webapps/owf.war/rest/js/vendor/jquery-1.9.1.min.js");
  }

  private void testHandleFilterContents(String sbomContent, String path, SbomFormat sbomFormat, String researchType)
      throws Exception
  {
    ThirdPartyScanContent content = new ThirdPartyScanContent(path, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScanContext thirdPartyScanContext = new ThirdPartyScanContext(null, null, null, null, null);
    thirdPartyScanContext.setSbomMetadataId("someSbomMetadataId");
    thirdPartyScanContext.setIsValid(true);
    sbomResultHandler =
        new SbomResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, thirdPartyScanContext);

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
          sbomFormat, true, false, thirdPartyScanContext.getSbomMetadataId(), researchType);
    }
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
    when(thirdPartyScanContext.isValid()).thenReturn(true);

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
  public void testHandleAndFilterContents_CpeAndSwid_Xml() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-with-cpe-swid.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-with-cpe-swid.xml", null, null, null, sbomContent);
    assertCpeAndSwid(content);
  }

  @Test
  public void testHandleAndFilterContents_CpeAndSwid_Json() throws Exception {
    String sbomContent = getSbomJsonFile("sbom-with-cpe-swid.json");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-with-cpe-swid.json", null, null, null, sbomContent);
    assertCpeAndSwid(content);
  }

  private void assertCpeAndSwid(ThirdPartyScanContent content) throws Exception {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = assertFilteredSbomFile(filteredContent, 1);
    Component component = bom.getComponents().get(0);
    assertThat(component.getCpe()).isEqualTo("cpe:/a:acme:application:9.1.1");
    Swid swid = component.getSwid();
    assertThat(swid.getTagId()).isEqualTo("swidgen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1");
    assertThat(swid.getName()).isEqualTo("Acme Application");
    assertThat(swid.getVersion()).isEqualTo("9.1.1");
    assertThat(swid.getTagVersion()).isNull();
    assertThat(swid.isPatch()).isNull();
    assertThat(swid.getAttachmentText().getEncoding()).isEqualTo("base64");
    assertThat(swid.getAttachmentText().getContentType()).isEqualTo("text/xml");
    assertThat(swid.getAttachmentText().getText()).isEqualTo("PD94bWwgdmVyc");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
    assertThat(thirdPartyFileCoordinate.getCpe()).isEqualTo("cpe:/a:acme:application:9.1.1");
    assertThat(thirdPartyFileCoordinate.getSwid())
        .isEqualTo("{" +
            "\"tagId\":\"swidgen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1\"," +
            "\"name\":\"Acme Application\"," +
            "\"version\":\"9.1.1\"," +
            "\"text\":{\"contentType\":\"text/xml\",\"encoding\":\"base64\",\"content\":\"PD94bWwgdmVyc\"}}");
  }

  @Test
  public void testHandleAndFilterContents_duplicatedVulnerabilities() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-duplicated-vulnerabilities.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-duplicated-vulnerabilities.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

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
    testHandleAndFilterContents_invalidSbom_isValidTrue(getSbomXmlFile("scan-with-invalid-sbom-data-cli.xml"),
        "scan-with-invalid-sbom-data-cli.xml");
  }

  @Test
  public void testHandleAndFilterContents_invalidSbom_content() throws Exception {
    testHandleAndFilterContents_invalidSbom_isValidTrue(getSbomXmlFile("scan-with-invalid-license-id.xml"),
        "scan-with-invalid-sbom-data-cli.xml");
    assertThat(logOutputUtils.getDebugMessages(ThirdPartyUtils.class.getName()))
        .contains("The sbom is not valid. There were 2 errors.");
  }

  @Test
  public void testHandleAndFilterContents_invalidSbom_Xml_v1_2() throws Exception {
    testHandleAndFilterContents_invalidSbom_isValidTrue(getSbomXmlFile("scan-with-invalid-sbom-v1_2.xml"),
        "scan-with-invalid-sbom-v1_2.xml");
  }

  @Test
  public void testHandleAndFilterContents_invalidSbom_Json() throws Exception {
    testHandleAndFilterContents_invalidSbom_isValidTrue(getSbomJsonFile("sbom-invalid.json"), "sbom-invalid.json");
  }

  private void testHandleAndFilterContents_invalidSbom_isValidTrue(String sbomContent, String path) {
    ThirdPartyScanContent content = new ThirdPartyScanContent(path, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

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
    List<Boolean> hasHashes = List.of(true);
    assertFilteredSbomFile(filteredContent, 1, false, hasHashes);
  }

  @Test
  public void testHandleAndFilterContents_v1_2() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_2.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_2.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    FilteredThirdPartyContent filteredContent =
        sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    List<Boolean> hasHashes = List.of(true, false);
    Bom bom = assertFilteredSbomFile(sbomXml, 2, false, hasHashes);
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/Acme%20Application@9.1.1");
  }

  @Test
  public void testHandleAndFilterContents_v1_3() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_3.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_3.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true, false);
    Bom bom = assertFilteredSbomFile(filteredContent, 2, false, hasHashes);
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/Acme%20Application@9.1.1");
  }

  @Test
  public void testHandleAndFilterContents_v1_4() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_4.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_4.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true, false);
    Bom bom = assertFilteredSbomFile(filteredContent, 2, false, hasHashes);
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/Acme/Acme%20Application@9.1.1");
  }

  @Test
  public void testHandleAndFilterContents_v1_4_containerType_noPurl() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_4-container-nopurl.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-v1_4-container-nopurl.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();

    assertThat(filteredContent).isNotNull();
    Bom bom = SbomTestHelper.parseToCycloneDxBom(filteredContent);
    assertThat(bom).isNotNull();

    List<Component> components = bom.getComponents();
    assertThat(components).hasSize(2);
    assertThat(components).extracting(Component::getType).containsExactlyInAnyOrder(Type.LIBRARY, Type.CONTAINER);
    assertThat(components).extracting(Component::getPurl).containsExactlyInAnyOrder(
        "pkg:generic/com.google.guava/guava@30.1-jre?sbom_type=library",
        "pkg:generic/annotation-api@1.1.6?sbom_type=container"
    );
  }

  @Test
  public void testHandleAndFilterContents_withSha256() throws Exception {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    String sbomContent = getSbomXmlFile("sbom-simple-v1_4.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_4.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true);
    assertFilteredSbomFile(filteredContent, 1, false, hasHashes);
  }

  @Test
  public void testHandleAndFilterContents_v1_4_json() throws Exception {
    String sbomContent = getSbomJsonFile("sbom-simple-v1-4.json");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_4.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true);
    assertFilteredSbomFile(filteredContent, 1, false, hasHashes);
  }

  @Test
  public void testHandleAndFilterContents_v1_5_json() throws Exception {
    String sbomContent = getSbomJsonFile("sbom-simple-v1-5.json");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_5.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true);
    assertFilteredSbomFile(filteredContent, 1, false, hasHashes);
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
  public void testProcessDependencyGraph_MavenWithoutExtension() {
    Bom sourceBom = new Bom();
    Bom targetBom = new Bom();
    targetBom.addComponent(new Component());
    Metadata metadata = new Metadata();
    Component rootComponent = new Component();
    rootComponent.setName("root");
    rootComponent.setBomRef("root");
    rootComponent.setVersion("1.0");
    rootComponent.setPurl("pkg:maven/test/root@1.0");
    metadata.setComponent(rootComponent);
    targetBom.setMetadata(metadata);
    Dependency root = createDependencyList("root", "pkg:maven/test/direct1@1.0", "pkg:maven/test/direct2@2.0");
    Dependency d1 = createDependencyList("pkg:maven/test/direct1@1.0", "pkg:maven/test/d1t1@1.1");
    Dependency d2 = createDependencyList("pkg:maven/test/direct2@2.0", "pkg:maven/test/d2t1@1.1");
    Dependency d1t1 = d1.getDependencies().get(0);
    Dependency d2t1 = d2.getDependencies().get(0);
    sourceBom.setDependencies(Arrays.asList(root, d1, d2, d1t1, d2t1));

    List<ProjectScanItem> result = new ArrayList<>();

    sbomResultHandler.processDependencyGraph(sourceBom, targetBom, result,
        new ThirdPartyFile("test-bom.xml", new Date()));

    assertThat(result).hasSize(1).allSatisfy(projectItem -> {
      assertThat(projectItem.getKind()).isEqualTo("sbom");
      assertThat(projectItem.getId()).isEqualTo("pkg:maven/test/root@1.0?type=jar");
      assertThat(projectItem.getPath()).isEqualTo("test-bom.xml");
      List<com.sonatype.insight.scan.model.Dependency> rootDependencies = projectItem.getDependencies();
      assertThat(rootDependencies).hasSize(4)
          .extracting(com.sonatype.insight.scan.model.Dependency::getId)
          .containsExactlyInAnyOrder(
              "pkg:maven/test/direct1@1.0?type=jar",
              "pkg:maven/test/direct2@2.0?type=jar",
              "pkg:maven/test/d1t1@1.1?type=jar",
              "pkg:maven/test/d2t1@1.1?type=jar");
      assertParentAndChildDependency(rootDependencies,
          "pkg:maven/test/direct1@1.0?type=jar",
          "pkg:maven/test/d1t1@1.1?type=jar");
      assertParentAndChildDependency(rootDependencies,
          "pkg:maven/test/direct2@2.0?type=jar",
          "pkg:maven/test/d2t1@1.1?type=jar");
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
    rootComponent.setPurl("pkg:nuget/NugetProject@0.0.0");
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
      assertThat(projectItem.getId()).isEqualTo("pkg:nuget/NugetProject@0.0.0");
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
      assertThat(rootDependencies.stream().filter(com.sonatype.insight.scan.model.Dependency::isDirect)).hasSize(1);
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
      assertParentAndChildDependency(rootDependencies, "pkg:npm/direct1@1.0", "pkg:npm/d1t1@1.1");
      assertParentAndChildDependency(rootDependencies, "pkg:npm/direct2@2.0", "pkg:npm/d2t1@1.1");
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
    Dependency root = createDependencyList("root", "pkg:npm/direct1@1.0", "pkg:npm/direct2@2.0");
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

  @Test
  public void testProcessDependencyGraph_componentsWithNoPurls() {
    //given
    Bom sourceBom = new Bom();

    Component c1 = new Component();
    c1.setBomRef("dd2");
    c1.setPurl("pkg:npm/direct1@1.0");

    Component c2 = new Component();
    c2.setBomRef("dd1");
    c2.setPurl("pkg:npm/direct2@2.0");

    Component c3 = new Component();
    c3.setBomRef("td1");

    Bom targetBom = new Bom();
    targetBom.addComponent(c1);
    targetBom.addComponent(c2);
    targetBom.addComponent(c3);
    Metadata metadata = new Metadata();
    Component rootComponent = new Component();
    rootComponent.setName("root");
    rootComponent.setBomRef("root");
    rootComponent.setVersion("1.0");
    rootComponent.setPurl("pkg:npm/root@1.0");
    metadata.setComponent(rootComponent);
    targetBom.setMetadata(metadata);
    Dependency root = createDependencyList("root", "dd1", "dd2");
    Dependency d1 = root.getDependencies().get(0);
    Dependency d2 = createDependencyList("dd2", "td1");
    Dependency d2t1 = d2.getDependencies().get(0);
    sourceBom.setDependencies(Arrays.asList(root, d1, d2, d2t1));
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
      assertThat(resultDependencies)
          .hasSize(2)
          .extracting(com.sonatype.insight.scan.model.Dependency::getId)
          .containsExactlyInAnyOrder("pkg:npm/direct2@2.0", "pkg:npm/direct1@1.0");
    });
    assertIdentityMetadata(targetBom, metadata);
  }

  @Test
  public void testProcessDependencyGraph_dependenciesWithNoComponents() {
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
    Dependency root = createDependencyList("root", "dd1", "dd2");
    Dependency d2 = createDependencyList("dd2", "td1");
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
      assertThat(resultDependencies).isEmpty();
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
    List<Boolean> hasHashes = List.of(true, false);
    assertFilteredSbomFile(filteredContent, 2, false, hasHashes);
  }

  @Test
  public void testHandleAndFilterContents_sbom_no_name_and_version_no_purl() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-no-name-and-version-no-purl.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("scan-with-sbom-no-name-and-version-no-purl.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true);
    assertFilteredSbomFile(filteredContent, 1, true, hasHashes);
  }

  @Test
  public void testHandleAndFilterContents_sbom_no_name_no_purl() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-no-name-no-purl.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("scan-with-sbom-no-name-no-purl.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true, false);
    assertFilteredSbomFile(filteredContent, 2, true, hasHashes);
  }

  @Test
  public void testHandleAndFilterContents_invalidPurl_invalidCoords() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-invalid-purl-invalid-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-invalid-purl-invalid-coords.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isNotNull();
    assertDebugLogOutput("Invalid purl: pkg:pypi/@1.2.3");

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
    assertDebugLogOutput("Invalid purl: pkg:pypi/@1.2.3");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
  }

  @Test
  public void testHandleAndFilterContents_cyclonedx_11_vulnerabilities() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-v1_1.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-v1_1.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
    assertThat(thirdPartyFileCoordinate.getIdentificationSources()).isEqualTo(IdentificationSource.SBOM.getId());
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
  public void testDetermineSource() {
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("abcd-bom.xml")).isEqualTo("abcd");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("ABCD123-BOM.XmL")).isEqualTo("ABCD123");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("sub/dir/abcd-bom.xml")).isEqualTo("abcd");

    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("ABCD-SBOM.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("abcdbom.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("bom.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("BOM.XML")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("-bom.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("sub/dir/bom.xml")).isEqualTo("Third-Party");

    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("nvd-build-image.cdx.xml"))
        .isEqualTo("nvd-build-image");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("NVD-BUILD-IMAGE12.CDX.xmL"))
        .isEqualTo("NVD-BUILD-IMAGE12");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource(
        "sub/dir/nvd-build-image.cdx.xml")).isEqualTo("nvd-build-image");

    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("ABCD-SBOM-CDX.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("abcdcdx.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("cdx.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("CDX.XML")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource(".cdx.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("sub/dir/cdx.xml")).isEqualTo("Third-Party");

    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("nvd-build-image.spdx.xml"))
        .isEqualTo("nvd-build-image");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("NVD-BUILD-IMAGE12.SPDX.xmL"))
        .isEqualTo("NVD-BUILD-IMAGE12");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource(
        "sub/dir/nvd-build-image.spdx.xml")).isEqualTo("nvd-build-image");

    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("ABCD-SBOM-SPDX.xml"))
        .isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("abcdspdx.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("spdx.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("SPDX.XML")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource(".spdx.xml")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("sub/dir/spdx.xml")).isEqualTo("Third-Party");

    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource("")).isEqualTo("Third-Party");
    assertThat(sbomResultHandler.determineThirdPartyIdentificationSource(null)).isEqualTo("Third-Party");
  }

  @Test
  public void testHandleAndFilterContents_truncate() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-truncate-coordinates-for-hds.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-truncate-coordinates-for-hds.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);

    // check filtered content (will be sent to HDS) has been truncated
    Bom filteredSbom = SbomTestHelper.parseToCycloneDxBom(filteredContent);
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
    assertThat(coordinateSecurity.getRefId()).isEqualTo("CVE-2018-7489CVE-2018-7489");
    assertThat(coordinateSecurity.getVulnerabilitySource()).hasSize(VULNERABILITY_SOURCE_MAX_LENGTH);
    assertThat(coordinateSecurity.getAttackVector()).hasSizeLessThanOrEqualTo(ATTACK_VECTOR_MAX_LENGTH);
    assertThat(coordinateSecurity.getRefId()).hasSize("CVE-2018-7489CVE-2018-7489".length());
  }

  @Test
  public void testHandleAndFilterContents_duplicateComponentsAndVulnerabilityAware() throws Exception {
    String fileName = "sbom-duplicate-aware-bom.xml";
    String sbomContent = getSbomXmlFile(fileName);
    ThirdPartyScanContent content =
        new ThirdPartyScanContent(fileName, null, null, null, sbomContent);
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata metadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, fileName);
    ThirdPartyFileCoordinate cp =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "source", "pypi", "numpy", "1.19.0",
            "c68bdc4f6f12b754bf3e", "pkg:pypi/numpy@1.19.0?extension=zip", "c68bdc4f6f12b754bf3e6ccdb8ab284c6a13c021");
    //duplicate vulnerability, that exists in the database and also in the ingested sbom
    ThirdPartyCoordinateSecurity coordSecurity =
        tempEntity.newThirdPartyCoordinateSecurity(cp, "CVE-22024-123456", metadata.getId(), "desc", "link", 5.0d,
            "sevDesc", "1.0.1");
    when(thirdPartyScanContext.isValid()).thenReturn(true);
    when(thirdPartyScanContext.getSbomMetadataId()).thenReturn(metadata.getId());

    //before - no existing vex
    assertThat(thirdPartyVexDAO.getByCoordinateSecurityIdAndRefId(coordSecurity.getId(), "CVE-22024-123456")).isNull();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);
    //after - vex merged from sbom
    ThirdPartyVulnerabilityExploitabilityExchange savedVex =
        thirdPartyVexDAO.getByCoordinateSecurityIdAndRefId(coordSecurity.getId(), "CVE-22024-123456");
    assertThat(savedVex).isNotNull();
    assertThat(savedVex.getState()).isEqualTo("resolved");
    assertThat(savedVex.getJustification()).isEqualTo("code_not_reachable");
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
    return assertFilteredSbomFile(content, expectedComponentCount, false, null);
  }

  private Bom assertFilteredSbomFile(
      final String content,
      final int expectedComponentCount,
      final boolean optional,
      final List<Boolean> hasHashes)
      throws Exception
  {
    assertThat(content).isNotNull();
    Bom bom = SbomTestHelper.parseToCycloneDxBom(content);
    assertThat(bom).isNotNull();
    assertThat(bom.getComponents()).hasSize(expectedComponentCount);

    int componentIndex = 0;
    for (Component component : bom.getComponents()) {
      assertThat(component.getComponents()).isNull();
      if (!optional) {
        assertThat(component.getName()).isNotNull();
        assertThat(component.getVersion()).isNotNull();
      }
      assertThat(component.getType()).isNotNull();

      assertThat(component.getLicenses()).isNull();
      assertThat(component.getAuthor()).isNull();
      assertThat(component.getCopyright()).isNull();
      assertThat(component.getEvidence()).isNull();
      assertThat(component.getPedigree()).isNull();

      if (hasHashes != null && hasHashes.get(componentIndex)) {
        assertThat(component.getHashes()).isNotNull();
      }
      else {
        assertThat(component.getHashes()).isNull();
      }
      assertThat(component.getExternalReferences()).isNull();
      assertThat(component.getExtensibleTypes()).isNull();
      assertThat(component.getExtensions()).isNull();

      assertThat(component.getProperties().stream()
          .filter(p -> p.getName().equals(SbomCycloneDxUtils.PROPERTY_COMPONENT_REF)).findFirst())
          .isNotEmpty()
          .satisfies(opt -> assertThat(opt.get().getValue()).isNotBlank());
      componentIndex += 1;
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
    Bom bomV11 = parseBom(contentV11, true);
    ThirdPartyScanContent contentV12 =
        new ThirdPartyScanContent("sbom-simple-v1-2.xml", null, null, null, getSbomXmlFile("sbom-simple-v1-2.xml"));
    Bom bomV12 = parseBom(contentV12, true);
    ThirdPartyScanContent contentV13 =
        new ThirdPartyScanContent("sbom-simple-v1_3.xml", null, null, null, getSbomXmlFile("sbom-simple-v1_3.xml"));
    Bom bomV13 = parseBom(contentV13, true);
    ThirdPartyScanContent contentV14 =
        new ThirdPartyScanContent("sbom-simple-v1_4.xml", null, null, null, getSbomXmlFile("sbom-simple-v1_4.xml"));
    Bom bomV14 = parseBom(contentV14, true);
    ThirdPartyScanContent contentV14json =
        new ThirdPartyScanContent("sbom-simple-v1-4.json", null, null, null, getSbomJsonFile("sbom-simple-v1-4.json"));
    Bom bomV14json = parseBom(contentV14json, true);
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
    assertThatExceptionOfType(UnsupportedSbomException.class)
        .isThrownBy(() -> sbomResultHandler.parseBom(contentJson))
        .withMessage("CycloneDX JSON 1.2 version is not supported");
  }

  @Test
  public void testParseFilesV11AndV12WithLicenses() throws Exception {
    ThirdPartyScanContent contentV11 =
        new ThirdPartyScanContent("sbom-licenses-v1-1.xml", null, null, null, getSbomXmlFile("sbom-licenses-v1-1.xml"));
    Bom bomV11 = parseBom(contentV11, true);
    ThirdPartyScanContent contentV12 =
        new ThirdPartyScanContent("sbom-licenses-v1-2.xml", null, null, null, getSbomXmlFile("sbom-licenses-v1-2.xml"));
    Bom bomV12 = parseBom(contentV12, true);
    assertThat(bomV11).isNotNull();
    assertThat(bomV12).isNotNull();
    assertThat(bomV11.getComponents()).hasSameElementsAs(bomV12.getComponents());
  }

  @Test
  public void testParseXmlFilesV11AndV12andV13WithVulnerabilities() throws Exception {
    ThirdPartyScanContent contentV11 = new ThirdPartyScanContent("sbom-vulnerabilities-v1_1.xml", null, null, null,
        getSbomXmlFile("sbom-vulnerabilities-v1_1.xml"));
    Bom bomV11 = parseBom(contentV11, true);
    ThirdPartyScanContent contentV12 = new ThirdPartyScanContent("sbom-vulnerabilities-v1_2.xml", null, null, null,
        getSbomXmlFile("sbom-vulnerabilities-v1_2.xml"));
    Bom bomV12 = parseBom(contentV12, true);
    ThirdPartyScanContent contentV13 = new ThirdPartyScanContent("sbom-vulnerabilities-v1_3.xml", null, null, null,
        getSbomXmlFile("sbom-vulnerabilities-v1_3.xml"));
    Bom bomV13 = parseBom(contentV13, true);
    assertThat(bomV11.getComponents()).isNotEmpty().allSatisfy(this::assertExtensionVulnerabilities);
    assertThat(bomV12.getComponents()).isNotEmpty().allSatisfy(this::assertExtensionVulnerabilities);
    assertThat(bomV13.getComponents()).isNotEmpty().allSatisfy(this::assertExtensionVulnerabilities);
  }

  @Test
  public void testParseXmlFilesV14WithVulnerabilities() throws Exception {
    ThirdPartyScanContent contentV14 = new ThirdPartyScanContent("sbom-vulnerabilities-v1_4.xml", null, null, null,
        getSbomXmlFile("sbom-vulnerabilities-v1_4.xml"));
    Bom bomV14 = parseBom(contentV14, true);
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
    Bom filteredSbom = SbomTestHelper.parseToCycloneDxBom(filteredContent);
    List<Boolean> hasHashes = List.of(false, true, false);
    assertFilteredSbomFile(filteredContent, 3, false, hasHashes);
    List<Component> components = filteredSbom.getComponents();
    assertThat(components).hasSize(3)
        .allSatisfy(component -> {
          assertThat(component.getExtensions()).isNull();
          assertThat(component.getLicenses()).isNull();
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
    assertThat(component3.getPurl()).isEqualTo("pkg:generic/org.example/sample-library@1.0.0?sbom_type=library");
    assertThat(component1.getHashes()).isNull();

    assertThat(component2.getHashes().get(0).getAlgorithm()).isEqualTo("MD5");
    assertThat(component2.getHashes().get(0).getValue()).isEqualTo(
        "3942447fac867ae5cdb3229b658f4d48");
    assertThat(component2.getHashes().get(1).getAlgorithm()).isEqualTo("SHA-1");
    assertThat(component2.getHashes().get(1).getValue()).isEqualTo(
        "e6b1000b94e835ffd37f4c6dcbdad43f4b48a02a");
    assertThat(component2.getHashes().get(2).getAlgorithm()).isEqualTo("SHA-256");
    assertThat(component2.getHashes().get(2).getValue()).isEqualTo(
        "f498a8ff2dd007e29c2074f5e4b01a9a01775c3ff3aeaf6906ea503bc5791b7b");
    assertThat(component2.getHashes().get(3).getAlgorithm()).isEqualTo("SHA-512");
    assertThat(component2.getHashes().get(3).getValue()).isEqualTo(
        "e8f33e424f3f4ed6db76a482fde1a5298970e442c531729119e3799" +
            "1884bdffab4f9426b7ee11fccd074eeda0634d71697d6f88a460dce0ac8d627a29f7d1282");

    assertThat(component3.getHashes()).isNull();
    assertThat(component1.getProperties()).hasSize(2);
    assertThat(component1.getProperties().get(0).getName()).isEqualTo(SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME);
    assertThat(component1.getProperties().get(0).getValue()).isEqualTo(
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar");
    assertThat(component2.getProperties()).hasSize(2);
    assertThat(component2.getProperties().get(0).getName()).isEqualTo(SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME);
    assertThat(component2.getProperties().get(0).getValue()).isEqualTo("e6b1000b94e835ffd37f");
    assertThat(component3.getProperties()).hasSize(2);
    assertThat(component3.getProperties().get(0).getName()).isEqualTo(SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME);
    assertThat(component3.getProperties().get(0).getValue()).isEqualTo("716e4909ac2db42da409");
  }

  @Test
  public void testHandleAndFilterContents_invalidPurl_missingCoords() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-invalid-purl-missing-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-invalid-purl-missing-coords.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 2);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(2)
        .extracting(ThirdPartyFileCoordinate::getPackageUrl)
        .containsExactlyInAnyOrder("pkg:pypi/django@1.2.3",
            "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar");
  }

  @Test
  public void testHandleAndFilterContents_RootDependencyNotFirst() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-root-dependency-not-first.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-root-dependency-not-first.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

    FilteredThirdPartyContent filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    Bom bom = assertFilteredSbomFile(filteredContent.getContent(), 2);
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/Acme/Acme%20Application@9.1.1");
    List<ProjectScanItem> moduleDependencies = filteredContent.getModuleDependencies();
    assertThat(moduleDependencies).hasSize(1).allSatisfy(projectItem -> {
      assertThat(projectItem.getKind()).isEqualTo("sbom");
      assertThat(projectItem.getId()).isEqualTo("pkg:generic/Acme/Acme%20Application@9.1.1");
      assertThat(projectItem.getPath()).isEqualTo("third-party-file");
      List<com.sonatype.insight.scan.model.Dependency> rootDependencies = projectItem.getDependencies();
      assertThat(rootDependencies).hasSize(3)
          .extracting(com.sonatype.insight.scan.model.Dependency::getId)
          .containsExactlyInAnyOrder(
              "pkg:maven/org.acme/persistence@3.1.0?type=jar",
              "pkg:maven/org.acme/web-framework@1.0.0?type=jar",
              "pkg:maven/org.acme/common-util@3.0.0?type=jar");
      assertParentAndChildDependency(rootDependencies, "pkg:maven/org.acme/persistence@3.1.0?type=jar",
          "pkg:maven/org.acme/common-util@3.0.0?type=jar");
      assertParentAndChildDependency(rootDependencies, "pkg:maven/org.acme/web-framework@1.0.0?type=jar",
          "pkg:maven/org.acme/common-util@3.0.0?type=jar");
      assertParentAndChildDependency(rootDependencies, "pkg:maven/org.acme/common-util@3.0.0?type=jar", null);
    });
  }

  @Test
  public void testParseVulnerabilityExploitability() throws Exception {
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-v1_4-vex-data.xml", null, null, null,
            getSbomXmlFile("sbom-vulnerabilities-v1_4-vex-data.xml"));
    Bom bom = parseBom(content, true);
    assertThat(bom.getVulnerabilities()).isNotEmpty().allSatisfy(vulnerability -> {
      ThirdPartyVulnerabilityExploitabilityExchange vex =
          sbomResultHandler.parseVulnerabilityExploitability(vulnerability, "anyId");
      assertThat(vex).isNotNull();
      assertThat(vex.getResponse().split(",")).hasSize(2).contains("will_not_fix", "update");
      assertThat(vex.getJustification()).isEqualTo("code_not_reachable");
    });
  }

  @Test
  public void testParseVulnerabilityExploitabilityPartialData() throws Exception {
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-v1_4-vex-partial-data.xml", null, null, null,
            getSbomXmlFile("sbom-vulnerabilities-v1_4-vex-partial-data.xml"));
    Bom bom = parseBom(content, true);
    assertThat(bom.getVulnerabilities()).isNotEmpty().allSatisfy(vulnerability -> {
      ThirdPartyVulnerabilityExploitabilityExchange vex =
          sbomResultHandler.parseVulnerabilityExploitability(vulnerability, "anyId");
      assertThat(vex).isNotNull();
      assertThat(vex.getDetail()).isEqualTo("Some analysis details");
      assertThat(vex.getState()).isEqualTo("resolved");
      assertThat(vex.getResponse()).isEmpty();
      assertThat(vex.getJustification()).isNull();
    });
  }

  @Test
  public void testParseVulnerabilityExploitabilityNullVexData() throws Exception {
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-vulnerabilities-v1_4.xml", null, null, null,
            getSbomXmlFile("sbom-vulnerabilities-v1_4.xml"));
    Bom bom = parseBom(content, true);
    assertThat(bom.getVulnerabilities()).isNotEmpty().allSatisfy(vulnerability -> {
      ThirdPartyVulnerabilityExploitabilityExchange vex =
          sbomResultHandler.parseVulnerabilityExploitability(vulnerability, "anyId");
      assertThat(vex).isNull();
    });
  }

  @Test
  public void testComponentInfoTelemetry_PurlCpeCoordinateCounts() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-purl-cpe-hash-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true, true, true, true, true, true);
    assertFilteredSbomFile(filteredContent, 6, false, hasHashes);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");

    assertThat(componentInfoTelemetry.getContentType()).isEqualTo("XML");
    assertThat(componentInfoTelemetry.getSpec()).isEqualTo("CYCLONEDX");
    assertThat(componentInfoTelemetry.getSpecVersion()).isEqualTo("1.1");
    assertThat(componentInfoTelemetry.getPurlCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getCpeCount()).isEqualTo(2);
    assertThat(componentInfoTelemetry.getCoordinateCount()).isEqualTo(3);
    assertThat(componentInfoTelemetry.getEcosystemCount()).contains(entry("generic", 5),
        entry("maven", 1));
  }

  @Test
  public void testComponentInfoTelemetry_HashCount() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-no-name-and-version-no-purl.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");

    assertThat(componentInfoTelemetry.getContentType()).isEqualTo("XML");
    assertThat(componentInfoTelemetry.getSpec()).isEqualTo("CYCLONEDX");
    assertThat(componentInfoTelemetry.getSpecVersion()).isEqualTo("1.1");
    assertThat(componentInfoTelemetry.getHashCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getEcosystemCount()).isEmpty();
  }

  @Test
  public void testComponentInfoTelemetry_Xml_SwidCount() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-purl-swid-hash-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true, true, true, true, true, true);
    assertFilteredSbomFile(filteredContent, 5, false, hasHashes);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");

    assertThat(componentInfoTelemetry.getContentType()).isEqualTo("XML");
    assertThat(componentInfoTelemetry.getSpec()).isEqualTo("CYCLONEDX");
    assertThat(componentInfoTelemetry.getSpecVersion()).isEqualTo("1.5");
    assertThat(componentInfoTelemetry.getPurlCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getSwidCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getCoordinateCount()).isEqualTo(3);
    assertThat(componentInfoTelemetry.getEcosystemCount()).contains(entry("generic", 3), entry("maven", 1),
        entry("swid", 1));
  }

  @Test
  public void testComponentInfoTelemetry_Json_CpeCount() throws Exception {
    String sbomContent = getSbomJsonFile("sbom-with-cpe-swid.json");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");

    assertThat(componentInfoTelemetry.getContentType()).isEqualTo("JSON");
    assertThat(componentInfoTelemetry.getSpec()).isEqualTo("CYCLONEDX");
    assertThat(componentInfoTelemetry.getSpecVersion()).isEqualTo("1.5");
    assertThat(componentInfoTelemetry.getCpeCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getEcosystemCount()).contains(entry("generic", 1));
  }

  @Test
  public void testComponentInfoTelemetry_VexCount() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-vulnerabilities-v1_4-vex-data.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");

    assertThat(componentInfoTelemetry.getContentType()).isEqualTo("JSON");
    assertThat(componentInfoTelemetry.getSpec()).isEqualTo("CYCLONEDX");
    assertThat(componentInfoTelemetry.getSpecVersion()).isEqualTo("1.4");
    assertThat(componentInfoTelemetry.getVulnerabilitiesWithVexInfoCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getEcosystemCount()).contains(entry("maven", 1));
  }

  @Test
  public void testParseBom_invalidSbom_isValidNull() throws Exception {
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_4-invalid-bom.xml", null, null, null,
        getSbomXmlFile("sbom-v1_4-invalid-bom.xml"));
    assertThatExceptionOfType(SbomProcessingException.class)
        .isThrownBy(() -> sbomResultHandler.parseBom(content));
  }

  @Test
  public void testParseBom_invalidSbom_isValidNull_skipSbomImportValidation() throws Exception {
    SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.setEnabled(true);
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_4-invalid-bom.xml", null, null, null,
        getSbomXmlFile("sbom-v1_4-invalid-bom.xml"));

    Bom bom = parseBom(content, false);

    assertThat(bom).isNotNull();
    List<Component> components = bom.getComponents();
    assertThat(components).isNotEmpty().hasSize(1);
    Component component = components.get(0);
    assertThat(component.getBomRef()).isEqualTo("pkg:fake/com.google.guava/guava@30.1-jre?type=jar");
    assertThat(component.getName()).isNull();
    assertThat(component.getGroup()).isEqualTo("com.google.guava");
    assertThat(component.getVersion()).isEqualTo("30.1-jre");
    assertThat(component.getPurl()).isEqualTo("pkg:fake/com.google.guava/guava@30.1-jre?type=jar");
  }

  @Test
  public void testParseBom_invalidSbom_isValidTrue() throws Exception {
    when(thirdPartyScanContext.isValid()).thenReturn(true);
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_4-invalid-bom.xml", null, null, null,
        getSbomXmlFile("sbom-v1_4-invalid-bom.xml"));
    assertThatExceptionOfType(SbomProcessingException.class)
        .isThrownBy(() -> sbomResultHandler.parseBom(content));
  }

  @Test
  public void testParseBom_invalidSbom_isValidFalse() throws Exception {
    when(thirdPartyScanContext.isValid()).thenReturn(false);
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-v1_4-invalid-bom.xml", null, null, null,
            getSbomXmlFile("sbom-v1_4-invalid-bom.xml"));

    Bom bom = parseBom(content, false);

    assertThat(bom).isNotNull();
    List<Component> components = bom.getComponents();
    assertThat(components).isNotEmpty().hasSize(1);
    Component component = components.get(0);
    assertThat(component.getBomRef()).isEqualTo("pkg:fake/com.google.guava/guava@30.1-jre?type=jar");
    assertThat(component.getName()).isNull();
    assertThat(component.getGroup()).isEqualTo("com.google.guava");
    assertThat(component.getVersion()).isEqualTo("30.1-jre");
    assertThat(component.getPurl()).isEqualTo("pkg:fake/com.google.guava/guava@30.1-jre?type=jar");
  }

  @Test
  public void testParseBom_invalidSbom_nullThirdPartyScanContext() throws Exception {
    sbomResultHandler = new SbomResultHandler(
        thirdPartyFileDAO,
        fileCoordinatePersister,
        thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO,
        thirdPartySbomMetadataDAO,
        multiLicenseDAO,
        thirdPartyVexDAO,
        telemetryUtils,
        telemetrySender,
        null
    );
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_4-invalid-bom.xml", null, null, null,
        getSbomXmlFile("sbom-v1_4-invalid-bom.xml"));
    assertThatExceptionOfType(SbomProcessingException.class)
        .isThrownBy(() -> sbomResultHandler.parseBom(content));
  }

  @Test
  public void testParseBom_invalidSbom_nullThirdPartyScanContext_skipSbomImportValidation() throws Exception {
    sbomResultHandler = new SbomResultHandler(
        thirdPartyFileDAO,
        fileCoordinatePersister,
        thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO,
        thirdPartySbomMetadataDAO,
        multiLicenseDAO,
        thirdPartyVexDAO,
        telemetryUtils,
        telemetrySender,
        null
    );
    SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.setEnabled(true);
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_4-invalid-bom.xml", null, null, null,
        getSbomXmlFile("sbom-v1_4-invalid-bom.xml"));

    Bom bom = parseBom(content, false);

    assertThat(bom).isNotNull();
    List<Component> components = bom.getComponents();
    assertThat(components).isNotEmpty().hasSize(1);
    Component component = components.get(0);
    assertThat(component.getBomRef()).isEqualTo("pkg:fake/com.google.guava/guava@30.1-jre?type=jar");
    assertThat(component.getName()).isNull();
    assertThat(component.getGroup()).isEqualTo("com.google.guava");
    assertThat(component.getVersion()).isEqualTo("30.1-jre");
    assertThat(component.getPurl()).isEqualTo("pkg:fake/com.google.guava/guava@30.1-jre?type=jar");
  }

  @Test
  public void testHandleAndFilterContents_fallbackToCpe() throws Exception {
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("invalid-purl-bom.json", null, null, null, getSbomJsonFile("invalid-purl-bom.json"));

    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(1).extracting(ThirdPartyFileCoordinate::getPackageUrl)
        .containsExactlyInAnyOrder("pkg:generic/red_inc./fonts-filesystem@2.0.5?part=a");

    assertDebugLogOutput("Invalid Component Identifier for provided purl pkg:rpm/fonts-filesystem@2.0.5");
  }

  @Test
  public void testHandleAndFilterContents_validSbom() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_4.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("sbom-v1_4.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

    FilteredThirdPartyContent filteredThirdPartyContent =
        sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredThirdPartyContent.hasErrors()).isFalse();
  }

  @Test
  public void testHandleAndFilterContents_validSbom_telemetryData() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_4.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-v1_4-invalid-bom.xml", null, null, null, sbomContent);
    when(thirdPartyScanContext.isValid()).thenReturn(true);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes).isNotNull();
    assertThat(telemetryAttributes.get("is_skip_sbom_validation_feature_flag_enabled")).isEqualTo(false);
    assertThat(telemetryAttributes.get("is_sbom_valid")).isEqualTo(true);

    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");
    assertThat(componentInfoTelemetry.getEcosystemCount()).contains(entry("generic", 1), entry("maven", 1));
  }

  @Test
  public void testHandleAndFilterContents_validSbom_skipSbomValidationFeatureEnabled_telemetryData() throws Exception {
    SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.setEnabled(true);
    when(thirdPartyScanContext.isValid()).thenReturn(true);
    String sbomContent = getSbomXmlFile("sbom-v1_4.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-v1_4-invalid-bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes).isNotNull();
    assertThat(telemetryAttributes.get("is_skip_sbom_validation_feature_flag_enabled")).isEqualTo(true);
    assertThat(telemetryAttributes.get("is_sbom_valid")).isEqualTo(true);

    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");
    assertThat(componentInfoTelemetry.getEcosystemCount()).contains(entry("generic", 1), entry("maven", 1));
  }

  @Test
  public void testHandleAndFilterContents_invalidSbom_telemetryData() throws Exception {
    when(thirdPartyScanContext.isValid()).thenReturn(false);

    String sbomContent = getSbomXmlFile("sbom-v1_4-invalid-bom.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-v1_4-invalid-bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes).isNotNull();
    assertThat(telemetryAttributes.get("is_skip_sbom_validation_feature_flag_enabled")).isEqualTo(false);
    assertThat(telemetryAttributes.get("is_sbom_valid")).isEqualTo(false);

    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");
    assertThat(componentInfoTelemetry.getEcosystemCount()).contains(entry("fake", 1));
  }

  @Test
  public void testHandleAndFilterContents_invalidSbom_skipSbomValidationFeatureEnabled_telemetryData()
      throws Exception
  {
    SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.setEnabled(true);
    when(thirdPartyScanContext.isValid()).thenReturn(false);

    String sbomContent = getSbomXmlFile("sbom-v1_4-invalid-bom.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-v1_4-invalid-bom.xml", null, null, null,
            sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes).isNotNull();
    assertThat(telemetryAttributes.get("is_skip_sbom_validation_feature_flag_enabled")).isEqualTo(true);
    assertThat(telemetryAttributes.get("is_sbom_valid")).isEqualTo(false);

    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");
    assertThat(componentInfoTelemetry.getEcosystemCount()).contains(entry("fake", 1));
  }

  @Test
  public void testDependencyGraph_telemetryData() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_4.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-v1_4.xml", null, null, null,
            sbomContent);
    when(thirdPartyScanContext.isValid()).thenReturn(true);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes).isNotNull();
    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");
    assertThat(componentInfoTelemetry.getHasDependencies()).isEqualTo(true);
    assertThat(componentInfoTelemetry.getInvalidLicensesCount()).isEqualTo(0);
    assertThat(componentInfoTelemetry.getValidLicensesCount()).isEqualTo(0);
  }

  @Test
  public void testDependencyGraph_noDependencies_telemetryData() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-v1_4_no_dependencies.xml");
    when(thirdPartyScanContext.isValid()).thenReturn(true);
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-v1_4_no_dependencies.xml", null, null, null,
            sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes).isNotNull();
    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");
    assertThat(componentInfoTelemetry.getHasDependencies()).isEqualTo(false);
    assertThat(componentInfoTelemetry.getInvalidLicensesCount()).isEqualTo(0);
    assertThat(componentInfoTelemetry.getValidLicensesCount()).isEqualTo(0);
  }

  @Test
  public void testInvalidLicenses_telemetryData() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-license-invalid-expression.xml");
    when(thirdPartyScanContext.isValid()).thenReturn(true);
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-license-invalid-expression.xml", null, null, null,
            sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    sbomResultHandler.handleAndFilterContents(content, thirdPartyFile);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes).isNotNull();
    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");
    assertThat(componentInfoTelemetry.getInvalidLicensesCount()).isEqualTo(2);
    assertThat(componentInfoTelemetry.getValidLicensesCount()).isEqualTo(0);
  }

  @Test
  public void testHandleAndFilterContents_originalSbomFilenamePropertyGetsInsertedInDb_json() throws Exception {
    String ingestedFilename = "original-bom-filename-property.json";
    String sbomContent = getSbomJsonFile(ingestedFilename);
    ThirdPartyScanContent content = new ThirdPartyScanContent(ingestedFilename, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, ingestedFilename);
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 4);
    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    // Check original filename property in ingested sbom metadata
    assertThat(sbomMetadata).isNotNull().extracting(ThirdPartySbomMetadata::getOriginalBinaryFileName)
        .isEqualTo("binary.temp");
  }

  @Test
  public void testHandleAndFilterContents_filteredIdentitySbom() throws Exception {
    String ingestedFilename = "sbom-identity-order.xml";
    String sbomContent = getSbomXmlFile(ingestedFilename);
    ThirdPartyScanContent content = new ThirdPartyScanContent(ingestedFilename, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, ingestedFilename);
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();

    String expectedContent = getSbomJsonFile("sbom-identity-order-expected.json");
    assertThatJson(filteredContent)
        .whenIgnoringPaths("components[*].properties[*].value")
        .isEqualTo(expectedContent);
  }

  @Test
  public void testHandleAndFilterContents_originalSbomFilenamePropertyGetsInsertedInDb_xml() throws Exception {
    String ingestedFilename = "original-bom-filename-property.xml";
    String sbomContent = getSbomXmlFile(ingestedFilename);
    ThirdPartyScanContent content = new ThirdPartyScanContent(ingestedFilename, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, ingestedFilename);
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(true);
    assertFilteredSbomFile(filteredContent, 1, false, hasHashes);
    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    // Check original filename property in ingested sbom metadata
    assertThat(sbomMetadata).isNotNull().extracting(ThirdPartySbomMetadata::getOriginalBinaryFileName)
        .isEqualTo("binary.temp");
  }

  @Test
  public void testHandlerAndFilterContent_ContainerScanWithVulnerability() throws Exception {
    String sbomContent = getSbomJsonFile("sbom-container-scan.json");
    String identificationSource = IdentificationSource.SONATYPE_CONTAINER.getName();
    ThirdPartyScanContent content =
        new ThirdPartyScanContent(identificationSource + "-bom.json", null, null, null,
            sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 1);
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).allSatisfy(coord -> assertThat(coord.getSource())
            .isEqualTo(identificationSource))
        .allSatisfy(coord -> assertThat(coord.getIdentificationSources()).isEqualTo(
            SbomMetadataUtils.SBOM_IDENTIFICATION_SOURCE));
  }

  @Test
  public void testHandlerAndFilterContent_LifecycleSbomsWithDuplicateComponentsHavingDifferentSonatypeSha1s()
      throws Exception
  {
    String sbomContent = getSbomXmlFile("sbom-duplicate-components-with-diff-sonatype-sha1.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("lc-bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    List<Boolean> hasHashes = List.of(false, false, false);
    assertFilteredSbomFile(filteredContent, 3, false, hasHashes);

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(3)
        .allSatisfy(coord -> assertThat(coord.getPackageUrl()).isEqualTo(
            "pkg:generic/System.Security.AccessControl.dll@4.700.19.56404?" +
                "nexusnamespace=Microsoft%20Corporation&nexustype=pecoff"));
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
      boolean optionalValuesPresent,
      String sbomMetadataId,
      String researchType)
  {
    assertThat(coordinateSecurity).isNotNull();
    assertThat(coordinateSecurity.getFileCoordinateId()).isEqualTo(coordinateId);
    assertThat(coordinateSecurity.getFixedBy()).isNull();

    assertThat(coordinateSecurity.getRefId()).isEqualTo(vulnerability.getId());
    assertThat(coordinateSecurity.getSbomMetadataId()).isEqualTo(sbomMetadataId);

    Vulnerability.Rating rating = sbomResultHandler.getValidRating(vulnerability.getRatings());
    double severityExpected = BigDecimal.valueOf(rating.getScore()).setScale(2, RoundingMode.UNNECESSARY).doubleValue();
    assertThat(coordinateSecurity.getSeverity()).isEqualTo(severityExpected);

    if (optionalValuesPresent) {
      assertThat(coordinateSecurity.getSeverityDescription()).isEqualTo(rating.getSeverity().getSeverityName());
      assertThat(Method.fromString(coordinateSecurity.getRatingMethod())).isIn(Method.CVSSV31, Method.CVSSV3,
          Method.CVSSV4);
      assertThat(coordinateSecurity.getAttackVector()).isEqualTo(rating.getVector());

      Vulnerability.Source source = vulnerability.getSource();
      assertThat(coordinateSecurity.getVulnerabilitySource()).isEqualTo(source.getName());
      assertThat(coordinateSecurity.getResearchType()).isEqualTo(researchType);
      assertThat(coordinateSecurity.getDetectionType()).isEqualTo(OTHER.getId());
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
      assertThat(coordinate.getFormat()).isEqualTo(PackageUrlIdentifier.GENERIC_FORMAT);
      assertThat(coordinate.getPackageUrl()).isEqualTo("pkg:generic/group/test@1.0?sbom_type=file");
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
      SbomFormat format,
      boolean optionalValuesPresent,
      String sbomMetadataId,
      String researchType) throws Exception
  {
    assertThirdPartyCoordinateSecurity(content, coordinateId, coordinateSecurity, format, optionalValuesPresent, true,
        sbomMetadataId, researchType);
  }

  private void assertThirdPartyCoordinateSecurity(
      String content,
      String coordinateId,
      ThirdPartyCoordinateSecurity coordinateSecurity,
      SbomFormat format,
      boolean optionalValuesPresent,
      boolean extensionVulnerability,
      String sbomMetadataId,
      String researchType) throws Exception
  {
    Bom expectedBom = ThirdPartyUtils.parseAndValidateCycloneDx(content, format);

    if (extensionVulnerability) {
      assertExtensionVulnerability(coordinateSecurity,
          (Vulnerability10) expectedBom.getComponents().get(0).getExtensions().get("vulnerabilities").getExtensions()
              .get(0), coordinateId, optionalValuesPresent, sbomMetadataId);
    }
    else {
      assertVulnerability(coordinateSecurity, expectedBom.getVulnerabilities().get(0), coordinateId,
          optionalValuesPresent, sbomMetadataId, researchType);
    }
  }

  private void assertThirdPartyCoordinateLicense(
      License licenseSbom,
      String coordinateId,
      ThirdPartyCoordinateLicense coordinateLicense)
  {
    MultiLicense sonatypeLicense = null;
    if (StringUtils.isNotEmpty(licenseSbom.getId())) {
      sonatypeLicense = multiLicenseDAO.getByIdNoReload(licenseSbom.getId());
    }
    else if (StringUtils.isNotEmpty(licenseSbom.getName())) {
      sonatypeLicense = multiLicenseDAO.getByNameNoReload(licenseSbom.getName());
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
      boolean optionalValuesPresent,
      String sbomMetadataId)
  {
    assertThat(coordinateSecurity).isNotNull();
    assertThat(coordinateSecurity.getFileCoordinateId()).isNotNull();
    assertThat(coordinateSecurity.getFileCoordinateId()).isEqualTo(coordinateId);
    assertThat(coordinateSecurity.getFixedBy()).isNull();

    assertThat(coordinateSecurity.getRefId()).isEqualTo(vulnerability.getId());
    assertThat(coordinateSecurity.getSbomMetadataId()).isEqualTo(sbomMetadataId);

    Rating rating = vulnerability.getRatings().get(0);
    double severityExpected =
        BigDecimal.valueOf(rating.getScore().getBase()).setScale(2, RoundingMode.UNNECESSARY).doubleValue();
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

  @Test
  public void testHandleAndFilterContents_originalPurlProperty_withValidPurl() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-purl-hashes-coordinates-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = SbomTestHelper.parseToCycloneDxBom(filteredContent);
    List<Component> components = bom.getComponents();

    // Verify that components with valid purls have the originalPurl property
    Component tomcatComponent = components.stream()
        .filter(c -> "tomcat-catalina".equals(c.getName()))
        .findFirst()
        .orElseThrow();

    assertThat(tomcatComponent.getProperties()).isNotNull();
    assertThat(tomcatComponent.getProperties().stream()
        .filter(p -> p.getName().equals(SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME))
        .findFirst())
        .isPresent()
        .hasValueSatisfying(property ->
            assertThat(property.getValue()).isEqualTo("pkg:maven/org.apache.tomcat/tomcat-catalina@9.0.14?type=jar"));
  }

  @Test
  public void testHandleAndFilterContents_originalPurlProperty_withInvalidPurl() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-purl-hashes-coordinates-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = SbomTestHelper.parseToCycloneDxBom(filteredContent);
    List<Component> components = bom.getComponents();

    // Django component has invalid purl "pkg:pypi" but should still have originalPurl property
    Component djangoComponent = components.stream()
        .filter(c -> "django".equals(c.getName()))
        .findFirst()
        .orElseThrow();

    assertThat(djangoComponent.getProperties()).isNotNull();
    assertThat(djangoComponent.getProperties().stream()
        .filter(p -> p.getName().equals(SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME))
        .findFirst())
        .isPresent()
        .hasValueSatisfying(property ->
            assertThat(property.getValue()).isEqualTo("pkg:pypi"));
  }

  @Test
  public void testHandleAndFilterContents_originalPurlProperty_withHashPrioritized() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-purl-cpe-hash-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = SbomTestHelper.parseToCycloneDxBom(filteredContent);
    List<Component> components = bom.getComponents();

    // Verify that components have the originalPurl property where applicable
    Component django = components.stream()
        .filter(c -> "django".equals(c.getName()) && "1.2.3".equals(c.getVersion()))
        .findFirst()
        .orElseThrow();

    assertThat(django.getProperties()).isNotNull();
    assertThat(django.getProperties().stream()
        .filter(p -> p.getName().equals(SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME))
        .findFirst())
        .isPresent();
  }

  @Test
  public void testHandleAndFilterContents_originalPurlProperty_withSwidConversion() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-purl-swid-hash-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = SbomTestHelper.parseToCycloneDxBom(filteredContent);
    List<Component> components = bom.getComponents();

    // Verify that components have the originalPurl property where applicable
    Component log4j = components.stream()
        .filter(c -> "Apache Log4J".equals(c.getName()) && "2.11.2".equals(c.getVersion()))
        .findFirst()
        .orElseThrow();

    assertThat(log4j.getPurl()).isNotNull().startsWith("pkg:swid");
    assertThat(log4j.getProperties()).isNotNull();
    assertThat(SbomUtils.getPropertyForPropertyName(log4j, SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME))
        .startsWith("pkg:log4j");
  }

  @Test
  public void testHandleAndFilterContents_originalPurlProperty_notIncluded() throws Exception {
    String sbomContent = getSbomXmlFile("scan-with-sbom-coords-no-purl.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = SbomTestHelper.parseToCycloneDxBom(filteredContent);
    List<Component> components = bom.getComponents();

    // Components without purls should NOT have the originalPurl property
    for (Component component : components) {
      if (component.getProperties() != null) {
        assertThat(component.getProperties().stream()
            .filter(p -> p.getName().equals(SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME))
            .findFirst())
            .isEmpty();
      }
    }
  }

  @Test
  public void testHandleAndFilterContents_originalPurlProperty_preservedThroughProcessing() throws Exception {
    String sbomContent = getSbomXmlFile("sbom-component-license-vulnerability.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("sbom-component-license-vulnerability.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = SbomTestHelper.parseToCycloneDxBom(filteredContent);
    List<Component> components = bom.getComponents();

    // Verify components with purls have originalPurl property
    Component jacksonComponent = components.stream()
        .filter(c -> "jackson-databind".equals(c.getName()))
        .findFirst()
        .orElseThrow();

    assertThat(jacksonComponent.getPurl()).isNotNull();
    assertThat(jacksonComponent.getProperties()).isNotNull();
    assertThat(jacksonComponent.getProperties().stream()
        .filter(p -> p.getName().equals(SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME))
        .findFirst())
        .isPresent()
        .hasValueSatisfying(property ->
            assertThat(property.getValue()).isEqualTo(
                "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar"));
  }

  private void assertDebugLogOutput(final String message) {
    assertThat(logOutput.getDebugMessages(loggerName)).contains(message);
  }

  private Bom parseBom(final ThirdPartyScanContent thirdPartyScanContent, final boolean expectedIsValid)
      throws Exception
  {
    Pair<Bom, Boolean> result = sbomResultHandler.parseBom(thirdPartyScanContent);
    assertThat(result).isNotNull();
    assertThat(result.getLeft()).isNotNull();
    assertThat(result.getRight()).isEqualTo(expectedIsValid);
    return result.getLeft();
  }
}
