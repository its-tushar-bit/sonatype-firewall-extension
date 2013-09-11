/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Test;

public class ActionTypeResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetActionTypes() throws Exception {
    final Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    final Object[] actionTypes = JsonHelpers.fromJson(response.getResponseBody(), Object[].class);
    Assert.assertNotNull(actionTypes);
    Assert.assertTrue(actionTypes.length > 0);
  }

  private String getServiceURL() {
    return getRestBaseUrl() + ActionTypeResource.SERVICE_PATH;
  }
}
