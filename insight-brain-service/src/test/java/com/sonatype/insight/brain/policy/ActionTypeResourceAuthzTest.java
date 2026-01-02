/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ActionTypeResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetActionTypes() throws Exception {
    testAuthcGet(restRequest().path(ActionTypeResource.RESOURCE_PATH));
  }
}
