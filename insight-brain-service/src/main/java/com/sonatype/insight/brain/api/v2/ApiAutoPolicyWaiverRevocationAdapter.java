/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationDTO;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;

public class ApiAutoPolicyWaiverRevocationAdapter
{
  public static ApiAutoPolicyWaiverRevocationDTO convertToDTO(
      final AutoPolicyWaiverRevocation autoPolicyWaiverRevocation)
  {
    if (autoPolicyWaiverRevocation == null) {
      return null;
    }

    final ApiAutoPolicyWaiverRevocationDTO dto = new ApiAutoPolicyWaiverRevocationDTO();

    dto.autoPolicyWaiverRevocationId = autoPolicyWaiverRevocation.getId();
    dto.ownerId = autoPolicyWaiverRevocation.getOwnerId();
    dto.creatorId = autoPolicyWaiverRevocation.getCreatorId();
    dto.creatorName = autoPolicyWaiverRevocation.getCreatorName();
    dto.createTime = autoPolicyWaiverRevocation.getCreateTime();
    dto.autoPolicyWaiverId = autoPolicyWaiverRevocation.getAutoPolicyWaiverId();
    dto.hash = autoPolicyWaiverRevocation.getHash();
    dto.associatedPackageUrl = autoPolicyWaiverRevocation.getAssociatedPackageUrl();
    dto.scanId = autoPolicyWaiverRevocation.getScanId();
    dto.componentMatchStrategy = autoPolicyWaiverRevocation.getComponentMatchStrategy();
    dto.policyViolationId = autoPolicyWaiverRevocation.getPolicyViolationId();
    dto.threatLevel = autoPolicyWaiverRevocation.getThreatLevel();
    dto.policyName = autoPolicyWaiverRevocation.getPolicyName();
    dto.componentDisplayName = autoPolicyWaiverRevocation.getComponentDisplayName();
    dto.vulnerabilityIdentifiers = autoPolicyWaiverRevocation.getVulnerabilityIdentifiers();
    return dto;
  }
}
