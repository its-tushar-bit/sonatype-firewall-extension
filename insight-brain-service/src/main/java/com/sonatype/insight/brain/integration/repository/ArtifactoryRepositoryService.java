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

@Named
public class ArtifactoryRepositoryService extends AbstractRepositoryService
{
  @Inject
  public ArtifactoryRepositoryService(RepositoryPolicyEvaluator repositoryPolicyEvaluator,
                                      CLMLicenseManager licenseManager,
                                      HdsClient hdsClient,
                                      PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    super(repositoryPolicyEvaluator, licenseManager, hdsClient, policyViolationLoggerFactory);
  }

  @Override
  protected void checkLicenseFeature() {
    if (!licenseManager.hasFeature(Feature.FIREWALL_FOR_ARTIFACTORY)) {
      throw new InvalidLicenseException();
    }
  }
}
