/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jooq.SQLDialect;
import org.jooq.Table;

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

  protected boolean isDatabasePostgresql() {
    return !operationalDataStore.isDatabaseInMemory() && org.postgresql.Driver.class.getName()
        .equals(operationalDataStore.getDatabaseConfig().getDriverClassName());
  }

  /**
   * Insert an entity into the database using jOOQ.
   * <p>
   * This implementation generates an ID if needed, calls the superclass insert, and then handles search index changes
   * and entity leak detection.
   * </p>
   *
   * @param tx the transaction context
   * @param entity the entity to insert
   */
  @Override
  public void insert(TransactionContext tx, T entity, boolean ignoreDuplicateKey) {
    generateIdIfNeeded(entity);

    // Call superclass to perform the actual insert
    super.insert(tx, entity, ignoreDuplicateKey);

    // Handle search index changes
    if (shouldAddSearchIndexChange(tx, entity)) {
      insertSearchIndexChange(tx, newSearchIndexChangeForInsert(entity));
    }

    // Handle entity leak detection
    if (detectTestEntityLeaks() && operationalDataStore.isDatabaseInMemory()) {
      Exception e = new Exception("Entity of type " + entity.getClass().getName() + " created at:");
      testEntityLeaksDetectionData.put(entity.getId(),
          new TestEntityLeakDetectionData(this, ExceptionUtils.getStackTrace(e)));
    }
  }

  @Override
  public void insertBatch(TransactionContext tx, List<T> entities, boolean ignoreDuplicateKey) {
    super.insertBatch(tx, entities, ignoreDuplicateKey);

    // Only add search index changes and leak detection for the non-H2 path. The H2 fallback in
    // AbstractDAO.insertBatch calls insert(tx, entity) per entity, which already handles these
    // via our insert() override above.
    if (tx.dsl().dialect() != SQLDialect.H2) {
      for (T entity : entities) {
        if (shouldAddSearchIndexChange(tx, entity)) {
          insertSearchIndexChange(tx, newSearchIndexChangeForInsert(entity));
        }
        if (detectTestEntityLeaks() && operationalDataStore.isDatabaseInMemory()) {
          Exception e = new Exception("Entity of type " + entity.getClass().getName() + " created at:");
          testEntityLeaksDetectionData.put(entity.getId(),
              new TestEntityLeakDetectionData(this, ExceptionUtils.getStackTrace(e)));
        }
      }
    }
  }

  /**
   * Update an entity in the database using jOOQ.
   * <p>
   * This implementation calls the superclass update and then handles search index changes.
   * </p>
   *
   * @param tx the transaction context
   * @param entity the entity to update
   * @throws IllegalStateException if the entity is not found
   */
  @Override
  public void update(TransactionContext tx, T entity) {
    // Call superclass to perform the actual update
    super.update(tx, entity);

    // Handle search index changes
    if (shouldAddSearchIndexChange(tx, entity)) {
      insertSearchIndexChange(tx, newSearchIndexChangeForUpdate(entity));
    }
  }

  @Override
  public void updateBatch(TransactionContext tx, List<T> entities) {
    super.updateBatch(tx, entities);

    // Only add search index changes for the non-H2 path. The H2 fallback in AbstractDAO.updateBatch
    // calls update(tx, entity) per entity, which already handles search index changes via our
    // update() override above.
    if (tx.dsl().dialect() != SQLDialect.H2) {
      for (T entity : entities) {
        if (shouldAddSearchIndexChange(tx, entity)) {
          insertSearchIndexChange(tx, newSearchIndexChangeForUpdate(entity));
        }
      }
    }
  }

  /**
   * Delete an entity from the database using jOOQ.
   *
   * @param tx the transaction context
   * @param entity the entity to delete
   */
  @Override
  public void delete(TransactionContext tx, T entity) {
    super.delete(tx, entity);

    if (entity != null && detectTestEntityLeaks() && operationalDataStore.isDatabaseInMemory()) {
      testEntityLeaksDetectionData.remove(entity.getId());
    }
  }

  protected String buildPositionalParameters(Collection<?> collection, int startFrom) {
    StringJoiner joiner = new StringJoiner(",");
    for (int i = 0; i < collection.size(); i++) {
      joiner.add("?" + (i + startFrom));
    }
    return "(" + joiner + ")";
  }

  protected boolean detectTestEntityLeaks() {
    return System.getProperty("detectTestEntityLeaks") != null;
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

  /**
   * Stream variant of {@link #getListWithSqlInClause(Collection, Function)}. The getter returns a {@link Stream}
   * (e.g. from jOOQ's {@code fetchStream()}) instead of a {@link List}. The caller must close the returned stream.
   */
  protected <E, U> Stream<U> getStreamWithSqlInClause(
      Collection<E> inClauseValues,
      Function<Collection<E>, Stream<U>> getter)
  {
    return super.getStreamWithSqlInClause(inClauseValues, getter, operationalDataStore);
  }

  public List<T> getByIds(Collection<String> ids) {
    return getListWithSqlInClause(ids,
        partition -> {
          try (TransactionContext tx = createTransactionContext()) {
            Table<?> table = getJooqTable();
            var idField = getIdField(table);
            return tx.dsl()
                .selectFrom(table)
                .where(idField.in(partition))
                .fetch(this::toEntity);
          }
        });
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
