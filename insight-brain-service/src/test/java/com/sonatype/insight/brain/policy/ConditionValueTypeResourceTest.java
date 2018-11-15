/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConditionValueTypeResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(ConditionValueTypeResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  public void testGetConditionValueTypes_Application() throws Exception {
    String appPublicId = "ConditionValueTypeResourceTest_AppId";
    tempEntity.newApplicationWithParent(appPublicId);

    final HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = response.getBody(Object[].class);
    assertNotNull(conditionValueTypes);
    assertTrue(conditionValueTypes.length > 0);
  }

  @Test
  public void testGetConditionValueTypes_Organization() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();

    final HttpResponse response = restRequest(OwnerType.ORGANIZATION, orgId).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = response.getBody(Object[].class);
    assertNotNull(conditionValueTypes);
    assertTrue(conditionValueTypes.length > 0);
  }
}
