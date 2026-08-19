/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApplicationMoveResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(ApplicationMoveResource.RESOURCE_PATH);
  }

  @Test
  void testGetDestinationOrganizations() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    Application app = ctx.tempEntity().newApplicationWithParent("test-app-id");

    HttpResponse response = restRequest().path(ApplicationMoveResource.DESTINATIONS_PATH).parameter(app.getId()).get();
    ctx.assertResponseStatus(200, response);
    Organization[] orgs = response.getBody(Organization[].class);
    assertThat(orgs).hasSize(1);
    assertThat(orgs[0].getId()).isEqualTo(org.getId());
    assertThat(orgs[0].getName()).isEqualTo(org.getName());
  }
}
