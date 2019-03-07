/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.features.Feature;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;

/**
 * @since 1.17.0
 */
@Named
public class RepositoryService extends AbstractRepositoryService
{
  @Inject
  public RepositoryService(RepositoryPolicyEvaluator repositoryPolicyEvaluator,
                           CLMLicenseManager licenseManager,
                           HdsClient hdsClient,
                           PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    super(repositoryPolicyEvaluator, licenseManager, hdsClient, policyViolationLoggerFactory);
  }

  @Override
  protected void checkLicenseFeature() {
    if (!licenseManager.hasFeature(Feature.FIREWALL)) {
      throw new InvalidLicenseException();
    }
  }
}
