/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class EnvironmentResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private static final String QUERY_PARAMS = "p=eclipse&version=2.0.1.qualifier";

  @Test
  public void testSubmitClientEnvironment() throws Exception {
    hdsRespondWith("").atUri("session/environment?" + QUERY_PARAMS);
    testAuthcGet(restRequest().path(EnvironmentResource.RESOURCE_PATH).query(QUERY_PARAMS));
  }
}
