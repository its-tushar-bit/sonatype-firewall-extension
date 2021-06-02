/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
    HttpResponse response = getHttpRequestLatest("1.1/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_11,
        MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_11, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetLatest_With_Version_1_2_Xml() throws Exception {
    HttpResponse response = getHttpRequestLatest("1.2/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_12,
        MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_12, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetLatest_With_Version_1_3_Xml() throws Exception {
    HttpResponse response = getHttpRequestLatest("1.3/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_13,
        MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_13, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetByReportId() throws Exception {
    HttpResponse response = getHttpRequestByReportId(ApiCycloneDxResourceV2.GET_BY_REPORT_PATH).get();
    assertValidResponse(response, Version.VERSION_11, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetByReportId_With_Version_1_1_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId("1.1/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_11,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_11, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetByReportId_With_Version_1_2_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId("1.2/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_12,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_12, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetByReportId_With_Version_1_3_Xml() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId("1.3/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_13,
            MediaType.APPLICATION_XML).get();
    assertValidResponse(response, Version.VERSION_13, MediaType.APPLICATION_XML);
  }

  @Test
  public void testGetLatest_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest(ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, null, MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetLatest_With_Version_1_1_Invalid_AcceptType() throws Exception {
    HttpResponse response = getHttpRequestLatest("1.1/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_11,
        MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetLatest_With_Version_1_2_Invalid_AcceptType() throws Exception {
    HttpResponse response = getHttpRequestLatest("1.2/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_12,
        MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetLatest_With_Version_1_3_Invalid_AcceptType() throws Exception {
    HttpResponse response = getHttpRequestLatest("1.3/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_12,
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
        getHttpRequestLatest("1.1/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_11,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_With_Version_1_2_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest("1.2/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_12,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_With_Version_1_3_Invalid_AcceptType() throws Exception {
    HttpResponse response =
        getHttpRequestLatest("1.3/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_13,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(406);
  }

  @Test
  public void testGetByReportId_With_Invalid_Version() throws Exception {
    HttpResponse response =
        getHttpRequestByReportId("1.4/" + ApiCycloneDxResourceV2.GET_BY_REPORT_PATH, Version.VERSION_12,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void testGetLatest_With_Invalid_Version() throws Exception {
    HttpResponse response =
        getHttpRequestLatest("1.4/" + ApiCycloneDxResourceV2.GET_BY_STAGE_PATH, Version.VERSION_12,
            MediaType.APPLICATION_ATOM_XML).get();
    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  private void assertValidResponse(HttpResponse response, Version version, String contentType) throws ParseException {
    assertResponseStatus(200, response);
    byte[] bytes = response.getBodyText().getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    Bom bom = parser.parse(bytes);
    assertThat(bom.getSpecVersion()).isEqualTo(version.getVersionString());
    assertThat(bom.getComponents()).hasSize(1);
    assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(contentType);
  }

  private HttpRequest getHttpRequest(final String path, final String mediaType) throws IOException {
    createReportFile(app.getId(), scanId);

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
      request.parameter(app.getId(), Stage.ID_BUILD, version.getVersionString());
    }
    else {
      request.parameter(app.getId(), Stage.ID_BUILD);
    }
    return request;
  }

  private HttpRequest getHttpRequestByReportId(String path) throws Exception {
    return getHttpRequestByReportId(path, null, null);
  }

  private HttpRequest getHttpRequestByReportId(String path, Version version, String mediaType) throws Exception {
    HttpRequest request = getHttpRequest(path, mediaType);
    if (version != null) {
      request.parameter(app.getId(), scanId, version.getVersionString());
    }
    else {
      request.parameter(app.getId(), scanId);
    }
    return request;
  }
}
