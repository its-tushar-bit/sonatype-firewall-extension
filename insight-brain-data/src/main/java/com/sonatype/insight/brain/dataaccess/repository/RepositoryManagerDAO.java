/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryManagerDAO
    extends AbstractOperationalSqlDAO<RepositoryManager>
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryManagerDAO.class);

  @Override
  public RepositoryManager getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM RepositoryManager entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public RepositoryManager getByIdNotNull(String id) {
    RepositoryManager repositoryManager = getById(id);
    if (repositoryManager == null) {
      throw new NotFoundException("Cannot find a repository manager with ID " + id + ".");
    }
    return repositoryManager;
  }

  public RepositoryManager getByInstanceId(String instanceId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByInstanceId(tx, instanceId);
    }
  }

  private RepositoryManager getByInstanceId(TransactionContext tx, String instanceId) {
    String sQuery = "SELECT entity FROM RepositoryManager entity" + //
        " WHERE entity.instanceId=?1";
    return get(tx, sQuery, instanceId);
  }

  /**
   * @since 1.161
   */
  public RepositoryManager getByInstanceIdNotNull(String instanceId) {
    RepositoryManager repositoryManager = getByInstanceId(instanceId);
    if (repositoryManager == null) {
      throw new NotFoundException("Cannot find a repository manager with instance ID " + instanceId + ".");
    }
    return repositoryManager;
  }

  private void validateInstanceId(String instanceId) {
    if (StringUtils.isBlank(instanceId)) {
      throw new InvalidRepositoryManagerException("The repository manager instance ID cannot be null or empty.");
    }
  }

  @Override
  public void insert(TransactionContext tx, RepositoryManager repositoryManager) {
    validateInstanceId(repositoryManager.getInstanceId());
    if (repositoryManager.getName() != null) {
      NameHelper.validate("Name", repositoryManager.getName(), NameHelper.MAX_NAME_LENGTH_APP_ORG);
    }

    if (getByInstanceId(tx, repositoryManager.getInstanceId()) != null) {
      throw new InvalidRepositoryManagerException("There is already a repository manager with instance ID "
          + repositoryManager.getInstanceId() + ".");
    }

    if (repositoryManager.getName() != null
            && getByName(tx, repositoryManager.getName()) != null) {
      throw new InvalidNameException(repositoryManager.getName() + " is already used as a name.");
    }

    super.insert(tx, repositoryManager);
  }

  @Override
  public void update(TransactionContext tx, RepositoryManager repositoryManager) {
    validateInstanceId(repositoryManager.getInstanceId());
    if (repositoryManager.getName() != null) {
      NameHelper.validate("Name", repositoryManager.getName(), NameHelper.MAX_NAME_LENGTH_APP_ORG);
    }

    RepositoryManager existingRepositoryManager = getByInstanceId(tx, repositoryManager.getInstanceId());
    if (existingRepositoryManager != null && !existingRepositoryManager.getId().equals(repositoryManager.getId())) {
      throw new InvalidRepositoryManagerException("There is already a repository manager with instance ID "
          + repositoryManager.getInstanceId() + ".");
    }

    if (repositoryManager.getName() != null) {
      RepositoryManager foundByNameRepositoryManager = getByName(tx,
              repositoryManager.getName());
      if (foundByNameRepositoryManager != null && !repositoryManager.getId()
              .equals(foundByNameRepositoryManager.getId())) {
        throw new InvalidNameException(
                repositoryManager.getName() + " is already used as a name.");
      }
    }

    super.update(tx, repositoryManager);
  }

  public RepositoryManager getByName(TransactionContext tx, String name) {
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM RepositoryManager entity WHERE entity.nameLowercaseNoWhitespace=?1";
    return get(tx, sQuery, name);
  }

  @Override
  public void delete(TransactionContext tx, RepositoryManager repositoryManager) {
    long start = System.currentTimeMillis();

    // Cascade to repositories
    RepositoryDAO repositoryDAO = new RepositoryDAO();
    List<Repository> repositories = repositoryDAO.getByRepositoryManagerId(tx, repositoryManager.getId());
    for (Repository repository : repositories) {
      repositoryDAO.delete(tx, repository);
    }

    super.delete(tx, repositoryManager);

    long duration = System.currentTimeMillis() - start;
    if (duration > 500) {
      log.debug("Deleted repository manager with id {} in {} ms.", repositoryManager.getId(), duration);
    }
  }

  /**
   * @since 1.35
   */
  public List<RepositoryManager> getAll() {
    String sQuery = "SELECT entity FROM RepositoryManager entity";
    return getList(sQuery);
  }

  /**
   * @since 1.160
   */
  public List<RepositoryManager> getUnconfigured() {
    // Currently, we only support Nexus Repository managers for firewall configuration.
    String sQuery = "SELECT entity FROM RepositoryManager entity" + //
        " WHERE entity.configured = false AND entity.userAgent LIKE 'Nexus/%'";
    return getList(sQuery);
  }
  
  public RepositoryManager getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }
}
