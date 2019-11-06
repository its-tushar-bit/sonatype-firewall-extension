/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.75
 */
public class ApiThirdPartyResourceTest
    extends AbstractResourceTest
{
  private Organization org;

  private Application app;

  @Before
  public void setupApplication() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testEvaluateComponents_thirdPartyScan() throws Exception {
    String sbom = getSbomFile("/ApiThirdPartyResourceTest/valid_sbom.xml");

    HttpResponse response = restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyResource.SCAN_COMPONENTS)
        .parameter(app.getId(), "clair").query("stageId", "build").body(sbom, MediaType.APPLICATION_XML).post();
    assertResponseStatus(202, response);

    ApiThirdPartyScanTicketDTO evaluationResult = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(evaluationResult).isNotNull();
    assertThat(evaluationResult.statusUrl).isNotNull();
    assertThat(new URI(evaluationResult.statusUrl)).isNotNull();
  }

  @Test
  public void testEvaluateComponents_nullSbom() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyResource.SCAN_COMPONENTS)
        .parameter(app.getId(), "clair").query("stageId", "build").body(null).post();
    assertResponseStatus(400, response);
  }
  
  @Test
  public void testEvaluateComponents_invalidStage() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyResource.SCAN_COMPONENTS)
        .parameter(app.getId(), "clair").query("stageId", "invalidStage").body(null).post();
    assertResponseStatus(400, response);
  }

  @Test
  public void testEvaluateComponents_invalidSbom() throws Exception {
    String sbom = getSbomFile("/ApiThirdPartyResourceTest/invalid_sbom.xml");

    HttpResponse response = restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyResource.SCAN_COMPONENTS)
        .parameter(app.getId(), "clair").query("stageId", "build").body(sbom, MediaType.APPLICATION_XML).post();
    assertResponseStatus(400, response);

    String error = response.getBodyText();
    assertThat(error).matches("cvc-complex-type.4:.*'name'.*'v:source'.*");
  }

  @Test
  public void testGetScanStatus() throws Exception {
    String sbom = getSbomFile("/ApiThirdPartyResourceTest/valid_sbom.xml");
    HttpResponse response =
        restRequest().path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyResource.SCAN_COMPONENTS)
            .parameter(app.getId(), "clair").query("stageId", "build").body(sbom, MediaType.APPLICATION_XML).post();
    assertResponseStatus(202, response);

    ApiThirdPartyScanTicketDTO apiThirdPartyScanTicketDTO = response.getBody(ApiThirdPartyScanTicketDTO.class);
    assertThat(apiThirdPartyScanTicketDTO).isNotNull();
    assertThat(apiThirdPartyScanTicketDTO.statusUrl).isNotNull();

    response = restRequest().path(apiThirdPartyScanTicketDTO.statusUrl).get();

    assertResponseStatus(404, response);
  }

  private String getSbomFile(String path) throws Exception {
    byte[] bytes =
        Files.readAllBytes(Paths.get(getClass().getResource(path).toURI()));
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
