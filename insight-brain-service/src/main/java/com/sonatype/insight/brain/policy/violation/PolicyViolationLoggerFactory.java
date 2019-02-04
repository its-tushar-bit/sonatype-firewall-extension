/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.features.Feature;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;

@Named
public class PolicyViolationLoggerFactory
{
  private final CLMLicenseManager licenseManager;

  @Inject
  public PolicyViolationLoggerFactory(CLMLicenseManager licenseManager) {
    this.licenseManager = licenseManager;
  }

  public ApplicationPolicyViolationLogger newLogger(Application application) {
    return new ApplicationPolicyViolationLogger(
        licenseManager.hasFeature(Feature.POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS), application);
  }

  public RepositoryPolicyViolationLogger newLogger(Repository repository) {
    return new RepositoryPolicyViolationLogger(
        licenseManager.hasFeature(Feature.POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES), repository);
  }
}
