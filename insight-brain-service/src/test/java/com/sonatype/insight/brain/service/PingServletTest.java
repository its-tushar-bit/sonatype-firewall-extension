/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class PingServletTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.PING_RESOURCE_PATH);
  }

  @Test
  public void testPing_Licensed() throws Exception {
    HttpResponse response = restRequest().anon().get();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText().trim()).isEqualTo("pong");
  }

  @Test
  public void testPing_Unlicensed() throws Exception {
    uninstallLicense();

    HttpResponse response = restRequest().anon().get();

    assertResponseStatus(200, response);
    assertThat(response.getBodyText().trim()).isEqualTo("pong");
  }
}
