/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentWaiversDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader;

/**
 * @since 1.75
 */
public class ApiComponentsWithWaiversReportingService
{
@SuppressWarnings("PMD")
  private final PolicyViolationLoader policyViolationLoader;

  @Inject
  public ApiComponentsWithWaiversReportingService(PolicyViolationLoader policyViolationLoader) {
    this.policyViolationLoader = policyViolationLoader;
  }

  public ApiComponentWaiversDTO getComponentsWithWaivers() {
    return null;
  }
}
