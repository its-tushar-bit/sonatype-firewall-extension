/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiversApplicableToViolationDTO;

/**
 * Resource for API Policy Violation
 */
public interface ApiPolicyViolationResourceV2
{
  ApiApplicationViolationListDTOV2 getPolicyViolations(Set<String> policyIds);

  /**
   * @since 1.86.0
   */
  ApiCrossStageViolationDTOV2 getCrossStagePolicyViolationById(String violationId);

  ApiCrossStageViolationDTOV2 getCrossStagePolicyViolationByConstituentId(String constituentId);

  /**
   * @since 1.98
   */
  ApiPolicyWaiversApplicableToViolationDTO getApplicableWaivers(String violationId);
}
