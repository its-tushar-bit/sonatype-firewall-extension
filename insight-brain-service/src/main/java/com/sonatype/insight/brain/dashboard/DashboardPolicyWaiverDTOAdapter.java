/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Map;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;

public class DashboardPolicyWaiverDTOAdapter
{
  private final Map<String, Policy> policiesById;

  private final Map<String, Owner> ownersById;

  private final boolean includeDetails;

  public DashboardPolicyWaiverDTOAdapter(
      Map<String, Policy> policiesById,
      Map<String, Owner> ownersById,
      final boolean includeDetails)
  {
    this.policiesById = policiesById;
    this.ownersById = ownersById;
    this.includeDetails = includeDetails;
  }

  public DashboardPolicyWaiverDTO toDto(PolicyWaiver policyWaiver, PolicyWaiverReason policyWaiverReason) {
    DashboardPolicyWaiverDTO dto = new DashboardPolicyWaiverDTO();
    dto.id = policyWaiver.getId();
    dto.threatLevel = policiesById.get(policyWaiver.getPolicyId()).getThreatLevel();
    dto.createTime = policyWaiver.getCreateTime();
    dto.expiryTime = policyWaiver.getExpiryTime();
    dto.policyId = policyWaiver.getPolicyId();
    dto.policyName = policiesById.get(policyWaiver.getPolicyId()).getName();
    dto.ownerId = policyWaiver.getOwnerId();
    dto.ownerName = ownersById.get(policyWaiver.getOwnerId()).getName();
    dto.ownerType = ScopeOwnerUtils.getScopeOwnerType(ownersById.get(policyWaiver.getOwnerId()).getType(), dto.ownerId);
    dto.componentMatchStrategy = policyWaiver.getComponentMatchStrategy();
    dto.hash = policyWaiver.getHash();
    dto.componentUpgradeAvailable = policyWaiver.isComponentUpgradeAvailable();
    dto.isAutoWaiver = false;
    dto.isExpireWhenRemediationAvailable = policyWaiver.isExpireWhenRemediationAvailable();
    dto.forContainerImage = policyWaiver.isForContainerImage();

    if (policyWaiver.getComponentIdentifier() != null) {
      dto.componentIdentifier = ApiComponentIdentifierDTOV2
          .fromComponentIdentifier(policyWaiver.getComponentIdentifier());
    }

    if (includeDetails) {
      dto.comment = policyWaiver.getComment();
      dto.constraintFacts = policyWaiver.getConstraintFacts();
      dto.creatorId = policyWaiver.getCreatorId();
      dto.creatorName = policyWaiver.getCreatorName();
      dto.policyWaiverReason = policyWaiverReason;
    }

    return dto;
  }

  public DashboardPolicyWaiverDTO toDto(AutoPolicyWaiver autoPolicyWaiver) {
    DashboardPolicyWaiverDTO dto = new DashboardPolicyWaiverDTO();
    dto.id = autoPolicyWaiver.getId();
    dto.threatLevel = autoPolicyWaiver.getThreatLevel();
    dto.createTime = autoPolicyWaiver.getCreateTime();
    dto.ownerId = autoPolicyWaiver.getOwnerId();
    dto.ownerName = ownersById.get(autoPolicyWaiver.getOwnerId()).getName();
    dto.ownerType =
        ScopeOwnerUtils.getScopeOwnerType(ownersById.get(autoPolicyWaiver.getOwnerId()).getType(), dto.ownerId);
    dto.componentMatchStrategy = ComponentMatcherStrategyForWaiver.DEFAULT;
    dto.componentUpgradeAvailable = false;
    dto.isAutoWaiver = true;
    dto.isExpireWhenRemediationAvailable = false;

    if (includeDetails) {
      dto.creatorId = autoPolicyWaiver.getCreatorId();
      dto.creatorName = autoPolicyWaiver.getCreatorName();
      dto.comment = "";
      dto.constraintFacts = null;
    }

    return dto;
  }
}
