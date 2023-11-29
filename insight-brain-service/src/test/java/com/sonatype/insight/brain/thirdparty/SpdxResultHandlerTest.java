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
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.file.ThirdPartyUtils.SbomFormat;
import com.sonatype.insight.scan.model.ProjectScanItem;
import com.sonatype.insight.test.LogOutput;

import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Hash.Algorithm;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Swid;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.parsers.XmlParser;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;
import org.spdx.library.model.SpdxDocument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SpdxResultHandlerTest
    extends AbstractComponentTest
{
  @Spy
  private SpdxResultHandler spdxResultHandler;

  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Spy
  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Spy
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final String loggerName = SpdxResultHandler.class.getName();

  @Rule
  public LogOutput logOutput = new LogOutput(loggerName);

  @Rule
  public LogOutput logOutputUtils = new LogOutput(ThirdPartyUtils.class.getName());

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
            "pkg:library/org.apache.logging.log4j/log4j-api@2.13.2",
            null);
    assertThat(components).extracting("properties.size")
        .containsOnly(null, null, null, 1);
    // 1 component hash was collected
    assertThat(components.get(3).getProperties())
        .flatExtracting(Property::getValue)
        .contains("9188560f22e0b73070d2");
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
            "pkg:library/org.apache.logging.log4j/log4j-api@2.13.2",
            "pkg:cpe/apache/log4j@2.11.2?update=rc3",
            null);
    assertThat(components).extracting("properties.size")
        .containsOnly(null, null, null, null, 1);
    // 1 component hash was collected
    assertThat(components.get(4).getProperties())
        .flatExtracting(Property::getValue)
        .contains("9188560f22e0b73070d2");
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
  public void testHandleAndFilterContents_invalid_Xml() throws Exception {
    testHandleAndFilterContents_invalid(getSbomXmlFile("spdx-invalid.xml"),"spdx-invalid.xml");
  }

  @Test
  public void testHandleAndFilterContents_invalid_Json() throws Exception {
    testHandleAndFilterContents_invalid(getSbomJsonFile("spdx-invalid.json"), "spdx-invalid.json");
  }

  private void testHandleAndFilterContents_invalid(String sbomContent, String path) {
    ThirdPartyScanContent content = new ThirdPartyScanContent(path, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> spdxResultHandler.handleAndFilterContents(content, thirdPartyFile))
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
            "pkg:application/sonatype/iq_application_SCM%20Test%201@76b10b862e7b42009f2415097620928c",
            "pkg:maven/org.hamcrest/hamcrest-core@1.3?type=jar",
            "pkg:maven/org.apache.logging.log4j/log4j-api@2.13.2?type=jar",
            "pkg:maven/org.yaml/snakeyaml@1.29?type=jar");
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
            "pkg:application/sonatype/iq_application_SCM%20Test%201@76b10b862e7b42009f2415097620928c",
            "pkg:maven/org.hamcrest/hamcrest-core@1.3?type=jar",
            "pkg:maven/org.apache.logging.log4j/log4j-api@2.13.2?type=jar",
            "pkg:maven/org.yaml/snakeyaml@1.29?type=jar");
  }

  @Test
  public void testHandleAndFilterContents_Json_Licenses() throws Exception {
    String sbomContent = getSbomJsonFile("spdx-licenses.json");
    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-licenses.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    FilteredThirdPartyContent filteredContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    Bom bom = assertFilteredSbomFile(sbomXml, 9);
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo(
        "pkg:maven/com.sonatype.testing/pr-comment-02@1.0-SNAPSHOT?type=jar");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(9);
    List<String> coordinateIds = coordinates.stream().map(ThirdPartyFileCoordinate::getId).collect(Collectors.toList());

    try (TransactionContext tx = thirdPartyCoordinateLicenseDAO.createTransactionContext()) {
      List<ThirdPartyCoordinateLicense> coordinatesLicenses = new ArrayList<>();
      for (String coordinateId : coordinateIds) {
        List<ThirdPartyCoordinateLicense> licenses =
            thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tx, coordinateId);
        coordinatesLicenses.addAll(licenses);
      }
      Set<String> licenseIdSet =
          coordinatesLicenses.stream().map(ThirdPartyCoordinateLicense::getLicenseId).collect(Collectors.toSet());
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
          "LicenseRef-COMMERCIAL",
          "LicenseRef-PUBLIC-DOMAIN"
      );
    }
  }

  @Test
  public void testHandleAndFilterContents_Json_Vulnerabilities() throws Exception {
    String sbomContent = getSbomJsonFile("spdx-v2_3.json");
    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-v2_3.json", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    FilteredThirdPartyContent filteredContent =
        spdxResultHandler.handleAndFilterContents(content, thirdPartyFile);
    String sbomXml = filteredContent.getContent();
    Bom bom = assertFilteredSbomFile(sbomXml, 6);
    assertThat(bom.getMetadata().getComponent().getPurl()).isEqualTo("pkg:generic/sonatype/iq_application_SCM_Test");

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(6);
    List<String> coordinateIds = coordinates.stream().map(ThirdPartyFileCoordinate::getId).collect(Collectors.toList());

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
    }
  }

  @Test
  public void testHandleAndFilterContents_withSha256() throws Exception {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);

    String sbomContent = getSbomXmlFile("spdx-sha256.xml");
    ThirdPartyScanContent content = new ThirdPartyScanContent("spdx-sha256.xml", null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertFilteredSbomFile(filteredContent, 3, Algorithm.SHA_256);

    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(false);
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
  public void testDetermineIdentificationSource() {
    assertThat(spdxResultHandler.determineIdentificationSource("abcd.spdx.xml")).isEqualTo("abcd");
    assertThat(spdxResultHandler.determineIdentificationSource("ABCD123.SPDX.XmL")).isEqualTo("ABCD123");
    assertThat(spdxResultHandler.determineIdentificationSource("sub/dir/abcd.spdx.xml")).isEqualTo("abcd");
    assertThat(spdxResultHandler.determineIdentificationSource("ABCD-SPDX.json")).isEqualTo("Third-Party");
    assertThat(spdxResultHandler.determineIdentificationSource("abcdspdx.xml")).isEqualTo("Third-Party");
    assertThat(spdxResultHandler.determineIdentificationSource("")).isEqualTo("Third-Party");
    assertThat(spdxResultHandler.determineIdentificationSource(null)).isEqualTo("Third-Party");
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

  private void assertCpeAndSwid(ThirdPartyScanContent content) throws Exception {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    String cpeValue = "cpe:2.3:a:pivotal_software:spring_framework:4.1.0:*:*:*:*:*:*:*";
    String swidTagId = "swid:gen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1";

    String filteredContent = spdxResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    Bom bom = assertFilteredSbomFile(filteredContent, 2);
    Component component = bom.getComponents().get(0);
    assertThat(component.getBomRef()).isEqualTo("SPDXRef-maven-org.apache.logging.log4j-log4j-core-2.13.2");
    assertThat(component.getCpe()).isEqualTo(cpeValue);
    Swid swid = component.getSwid();
    assertThat(swid.getTagId()).isEqualTo(swidTagId);
    assertThat(swid.getName()).isNull();
    assertThat(swid.getVersion()).isNull();
    assertThat(swid.getTagVersion()).isEqualTo(0);
    assertThat(swid.isPatch()).isFalse();
    assertThat(swid.getAttachmentText()).isNull();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(2);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate = coordinates.get(0);
    assertThat(thirdPartyFileCoordinate.getCpe()).isEqualTo(cpeValue);
    assertThat(thirdPartyFileCoordinate.getSwid())
        .isEqualTo("{" +
            "\"tagId\":\"" + swidTagId + "\"," +
            "\"tagVersion\":0," +
            "\"patch\":false}");
  }

  private String getSbomFile(final String fileType, final String fileName) throws Exception {
    URL resource = getClass().getResource("/SpdxResultHandlerTest/" + fileType + "/" + fileName);
    return new String(Files.readAllBytes(Paths.get(resource.toURI())), StandardCharsets.UTF_8);
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
    Bom bom = getBom(content);
    assertThat(bom).isNotNull();
    assertThat(bom.getComponents()).hasSize(expectedComponentCount);

    for (Component component : bom.getComponents()) {
      assertThat(component.getComponents()).isNull();
      assertThat(component.getName()).isNotNull();
      assertThat(component.getType()).isNotNull();
      assertThat(component).satisfiesAnyOf(
          c -> assertThat(c.getVersion()).isNotNull(),
          c -> assertThat(c.getProperties()).hasSize(1)
      );

      if (checksumAlgorithm != null) {
        assertThat(component.getHashes()).isNotNull();
        assertThat(component.getHashes().get(0).getAlgorithm()).isEqualTo(checksumAlgorithm.getSpec());
      }
      else {
        assertThat(component.getHashes()).isNull();
      }
    }
    return bom;
  }

  private Bom getBom(String content) throws ParseException {
    Parser parser = new XmlParser();
    return parser.parse(new StringReader(content));
  }
}
