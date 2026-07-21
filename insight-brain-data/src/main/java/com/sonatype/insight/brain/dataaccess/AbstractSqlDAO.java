/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sonatype.insight.brain.common.config.ConfigUtil;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.jooq.DialectHelper;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.model.HasStringId;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.Table;

import static java.util.stream.Collectors.toList;

public abstract class AbstractSqlDAO<T extends HasStringId>
    extends AbstractDAO<T>
{
  private final SearchIndexManager searchIndexManager;

  public static final int H2_IN_OPERATOR_THRESHOLD = 2000;

  public static final int POSTGRES_IN_OPERATOR_THRESHOLD = 65_535;

  // A query passed into `getListWithSqlInClause` which uses POSTGRES_IN_OPERATOR_THRESHOLD might have additional
  // parameters beyond those in the `IN` clause. The 65,535 Postgres JDBC limit applies to ALL parameters, not just
  // those in the `IN` clause itself. So we add an arbitrary buffer of 100 when we partition the list of values. This
  // is to keep the TOTAL parameter count under the max.
  private static final int PARAMETER_BUFFER = 100;

  public static final int DEFAULT_MAX_ALLOWED_DB_RESULTS = 500_000;

  // Any query that returns more than 1 result should be paged. We're not there yet but this acts as a safeguard while
  // we work towards that.
  @VisibleForTesting
  static int MAX_ALLOWED_DB_RESULTS =
      ConfigUtil.getIntegerConfig("com.sonatype.insight.maxAllowedDbResults", DEFAULT_MAX_ALLOWED_DB_RESULTS);

  /**
   * Constructor for DAOs that require the search index. These DAOs must override one of the methods:
   * <ul>
   * <li>{@link #newSearchIndexChange(HasStringId)}</li>
   * <li>{@link #newSearchIndexChangeForInsert(HasStringId)}</li>
   * <li>{@link #newSearchIndexChangeForUpdate(HasStringId)}</li>
   * <li>{@link #newSearchIndexChangeForDelete(HasStringId)}</li>
   * </ul>
   */
  protected AbstractSqlDAO(final SearchIndexManager searchIndexManager) {
    this.searchIndexManager = searchIndexManager;
  }

  /**
   * Constructor for DAOs that will <B>NOT</B> require the search index. The {@link #searchIndexManager} will be set to
   * null. If the <T> entity for this DAO needs to be searchable then use the {@link AbstractOperationalSqlDAO}
   * constructor that takes a SearchIndexManager.
   */
  protected AbstractSqlDAO() {
    this(null);
  }

  /**
   * Gets a page of entities starting after the given ID using jOOQ.
   * <p>
   * This implementation uses the jOOQ table reference from {@link #getJooqTable()} to perform the select operation.
   * </p>
   *
   * @param tx the transaction context
   * @param lastProcessedId the ID to start after (exclusive)
   * @param pageSize the maximum number of results to return
   * @return list of entities
   */
  public List<T> getPage(TransactionContext tx, String lastProcessedId, int pageSize) {
    Table<?> table = getJooqTable();
    var idField = getIdField(table);
    return tx.dsl()
        .selectFrom(table)
        .where(idField.gt(lastProcessedId))
        .orderBy(idField)
        .limit(pageSize)
        .fetch(this::toEntity);
  }

  protected String newUUID() {
    return IdUtil.newUUID();
  }

  /**
   * Generates and sets a UUID for the entity if its ID is null or empty. Call this at the start of insert() methods in
   * subclasses.
   *
   * @param entity the entity to generate an ID for
   */
  protected void generateIdIfNeeded(T entity) {
    String id = entity.getId();
    if (id == null || id.trim().isEmpty()) {
      entity.setId(newUUID());
    }
  }

  @Override
  public int insert(final TransactionContext tx, final T entity, final boolean ignoreDuplicateKey) {
    generateIdIfNeeded(entity);
    return super.insert(tx, entity, ignoreDuplicateKey);
  }

  @Override
  public int insertBatch(final TransactionContext tx, final List<T> entities, final boolean ignoreDuplicateKey) {
    for (T entity : entities) {
      generateIdIfNeeded(entity);
    }
    return super.insertBatch(tx, entities, ignoreDuplicateKey);
  }

  /**
   * Handle search index changes for entity deletion.
   * <p>
   * This method only handles search index changes - it does NOT perform the actual database delete. The actual delete
   * is typically performed by {@link AbstractOperationalSqlDAO#delete(TransactionContext, HasStringId)}, which uses
   * jOOQ and then calls this method.
   * </p>
   * <p>
   * Subclasses that override delete() completely (without calling super) should handle search index changes themselves
   * by calling {@link #shouldAddSearchIndexChange(TransactionContext, HasStringId)} and
   * {@link #insertSearchIndexChange(TransactionContext, SearchIndexChange)} directly:
   * </p>
   *
   * <pre>
   * {@literal @}Override
   * public void delete(TransactionContext tx, MyEntity entity) {
   *   // Perform the actual delete using jOOQ
   *   tx.dsl().deleteFrom(MY_TABLE)
   *       .where(MY_TABLE.ID.eq(entity.getId()))
   *       .execute();
   *
   *   // Handle search index changes
   *   if (shouldAddSearchIndexChange(tx, entity)) {
   *     insertSearchIndexChange(tx, newSearchIndexChangeForDelete(entity));
   *   }
   * }
   * </pre>
   *
   * @param tx the transaction context
   * @param entity the entity to delete
   */
  @Override
  public void delete(TransactionContext tx, T entity) {
    super.delete(tx, entity);
    if (shouldAddSearchIndexChange(tx, entity)) {
      insertSearchIndexChange(tx, newSearchIndexChangeForDelete(entity));
    }
  }

  /**
   * Get all entities of this type. Subclasses can override this method to provide custom ordering or filtering.
   *
   * @param tx the transaction context
   * @return list of all entities
   */
  public List<T> getAll(TransactionContext tx) {
    Table<?> table = getJooqTable();
    return tx.dsl()
        .selectFrom(table)
        .fetch(this::toEntity);
  }

  public List<T> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAll(tx);
    }
  }

  /**
   * Gets the count of all entities of this type using jOOQ.
   * <p>
   * This implementation uses the jOOQ table reference from {@link #getJooqTable()} to perform the count operation.
   * </p>
   *
   * @param tx the transaction context
   * @return the count of entities
   */
  public long getCount(TransactionContext tx) {
    Table<?> table = getJooqTable();
    return tx.dsl()
        .selectCount()
        .from(table)
        .fetchOne(0, Long.class);
  }

  /**
   * Gets the count of all entities of this type in a new transaction using jOOQ.
   *
   * @return the count of entities
   */
  public long getCount() {
    try (TransactionContext tx = createTransactionContext()) {
      return getCount(tx);
    }
  }

  public String getEntityName() {
    return getEntityClass().getSimpleName();
  }

  @Override
  public TransactionContext createTransactionContext() {
    DataStore dataStore = getDataStore();
    return new TransactionContext(
        dataStore.getDataSource(),
        DialectHelper.detectDialect(dataStore),
        dataStore.getDatabaseSchema());
  }

  protected void insertSearchIndexChange(final TransactionContext tx, final SearchIndexChange searchIndexChange) {
    if (searchIndexManager != null && searchIndexChange != null) {
      searchIndexManager.insert(tx, searchIndexChange);
    }
  }

  protected SearchIndexChange newSearchIndexChangeForInsert(T entity) {
    return newSearchIndexChange(entity);
  }

  protected SearchIndexChange newSearchIndexChangeForUpdate(T entity) {
    return newSearchIndexChange(entity);
  }

  protected SearchIndexChange newSearchIndexChangeForDelete(T entity) {
    return newSearchIndexChange(entity);
  }

  protected boolean shouldAddSearchIndexChange(
      @SuppressWarnings("unused") TransactionContext tx,
      @SuppressWarnings("unused") T entity)
  {
    return searchIndexManager != null;
  }

  protected SearchIndexChange newSearchIndexChange(@SuppressWarnings("unused") T entity) {
    // by default, no contribution to the search index
    return null;
  }

  public boolean isDatabaseEmbedded(DataStore datastore) {
    return datastore.isDatabaseEmbedded();
  }

  // Note: jOOQ's inlineThreshold (default 0 = dialect-specific limits) automatically inlines all bind variables when
  // the vendor limit is reached (65,535 for PostgreSQL; no limit for H2), so splitting is not required for correctness.
  // Splitting pros: avoids H2's superlinear IN clause performance (see below); parameterized queries get better plan
  // caching than large inlined SQL on PostgreSQL.
  // Splitting cons: multiple round trips instead of one, added code complexity for callers.
  // The H2 threshold of 2,000 was chosen in CLM-6085 due to H2's roughly O(n^2) IN clause performance:
  // 100 items=7ms, 1k=137ms, 2k=393ms, 5k=2.2s, 10k=8.6s. Those benchmarks were on OpenJPA — may differ with jOOQ.
  // See InOperatorThresholdTest / InOperatorThresholdPostgresTest for correctness verification.
  public int getInOperatorThreshold(DataStore dataStore) {
    return isDatabaseEmbedded(dataStore) ? H2_IN_OPERATOR_THRESHOLD : POSTGRES_IN_OPERATOR_THRESHOLD;
  }

  /**
   * Computes the maximum number of list elements that can safely fit in one query, given the database's total
   * parameter limit, how many bind parameters each element uses, and how many extra bind parameters exist outside
   * the IN clause.
   *
   * @param dataStore The data store (determines H2 vs PostgreSQL threshold).
   * @param paramsPerElement Number of bind parameters each list element contributes (1 for simple IN, 2+ for
   *          row-value).
   * @param extraParams Number of additional bind parameters outside the IN clause.
   */
  protected int getPartitionSize(DataStore dataStore, int paramsPerElement, int extraParams) {
    int threshold = getInOperatorThreshold(dataStore);
    return Math.max(1, (threshold - extraParams) / paramsPerElement);
  }

  /**
   * This method should be used for queries that use an "IN" clause. H2 and Postgres limit the number of elements in
   * "IN" clauses. This method breaks the list of values into partitions, runs the given query on each partition and
   * merges the results from all partitions.
   *
   * @param <E> The type of the values in the list to be used in the "IN" clause.
   * @param inClauseValues List of values to be used in the "IN" clause.
   * @param getter Function to be used to query the values.
   * @param dataStore A related set of data/tables.
   */
  protected <E, U> List<U> getListWithSqlInClause(
      Collection<E> inClauseValues,
      Function<Collection<E>, List<U>> getter,
      DataStore dataStore)
  {
    return getListWithSqlInClause(inClauseValues, getter, dataStore, 1, PARAMETER_BUFFER);
  }

  /**
   * Variant for queries where each list element contributes multiple bind parameters (e.g. row-value expressions),
   * or where the query has additional bind parameters outside the IN clause.
   *
   * @param <E> The type of the values in the list to be used in the "IN" clause.
   * @param inClauseValues List of values to be used in the "IN" clause.
   * @param getter Function to be used to query the values.
   * @param dataStore A related set of data/tables.
   * @param paramsPerElement Number of bind parameters each list element contributes (e.g. 2 for row-value tuples).
   * @param extraParams Number of additional bind parameters in the query outside the IN clause (e.g. ownerId, type).
   */
  protected <E, U> List<U> getListWithSqlInClause(
      Collection<E> inClauseValues,
      Function<Collection<E>, List<U>> getter,
      DataStore dataStore,
      int paramsPerElement,
      int extraParams)
  {
    if (CollectionUtils.isEmpty(inClauseValues)) {
      return List.of();
    }
    int partitionSize = getPartitionSize(dataStore, paramsPerElement, extraParams);
    // Uses > (not >=) intentionally: partitionSize is computed so that exactly partitionSize elements
    // produce exactly threshold bind params, which is safe (PG limit is inclusive).
    if (inClauseValues.size() > partitionSize) {
      return getStreamWithSqlInClause(inClauseValues, chunk -> getter.apply(chunk).stream(),
          dataStore, paramsPerElement, extraParams)
              .collect(toList());
    }
    return getter.apply(inClauseValues);
  }

  /**
   * Stream variant of {@link #getListWithSqlInClause}. The getter function returns a {@link Stream} (e.g. from
   * jOOQ's {@code fetchStream()}) instead of a {@link List}. Partitions are processed lazily via
   * {@link Stream#flatMap}, which closes each sub-stream before opening the next.
   *
   * <p>
   * <b>Important:</b> the caller must close the returned stream (try-with-resources) to release any underlying
   * database cursors.
   * </p>
   */
  protected <E, U> Stream<U> getStreamWithSqlInClause(
      Collection<E> inClauseValues,
      Function<Collection<E>, Stream<U>> getter,
      DataStore dataStore)
  {
    return getStreamWithSqlInClause(inClauseValues, getter, dataStore, 1, PARAMETER_BUFFER);
  }

  /**
   * Stream variant for queries where each list element contributes multiple bind parameters.
   *
   * @see #getListWithSqlInClause(Collection, Function, DataStore, int, int)
   */
  protected <E, U> Stream<U> getStreamWithSqlInClause(
      Collection<E> inClauseValues,
      Function<Collection<E>, Stream<U>> getter,
      DataStore dataStore,
      int paramsPerElement,
      int extraParams)
  {
    if (CollectionUtils.isEmpty(inClauseValues)) {
      return Stream.of();
    }
    int partitionSize = getPartitionSize(dataStore, paramsPerElement, extraParams);
    if (inClauseValues.size() > partitionSize) {
      List<E> inClauseValuesList;
      if (inClauseValues instanceof List<E>) {
        inClauseValuesList = (List<E>) inClauseValues;
      }
      else {
        inClauseValuesList = new ArrayList<>(inClauseValues);
      }

      return Lists.partition(inClauseValuesList, partitionSize).stream().flatMap(getter);
    }
    else {
      return getter.apply(inClauseValues);
    }
  }

  /**
   * Creates a native SQL query with pagination support using jOOQ DSL. This method uses the provided SQL query string
   * to create a native query using the given {@link TransactionContext}. It also applies pagination by appending OFFSET
   * and LIMIT clauses.
   *
   * @param tx the {@link TransactionContext} used to create the query. It must not be null.
   * @param sQuery the native SQL query string to execute. It must not be null or empty.
   * @param offset the starting position of the first result (zero-based). For example, to skip the first 10 results,
   *          set this to 10.
   * @param pageSize the maximum number of results to return. A positive integer specifies the page size.
   * @return a {@link ResultQuery} object configured with the specified SQL query, offset, and page size.
   */
  public static ResultQuery<Record> createNativePaginationQuery(
      TransactionContext tx,
      String sQuery,
      int offset,
      int pageSize)
  {
    String paginatedQuery = sQuery + " OFFSET ? LIMIT ?";
    return tx.dsl().resultQuery(paginatedQuery, offset, pageSize);
  }

  public T getByIdNotNull(TransactionContext tx, String id) {
    T entity = getById(tx, id);
    if (entity == null) {
      throw new NotFoundException(getEntityName() + " with ID " + id + " does not exist.");
    }
    return entity;
  }

  public T getByIdNotNull(String id) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdNotNull(tx, id);
    }
  }

  protected abstract DataStore getDataStore();

  protected static <T> Integer getInteger(T value) {
    if (value instanceof Short) {
      return Integer.valueOf((Short) value);
    }
    if (value instanceof Integer) {
      return (Integer) value;
    }
    if (value instanceof Long) {
      return ((Long) value).intValue();
    }

    return null;
  }
}
