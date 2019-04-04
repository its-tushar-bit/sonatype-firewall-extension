/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.service.ComponentEvaluationV2Helper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiComponentRemediationResourceTest
    extends AbstractResourceTest
{
  private ComponentEvaluationV2Helper componentEvaluationV2Helper = new ComponentEvaluationV2Helper();

  @Test
  public void testGetComponentDetails() throws Exception {
    Application app = tempEntity.newApplicationWithParent("testApp");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier, null);

    HttpResponse response =
        restRequest().path(PublicApiPaths.COMPONENT_REMEDIATION_PATH_V2).parameter(OwnerType.APPLICATION, app.getId())
            .body(component).post();
    assertResponseStatus(200, response);

    String responseText = response.getBodyText();
    assertThat(responseText).doesNotContain("proprietary");

    ApiComponentRemediationDTO result = response.getBody(ApiComponentRemediationDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.remediation.versionChanges).isNotNull();
  }
}
