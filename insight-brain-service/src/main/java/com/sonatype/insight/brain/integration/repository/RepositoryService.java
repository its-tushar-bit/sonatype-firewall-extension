/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.error.exception.BadGatewayException;

/**
 * @since 1.17.0
 */
@Named
public class RepositoryService extends AbstractRepositoryService
{
  @Inject
  public RepositoryService(RepositoryPolicyEvaluator repositoryPolicyEvaluator,
                           ProductLicense productLicense,
                           HdsClient hdsClient,
                           PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    super(repositoryPolicyEvaluator, productLicense, hdsClient, policyViolationLoggerFactory);
  }

  @Override
  protected void checkLicenseFeature() {
    if (!productLicense.hasFeature(LicensedFeature.FIREWALL)) {
      throw new InvalidLicenseException();
    }
  }

  FirewallIgnorePatterns getIgnorePatterns() {
    try {
      return hdsClient.get(FirewallIgnorePatterns.class, HDS_IGNORE_PATTERNS_PATH);
    }
    catch (BadGatewayException e) {
      throw new RuntimeException("Failed to get ignore patterns from remote: " + e.getMessage(), e);
    }
  }
}
