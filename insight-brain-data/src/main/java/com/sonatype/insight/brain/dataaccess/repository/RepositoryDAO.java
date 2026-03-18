/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class RepositoryDAO
    extends AbstractOperationalSqlDAO<Repository>
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryDAO.class);

  private final ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final Provider<OwnerDAO> ownerDAOProvider;

  private final RepositoryMigrationDAO repositoryMigrationDAO;

  @Inject
  public RepositoryDAO(
      final OperationalDataStore operationalDataStore,
      final ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final Provider<OwnerDAO> ownerDAOProvider,
      final RepositoryMigrationDAO repositoryMigrationDAO)
  {
    super(operationalDataStore);
    this.proprietaryComponentNamePatternDAO = proprietaryComponentNamePatternDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.ownerDAOProvider = ownerDAOProvider;
    this.repositoryMigrationDAO = repositoryMigrationDAO;
  }

  public static String getErrMsgMissingRepo(final String repositoryManagerInstanceId, final String repositoryPublicId) {
    return "Cannot find a repository with repositoryManagerInstanceId=" + repositoryManagerInstanceId
        + " and publicId=" + repositoryPublicId + ".";
  }

  public List<Repository> getByRepositoryManagerId(String repositoryManagerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryManagerId(tx, repositoryManagerId);
    }
  }

  public List<Repository> getByAncestorId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByAncestorId(tx, ownerId);
    }
  }

  public List<Repository> getByAncestorId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT repo FROM Repository repo, RepositoryAncestor ra " +
        "WHERE ra.ancestorId = ?1 AND ra.id = repo.id AND ra.id <> ra.ancestorId";

    return getList(tx, sQuery, ownerId);
  }

  public List<Repository> getByRepositoryManagerId(TransactionContext tx, String repositoryManagerId) {
    String sQuery = "SELECT entity FROM Repository entity" + //
        " WHERE entity.repositoryManagerId=?1";
    return getList(tx, sQuery, repositoryManagerId);
  }

  public Repository getByRepositoryManagerInstanceIdAndPublicIdNotNull(
      final String repositoryManagerInstanceId,
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

  public Repository getByRepositoryManagerIdAndPublicId(String repositoryManagerId, String publicId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryManagerIdAndPublicId(tx, repositoryManagerId, publicId);
    }
  }

  private Repository getByRepositoryManagerIdAndPublicId(
      TransactionContext tx,
      String repositoryManagerId,
      String publicId)
  {
    String sQuery = "SELECT entity FROM Repository entity" + //
        " WHERE entity.repositoryManagerId=?1 AND entity.publicId=?2";
    return get(tx, sQuery, repositoryManagerId, publicId);
  }

  public void validateNotEmptyPublicId(String publicId) {
    if (StringUtils.isBlank(publicId)) {
      throw new InvalidRepositoryException("The repository public ID cannot be null or empty.");
    }
  }

  public void validateEnabledFeatures(Repository repository) {
    if (RepositoryType.proxy.equals(repository.getRepositoryType())) {
      // If audit is disabled for a proxy repository, then quarantine must be disabled too.
      // This behavior is important for back compatibility, so we cannot fail it as invalid if audit=disabled and
      // quarantine=enabled.
      if (!repository.isAuditEnabled()) {
        repository.setQuarantineEnabled(false);
      }

      if (repository.isPolicyCompliantComponentSelectionEnabled()
          && (!repository.isAuditEnabled() || !repository.isQuarantineEnabled()))
      {
        throw new InvalidRepositoryException(
            "Policy Compliant Component Selection requires Audit and Quarantine to be enabled.");
      }
      if (repository.isNamespaceConfusionProtectionEnabled()) {
        throw new InvalidRepositoryException(
            "Namespace Confusion Protection can be enabled only for hosted repositories.");
      }
    }
    else {
      if (repository.isAuditEnabled()) {
        throw new InvalidRepositoryException("Audit can be enabled only for proxy repositories.");
      }
      if (repository.isQuarantineEnabled()) {
        throw new InvalidRepositoryException("Quarantine can be enabled only for proxy repositories.");
      }
      if (repository.isPolicyCompliantComponentSelectionEnabled()) {
        throw new InvalidRepositoryException(
            "Policy Compliant Component Selection can be enabled only for proxy repositories.");
      }
    }
  }

  @Override
  public void insert(TransactionContext tx, Repository repository) {
    validateNotEmptyPublicId(repository.getPublicId());

    if (getByRepositoryManagerIdAndPublicId(tx, repository.getRepositoryManagerId(),
        repository.getPublicId()) != null)
    {
      throw new InvalidRepositoryException("There is already a repository with public ID '" + repository.getPublicId()
          + "' for the same repository manager.");
    }

    validateEnabledFeatures(repository);

    super.insert(tx, repository);
  }

  @Override
  public void update(TransactionContext tx, Repository repository) {
    validateNotEmptyPublicId(repository.getPublicId());

    Repository existingRepository = getByRepositoryManagerIdAndPublicId(tx, repository.getRepositoryManagerId(),
        repository.getPublicId());
    if (existingRepository != null) {
      validateUpdate(existingRepository, repository);
    }

    validateEnabledFeatures(repository);

    if (RepositoryType.proxy.equals(repository.getRepositoryType())) {
      // This is a proxy repository
      if (!repository.isAuditEnabled()) {
        onDisableAudit(tx, repository);
      }
      else if (!repository.isQuarantineEnabled()) {
        onDisableQuarantine(tx, repository);
      }
    }
    else {
      // This is a hosted repository
      if (!repository.isNamespaceConfusionProtectionEnabled()) {
        proprietaryComponentNamePatternDAO.deleteByRepository(tx, repository.getId());
      }
    }

    super.update(tx, repository);
  }

  public void validateUpdate(Repository existingRepository, Repository repository) {
    if (!existingRepository.getId().equals(repository.getId())) {
      throw new InvalidRepositoryException("There is already a repository with public ID '" + repository.getPublicId()
          + "' for the same repository manager.");
    }

    if (!existingRepository.getRepositoryType().equals(repository.getRepositoryType())) {
      throw new InvalidRepositoryException("Cannot change the repository type.");
    }

    if (!(existingRepository.getFormat() == null) && !existingRepository.getFormat().equals(repository.getFormat())) {
      throw new InvalidRepositoryException("Cannot change the repository format.");
    }
  }

  /**
   * If the repository is disabled, delete all components and all active policy violations in this repository.
   */
  private void onDisableAudit(TransactionContext tx, Repository repository) {
    Repository existingRepository = getById(tx, repository.getId());
    if (existingRepository.isAuditEnabled()) {
      repositoryPolicyViolationDAO.deleteByRepositoryId(tx, repository.getId());

      repositoryComponentDAO.deleteByRepositoryId(tx, repository.getId());
    }
  }

  /**
   * If quarantine is disabled, then unquarantine all components in this repository.
   */
  private void onDisableQuarantine(TransactionContext tx, Repository repository) {
    Repository existingRepository = getById(tx, repository.getId());
    if (existingRepository.isQuarantineEnabled()) {
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
    ownerDAOProvider.get().cascadeDelete(tx, repository);

    // For H2, we do not enroll the policy violation and component deletions in the transaction on purpose.
    // This improves performance and keeps db operations (including commits) reasonably short, which means other
    // concurrent db operations are blocked for shorter periods of time (H2 is single threaded).

    switch (repository.getRepositoryType()) {
      case proxy:
        // Cascade to repository policy violations
        repositoryPolicyViolationDAO.deleteByRepositoryId(tx, repository.getId());

        // Cascade to repository components
        repositoryComponentDAO.deleteByRepositoryId(tx, repository.getId());

        // Cascade to repository migration (if any)
        if (includeRepositoryMigration) {
          repositoryMigrationDAO.delete(tx, repositoryMigrationDAO.getByRepositoryId(tx, repository.getId()));
        }
        break;
      case hosted:
        // Cascade to proprietary component name patterns
        proprietaryComponentNamePatternDAO.deleteByRepository(repository.getId());
        break;
      default:
        throw new IllegalStateException("Unknown repository type: " + repository.getRepositoryType());
    }

    long duration = System.currentTimeMillis() - start;
    if (duration > 1000) {
      log.debug("Deleted owned entities of repository {} with id {} in {} ms.", repository.getName(),
          repository.getId(), duration);
    }
  }

  /**
   * @since 1.106
   */
  public long getQuarantineEnabledCount() {
    String sQuery = "SELECT COUNT(entity) FROM Repository entity" +
        " WHERE entity.quarantineEnabled = true";

    return getSingle(Long.class, sQuery);
  }

  /**
   * @since 1.161
   */
  public List<Repository> getByRepositoryManagerIdAndLastManualConfigureTime(
      String repositoryManagerId,
      Date lastManualConfigureTime)
  {
    String sQuery = "SELECT entity FROM Repository entity" + //
        " WHERE entity.repositoryManagerId=?1 AND entity.lastManualConfigureTime >= ?2";
    return getList(sQuery, repositoryManagerId, lastManualConfigureTime);
  }

  public List<Repository> getByRepositoryType(RepositoryType repositoryType) {
    String sQuery = "SELECT entity FROM Repository entity" + //
        " WHERE entity.repositoryType=?1";
    return getList(sQuery, repositoryType);
  }

  public List<Repository> getByRepositoryManagerIdAndRepositoryType(
      String repositoryManagerId,
      RepositoryType repositoryType)
  {
    String sQuery = "SELECT entity FROM Repository entity" + //
        " WHERE entity.repositoryManagerId=?1 AND entity.repositoryType=?2";
    return getList(sQuery, repositoryManagerId, repositoryType);
  }

  /**
   * @since 1.174
   */
  public long getCountByRepositoryType(RepositoryType repositoryType) {
    String sQuery = "SELECT COUNT(entity) FROM Repository entity" + //
        " WHERE entity.repositoryType=?1";

    return getSingle(Long.class, sQuery, repositoryType);
  }

  public Repository getByRepositoryIdAndManagerId(String repositoryManagerId, String repositoryId) {
    String sQuery = "SELECT entity FROM Repository entity" + //
        " WHERE entity.repositoryManagerId=?1 AND entity.id=?2";

    return get(sQuery, repositoryManagerId, repositoryId);
  }

  public Repository getByContainerImageId(String containerImageId) {
    String sQuery = """
        SELECT repository
          FROM Repository repository, Organization organization, Application application
         WHERE (application.id = ?1 OR application.publicId = ?1)
           AND organization.id = application.organizationId
           AND repository.id = organization.relatedRepositoryId
           AND repository.repositoryType = ?2
           AND repository.format = ?3
        """;
    return get(sQuery, containerImageId, RepositoryType.proxy, "docker");
  }

  public List<Repository> getByIds(Set<String> repositoryIds) {
    String sQuery = """
        SELECT entity
          FROM Repository entity
         WHERE entity.id IN ?1
        """;
    return getListWithSqlInClause(repositoryIds, inClauseValuesPartition -> getList(sQuery, inClauseValuesPartition));
  }
}
