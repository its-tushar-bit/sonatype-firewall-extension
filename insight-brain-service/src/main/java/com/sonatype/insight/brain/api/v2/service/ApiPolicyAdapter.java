/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyOwnerType;
import com.sonatype.insight.brain.model.policy.Policy;

/**
 * @since 1.12.0
 */
class ApiPolicyAdapter
{
  static List<ApiPolicyDTO> convert(Collection<Policy> policies, ApiPolicyOwnerType ownerType) {
    List<ApiPolicyDTO> apiPolicyDTOs = new ArrayList<>(policies.size());
    for (Policy policy : policies) {
      apiPolicyDTOs.add(convert(policy, ownerType));
    }
    return apiPolicyDTOs;
  }

  static ApiPolicyDTO convert(Policy policy, ApiPolicyOwnerType ownerType) {
    ApiPolicyDTO policyDTO = new ApiPolicyDTO();
    policyDTO.id = policy.getId();
    policyDTO.name = policy.getName();
    policyDTO.ownerId = policy.getOwnerId();
    policyDTO.ownerType = ownerType;
    policyDTO.threatLevel = policy.getThreatLevel();
    policyDTO.policyType = policy.getThreatCategory().getName();
    return policyDTO;
  }
}
