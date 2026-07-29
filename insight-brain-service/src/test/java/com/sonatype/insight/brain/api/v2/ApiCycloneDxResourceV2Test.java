/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.collections4.CollectionUtils;
import org.cyclonedx.parsers.BomParserFactory;
import org.cyclonedx.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.parsers.Parser;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2.IQ_APP_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiCycloneDxResourceV2Test
    extends AbstractResourceTest
{
  private String scanId;

  Application app;

  @Before
  public void setUp() {
    scanId = TemporaryEntity.uuid();
    app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CYCLONE_DX_RESOURCE_PATH);
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    createReportFile(appId, scanId, "/" + getClass().getSimpleName() + "/report");
  }

  @Test
  public void testGetLatest() throws Exception {
    HttpResponse response = getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH).get();
    assertValidResponse(response, Version.VERSION_11, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetLatest_With_Version_1_1_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_11,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_11, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetLatest_With_Version_1_2_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_12,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_12, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetLatest_With_Version_1_3_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_13,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_13, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetLatest_With_Version_14_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_14,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_14, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetLatest_With_Version_15_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_15,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_15, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetLatest_With_Version_16_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_16,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_16, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetLatest_With_Version_11_Json() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_11,
            MediaType.APPLICATION_JSON).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetLatest_With_Version_12_Json() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_12,
            MediaType.APPLICATION_JSON).get();
    assertValidResponse(response, Version.VERSION_12, MediaType.APPLICATION_JSON);
  }

  @Test
  public void testGetLatest_With_Version_13_Json() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_13,
            MediaType.APPLICATION_JSON).get();
    assertValidResponse(response, Version.VERSION_13, MediaType.APPLICATION_JSON);
  }

  @Test
  public void testGetLatest_With_Version_14_Json() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_14,
            MediaType.APPLICATION_JSON).get();
    assertValidResponse(response, Version.VERSION_14, MediaType.APPLICATION_JSON);
  }

  @Test
  public void testGetLatest_With_Version_15_Json() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_15,
            MediaType.APPLICATION_JSON).get();
    assertValidResponse(response, Version.VERSION_15, MediaType.APPLICATION_JSON);
  }

  @Test
  public void testGetLatest_With_Version_16_Json() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_16,
            MediaType.APPLICATION_JSON).get();
    assertValidResponse(response, Version.VERSION_16, MediaType.APPLICATION_JSON);
  }

  @Test
  public void testGetByReportId() throws Exception {
    HttpResponse response = getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH).get();
    assertValidResponse(response, Version.VERSION_11, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetByReportId_With_Version_1_1_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_11,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_11, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetByReportId_With_Version_1_2_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_12,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_12, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetByReportId_With_Version_1_3_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_13,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_13, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetByReportId_With_Version_1_4_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_14,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_14, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetByReportId_With_Version_1_5_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_15,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_15, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetByReportId_With_Version_1_6_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_16,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_16, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetByReportId_Maven_With_Version_1_2_Xml() throws Exception {
    String sourceReportDir = "/" + getClass().getSimpleName() + "-mavenComponent/report";
    HttpResponse response =
        getHttpRequestByReportId(
            ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION,
            Version.VERSION_12,
            MediaType.APPLICATION_XML,
            sourceReportDir).get();
    assertValidMavenResponse(response);
  }

  @Test
  public void testGetByReportId_Maven_With_Version_1_2_Json() throws Exception {
    String sourceReportDir = "/" + getClass().getSimpleName() + "-mavenComponent/report";
    HttpResponse response =
        getHttpRequestByReportId(
            ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION,
            Version.VERSION_12,
            MediaType.APPLICATION_JSON,
            sourceReportDir).get();
    assertValidMavenResponse(response);
  }

  @Test
  public void testGetByReportId_Empty_Xml() throws Exception {
    String sourceReportDir = "/" + getClass().getSimpleName() + "-empty/report";
    HttpResponse response =
        getHttpRequestByReportId(
            ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION,
            Version.VERSION_14,
            MediaType.APPLICATION_XML,
            sourceReportDir).get();
    assertValidEmptyResponse(response, "xml");
  }

  @Test
  public void testGetByReportId_Empty_Json() throws Exception {
    String sourceReportDir = "/" + getClass().getSimpleName() + "-empty/report";
    HttpResponse response =
        getHttpRequestByReportId(
            ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION,
            Version.VERSION_14,
            MediaType.APPLICATION_JSON,
            sourceReportDir).get();
    assertValidEmptyResponse(response, "json");
  }

  @Test
  public void testGetByReportId_With_Version_11() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_11,
            MediaType.APPLICATION_JSON).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_With_Version_12_Json() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_12,
            MediaType.APPLICATION_JSON).get();
    assertValidResponse(response, Version.VERSION_12, MediaType.APPLICATION_JSON);
  }

  @Test
  public void testGetByReportId_With_Version_13_Json() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_13,
            MediaType.APPLICATION_JSON).get();
    assertValidResponse(response, Version.VERSION_13, MediaType.APPLICATION_JSON);
  }

  @Test
  public void testGetByReportId_With_Version_14_Json() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_14,
            MediaType.APPLICATION_JSON).get();
    assertValidResponse(response, Version.VERSION_14, MediaType.APPLICATION_JSON);
  }

  @Test
  public void testGetByReportId_With_Version_15_Json() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_15,
            MediaType.APPLICATION_JSON).get();
    assertValidResponse(response, Version.VERSION_15, MediaType.APPLICATION_JSON);
  }

  @Test
  public void testGetByReportId_With_Version_16_Json() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_16,
            MediaType.APPLICATION_JSON).get();
    assertValidResponse(response, Version.VERSION_16, MediaType.APPLICATION_JSON);
  }

  @Test
  public void testGetLatest_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, null, MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetLatest_With_Version_1_1_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_11,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetLatest_With_Version_1_2_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_12,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetLatest_With_Version_1_3_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_13,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetLatest_With_Version_1_4_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_14,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetLatest_With_Version_1_5_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_15,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetLatest_With_Version_1_6_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_16,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, null, MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_With_Version_1_1_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_11,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_With_Version_1_2_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_12,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_With_Version_1_3_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_13,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_With_Version_1_4_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_14,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_With_Version_1_5_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_15,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_With_Version_1_6_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION, Version.VERSION_16,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_With_Invalid_Version() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId("1.8/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_12,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void testGetLatest_With_Invalid_Version() throws Exception {
    HttpResponse response =
        getHttpRequestLatest("1.8/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_12,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  private void assertValidResponse(HttpResponse response, Version version, String contentType) throws ParseException {
    assertResponseStatus(200, response);
    byte[] bytes = response.getBodyText().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);
    assertThat(bom.getSpecVersion()).isEqualTo(version.getVersionString());
    assertThat(bom.getComponents()).hasSize(3);
    assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
    assertFileName(response);
  }

  private void assertFileName(HttpResponse response) {
    String fileName =
        response.getHeader(HttpHeaders.CONTENT_DISPOSITION).replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
    assertThat(fileName.matches("^(?i)(?:[a-zA-Z0-9][a-zA-Z0-9_.-]+-)?bom\\.(?:xml|json)$")).isTrue();
  }

  private void assertValidEmptyResponse(
      HttpResponse response,
      String format) throws URISyntaxException, IOException, ParseException
  {
    assertResponseStatus(200, response);
    byte[] actualBytes = response.getBodyText().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(actualBytes);
    Bom actualBom = parser.parse(actualBytes);
    byte[] expectedBytes =
        Files.readAllBytes(Paths.get(getClass().getResource(
            "/" + getClass().getSimpleName() + "-empty/sbom/sbom." + format).toURI()));
    parser = BomParserFactory.createParser(expectedBytes);
    Bom expectedBom = parser.parse(expectedBytes);
    assertThat(actualBom).usingRecursiveComparison()
        .ignoringFields("externalReferences", "serialNumber", "metadata.timestamp", "metadata.tools.version",
            "metadata.component", "metadata.properties.value")
        .isEqualTo(expectedBom);
    Component rootComponent = actualBom.getMetadata().getComponent();
    assertThat(rootComponent).satisfies(component -> {
      assertThat(component.getName()).isEqualTo(IQ_APP_PREFIX + app.getName());
      assertThat(component.getVersion()).isEqualTo(scanId);
      assertThat(component.getPurl()).isEqualTo(
          String.format("pkg:generic/sonatype/iq_application_%s@%s", app.getName(), scanId));
    });
  }

  private void assertValidMavenResponse(HttpResponse response) throws ParseException {
    assertResponseStatus(200, response);
    byte[] actualBytes = response.getBodyText().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(actualBytes);
    Bom bom = parser.parse(actualBytes);
    Component rootComponent = bom.getMetadata().getComponent();
    assertBomComponent(rootComponent,
        "pkg:maven/com.sonatype.insight.scan/insight-scanner@2.36.19-SNAPSHOT?type=pom");
    assertThat(bom.getComponents()).hasSize(4);
    assertBomComponent(bom, "pkg:maven/com.sonatype.insight.scan/insight-test-networking@2.36.19-SNAPSHOT?type=jar");
    assertBomComponent(bom, "pkg:maven/com.sonatype.insight.scan/insight-test-reverse-proxy@2.36.19-SNAPSHOT?type=jar");
    assertBomComponent(bom, "pkg:maven/org.slf4j/jcl-over-slf4j@1.7.36?type=jar");
    assertBomComponent(bom, "pkg:maven/org.slf4j/slf4j-api@1.7.36?type=jar");

    assertThat(bom.getDependencies()).hasSize(5);
    Dependency root = bom.getDependencies().get(0);
    assertThat(root.getRef()).isEqualTo(rootComponent.getBomRef());
    assertThat(root.getDependencies()).hasSize(2)
        .extracting("ref")
        .containsExactlyInAnyOrder(
            bomRefOf(bom, "pkg:maven/com.sonatype.insight.scan/insight-test-networking@2.36.19-SNAPSHOT?type=jar"),
            bomRefOf(bom, "pkg:maven/com.sonatype.insight.scan/insight-test-reverse-proxy@2.36.19-SNAPSHOT?type=jar"));

    Dependency d1 = bom.getDependencies().get(1);
    assertThat(d1.getRef()).isEqualTo(bomRefOf(bom,
        "pkg:maven/com.sonatype.insight.scan/insight-test-networking@2.36.19-SNAPSHOT?type=jar"));
    assertThat(CollectionUtils.isEmpty(d1.getDependencies())).isTrue();

    Dependency d2 = bom.getDependencies().get(2);
    assertThat(d2.getRef()).isEqualTo(bomRefOf(bom,
        "pkg:maven/com.sonatype.insight.scan/insight-test-reverse-proxy@2.36.19-SNAPSHOT?type=jar"));
    assertThat(d2.getDependencies()).hasSize(2);
    assertThat(d2.getDependencies()).extracting("ref")
        .containsExactlyInAnyOrder(
            bomRefOf(bom, "pkg:maven/org.slf4j/jcl-over-slf4j@1.7.36?type=jar"),
            bomRefOf(bom, "pkg:maven/org.slf4j/slf4j-api@1.7.36?type=jar"));

    Dependency d3 = bom.getDependencies().get(3);
    assertThat(d3.getRef()).isEqualTo(bomRefOf(bom, "pkg:maven/org.slf4j/jcl-over-slf4j@1.7.36?type=jar"));
    assertThat(CollectionUtils.isEmpty(d3.getDependencies())).isTrue();

    Dependency d4 = bom.getDependencies().get(4);
    assertThat(d4.getRef()).isEqualTo(bomRefOf(bom, "pkg:maven/org.slf4j/slf4j-api@1.7.36?type=jar"));
    assertThat(CollectionUtils.isEmpty(d4.getDependencies())).isTrue();
  }

  private String bomRefOf(final Bom bom, final String purl) {
    return bom.getComponents()
        .stream()
        .filter(c -> c.getPurl().equals(purl))
        .findFirst()
        .map(Component::getBomRef)
        .orElse(null);
  }

  private void assertBomComponent(final Bom bom, final String purl) {
    Component component = bom.getComponents().stream().filter(c -> c.getPurl().equals(purl)).findFirst().orElse(null);
    assertThat(component).isNotNull();
    assertBomComponent(component, purl);
  }

  private void assertBomComponent(Component component, String purlString) {
    PackageUrlIdentifier purl = new PackageUrlIdentifier(purlString);
    assertThat(component.getPurl()).isEqualTo(purlString);
    assertThat(component.getName()).isEqualTo(purl.getName());
    assertThat(component.getGroup()).isEqualTo(purl.getNamespace());
    assertThat(component.getVersion()).isEqualTo(purl.getVersion());
  }

  private HttpRequest getHttpRequest(
      final String path,
      final String mediaType,
      final String... sourceReportDir) throws IOException
  {
    if (sourceReportDir.length > 0) {
      createReportFile(app.getId(), scanId, sourceReportDir[0]);
    }
    else {
      createReportFile(app.getId(), scanId);
    }

    HttpRequest request = restRequest().path(path);
    if (mediaType != null) {
      request.header("Accept", mediaType);
    }
    return request;
  }

  private HttpRequest getHttpRequestLatest(String path) throws Exception {
    return getHttpRequestLatest(path, null, null);
  }

  private HttpRequest getHttpRequestLatest(String path, Version version, String mediaType) throws Exception {
    HttpRequest request = getHttpRequest(path, mediaType);

    if (version != null) {
      request.parameter(version.getVersionString(), app.getId(), Stage.ID_BUILD);
    }
    else {
      request.parameter(app.getId(), Stage.ID_BUILD);
    }
    return request;
  }

  private HttpRequest getHttpRequestByReportId(String path) throws Exception {
    return getHttpRequestByReportId(path, null, null);
  }

  private HttpRequest getHttpRequestByReportId(
      String path,
      Version version,
      String mediaType,
      String... sourceReportDir) throws Exception
  {
    HttpRequest request = getHttpRequest(path, mediaType, sourceReportDir);

    if (version != null) {
      request.parameter(version.getVersionString(), app.getId(), scanId);
    }
    else {
      request.parameter(app.getId(), scanId);
    }
    return request;
  }
}
