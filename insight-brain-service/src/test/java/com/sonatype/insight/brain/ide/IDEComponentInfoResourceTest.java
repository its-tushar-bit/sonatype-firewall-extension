/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.hds.AbstractComponentInfoResourceTest;

import org.junit.Test;

@Category(SlowTest.class)
public class IDEComponentInfoResourceTest
    extends AbstractComponentInfoResourceTest
{
  @Override
  protected String getResourcePath() {
    return IDEComponentInfoResource.RESOURCE_PATH;
  }

  @Test
  public void testGetComponentDetails() throws Exception {
    super.testGetComponentDetails_EvaluateComponentPermission();
  }

  @Test
  public void testGetComponentDetailsList() throws Exception {
    super.testGetComponentDetailsList_EvaluateComponentPermission();
  }
}
