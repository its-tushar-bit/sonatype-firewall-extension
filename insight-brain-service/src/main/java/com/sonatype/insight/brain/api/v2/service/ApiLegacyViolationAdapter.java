/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

public class ApiLegacyViolationAdapter
{
  private ApiLegacyViolationAdapter() {
    // static-only
  }

  public static ApiPolicyViolationDTOV2 convert(PolicyViolation policyViolation) {
    ApiPolicyViolationDTOV2 dto = new ApiPolicyViolationDTOV2();
    dto.policyViolationId = policyViolation.getId();
    dto.policyId = policyViolation.getPolicyId();
    dto.policyName = policyViolation.getPolicyName();
    dto.threatLevel = policyViolation.getThreatLevel();
    dto.openTime = policyViolation.getOpenTime();
    dto.waiveTime = policyViolation.getWaiveTime();
    dto.legacyViolationTime = policyViolation.getLegacyViolationTime();
    dto.constraintViolations = PolicyViolationAdapter.convert(policyViolation);
    return dto;
  }
}
