/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sonatype.insight.brain.common.config.ConfigUtil;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.AbstractDAO;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.model.HasStringId;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import jakarta.persistence.EntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;

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

  private final Class<T> entityClass;

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
    entityClass = (Class<T>) getParameterizedSuperClass().getActualTypeArguments()[0];
  }

  /**
   * Constructor for DAOs that will <B>NOT</B> require the search index. The {@link #searchIndexManager} will be set to
   * null. If the <T> entity for this DAO needs to be searchable then use the
   * {@link #AbstractOperationalSqlDAO(OperationalDataStore, SearchIndexManager)} constructor.
   */
  protected AbstractSqlDAO() {
    this(null);
  }

  @Override
  protected List<T> getList(TransactionContext tx, String sQuery, Object... parameters) {
    jakarta.persistence.Query query = this.createQuery(tx, sQuery, parameters);
    query.setMaxResults(MAX_ALLOWED_DB_RESULTS);

    return query.getResultList();
  }

  public List<T> getPage(TransactionContext tx, String lastProcessedId, int pageSize) {
    String sQuery = "SELECT entity FROM " + getEntityName()
        + " entity WHERE entity.id > :lastProcessedId ORDER BY entity.id";
    jakarta.persistence.Query query = tx.createQuery(sQuery);
    query.setParameter("lastProcessedId", lastProcessedId);
    query.setMaxResults(pageSize);
    return query.getResultList();
  }

  private String newUUID() {
    return IdUtil.newUUID();
  }

  @Override
  public void insert(TransactionContext tx, T entity) {
    String id = entity.getId();
    if (id == null || id.trim().isEmpty()) {
      entity.setId(newUUID());
    }
    super.insert(tx, entity);

    if (shouldAddSearchIndexChange(tx, entity)) {
      insertSearchIndexChange(tx, newSearchIndexChangeForInsert(entity));
    }
  }

  @Override
  public void update(TransactionContext tx, T entity) {
    super.update(tx, entity);
    if (shouldAddSearchIndexChange(tx, entity)) {
      insertSearchIndexChange(tx, newSearchIndexChangeForUpdate(entity));
    }
  }

  @Override
  public void delete(TransactionContext tx, T entity) {
    super.delete(tx, entity);
    if (shouldAddSearchIndexChange(tx, entity)) {
      insertSearchIndexChange(tx, newSearchIndexChangeForDelete(entity));
    }
  }

  public long getCount(TransactionContext tx) {
    String sQuery = "SELECT COUNT(entity) FROM " + getEntityName() + " entity";
    return getSingle(tx, Long.class, sQuery);
  }

  public long getCount() {
    String sQuery = "SELECT COUNT(entity) FROM " + getEntityName() + " entity";
    return getSingle(Long.class, sQuery);
  }

  public String getEntityName() {
    return entityClass.getSimpleName();
  }

  @Override
  public abstract TransactionContext createTransactionContext();

  private void insertSearchIndexChange(final TransactionContext tx, final SearchIndexChange searchIndexChange) {
    if (searchIndexManager != null) {
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

  public int getInOperatorThreshold(DataStore dataStore) {
    return isDatabaseEmbedded(dataStore) ? H2_IN_OPERATOR_THRESHOLD : POSTGRES_IN_OPERATOR_THRESHOLD;
  }

  /**
   * This method should be used for queries that use an "IN" clause.
   * H2 and Postgres limit the number of elements in "IN" clauses. This method breaks the list of values into
   * partitions, runs the given query on each partition and merges the results from all partitions.
   *
   * @param <E> The type of the values in the list to be used in the "IN" clause.
   * @param inClauseValues List of values to be used in the "IN" clause.
   * @param getter Function to be used to query the values.
   * @param dataStore A related set of data/tables
   */
  protected <E, U> List<U> getListWithSqlInClause(
      Collection<E> inClauseValues,
      Function<Collection<E>, List<U>> getter,
      DataStore dataStore)
  {
    int inOperatorThreshold = getInOperatorThreshold(dataStore);
    if (inClauseValues.size() >= inOperatorThreshold) {
      List<E> inClauseValuesList;
      if (inClauseValues instanceof List<E>) {
        inClauseValuesList = (List<E>) inClauseValues;
      }
      else {
        inClauseValuesList = new ArrayList<>(inClauseValues);
      }

      // Some tests set the inOperatorThreshold to a low value. This prevents the partition size from being negative.
      int partitionSize = Math.max(1, inOperatorThreshold - PARAMETER_BUFFER);
      List<List<E>> inClauseValuesPartitions = Lists.partition(inClauseValuesList, partitionSize);

      return inClauseValuesPartitions.stream().map(getter).flatMap(Collection::stream).collect(toList());
    }
    else {
      return getter.apply(inClauseValues);
    }
  }

  public static jakarta.persistence.Query createPaginationQuery(
      TransactionContext tx,
      String sQuery,
      int offset,
      int pageSize)
  {
    jakarta.persistence.Query query = tx.createQuery(sQuery);
    query.setFirstResult(offset).setMaxResults(pageSize);
    return query;
  }

  /**
   * Creates a native SQL query with pagination support.
   * This method uses the provided SQL query string to create a native query using the given
   * {@link TransactionContext}. It also applies pagination by setting the starting offset
   * and the maximum number of results to return.
   *
   * @param tx the {@link TransactionContext} used to create the query. It must not be null.
   * @param sQuery the native SQL query string to execute. It must not be null or empty.
   * @param offset the starting position of the first result (zero-based). For example,
   *          to skip the first 10 results, set this to 10.
   * @param pageSize the maximum number of results to return. A positive integer specifies the page size.
   * @return a {@link jakarta.persistence.Query} object configured with the specified SQL query,
   *         offset, and page size.
   */
  public static jakarta.persistence.Query createNativePaginationQuery(
      TransactionContext tx,
      String sQuery,
      int offset,
      int pageSize)
  {
    jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
    query.setFirstResult(offset).setMaxResults(pageSize);
    return query;
  }

  @Override
  public T getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM " + getEntityName() + " entity WHERE entity.id=?1";
    return get(tx, sQuery, id);
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

  protected <C> List<C> getScalars(Class<C> type, String sQuery, Object... parameters) {
    try (TransactionContext tx = createTransactionContext()) {
      return getScalars(tx, type, sQuery, parameters);
    }
  }

  protected <C> List<C> getScalars(TransactionContext tx, Class<C> type, String sQuery, Object... parameters) {
    try (Stream<C> resultStream = getScalarsStream(tx, type, sQuery, parameters)) {
      return resultStream.toList();
    }
  }

  protected List<?> getUntypedResult(String sQuery, Object... parameters) {
    try (TransactionContext tx = createTransactionContext()) {
      return getUntypedResult(tx, sQuery, parameters);
    }
  }

  protected List<?> getUntypedResult(TransactionContext tx, String sQuery, Object... parameters) {
    try (Stream<?> resultStream = getUntypedStream(tx, sQuery, parameters)) {
      return resultStream.toList();
    }
  }

  /**
   * Note: This stream should be closed after use
   */
  protected <C> Stream<C> getScalarsStream(TransactionContext tx, Class<C> type, String sQuery, Object... parameters) {
    Stream<?> resultStream = createQuery(tx, sQuery, parameters).getResultStream();
    return resultStream.map(type::cast);
  }

  protected <C> List<C> getScalarsNative(Class<C> type, String sQuery, Object... parameters) {
    try (TransactionContext tx = createTransactionContext()) {
      return getScalarsNative(tx, type, sQuery, parameters);
    }
  }

  protected <C> List<C> getScalarsNative(TransactionContext tx, Class<C> type, String sQuery, Object... parameters) {
    try (Stream<C> resultStream = getScalarsStreamNative(tx, type, sQuery, parameters)) {
      return resultStream.toList();
    }
  }

  /**
   * Note: This stream should be closed after use
   */
  protected Stream<?> getUntypedStream(TransactionContext tx, String sQuery, Object... parameters) {
    return createQuery(tx, sQuery, parameters).getResultStream();
  }

  /**
   * Note: This stream should be closed after use
   */
  protected <C> Stream<C> getScalarsStreamNative(
      TransactionContext tx,
      Class<C> type,
      String sQuery,
      Object... parameters)
  {
    Stream<?> resultStream = createNativeQuery(tx, sQuery, parameters).getResultStream();
    return resultStream.map(type::cast);
  }

  protected ParameterizedType getParameterizedSuperClass() {
    Type genericSuperclass = getClass().getGenericSuperclass();
    if (!(genericSuperclass instanceof ParameterizedType)) {
      genericSuperclass = getClass().getSuperclass().getGenericSuperclass();
    }

    return (ParameterizedType) genericSuperclass;
  }

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

  public void removeEntityFromCache(T entity) {
    if (entity != null) {
      removeEntityFromCacheByPrimaryKey(entity.getId());
    }
  }

  public void removeEntityFromCacheByPrimaryKey(Object primaryKey) {
    getDataStore().getJPAEntityManagerFactory().getCache().evict(entityClass, primaryKey);
  }

  public void clearQueryCache() {
    EntityManagerFactory entityManagerFactory = getDataStore().getJPAEntityManagerFactory();
    OpenJPAEntityManagerFactorySPI openJPAEntityManagerFactorySPI =
        (OpenJPAEntityManagerFactorySPI) OpenJPAPersistence.cast(entityManagerFactory);
    openJPAEntityManagerFactorySPI.getQueryResultCache().evictAll(entityClass);
  }
}
