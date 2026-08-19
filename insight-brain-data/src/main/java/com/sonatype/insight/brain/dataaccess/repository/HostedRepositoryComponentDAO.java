/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.EligibilityCursor;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.HostedRepositoryComponentAncestor.HOSTED_REPOSITORY_COMPONENT_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.HostedRepositoryComponent.HOSTED_REPOSITORY_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.LastPolicyEvaluation.LAST_POLICY_EVALUATION;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerComponent.OWNER_COMPONENT;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyEvaluation.POLICY_EVALUATION;
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

  public record HrcWithOwnerComponent(HostedRepositoryComponent hrc, @Nullable OwnerComponent ownerComponent)
  {
  }

  public List<HrcWithOwnerComponent> getByRepositoryIdPaged(
      String repositoryId,
      String filter,
      int limit,
      int offset)
  {
    if (repositoryId == null || repositoryId.isEmpty()) {
      return List.of();
    }
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .select(HOSTED_REPOSITORY_COMPONENT.fields())
          .select(OWNER_COMPONENT.fields())
          .from(HOSTED_REPOSITORY_COMPONENT)
          .leftJoin(OWNER_COMPONENT)
          .on(OWNER_COMPONENT.OWNER_COMPONENT_ID.eq(HOSTED_REPOSITORY_COMPONENT.OWNER_COMPONENT_ID))
          .where(HOSTED_REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId));
      Condition filterCondition = displayNameOrPathnameFilter(filter);
      if (filterCondition != null) {
        query = query.and(filterCondition);
      }
      // Pathname + HRC id tiebreaker for stable paging.
      return query
          .orderBy(HOSTED_REPOSITORY_COMPONENT.PATHNAME.asc(),
              HOSTED_REPOSITORY_COMPONENT.HOSTED_REPOSITORY_COMPONENT_ID.asc())
          .limit(limit)
          .offset(offset)
          .fetch(this::toHrcWithOwnerComponent);
    }
  }

  @Nullable
  public HrcWithOwnerComponent getByRepositoryIdAndComponentId(String repositoryId, String componentId) {
    if (StringUtils.isBlank(repositoryId) || StringUtils.isBlank(componentId)) {
      return null;
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(HOSTED_REPOSITORY_COMPONENT.fields())
          .select(OWNER_COMPONENT.fields())
          .from(HOSTED_REPOSITORY_COMPONENT)
          .leftJoin(OWNER_COMPONENT)
          .on(OWNER_COMPONENT.OWNER_COMPONENT_ID.eq(HOSTED_REPOSITORY_COMPONENT.OWNER_COMPONENT_ID))
          .where(HOSTED_REPOSITORY_COMPONENT.HOSTED_REPOSITORY_COMPONENT_ID.eq(componentId))
          .and(HOSTED_REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId))
          .fetchOne(this::toHrcWithOwnerComponent);
    }
  }

  public int countByRepositoryIdWithFilter(String repositoryId, String filter) {
    if (repositoryId == null || repositoryId.isEmpty()) {
      return 0;
    }
    try (TransactionContext tx = createTransactionContext()) {
      var query = tx.dsl()
          .selectCount()
          .from(HOSTED_REPOSITORY_COMPONENT)
          .leftJoin(OWNER_COMPONENT)
          .on(OWNER_COMPONENT.OWNER_COMPONENT_ID.eq(HOSTED_REPOSITORY_COMPONENT.OWNER_COMPONENT_ID))
          .where(HOSTED_REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId));
      Condition filterCondition = displayNameOrPathnameFilter(filter);
      if (filterCondition != null) {
        query = query.and(filterCondition);
      }
      return query.fetchOne(0, Integer.class);
    }
  }

  // Case-insensitive substring match on pathname OR coordinates_json (source of displayName).
  // Escape order matters: backslash first, then LIKE metacharacters.
  @Nullable
  private static Condition displayNameOrPathnameFilter(String filter) {
    if (filter == null || filter.isEmpty()) {
      return null;
    }
    String escaped = filter.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    String pattern = "%" + escaped + "%";
    return HOSTED_REPOSITORY_COMPONENT.PATHNAME.likeIgnoreCase(pattern, '\\')
        .or(OWNER_COMPONENT.COMPONENT_ID_COORDINATES_JSON.likeIgnoreCase(pattern, '\\'));
  }

  private HrcWithOwnerComponent toHrcWithOwnerComponent(Record record) {
    HostedRepositoryComponent hrc =
        record.into(HOSTED_REPOSITORY_COMPONENT.fields()).into(HostedRepositoryComponent.class);
    OwnerComponent oc = record.get(OWNER_COMPONENT.OWNER_COMPONENT_ID) != null
        ? record.into(OWNER_COMPONENT.fields()).into(OwnerComponent.class)
        : null;
    return new HrcWithOwnerComponent(hrc, oc);
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

  public Map<String, Date> getLastScanTimesByRepositoryIds(Collection<String> repositoryIds) {
    if (repositoryIds == null || repositoryIds.isEmpty()) {
      return Map.of();
    }
    Map<String, Date> merged = new HashMap<>();
    getListWithSqlInClause(repositoryIds, chunk -> {
      try (TransactionContext tx = createTransactionContext()) {
        getLastScanTimesByRepositoryIds(tx, chunk)
            .forEach((repoId, date) -> merged.merge(repoId, date, (a, b) -> a.after(b) ? a : b));
      }
      return List.of();
    }, getDataStore());
    return merged;
  }

  public Map<String, Date> getLastScanTimesByRepositoryIds(TransactionContext tx, Collection<String> repositoryIds) {
    if (repositoryIds == null || repositoryIds.isEmpty()) {
      return Map.of();
    }
    return tx.dsl()
        .select(HOSTED_REPOSITORY_COMPONENT.REPOSITORY_ID, DSL.max(POLICY_EVALUATION.TIME))
        .from(HOSTED_REPOSITORY_COMPONENT)
        .join(LAST_POLICY_EVALUATION)
        .on(LAST_POLICY_EVALUATION.OWNER_ID.eq(HOSTED_REPOSITORY_COMPONENT.HOSTED_REPOSITORY_COMPONENT_ID))
        .join(POLICY_EVALUATION)
        .on(POLICY_EVALUATION.POLICY_EVALUATION_ID.eq(LAST_POLICY_EVALUATION.POLICY_EVALUATION_ID))
        .where(HOSTED_REPOSITORY_COMPONENT.REPOSITORY_ID.in(repositoryIds))
        .groupBy(HOSTED_REPOSITORY_COMPONENT.REPOSITORY_ID)
        .fetch()
        .stream()
        .filter(r -> r.value2() != null)
        .collect(Collectors.toMap(Record2::value1, r -> new Date(r.value2().getTime())));
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
