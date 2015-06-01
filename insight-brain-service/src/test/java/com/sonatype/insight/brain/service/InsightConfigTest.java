/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.util.List;

import com.yammer.dropwizard.validation.Validator;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

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

  @Test
  public void testUserAgentSuffix_NoControlCharactersToBlockHeaderInjection() {
    Validator validator = new Validator();
    InsightConfig config = new InsightConfig();
    config.setUserAgentSuffix("\nInjected-Header: Value");
    List<String> errors = validator.validate(config);
    assertThat(errors, hasSize(1));
    assertThat(errors.get(0), containsString("userAgentSuffix")); // validator messages are localized...
    config.setUserAgentSuffix("Valid User Agent Suffix (Custom/1.0, Bla)");
    errors = validator.validate(config);
    assertThat(errors, hasSize(0));
  }

  @Test
  public void testGetDbBackupDir() {
    InsightConfig config = new InsightConfig();
    assertThat(config.getDbBackupDir(), is(new File(config.getSonatypeWork(), InsightConfig.DEFAULT_BACKUP_DIR)));

    config.setDbBackupDir("");
    assertThat(config.getDbBackupDir(), is(new File(config.getSonatypeWork(), InsightConfig.DEFAULT_BACKUP_DIR)));

    String relativePath = "abc";
    assertThat(new File(relativePath).isAbsolute(), is(false));
    config.setDbBackupDir(relativePath);
    assertThat(config.getDbBackupDir(), is(new File(config.getSonatypeWork(), relativePath)));

    String absolutePath = new File("abc").getAbsolutePath();
    assertThat(new File(absolutePath).isAbsolute(), is(true));
    config.setDbBackupDir(absolutePath);
    assertThat(config.getDbBackupDir(), is(new File(absolutePath)));
  }
}
