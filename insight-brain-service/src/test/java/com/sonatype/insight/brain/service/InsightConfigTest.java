/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class InsightConfigTest
{
  @Test
  public void testBaseUrl() {
    InsightConfig config = new InsightConfig();
    Assert.assertEquals(null, config.getBaseUrl());
    Assert.assertEquals(true, config.isValidBaseUrl());

    config.setBaseUrl("https://clm.sonatype.com/");
    Assert.assertEquals("https://clm.sonatype.com/", config.getBaseUrl());
    Assert.assertEquals(true, config.isValidBaseUrl());

    config.setBaseUrl("https://clm.sonatype.com");
    Assert.assertEquals("https://clm.sonatype.com/", config.getBaseUrl());
    Assert.assertEquals(true, config.isValidBaseUrl());

    config.setBaseUrl("invalid");
    Assert.assertEquals(false, config.isValidBaseUrl());
  }

  @Test
  public void testCdnUrl() {
    InsightConfig config = new InsightConfig();
    Assert.assertEquals("http://cdn.sonatype.com/", config.getCdnUrl());
    Assert.assertEquals(true, config.isValidCdnUrl());

    config.setCdnUrl("https://clm.sonatype.com/");
    Assert.assertEquals("https://clm.sonatype.com/", config.getCdnUrl());
    Assert.assertEquals(true, config.isValidCdnUrl());

    config.setCdnUrl("https://clm.sonatype.com");
    Assert.assertEquals("https://clm.sonatype.com/", config.getCdnUrl());
    Assert.assertEquals(true, config.isValidCdnUrl());

    config.setCdnUrl("invalid");
    Assert.assertEquals(false, config.isValidCdnUrl());

    config.setCdnUrl(null);
    Assert.assertEquals(false, config.isValidCdnUrl());
  }
}
