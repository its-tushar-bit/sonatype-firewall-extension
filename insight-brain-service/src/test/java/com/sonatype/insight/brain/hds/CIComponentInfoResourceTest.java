/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.hds.CIComponentInfoResource;

import org.junit.Test;

public class CIComponentInfoResourceTest
    extends AbstractComponentInfoResourceTest
{
  @Override
  protected String getResourcePath() {
    return CIComponentInfoResource.RESOURCE_PATH;
  }

  @Test
  public void testGetComponentDetails() throws Exception {
    super.testGetComponentDetails_ReadPermission();
  }

  @Test
  public void testGetComponentDetailsList() throws Exception {
    super.testGetComponentDetailsList_ReadPermission();
  }
}
