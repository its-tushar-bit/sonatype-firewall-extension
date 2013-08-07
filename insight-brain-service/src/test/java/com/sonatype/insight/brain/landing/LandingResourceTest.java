/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.TestInsightBrainService;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LandingResourceTest
    extends AbstractResourceTest
{

  @Override
  protected void configureBrain(TestInsightBrainService brain) {
    super.configureBrain(brain);

    if (testName.getMethodName().endsWith("ConfiguredBaseUrl")) {
      brain.setBaseUrl("http://clm.sonatype.com/test");
    }
  }

  @Test
  public void testHome_RequestBaseUrl() throws Exception {
    Response response = RestAccess.get(getRestBaseUrl() + "?x=y&a=b");
    assertResponseStatus(303, response);
    assertEquals(getRestBaseUrl() + InsightBrainService.BRAIN_ASSET_PATH.substring(1) + "index.html?x=y&a=b",
        response.getHeader("Location"));
  }

  @Test
  public void testHome_ConfiguredBaseUrl() throws Exception {
    Response response = RestAccess.get(getRestBaseUrl() + "?x=y&a=b");
    assertResponseStatus(303, response);
    assertEquals("http://clm.sonatype.com/test/" + InsightBrainService.BRAIN_ASSET_PATH.substring(1)
        + "index.html?x=y&a=b", response.getHeader("Location"));
  }

}
