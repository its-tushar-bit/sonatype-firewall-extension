/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ProprietaryConfigResourceTest
    extends AbstractResourceTest
{
  private String getServiceUrl() {
    return getRestBaseUrl() + ProprietaryConfigResource.SERVICE_PATH;
  }

  @After
  public void cleanup() throws Exception {
    File configFile = new File(brain.getDataDir(), "proprietary.json");
    assertTrue(configFile.delete() || !configFile.exists());
  }

  @Test
  public void testGet_InitialConfig() throws Exception {
    Response response = RestAccess.get(getServiceUrl());
    assertResponseStatus(200, response);
    ProprietaryConfig config = JsonHelpers.fromJson(response.getResponseBody(), ProprietaryConfig.class);
    assertNotNull(config);
    assertEquals(0, config.getPackages().size());
  }

  @Test
  public void testUpdate() throws Exception {
    List<String> packages = Arrays.asList("org.sonatype", "com.sonatype");
    ProprietaryConfig config = new ProprietaryConfig();
    config.setPackages(packages);
    Response response = RestAccess.put(getServiceUrl(), JsonHelpers.asJson(config));
    assertResponseStatus(204, response);

    response = RestAccess.get(getServiceUrl());
    assertResponseStatus(200, response);
    config = JsonHelpers.fromJson(response.getResponseBody(), ProprietaryConfig.class);
    assertEquals(packages, config.getPackages());
  }

}
