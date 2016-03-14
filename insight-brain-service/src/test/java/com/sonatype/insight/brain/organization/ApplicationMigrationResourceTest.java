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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;

public class ApplicationMigrationResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ApplicationMigrationResource.RESOURCE_PATH);
  }

  @Test
  public void testGetDestinationOrganizations() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent("test-app-id");

    HttpResponse response = restRequest().path(ApplicationMigrationResource.DESTINATIONS_PATH).parameter(app.getId())
        .get();
    assertResponseStatus(200, response);
    Organization[] orgs = response.getBody(Organization[].class);
    assertThat(orgs, is(arrayWithSize(1)));
    assertThat(orgs[0].getId(), is(org.getId()));
    assertThat(orgs[0].getName(), is(org.getName()));
  }

  @Test
  public void testMigrateApplication() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");

    HttpResponse response = restRequest().path(ApplicationMigrationResource.DESTINATION_PATH)
        .parameter(app.getId(), app.getOrganizationId()).post();
    assertResponseStatus(204, response);
  }
}
