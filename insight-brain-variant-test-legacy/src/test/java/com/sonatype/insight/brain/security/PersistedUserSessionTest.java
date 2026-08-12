/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.net.HttpCookie;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.organization.OrganizationResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class PersistedUserSessionTest
    extends AbstractResourceTest
{
  @Test
  public void testPersistedUserSession() throws Exception {
    HttpResponse response = restRequest().path(UserSessionResource.RESOURCE_PATH).post();
    HttpCookie httpCookie = response.getSessionCookie();
    response = restRequest().anon().cookie(httpCookie).path(OrganizationResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    getTestCLMServer().stop();
    getTestCLMServer().start();

    response = restRequest().anon().cookie(httpCookie).path(OrganizationResource.RESOURCE_PATH).get();

    assertResponseStatus(200, response);
  }
}
