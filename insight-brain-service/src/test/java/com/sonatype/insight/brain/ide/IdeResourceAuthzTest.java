/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class IdeResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(IdeResource.RESOURCE_PATH);
  }

  @Test
  public void testDoScan() throws Exception {
    String hash = "0123456789";
    hdsRespondWith("{}").atUri("rest/ide/scan/simple/" + hash);
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);

    HttpRequest request = restRequest().path("scan/simple/{appPublicId}/{hash}").parameter(app.getPublicId(), hash);
    testAuthzGet(request);
  }

  @Test
  public void testPostScan() throws Exception {
    String hash = "0123456789";
    hdsRespondWith("{}").atUri("rest/ide/scan/enhanced/" + hash);
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);

    HttpRequest request = restRequest().path("scan/enhanced/{appPublicId}/{hash}")
        .parameter(app.getPublicId(), hash)
        .body("{}");
    testAuthzPost(request);
  }

  @Test
  public void testDoCoordinatesScan() throws Exception {
    hdsRespondWith("[]").atUri("rest/ide/scan/coordinates");
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);

    HttpRequest request = restRequest().path(IdeResource.COORDINATES_SCAN_PATH).parameter(app.getPublicId());
    testAuthzGet(request);
  }
}
