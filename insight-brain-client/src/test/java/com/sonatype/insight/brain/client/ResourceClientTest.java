/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import com.sonatype.clm.dto.model.Resource;
import com.sonatype.insight.brain.service.AbstractLicenseTest;

import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ResourceClientTest
    extends AbstractLicenseTest
{

  @Test
  public void testMissingFile() throws Exception {
    try {
      new ResourceClient(brain.getClientConfiguration()).getResource("/assets/foo/bar");
      fail("No exception thrown");
    }
    catch (HttpResponseException e) {
      assertEquals(404, e.getStatusCode());
      assertThat(e.getMessage(), containsString("Problem accessing /assets/foo/bar. Reason"));
    }
  }

  @Test
  public void testValidFile() throws Exception {
    Resource resource = new ResourceClient(brain.getClientConfiguration()).getResource("/assets/index.html");
    assertTrue(new String(resource.getData()).startsWith("<!DOCTYPE html>"));
    // check mime type
    assertEquals("text/html;charset=UTF-8", resource.getContentType());
  }
}
