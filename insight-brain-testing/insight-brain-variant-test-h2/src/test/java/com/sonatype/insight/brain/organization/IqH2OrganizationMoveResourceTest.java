/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2OrganizationMoveResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(OrganizationMoveResource.RESOURCE_PATH);
  }

  @Test
  void testGetDestinationOrganizations() throws Exception {
    Organization organizationToMove = ctx.tempEntity().newOrganization();
    Organization destination1 = ctx.tempEntity().newOrganization("destination1");
    Organization destination2 = ctx.tempEntity().newOrganization("destination2");

    HttpResponse response =
        restRequest().path(OrganizationMoveResource.DESTINATIONS_PATH).parameter(organizationToMove.getId()).get();
    ctx.assertResponseStatus(200, response);
    List<Organization> availableDestinationOrgs = response.getBodyList(Organization.class);
    assertThat(availableDestinationOrgs).hasSize(2);
    assertThat(availableDestinationOrgs.stream().map(Organization::getId)).contains(destination1.getId(),
        destination2.getId());
  }
}
