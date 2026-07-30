/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.60
 */
@Named
@Singleton
public class PolicyViolationLoggerFactory
    implements ProductLicenseListener
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationLoggerFactory.class);

  private final ProductLicense productLicense;

  private final CurrentUser currentUser;

  private final OrganizationDAO organizationDAO;

  private final RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  public PolicyViolationLoggerFactory(
      ProductLicense productLicense,
      final CurrentUser currentUser,
      final OrganizationDAO organizationDAO,
      final RepositoryManagerDAO repositoryManagerDAO)
  {
    this.productLicense = productLicense;
    this.currentUser = currentUser;
    this.organizationDAO = organizationDAO;
    this.repositoryManagerDAO = repositoryManagerDAO;
  }

  public OrganizationPolicyViolationLogger newLogger(Date logTimestamp, Organization organization) {
    return new OrganizationPolicyViolationLogger(
        productLicense.hasFeature(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS), logTimestamp,
        organization, currentUser);
  }

  public ApplicationPolicyViolationLogger newLogger(Date logTimestamp, Application application) {
    Organization organization = organizationDAO.getById(application.getOrganizationId());
    return new ApplicationPolicyViolationLogger(
        productLicense.hasFeature(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS), logTimestamp,
        application, organization, currentUser);
  }

  public ProxyRepositoryPolicyViolationLogger newLogger(Date logTimestamp, Repository repository) {
    return new ProxyRepositoryPolicyViolationLogger(
        productLicense.hasFeature(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES), logTimestamp, repository,
        currentUser, repositoryManagerDAO);
  }

  private void logPotentialMisconfiguration() {
    if (LoggerFactory.getLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME).isInfoEnabled()
        && !productLicense.hasFeature(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS))
    {
      if (!productLicense.hasFeature(LicensedFeature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES)) {
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
  public void productLicenseChanged() {
    logPotentialMisconfiguration();
  }
}
