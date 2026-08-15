/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Properties;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.version.VersionResource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 port of {@code VersionResourceTest}.
 */
@IqH2Test
class IqH2VersionResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(VersionResource.RESOURCE_PATH).anon();
  }

  @Test
  void testGetVersionInfo_Licensed() throws Exception {
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    Properties versionInfo = response.getBody(Properties.class);
    assertThat(versionInfo).isNotNull();
    for (String key : new String[]{"name", "version", "timestamp", "tag", "build"}) {
      assertThat(versionInfo.getProperty(key, "")).as("Testing: " + key + " of " + versionInfo).isNotEmpty();
    }
  }

  @Test
  void testGetVersionInfo_Unlicensed() throws Exception {
    ctx.uninstallLicense();
    testGetVersionInfo_Licensed();
  }
}
