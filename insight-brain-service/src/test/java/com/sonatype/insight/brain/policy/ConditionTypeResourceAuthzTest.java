/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class ConditionTypeResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetConditionTypes() throws Exception {
    String url = getRestUrl(ConditionTypeResource.SERVICE_PATH);
    testAuthcGet(url);
  }
}
