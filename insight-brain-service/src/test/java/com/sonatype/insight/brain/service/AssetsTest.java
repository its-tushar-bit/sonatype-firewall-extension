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

import io.dropwizard.server.DefaultServerFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AssetsTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(InsightBrainService.BRAIN_ASSET_PATH);
  }

  @Test
  public void testCssUrlsRelative() throws Exception {
    final List<String> CSS_PATHS = Arrays.asList("audit-report.css", "cip.css", "style-1.css", "style-2.css",
        "cip-loader.css", "version.graph.app.css", "viewdetails.css");
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

    response = restRequest().path("style-1.css").get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualToIgnoringCase("text/css;charset=UTF-8");

    response = restRequest().path("bundle.js").get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualToIgnoringCase("application/javascript;charset=UTF-8");

    response = restRequest().path("fonts/glyphicons-regular.woff").get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType()).isEqualToIgnoringCase("application/font-woff;charset=UTF-8");
  }

  @Test
  @ManualServerInit
  public void testNonEmptyContextPath() throws Exception {
    initServer(config -> {
      ((DefaultServerFactory) config.getServerFactory()).setApplicationContextPath("/testContext");
    });
    assertThat(restRequest().getUrl()).contains("/testContext/");

    HttpResponse response = restRequest().path("index.html").get();
    assertResponseStatus(200, response);
  }
}
