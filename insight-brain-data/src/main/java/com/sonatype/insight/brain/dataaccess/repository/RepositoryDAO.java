/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ClusterLock;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryDAO
    extends AbstractOperationalSqlDAO<Repository>
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryDAO.class);

  public static String getErrMsgMissingRepo(final String repositoryManagerInstanceId, final String repositoryPublicId) {
    return "Cannot find a repository with repositoryManagerInstanceId=" + repositoryManagerInstanceId
        + " and publicId=" + repositoryPublicId + ".";
  }

  @Override
  public Repository getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM Repository entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<Repository> getByRepositoryManagerId(String repositoryManagerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryManagerId(tx, repositoryManagerId);
    }
  }

  public List<Repository> getByRepositoryManagerId(TransactionContext tx, String repositoryManagerId) {
    String sQuery = "SELECT entity FROM Repository entity" + //
        " WHERE entity.repositoryManagerId=?1";
    return getList(tx, sQuery, repositoryManagerId);
  }

  public Repository getByRepositoryManagerInstanceIdAndPublicIdNotNull(final String repositoryManagerInstanceId,
                                                                       final String publicId)
  {
    final Repository repository = getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId, publicId);
    if (repository == null) {
      throw new NotFoundException(getErrMsgMissingRepo(repositoryManagerInstanceId, publicId));
    }
    return repository;
  }

  public Repository getByRepositoryManagerInstanceIdAndPublicId(String repositoryManagerInstanceId, String publicId) {
    String sQuery = "SELECT repository FROM Repository repository, RepositoryManager repositoryManager" + //
        " WHERE repository.repositoryManagerId=repositoryManager.id" + //
        " AND repositoryManager.instanceId=?1 AND repository.publicId=?2";
    return get(sQuery, repositoryManagerInstanceId, publicId);
  }

  private Repository getByRepositoryManagerIdAndPublicId(TransactionContext tx,
                                                         String repositoryManagerId,
                                                         String publicId)
  {
    String sQuery = "SELECT entity FROM Repository entity" + //
        " WHERE entity.repositoryManagerId=?1 AND entity.publicId=?2";
    return get(tx, sQuery, repositoryManagerId, publicId);
  }

  private void validateNotEmptyPublicId(String publicId) {
    if (StringUtils.isBlank(publicId)) {
      throw new InvalidRepositoryException("The repository public ID cannot be null or empty.");
    }
  }

  /**
   * If the repository is disabled, then quarantine must be disabled too.
   */
  private void ensureCorrectQuarantineMode(Repository repository) {
    if (!repository.isEnabled()) {
      repository.setQuarantineEnabled(false);
    }
  }

  @Override
  public void insert(TransactionContext tx, Repository repository) {
    validateNotEmptyPublicId(repository.getPublicId());

    if (getByRepositoryManagerIdAndPublicId(tx, repository.getRepositoryManagerId(),
        repository.getPublicId()) != null) {
      throw new InvalidRepositoryException("There is already a repository with public ID '" + repository.getPublicId()
          + "' for the same repository manager.");
    }

    ensureCorrectQuarantineMode(repository);

    super.insert(tx, repository);
  }

  @Override
  public void update(TransactionContext tx, Repository repository) {
    validateNotEmptyPublicId(repository.getPublicId());

    Repository existingRepository = getByRepositoryManagerIdAndPublicId(tx, repository.getRepositoryManagerId(),
        repository.getPublicId());
    if (existingRepository != null && !existingRepository.getId().equals(repository.getId())) {
      throw new InvalidRepositoryException("There is already a repository with public ID '" + repository.getPublicId()
          + "' for the same repository manager.");
    }

    ensureCorrectQuarantineMode(repository);

    if (!repository.isEnabled()) {
      onDisable(tx, repository);
    }
    else if (!repository.isQuarantineEnabled()) {
      onDisableQuarantine(tx, repository);
    }

    super.update(tx, repository);
  }

  /**
   * If the repository is disabled, delete all components and all active policy violations in this repository.
   */
  private void onDisable(TransactionContext tx, Repository repository) {
    Repository existingRepository = getById(tx, repository.getId());
    if (existingRepository.isEnabled()) {
      new RepositoryPolicyViolationDAO().deleteByRepositoryId(tx, repository.getId());

      new RepositoryComponentDAO().deleteByRepositoryId(tx, repository.getId());
    }
  }

  /**
   * If quarantine is disabled, then unquarantine all components in this repository.
   */
  private void onDisableQuarantine(TransactionContext tx, Repository repository) {
    Repository existingRepository = getById(tx, repository.getId());
    if (existingRepository.isQuarantineEnabled()) {
      RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();
      Date unquarantineTime = new Date();
      List<RepositoryComponent> quarantinedComponents = repositoryComponentDAO.getQuarantinedByRepositoryId(tx,
          repository.getId());
      for (RepositoryComponent quarantinedComponent : quarantinedComponents) {
        quarantinedComponent.setUnquarantineTimeForManualRelease(unquarantineTime);
        repositoryComponentDAO.update(tx, quarantinedComponent);
      }
    }
  }

  @Override
  public void delete(TransactionContext tx, Repository repository) {
    cascadeDelete(tx, repository, true /* includeRepositoryMigration */);

    super.delete(tx, repository);
  }

  public void cascadeDelete(Repository repository, boolean includeRepositoryMigration) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      cascadeDelete(tx, repository, includeRepositoryMigration);
      tx.commit();
    }
  }

  private void cascadeDelete(TransactionContext tx, Repository repository, boolean includeRepositoryMigration) {
    long start = System.currentTimeMillis();

    // Cascade to owned entities
    new OwnerDAO().cascadeDelete(tx, repository);

    // For H2, we do not enroll the policy violation and component deletions in the transaction on purpose.
    // This improves performance and keeps db operations (including commits) reasonably short, which means other
    // concurrent db operations are blocked for shorter periods of time (H2 is single threaded).

    // Cascade to repository policy violations
    new RepositoryPolicyViolationDAO().deleteByRepositoryId(tx, repository.getId());

    // Cascade to repository components
    new RepositoryComponentDAO().deleteByRepositoryId(tx, repository.getId());

    // Cascade to repository reevaluation locks
    ClusterLock.deleteForRepositoryReevaluation(tx, repository);

    // Cascade to repository migration (if any)
    if (includeRepositoryMigration) {
      RepositoryMigrationDAO repositoryMigrationDAO = new RepositoryMigrationDAO();
      repositoryMigrationDAO.delete(tx, repositoryMigrationDAO.getByRepositoryId(tx, repository.getId()));
    }

    long duration = System.currentTimeMillis() - start;
    if (duration > 1000) {
      log.debug("Deleted owned entities of repository {} with id {} in {} ms.", repository.getName(),
          repository.getId(), duration);
    }
  }

  public List<Repository> getAll(TransactionContext tx) {
    String sQuery = "SELECT entity FROM Repository entity";
    return getList(tx, sQuery);
  }

  public List<Repository> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAll(tx);
    }
  }

  public Repository getByIdNotNull(String id) {
    Repository repository = getById(id);
    if (repository == null) {
      throw new NotFoundException("Cannot find a repository with ID " + id + ".");
    }
    return repository;
  }

  public long getCount() {
    String sQuery = "SELECT COUNT(entity) FROM Repository entity";
    return getSingle(Long.class, sQuery);
  }

  /**
   * @since 1.106
   */
  public long getQuarantineEnabledCount() {
    String sQuery = "SELECT COUNT(entity) FROM Repository entity" +
        " WHERE entity.quarantineEnabled = true";

    return getSingle(Long.class, sQuery);
  }
}
