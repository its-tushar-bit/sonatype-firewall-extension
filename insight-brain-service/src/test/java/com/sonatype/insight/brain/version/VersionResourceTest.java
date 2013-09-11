/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.util.Map;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VersionResourceTest
    extends AbstractResourceTest
{

  private String getServiceURL() {
    return getRestBaseUrl() + VersionResource.SERVICE_PATH;
  }

  @Test
  public void testGetVersionInfo_Licensed() throws Exception {
    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    Map<?, ?> versionInfo = JsonHelpers.fromJson(response.getResponseBody(), Map.class);
    assertNotNull(versionInfo);
    for (String key : new String[] { "name", "version", "timestamp", "tag" }) {
      assertTrue(key, versionInfo.get(key).toString().length() > 0);
    }
  }

  @Test
  public void testGetVersionInfo_Unlicensed() throws Exception {
    uninstallLicense();
    testGetVersionInfo_Licensed();
  }

}
