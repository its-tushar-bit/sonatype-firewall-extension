/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpResponse;

import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AssetsPathsTest
    extends AbstractResourceTest
{
  @Test
  public void testCssUrlsRelative() throws Exception {
    final List<String> CSS_PATHS = Arrays.asList("/assets/audit-report/audit-report.css", "/assets/cip/cip.css",
        "/assets/css/style-1.css", "/assets/css/style-2.css", "/assets/policy/css/cip-loader.css",
        "/assets/version-graph/version.graph.app.css", "/assets/version-graph/viewdetails.css");
    final List<String> failingCssPaths = new ArrayList<>();
    for (String cssPath : CSS_PATHS) {
      HttpResponse response = restRequest().subpath(cssPath).get();
      assertResponseStatus(200, response);
      String body = response.getBodyText();
      if (body.contains("url(/")) {
        failingCssPaths.add(cssPath);
      }
    }
    assertThat(failingCssPaths, is(empty()));
  }
}
