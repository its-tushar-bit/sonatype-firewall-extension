/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;

public class ApiAutoPolicyWaiverAdapter
{
  public static ApiAutoPolicyWaiverDTO convertToDTO(final AutoPolicyWaiver autoPolicyWaiver) {
    if (autoPolicyWaiver == null) {
      return null;
    }

    final ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();

    dto.autoPolicyWaiverId = autoPolicyWaiver.getId();
    dto.ownerId = autoPolicyWaiver.getOwnerId();
    dto.threatLevel = autoPolicyWaiver.getThreatLevel();
    dto.reachability = autoPolicyWaiver.hasReachability();
    dto.pathForward = autoPolicyWaiver.hasPathForward();
    dto.creatorId = autoPolicyWaiver.getCreatorId();
    dto.creatorName = autoPolicyWaiver.getCreatorName();
    dto.createTime = autoPolicyWaiver.getCreateTime();
    dto.scopesOperatorAny = autoPolicyWaiver.getScopesOperatorAny();
    return dto;
  }

  public static ApiAutoPolicyWaiverDTO convertToDTO(final AutoPolicyWaiver autoPolicyWaiver, final Owner owner) {
    final ApiAutoPolicyWaiverDTO dto = convertToDTO(autoPolicyWaiver);
    if (dto != null && owner != null) {
      dto.ownerType = ScopeOwnerUtils.getScopeOwnerType(owner.getType(), owner.getId());
      dto.ownerName = owner.getName();
      dto.publicId = owner.getPublicId();
    }
    return dto;
  }
}
