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

public class CIResourceAuthzTest
    extends AbstractResourceAuthzTest
{

  @Test
  public void testValidate() throws Exception {
    grantReadPermission(app.getId());
    testAuthzGet(getServiceURL() + "/validate/" + app.getPublicId());
  }

  @Test
  public void testValidate_UnauthorizedAnonymousAllowed() throws Exception {
    Response response = RestAccess.get(getServiceURL() + "/validate/" + app.getPublicId());
    assertResponseStatus(200, response);
  }

  @Test
  public void testValidate_Unauthorized() throws Exception {
    Response response = RestAccess.get(getServiceURL() + "/validate/" + app.getPublicId(),
        "unknownUser", "unknownPassword");
    assertResponseStatus(401, response);
  }

  @Test
  public void testScan() throws Exception {
    grantWritePermission(app.getId());
    testAuthzPut(getServiceURL() + "/scan/" + app.getPublicId(), "");
  }

  @Test
  public void testScan_UnauthorizedAnonymousAllowed() throws Exception {
    Response response = RestAccess.put(getServiceURL() + "/scan/" + app.getPublicId(), "");
    assertResponseStatus(200, response);
  }

  @Test
  public void testScan_Unauthorized() throws Exception {
    Response response = RestAccess.put(getServiceURL() + "/scan/" + app.getPublicId(),
        "unknownUser", "unknownPassword");
    assertResponseStatus(401, response);
  }

  private String getServiceURL() {
    return getRestBaseUrl() + CIResource.SERVICE_PATH;
  }
}
