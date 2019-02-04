/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;

public class RepositoryPolicyViolationLogger
    extends AbstractPolicyViolationLogger<RepositoryPolicyViolation>
{
  private final Repository repository;

  public RepositoryPolicyViolationLogger(boolean licensed, Repository repository) {
    super(licensed);

    this.repository = repository;
  }

  @Override
  protected PolicyViolationLogDTO createPolicyViolationLogDTO(PolicyViolationLogEvent policyViolationLogEvent,
                                                              RepositoryPolicyViolation policyViolation)
  {
    PolicyViolationLogDTO policyViolationLogDTO =
        super.createPolicyViolationLogDTO(policyViolationLogEvent, policyViolation);

    policyViolationLogDTO.eventTimestamp = formatTimestamp(policyViolation.getTime());
    policyViolationLogDTO.stageTypeId = StageTypes.PROXY.getId();
    policyViolationLogDTO.repositoryId = repository.getId();
    policyViolationLogDTO.repositoryPublicId = repository.getPublicId();
    return policyViolationLogDTO;
  }

  @Override
  protected boolean shouldIncludeStagePolicyAction(PolicyViolationLogEvent policyViolationLogEvent,
                                                   RepositoryPolicyViolation policyViolation)
  {
    return super.shouldIncludeStagePolicyAction(policyViolationLogEvent, policyViolation) &&
        !policyViolation.isWaived();
  }
}
