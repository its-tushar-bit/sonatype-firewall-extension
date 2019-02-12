/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.features.Feature;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.LicenseListener;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class PolicyViolationLoggerFactory
    implements Managed, LicenseListener
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationLoggerFactory.class);

  private final CLMLicenseManager licenseManager;

  @Inject
  public PolicyViolationLoggerFactory(CLMLicenseManager licenseManager) {
    this.licenseManager = licenseManager;
    licenseManager.addListener(this);
  }

  public OrganizationPolicyViolationLogger newLogger(Date logTimestamp, Organization organization) {
    return new OrganizationPolicyViolationLogger(
        licenseManager.hasFeature(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS), logTimestamp, organization);
  }

  public ApplicationPolicyViolationLogger newLogger(Date logTimestamp, Application application) {
    return new ApplicationPolicyViolationLogger(
        licenseManager.hasFeature(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS), logTimestamp, application);
  }

  public RepositoryPolicyViolationLogger newLogger(Date logTimestamp, Repository repository) {
    return new RepositoryPolicyViolationLogger(
        licenseManager.hasFeature(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES), logTimestamp, repository);
  }

  private void logPotentialMisconfiguration() {
    if (LoggerFactory.getLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME).isInfoEnabled()
        && !licenseManager.hasFeature(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS)) {
      if (!licenseManager.hasFeature(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES)) {
        log.warn(
            "Disabling policy violation logging for logger {}."
                + " Installed license does not support policy violation logging.",
            AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
      }
      else {
        log.info(
            "Disabling application policy violation logging for logger {}."
                + " Installed license does not support policy violation logging for applications.",
            AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
      }
    }
  }

  @Override
  public void start() {
    logPotentialMisconfiguration();
  }

  @Override
  public void stop() {
    // noop
  }

  @Override
  public void licenseChanged() {
    logPotentialMisconfiguration();
  }
}
