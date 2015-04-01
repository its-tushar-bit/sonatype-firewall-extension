/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class LicensedStagesResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testGet_UnauthenticatedAnonymousAllowed() throws Exception {
    Response response = RestAccess.get(getRestUrl(LicensedStagesResource.SERVICE_PATH));
    assertResponseStatus(200, response);
  }

  @Test
  public void testGet_UnauthenticatedUserNotAllowed() throws Exception {
    Response response = RestAccess.get(getRestUrl(LicensedStagesResource.SERVICE_PATH),
        "unknownUser", "unknownPassword");
    assertResponseStatus(401, response);
  }
}
