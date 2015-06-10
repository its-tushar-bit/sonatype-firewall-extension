/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.junit.Assert;
import org.junit.Test;

public class ConditionTypeResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetConditionTypes() throws Exception {
    final Response response = restRequest().path(ConditionTypeResource.SERVICE_PATH).get();
    assertResponseStatus(200, response);
    final Object[] conditionTypes = fromJson(response, Object[].class);
    Assert.assertNotNull(conditionTypes);
    Assert.assertTrue(conditionTypes.length > 0);
  }
}
