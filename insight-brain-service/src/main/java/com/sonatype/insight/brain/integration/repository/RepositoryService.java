/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.license.model.LicensedFeature;

/**
 * @since 1.17.0
 */
@Named
@Singleton
public class RepositoryService extends AbstractRepositoryService
{
  @Inject
  public RepositoryService(RepositoryPolicyEvaluator repositoryPolicyEvaluator,
                           ProductLicense productLicense,
                           HdsClient hdsClient,
                           PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    super(repositoryPolicyEvaluator, productLicense, hdsClient, policyViolationLoggerFactory, LicensedFeature.FIREWALL);
  }
}
