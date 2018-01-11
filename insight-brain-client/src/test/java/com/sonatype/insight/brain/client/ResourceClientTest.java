/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.nio.charset.StandardCharsets;

import com.sonatype.clm.dto.model.Resource;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ResourceClientTest
    extends AbstractBrainServiceTest
{

  @Test
  public void testMissingFile() throws Exception {
    try {
      new ResourceClient(getCLMServer().getClientConfiguration()).getResource("/assets/foo/bar");
      fail("No exception thrown");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode(), is(HttpStatus.SC_NOT_FOUND));
      assertThat(e.getMessage(), containsString("Not Found"));
    }
  }

  @Test
  public void testValidFile() throws Exception {
    Resource resource = new ResourceClient(getCLMServer().getClientConfiguration()).getResource("/assets/index.html");
    assertThat(new String(resource.getData(), StandardCharsets.UTF_8), startsWith("<!DOCTYPE html>"));
    // check mime type
    assertThat(resource.getContentType(), is(equalToIgnoringCase("text/html;charset=UTF-8")));
  }
}
