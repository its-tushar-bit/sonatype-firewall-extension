/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.search.EmptySearchIndexManager;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.model.HasStringId;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.exception.ExceptionUtils;

import static java.util.stream.Collectors.toList;

public abstract class AbstractOperationalSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  //visible for testing
  public static final int H2_IN_OPERATOR_THRESHOLD = 2000;

  //visible for testing
  public static final int POSTGRES_IN_OPERATOR_THRESHOLD = Short.MAX_VALUE;

  public static Map<String, TestEntityLeakDetectionData> testEntityLeaksDetectionData = new LinkedHashMap<>();

  private String entityName;

  private final OperationalDataStore operationalDataStore;

  private final SearchIndexManager searchIndexManager;

  /**
   * Constructor for DAOs that require the search index. These DAOs must override one of the methods:
   * <ul>
   *   <li>{@link #newSearchIndexChange(HasStringId)}</li>
   *   <li>{@link #newSearchIndexChangeForInsert(HasStringId)}</li>
   *   <li>{@link #newSearchIndexChangeForUpdate(HasStringId)}</li>
   *   <li>{@link #newSearchIndexChangeForDelete(HasStringId)}</li>
   * </ul>
   */
  protected AbstractOperationalSqlDAO(
      final OperationalDataStore operationalDataStore,
      final SearchIndexManager searchIndexManager)
  {
    this.operationalDataStore = operationalDataStore;
    this.searchIndexManager = searchIndexManager;
    entityName = ((Class<?>) getParameterizedSuperClass().getActualTypeArguments()[0]).getSimpleName();
  }

  /**
   * Constructor for DAOs that will <B>NOT</B> require the search index. The {@link #searchIndexManager} will be set to
   * a {@link EmptySearchIndexManager} instance. If the <T> entity for this DAO needs to be searchable then use the
   * {@link #AbstractOperationalSqlDAO(OperationalDataStore, SearchIndexManager)} constructor.
   */
  protected AbstractOperationalSqlDAO(OperationalDataStore operationalDataStore) {
    // Note: singleton pattern used to reduce churn in tests
    this(operationalDataStore, EmptySearchIndexManager.getInstance());
  }

  private ParameterizedType getParameterizedSuperClass() {
    Type genericSuperclass = getClass().getGenericSuperclass();
    if (!(genericSuperclass instanceof ParameterizedType)) {
      genericSuperclass = getClass().getSuperclass().getGenericSuperclass();
    }

    return (ParameterizedType) genericSuperclass;
  }

  @Override
  public TransactionContext createTransactionContext() {
    return new TransactionContext(operationalDataStore.getJPAEntityManagerFactory().createEntityManager());
  }

  public boolean isDatabaseEmbedded() {
    return operationalDataStore.isDatabaseEmbedded();
  }

  protected boolean isDatabasePostgresql() {
    return !operationalDataStore.isDatabaseInMemory() && org.postgresql.Driver.class.getName()
        .equals(operationalDataStore.getDatabaseConfig().getDriverClassName());
  }

  public int getInOperatorThreshold() {
    return isDatabaseEmbedded() ? H2_IN_OPERATOR_THRESHOLD : POSTGRES_IN_OPERATOR_THRESHOLD;
  }

  @Override
  public void insert(TransactionContext tx, T entity) {
    super.insert(tx, entity);

    if (detectTestEntityLeaks() && operationalDataStore.isDatabaseInMemory()) {
      Exception e = new Exception("Entity of type " + entity.getClass().getName() + " created at:");
      testEntityLeaksDetectionData.put(entity.getId(),
          new TestEntityLeakDetectionData(this, ExceptionUtils.getStackTrace(e)));
    }

    insertSearchIndexChange(tx, newSearchIndexChangeForInsert(entity));
  }

  @Override
  public void update(TransactionContext tx, T entity) {
    super.update(tx, entity);
    insertSearchIndexChange(tx, newSearchIndexChangeForUpdate(entity));
  }

  @Override
  public void delete(TransactionContext tx, T entity) {
    super.delete(tx, entity);

    if (entity != null && detectTestEntityLeaks() && operationalDataStore.isDatabaseInMemory()) {
      testEntityLeaksDetectionData.remove(entity.getId());
    }

    insertSearchIndexChange(tx, newSearchIndexChangeForDelete(entity));
  }

  public static javax.persistence.Query createPaginationQuery(
      TransactionContext tx,
      String sQuery,
      int offset,
      int pageSize)
  {
    javax.persistence.Query query = tx.createQuery(sQuery);
    query.setFirstResult(offset).setMaxResults(pageSize);
    return query;
  }

  private void insertSearchIndexChange(final TransactionContext tx, final SearchIndexChange searchIndexChange) {
    searchIndexManager.insert(tx, searchIndexChange);
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

  protected SearchIndexChange newSearchIndexChange(@SuppressWarnings("unused") T entity) {
    // by default, no contribution to the search index
    return null;
  }

  protected String buildPositionalParameters(Collection<?> collection, int startFrom) {
    StringJoiner joiner = new StringJoiner(",");
    for (int i = 0; i < collection.size(); i++) {
      joiner.add("?" + (i + startFrom));
    }
    return "(" + joiner.toString() + ")";
  }

  protected void addPositionalParameters(javax.persistence.Query query, Collection<?> collection, int startFrom) {
    for (Object object : collection) {
      query.setParameter(startFrom++, object);
    }
  }

  protected boolean detectTestEntityLeaks() {
    return System.getProperty("detectTestEntityLeaks") != null;
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

  public List<T> getAll(TransactionContext tx) {
    String sQuery = "SELECT entity FROM " + getEntityName() + " entity";
    return getList(tx, sQuery);
  }

  public List<T> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAll(tx);
    }
  }

  public long getCount() {
    String sQuery = "SELECT COUNT(entity) FROM " + getEntityName() + " entity";
    return getSingle(Long.class, sQuery);
  }

  /**
   * This method should be used for queries that use an "IN" clause.
   * H2 and Postgres limit the number of elements in "IN" clauses. This method breaks the list of values into
   * partitions, runs the given query on each partition and merges the results from all partitions.
   * 
   * @param <E> The type of the values in the list to be used in the "IN" clause.
   * @param inClauseValues List of values to be used in the "IN" clause.
   * @param getter Function to be used to query the values.
   */
  protected <E> List<T> getListWithSqlInClause(List<E> inClauseValues, Function<Collection<E>, List<T>> getter) {
    int inOperatorThreshold = getInOperatorThreshold();
    if (inClauseValues.size() >= inOperatorThreshold) {
      List<List<E>> inClauseValuesPartitions = Lists.partition(inClauseValues, inOperatorThreshold);

      return inClauseValuesPartitions.stream().map(getter).flatMap(Collection::stream).collect(toList());
    }
    else {
      return getter.apply(inClauseValues);
    }
  }

  public String getEntityName() {
    return entityName;
  }

  protected String getDatabaseSchema() {
    return operationalDataStore.getDatabaseSchema();
  }

  public static class TestEntityLeakDetectionData
  {
    private final String creationStackTrace;

    private final AbstractOperationalSqlDAO<?> dao;

    private TestEntityLeakDetectionData(
        AbstractOperationalSqlDAO<?> dao,
        String creationStackTrace)
    {
      this.dao = dao;
      this.creationStackTrace = creationStackTrace;
    }

    public String getCreationStackTrace() {
      return creationStackTrace;
    }

    public AbstractOperationalSqlDAO<?> getDAO() {
      return dao;
    }
  }
}
