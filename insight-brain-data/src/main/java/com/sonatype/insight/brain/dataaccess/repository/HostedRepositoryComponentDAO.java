/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.EligibilityCursor;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jooq.Condition;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.HostedRepositoryComponentAncestor.HOSTED_REPOSITORY_COMPONENT_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.HostedRepositoryComponent.HOSTED_REPOSITORY_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.Repository.REPOSITORY;

/**
 * DAO for the {@link HostedRepositoryComponent} entity. Delete cascades to owned scan-based rows in
 * {@code policy_evaluation} / {@code policy_violation} / {@code owner_component} /
 * {@code last_policy_evaluation} via {@link OwnerDAO#cascadeDelete(TransactionContext,
 * com.sonatype.insight.brain.model.Owner)}.
 *
 * @since 1.207
 */
@Named
@Singleton
public class HostedRepositoryComponentDAO
    extends AbstractOperationalSqlDAO<HostedRepositoryComponent>
{
  /**
   * Page size for {@link #deleteByRepositoryId(TransactionContext, String)}. Bounds both the size of the IN clause
   * used to batch-delete owned rows and the number of HRC rows materialized into heap per iteration.
   */
  private static final int DELETE_CHUNK_SIZE = 500;

  private final Provider<OwnerDAO> ownerDAOProvider;

  @Inject
  public HostedRepositoryComponentDAO(
      final OperationalDataStore operationalDataStore,
      final Provider<OwnerDAO> ownerDAOProvider)
  {
    super(operationalDataStore);
    this.ownerDAOProvider = ownerDAOProvider;
  }

  @Override
  public Table<?> getJooqTable() {
    return HOSTED_REPOSITORY_COMPONENT;
  }

  @Override
  public Class<HostedRepositoryComponent> getEntityClass() {
    return HostedRepositoryComponent.class;
  }

  public HostedRepositoryComponent getByRepositoryIdAndPathname(
      TransactionContext tx,
      String repositoryId,
      String pathname)
  {
    return tx.dsl()
        .selectFrom(HOSTED_REPOSITORY_COMPONENT)
        .where(HOSTED_REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
        .and(HOSTED_REPOSITORY_COMPONENT.PATHNAME.eq(pathname))
        .fetchOneInto(HostedRepositoryComponent.class);
  }

  public List<HostedRepositoryComponent> getByRepositoryId(TransactionContext tx, String repositoryId) {
    return tx.dsl()
        .selectFrom(HOSTED_REPOSITORY_COMPONENT)
        .where(HOSTED_REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
        .fetchInto(HostedRepositoryComponent.class);
  }

  /**
   * Returns every hosted-repository component in the repository carrying the given hash. More than one
   * pathname can hold the same bytes, so this is a list.
   */
  public List<HostedRepositoryComponent> getByRepositoryIdAndHash(
      final TransactionContext tx,
      final String repositoryId,
      final String hash)
  {
    return tx.dsl()
        .selectFrom(HOSTED_REPOSITORY_COMPONENT)
        .where(HOSTED_REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
        .and(HOSTED_REPOSITORY_COMPONENT.HASH.eq(hash))
        .fetchInto(HostedRepositoryComponent.class);
  }

  /**
   * Returns all HRCs whose enclosing repository has the given ancestor (an org, repo container,
   * repo manager, or a repository itself). Joins through {@code hosted_repository_component_ancestor}.
   */
  public List<HostedRepositoryComponent> getByAncestorId(TransactionContext tx, String ancestorId) {
    return tx.dsl()
        .select(HOSTED_REPOSITORY_COMPONENT.fields())
        .from(HOSTED_REPOSITORY_COMPONENT)
        .join(HOSTED_REPOSITORY_COMPONENT_ANCESTOR)
        .on(HOSTED_REPOSITORY_COMPONENT_ANCESTOR.HOSTED_REPOSITORY_COMPONENT_ID
            .eq(HOSTED_REPOSITORY_COMPONENT.HOSTED_REPOSITORY_COMPONENT_ID))
        .where(HOSTED_REPOSITORY_COMPONENT_ANCESTOR.ANCESTOR_ID.eq(ancestorId))
        .and(HOSTED_REPOSITORY_COMPONENT_ANCESTOR.HOSTED_REPOSITORY_COMPONENT_ID
            .ne(HOSTED_REPOSITORY_COMPONENT_ANCESTOR.ANCESTOR_ID))
        .fetchInto(HostedRepositoryComponent.class);
  }

  /**
   * Returns one page of monitoring candidates: every hosted-repository component in a
   * monitoring-enabled hosted repository, ordered by {@code hosted_repository_component_id DESC} and
   * keyset-advanced past {@code cursor}.
   * <p>
   * There is deliberately no {@code last_evaluation_time} predicate. A component's evaluation history
   * lives in {@code policy_evaluation}, which the monitoring flow reads directly, so this table holds
   * no evaluation state to filter on. Guarding against re-enqueueing within a cycle is not this
   * query's job either: the producer job is {@code @DisallowConcurrentExecution} and runs daily, which
   * prevents overlapping cycles structurally. This mirrors how Lifecycle monitoring enumerates its
   * work — {@code PolicyMonitor.evaluateApplications} pages {@code applicationDAO.getAll(page, pageSize)}
   * with no time filter.
   *
   * @param tx the transaction the read participates in
   * @param limit page size
   * @param cursor keyset position from the previous page, or null for the first page
   */
  public List<HostedRepositoryComponent> getMonitoringEligiblePage(
      final TransactionContext tx,
      final int limit,
      @Nullable final EligibilityCursor cursor)
  {
    // Keyset on the primary key alone: hosted_repository_component carries no timestamp, and the PK is
    // unique, so it totally orders the table on its own. A cursor's time component is unused here.
    Condition cursorCondition = cursor == null
        ? DSL.trueCondition()
        : HOSTED_REPOSITORY_COMPONENT.HOSTED_REPOSITORY_COMPONENT_ID.lessThan(cursor.repositoryComponentId());

    return tx.dsl()
        .select(HOSTED_REPOSITORY_COMPONENT.fields())
        .from(HOSTED_REPOSITORY_COMPONENT)
        .join(REPOSITORY)
        .on(REPOSITORY.REPOSITORY_ID.eq(HOSTED_REPOSITORY_COMPONENT.REPOSITORY_ID))
        .where(REPOSITORY.MONITORING_ENABLED.isTrue())
        .and(REPOSITORY.REPOSITORY_TYPE.eq(RepositoryType.hosted.name()))
        .and(cursorCondition)
        .orderBy(HOSTED_REPOSITORY_COMPONENT.HOSTED_REPOSITORY_COMPONENT_ID.desc())
        .limit(limit)
        .fetchInto(HostedRepositoryComponent.class);
  }

  /**
   * Writes {@code owner_component_id} only, leaving every other column at its stored value.
   * <p>
   * Callers pin the owner component from an entity they read before an HDS upload and a policy
   * evaluation, so their in-memory copy can be stale by the time the pin runs. {@link #update} would
   * rewrite all columns from that stale copy and silently revert a concurrent writer's
   * {@code component_id} — the NXRM id that component-keyed waivers and quarantine join on.
   */
  public int updateOwnerComponentId(TransactionContext tx, String id, String ownerComponentId) {
    return tx.dsl()
        .update(HOSTED_REPOSITORY_COMPONENT)
        .set(HOSTED_REPOSITORY_COMPONENT.OWNER_COMPONENT_ID, ownerComponentId)
        .where(HOSTED_REPOSITORY_COMPONENT.HOSTED_REPOSITORY_COMPONENT_ID.eq(id))
        .execute();
  }

  /**
   * Writes {@code hash} and {@code component_id} only, leaving every other column at its stored
   * value — notably {@code owner_component_id}, which the resolver's caller does not own.
   * <p>
   * A null {@code componentId} leaves the stored value alone rather than clearing it: the NXRM id is
   * supplied opportunistically and absence means "no fresher value", not "no id".
   */
  public int updateHashAndComponentId(TransactionContext tx, String id, String hash, String componentId) {
    var update = tx.dsl()
        .update(HOSTED_REPOSITORY_COMPONENT)
        .set(HOSTED_REPOSITORY_COMPONENT.HASH, hash);
    if (componentId != null) {
      update = update.set(HOSTED_REPOSITORY_COMPONENT.COMPONENT_ID, componentId);
    }
    return update.where(HOSTED_REPOSITORY_COMPONENT.HOSTED_REPOSITORY_COMPONENT_ID.eq(id)).execute();
  }

  @Override
  public void delete(TransactionContext tx, HostedRepositoryComponent entity) {
    ownerDAOProvider.get().cascadeDelete(tx, entity);
    super.delete(tx, entity);
  }

  /**
   * Deletes all hosted_repository_component rows for the given repository, along with their owned scan-based rows,
   * using chunked set-based SQL instead of a per-row delete loop.
   * <p>
   * HRC IDs are paged in fixed-size batches (no {@code OFFSET} — each batch is deleted before the next page is
   * fetched, so there is no offset drift) to avoid materializing a repository's entire HRC list into heap.
   * </p>
   *
   * @param tx the transaction context that all operations will participate in
   * @param repositoryId the repository whose HRC rows should be deleted
   */
  public void deleteByRepositoryId(TransactionContext tx, String repositoryId) {
    List<String> ids;
    while (!(ids = tx.dsl()
        .select(HOSTED_REPOSITORY_COMPONENT.HOSTED_REPOSITORY_COMPONENT_ID)
        .from(HOSTED_REPOSITORY_COMPONENT)
        .where(HOSTED_REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
        .limit(DELETE_CHUNK_SIZE)
        .fetch(HOSTED_REPOSITORY_COMPONENT.HOSTED_REPOSITORY_COMPONENT_ID)).isEmpty())
    {
      ownerDAOProvider.get().cascadeDeleteByOwnerIds(tx, ids);
      tx.dsl()
          .deleteFrom(HOSTED_REPOSITORY_COMPONENT)
          .where(HOSTED_REPOSITORY_COMPONENT.HOSTED_REPOSITORY_COMPONENT_ID.in(ids))
          .execute();
    }
  }
}
