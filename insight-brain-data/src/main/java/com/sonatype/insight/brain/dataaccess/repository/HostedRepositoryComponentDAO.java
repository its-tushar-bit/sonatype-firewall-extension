/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.HostedRepositoryComponentAncestor.HOSTED_REPOSITORY_COMPONENT_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.HostedRepositoryComponent.HOSTED_REPOSITORY_COMPONENT;

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
