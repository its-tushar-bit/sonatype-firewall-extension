/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ApplicationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ApplicationResource.RESOURCE_PATH);
  }

  @Test
  public void testGetScanApplicationManagementSummary() throws Exception {
    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path(ApplicationResource.GET_SCAN_APPLICATION_MANAGEMENT_SUMMARY).parameter(
        app.getPublicId(), "scan123");
    testAuthzGet(request, 404);
  }

  @Test
  public void testGetAllSummaries() throws Exception {
    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES);
    HttpResponse response = request.auth(unauthorized.getUsername(), unauthorized.getPassword()).get();
    assertResponseStatus(200, response);
    ApplicationManagementSummaryDTO[] entities = response.getBody(ApplicationManagementSummaryDTO[].class);
    assertThat(entities, is(emptyArray()));

    response = request.auth(authorized.getUsername(), authorized.getPassword()).get();
    assertResponseStatus(200, response);
    entities = response.getBody(ApplicationManagementSummaryDTO[].class);
    assertThat(entities.length, is(1));
    assertThat(entities[0].getId(), is(app.getId()));
  }

  @Test
  public void testGenerateIcon() throws Exception {
    String hash = "abababababababababab";
    setHdsResponseForURI("rest/application/icon/generate/" + hash, new byte[0], 200);
    HttpRequest request = restRequest().path(ApplicationResource.GENERATE_ICON_PATH).parameter(hash);
    testAuthcGet(request);
  }

  @Test
  public void testGetApplicationManagementSummary() throws Exception {
    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARY).parameter(
        app.getPublicId());
    testAuthzGet(request);
  }

  @Test
  public void testGetIcon() throws Exception {
    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path(ApplicationResource.GET_APPLICATION_ICON_PATH)
        .parameter(app.getPublicId());
    testAuthzGet(request);
  }

  @Test
  public void testSetIcon() throws Exception {
    grantWritePermission(app.getId());

    HttpRequest request = restRequest().path(ApplicationResource.ICON_PATH).part("applicationId", app.getId())
        .part("hasRobotSource", "false");
    testAuthzPost(request);
  }

  @Test
  public void testGetApplicationNamesForEvaluateComponent_Anonymous() throws Exception {
    HttpResponse response = restRequest().path(ApplicationResource.GET_APPLICATION_NAMES).anon().get();
    assertResponseStatus(401, response);
  }
}
