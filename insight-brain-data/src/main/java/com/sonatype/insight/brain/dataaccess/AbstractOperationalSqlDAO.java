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

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.lang3.exception.ExceptionUtils;

public abstract class AbstractOperationalSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  //visible for testing
  public static final int H2_IN_OPERATOR_THRESHOLD = 2000;

  //visible for testing
  public static final int POSTGRES_IN_OPERATOR_THRESHOLD = Short.MAX_VALUE;

  public static Map<String, TestEntityLeakDetectionData> testEntityLeaksDetectionData = new LinkedHashMap<>();

  private String entityName;

  public AbstractOperationalSqlDAO() {
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
    return new TransactionContext(OperationalDataStoreProvider.getJPAEntityManagerFactory().createEntityManager());
  }

  public boolean isDatabaseEmbedded() {
    return OperationalDataStoreProvider.isDatabaseEmbedded();
  }

  public int getInOperatorThreshold() {
    return isDatabaseEmbedded() ? H2_IN_OPERATOR_THRESHOLD : POSTGRES_IN_OPERATOR_THRESHOLD;
  }

  @Override
  public void insert(TransactionContext tx, T entity) {
    super.insert(tx, entity);

    if (detectTestEntityLeaks() && OperationalDataStoreProvider.isDatabaseInMemory()) {
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

    if (entity != null && detectTestEntityLeaks() && OperationalDataStoreProvider.isDatabaseInMemory()) {
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

  protected void insertSearchIndexChange(TransactionContext tx, SearchIndexChange searchIndexChange) {
    if (searchIndexChange != null) {
      new SearchIndexChangeDAO().insert(tx, searchIndexChange);
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

  public List<T> getAll() {
    String sQuery = "SELECT entity FROM " + getEntityName() + " entity";
    return getList(sQuery);
  }

  public long getCount() {
    String sQuery = "SELECT COUNT(entity) FROM " + getEntityName() + " entity";
    return getSingle(Long.class, sQuery);
  }

  public String getEntityName() {
    return entityName;
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
