/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.net.URI;

import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
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
  }

  @Test
  public void testEvaluateComponents_enabledThirdPartyScan() throws Exception {
    initServer(config -> config.setThirdPartyEvaluationApiEnabled(true));
    HttpResponse response = restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyResource.SCAN_COMPONENTS)
        .parameter(app.getId(), "clair").query("stageId", "build").body("<bom/>", MediaType.APPLICATION_XML).post();
    assertResponseStatus(200, response);

    ApiComponentEvaluationTicketDTOV2 evaluationResult = response.getBody(ApiComponentEvaluationTicketDTOV2.class);
    assertThat(evaluationResult).isNotNull();
    assertThat(evaluationResult.applicationId.equals(app.getId()));
    assertThat(evaluationResult.resultId).isNotNull();
    assertThat(evaluationResult.resultsUrl).isNotNull();
    assertThat(new URI(evaluationResult.resultsUrl)).hasNoParameters();
  }

  @Test
  public void testEvaluateComponents_nullSbom() throws Exception {
    initServer(config -> config.setThirdPartyEvaluationApiEnabled(true));
    HttpResponse response = restRequest()
        .path(PublicApiPaths.THIRD_PARTY_SCAN_PATH, ApiThirdPartyResource.SCAN_COMPONENTS)
        .parameter(app.getId(), "clair").query("stageId", "build").body(null).post();
    assertResponseStatus(400, response);
  }
}
