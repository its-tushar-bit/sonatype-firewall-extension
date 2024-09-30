/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;

public class ApiAutoPolicyWaiverAdapter
{
  public static ApiAutoPolicyWaiverDTO convertToDTO(final AutoPolicyWaiver autoPolicyWaiver) {
    if (autoPolicyWaiver == null) {
      return null;
    }

    final ApiAutoPolicyWaiverDTO  dto = new ApiAutoPolicyWaiverDTO();

    dto.autoPolicyWaiverId = autoPolicyWaiver.getId();
    dto.ownerId = autoPolicyWaiver.getOwnerId();
    dto.threatLevel = autoPolicyWaiver.getThreatLevel();
    dto.reachable = autoPolicyWaiver.isReachable();
    dto.pathForward = autoPolicyWaiver.hasPathForward();
    dto.creatorId = autoPolicyWaiver.getCreatorId();
    dto.creatorName = autoPolicyWaiver.getCreatorName();
    dto.createTime = autoPolicyWaiver.getCreateTime();

    return dto;
  }
}
