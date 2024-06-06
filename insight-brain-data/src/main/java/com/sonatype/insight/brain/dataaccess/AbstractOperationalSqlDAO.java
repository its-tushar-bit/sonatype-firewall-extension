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

import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.lang3.exception.ExceptionUtils;

public abstract class AbstractOperationalSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  public static Map<String, TestEntityLeakDetectionData> testEntityLeaksDetectionData = new LinkedHashMap<>();

  private String entityName;

  private final OperationalDataStore operationalDataStore;

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
    super(searchIndexManager);
    this.operationalDataStore = operationalDataStore;
    entityName = ((Class<?>) getParameterizedSuperClass().getActualTypeArguments()[0]).getSimpleName();
  }

  protected AbstractOperationalSqlDAO(OperationalDataStore operationalDataStore) {
    this.operationalDataStore = operationalDataStore;
    entityName = ((Class<?>) getParameterizedSuperClass().getActualTypeArguments()[0]).getSimpleName();
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

  protected boolean isDatabasePostgresql() {
    return !operationalDataStore.isDatabaseInMemory() && org.postgresql.Driver.class.getName()
        .equals(operationalDataStore.getDatabaseConfig().getDriverClassName());
  }

  @Override
  public void insert(TransactionContext tx, T entity) {
    super.insert(tx, entity);

    if (detectTestEntityLeaks() && operationalDataStore.isDatabaseInMemory()) {
      Exception e = new Exception("Entity of type " + entity.getClass().getName() + " created at:");
      testEntityLeaksDetectionData.put(entity.getId(),
          new TestEntityLeakDetectionData(this, ExceptionUtils.getStackTrace(e)));
    }
  }

  @Override
  public void delete(TransactionContext tx, T entity) {
    super.delete(tx, entity);

    if (entity != null && detectTestEntityLeaks() && operationalDataStore.isDatabaseInMemory()) {
      testEntityLeaksDetectionData.remove(entity.getId());
    }
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

  public static javax.persistence.Query createPaginationNativeQuery(
      TransactionContext tx,
      String sQuery,
      int offset,
      int pageSize)
  {
    javax.persistence.Query query = tx.createNativeQuery(sQuery);
    query.setFirstResult(offset).setMaxResults(pageSize);
    return query;
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

  public boolean isDatabaseEmbedded() {
    return super.isDatabaseEmbedded(operationalDataStore);
  }

  public int getInOperatorThreshold() {
    return super.getInOperatorThreshold(operationalDataStore);
  }

  protected <E> List<T> getListWithSqlInClause(List<E> inClauseValues, Function<Collection<E>, List<T>> getter) {
    return super.getListWithSqlInClause(inClauseValues, getter, operationalDataStore);
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
