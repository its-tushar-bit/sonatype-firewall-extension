/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiRoleResourceTest
    extends AbstractBrainServiceIntegrationTest
{
  @Test
  public void testGetRoles() throws Exception {
    HttpResponse response = restRequest().get();
    ApiRoleListDTO apiRoleListDTO = response.getBody(ApiRoleListDTO.class);

    assertResponseStatus(200, response);
    assertThat(apiRoleListDTO.roles)
        .extracting(role -> role.id)
        .containsExactlyInAnyOrder(
            Role.SYSTEM_ADMIN_ROLE_ID,
            Role.POLICY_ADMIN_ROLE_ID,
            Role.APPLICATION_EVALUATOR_ROLE_ID,
            Role.COMPONENT_EVALUATOR_ROLE_ID,
            Role.DEVELOPER_ROLE_ID,
            Role.LEGAL_REVIEWER_ROLE_ID,
            Role.OWNER_ROLE_ID
        );
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ROLE_RESOURCE_PATH_V2);
  }
}
