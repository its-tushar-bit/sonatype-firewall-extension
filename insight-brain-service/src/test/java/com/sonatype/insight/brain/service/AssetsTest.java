/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;

import io.dropwizard.core.server.DefaultServerFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class AssetsTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(InsightBrainService.BRAIN_ASSET_PATH);
  }

  @Test
  public void testCssUrlsRelative() throws Exception {
    final List<String> CSS_PATHS = Arrays.asList("bundle.css", "version-graph-react.css", "viewdetails-react.css");
    final List<String> failingCssPaths = new ArrayList<>();
    for (String cssPath : CSS_PATHS) {
      HttpResponse response = restRequest().path(cssPath).get();
      assertResponseStatus(200, response);
      String body = response.getBodyText();
      if (body.contains("url(/")) {
        failingCssPaths.add(cssPath);
      }
    }
    assertThat(failingCssPaths).isEmpty();
  }

  @Test
  public void testMimeTypes() throws Exception {
    HttpResponse response = restRequest().path("index.html").get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualToIgnoringCase("text/html;charset=UTF-8");

    response = restRequest().path("bundle.css").get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualToIgnoringCase("text/css;charset=UTF-8");

    response = restRequest().path("bundle.js").get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualToIgnoringCase("text/javascript;charset=UTF-8");

    response = restRequest().path("fonts/sonatype-icons.woff").get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualToIgnoringCase("font/woff;charset=utf-8");
  }

  @Test
  @ManualIqServerInit
  public void testNonEmptyContextPath() throws Exception {
    startIqTestServer(
        config -> ((DefaultServerFactory) config.getServerFactory()).setApplicationContextPath("/testContext"));
    assertThat(restRequest().getUrl()).contains("/testContext/");

    HttpResponse response = restRequest().path("index.html").get();
    assertResponseStatus(200, response);
  }
}
