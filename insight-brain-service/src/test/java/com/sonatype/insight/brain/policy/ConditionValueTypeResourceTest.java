/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ConditionValueTypeResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetConditionValueTypes_Application() throws Exception {
    String appPublicId = "ConditionValueTypeResourceTest_AppId";
    createApplication(appPublicId);

    final Response response = RestAccess.get(getServiceURL("application", appPublicId));
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = JsonHelpers.fromJson(response.getResponseBody(), Object[].class);
    Assert.assertNotNull(conditionValueTypes);
    Assert.assertTrue(conditionValueTypes.length > 0);
  }

  @Test
  public void testGetConditionValueTypes_Organization() throws Exception {
    String orgId = createOrganization("test").getId();

    final Response response = RestAccess.get(getServiceURL("organization", orgId));
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = JsonHelpers.fromJson(response.getResponseBody(), Object[].class);
    Assert.assertNotNull(conditionValueTypes);
    Assert.assertTrue(conditionValueTypes.length > 0);
  }

  private String getServiceURL(String ownerType, String ownerId) {
    return getRestBaseUrl() + expandRestUrl(ConditionValueTypeResource.SERVICE_PATH, ownerType, ownerId);
  }
}
