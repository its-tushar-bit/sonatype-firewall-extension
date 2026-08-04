/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;

import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.security.CurrentUser;

public class HostedRepositoryComponentPolicyViolationLogger
    extends AbstractPolicyViolationLogger<PolicyViolation>
{
  private HostedRepositoryComponent hostedRepositoryComponent;

  public HostedRepositoryComponentPolicyViolationLogger(
      boolean licensed,
      Date logTimestamp,
      HostedRepositoryComponent hostedRepositoryComponent,
      CurrentUser currentUser)
  {
    super(licensed, logTimestamp, currentUser);

    if (isEnabled()) {
      this.hostedRepositoryComponent = hostedRepositoryComponent;
    }
  }

  @Override
  protected PolicyViolationLogDTO createPolicyViolationLogDTO(
      PolicyViolationData<PolicyViolation> policyViolationData)
  {
    PolicyViolationLogDTO policyViolationLogDTO = super.createPolicyViolationLogDTO(policyViolationData);

    policyViolationLogDTO.stageTypeId =
        policyViolationData.policyViolation == null ? null : policyViolationData.policyViolation.getStageTypeId();
    policyViolationLogDTO.repositoryId = hostedRepositoryComponent.getParentOwnerId();
    return policyViolationLogDTO;
  }

  @Override
  protected boolean shouldIncludeStagePolicyAction(
      PolicyViolationLogEvent policyViolationLogEvent,
      PolicyViolation policyViolation)
  {
    return super.shouldIncludeStagePolicyAction(policyViolationLogEvent, policyViolation) &&
        !policyViolation.isLegacyViolation() && !policyViolation.isWaived();
  }
}
