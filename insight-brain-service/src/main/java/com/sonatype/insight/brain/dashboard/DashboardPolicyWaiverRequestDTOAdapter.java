/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Map;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;

public class DashboardPolicyWaiverRequestDTOAdapter
{
  private final Map<String, Policy> policiesById;

  private final Map<String, Owner> ownersById;

  private final boolean includeDetails;

  public DashboardPolicyWaiverRequestDTOAdapter(
      Map<String, Policy> policiesById,
      Map<String, Owner> ownersById,
      boolean includeDetails)
  {
    this.policiesById = policiesById;
    this.ownersById = ownersById;
    this.includeDetails = includeDetails;
  }

  public DashboardPolicyWaiverRequestDTO toDto(
      PolicyWaiverRequest policyWaiverRequest,
      PolicyWaiverReason policyWaiverReason)
  {
    DashboardPolicyWaiverRequestDTO dto = new DashboardPolicyWaiverRequestDTO();
    dto.id = policyWaiverRequest.getId();
    dto.threatLevel = policiesById.get(policyWaiverRequest.getPolicyId()).getThreatLevel();
    dto.requestTime = policyWaiverRequest.getRequestTime();
    dto.expiryTime = policyWaiverRequest.getExpiryTime();
    dto.policyId = policyWaiverRequest.getPolicyId();
    dto.policyName = policiesById.get(policyWaiverRequest.getPolicyId()).getName();
    dto.ownerId = policyWaiverRequest.getOwnerId();
    dto.ownerName = ownersById.get(policyWaiverRequest.getOwnerId()).getName();
    dto.ownerType =
        ScopeOwnerUtils.getScopeOwnerType(ownersById.get(policyWaiverRequest.getOwnerId()).getType(), dto.ownerId);
    dto.componentMatchStrategy = policyWaiverRequest.getComponentMatchStrategy();
    dto.hash = policyWaiverRequest.getHash();
    dto.componentUpgradeAvailable = policyWaiverRequest.isComponentUpgradeAvailable();
    dto.isExpireWhenRemediationAvailable = policyWaiverRequest.isExpireWhenRemediationAvailable();

    if (policyWaiverRequest.getComponentIdentifier() != null) {
      dto.componentIdentifier = ApiComponentIdentifierDTOV2
          .fromComponentIdentifier(policyWaiverRequest.getComponentIdentifier());
    }
    dto.status = policyWaiverRequest.getStatus();
    dto.requesterId = policyWaiverRequest.getRequesterId();
    dto.requesterName = policyWaiverRequest.getRequesterName();

    if (includeDetails) {
      dto.comment = policyWaiverRequest.getComment();
      dto.constraintFacts = policyWaiverRequest.getConstraintFacts();
      dto.policyWaiverReason = policyWaiverReason;
    }

    return dto;
  }
}
