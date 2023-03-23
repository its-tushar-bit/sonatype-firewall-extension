/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository.onboarding;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.repository.InvalidRepositoryManagerException;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepository;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepositoryManager;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;

public class FirewallOnboardingRepositoryManagerDAO
    extends AbstractOperationalSqlDAO<FirewallOnboardingRepositoryManager>
{
  @Override
  public FirewallOnboardingRepositoryManager getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM FirewallOnboardingRepositoryManager entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public FirewallOnboardingRepositoryManager getByInstanceId(String instanceId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByInstanceId(tx, instanceId);
    }
  }

  private FirewallOnboardingRepositoryManager getByInstanceId(TransactionContext tx, String instanceId) {
    String sQuery = "SELECT entity FROM FirewallOnboardingRepositoryManager entity" + //
        " WHERE entity.instanceId=?1";
    return get(tx, sQuery, instanceId);
  }

  private void validateInstanceId(String instanceId) {
    if (StringUtils.isBlank(instanceId)) {
      throw new InvalidRepositoryManagerException("The repository manager instance ID cannot be null or empty.");
    }
  }

  @Override
  public void insert(TransactionContext tx, FirewallOnboardingRepositoryManager repositoryManager) {
    validateInstanceId(repositoryManager.getInstanceId());

    repositoryManager.setRequestTime(new Date());
    repositoryManager.setConfigureTime(null);
    repositoryManager.setConfigureUsername(null);

    if (getByInstanceId(tx, repositoryManager.getInstanceId()) != null) {
      throw new InvalidRepositoryManagerException(
          "There is already a repository manager with instance ID " + repositoryManager.getInstanceId() + ".");
    }

    super.insert(tx, repositoryManager);
  }

  @Override
  public void update(TransactionContext tx, FirewallOnboardingRepositoryManager repositoryManager) {
    validateInstanceId(repositoryManager.getInstanceId());

    FirewallOnboardingRepositoryManager existingRepositoryManager =
        getByInstanceId(tx, repositoryManager.getInstanceId());
    if (existingRepositoryManager == null) {
      throw new NotFoundException(
          "Cannot find a repository manager with instance ID '" + repositoryManager.getInstanceId() + "'.");
    }
    if (!existingRepositoryManager.getId().equals(repositoryManager.getId())) {
      throw new InvalidRepositoryManagerException(
          "There is already a repository manager with instance ID " + repositoryManager.getInstanceId() + ".");
    }

    if (!Objects.equals(existingRepositoryManager.getRequestTime(), repositoryManager.getRequestTime())) {
      throw new BadRequestException("The request time cannot be changed.");
    }
    if (!Objects.equals(existingRepositoryManager.getRequestUsername(), repositoryManager.getRequestUsername())) {
      throw new BadRequestException("The request user name cannot be changed.");
    }
    if (!Objects.equals(existingRepositoryManager.getRequestUserAgent(), repositoryManager.getRequestUserAgent())) {
      throw new BadRequestException("The request user agent cannot be changed.");
    }

    super.update(tx, repositoryManager);
  }

  @Override
  public void delete(TransactionContext tx, FirewallOnboardingRepositoryManager repositoryManager) {
    // Cascade to repositories
    FirewallOnboardingRepositoryDAO repositoryDAO = new FirewallOnboardingRepositoryDAO();
    List<FirewallOnboardingRepository> repositories =
        repositoryDAO.getByRepositoryManagerId(tx, repositoryManager.getId());
    for (FirewallOnboardingRepository repository : repositories) {
      repositoryDAO.delete(tx, repository);
    }

    super.delete(tx, repositoryManager);
  }
}
