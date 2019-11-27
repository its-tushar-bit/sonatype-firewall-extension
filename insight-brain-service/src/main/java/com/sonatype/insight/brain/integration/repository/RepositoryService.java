/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.base.Suppliers;

/**
 * @since 1.17.0
 */
@Named
@Singleton
public class RepositoryService extends AbstractRepositoryService
{
  private final Supplier<FirewallIgnorePatterns> ignorePatternsCache;

  @Inject
  public RepositoryService(RepositoryPolicyEvaluator repositoryPolicyEvaluator,
                           ProductLicense productLicense,
                           HdsClient hdsClient,
                           PolicyViolationLoggerFactory policyViolationLoggerFactory)
  {
    super(repositoryPolicyEvaluator, productLicense, hdsClient, policyViolationLoggerFactory, LicensedFeature.FIREWALL);
    this.ignorePatternsCache = Suppliers.memoizeWithExpiration(this::fetchFirewallIgnorePatterns, 6, TimeUnit.HOURS);
  }

  public FirewallIgnorePatterns getIgnorePatterns() {
    return ignorePatternsCache.get();
  }

  private FirewallIgnorePatterns fetchFirewallIgnorePatterns() {
    try {
      return hdsClient.get(FirewallIgnorePatterns.class, HDS_IGNORE_PATTERNS_PATH);
    }
    catch (BadGatewayException e) {
      throw new RuntimeException("Failed to get ignore patterns from remote: " + e.getMessage(), e);
    }
  }
}
