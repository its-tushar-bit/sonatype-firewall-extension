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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ResourceClientTest
    extends AbstractBrainServiceTest
{
  @Test
  public void testMissingFile() {
    assertThatExceptionOfType(HttpResponseException.class)
        .isThrownBy(() -> new ResourceClient(getCLMServer().getClientConfiguration()).getResource("/assets/foo/bar"))
        .withMessageContaining("Not Found")
        .satisfies(e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND));
  }

  @Test
  public void testValidFile() throws Exception {
    Resource resource = new ResourceClient(getCLMServer().getClientConfiguration()).getResource("/assets/index.html");
    assertThat(new String(resource.getData(), StandardCharsets.UTF_8)).startsWith("<!DOCTYPE html>");
    // check mime type
    assertThat(resource.getContentType()).isEqualToIgnoringCase("text/html;charset=UTF-8");
  }
}
