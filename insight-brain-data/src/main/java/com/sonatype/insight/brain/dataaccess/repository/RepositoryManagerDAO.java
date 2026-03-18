/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class RepositoryManagerDAO
    extends AbstractOperationalSqlDAO<RepositoryManager>
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryManagerDAO.class);

  private final Provider<OwnerDAO> ownerDAOProvider;

  @Inject
  public RepositoryManagerDAO(
      final OperationalDataStore operationalDataStore,
      final Provider<OwnerDAO> ownerDAOProvider)
  {
    super(operationalDataStore);
    this.ownerDAOProvider = ownerDAOProvider;
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
    try (TransactionContext tx = createTransactionContext()) {
      return getByInstanceIdNotNull(tx, instanceId);
    }
  }

  public RepositoryManager getByInstanceIdNotNull(TransactionContext tx, String instanceId) {
    RepositoryManager repositoryManager = getByInstanceId(tx, instanceId);
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

  private void validateName(RepositoryManager repositoryManager) {
    // We default the name to the instance ID if the name was not set.
    // So we need to accept any instance ID as name, and only validate the name if it is set to something different from
    // the instance ID.
    if (!Objects.equals(repositoryManager.getInstanceId(), repositoryManager.getName())) {
      NameHelper.validate("Name", repositoryManager.getName(), NameHelper.MAX_NAME_LENGTH_APP_ORG);
    }
  }

  public RepositoryManager getByRelatedOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRelatedOrganizationId(tx, organizationId);
    }
  }

  private RepositoryManager getByRelatedOrganizationId(TransactionContext tx, String organizationId) {
    String sQuery = "SELECT entity FROM RepositoryManager entity" + //
        " WHERE entity.relatedOrganizationId=?1";
    return get(tx, sQuery, organizationId);
  }

  @Override
  public void insert(TransactionContext tx, RepositoryManager repositoryManager) {
    validateInstanceId(repositoryManager.getInstanceId());

    validateName(repositoryManager);

    if (getByInstanceId(tx, repositoryManager.getInstanceId()) != null) {
      throw new InvalidRepositoryManagerException("There is already a repository manager with instance ID "
          + repositoryManager.getInstanceId() + ".");
    }

    if (repositoryManager.getName() != null
        && getByName(tx, repositoryManager.getName()) != null)
    {
      throw new InvalidNameException(repositoryManager.getName() + " is already used as a name.");
    }

    super.insert(tx, repositoryManager);
  }

  @Override
  public void update(TransactionContext tx, RepositoryManager repositoryManager) {
    validateInstanceId(repositoryManager.getInstanceId());

    validateName(repositoryManager);

    RepositoryManager existingRepositoryManager = getByInstanceId(tx, repositoryManager.getInstanceId());
    if (existingRepositoryManager != null && !existingRepositoryManager.getId().equals(repositoryManager.getId())) {
      throw new InvalidRepositoryManagerException("There is already a repository manager with instance ID "
          + repositoryManager.getInstanceId() + ".");
    }

    if (repositoryManager.getName() != null) {
      RepositoryManager foundByNameRepositoryManager = getByName(tx,
          repositoryManager.getName());
      if (foundByNameRepositoryManager != null && !repositoryManager.getId()
          .equals(foundByNameRepositoryManager.getId()))
      {
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

    // Cascade to owned entities
    ownerDAOProvider.get().cascadeDelete(tx, repositoryManager);

    super.delete(tx, repositoryManager);

    long duration = System.currentTimeMillis() - start;
    if (duration > 500) {
      log.debug("Deleted repository manager with id {} in {} ms.", repositoryManager.getId(), duration);
    }
  }

  /**
   * @since 1.160
   */
  public List<RepositoryManager> getUnconfigured() {
    String sQuery = "SELECT entity FROM RepositoryManager entity" + //
        " WHERE entity.configured = false";
    return getList(sQuery);
  }

  public RepositoryManager getByName(String name) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByName(tx, name);
    }
  }

  /**
   * @return the RepositoryManager that either has the specified id or is the parent of a repository with that id
   */
  public RepositoryManager getByIdOrRepositoryId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdOrRepositoryId(tx, ownerId);
    }
  }

  public RepositoryManager getByIdOrRepositoryId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT repoManager FROM RepositoryManager repoManager, OwnerAncestor oa " +
        "WHERE repoManager.id = oa.ancestorId AND " +
        "( " +
        "  oa.ownerType = com.sonatype.insight.brain.model.OwnerType.REPOSITORY " +
        "  OR oa.ownerType = com.sonatype.insight.brain.model.OwnerType.REPOSITORY_MANAGER " +
        ") " +
        "AND oa.id = ?1";

    return get(tx, sQuery, ownerId);
  }

  public List<RepositoryManager> getByRepositoryIds(Set<String> repositoryIds) {
    String sQuery = """
        SELECT repositoryManager FROM RepositoryManager repositoryManager, Repository repository
        WHERE repositoryManager.id = repository.repositoryManagerId AND
        repository.id IN ?1
        """;
    return getListWithSqlInClause(repositoryIds, inClauseValuesPartition -> getList(sQuery, inClauseValuesPartition));
  }

  public List<RepositoryManager> getByIds(Set<String> repositoryManagerIds) {
    String sQuery = """
        SELECT entity
          FROM RepositoryManager entity
         WHERE entity.id IN ?1
        """;
    return getListWithSqlInClause(
        repositoryManagerIds, inClauseValuesPartition -> getList(sQuery, inClauseValuesPartition));
  }
}
