/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.sql.Array;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.lang3.exception.ExceptionUtils;

public abstract class AbstractOperationalSqlDAO<T extends HasStringId>
    extends AbstractSqlDAO<T>
{
  public static Map<String, TestEntityLeakDetectionData> testEntityLeaksDetectionData = new LinkedHashMap<>();

  private final OperationalDataStore operationalDataStore;

  /**
   * Constructor for DAOs that require the search index. These DAOs must override one of the methods:
   * <ul>
   * <li>{@link #newSearchIndexChange(HasStringId)}</li>
   * <li>{@link #newSearchIndexChangeForInsert(HasStringId)}</li>
   * <li>{@link #newSearchIndexChangeForUpdate(HasStringId)}</li>
   * <li>{@link #newSearchIndexChangeForDelete(HasStringId)}</li>
   * </ul>
   */
  protected AbstractOperationalSqlDAO(
      final OperationalDataStore operationalDataStore,
      final SearchIndexManager searchIndexManager)
  {
    super(searchIndexManager);
    this.operationalDataStore = operationalDataStore;
  }

  protected AbstractOperationalSqlDAO(OperationalDataStore operationalDataStore) {
    this.operationalDataStore = operationalDataStore;
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

  public static jakarta.persistence.Query createPaginationNativeQuery(
      TransactionContext tx,
      String sQuery,
      int offset,
      int pageSize)
  {
    jakarta.persistence.Query query = tx.createNativeQuery(sQuery);
    query.setFirstResult(offset).setMaxResults(pageSize);
    return query;
  }

  public static <T> jakarta.persistence.Query createPaginationNativeQuery(
      TransactionContext tx,
      Class<T> resultClass,
      String sQuery,
      int offset,
      int pageSize)
  {
    jakarta.persistence.Query query = tx.createNativeQuery(sQuery, resultClass);
    query.setFirstResult(offset).setMaxResults(pageSize);
    return query;
  }

  protected Array createArrayOf(JDBCType jdbcType, Object[] elements) throws SQLException {
    try (Connection connection = operationalDataStore.getDataSource().getConnection()) {
      return connection.createArrayOf(jdbcType.name(), elements);
    }
  }

  protected String buildPositionalParameters(Collection<?> collection, int startFrom) {
    StringJoiner joiner = new StringJoiner(",");
    for (int i = 0; i < collection.size(); i++) {
      joiner.add("?" + (i + startFrom));
    }
    return "(" + joiner.toString() + ")";
  }

  protected void addPositionalParameters(jakarta.persistence.Query query, Collection<?> collection, int startFrom) {
    for (Object object : collection) {
      query.setParameter(startFrom++, object);
    }
  }

  protected boolean detectTestEntityLeaks() {
    return System.getProperty("detectTestEntityLeaks") != null;
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

  public boolean isDatabaseEmbedded() {
    return super.isDatabaseEmbedded(operationalDataStore);
  }

  public int getInOperatorThreshold() {
    return super.getInOperatorThreshold(operationalDataStore);
  }

  protected <E, U> List<U> getListWithSqlInClause(
      Collection<E> inClauseValues,
      Function<Collection<E>, List<U>> getter)
  {
    return super.getListWithSqlInClause(inClauseValues, getter, operationalDataStore);
  }

  protected String getDatabaseSchema() {
    return operationalDataStore.getDatabaseSchema();
  }

  protected String injectSchemaName(final String sql) {
    return sql.replace("_SCHEMA_", getDatabaseSchema());
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

  @Override
  protected OperationalDataStore getDataStore() {
    return operationalDataStore;
  }
}
