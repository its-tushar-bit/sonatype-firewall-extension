/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentTransitivePolicyViolationsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiversApplicableToViolationDTO;
import com.sonatype.insight.brain.model.OwnerType;

/**
 * Resource for API Policy Violation
 */
public interface ApiPolicyViolationResourceV2
{
  ApiApplicationViolationListDTOV2 getPolicyViolations(
      Set<String> policyIds,
      String openTimeBefore,
      String openTimeAfter);

  /**
   * @since 1.86.0
   */
  ApiCrossStageViolationDTOV2 getCrossStagePolicyViolationById(String violationId);

  ApiCrossStageViolationDTOV2 getCrossStagePolicyViolationByConstituentId(String constituentId);

  /**
   * @since 1.98
   */
  ApiPolicyWaiversApplicableToViolationDTO getApplicableWaivers(String violationId);

  /**
   * @since 1.115
   */
  ApiComponentTransitivePolicyViolationsDTO getTransitivePolicyViolationsByOwnerStageComponent(
      OwnerType ownerType,
      String ownerId,
      String stageId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash);

  /**
   * @since 1.117
   */
  ApiComponentTransitivePolicyViolationsDTO getTransitivePolicyViolationsByAppScanComponent(
      OwnerType ownerType,
      String ownerId,
      String scanId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash);
}
