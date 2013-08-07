/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class EnvironmentResourceTest
    extends AbstractResourceTest
{

  @Test
  public void testSubmitClientEnvironment() throws Exception {
    String queryParams = "p=eclipse&version=2.0.1.qualifier";
    setSaasResponseForURI("session/environment?" + queryParams, "", 200);
    final Response response = RestAccess.get(getRestBaseUrl() + EnvironmentResource.RESOURCE_PATH + "?" + queryParams);
    assertResponseStatus(200, response);
  }

}
