/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class LandingResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testHome() throws Exception {
    Response response = RestAccess.get(getRestBaseUrl());
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location"), startsWith(getRestBaseUrl()));
  }
}
