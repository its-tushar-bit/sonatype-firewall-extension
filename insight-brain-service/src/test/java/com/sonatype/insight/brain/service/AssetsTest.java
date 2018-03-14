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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AssetsTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(InsightBrainService.BRAIN_ASSET_PATH);
  }

  @Test
  public void testCssUrlsRelative() throws Exception {
    final List<String> CSS_PATHS = Arrays.asList("audit-report/audit-report.css", "cip/cip.css",
        "css/style-1.css", "css/style-2.css", "policy/css/cip-loader.css",
        "version-graph/version.graph.app.css", "version-graph/viewdetails.css");
    final List<String> failingCssPaths = new ArrayList<>();
    for (String cssPath : CSS_PATHS) {
      HttpResponse response = restRequest().path(cssPath).get();
      assertResponseStatus(200, response);
      String body = response.getBodyText();
      if (body.contains("url(/")) {
        failingCssPaths.add(cssPath);
      }
    }
    assertThat(failingCssPaths, is(empty()));
  }

  @Test
  public void testMimeTypes() throws Exception {
    HttpResponse response = restRequest().path("index.html").get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType(), is(equalToIgnoringCase("text/html;charset=UTF-8")));

    response = restRequest().path("css/style-1.css").get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType(), is(equalToIgnoringCase("text/css;charset=UTF-8")));

    response = restRequest().path("bundle.js").get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType(), is(equalToIgnoringCase("application/javascript")));

    response = restRequest().path("fonts/glyphicons-regular.woff").get();
    assertResponseStatus(200, response);
    assertThat(response.getContentType(), is(equalToIgnoringCase("application/font-woff")));
  }

  @Test
  @ManualServerInit
  public void testNonEmptyContextPath() throws Exception {
    initServer(config -> {
      ((DefaultServerFactory) config.getServerFactory()).setApplicationContextPath("/testContext");
    });
    assertThat(restRequest().getUrl(), containsString("/testContext/"));

    HttpResponse response = restRequest().path("index.html").get();
    assertResponseStatus(200, response);
  }
}
