/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;

/**
 * @since 1.107.0
 */
public class ApiPolicyViolationAdapter
{
  public static ApiPolicyViolationDTOV2 convert(
      final ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation)
  {
    ApiPolicyViolationDTOV2 policyViolationDTOV2 = new ApiPolicyViolationDTOV2();
    policyViolationDTOV2.policyId = proxyRepositoryPolicyViolation.getPolicyId();
    policyViolationDTOV2.policyName = proxyRepositoryPolicyViolation.getPolicyName();
    policyViolationDTOV2.threatLevel = proxyRepositoryPolicyViolation.getThreatLevel();
    policyViolationDTOV2.policyViolationId = proxyRepositoryPolicyViolation.getId();
    policyViolationDTOV2.openTime = proxyRepositoryPolicyViolation.getOpenTime();
    policyViolationDTOV2.waiveTime = proxyRepositoryPolicyViolation.getWaiveTime();
    policyViolationDTOV2.constraintViolations = PolicyViolationAdapter.convert(proxyRepositoryPolicyViolation);
    return policyViolationDTOV2;
  }
}
