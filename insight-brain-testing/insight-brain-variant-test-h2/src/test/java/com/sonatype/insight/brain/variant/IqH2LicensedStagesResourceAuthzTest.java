/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.policy.LicensedStagesResource;

import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2LicensedStagesResourceAuthzTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().anon().path(LicensedStagesResource.RESOURCE_PATH);
  }

  @Test
  void testGet_UnauthenticatedUserNotAllowed() throws Exception {
    HttpResponse response = restRequest().auth("unknownUser", "unknownPassword").get();
    ctx.assertResponseStatus(401, response);
  }
}
