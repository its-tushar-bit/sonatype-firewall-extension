/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ComponentH2Test
public class PolicyViolationLoggerFactoryTest
    extends AbstractComponentH2Test
{
  // the rule also ensures the violation logger is generally enabled during tests and reset afterwards
  public LogOutput logOutput = new LogOutput(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME,
      PolicyViolationLoggerFactory.class.getName());

  @Inject
  private PolicyViolationLoggerFactory policyViolationLoggerFactory;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testNewLogger_ForOrganization_FeatureLicensed() {
    testProductLicense.setFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), new Organization()).isEnabled()).isTrue();
  }

  @Test
  public void testNewLogger_ForOrganization_FeatureUnlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), new Organization()).isEnabled()).isFalse();
  }

  @Test
  public void testNewLogger_ForApplication_FeatureLicensed() {
    testProductLicense.setFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), new Application()).isEnabled()).isTrue();
  }

  @Test
  public void testNewLogger_ForApplication_FeatureUnlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), new Application()).isEnabled()).isFalse();
  }

  @Test
  public void testNewLogger_ForHostedRepositoryComponent_FeatureLicensed() {
    testProductLicense.setFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    assertThat(policyViolationLoggerFactory.newLogger(new Date(),
        new HostedRepositoryComponent("repo-id", "path/foo.jar", "hash")))
            .isInstanceOf(HostedRepositoryComponentPolicyViolationLogger.class)
            .satisfies(logger -> assertThat(logger.isEnabled()).isTrue());
  }

  @Test
  public void testNewLogger_ForHostedRepositoryComponent_FeatureUnlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    assertThat(policyViolationLoggerFactory.newLogger(new Date(),
        new HostedRepositoryComponent("repo-id", "path/foo.jar", "hash"))
        .isEnabled()).isFalse();
  }

  @Test
  public void testNewPolicyViolationLogger_ForOwner_DispatchesToApplicationLogger() {
    testProductLicense.setFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    Owner owner = new Application();
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), owner))
        .isInstanceOf(ApplicationPolicyViolationLogger.class);
  }

  @Test
  public void testNewPolicyViolationLogger_ForOwner_DispatchesToHostedRepositoryComponentLogger() {
    testProductLicense.setFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    Owner owner = new HostedRepositoryComponent("repo-id", "path/foo.jar", "hash");
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), owner))
        .isInstanceOf(HostedRepositoryComponentPolicyViolationLogger.class);
  }

  @Test
  public void testNewPolicyViolationLogger_ForOwner_DispatchesToOrganizationLogger() {
    testProductLicense.setFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    Owner owner = new Organization();
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), owner))
        .isInstanceOf(OrganizationPolicyViolationLogger.class);
  }

  @Test
  public void testNewPolicyViolationLogger_ForOwner_UnsupportedType_Throws() {
    Owner owner = new RepositoryManager();
    assertThatThrownBy(() -> policyViolationLoggerFactory.newLogger(new Date(), owner))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void testNewLogger_ForRepository_FeatureLicensed() {
    testProductLicense.setFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), new Repository()).isEnabled()).isTrue();
  }

  @Test
  public void testNewLogger_ForRepository_FeatureUnlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    assertThat(policyViolationLoggerFactory.newLogger(new Date(), new Repository()).isEnabled()).isFalse();
  }

  private void disablePolicyViolationLogger() {
    ((Logger) LoggerFactory.getLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)).setLevel(Level.OFF);
  }

  @Test
  public void testProductLicenseChanged_FeatureUnlicensed_LoggerEnabled() {
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    policyViolationLoggerFactory.productLicenseChanged();
    assertThat(logOutput).atWarnLevel()
        .contains("Disabling policy violation logging for logger com.sonatype.insight.policy.violation. "
            + "Installed license does not support policy violation logging.");
  }

  @Test
  public void testProductLicenseChanged_FeatureUnlicensed_LoggerDisabled() {
    disablePolicyViolationLogger();
    testProductLicense.setMissingFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,
        LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    policyViolationLoggerFactory.productLicenseChanged();
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }

  @Test
  public void testProductLicenseChanged_FeatureLicensed_ForApplication_LoggerEnabled() {
    testProductLicense.setFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    policyViolationLoggerFactory.productLicenseChanged();
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }

  @Test
  public void testProductLicenseChanged_FeatureLicensed_ForRepository_LoggerEnabled() {
    testProductLicense.setFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    policyViolationLoggerFactory.productLicenseChanged();
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atInfoLevel()
        .contains("Disabling application policy violation logging for logger com.sonatype.insight.policy.violation. "
            + "Installed license does not support policy violation logging for applications.");
  }

  @Test
  public void testProductLicenseChanged_FeatureLicensed_ForApplication_LoggerDisabled() {
    disablePolicyViolationLogger();
    testProductLicense.setFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS);
    policyViolationLoggerFactory.productLicenseChanged();
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }

  @Test
  public void testProductLicenseChanged_FeatureLicensed_ForRepository_LoggerDisabled() {
    disablePolicyViolationLogger();
    testProductLicense.setFeatures(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES);
    policyViolationLoggerFactory.productLicenseChanged();
    assertThat(logOutput).atWarnLevel().isEmpty();
    assertThat(logOutput).atAnyLevel().doesNotContain("Installed license does not support policy violation logging");
  }
}
