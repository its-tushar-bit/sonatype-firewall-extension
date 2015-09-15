/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class LicenseResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetLicenses() throws Exception {
    testAuthcGet(restRequest().path(LicenseResource.RESOURCE_PATH));
  }
}
