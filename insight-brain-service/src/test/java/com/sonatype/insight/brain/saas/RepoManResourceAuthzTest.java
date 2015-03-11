/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class RepoManResourceAuthzTest
    extends AbstractResourceAuthzTest
{

  @Test
  public void testUploadScan() throws Exception {
    grantWritePermission(app.getId());
    testAuthzPut(getServiceURL() + "/scan/" + app.getPublicId(), "");
  }

  @Test
  public void testUploadScan_UnauthorizedAnonymousAllowed() throws Exception {
    Response response = RestAccess.put(getServiceURL() + "/scan/" + app.getPublicId(), "");
    assertResponseStatus(200, response);
  }

  @Test
  public void testUploadScan_Unauthorized() throws Exception {
    final Response response = RestAccess.put(getServiceURL() + "/scan/" + app.getPublicId(),
        "unknownUser", "unknownPassword");
    assertResponseStatus(401, response);
  }

  private String getServiceURL() {
    return getRestBaseUrl() + RepoManResource.SERVICE_PATH;
  }
}
