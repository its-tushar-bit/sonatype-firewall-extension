/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class EnvironmentResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private static final String QUERY_PARAMS = "p=eclipse&version=2.0.1.qualifier";

  @Test
  public void testSubmitClientEnvironment() throws Exception {
    setSaasResponseForURI("session/environment?" + QUERY_PARAMS, "", 200);
    testAuthcGet(getServiceURL(QUERY_PARAMS));
  }

  public String getServiceURL(final String queryParams) {
    return getRestBaseUrl() + EnvironmentResource.RESOURCE_PATH + "?" + queryParams;
  }
}
