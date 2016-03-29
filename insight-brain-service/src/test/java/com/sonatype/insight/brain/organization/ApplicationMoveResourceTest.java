/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;

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
    assertThat(orgs, is(arrayWithSize(1)));
    assertThat(orgs[0].getId(), is(org.getId()));
    assertThat(orgs[0].getName(), is(org.getName()));
  }

  @Test
  public void testMoveApplication() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent("test-app-id");

    HttpResponse response = restRequest().path(ApplicationMoveResource.DESTINATION_PATH)
        .parameter(app.getId(), org.getId()).post();
    assertResponseStatus(200, response);
    String[] warnings = response.getBody(String[].class);
    assertThat(warnings, is(arrayWithSize(0)));
    assertThat(new ApplicationDAO().getById(app.getId()).getOrganizationId(), is(org.getId()));
  }

  @Test
  public void testMoveApplication_UnsatisfiedPreconditions() throws Exception {
    Organization org1 = tempEntity.newOrganization("New Parent");
    Organization org2 = tempEntity.newOrganization("Old Parent");
    Application app = tempEntity.newApplication("My App", "test-app-id", org2.getId());
    tempEntity.newPolicy(app.getOrganizationId(), "Missing Policy");

    HttpResponse response = restRequest().path(ApplicationMoveResource.DESTINATION_PATH)
        .parameter(app.getId(), org1.getId()).post();
    assertResponseStatus(409, response);
    String[] issues = response.getBody(String[].class);
    assertThat(issues,
        is(arrayContaining(String.format(ApplicationMoveService.POLICY_MISSING_MSG, "Missing Policy", org2.getName()))));
    assertThat(new ApplicationDAO().getById(app.getId()).getOrganizationId(), is(app.getOrganizationId()));
  }
}
