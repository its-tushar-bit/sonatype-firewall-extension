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
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.security.CurrentUser;

/**
 * @since 1.60
 */
public class RepositoryPolicyViolationLogger
    extends AbstractPolicyViolationLogger<RepositoryPolicyViolation>
{
  private final Repository repository;

  private final RepositoryManagerDAO repositoryManagerDAO;

  public RepositoryPolicyViolationLogger(
      boolean licensed,
      Date logTimestamp,
      Repository repository,
      CurrentUser currentUser,
      RepositoryManagerDAO repositoryManagerDAO)
  {
    super(licensed, logTimestamp, currentUser);
    this.repository = repository;
    this.repositoryManagerDAO = repositoryManagerDAO;
  }

  @Override
  protected PolicyViolationLogDTO createPolicyViolationLogDTO(
      PolicyViolationData<RepositoryPolicyViolation> policyViolationData)
  {
    PolicyViolationLogDTO policyViolationLogDTO = super.createPolicyViolationLogDTO(policyViolationData);

    if (!PolicyViolationLogEvent.CLEAR.equals(policyViolationData.policyViolationLogEvent)) {
      policyViolationLogDTO.stageTypeId = StageTypes.PROXY.getId();
    }
    policyViolationLogDTO.repositoryManagerId = repository.getRepositoryManagerId();
    RepositoryManager repositoryManager = repositoryManagerDAO.getById(repository.getRepositoryManagerId());
    policyViolationLogDTO.repositoryManagerInstanceId = repositoryManager.getInstanceId();
    policyViolationLogDTO.repositoryManagerName = repositoryManager.getName();
    policyViolationLogDTO.repositoryId = repository.getId();
    policyViolationLogDTO.repositoryPublicId = repository.getPublicId();
    return policyViolationLogDTO;
  }

  @Override
  protected boolean shouldIncludeStagePolicyAction(
      PolicyViolationLogEvent policyViolationLogEvent,
      RepositoryPolicyViolation policyViolation)
  {
    return super.shouldIncludeStagePolicyAction(policyViolationLogEvent, policyViolation) &&
        !policyViolation.isWaived();
  }
}
