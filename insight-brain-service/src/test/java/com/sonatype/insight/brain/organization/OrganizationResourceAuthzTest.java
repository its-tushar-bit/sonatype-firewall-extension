/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import com.ning.http.client.Response;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class OrganizationResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(OrganizationResource.SERVICE_PATH);
  }

  @Test
  public void testGenerateIcon() throws Exception {
    String hash = "abababababababababab";
    setSaasResponseForURI("rest/application/icon/generate/" + hash, new byte[0], 200);
    HttpRequest request = restRequest().path(OrganizationResource.GENERATE_ICON_PATH).parameter(hash);
    testAuthcGet(request);
  }

  @Test
  public void testGetIcon() throws Exception {
    grantReadPermission(org.getId());

    HttpRequest request = restRequest().path(OrganizationResource.GET_ICON_PATH).parameter(org.getId());
    testAuthzGet(request, 307);
  }

  @Test
  public void testSetIcon() throws Exception {
    grantWritePermission(org.getId());

    HttpRequest request = restRequest().path(OrganizationResource.ICON_PATH).part("organizationId", org.getId())
        .part("hasRobotSource", "false");
    testAuthzPost(request, 204);
  }

  @Test
  public void testSetIconSync() throws Exception {
    grantWritePermission(org.getId());

    HttpRequest request = restRequest().path(OrganizationResource.ICON_PATH_SYNC).part("organizationId", org.getId())
        .part("hasRobotSource", "false");
    request.auth(unauthorized.getUsername(), unauthorized.getPassword());
    Response response = request.post();
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), is("Insufficient permissions"));

    request.auth(authorized.getUsername(), authorized.getPassword());
    response = request.post();
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), is(""));
  }
}
