/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jooq.Condition;
import org.jooq.ResultQuery;
import org.jooq.SortField;

import com.sonatype.insight.brain.jooq.generated.ods.tables.records.RepositoryRecord;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryMigration;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Record;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Organization.ORGANIZATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Repository.REPOSITORY;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryAncestor.REPOSITORY_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryManager.REPOSITORY_MANAGER;

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

  private final HostedComponentScanQueueDAO hostedComponentScanQueueDAO;

  @Inject
  public RepositoryDAO(
      final OperationalDataStore operationalDataStore,
      final ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final Provider<OwnerDAO> ownerDAOProvider,
      final RepositoryMigrationDAO repositoryMigrationDAO,
      final HostedComponentScanQueueDAO hostedComponentScanQueueDAO)
  {
    super(operationalDataStore);
    this.proprietaryComponentNamePatternDAO = proprietaryComponentNamePatternDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.ownerDAOProvider = ownerDAOProvider;
    this.repositoryMigrationDAO = repositoryMigrationDAO;
    this.hostedComponentScanQueueDAO = hostedComponentScanQueueDAO;
  }

  @Override
  public Table<?> getJooqTable() {
    return REPOSITORY;
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
    return tx.dsl()
        .select(REPOSITORY.fields())
        .from(REPOSITORY)
        .join(REPOSITORY_ANCESTOR)
        .on(REPOSITORY_ANCESTOR.REPOSITORY_ID.eq(REPOSITORY.REPOSITORY_ID))
        .where(REPOSITORY_ANCESTOR.ANCESTOR_ID.eq(ownerId))
        .and(REPOSITORY_ANCESTOR.REPOSITORY_ID.ne(REPOSITORY_ANCESTOR.ANCESTOR_ID))
        .fetch(r -> toEntity(r.into(REPOSITORY)));
  }

  public List<Repository> getByRepositoryManagerId(TransactionContext tx, String repositoryManagerId) {
    return tx.dsl()
        .selectFrom(REPOSITORY)
        .where(REPOSITORY.REPOSITORY_MANAGER_ID.eq(repositoryManagerId))
        .fetch(this::toEntity);
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
    try (TransactionContext tx = createTransactionContext()) {
      Record record = tx.dsl()
          .select(REPOSITORY.fields())
          .from(REPOSITORY)
          .join(REPOSITORY_MANAGER)
          .on(REPOSITORY.REPOSITORY_MANAGER_ID.eq(REPOSITORY_MANAGER.REPOSITORY_MANAGER_ID))
          .where(REPOSITORY_MANAGER.INSTANCE_ID.eq(repositoryManagerInstanceId))
          .and(REPOSITORY.PUBLIC_ID.eq(publicId))
          .fetchOne();
      return record != null ? toEntity(record.into(REPOSITORY)) : null;
    }
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
    return toEntity(tx.dsl()
        .selectFrom(REPOSITORY)
        .where(REPOSITORY.REPOSITORY_MANAGER_ID.eq(repositoryManagerId))
        .and(REPOSITORY.PUBLIC_ID.eq(publicId))
        .fetchOne());
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
  public int insert(TransactionContext tx, Repository repository) {
    validateNotEmptyPublicId(repository.getPublicId());

    if (getByRepositoryManagerIdAndPublicId(tx, repository.getRepositoryManagerId(),
        repository.getPublicId()) != null)
    {
      throw new InvalidRepositoryException("There is already a repository with public ID '" + repository.getPublicId()
          + "' for the same repository manager.");
    }

    validateEnabledFeatures(repository);

    generateIdIfNeeded(repository);

    return super.insert(tx, repository);
  }

  @Override
  public int update(TransactionContext tx, Repository repository) {
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

    return super.update(tx, repository);
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

  /**
   * Deletes all entities owned by or associated with this repository without deleting the repository itself.
   * <p>
   * This is the standalone entry point that creates its own transaction. All cascade operations will participate in a
   * single transaction for atomicity - if any step fails, all changes will be rolled back together.
   * </p>
   *
   * @param repository the repository whose owned entities should be deleted
   * @param includeRepositoryMigration whether to also delete associated repository migration records
   * @see #cascadeDelete(TransactionContext, Repository, boolean) for details on what gets deleted
   */
  public void cascadeDelete(Repository repository, boolean includeRepositoryMigration) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      cascadeDelete(tx, repository, includeRepositoryMigration);
      tx.commit();
    }
  }

  /**
   * Deletes all entities owned by or associated with this repository without deleting the repository itself.
   * <p>
   * <b>Transaction Boundary Semantics:</b> All delete operations in this method participate in the
   * provided transaction context. This ensures atomic rollback behavior - if any cascade operation fails, all previous
   * operations in this cascade will also be rolled back when the transaction is rolled back. This is intentional to
   * maintain data consistency.
   * </p>
   * <p>
   * <b>What gets deleted:</b>
   * </p>
   * <ul>
   * <li>Owned entities via {@link OwnerDAO#cascadeDelete(TransactionContext, com.sonatype.insight.brain.model.Owner)}
   * (policy waivers, license overrides, vulnerability overrides, etc.)</li>
   * <li>For proxy repositories: policy violations, components, and optionally migration records</li>
   * <li>For hosted repositories: policy violations, scan queue entries, components, proprietary component name
   * patterns</li>
   * </ul>
   * <p>
   * <b>Note:</b> Prior to the jOOQ migration, some operations created independent transactions
   * which would persist even if subsequent operations failed. The current implementation ensures
   * all operations roll back together for better consistency.
   * </p>
   *
   * @param tx the transaction context that all operations will participate in
   * @param repository the repository whose owned entities should be deleted
   * @param includeRepositoryMigration whether to also delete associated repository migration records
   */
  private void cascadeDelete(TransactionContext tx, Repository repository, boolean includeRepositoryMigration) {
    long start = System.currentTimeMillis();

    // Cascade to owned entities
    ownerDAOProvider.get().cascadeDelete(tx, repository);

    // All operations below participate in the same transaction for atomic rollback behavior.

    switch (repository.getRepositoryType()) {
      case proxy:
        // Cascade to repository policy violations
        repositoryPolicyViolationDAO.deleteByRepositoryId(tx, repository.getId());

        // Cascade to repository components
        repositoryComponentDAO.deleteByRepositoryId(tx, repository.getId());

        // Cascade to repository migration (if any)
        if (includeRepositoryMigration) {
          RepositoryMigration repositoryMigration = repositoryMigrationDAO.getByRepositoryId(tx, repository.getId());
          if (repositoryMigration != null) {
            repositoryMigrationDAO.delete(tx, repositoryMigration);
          }
        }
        break;
      case hosted:
        repositoryPolicyViolationDAO.deleteByRepositoryId(tx, repository.getId());

        // Cascade to scan queue entries (must precede component delete — no DB-level FK exists)
        hostedComponentScanQueueDAO.deleteByRepositoryComponentIds(tx, repository.getId());

        // Cascade to repository components
        repositoryComponentDAO.deleteByRepositoryId(tx, repository.getId());

        // Cascade to proprietary component name patterns
        proprietaryComponentNamePatternDAO.deleteByRepository(tx, repository.getId());
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
    try (TransactionContext tx = createTransactionContext()) {
      Integer count = tx.dsl()
          .selectCount()
          .from(REPOSITORY)
          .where(REPOSITORY.QUARANTINE_ENABLED.eq(true))
          .fetchOne(0, Integer.class);
      return count != null ? count.longValue() : 0L;
    }
  }

  /**
   * @since 1.161
   */
  public List<Repository> getByRepositoryManagerIdAndLastManualConfigureTime(
      String repositoryManagerId,
      Date lastManualConfigureTime)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY)
          .where(REPOSITORY.REPOSITORY_MANAGER_ID.eq(repositoryManagerId))
          .and(REPOSITORY.LAST_MANUAL_CONFIGURE_TIME.ge(lastManualConfigureTime))
          .fetch(this::toEntity);
    }
  }

  public List<Repository> getByRepositoryType(RepositoryType repositoryType) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY)
          .where(REPOSITORY.REPOSITORY_TYPE.eq(repositoryType != null ? repositoryType.name() : null))
          .fetch(this::toEntity);
    }
  }

  public List<Repository> getHostedRepositoriesWithMonitoringEnabled() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY)
          .where(REPOSITORY.REPOSITORY_TYPE.eq(RepositoryType.hosted.name()))
          .and(REPOSITORY.MONITORING_ENABLED.eq(true))
          .fetch(this::toEntity);
    }
  }

  public List<Repository> getByRepositoryManagerIdAndRepositoryType(
      String repositoryManagerId,
      RepositoryType repositoryType)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryManagerIdAndRepositoryType(tx, repositoryManagerId, repositoryType);
    }
  }

  public List<Repository> getByRepositoryManagerIdAndRepositoryType(
      TransactionContext tx,
      String repositoryManagerId,
      RepositoryType repositoryType)
  {
    return tx.dsl()
        .selectFrom(REPOSITORY)
        .where(REPOSITORY.REPOSITORY_MANAGER_ID.eq(repositoryManagerId))
        .and(REPOSITORY.REPOSITORY_TYPE.eq(repositoryType != null ? repositoryType.name() : null))
        .fetch(this::toEntity);
  }

  /**
   * Returns a filtered, paginated page of hosted repositories.
   *
   * <p>
   * Note: when {@code sortBy} is {@code "lastScannedTime"}, DB-layer pagination still uses
   * {@code publicId} ordering. The caller ({@code RepositoryService}) re-sorts the page in Java
   * after populating the derived field, so cross-page ordering is correct only once the frontend
   * wires up pagination for this sort column.
   */
  private Condition buildHostedRepositoryFilterCondition(
      String repositoryManagerId,
      String searchText,
      String format)
  {
    Condition condition = REPOSITORY.REPOSITORY_MANAGER_ID.eq(repositoryManagerId)
        .and(REPOSITORY.REPOSITORY_TYPE.eq(RepositoryType.hosted.name()))
        .and(REPOSITORY.MONITORING_ENABLED.isTrue());
    if (StringUtils.isNotBlank(searchText)) {
      condition = condition.and(REPOSITORY.PUBLIC_ID.containsIgnoreCase(searchText.trim()));
    }
    if (StringUtils.isNotBlank(format)) {
      condition = condition.and(REPOSITORY.FORMAT.eq(format));
    }
    return condition;
  }

  public List<Repository> getFilteredHostedRepositories(
      TransactionContext tx,
      String repositoryManagerId,
      String searchText,
      String format,
      String sortBy,
      String sortDir,
      Integer page,
      Integer pageSize)
  {
    Condition condition = buildHostedRepositoryFilterCondition(repositoryManagerId, searchText, format);

    boolean desc = "desc".equalsIgnoreCase(sortDir);
    SortField<?> sortField;
    if ("format".equalsIgnoreCase(sortBy)) {
      sortField = desc ? REPOSITORY.FORMAT.desc().nullsLast() : REPOSITORY.FORMAT.asc().nullsLast();
    }
    else {
      sortField = desc ? REPOSITORY.PUBLIC_ID.desc().nullsLast() : REPOSITORY.PUBLIC_ID.asc().nullsLast();
    }

    var ordered = tx.dsl()
        .selectFrom(REPOSITORY)
        .where(condition)
        .orderBy(sortField);

    ResultQuery<RepositoryRecord> query;
    if (page != null && pageSize != null && page > 0 && pageSize > 0) {
      query = ordered.limit(pageSize).offset((long) (page - 1) * pageSize);
    }
    else {
      query = ordered;
    }

    return query.fetch(this::toEntity);
  }

  public int countFilteredHostedRepositories(
      TransactionContext tx,
      String repositoryManagerId,
      String searchText,
      String format)
  {
    Condition condition = buildHostedRepositoryFilterCondition(repositoryManagerId, searchText, format);
    Integer count = tx.dsl()
        .selectCount()
        .from(REPOSITORY)
        .where(condition)
        .fetchOne(0, Integer.class);
    return count != null ? count : 0;
  }

  /**
   * @since 1.174
   */
  public long getCountByRepositoryType(RepositoryType repositoryType) {
    try (TransactionContext tx = createTransactionContext()) {
      Integer count = tx.dsl()
          .selectCount()
          .from(REPOSITORY)
          .where(REPOSITORY.REPOSITORY_TYPE.eq(repositoryType != null ? repositoryType.name() : null))
          .fetchOne(0, Integer.class);
      return count != null ? count.longValue() : 0L;
    }
  }

  public Repository getByRepositoryIdAndManagerId(String repositoryManagerId, String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(REPOSITORY)
          .where(REPOSITORY.REPOSITORY_MANAGER_ID.eq(repositoryManagerId))
          .and(REPOSITORY.REPOSITORY_ID.eq(repositoryId))
          .fetchOne());
    }
  }

  public Repository getByContainerImageId(String containerImageId) {
    try (TransactionContext tx = createTransactionContext()) {
      Record record = tx.dsl()
          .select(REPOSITORY.fields())
          .from(REPOSITORY)
          .join(ORGANIZATION)
          .on(REPOSITORY.REPOSITORY_ID.eq(ORGANIZATION.RELATED_REPOSITORY_ID))
          .join(APPLICATION)
          .on(ORGANIZATION.ORGANIZATION_ID.eq(APPLICATION.ORGANIZATION_ID))
          .where(APPLICATION.APPLICATION_ID.eq(containerImageId)
              .or(APPLICATION.PUBLIC_ID.eq(containerImageId)))
          .and(REPOSITORY.REPOSITORY_TYPE.eq(RepositoryType.proxy.name()))
          .and(REPOSITORY.FORMAT.eq("docker"))
          .fetchOne();
      return record != null ? toEntity(record.into(REPOSITORY)) : null;
    }
  }

  public List<Repository> getByIds(Set<String> repositoryIds) {
    if (repositoryIds == null || repositoryIds.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(REPOSITORY)
          .where(REPOSITORY.REPOSITORY_ID.in(repositoryIds))
          .fetch(this::toEntity);
    }
  }

  @Override
  public Class<Repository> getEntityClass() {
    return Repository.class;
  }
}
