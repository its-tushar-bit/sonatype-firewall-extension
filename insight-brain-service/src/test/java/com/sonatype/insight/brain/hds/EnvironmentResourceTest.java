/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

public class EnvironmentResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testSubmitClientEnvironment() throws Exception {
    String queryParams = "p=eclipse&version=2.0.1.qualifier";
    hdsRespondWith("").atUri("session/environment?" + queryParams);
    final HttpResponse response = restRequest().path(EnvironmentResource.RESOURCE_PATH).query(queryParams).get();
    assertResponseStatus(200, response);
  }
}
