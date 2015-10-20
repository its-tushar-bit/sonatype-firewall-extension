/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryDAO
    extends AbstractOperationalSqlDAO<Repository>
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryDAO.class);

  public static String getErrMsgMissingRepo(final String repositoryManagerInstanceId, final String repositoryPublicId) {
    return "Cannot find a repository with repositoryManagerInstanceId=" + repositoryManagerInstanceId + " and publicId="
        + repositoryPublicId + ".";
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

  private Repository getByRepositoryManagerIdAndPublicId(TransactionContext tx, String repositoryManagerId,
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

  @Override
  public void insert(TransactionContext tx, Repository repository) {
    validateNotEmptyPublicId(repository.getPublicId());

    if (getByRepositoryManagerIdAndPublicId(tx, repository.getRepositoryManagerId(), repository.getPublicId()) != null) {
      throw new InvalidRepositoryException("There is already a repository with public ID '" + repository.getPublicId()
          + "' for the same repository manager.");
    }

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

    super.update(tx, repository);
  }

  @Override
  public void delete(TransactionContext tx, Repository repository) {
    long start = System.currentTimeMillis();

    // Cascade to repository components
    RepositoryComponentDAO repositoryComponentDAO = new RepositoryComponentDAO();
    List<RepositoryComponent> repositoryComponents = repositoryComponentDAO.getByRepositoryId(tx, repository.getId());
    for (RepositoryComponent repositoryComponent : repositoryComponents) {
      repositoryComponentDAO.delete(tx, repositoryComponent);
    }

    // Cascade to repository policy violations
    RepositoryPolicyViolationDAO repositoryPolicyViolationDAO = new RepositoryPolicyViolationDAO();
    List<RepositoryPolicyViolation> policyViolations = repositoryPolicyViolationDAO.getByRepositoryId(tx,
        repository.getId());
    for (RepositoryPolicyViolation policyViolation : policyViolations) {
      repositoryPolicyViolationDAO.delete(tx, policyViolation);
    }

    super.delete(tx, repository);

    long duration = System.currentTimeMillis() - start;
    if (duration > 500) {
      log.debug("Deleted repository {} with id {} in {} ms.", repository.getName(), repository.getId(), duration);
    }
  }

  public List<Repository> getAll(TransactionContext tx) {
    String sQuery = "SELECT entity FROM Repository entity";
    return getList(tx, sQuery);
  }

  public Repository getByIdNotNull(String id) {
    Repository repository = getById(id);
    if (repository == null) {
      throw new NotFoundException("Cannot find a repository with ID " + id + ".");
    }
    return repository;
  }
}
