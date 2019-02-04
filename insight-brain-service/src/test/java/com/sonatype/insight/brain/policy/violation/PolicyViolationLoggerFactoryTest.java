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

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationLoggerFactoryTest
    extends AbstractComponentTest
{
  // the rule ensures the logger is generally enabled
  @Rule
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

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
}
