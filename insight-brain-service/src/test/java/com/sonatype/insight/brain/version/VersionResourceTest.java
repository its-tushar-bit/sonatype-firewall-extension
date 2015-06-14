/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VersionResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetVersionInfo_Licensed() throws Exception {
    HttpResponse response = restRequest().path(VersionResource.SERVICE_PATH).anon().get();
    assertResponseStatus(200, response);
    Map<String, String> versionInfo = response.getBody(Map.class);
    assertNotNull(versionInfo);
    for (String key : new String[] { "name", "version", "timestamp", "tag", "build" }) {
      assertTrue("Testing: " + key + " of " + versionInfo.toString(), versionInfo.get(key).length() > 0);
    }
  }

  @Test
  public void testGetVersionInfo_Unlicensed() throws Exception {
    uninstallLicense();
    testGetVersionInfo_Licensed();
  }

}
