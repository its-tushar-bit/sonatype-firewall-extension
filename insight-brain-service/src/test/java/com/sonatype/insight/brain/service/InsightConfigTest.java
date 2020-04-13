/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.service.InsightConfig.Feature;
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
  public void testGetSourceControl() {
    // SourceControlConfig depends on InsightConfig settings its SourceControlConfig#setSonatypeWork(String) method
    InsightConfig config = new InsightConfig();
    assertThat(config.getSourceControl()).isNotNull();
    assertThat(config.getSourceControl().getCloneDirectory())
        .isEqualTo(new File(config.getSonatypeWork(), SourceControlConfig.DEFAULT_SOURCE_CONTROL_CLONE_DIR));

    String relativePath = "abc";
    config.setSonatypeWork(relativePath);
    assertThat(config.getSourceControl().getCloneDirectory())
        .isEqualTo(new File(relativePath, SourceControlConfig.DEFAULT_SOURCE_CONTROL_CLONE_DIR));

    // INT-1925 - Emulate partial configuration of just 'sourceControl:'
    config = new InsightConfig();
    config.setSourceControl(null);
    assertThat(config.getSourceControl()).isNotNull();
  }

  @Test
  public void testSupport() {
    InsightConfig config = new InsightConfig();
    assertThat(config.getSupportConfig().getReadLimitBytes()).isEqualTo(SupportConfig.DEFAULT_READ_LIMIT_30MB);

    config.getSupportConfig().setReadLimitBytes(-1);
    assertThat(config.getSupportConfig().getReadLimitBytes()).isEqualTo(-1);
  }

  @Test
  public void testFeatures() {
    InsightConfig config = new InsightConfig();

    // test unspecified feature is enabled
    assertThat(config.getFeatures()).isNull();
    assertThat(config.isFeatureEnabled(Feature.PR_COMMENTING)).isTrue();

    // test feature is enabled, when feature flag is set to true
    Map<String, Boolean> features = new HashMap<>();
    features.put("featureOne", true);
    config.setFeatures(features);
    assertThat(config.getFeatures()).isNotNull();
    assertThat(config.isFeatureEnabled("featureOne")).isTrue();

    // test feature is disabled, when feature flag is set to false
    features.put("featureOne", false);
    assertThat(config.getFeatures()).isNotNull();
    assertThat(config.isFeatureEnabled("featureOne")).isFalse();
  }

  @Test
  public void testExperimentalFeatures() {
    InsightConfig config = new InsightConfig();

    // test unspecified experimental feature is disabled
    assertThat(config.getExperimentalFeatures()).isNull();
    assertThat(config.isExperimentalFeatureEnabled("unspecifiedFeature")).isFalse();

    // test experimental feature is enabled, when feature flag is set to true
    Map<String, Boolean> features = new HashMap<>();
    features.put("featureTwo", true);
    config.setExperimentalFeatures(features);
    assertThat(config.getExperimentalFeatures()).isNotNull();
    assertThat(config.isExperimentalFeatureEnabled("featureTwo")).isTrue();

    // test experimental feature is disabled, when feature flag is set to false
    features.put("featureTwo", false);
    assertThat(config.getExperimentalFeatures()).isNotNull();
    assertThat(config.isExperimentalFeatureEnabled("featureTwo")).isFalse();
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
