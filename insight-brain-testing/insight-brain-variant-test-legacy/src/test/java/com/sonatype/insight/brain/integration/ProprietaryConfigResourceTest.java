/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.variant.LegacyServerTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@LegacyServerTest
public class ProprietaryConfigResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return restRequest(null, null);
  }

  protected HttpRequest restRequest(Goal goal, String applicationId) {
    HttpRequest request = super.restRequest().path(ProprietaryConfigResource.RESOURCE_PATH);
    if (goal != null) {
      request.query(ProprietaryConfigResource.GOAL_PARAM, goal);
    }
    if (applicationId != null) {
      request.query(ProprietaryConfigResource.APPLICATION_PARAM, applicationId);
    }
    return request;
  }

  @Test
  public void testGet_InitialConfig() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    ProprietaryConfig config = response.getBody(ProprietaryConfig.class);
    assertThat(config).isNotNull();
    assertThat(config.getPackages()).isEmpty();
  }

  @Test
  public void testGet_GoalEvaluateApplication() throws Exception {
    tempEntity.newApplicationWithParent("app-id");

    HttpResponse response = restRequest(Goal.EVALUATE_APPLICATION, "app-id").get();
    assertResponseStatus(200, response);
    ProprietaryConfig config = response.getBody(ProprietaryConfig.class);
    assertThat(config).isNotNull();
    assertThat(config.getPackages()).isEmpty();
  }

  @Test
  public void testGet_GoalEvaluateComponent() throws Exception {
    tempEntity.newApplicationWithParent("app-id");

    HttpResponse response = restRequest(Goal.EVALUATE_COMPONENT, "app-id").get();
    assertResponseStatus(200, response);
    ProprietaryConfig config = response.getBody(ProprietaryConfig.class);
    assertThat(config).isNotNull();
    assertThat(config.getPackages()).isEmpty();
  }
}
