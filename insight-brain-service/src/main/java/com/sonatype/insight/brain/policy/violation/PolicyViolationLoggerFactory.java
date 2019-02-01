/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import javax.inject.Inject;
import javax.inject.Named;

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
    // consult license manager for feature state and configure logger appropriately
    return new ApplicationPolicyViolationLogger(licenseManager != null, application);
  }

  public RepositoryPolicyViolationLogger newLogger(Repository repository) {
    return new RepositoryPolicyViolationLogger(licenseManager != null, repository);
  }
}
