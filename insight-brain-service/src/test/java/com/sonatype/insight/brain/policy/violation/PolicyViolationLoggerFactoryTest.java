/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestLicenseManager;
import com.sonatype.insight.brain.features.Feature;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationLoggerFactoryTest
    extends AbstractComponentTest
{
  // the rule also ensures the violation logger is generally enabled during tests and reset afterwards
  @Rule
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME,
      PolicyViolationLoggerFactory.class.getName());

  @Inject
  private PolicyViolationLoggerFactory policyViolationLoggerFactory;

  @Inject
  private TestLicenseManager licenseManager;

  @Test
  public void testNewLogger_ForApplication_FeatureLicensed() {
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    assertThat(policyViolationLoggerFactory.newLogger(new Application()).isEnabled()).isTrue();
  }

  @Test
  public void testNewLogger_ForApplication_FeatureUnlicensed() {
    licenseManager.setMissingFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    assertThat(policyViolationLoggerFactory.newLogger(new Application()).isEnabled()).isFalse();
  }

  @Test
  public void testNewLogger_ForRepository_FeatureLicensed() {
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    assertThat(policyViolationLoggerFactory.newLogger(new Repository()).isEnabled()).isTrue();
  }

  @Test
  public void testNewLogger_ForRepository_FeatureUnlicensed() {
    licenseManager.setMissingFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    assertThat(policyViolationLoggerFactory.newLogger(new Repository()).isEnabled()).isFalse();
  }

  public void testStart_FeatureCompletelyUnlicensed_LoggerEnabled() {
    licenseManager.setMissingFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,
        Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    policyViolationLoggerFactory.start();
    assertThat(logOutput).atWarnLevel().contains("license does not support policy violation logging");
  }

  @Test
  public void testStart_FeatureCompletelyUnlicensed_LoggerDisabled() {
    licenseManager.setMissingFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,
        Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    ((Logger) LoggerFactory.getLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).setLevel(Level.OFF);
    policyViolationLoggerFactory.start();
    assertThat(logOutput).atWarnLevel().isEmpty();
  }

  @Test
  public void testStart_FeatureLicensed_LoggerEnabled() {
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    policyViolationLoggerFactory.start();
    assertThat(logOutput).atWarnLevel().isEmpty();
  }

  @Test
  public void testStart_FeatureLicensed_LoggerDisabled() {
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    ((Logger) LoggerFactory.getLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).setLevel(Level.OFF);
    policyViolationLoggerFactory.start();
    assertThat(logOutput).atWarnLevel().isEmpty();
  }
}
