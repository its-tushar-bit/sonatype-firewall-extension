/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.SbomTaxonomy;
import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.sbom.SbomComponentInfoTelemetry;
import com.sonatype.insight.brain.sbom.SbomTestHelper;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.TelemetryDataObfuscator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.model.ProjectScanItem;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Swid;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.spdx.library.model.SpdxDocument;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils.PROPERTY_COMPONENT_REF;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.OTHER;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.PUBLIC_RESEARCH;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.VENDOR_RESEARCH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class SpdxResultHandlerTest
    extends AbstractDataTest
{
  private ThirdPartyFileDAO thirdPartyFileDAO;

  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister;

  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVexDAO;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private ThirdPartyScanContext thirdPartyScanContext;

  private TelemetryUtils telemetryUtils;

  private TelemetrySender telemetrySender;

  private MultiLicenseDAO multiLicenseDAO;

  private SpdxResultHandler spdxResultHandler;

  private final String loggerName = SpdxResultHandler.class.getName();

  @Rule
  public LogOutput logOutput = new LogOutput(loggerName);

  @Rule
  public LogOutput logOutputUtils = new LogOutput(ThirdPartyUtils.class.getName());

  @Before
  public void setUp() {
    Configuration configurationMock = mock(Configuration.class);
    telemetryUtils = new TelemetryUtils(new TelemetryDataObfuscator(configurationMock));
    telemetrySender = mock(TelemetrySender.class);
    thirdPartyCoordinateLicenseDAO = daoFactory.createThirdPartyCoordinateLicenseDAO();
    thirdPartyVexDAO = daoFactory.createThirdPartyVulnerabilityExploitabilityExchangeDAO();
    thirdPartyCoordinateSecurityDAO = daoFactory.createThirdPartyCoordinateSecurityDAO();
    thirdPartyFileCoordinateDAO = daoFactory.createThirdPartyFileCoordinateDAO();
    thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();
    thirdPartyScanContext = mock(ThirdPartyScanContext.class);
    multiLicenseDAO = daoFactory.createMultiLicenseDAO();
    fileCoordinatePersister = new DuplicateAwareThirdPartyFileCoordinatePersister(thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO, thirdPartyCoordinateLicenseDAO, thirdPartyVexDAO);
    // Mockito mocks with Boolean values return false by default instead of null
    // Change it to return null since we rely on this value
    when(thirdPartyScanContext.isValid()).thenReturn(null);
    spdxResultHandler =
        new SpdxResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, thirdPartyScanContext);
  }

  @Test
  public void testHandleAndFilterContents_Purl_Then_Sha1_Then_Coordinates() throws Exception {
    String sbomContent = getSbomXmlFile("purl-hashes-coordinates.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("spdx.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = assertFilteredSbomFile(filteredContent, 4);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName)
        .containsExactlyInAnyOrder("iq_application_SCM Test 1", "log4j-core", "log4j-api", "joda-time");
    assertThat(components).extracting(Component::getVersion)
        .containsExactlyInAnyOrder("76b10b862e7b42009f2415097620928c", "2.13.2", "2.13.2", null);
    // 3 purls were collected: 2 original purls, 1 from coordinates, 1 not collected (hash beats coordinates)
    assertThat(components).extracting(Component::getPurl)
        .containsExactlyInAnyOrder(
            "pkg:generic/sonatype/iq_application_SCM%20Test%201@76b10b862e7b42009f2415097620928c",
            "pkg:maven/org.apache.logging.log4j/log4j-core@2.13.2?type=jar",
            "pkg:generic/org.apache.logging.log4j/log4j-api@2.13.2?sbom_type=library",
            null);
    assertThat(components).extracting("properties.size")
        .containsOnly(1, 1, 1, 2);
    // 1 component hash was collected
    assertThat(components.get(3).getProperties())
        .flatExtracting(Property::getValue)
        .contains("9188560f22e0b73070d2");
    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testHandleAndFilterContents_Purl_Then_Cpe_Then_Sha1_Then_Coordinates() throws Exception {
    String sbomContent = getSbomXmlFile("purl-cpe-hash-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("spdx.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = assertFilteredSbomFile(filteredContent, 5);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName).containsExactlyInAnyOrder(
        "iq_application_SCM Test 1", "log4j-core", "log4j", "log4j-api", "joda-time");
    assertThat(components).extracting(Component::getVersion)
        .containsExactlyInAnyOrder("76b10b862e7b42009f2415097620928c", "2.13.2", "2.13.2", "2.11.2", null);
    // 4 purls were collected: 2 original purls, 1 from cpe, 1 from coordinates, 1 skipped (hash beats coordinates)
    assertThat(components).extracting(Component::getPurl)
        .containsExactlyInAnyOrder(
            "pkg:generic/sonatype/iq_application_SCM%20Test%201@76b10b862e7b42009f2415097620928c",
            "pkg:maven/org.apache.logging.log4j/log4j-core@2.13.2?type=jar",
            "pkg:generic/org.apache.logging.log4j/log4j-api@2.13.2?sbom_type=library",
            "pkg:generic/apache/log4j@2.11.2?part=a&update=rc3",
            null);
    assertThat(components).extracting("properties.size")
        .containsOnly(1, 1, 1, 1, 2);
    // 1 component hash was collected
    assertThat(components.get(4).getProperties())
        .flatExtracting(Property::getValue)
        .contains("9188560f22e0b73070d2");
    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testHandleAndFilterContents_priority_Sha1_Then_Coordinates() throws Exception {
    String sbomContent = getSbomXmlFile("hash-coordinates-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = assertFilteredSbomFile(filteredContent, 1);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName).containsOnly("tomcat-catalina");
    assertThat(components).extracting(Component::getVersion).containsOnly("9.0.14");
    assertThat(components).extracting(Component::getPurl)
        .containsExactly("pkg:generic/tomcat-catalina@9.0.14?sbom_type=application");
    assertThat(components).extracting("properties.size").containsOnly(2);
    assertThat(components.get(0).getProperties())
        .flatExtracting(Property::getValue)
        .contains("e7b1000b94e835ffd37f");
    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testHandleAndFilterContents_fallbackToCpe() throws Exception {
    String sbomContent = getSbomXmlFile("invalid-purl-bom.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("invalid-purl-bom.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = assertFilteredSbomFile(filteredContent, 1);
    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getName).containsExactly("fonts-filesystem");
    assertThat(components).extracting(Component::getVersion).containsExactly("2.0.5");

    assertThat(components).extracting(Component::getPurl)
        .containsExactly("pkg:generic/red_inc./fonts-filesystem@2.0.5?part=a");
    assertComponentRef(thirdPartyFile.getId());
    assertDebugLogOutput("Invalid purl: pkg:rpm/fonts-filesystem@2.0.5");
  }

  @Test
  public void testHandleAndFilterContents_nullContent() {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, null);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isNull();
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_emptyContent() {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, "");
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isBlank();
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_invalid_isValidNull_Xml() throws Exception {
    testHandleAndFilterContents_invalid_isValidNull(getSbomXmlFile("spdx-invalid.xml"), "spdx-invalid.xml");
    spdxResultHandler =
        new SpdxResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, null);
    testHandleAndFilterContents_invalid_isValidNull(getSbomXmlFile("spdx-invalid.xml"), "spdx-invalid.xml");
  }

  @Test
  public void testHandleAndFilterContents_invalid_isValidNull_Json() throws Exception {
    testHandleAndFilterContents_invalid_isValidNull(getSbomJsonFile("spdx-invalid.json"), "spdx-invalid.json");
    spdxResultHandler =
        new SpdxResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, null);
    testHandleAndFilterContents_invalid_isValidNull(getSbomJsonFile("spdx-invalid.json"), "spdx-invalid.json");
  }

  private void testHandleAndFilterContents_invalid_isValidNull(String sbomContent, String path) {
    ThirdPartyScanContent content = new ThirdPartyScanContent(path, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
            () -> spdxResultHandler.handleAndFilterContents(content, thirdPartyFile))
        .withMessage("Error filtering SPDX file " + path);

    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId())).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_invalid_isValidNull_skipSbomImportValidation_Xml() throws Exception {
    testHandleAndFilterContents_invalid_isValidNull_skipSbomImportValidation(getSbomXmlFile("spdx-invalid.xml"),
        "spdx-invalid.xml");
    spdxResultHandler =
        new SpdxResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, null);
    testHandleAndFilterContents_invalid_isValidNull_skipSbomImportValidation(getSbomXmlFile("spdx-invalid.xml"),
        "spdx-invalid.xml");
  }

  @Test
  public void testHandleAndFilterContents_invalid_isValidNull_skipSbomImportValidation_Json() throws Exception {
    testHandleAndFilterContents_invalid_isValidNull_skipSbomImportValidation(getSbomJsonFile("spdx-invalid.json"),
        "spdx-invalid.json");
    spdxResultHandler =
        new SpdxResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, null);
    testHandleAndFilterContents_invalid_isValidNull_skipSbomImportValidation(getSbomJsonFile("spdx-invalid.json"),
        "spdx-invalid.json");
  }

  private void testHandleAndFilterContents_invalid_isValidNull_skipSbomImportValidation(
      String sbomContent,
      String path)
  {
    SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.setEnabled(true);

    ThirdPartyScanContent content = new ThirdPartyScanContent(path, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    FilteredThirdPartyContent filteredThirdPartyContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredThirdPartyContent.hasErrors()).isTrue();
  }

  @Test
  public void testHandleAndFilterContents_invalid_isValidFalse_Xml() throws Exception {
    testHandleAndFilterContents_invalid_isValidFalse(getSbomXmlFile("spdx-invalid.xml"), "spdx-invalid.xml");
  }

  @Test
  public void testHandleAndFilterContents_invalid_isValidFalse_Json() throws Exception {
    testHandleAndFilterContents_invalid_isValidFalse(getSbomJsonFile("spdx-invalid.json"), "spdx-invalid.json");
  }

  private void testHandleAndFilterContents_invalid_isValidFalse(String sbomContent, String path) {
    when(thirdPartyScanContext.isValid()).thenReturn(false);

    ThirdPartyScanContent content = new ThirdPartyScanContent(path, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    FilteredThirdPartyContent filteredThirdPartyContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredThirdPartyContent.hasErrors()).isTrue();
  }

  @Test
  public void testHandleAndFilterContents_invalid_isValidTrue_Xml() throws Exception {
    testHandleAndFilterContents_invalid_isValidTrue(getSbomXmlFile("spdx-invalid.xml"), "spdx-invalid.xml");
  }

  @Test
  public void testHandleAndFilterContents_invalid_isValidTrue_Json() throws Exception {
    testHandleAndFilterContents_invalid_isValidTrue(getSbomJsonFile("spdx-invalid.json"), "spdx-invalid.json");
  }

  private void testHandleAndFilterContents_invalid_isValidTrue(String sbomContent, String path) {
    ThirdPartyScanContent content = new ThirdPartyScanContent(path, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
            () -> spdxResultHandler.handleAndFilterContents(content, thirdPartyFile))
        .withMessage("Error filtering SPDX file " + path);

    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId())).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_Xml_v2_3() throws Exception {
    String sbomContent = getSbomXmlFile("spdx-v2_3.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-v2_3.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    FilteredThirdPartyContent filteredContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    Bom bom = assertFilteredSbomFile(sbomXml, 6);
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/sonatype/iq_application_SCM_Test");

    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getPurl)
        .containsExactlyInAnyOrder(
            "pkg:maven/org.apache.logging.log4j/log4j-core@2.13.2?type=jar",
            "pkg:maven/junit/junit@4.12?type=jar",
            "pkg:generic/sonatype/iq_application_SCM%20Test%201@76b10b862e7b42009f2415097620928c?sbom_type=application",
            "pkg:maven/org.hamcrest/hamcrest-core@1.3?type=jar",
            "pkg:maven/org.apache.logging.log4j/log4j-api@2.13.2?type=jar",
            "pkg:maven/org.yaml/snakeyaml@1.29?type=jar");
    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testHandleAndFilterContents_Json_v2_3() throws Exception {
    String sbomContent = getSbomJsonFile("spdx-v2_3.json");
    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-v2_3.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    FilteredThirdPartyContent filteredContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    Bom bom = assertFilteredSbomFile(sbomXml, 6);
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/sonatype/iq_application_SCM_Test");

    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getPurl)
        .containsExactlyInAnyOrder(
            "pkg:maven/org.apache.logging.log4j/log4j-core@2.13.2?type=jar",
            "pkg:maven/junit/junit@4.12?type=jar",
            "pkg:generic/sonatype/iq_application_SCM%20Test%201@76b10b862e7b42009f2415097620928c?sbom_type=application",
            "pkg:maven/org.hamcrest/hamcrest-core@1.3?type=jar",
            "pkg:maven/org.apache.logging.log4j/log4j-api@2.13.2?type=jar",
            "pkg:maven/org.yaml/snakeyaml@1.29?type=jar");
    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testHandleAndFilterContents_Json_v2_3_noPurl() throws Exception {
    String sbomContent = getSbomJsonFile("spdx-v2_3-nopurl.json");
    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-v2_3-nopurl.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    FilteredThirdPartyContent filteredContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    // 3 components (1 maven, 1 generic, 1 library with empty purl: 1 skipped because purl is empty and unable to parse
    Bom bom = assertFilteredSbomFile(sbomXml, 2);
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/sonatype/iq_application_SCM_Test");

    List<Component> components = bom.getComponents();
    assertThat(components).extracting(Component::getPurl)
        .containsExactlyInAnyOrder(
            "pkg:maven/org.apache.logging.log4j/log4j-core@2.13.2?type=jar",
            "pkg:generic/sonatype/iq_application_SCM%20Test%201@76b10b862e7b42009f2415097620928c?sbom_type=" +
                "application");
    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testHandleAndFilterContents_Json_Licenses() throws Exception {
    String sbomContent = getSbomJsonFile("spdx-licenses.json");
    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-licenses.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);
    FilteredThirdPartyContent filteredContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    Bom bom = assertFilteredSbomFile(sbomXml, 10);
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo(
        "pkg:maven/com.sonatype.testing/pr-comment-02@1.0-SNAPSHOT?type=jar");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(10);
    List<String> coordinateIds = coordinates.stream().map(ThirdPartyFileCoordinate::getId).toList();

    try (TransactionContext tx = thirdPartyCoordinateLicenseDAO.createTransactionContext()) {
      List<ThirdPartyCoordinateLicense> coordinatesLicenses = new ArrayList<>();
      for (String coordinateId : coordinateIds) {
        List<ThirdPartyCoordinateLicense> licenses =
            thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tx, coordinateId);
        coordinatesLicenses.addAll(licenses);
      }
      Set<String> licenseIdSet =
          coordinatesLicenses.stream().map(ThirdPartyCoordinateLicense::getLicenseId).collect(Collectors.toSet());
      Set<String> licenseNameSet =
          coordinatesLicenses.stream().map(ThirdPartyCoordinateLicense::getName).collect(Collectors.toSet());

      assertThat(licenseIdSet).containsExactlyInAnyOrder(
          "Apache-2.0",
          "Apache-2.0-EPL-1.0",
          "CC0-1.0",
          "EPL-1.0",
          "GPL-2.0-with-classpath-exception",
          "LGPL-2.1",
          "LGPL-3.0",
          "MIT",
          "MPL-1.1",
          "COMMERCIAL",
          "PUBLIC-DOMAIN",
          "UNSPECIFIED"
      );

      assertThat(licenseNameSet).containsExactlyInAnyOrder(
          "Apache-2.0",
          "Apache-2.0 or EPL-1.0",
          "CC0-1.0",
          "EPL-1.0",
          "GPL-2.0-with-classpath-exception",
          "LGPL-2.1",
          "LGPL-3.0",
          "MIT",
          "MPL-1.1",
          "COMMERCIAL",
          "Public Domain",
          "Not Provided"
      );
    }
    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testHandleAndFilterContents_Json_Vulnerabilities() throws Exception {
    String sbomContent = getSbomJsonFile("spdx-v2_3.json");
    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-v2_3.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScanContext thirdPartyScanContext = new ThirdPartyScanContext(null, null, null, null, null);
    thirdPartyScanContext.setSbomMetadataId("someSbomMetadataId");
    thirdPartyScanContext.setIsValid(true);
    spdxResultHandler =
        new SpdxResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, thirdPartyScanContext);
    FilteredThirdPartyContent filteredContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    Bom bom = assertFilteredSbomFile(sbomXml, 6);
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/sonatype/iq_application_SCM_Test");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(6);
    List<String> coordinateIds = coordinates.stream().map(ThirdPartyFileCoordinate::getId).toList();

    try (TransactionContext tx = thirdPartyCoordinateSecurityDAO.createTransactionContext()) {
      List<ThirdPartyCoordinateSecurity> allSecurityRecords = new ArrayList<>();
      for (String coordinateId : coordinateIds) {
        List<ThirdPartyCoordinateSecurity> securityRecords =
            thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(tx, coordinateId);
        allSecurityRecords.addAll(securityRecords);
      }
      Set<String> refIdSet =
          allSecurityRecords.stream().map(ThirdPartyCoordinateSecurity::getRefId).collect(Collectors.toSet());
      assertThat(refIdSet).containsExactlyInAnyOrder(
          "CVE-2021-45046", "CVE-2021-45105", "CVE-2020-15250", "sonatype-2021-4560", "GHSA-5469-c5p2-xv5g"
      );
      assertThat(allSecurityRecords).allMatch(
          s -> s.getSbomMetadataId().equals(thirdPartyScanContext.getSbomMetadataId()));

      // PUBLIC_RESEARCH: CVE-2021-45046, CVE-2021-45105, CVE-2020-15250
      // VENDOR_RESEARCH: sonatype-2021-4560, GHSA-5469-c5p2-xv5g
      assertThat(allSecurityRecords).allMatch(s -> Objects.equals(s.getResearchType(),
          s.getRefId().startsWith("CVE-") ? PUBLIC_RESEARCH.name() : VENDOR_RESEARCH.name()));
      assertThat(allSecurityRecords).allMatch(s -> s.getDetectionType().equals(OTHER.getId()));
    }
    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testHandleAndFilterContents_withSha256() throws Exception {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);

    String sbomContent = getSbomXmlFile("spdx-sha256.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-sha256.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 3, Algorithm.SHA_256);
    assertComponentRef(thirdPartyFile.getId());
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(false);
  }

  @Test
  public void testHandleAndFilterContents_NoComponentsSaved() throws Exception {
    String sbomContent = getSbomJsonFile("spdx-no-packages-saved.json");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("spdx-no-packages-saved.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String actualFilteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();

    assertThat(actualFilteredContent).isNotNull();
    Bom actualFilteredBom = SbomTestHelper.parseToCycloneDxBom(actualFilteredContent);

    assertThat(actualFilteredBom).isNotNull();
    assertThat(actualFilteredBom.getComponents()).hasSize(1);
    Component actualComponent = actualFilteredBom.getComponents().get(0);
    assertThat(actualComponent.getName()).isEqualTo("django");
    assertThat(actualComponent.getProperties()).size().isEqualTo(2);
    assertThat(actualComponent.getProperties().get(0).getName())
        .isEqualTo(SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME);
    assertThat(actualComponent.getProperties().get(0).getValue()).isEqualTo("9188560f22e0b73070d2");
    assertThat(actualComponent.getVersion()).isNull();
    assertThat(actualComponent.getHashes()).isNull();
    assertThat(actualComponent.getAuthor()).isNull();
    assertThat(actualComponent.getHashes()).isNull();
    assertThat(actualComponent.getCpe()).isNull();
    assertThat(actualComponent.getPurl()).isNull();
    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(0);
  }

  @Test
  public void testProcessDependencyGraph() throws Exception {
    String sbomContent = getSbomJsonFile("spdx-v2_3.json");
    SpdxDocument spdxDocument = ThirdPartyUtils.parseAndValidateSpdx(sbomContent, SbomFormat.JSON);

    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-v2_3.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    FilteredThirdPartyContent filteredContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    Bom targetBom = assertFilteredSbomFile(sbomXml, 6);

    List<ProjectScanItem> result = new ArrayList<>();

    spdxResultHandler.processDependencyGraph(spdxDocument, targetBom, result,
        new ThirdPartyFile("spdx-v2_3.json", new Date()));

    assertThat(result).hasSize(1).allSatisfy(projectItem -> {
      assertThat(projectItem.getKind()).isEqualTo("sbom");
      assertThat(projectItem.getId()).isEqualTo("pkg:generic/sonatype/iq_application_SCM_Test");
      assertThat(projectItem.getPath()).isEqualTo("spdx-v2_3.json");
      List<com.sonatype.insight.scan.model.Dependency> rootDependencies = projectItem.getDependencies();
      assertThat(rootDependencies).hasSize(5)
          .extracting(com.sonatype.insight.scan.model.Dependency::getId)
          .containsExactlyInAnyOrder(
              "pkg:maven/org.hamcrest/hamcrest-core@1.3?type=jar",
              "pkg:maven/org.yaml/snakeyaml@1.29?type=jar",
              "pkg:maven/org.apache.logging.log4j/log4j-core@2.13.2?type=jar",
              "pkg:maven/junit/junit@4.12?type=jar",
              "pkg:maven/org.apache.logging.log4j/log4j-api@2.13.2?type=jar");
      assertParentAndChildDependency(rootDependencies, "pkg:maven/org.apache.logging.log4j/log4j-core@2.13.2?type=jar",
          "pkg:maven/org.apache.logging.log4j/log4j-api@2.13.2?type=jar");
      assertParentAndChildDependency(rootDependencies, "pkg:maven/junit/junit@4.12?type=jar",
          "pkg:maven/org.hamcrest/hamcrest-core@1.3?type=jar");
    });
    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testProcessDependencyGraph_MissingRootDependency() throws Exception {
    String sbomContent = getSbomJsonFile("spdx-no-root.json");
    SpdxDocument spdxDocument = ThirdPartyUtils.parseAndValidateSpdx(sbomContent, SbomFormat.JSON);

    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-no-root.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    FilteredThirdPartyContent filteredContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    Bom targetBom = assertFilteredSbomFile(sbomXml, 6);

    List<ProjectScanItem> result = new ArrayList<>();

    spdxResultHandler.processDependencyGraph(spdxDocument, targetBom, result,
        new ThirdPartyFile("spdx-no-root.json", new Date()));

    assertThat(result).isEmpty();
    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testProcessDependencyGraph_NoSourceDependencies() throws Exception {
    String sbomContent = getSbomJsonFile("spdx-no-dep-graph.json");
    SpdxDocument spdxDocument = ThirdPartyUtils.parseAndValidateSpdx(sbomContent, SbomFormat.JSON);

    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-no-dep-graph.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    FilteredThirdPartyContent filteredContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    Bom targetBom = assertFilteredSbomFile(sbomXml, 6);

    List<ProjectScanItem> result = new ArrayList<>();

    spdxResultHandler.processDependencyGraph(spdxDocument, targetBom, result,
        new ThirdPartyFile("spdx-no-dep-graph.json", new Date()));

    assertThat(result).isEmpty();
    assertComponentRef(thirdPartyFile.getId());
  }

  private void assertParentAndChildDependency(
      final List<com.sonatype.insight.scan.model.Dependency> rootDependencies,
      final String parentPurl,
      final String childPurl)
  {
    com.sonatype.insight.scan.model.Dependency parent =
        rootDependencies.stream().filter(d -> d.getId().equals(parentPurl)).findFirst().orElse(null);
    if (childPurl != null) {
      assertThat(parent).isNotNull();
      assertThat(parent.getDependencies().get(0).getId()).isEqualTo(childPurl);
    }
  }

  @Test
  public void testDetermineThirdPartyIdentificationSource() {
    assertThat(spdxResultHandler.determineThirdPartyIdentificationSource("abcd.spdx.xml")).isEqualTo("abcd");
    assertThat(spdxResultHandler.determineThirdPartyIdentificationSource("ABCD123.SPDX.XmL")).isEqualTo("ABCD123");
    assertThat(spdxResultHandler.determineThirdPartyIdentificationSource("sub/dir/abcd.spdx.xml")).isEqualTo("abcd");
    assertThat(spdxResultHandler.determineThirdPartyIdentificationSource("ABCD-SPDX.json")).isEqualTo("Third-Party");
    assertThat(spdxResultHandler.determineThirdPartyIdentificationSource("abcdspdx.xml")).isEqualTo("Third-Party");
    assertThat(spdxResultHandler.determineThirdPartyIdentificationSource("")).isEqualTo("Third-Party");
    assertThat(spdxResultHandler.determineThirdPartyIdentificationSource(null)).isEqualTo("Third-Party");
  }

  @Test
  public void testHandleAndFilterContents_CpeAndSwid_Xml() throws Exception {
    String fileName = "spdx-v2_3-with-cpe-swid.xml";
    String sbomContent = getSbomXmlFile(fileName);
    ThirdPartyScanContent content = new ThirdPartyScanContent(fileName, null, null, null, sbomContent);
    assertCpeAndSwid(content);
  }

  @Test
  public void testHandleAndFilterContents_CpeAndSwid_Json() throws Exception {
    String filename = "spdx-v2_3-with-cpe-swid.json";
    String sbomContent = getSbomJsonFile(filename);
    ThirdPartyScanContent content = new ThirdPartyScanContent(filename, null, null, null, sbomContent);
    assertCpeAndSwid(content);
  }

  @Test
  public void testComponentInfoTelemetry_Xml_PurlHashCoordinateCounts() throws Exception {
    String sbomContent = getSbomXmlFile("purl-hashes-coordinates.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("spdx.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);
    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 4);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes.get("is_skip_sbom_validation_feature_flag_enabled")).isEqualTo(false);
    assertThat(telemetryAttributes.get("is_sbom_valid")).isEqualTo(true);

    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");

    assertThat(componentInfoTelemetry.getContentType()).isEqualTo("XML");
    assertThat(componentInfoTelemetry.getSpec()).isEqualTo("SPDX");
    assertThat(componentInfoTelemetry.getSpecVersion()).isEqualTo("SPDX-2.3");
    assertThat(componentInfoTelemetry.getPurlCount()).isEqualTo(2);
    assertThat(componentInfoTelemetry.getHashCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getCoordinateCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getEcosystemCount()).contains(entry("generic", 2), entry("maven", 1));
    assertThat(componentInfoTelemetry.getHasDependencies()).isEqualTo(true);
    assertThat(componentInfoTelemetry.getValidLicensesCount()).isEqualTo(2);
    assertThat(componentInfoTelemetry.getInvalidLicensesCount()).isEqualTo(0);

    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testNoDependencies_telemetryData() throws Exception {
    String sbomContent = getSbomXmlFile("spdx-v2_3-no-dependencies.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("spdx.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

    spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes.get("is_skip_sbom_validation_feature_flag_enabled")).isEqualTo(false);
    assertThat(telemetryAttributes.get("is_sbom_valid")).isEqualTo(true);

    assertThat(telemetryAttributes).isNotNull();
    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");
    assertThat(componentInfoTelemetry.getHasDependencies()).isEqualTo(false);

    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testComponentInfoTelemetry_Json_PurlSwidCoordinateCounts() throws Exception {
    String sbomContent = getSbomJsonFile("spdx-v2_3-with-cpe-swid.json");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("spdx.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 2);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes.get("is_skip_sbom_validation_feature_flag_enabled")).isEqualTo(false);
    assertThat(telemetryAttributes.get("is_sbom_valid")).isEqualTo(true);

    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");

    assertThat(componentInfoTelemetry.getContentType()).isEqualTo("JSON");
    assertThat(componentInfoTelemetry.getSpec()).isEqualTo("SPDX");
    assertThat(componentInfoTelemetry.getSpecVersion()).isEqualTo("SPDX-2.3");
    assertThat(componentInfoTelemetry.getPurlCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getSwidCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getCoordinateCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getEcosystemCount()).contains(entry("generic", 1), entry("maven", 1));

    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testComponentInfoTelemetry_CpeCount() throws Exception {
    String sbomContent = getSbomXmlFile("purl-cpe-hash-coords.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("spdx.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(true);

    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 5);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes.get("is_skip_sbom_validation_feature_flag_enabled")).isEqualTo(false);
    assertThat(telemetryAttributes.get("is_sbom_valid")).isEqualTo(true);

    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");

    assertThat(componentInfoTelemetry.getContentType()).isEqualTo("XML");
    assertThat(componentInfoTelemetry.getSpec()).isEqualTo("SPDX");
    assertThat(componentInfoTelemetry.getSpecVersion()).isEqualTo("SPDX-2.3");
    assertThat(componentInfoTelemetry.getPurlCount()).isEqualTo(2);
    assertThat(componentInfoTelemetry.getCpeCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getHashCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getCoordinateCount()).isEqualTo(1);
    assertThat(componentInfoTelemetry.getEcosystemCount()).contains(entry("generic", 3), entry("maven", 1));

    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testHandleAndFilterContents_invalidSbom_skipSbomValidationFeatureEnabled_telemetryData()
      throws Exception
  {
    SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.setEnabled(true);

    String sbomContent = getSbomXmlFile("spdx-invalid.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent("spdx-invalid.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    when(thirdPartyScanContext.isValid()).thenReturn(false);

    spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_DATA_METRICS);

    Map<String, Object> telemetryAttributes = telemetryData.getAttributes();
    assertThat(telemetryAttributes.get("is_skip_sbom_validation_feature_flag_enabled")).isEqualTo(true);
    assertThat(telemetryAttributes.get("is_sbom_valid")).isEqualTo(false);

    SbomComponentInfoTelemetry componentInfoTelemetry =
        (SbomComponentInfoTelemetry) telemetryAttributes.get("sbom_data_summary");
    assertThat(telemetryAttributes).isNotNull();
    assertThat(componentInfoTelemetry.getContentType()).isEqualTo("XML");
    assertThat(componentInfoTelemetry.getSpec()).isEqualTo("SPDX");
    assertThat(componentInfoTelemetry.getSpecVersion()).isEqualTo("SPDX-2.3");

    assertComponentRef(thirdPartyFile.getId());
  }

  @Test
  public void testHandleAndFilterContents_filteredIdentitySbom() throws Exception {
    String ingestedFilename = "sbom-identity-order.xml";
    String sbomContent = getSbomXmlFile(ingestedFilename);
    ThirdPartyScanContent content = new ThirdPartyScanContent(ingestedFilename, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, ingestedFilename);
    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();

    String expectedContent = getSbomJsonFile("sbom-identity-order-expected.json");
    assertThatJson(filteredContent)
        .whenIgnoringPaths("metadata.timestamp", "components[*].properties[*].value")
        .isEqualTo(expectedContent);
  }

  private void assertCpeAndSwid(ThirdPartyScanContent content) throws Exception {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String cpeValue = "cpe:2.3:a:pivotal_software:spring_framework:4.1.0:*:*:*:*:*:*:*";
    String swidTagId = "gen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1";

    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = assertFilteredSbomFile(filteredContent, 2);
    Component component = bom.getComponents().get(0);
    assertThat(component.getBomRef()).isEqualTo("SPDXRef-maven-org.apache.logging.log4j-log4j-core-2.13.2");
    assertThat(component.getCpe()).isEqualTo(cpeValue);
    Swid swid = component.getSwid();
    assertThat(swid.getTagId()).isEqualTo(swidTagId);
    assertThat(swid.getName()).isNull();
    assertThat(swid.getVersion()).isNull();
    assertThat(swid.getTagVersion()).isNull();
    assertThat(swid.isPatch()).isNull();
    assertThat(swid.getAttachmentText()).isNull();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(2);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
    assertThat(thirdPartyFileCoordinate.getCpe()).isEqualTo(cpeValue);
    assertThat(thirdPartyFileCoordinate.getSwid())
        .isEqualTo("{" +
            "\"tagId\":\"" + swidTagId + "\"}");
    assertThat(thirdPartyFileCoordinate.getIdentificationSources())
        .isEqualTo(SbomMetadataUtils.SBOM_IDENTIFICATION_SOURCE);

    assertComponentRef(thirdPartyFile.getId());
  }

  private String getSbomFile(final String fileType, final String fileName) throws Exception {
    URL resource = getClass().getResource("/SpdxResultHandlerTest/" + fileType + "/" + fileName);
    return Files.readString(Paths.get(resource.toURI()));
  }

  private String getSbomXmlFile(final String fileName) throws Exception {
    return getSbomFile("xml", fileName);
  }

  private String getSbomJsonFile(final String fileName) throws Exception {
    return getSbomFile("json", fileName);
  }

  private Bom assertFilteredSbomFile(final String content, final int expectedComponentCount) throws Exception {
    return assertFilteredSbomFile(content, expectedComponentCount, null);
  }

  private Bom assertFilteredSbomFile(
      final String content,
      final int expectedComponentCount,
      final Algorithm checksumAlgorithm)
      throws Exception
  {
    assertThat(content).isNotNull();
    Bom bom = SbomTestHelper.parseToCycloneDxBom(content);
    assertThat(bom).isNotNull();
    assertThat(bom.getComponents()).hasSize(expectedComponentCount);

    for (Component component : bom.getComponents()) {
      assertThat(component.getComponents()).isNull();
      assertThat(component.getName()).isNotNull();
      assertThat(component.getType()).isNotNull();
      assertThat(component).satisfiesAnyOf(
          c -> assertThat(c.getVersion()).isNotNull(),
          c -> assertThat(c.getProperties()).hasSize(2)
      );

      if (checksumAlgorithm != null) {
        assertThat(component.getHashes()).isNotNull();
        assertThat(component.getHashes().get(0).getAlgorithm()).isEqualTo(checksumAlgorithm.getSpec());
      }
      else {
        assertThat(component.getHashes()).isNull();
      }
      assertThat(component.getProperties().stream().filter(p -> p.getName().equals(PROPERTY_COMPONENT_REF))
          .findFirst()).isNotEmpty();
    }
    return bom;
  }

  private void assertDebugLogOutput(final String message) {
    assertThat(logOutput.getDebugMessages(loggerName)).contains(message);
  }

  private void assertComponentRef(String thirdPartyFileId) {
    List<ThirdPartyFileCoordinate> thirdPartyFileCoordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFileId);
    assertThat(thirdPartyFileCoordinates).isNotEmpty()
        .allSatisfy(tpfc -> assertThat(tpfc.getComponentRef()).isNotBlank());
  }
}
