/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationMoveResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ApplicationMoveResource.RESOURCE_PATH);
  }

  @Test
  public void testGetDestinationOrganizations() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent("test-app-id");

    HttpResponse response = restRequest().path(ApplicationMoveResource.DESTINATIONS_PATH).parameter(app.getId()).get();
    assertResponseStatus(200, response);
    Organization[] orgs = response.getBody(Organization[].class);
    assertThat(orgs).hasSize(1);
    assertThat(orgs[0].getId()).isEqualTo(org.getId());
    assertThat(orgs[0].getName()).isEqualTo(org.getName());
  }
}
