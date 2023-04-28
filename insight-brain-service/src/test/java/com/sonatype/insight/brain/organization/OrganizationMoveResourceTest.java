/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationMoveResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(OrganizationMoveResource.RESOURCE_PATH);
  }

  @Test
  public void testGetDestinationOrganizations() throws Exception {
    Organization organizationToMove = tempEntity.newOrganization();
    Organization destination1 = tempEntity.newOrganization("destination1");
    Organization destination2 = tempEntity.newOrganization("destination2");

    HttpResponse response =
        restRequest().path(OrganizationMoveResource.DESTINATIONS_PATH).parameter(organizationToMove.getId()).get();
    assertResponseStatus(200, response);
    List<Organization> availableDestinationOrgs = response.getBodyList(Organization.class);
    assertThat(availableDestinationOrgs).hasSize(2);
    assertThat(availableDestinationOrgs.stream().map(Organization::getId)).contains(destination1.getId(),
        destination2.getId());
  }
}
