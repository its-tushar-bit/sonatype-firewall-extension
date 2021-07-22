/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;

/**
 * @since 1.107.0
 */
@Named
public class ApiPolicyViolationAdapter
{
  public ApiPolicyViolationDTOV2 convert(
      final RepositoryPolicyViolation repositoryPolicyViolation)
  {
    ApiPolicyViolationDTOV2 policyViolationDTOV2 = new ApiPolicyViolationDTOV2();
    policyViolationDTOV2.policyId = repositoryPolicyViolation.getPolicyId();
    policyViolationDTOV2.policyName = repositoryPolicyViolation.getPolicyName();
    policyViolationDTOV2.threatLevel = repositoryPolicyViolation.getThreatLevel();
    policyViolationDTOV2.policyViolationId = repositoryPolicyViolation.getId();
    policyViolationDTOV2.constraintViolations = PolicyViolationAdapter.convert(repositoryPolicyViolation);
    return policyViolationDTOV2;
  }
}
