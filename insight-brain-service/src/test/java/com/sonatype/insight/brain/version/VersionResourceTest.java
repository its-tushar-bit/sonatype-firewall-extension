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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    assertNotNull(versionInfo);
    for (String key : new String[] { "name", "version", "timestamp", "tag", "build" }) {
      assertTrue("Testing: " + key + " of " + versionInfo, !versionInfo.getProperty(key, "").isEmpty());
    }
  }

  @Test
  public void testGetVersionInfo_Unlicensed() throws Exception {
    uninstallLicense();
    testGetVersionInfo_Licensed();
  }

}
