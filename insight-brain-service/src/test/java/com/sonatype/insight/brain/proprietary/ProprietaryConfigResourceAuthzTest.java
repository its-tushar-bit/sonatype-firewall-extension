/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ProprietaryConfigResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @After
  public void cleanup() throws Exception {
    File configFile = new File(brain.getDataDir(), "proprietary.json");
    assertTrue(configFile.delete() || !configFile.exists());
  }

  @Test
  public void testUpdate() throws Exception {
    List<String> packages = Arrays.asList("org.sonatype", "com.sonatype");
    ProprietaryConfig config = new ProprietaryConfig();
    config.setPackages(packages);

    grantAdminPermission();

    String url = getRestUrl(ProprietaryConfigResource.SERVICE_PATH + "/update");

    testAuthzPut(url, toJson(config), 204);
  }
}
