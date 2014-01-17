/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class IdeResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testDoScan() throws Exception {
    String hash = "0123456789";
    setSaasResponseForURI("rest/ide/scan/simple/" + hash, "{}", 200);
    grantReadPermission(app.getId());

    String url = getRestUrl(IdeResource.SERVICE_PATH + "/scan/simple/{appPublicId}/{hash}", app.getPublicId(), hash);
    testAuthzGet(url);
  }

  @Test
  public void testPostScan() throws Exception {
    String hash = "0123456789";
    setSaasResponseForURI("rest/ide/scan/enhanced/" + hash, "{}", 200);
    grantReadPermission(app.getId());

    String url = getRestUrl(IdeResource.SERVICE_PATH + "/scan/enhanced/{appPublicId}/{hash}", app.getPublicId(), hash);
    String json = "{}";
    testAuthzPost(url, json);
  }
}
