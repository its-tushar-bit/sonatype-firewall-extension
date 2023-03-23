/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository.onboarding;

import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.repository.InvalidRepositoryException;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepository;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;

public class FirewallOnboardingRepositoryDAO
    extends AbstractOperationalSqlDAO<FirewallOnboardingRepository>
{
  @Override
  public FirewallOnboardingRepository getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM FirewallOnboardingRepository entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<FirewallOnboardingRepository> getByRepositoryManagerId(String repositoryManagerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryManagerId(tx, repositoryManagerId);
    }
  }

  List<FirewallOnboardingRepository> getByRepositoryManagerId(
      TransactionContext tx,
      String repositoryManagerId)
  {
    String sQuery = "SELECT entity FROM FirewallOnboardingRepository entity" + //
        " WHERE entity.repositoryManagerId=?1";
    return getList(tx, sQuery, repositoryManagerId);
  }

  private void validateName(String name) {
    if (StringUtils.isBlank(name)) {
      throw new InvalidRepositoryException("The repository name cannot be null or empty.");
    }
  }

  public FirewallOnboardingRepository getByRepositoryManagerIdAndName(String repositoryManagerId, String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryManagerIdAndName(tx, repositoryManagerId, name);
    }
  }

  private FirewallOnboardingRepository getByRepositoryManagerIdAndName(
      TransactionContext tx,
      String repositoryManagerId,
      String name)
  {
    String sQuery = "SELECT entity FROM FirewallOnboardingRepository entity" + //
        " WHERE entity.repositoryManagerId=?1 AND entity.name=?2";
    return get(tx, sQuery, repositoryManagerId, name);
  }

  @Override
  public void insert(TransactionContext tx, FirewallOnboardingRepository repository) {
    validateName(repository.getName());
    if (StringUtils.isBlank(repository.getFormat())) {
      throw new InvalidRepositoryException("The repository format cannot be null or empty.");
    }
    if (repository.getType() == null) {
      throw new InvalidRepositoryException("The repository type cannot be null.");
    }

    if (getByRepositoryManagerIdAndName(tx, repository.getRepositoryManagerId(), repository.getName()) != null) {
      throw new InvalidRepositoryException(
          "There is already a repository with name '" + repository.getName() + "' for the same repository manager.");
    }

    super.insert(tx, repository);
  }

  @Override
  public void update(TransactionContext tx, FirewallOnboardingRepository repository) {
    validateName(repository.getName());

    FirewallOnboardingRepository existingRepository =
        getByRepositoryManagerIdAndName(tx, repository.getRepositoryManagerId(), repository.getName());
    if (existingRepository == null) {
      throw new NotFoundException("Cannot find a repository with name '" + repository.getName() + "'.");
    }
    if (!existingRepository.getId().equals(repository.getId())) {
      throw new InvalidRepositoryException(
          "There is already a repository with name '" + repository.getName() + "' for the same repository manager.");
    }

    if (!Objects.equals(existingRepository.getFormat(), repository.getFormat())) {
      throw new BadRequestException("The repository format cannot be changed.");
    }
    if (!Objects.equals(existingRepository.getType(), repository.getType())) {
      throw new BadRequestException("The repository type cannot be changed.");
    }

    super.update(tx, repository);
  }
}
