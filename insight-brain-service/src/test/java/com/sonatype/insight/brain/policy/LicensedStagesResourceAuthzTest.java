/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import com.ning.http.client.Response;
import org.junit.Test;

public class LicensedStagesResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LicensedStagesResource.SERVICE_PATH);
  }

  @Test
  public void testGet_UnauthenticatedAnonymousAllowed() throws Exception {
    Response response = restRequest().anon().get();
    assertResponseStatus(200, response);
  }

  @Test
  public void testGet_UnauthenticatedUserNotAllowed() throws Exception {
    Response response = restRequest().auth("unknownUser", "unknownPassword").get();
    assertResponseStatus(401, response);
  }
}
