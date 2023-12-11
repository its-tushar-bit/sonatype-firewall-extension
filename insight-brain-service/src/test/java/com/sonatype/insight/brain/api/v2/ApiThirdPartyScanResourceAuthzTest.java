/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

public class ApiThirdPartyScanResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGetIdeUsersOverview_Requires_Authentication() throws Exception {
    String endpointPath = PublicApiPaths.THIRD_PARTY_SCAN_PATH + '/' + ApiThirdPartyScanResource.IDE_USER_OVERVIEW;
    testAuthcGet(restRequest().path(endpointPath));
  }
}
