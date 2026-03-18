/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.util.Properties;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class VersionResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(VersionResource.RESOURCE_PATH).anon();
  }

  @Test
  public void testGetVersionInfo_Licensed() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    Properties versionInfo = response.getBody(Properties.class);
    assertThat(versionInfo).isNotNull();
    for (String key : new String[]{"name", "version", "timestamp", "tag", "build"}) {
      assertThat(versionInfo.getProperty(key, "")).as("Testing: " + key + " of " + versionInfo).isNotEmpty();
    }
  }

  @Test
  public void testGetVersionInfo_Unlicensed() throws Exception {
    uninstallLicense();
    testGetVersionInfo_Licensed();
  }
}
