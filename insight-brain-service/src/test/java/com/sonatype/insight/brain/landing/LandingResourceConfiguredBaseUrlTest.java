/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightBrainService;

import com.ning.http.client.Response;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LandingResourceConfiguredBaseUrlTest
    extends AbstractResourceTest
{
  @Override
  protected String getBrainBaseUrl() {
    return "http://clm.sonatype.com/test";
  }

  @Test
  public void testHome_ConfiguredBaseUrl() throws Exception {
    Response response = AuthedRestAccess.get(getRestBaseUrl() + "?x=y&a=b");
    assertResponseStatus(303, response);
    assertEquals("http://clm.sonatype.com/test/" + InsightBrainService.BRAIN_ASSET_PATH.substring(1)
        + "reports.html?x=y&a=b", response.getHeader("Location"));
  }
}
