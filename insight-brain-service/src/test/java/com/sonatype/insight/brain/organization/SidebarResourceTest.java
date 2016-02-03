/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class SidebarResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SidebarResource.RESOURCE_PATH);
  }

  @Test
  public void testGetOwnerDetails_Organization() throws Exception {
    HttpResponse response = restRequest().path(SidebarResource.GET_OWNER_DETAILS_PATH)
        .parameter(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID).get();

    assertValidOwnerDetailsDTO(response);
  }

  @Test
  public void testGetOwnerDetails_Application() throws Exception {
    final String applicationPublicId = "SidebarResourceTest_Application";
    tempEntity.newApplicationWithParent(applicationPublicId);

    HttpResponse response = restRequest().path(SidebarResource.GET_OWNER_DETAILS_PATH)
        .parameter(OwnerType.APPLICATION, applicationPublicId).get();

    assertValidOwnerDetailsDTO(response);
  }

  private void assertValidOwnerDetailsDTO(HttpResponse response) {
    assertResponseStatus(200, response);
    assertNotNull(response.getBodyBytes());
    OwnerDetailsDTO ownerDetailsDTO = response.getBody(OwnerDetailsDTO.class);
    assertNotNull(ownerDetailsDTO);
  }
}
