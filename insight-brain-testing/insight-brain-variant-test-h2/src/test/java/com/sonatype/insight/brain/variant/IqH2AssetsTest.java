/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AssetPaths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2AssetsTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(AssetPaths.BRAIN_ASSET_PATH);
  }

  @Test
  void testCssUrlsRelative() throws Exception {
    final List<String> CSS_PATHS = Arrays.asList("bundle.css", "version-graph-react.css", "viewdetails-react.css");
    final List<String> failingCssPaths = new ArrayList<>();
    for (String cssPath : CSS_PATHS) {
      HttpResponse response = restRequest().path(cssPath).get();
      ctx.assertResponseStatus(200, response);
      String body = response.getBodyText();
      if (body.contains("url(/")) {
        failingCssPaths.add(cssPath);
      }
    }
    assertThat(failingCssPaths).isEmpty();
  }

  @Test
  void testMimeTypes() throws Exception {
    HttpResponse response = restRequest().path("index.html").get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualToIgnoringCase("text/html;charset=UTF-8");

    response = restRequest().path("bundle.css").get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualToIgnoringCase("text/css;charset=UTF-8");

    response = restRequest().path("bundle.js").get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualToIgnoringCase("text/javascript;charset=UTF-8");

    response = restRequest().path("fonts/sonatype-icons.woff").get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualToIgnoringCase("font/woff");
  }

  // Test for non-empty context path requires Spring Boot configuration
  // and is covered by integration tests that set server.servlet.context-path
  // @Test
  // @ManualIqServerInit
  // public void testNonEmptyContextPath() throws Exception {
  // startIqTestServer(
  // config -> config.setApplicationContextPath("/testContext"));
  // assertThat(restRequest().getUrl()).contains("/testContext/");
  //
  // HttpResponse response = restRequest().path("index.html").get();
  // assertResponseStatus(200, response);
  // }
}
