/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v1.dto.ApiPolicyDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiPolicyOwnerType;
import com.sonatype.insight.brain.model.policy.Policy;

/**
 * @since 1.12.0
 */
@Named
@Singleton
public class ApiPolicyAdapter
{
  public List<ApiPolicyDTO> convert(Collection<Policy> policies, ApiPolicyOwnerType ownerType) {
    List<ApiPolicyDTO> apiPolicyDTOs = new ArrayList<>(policies.size());
    for (Policy policy : policies) {
      apiPolicyDTOs.add(convert(policy, ownerType));
    }
    return apiPolicyDTOs;
  }

  public ApiPolicyDTO convert(Policy policy, ApiPolicyOwnerType ownerType) {
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
