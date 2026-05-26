/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSpdxResourceTest
    extends AbstractResourceTest
{
  private String scanId;

  private Application app;

  @Before
  public void setUp() {
    scanId = TemporaryEntity.uuid();
    app = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, scanId);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SPDX_RESOURCE_PATH);
  }

  private void createReportFile(String appId, String scanId) throws IOException {
    createReportFile(appId, scanId, "/ApiSpdxServiceTest/report");
  }

  @Test
  public void testGetLatestForStage() throws Exception {
    HttpResponse response = getHttpRequestLatestForStage("json", "false", "2.3").get();
    assertValidResponse(response, "json", "2.3");
  }

  @Test
  public void testGetLatestForStage_defaults() throws Exception {
    HttpResponse response = getHttpRequestLatestForStage(null, null, null).get();
    assertValidResponse(response, "json", "2.3");
  }

  @Test
  public void testGetByScanId() throws Exception {
    HttpResponse response = getHttpRequestByScanId("xml", "false", "2.3").get();
    assertValidResponse(response, "xml", "2.3");
  }

  @Test
  public void testGetByScanId_defaults() throws Exception {
    HttpResponse response = getHttpRequestByScanId(null, null, null).get();
    assertValidResponse(response, "json", "2.3");
  }

  private void assertValidResponse(HttpResponse response, String format, String spdxVersion) throws Exception {
    assertResponseStatus(200, response);

    final SpdxDocument document = deserialize(response, format);
    assertMetadata(document, spdxVersion);

    MediaType type = "json".equals(format) ? MediaType.APPLICATION_JSON_TYPE : MediaType.APPLICATION_XML_TYPE;
    assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(type.toString());

    assertFileName(response);
  }

  private void assertMetadata(SpdxDocument document, String spdxVersion) throws Exception {
    assertThat(document.getSpecVersion()).isEqualTo("SPDX-" + spdxVersion);
    assertThat(document.getCreationInfo().getCreated()).isNotNull();
    assertThat(document.getCreationInfo().getCreators().stream().findFirst().get()).contains(
        "Tool: Sonatype IQ Server -");
  }

  private void assertFileName(HttpResponse response) {
    String fileName =
        response.getHeader(HttpHeaders.CONTENT_DISPOSITION).replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
    assertThat(fileName.matches("^[a-zA-Z0-9][a-zA-Z0-9_.-]+\\.spdx\\.(?:xml|json)$")).isTrue();
  }

  private HttpRequest getHttpRequest(
      final String path,
      final String format,
      final String generateCycloneDx,
      final String spdxVersion) throws IOException
  {
    createReportFile(app.getId(), scanId);

    HttpRequest request = restRequest().path(path);
    if (format != null) {
      request.query("format", format);
    }
    if (generateCycloneDx != null) {
      request.query("generateCycloneDx", generateCycloneDx);
    }
    if (spdxVersion != null) {
      request.query("spdxVersion", spdxVersion);
    }
    return request;
  }

  private HttpRequest getHttpRequestLatestForStage(
      String format,
      String generateCycloneDx,
      String spdxVersion) throws Exception
  {
    HttpRequest request = getHttpRequest(ApiSpdxResource.GET_BY_STAGE_PATH, format, generateCycloneDx, spdxVersion);
    request.parameter(app.getId(), Stage.ID_BUILD);
    return request;
  }

  private HttpRequest getHttpRequestByScanId(
      String format,
      String generateCycloneDx,
      String spdxVersion) throws Exception
  {
    HttpRequest request = getHttpRequest(ApiSpdxResource.GET_BY_REPORT_PATH, format, generateCycloneDx, spdxVersion);
    request.parameter(app.getId(), scanId);
    return request;
  }

  private SpdxDocument deserialize(HttpResponse response, String format) throws Exception {
    IModelStore modelStore = new InMemSpdxStore();
    try (MultiFormatStore multiFormatStore =
        new MultiFormatStore(modelStore, "json".equals(format) ? Format.JSON : Format.XML, Verbose.COMPACT);
        InputStream in = response.getBodyStream())
    {
      return (SpdxDocument) multiFormatStore.deSerialize(in, true);
    }
  }
}
