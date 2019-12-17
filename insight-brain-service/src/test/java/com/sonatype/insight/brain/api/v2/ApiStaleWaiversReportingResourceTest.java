/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleWaiversResponseDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiStaleWaiversReportingResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetStaleWaivers() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy orgPolicy = tempEntity.newPolicy(app.getParentOwnerId());
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId1App1");
    tempEntity.newWaiver(orgPolicy.getId(), app.getParentOwnerId());

    HttpResponse response =
        restRequest().path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiStaleWaiversReportingResource.PATH).get();

    assertResponseStatus(200, response);
    ApiStaleWaiversResponseDTO responseDTO = response.getBody(ApiStaleWaiversResponseDTO.class);
    assertThat(responseDTO).isNotNull();
    assertThat(responseDTO.staleWaivers).hasSize(1);
  }
}
