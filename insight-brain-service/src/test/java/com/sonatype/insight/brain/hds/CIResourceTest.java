/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.features.Feature;

import org.junit.Test;

public class CIResourceTest
    extends AbstractScanResourceTest
{
  @Test
  public void testScan_FeatureUnlicensed() throws Exception {
    setMissingFeature(Feature.CI_INTEGRATION);

    HttpResponse response = scanRequest("unlicensedapp").put();
    assertResponseStatus(402, response);
  }

  @Override
  protected HttpRequest scanRequest(String appId) {
    return restRequest().path(CIResource.RESOURCE_PATH, CIResource.SCAN_PATH).parameter(appId);
  }
}
