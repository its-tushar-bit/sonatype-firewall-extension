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
import java.util.List;

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
  public void testEvaluateComponents_disabledThirdPartyScan() throws Exception {
    HttpResponse response = restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyResource.SCAN_COMPONENTS)
        .parameter(app.getId(), "clair").query("stageId", "build").body("<bom/>", MediaType.APPLICATION_XML).post();
    assertResponseStatus(501, response);

    assertThat(response.getBodyText()).isEmpty();
  }

  @Test
  public void testEvaluateComponents_enabledThirdPartyScan() throws Exception {
    byte[] bytes =
        Files.readAllBytes(Paths.get(getClass().getResource("/ApiThirdPartyResourceTest/valid_sbom.xml").toURI()));
    String sbom = new String(bytes, StandardCharsets.UTF_8);

    initServer(config -> config.setThirdPartyEvaluationApiEnabled(true));
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
    initServer(config -> config.setThirdPartyEvaluationApiEnabled(true));
    HttpResponse response = restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyResource.SCAN_COMPONENTS)
        .parameter(app.getId(), "clair").query("stageId", "build").body(null).post();
    assertResponseStatus(400, response);
  }

  @Test
  public void testEvaluateComponents_invalidSbom() throws Exception {
    byte[] bytes =
        Files.readAllBytes(Paths.get(getClass().getResource("/ApiThirdPartyResourceTest/invalid_sbom.xml").toURI()));
    String sbom = new String(bytes, StandardCharsets.UTF_8);

    initServer(config -> config.setThirdPartyEvaluationApiEnabled(true));
    HttpResponse response = restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyResource.SCAN_COMPONENTS)
        .parameter(app.getId(), "clair").query("stageId", "build").body(sbom, MediaType.APPLICATION_XML).post();
    assertResponseStatus(400, response);

    List<String> errors = response.getBodyList();
    assertThat(errors).containsExactlyInAnyOrder(
        "cvc-complex-type.4: Attribute 'ref' must appear on element 'v:vulnerability'.",
        "cvc-complex-type.4: Attribute 'name' must appear on element 'v:source'.");
  }
}
