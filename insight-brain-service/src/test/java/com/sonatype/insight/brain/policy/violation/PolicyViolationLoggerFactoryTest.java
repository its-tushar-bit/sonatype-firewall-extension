/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;

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
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), new Application()).isEnabled()).isTrue();
  }

  @Test
  public void testNewLogger_ForApplication_FeatureUnlicensed() {
    licenseManager.setMissingFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), new Application()).isEnabled()).isFalse();
  }

  @Test
  public void testNewLogger_ForRepository_FeatureLicensed() {
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), new Repository()).isEnabled()).isTrue();
  }

  @Test
  public void testNewLogger_ForRepository_FeatureUnlicensed() {
    licenseManager.setMissingFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), new Repository()).isEnabled()).isFalse();
  }

  /**
   * Tweaking the features of the product license causes notifications to any listener. In some tests, we do not want
   * this callback on the logger factory and solely observe the effects of methods that get explicitly called from the
   * test themselves.
   */
  private void avoidInterferenceFromLicenseChange() {
    licenseManager.removeListener(policyViolationLoggerFactory);
  }

  private void disablePolicyViolationLogger() {
    ((Logger) LoggerFactory.getLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).setLevel(Level.OFF);
  }

  @Test
  public void testStart_FeatureUnlicensed_LoggerEnabled() {
    avoidInterferenceFromLicenseChange();
    licenseManager.setMissingFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,
        Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    policyViolationLoggerFactory.start();
    assertThat(logOutput).atWarnLevel()
        .contains("Disabling policy violation logging for logger com.sonatype.insight.policy.violation. "
            + "Installed license does not support policy violation logging.");
  }

  @Test
  public void testStart_FeatureUnlicensed_LoggerDisabled() {
    disablePolicyViolationLogger();
    avoidInterferenceFromLicenseChange();
    licenseManager.setMissingFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,
        Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    policyViolationLoggerFactory.start();
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }

  @Test
  public void testStart_FeatureLicensed_ForApplication_LoggerEnabled() {
    avoidInterferenceFromLicenseChange();
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    policyViolationLoggerFactory.start();
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }

  @Test
  public void testStart_FeatureLicensed_ForRepository_LoggerEnabled() {
    avoidInterferenceFromLicenseChange();
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    policyViolationLoggerFactory.start();
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atInfoLevel()
        .contains("Disabling application policy violation logging for logger com.sonatype.insight.policy.violation. "
            + "Installed license does not support policy violation logging for applications.");
  }

  @Test
  public void testStart_FeatureLicensed_ForApplication_LoggerDisabled() {
    disablePolicyViolationLogger();
    avoidInterferenceFromLicenseChange();
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    policyViolationLoggerFactory.start();
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }

  @Test
  public void testStart_FeatureLicensed_ForRepository_LoggerDisabled() {
    disablePolicyViolationLogger();
    avoidInterferenceFromLicenseChange();
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    policyViolationLoggerFactory.start();
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }

  @Test
  public void testLicenseChanged_FeatureUnlicensed_LoggerEnabled() {
    licenseManager.setMissingFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,
        Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    assertThat(logOutput).atWarnLevel()
        .contains("Disabling policy violation logging for logger com.sonatype.insight.policy.violation. "
            + "Installed license does not support policy violation logging.");
  }

  @Test
  public void testLicenseChanged_FeatureUnlicensed_LoggerDisabled() {
    disablePolicyViolationLogger();
    licenseManager.setMissingFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,
        Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }

  @Test
  public void testLicenseChanged_FeatureLicensed_ForApplication_LoggerEnabled() {
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }

  @Test
  public void testLicenseChanged_FeatureLicensed_ForRepository_LoggerEnabled() {
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atInfoLevel()
        .contains("Disabling application policy violation logging for logger com.sonatype.insight.policy.violation. "
            + "Installed license does not support policy violation logging for applications.");
  }

  @Test
  public void testLicenseChanged_FeatureLicensed_ForApplication_LoggerDisabled() {
    disablePolicyViolationLogger();
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }

  @Test
  public void testLicenseChanged_FeatureLicensed_ForRepository_LoggerDisabled() {
    disablePolicyViolationLogger();
    licenseManager.setFeatures(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }
}
