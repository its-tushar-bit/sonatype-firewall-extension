/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryDAO
    extends AbstractOperationalSqlDAO<Repository>
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryDAO.class);

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

  private Repository getByRepositoryManagerAndPublicId(TransactionContext tx, String repositoryManagerId,
      String publicId)
  {
    String sQuery = "SELECT entity FROM Repository entity" + //
        " WHERE entity.repositoryManagerId=?1 AND entity.publicId=?2";
    return get(tx, sQuery, repositoryManagerId, publicId);
  }

  private void validateNotEmptyField(String fieldName, String value) {
    if (StringUtils.isBlank(value)) {
      throw new DataAccessException("The repository " + fieldName + " cannot be null or empty.");
    }
  }

  @Override
  public void insert(TransactionContext tx, Repository repository) {
    validateNotEmptyField("name", repository.getName());
    validateNotEmptyField("public ID", repository.getPublicId());

    if (getByRepositoryManagerAndPublicId(tx, repository.getRepositoryManagerId(), repository.getPublicId()) != null) {
      throw new DataAccessException("There is already a repository with public ID '" + repository.getPublicId()
          + "' for the same repository manager.");
    }

    super.insert(tx, repository);
  }

  @Override
  public void update(TransactionContext tx, Repository repository) {
    validateNotEmptyField("name", repository.getName());
    validateNotEmptyField("public ID", repository.getPublicId());

    Repository existingRepository = getByRepositoryManagerAndPublicId(tx, repository.getRepositoryManagerId(),
        repository.getPublicId());
    if (existingRepository != null && !existingRepository.getId().equals(repository.getId())) {
      throw new DataAccessException("There is already a repository with public ID '" + repository.getPublicId()
          + "' for the same repository manager.");
    }

    super.update(tx, repository);
  }

  @Override
  public void delete(TransactionContext tx, Repository repository) {
    long start = System.currentTimeMillis();

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
}
