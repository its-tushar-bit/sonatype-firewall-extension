/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class LicenseResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGet() throws Exception {
    Response response = RestAccess.get(getRestBaseUrl() + LicenseResource.SERVICE_PATH);
    assertResponseStatus(200, response);

    License[] licenses = JsonHelpers.fromJson(response.getResponseBody(), License[].class);
    Assert.assertNotNull(licenses);
    Assert.assertNotEquals(licenses.length, 0);
  }
}