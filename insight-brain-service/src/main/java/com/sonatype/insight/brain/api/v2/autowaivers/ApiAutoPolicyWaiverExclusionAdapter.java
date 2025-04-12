/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionResponseDTO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;

public class ApiAutoPolicyWaiverExclusionAdapter
{
  public static ApiAutoPolicyWaiverExclusionResponseDTO convertToDTO(
      final Owner owner,
      final AutoPolicyWaiverExclusion autoPolicyWaiverExclusion)
  {
    ApiAutoPolicyWaiverExclusionResponseDTO responseDTO = convertToDTO(autoPolicyWaiverExclusion);
    responseDTO.ownerName = owner.getName();
    responseDTO.ownerType = owner.getType().toString();
    responseDTO.ownerPublicId = owner.getPublicId();
    return responseDTO;
  }

  public static ApiAutoPolicyWaiverExclusionResponseDTO convertToDTO(
      final AutoPolicyWaiverExclusion autoPolicyWaiverExclusion)
  {
    if (autoPolicyWaiverExclusion == null) {
      return null;
    }

    final ApiAutoPolicyWaiverExclusionResponseDTO dto = new ApiAutoPolicyWaiverExclusionResponseDTO();

    dto.autoPolicyWaiverExclusionId = autoPolicyWaiverExclusion.getId();
    dto.ownerId = autoPolicyWaiverExclusion.getOwnerId();
    dto.creatorId = autoPolicyWaiverExclusion.getCreatorId();
    dto.creatorName = autoPolicyWaiverExclusion.getCreatorName();
    dto.createTime = autoPolicyWaiverExclusion.getCreateTime();
    dto.autoPolicyWaiverId = autoPolicyWaiverExclusion.getAutoPolicyWaiverId();
    dto.hash = autoPolicyWaiverExclusion.getHash();
    dto.scanId = autoPolicyWaiverExclusion.getScanId();
    dto.componentMatchStrategy = autoPolicyWaiverExclusion.getComponentMatchStrategy();
    dto.policyViolationId = autoPolicyWaiverExclusion.getPolicyViolationId();
    dto.threatLevel = autoPolicyWaiverExclusion.getThreatLevel();
    dto.policyName = autoPolicyWaiverExclusion.getPolicyName();
    dto.componentDisplayName = autoPolicyWaiverExclusion.getComponentDisplayName();
    dto.vulnerabilityIdentifiers = autoPolicyWaiverExclusion.getVulnerabilityIdentifiers();
    dto.policyId = autoPolicyWaiverExclusion.getPolicyId();
    dto.componentIdentifier = autoPolicyWaiverExclusion.getComponentIdentifier();
    dto.constraintFacts = autoPolicyWaiverExclusion.getConstraintFacts();
    return dto;
  }
}
