/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.util.List;

import com.sonatype.insight.test.LogOutput;

import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.validation.ConstraintViolations;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InsightConfigTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(InsightConfig.class);

  @Test
  public void testBaseUrl() {
    InsightConfig config = new InsightConfig();
    assertThat(config.getBaseUrl()).isNull();
    assertThat(config.isValidBaseUrl()).isTrue();

    config.setBaseUrl("https://clm.sonatype.com/");
    assertThat(config.getBaseUrl()).isEqualTo("https://clm.sonatype.com/");
    assertThat(config.isValidBaseUrl()).isTrue();

    config.setBaseUrl("https://clm.sonatype.com");
    assertThat(config.getBaseUrl()).isEqualTo("https://clm.sonatype.com/");
    assertThat(config.isValidBaseUrl()).isTrue();

    config.setBaseUrl("invalid");
    assertThat(config.isValidBaseUrl()).isFalse();
  }

  @Test
  public void testCdnUrl() {
    InsightConfig config = new InsightConfig();
    assertThat(config.getCdnUrl()).isEqualTo("http://cdn.sonatype.com/");
    assertThat(config.isValidCdnUrl()).isTrue();

    config.setCdnUrl("https://clm.sonatype.com/");
    assertThat(config.getCdnUrl()).isEqualTo("https://clm.sonatype.com/");
    assertThat(config.isValidCdnUrl()).isTrue();

    config.setCdnUrl("https://clm.sonatype.com");
    assertThat(config.getCdnUrl()).isEqualTo("https://clm.sonatype.com/");
    assertThat(config.isValidCdnUrl()).isTrue();

    config.setCdnUrl("invalid");
    assertThat(config.isValidCdnUrl()).isFalse();

    config.setCdnUrl(null);
    assertThat(config.isValidCdnUrl()).isFalse();
  }

  @Test
  public void testUserAgentSuffix_NoControlCharactersToBlockHeaderInjection() {
    InsightConfig config = new InsightConfig();
    config.setUserAgentSuffix("\nInjected-Header: Value");
    List<String> errors = ConstraintViolations.format(Validators.newValidatorFactory().getValidator().validate(config));
    assertThat(errors).hasSize(1);
    assertThat(errors.get(0)).contains("userAgentSuffix"); // validator messages are localized...
    config.setUserAgentSuffix("Valid User Agent Suffix (Custom/1.0, Bla)");
    errors = ConstraintViolations.format(Validators.newValidatorFactory().getValidator().validate(config));
    assertThat(errors).isEmpty();
  }

  @Test
  public void testGetDbBackupDir() {
    InsightConfig config = new InsightConfig();
    assertThat(config.getDbBackupDir()).isEqualTo(new File(config.getSonatypeWork(), InsightConfig.DEFAULT_BACKUP_DIR));

    config.setDbBackupDir("");
    assertThat(config.getDbBackupDir()).isEqualTo(new File(config.getSonatypeWork(), InsightConfig.DEFAULT_BACKUP_DIR));

    String relativePath = "abc";
    assertThat(new File(relativePath)).isRelative();
    config.setDbBackupDir(relativePath);
    assertThat(config.getDbBackupDir()).isEqualTo(new File(config.getSonatypeWork(), relativePath));

    String absolutePath = new File("abc").getAbsolutePath();
    assertThat(new File(absolutePath)).isAbsolute();
    config.setDbBackupDir(absolutePath);
    assertThat(config.getDbBackupDir()).isEqualTo(new File(absolutePath));
  }

  @Test
  public void testSupport() {
    InsightConfig config = new InsightConfig();
    assertThat(config.getSupportConfig().getReadLimitBytes()).isEqualTo(SupportConfig.DEFAULT_READ_LIMIT_30MB);

    config.getSupportConfig().setReadLimitBytes(-1);
    assertThat(config.getSupportConfig().getReadLimitBytes()).isEqualTo(-1);
  }

  /**
   * @deprecated The tested method is deprecated.
   */
  @Test
  @Deprecated
  public void testSetAnonymousClientAccessAllowed() {
    InsightConfig config = new InsightConfig();

    config.setAnonymousClientAccessAllowed(true);

    assertThat(logOutput).atWarnLevel()
        .contains("The support for anonymous client access was removed in Nexus IQ Server 72. "
            + "The anonymousClientAccessAllowed configuration option should be removed from the config yml file.");
  }
}
