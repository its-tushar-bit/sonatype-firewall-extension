/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Assert;
import org.junit.Test;

public class ConditionValueTypeResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(String ownerType, String ownerId) {
    return restRequest().path(ConditionValueTypeResource.SERVICE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  public void testGetConditionValueTypes_Application() throws Exception {
    String appPublicId = "ConditionValueTypeResourceTest_AppId";
    tempEntity.newApplicationWithParent(appPublicId);

    final HttpResponse response = restRequest("application", appPublicId).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = fromJson(response, Object[].class);
    Assert.assertNotNull(conditionValueTypes);
    Assert.assertTrue(conditionValueTypes.length > 0);
  }

  @Test
  public void testGetConditionValueTypes_Organization() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();

    final HttpResponse response = restRequest("organization", orgId).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = fromJson(response, Object[].class);
    Assert.assertNotNull(conditionValueTypes);
    Assert.assertTrue(conditionValueTypes.length > 0);
  }
}
