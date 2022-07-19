/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.security.CurrentUser;

/**
 * @since 1.60
 */
public class RepositoryPolicyViolationLogger
    extends AbstractPolicyViolationLogger<RepositoryPolicyViolation>
{
  private final Repository repository;

  public RepositoryPolicyViolationLogger(
      boolean licensed,
      Date logTimestamp,
      Repository repository,
      CurrentUser currentUser)
  {
    super(licensed, logTimestamp, currentUser);
    this.repository = repository;
  }

  @Override
  protected PolicyViolationLogDTO createPolicyViolationLogDTO(
      PolicyViolationData<RepositoryPolicyViolation> policyViolationData)
  {
    PolicyViolationLogDTO policyViolationLogDTO = super.createPolicyViolationLogDTO(policyViolationData);

    if (!PolicyViolationLogEvent.CLEAR.equals(policyViolationData.policyViolationLogEvent)) {
      policyViolationLogDTO.stageTypeId = StageTypes.PROXY.getId();
    }
    RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();
    policyViolationLogDTO.repositoryManagerId = repository.getRepositoryManagerId();
    policyViolationLogDTO.repositoryManagerInstanceId =
        repositoryManagerDAO.getById(repository.getRepositoryManagerId()).getInstanceId();
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
