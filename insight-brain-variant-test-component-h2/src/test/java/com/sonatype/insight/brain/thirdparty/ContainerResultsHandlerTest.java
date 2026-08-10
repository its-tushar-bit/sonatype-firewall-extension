/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.sbom.SbomComponentInfoTelemetry;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.SbomTestHelper;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

@ComponentH2Test
public class ContainerResultsHandlerTest
    extends AbstractComponentH2Test
{
  @Inject
  private ThirdPartyFileDAO thirdPartyFileDAO;

  @Inject
  private DuplicateAwareThirdPartyFileCoordinatePersister fileCoordinatePersister;

  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

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

  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  @Inject
  private TestProductLicense testProductLicense;

  private ContainerResultHandler containerResultHandler;

  private ContainerResultHandler proxyContainerResultHandler;

  private String loadResource(String name) throws Exception {
    URL resource = getClass().getResource("/ContainerResultsHandlerTest/" + name);
    return new String(Files.readAllBytes(Paths.get(resource.toURI())), StandardCharsets.UTF_8);
  }

  @BeforeEach
  public void before() {
    containerResultHandler =
        new ContainerResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, null, null);

    ThirdPartyScanContext scanContext = new ThirdPartyScanContext("scan-request-id",
        "app-id", null, null, "proxy");
    scanContext.setContainerItemContentType(ItemContentType.CONTAINER_URI_SONATYPE);
    proxyContainerResultHandler =
        new ContainerResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, scanContext, testProductLicense);
  }

  @Test
  public void testHandleAndFilterContents_FilteredProxy() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    String json = loadResource("alpine-3.6-proxy.json");

    ThirdPartyScanContent content =
        new ThirdPartyScanContent(
            "container:alpine:3.6", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String actualFilteredContent = proxyContainerResultHandler.handleAndFilterContents(
        content, thirdPartyFile).getContent();
    String expectedFiltered = loadResource("alpine-3.6-expected-bom-proxy.json");

    assertThatJson(actualFilteredContent).whenIgnoringPaths("components[*].properties[*].value",
        "components[*].bom-ref")
        .isEqualTo(expectedFiltered);

    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId()))
        .isNotEmpty()
        .allSatisfy(cp -> assertThat(cp.getComponentRef()).isNotBlank());
    Bom bom = SbomTestHelper.parseToCycloneDxBom(actualFilteredContent);
    assertThat(bom.getComponents()).isNotEmpty()
        .allSatisfy(component -> assertThat(component.getProperties()
            .stream()
            .filter(p -> SbomCycloneDxUtils.PROPERTY_COMPONENT_REF.equals(p.getName()))
            .findFirst()).isNotEmpty()
                .satisfies(optional -> assertThat(optional.get().getValue()).isNotBlank()));
  }

  @Test
  public void testHandleAndFilterContents_FilteredProxy_EvalNotEnabled() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    testProductLicense.setMissingFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);

    String json = loadResource("alpine-3.6-proxy.json");

    ThirdPartyScanContent content =
        new ThirdPartyScanContent(
            "container:alpine:3.6", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String actualFilteredContent = proxyContainerResultHandler.handleAndFilterContents(
        content, thirdPartyFile).getContent();
    String expectedFiltered = loadResource("alpine-3.6-expected-bom-generic.json");

    assertThatJson(actualFilteredContent).whenIgnoringPaths("components[*].properties[*].value",
        "components[*].bom-ref")
        .isEqualTo(expectedFiltered);

    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId()))
        .isNotEmpty()
        .allSatisfy(cp -> assertThat(cp.getComponentRef()).isNotBlank());
    Bom bom = SbomTestHelper.parseToCycloneDxBom(actualFilteredContent);
    assertThat(bom.getComponents()).isNotEmpty()
        .allSatisfy(component -> assertThat(component.getProperties()
            .stream()
            .filter(p -> SbomCycloneDxUtils.PROPERTY_COMPONENT_REF.equals(p.getName()))
            .findFirst()).isNotEmpty()
                .satisfies(optional -> assertThat(optional.get().getValue()).isNotBlank()));
  }

  @Test
  public void testHandleAndFilterContents_FilteredProxy_WithoutContainerEvalLicense() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    testProductLicense.setMissingFeatures(LicensedFeature.CONTAINER_IMAGES_EVALUATION);

    String json = loadResource("alpine-3.6-proxy.json");

    ThirdPartyScanContent content =
        new ThirdPartyScanContent(
            "container:alpine:3.6", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String actualFilteredContent = proxyContainerResultHandler.handleAndFilterContents(
        content, thirdPartyFile).getContent();
    String expectedFiltered = loadResource("alpine-3.6-expected-bom-generic.json");

    assertThatJson(actualFilteredContent).whenIgnoringPaths("components[*].properties[*].value",
        "components[*].bom-ref")
        .isEqualTo(expectedFiltered);

    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId()))
        .isNotEmpty()
        .allSatisfy(cp -> assertThat(cp.getComponentRef()).isNotBlank());
    Bom bom = SbomTestHelper.parseToCycloneDxBom(actualFilteredContent);
    assertThat(bom.getComponents()).isNotEmpty()
        .allSatisfy(component -> assertThat(component.getProperties()
            .stream()
            .filter(p -> SbomCycloneDxUtils.PROPERTY_COMPONENT_REF.equals(p.getName()))
            .findFirst()).isNotEmpty()
                .satisfies(optional -> assertThat(optional.get().getValue()).isNotBlank()));
  }

  @Test
  public void testHandleAndFilterContents_noModules() throws Exception {
    String json = loadResource("container-image-no-modules.json");

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("container:hello-world:latest", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String actualFilteredContent = containerResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(actualFilteredContent).isNotNull();

    Bom bom = SbomTestHelper.parseToCycloneDxBom(actualFilteredContent);

    assertThat(bom).isNotNull();
    assertThat(bom.getComponents()).isNullOrEmpty();
  }

  @Test
  public void testHandleAndFilterContents_Filtered() throws Exception {
    String json = loadResource("alpine-3.6.json");

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("container:alpine:3.6", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String actualFilteredContent = containerResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    String expectedFiltered = loadResource("alpine-3.6-expected-bom.json");

    assertThatJson(actualFilteredContent).whenIgnoringPaths("components[*].properties[*].value",
        "components[*].bom-ref")
        .isEqualTo(expectedFiltered);

    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId()))
        .isNotEmpty()
        .allSatisfy(cp -> assertThat(cp.getComponentRef()).isNotBlank());
    Bom bom = SbomTestHelper.parseToCycloneDxBom(actualFilteredContent);
    assertThat(bom.getComponents()).isNotEmpty()
        .allSatisfy(component -> assertThat(component.getProperties()
            .stream()
            .filter(p -> SbomCycloneDxUtils.PROPERTY_COMPONENT_REF.equals(p.getName()))
            .findFirst()).isNotEmpty()
                .satisfies(optional -> assertThat(optional.get().getValue()).isNotBlank()));
  }

  @Test
  public void testHandleAndFilterContents_TelemetryData() throws Exception {
    String json = loadResource("alpine-3.6.json");

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("container:alpine:3.6", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    containerResultHandler.handleAndFilterContents(content, thirdPartyFile);

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
    assertThat(componentInfoTelemetry.getSpecVersion()).isNull();
    assertThat(componentInfoTelemetry.getCpeCount()).isZero();
    assertThat(componentInfoTelemetry.getCoordinateCount()).isEqualTo(9);
  }

  @Test
  public void testHandleAndFilterContents_DuplicateAffects() throws Exception {
    // This resource is the scan file generated by scanning the image created by the following dockerfile with Neuvector
    // webgoat.jar can be downloaded from https://github.com/WebGoat/WebGoat/releases
    // FROM alpine:3.14
    // COPY ./webgoat.jar ./webgoat1.jar
    // COPY ./webgoat.jar ./webgoat2.jar

    String json = loadResource("webgoat.json");

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("container:webgoat", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    containerResultHandler.handleAndFilterContents(content, thirdPartyFile);

    List<ThirdPartyCoordinateSecurity> coordinateSecurityList = thirdPartyCoordinateSecurityDAO.getAll();
    assertThat(coordinateSecurityList).hasSize(50);
  }

  @Test
  public void testHandleAndFilterContents() throws Exception {
    String json = loadResource("alpine-3.6.json");

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("container:alpine:3.6", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = containerResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isNotNull();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(9);

    for (ThirdPartyFileCoordinate coord : coordinates) {
      assertThat(coord.getSource()).isEqualTo("Sonatype-Container");
      assertThat(coord.getFormat()).isEqualTo("container");
    }

    List<ThirdPartyCoordinateSecurity> coordinateSecurityList = thirdPartyCoordinateSecurityDAO.getAll();
    assertThat(coordinateSecurityList).hasSize(8);

    // New list for sorting
    coordinateSecurityList = new ArrayList<>(coordinateSecurityList);
    coordinateSecurityList.sort(Comparator.comparing(ThirdPartyCoordinateSecurity::getRefId));

    String source = "Sonatype-Container";

    Iterator<ThirdPartyCoordinateSecurity> iterator = coordinateSecurityList.iterator();
    assertCoordinateSecurity(iterator.next(), "CVE-2017-15874", "archival/libarchive/decompress_unlzma.c in BusyBox",
        "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-15874", 5.5d, source, "medium",
        "CVSS:3.0/AV:L/AC:L/PR:N/UI:R/S:U/C:N/I:N/A:H", "1.27.2-r4", "1.27.2-r4");

    assertCoordinateSecurity(iterator.next(), "CVE-2018-1000500", "Busybox contains a Missing SSL certificate",
        "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-1000500", 8.1d, source, "high",
        "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:H", "1.28.3-r2", "1.28.3-r2");

    assertCoordinateSecurity(iterator.next(), "CVE-2018-20679", "An issue was discovered in BusyBox",
        "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-20679", 7.5d, source, "high",
        "CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N", "1.29.3-r10", "1.29.3-r10");

    assertCoordinateSecurity(iterator.next(), "CVE-2019-14697",
        "musl libc through 1.1.23 has an x87",
        "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2019-14697", 9.8d, source, "high",
        "CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H", "1.1.23-r2", "1.1.23-r2");

    assertCoordinateSecurity(iterator.next(), "CVE-2019-5747", "An issue was discovered in BusyBox",
        "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2019-5747", 7.5d, source, "high",
        "CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N", "1.30.1-r2", "1.30.1-r2");

    assertCoordinateSecurity(iterator.next(), "CVE-2020-28928",
        "In musl libc through 1.2.1",
        "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-28928", 5.5d, source, "medium",
        "CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:N/I:N/A:H", "1.2.2_pre2-r0", "1.2.2_pre2-r0");

    assertCoordinateSecurity(iterator.next(), "CVE-2021-28831", "decompress_gunzip.c in BusyBox through 1.32.1",
        "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-28831", 7.5d, source, "high",
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "1.33.0-r5", "1.33.0-r5");

    assertCoordinateSecurity(iterator.next(), "CVE-2021-30139", "In Alpine Linux apk-tools",
        "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-30139", 7.5d, source, "high",
        "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H", "2.12.5-r0", "2.12.5-r0");
  }

  @Test
  public void testHandleAndFilterContents_DoesNotIncludeCoordinatesForMissingCveInformation() throws Exception {
    String json = loadResource("alpine-3.6-missing-cve.json");

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("container:alpine:3.6", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = containerResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();
    assertThat(filteredContent).isNotNull();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(9);

    List<ThirdPartyCoordinateSecurity> coordinateSecurityList = thirdPartyCoordinateSecurityDAO.getAll();
    assertThat(coordinateSecurityList).hasSize(7);

    String cveThatIsMissingInformation = "CVE-2021-30139";

    List<ThirdPartyCoordinateSecurity> list =
        coordinateSecurityList.stream()
            .filter(p -> p.getRefId().equals(cveThatIsMissingInformation))
            .collect(Collectors.toList());
    assertThat(list).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_MalformedUrlVulnerability() throws Exception {
    String json = loadResource("malformed-url-vulnerability.json");

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("container:test:latest", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    assertThatCode(() -> containerResultHandler.handleAndFilterContents(content, thirdPartyFile))
        .doesNotThrowAnyException();
  }

  @Test
  public void testParseBom_ContainerScannerCycloneDx_NullContent() {
    ThirdPartyScanContext scanContext = new ThirdPartyScanContext("scan-request-id",
        "app-id", null, null, ProxyStageType.ID);
    scanContext.setContainerImageSbomSpecification(SbomSpecification.CYCLONEDX);

    ContainerResultHandler firewallContainerResultHandler =
        new ContainerResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, scanContext, testProductLicense);

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("image:tag", ItemContentType.CONTAINER_URI_SONATYPE, null, null, null);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> firewallContainerResultHandler.parseBom(content))
        .withMessage("Empty content for container image");
  }

  @Test
  public void testParseBom_ContainerScannerCycloneDx_InvalidContent() throws Exception {
    String invalidJson = "invalid json";

    ThirdPartyScanContext scanContext = new ThirdPartyScanContext("scan-request-id",
        "app-id", null, null, ProxyStageType.ID);
    scanContext.setContainerImageSbomSpecification(SbomSpecification.CYCLONEDX);

    ContainerResultHandler firewallContainerResultHandler =
        new ContainerResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, scanContext, testProductLicense);

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("image:tag", ItemContentType.CONTAINER_URI_SONATYPE, null, null, invalidJson);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> firewallContainerResultHandler.parseBom(content))
        .withMessage("Invalid content for container image");
  }

  @Test
  public void testParseBom_ContainerScannerCycloneDx() throws Exception {
    String json = loadResource("container-scanner-cyclonedx.json");

    ThirdPartyScanContext scanContext = new ThirdPartyScanContext("scan-request-id",
        "app-id", null, null, ProxyStageType.ID);
    scanContext.setContainerImageSbomSpecification(SbomSpecification.CYCLONEDX);

    ContainerResultHandler firewallContainerResultHandler =
        new ContainerResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, scanContext, testProductLicense);

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("image:tag", ItemContentType.CONTAINER_URI_SONATYPE, null, null, json);

    Pair<Bom, Boolean> result = firewallContainerResultHandler.parseBom(content);

    assertThat(result).isNotNull();
    assertThat(result.getRight()).isTrue();
    assertThat(result.getLeft()).isNotNull();

    Bom bom = result.getLeft();
    assertThat(bom.getComponents()).hasSize(2);
    assertThat(bom.getVulnerabilities()).isNullOrEmpty();

    assertThat(bom.getComponents().get(0).getGroup()).isEqualTo("alpine:3.4.6");
    assertThat(bom.getComponents().get(0).getName()).isEqualTo("alpine-baselayout");
    assertThat(bom.getComponents().get(0).getVersion()).isEqualTo("3.0.3-r0");
    assertThat(bom.getComponents().get(0).getPurl()).isEqualTo(
        "pkg:generic/alpine%3A3.4.6/alpine-baselayout@3.0.3-r0?nexustype=container");

    assertThat(bom.getComponents().get(1).getGroup()).isEqualTo("alpine:3.4.6");
    assertThat(bom.getComponents().get(1).getName()).isEqualTo("zlib");
    assertThat(bom.getComponents().get(1).getVersion()).isEqualTo("1.2.11-r0");
    assertThat(bom.getComponents().get(1).getPurl()).isEqualTo(
        "pkg:generic/alpine%3A3.4.6/zlib@1.2.11-r0?nexustype=container");
  }

  private void assertCoordinateSecurity(
      final ThirdPartyCoordinateSecurity coordinateSecurity,
      final String cve,
      final String descriptionStartsWith,
      final String link,
      final double severity,
      final String source,
      final String severityDescription,
      final String vector,
      final String recommendation,
      final String fixedBy)
  {
    assertThat(coordinateSecurity.getRefId()).isEqualTo(cve);
    assertThat(coordinateSecurity.getDescription()).startsWith(descriptionStartsWith);
    assertThat(coordinateSecurity.getLink()).isEqualTo(link);
    assertThat(coordinateSecurity.getSeverity()).isEqualTo(severity);
    assertThat(coordinateSecurity.getVulnerabilitySource()).isEqualTo(source);
    assertThat(coordinateSecurity.getSeverityDescription()).isEqualTo(severityDescription);
    assertThat(coordinateSecurity.getAttackVector()).isEqualTo(vector);
    assertThat(coordinateSecurity.getRecommendations()).isEqualTo(recommendation);
    assertThat(coordinateSecurity.getFixedBy()).isEqualTo(fixedBy);
    // The stored rating method must match the CVSS version encoded in the vector — this is the
    // regression guard for the CVSS-method fix: if the method were dropped, getValidRating would
    // reject the rating and severity would silently become 0.0.
    Rating.Method expectedMethod =
        StringUtils.startsWith(vector, "CVSS:3.1") ? Rating.Method.CVSSV31 : Rating.Method.CVSSV3;
    assertThat(Rating.Method.fromString(coordinateSecurity.getRatingMethod())).isEqualTo(expectedMethod);
  }

  @Test
  public void testParseBom_extractsContentSetsFromNeuVectorFormat() {
    String json = """
        {
          "error_message": null,
          "report": {
            "modules": [],
            "ContentSets": [
              "rhel-9-for-x86_64-baseos-rpms",
              "rhel-9-for-x86_64-appstream-rpms"
            ]
          }
        }""";

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("image:tag", ItemContentType.CONTAINER_URI, null, null, json);

    Pair<Bom, Boolean> result = containerResultHandler.parseBom(content);

    Bom bom = result.getLeft();
    assertThat(bom.getMetadata()).isNotNull();
    assertThat(bom.getMetadata().getProperties()).isNotNull();
    assertThat(bom.getMetadata().getProperties())
        .filteredOn(p -> ContainerResultHandler.CONTENT_SET_PROPERTY_NAME.equals(p.getName()))
        .extracting(org.cyclonedx.model.Property::getValue)
        .containsExactlyInAnyOrder(
            "rhel-9-for-x86_64-baseos-rpms",
            "rhel-9-for-x86_64-appstream-rpms");
  }

  @Test
  public void testParseBom_noContentSetsForNonRedHatImage() {
    String json = """
        {
          "error_message": null,
          "report": {"modules": []}
        }""";

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("image:tag", ItemContentType.CONTAINER_URI, null, null, json);

    Pair<Bom, Boolean> result = containerResultHandler.parseBom(content);

    Bom bom = result.getLeft();
    assertThat(bom.getMetadata()).isNull();
  }

  @Test
  public void testParseBom_handlesErrorResponseWithoutReport() {
    String json = """
        {"error_message": "Scan failed: registry unreachable"}""";

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("image:tag", ItemContentType.CONTAINER_URI, null, null, json);

    Pair<Bom, Boolean> result = containerResultHandler.parseBom(content);

    Bom bom = result.getLeft();
    assertThat(bom).isNotNull();
    assertThat(bom.getComponents()).isEmpty();
    assertThat(bom.getVulnerabilities()).isEmpty();
  }

  @Test
  public void testHandleAndFilterContents_includesContentSetsInForwardedBom() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    String json = """
        {
          "error_message": "",
          "report": {
            "image_id": "abc",
            "repository": "rhel-image",
            "tag": "1.0",
            "modules": [
              {"name": "bash", "version": "5.0", "source": "rhel:8.6"}
            ],
            "ContentSets": [
              "rhel-9-for-x86_64-baseos-rpms",
              "rhel-9-for-x86_64-appstream-rpms"
            ]
          }
        }""";

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("container:rhel:1.0", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = proxyContainerResultHandler.handleAndFilterContents(content, thirdPartyFile).getContent();

    Bom forwardedBom = SbomTestHelper.parseToCycloneDxBom(filteredContent);
    assertThat(forwardedBom.getMetadata()).isNotNull();
    assertThat(forwardedBom.getMetadata().getProperties())
        .filteredOn(p -> ContainerResultHandler.CONTENT_SET_PROPERTY_NAME.equals(p.getName()))
        .extracting(org.cyclonedx.model.Property::getValue)
        .containsExactlyInAnyOrder(
            "rhel-9-for-x86_64-baseos-rpms",
            "rhel-9-for-x86_64-appstream-rpms");
  }

  @Test
  public void testHandleAndFilterContents_dropsNonAllowlistedMetadataProperties() throws Exception {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    String json = """
        {
          "bomFormat": "CycloneDX",
          "specVersion": "1.6",
          "version": 1,
          "metadata": {
            "component": {
              "type": "container",
              "group": "library",
              "name": "rhel-image",
              "version": "1.0",
              "purl": "pkg:generic/library/rhel-image@1.0?nexustype=container"
            },
            "properties": [
              {"name": "%s", "value": "rhel-9-for-x86_64-baseos-rpms"},
              {"name": "some.other.property", "value": "should-be-dropped"}
            ]
          },
          "components": [
            {
              "type": "file",
              "bom-ref": "bash-ref",
              "name": "bash",
              "version": "5.0",
              "purl": "pkg:generic/library/bash@5.0?nexustype=container"
            }
          ]
        }""".formatted(ContainerResultHandler.CONTENT_SET_PROPERTY_NAME);

    ThirdPartyScanContext scanContext = new ThirdPartyScanContext("scan-request-id",
        "app-id", null, null, ProxyStageType.ID);
    scanContext.setContainerItemContentType(ItemContentType.CONTAINER_URI_SONATYPE);
    scanContext.setContainerImageSbomSpecification(SbomSpecification.CYCLONEDX);

    ContainerResultHandler cycloneDxHandler =
        new ContainerResultHandler(thirdPartyFileDAO, fileCoordinatePersister, thirdPartyCoordinateSecurityDAO,
            thirdPartyCoordinateLicenseDAO, thirdPartySbomMetadataDAO, multiLicenseDAO, thirdPartyVexDAO,
            telemetryUtils, telemetrySender, scanContext, testProductLicense);

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("container:rhel:1.0", ItemContentType.CONTAINER_URI_SONATYPE, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = cycloneDxHandler.handleAndFilterContents(content, thirdPartyFile).getContent();

    Bom forwardedBom = SbomTestHelper.parseToCycloneDxBom(filteredContent);
    assertThat(forwardedBom.getMetadata().getProperties())
        .extracting(org.cyclonedx.model.Property::getName)
        .containsExactly(ContainerResultHandler.CONTENT_SET_PROPERTY_NAME)
        .doesNotContain("some.other.property");
  }
}
