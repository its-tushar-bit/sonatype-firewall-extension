/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class LicensedStagesResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LicensedStagesResource.RESOURCE_PATH);
  }

  @Test
  public void testGet_UnauthenticatedUserNotAllowed() throws Exception {
    HttpResponse response = restRequest().auth("unknownUser", "unknownPassword").get();
    assertResponseStatus(401, response);
  }
}
