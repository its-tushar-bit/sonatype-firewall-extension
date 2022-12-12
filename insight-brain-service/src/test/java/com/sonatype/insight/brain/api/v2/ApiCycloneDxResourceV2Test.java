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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.cyclonedx.BomParserFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.Parser;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiCycloneDxResourceV2Test
    extends AbstractResourceTest
{
  private String scanId;

  Application app;

  @Before
  public void setUp() {
    scanId = tempEntity.uuid();
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
  public void testGetByReportId_Maven_With_Version_1_2_Xml() throws Exception {
    String sourceReportDir = "/" + getClass().getSimpleName() + "-mavenComponent/report";
    HttpResponse response =
        getHttpRequestByReportId(
            ApiCycloneDxResourceV2.GET_BY_REPORT_PATH_WITH_VERSION,
            Version.VERSION_12,
            MediaType.APPLICATION_XML,
            sourceReportDir).get();
    assertValidMavenResponse(response, "xml");
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
    assertValidMavenResponse(response, "json");
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
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH_WITH_VERSION, Version.VERSION_12,
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
  public void testGetByReportId_With_Invalid_Version() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId("1.5/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_12,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void testGetLatest_With_Invalid_Version() throws Exception {
    HttpResponse response =
        getHttpRequestLatest("1.5/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_12,
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
  }

  private void assertValidEmptyResponse(HttpResponse response, String format)
      throws URISyntaxException, IOException, ParseException
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
        .ignoringFieldsMatchingRegexes("(externalReferences|serialNumber|metadata.timestamp)")
        .isEqualTo(expectedBom);
  }

  private void assertValidMavenResponse(HttpResponse response, String format)
      throws URISyntaxException, IOException, ParseException
  {
    assertResponseStatus(200, response);
    byte[] actualBytes = response.getBodyText().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(actualBytes);
    Bom actualBom = parser.parse(actualBytes);
    byte[] expectedBytes =
        Files.readAllBytes(Paths.get(getClass().getResource(
            "/" + getClass().getSimpleName() + "-mavenComponent/sbom/sbom." + format).toURI()));
    parser = BomParserFactory.createParser(expectedBytes);
    Bom expectedBom = parser.parse(expectedBytes);
    assertThat(actualBom).usingRecursiveComparison()
        .ignoringFieldsMatchingRegexes("(externalReferences|serialNumber|metadata.timestamp)")
        .isEqualTo(expectedBom);
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
