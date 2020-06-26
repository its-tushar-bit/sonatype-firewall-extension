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

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ApplicationResource.RESOURCE_PATH);
  }

  @Test
  public void testGetAllSummaries() throws Exception {
    grantReadPermission(app.getId());

    HttpRequest request = restRequest().path(ApplicationResource.GET_APPLICATION_MANAGEMENT_SUMMARIES)
        .query("page", "1").query("pageSize", "50");
    HttpResponse response = request.auth(unauthorized).get();
    assertResponseStatus(200, response);
    ApplicationManagementSummaryDTO[] entities = response.getBody(ApplicationManagementSummaryDTO[].class);
    assertThat(entities).isEmpty();

    response = request.auth(authorized).get();
    assertResponseStatus(200, response);
    entities = response.getBody(ApplicationManagementSummaryDTO[].class);
    assertThat(entities).extracting(ApplicationManagementSummaryDTO::getId).containsExactly(app.getId());
  }

  @Test
  public void testGenerateIcon() throws Exception {
    HttpRequest request = restRequest().parameter("robohash").path(ApplicationResource.GENERATE_ICON_PATH);
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

    HttpRequest request = restRequest().path(ApplicationResource.SET_APPLICATION_ICON_PATH).parameter(app.getId())
        .part("hasRobotSource", "false");
    testAuthzPost(request);
  }
}
