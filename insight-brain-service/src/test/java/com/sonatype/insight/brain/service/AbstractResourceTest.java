/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;

import com.ning.http.client.Response;
import org.junit.Assert;

public abstract class AbstractResourceTest
    extends AbstractLicenseTest
{
  protected static void assertResponseStatus(final int expectedStatus, final Response response) throws IOException {
    final int actualStatus = response.getStatusCode();
    Assert.assertEquals("URI:" + response.getUri() + ", StatusText:" + response.getStatusText() + ", ResponseBody:"
        + response.getResponseBody(), expectedStatus, actualStatus);
  }
}
