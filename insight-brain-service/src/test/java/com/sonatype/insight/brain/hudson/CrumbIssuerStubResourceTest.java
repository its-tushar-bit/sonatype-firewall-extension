/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hudson;

import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

public class CrumbIssuerStubResourceTest
    extends AbstractResourceTest
{

  @Test
  public void testGet() throws Exception {
    uninstallLicense();
    Response response = RestAccess.get(getRestBaseUrl() + CrumbIssuerStubResource.SERVICE_PATH);
    assertResponseStatus(404, response);
  }

}
