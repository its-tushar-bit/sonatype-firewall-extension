/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.dataaccess;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;

import com.sonatype.insight.model.HasStringId;

import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Field;
import org.jooq.InsertSetMoreStep;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jooq.exception.SQLStateClass.C23_INTEGRITY_CONSTRAINT_VIOLATION;

public abstract class AbstractDAO<T>
{
  private static final Logger log = LoggerFactory.getLogger(AbstractDAO.class);

  protected abstract TransactionContext createTransactionContext();

  protected TransactionContext createReadOnlyTransactionContext() {
    return createTransactionContext();
  }

  /**
   * Returns the jOOQ table reference for this DAO's entity type.
   *
   * @return the jOOQ table for this entity
   */
  public abstract Table<?> getJooqTable();

  /**
   * Returns the entity class for this DAO. Subclasses must implement this method to return their specific entity
   * class.
   *
   * @return the entity class
   */
  public abstract Class<T> getEntityClass();

  /**
   * Maps entity fields to a jOOQ record for insert/update operations. Subclasses should override this to provide
   * explicit field mapping (e.g., computed columns, JSON serialization).
   * <p>
   * The default implementation uses jOOQ's {@link UpdatableRecord#from(Object)} which relies on matching field names or
   * {@code @Column} annotations.
   * </p>
   * <p>
   * Note: The ID field should be set by the caller, not in this method.
   * </p>
   *
   * @param record the jOOQ record to populate
   * @param entity the entity to read values from
   * @return the populated record for fluent usage
   */
  protected UpdatableRecord<?> fromEntity(UpdatableRecord<?> record, T entity) {
    record.from(entity);
    return record;
  }

  /**
   * Maps a jOOQ record to an entity. Subclasses should override this to provide explicit field mapping when needed
   * (e.g., for type conversions like Date/LocalDateTime, Enum/String).
   * <p>
   * The default implementation uses jOOQ's {@link Record#into(Class)} which relies on matching field names or
   * {@code @Column} annotations.
   * </p>
   *
   * @param record the jOOQ record to read from
   * @return the entity, or null if record is null
   */
  protected T toEntity(Record record) {
    if (record == null) {
      return null;
    }
    return record.into(getEntityClass());
  }

  /**
   * Gets the ID field from a jOOQ table by looking up the primary key. The primary key is expected to be a
   * single-column String field.
   *
   * @param table the jOOQ table
   * @return the ID field from the table's primary key
   * @throws IllegalStateException if the table has no primary key or the primary key is not a single column
   */
  @SuppressWarnings("unchecked")
  protected Field<String> getIdField(Table<?> table) {
    var primaryKey = table.getPrimaryKey();
    if (primaryKey == null) {
      throw new IllegalStateException("Table " + table.getName() + " has no primary key defined");
    }
    var fields = primaryKey.getFields();
    if (fields.size() != 1) {
      throw new IllegalStateException(
          "Table " + table.getName() + " has a composite primary key with " + fields.size() +
              " fields, but this operation requires a single-column primary key");
    }
    return (Field<String>) fields.get(0);
  }

  public T getById(String id) {
    try (TransactionContext tx = createReadOnlyTransactionContext()) {
      return getById(tx, id);
    }
  }

  /**
   * Gets an entity by its ID using jOOQ.
   *
   * @param tx the transaction context
   * @param id the entity ID
   * @return the entity or null if not found
   */
  public T getById(TransactionContext tx, String id) {
    Table<?> table = getJooqTable();
    var idField = getIdField(table);
    return toEntity(tx.dsl()
        .selectFrom(table)
        .where(idField.eq(id))
        .fetchOne());
  }

  /**
   * Gets a single entity by matching a field value. This is a convenience method for the common pattern of looking up
   * an entity by a single non-ID column (e.g., getByServerId, getByOwnerId).
   *
   * @param field the jOOQ field to match against
   * @param value the value to match
   * @param <V> the field value type
   * @return the entity or null if not found
   */
  protected <V> T getByField(final Field<V> field, final V value) {
    try (TransactionContext tx = createReadOnlyTransactionContext()) {
      return getByField(tx, field, value);
    }
  }

  /**
   * Gets a single entity by matching a field value within an existing transaction.
   *
   * @param tx the transaction context
   * @param field the jOOQ field to match against
   * @param value the value to match
   * @param <V> the field value type
   * @return the entity or null if not found
   */
  protected <V> T getByField(final TransactionContext tx, final Field<V> field, final V value) {
    return toEntity(tx.dsl()
        .selectFrom(getJooqTable())
        .where(field.eq(value))
        .fetchOne());
  }

  /**
   * Insert an entity into the database using jOOQ.
   * <p>
   * This implementation creates a new jOOQ record, populates it using {@link #fromEntity(UpdatableRecord, Object)}, and
   * inserts it.
   * </p>
   * <p>
   * Subclasses can override this method to add validation or business logic, then call {@code super.insert(tx, entity)}
   * to perform the actual insert.
   * </p>
   *
   * @param tx the transaction context
   * @param entity the entity to insert
   * @param ignoreDuplicateKey whether to ignore duplicate key errors during insert
   * @return the number of rows inserted: {@code 1} if the row was inserted, or {@code 0} if {@code ignoreDuplicateKey}
   *         is {@code true} and an existing duplicate caused the insert to be skipped
   */
  public int insert(TransactionContext tx, T entity, boolean ignoreDuplicateKey) {
    Table<?> table = getJooqTable();
    UpdatableRecord<?> record = (UpdatableRecord<?>) tx.dsl().newRecord(table);

    // Set ID field if entity has one
    if (entity instanceof HasStringId hasStringId) {
      record.set(getIdField(table), hasStringId.getId());
    }

    fromEntity(record, entity);

    // Use insertInto().set() instead of record.insert() to avoid
    // jOOQ's RETURNING clause emulation which doesn't work well with H2
    InsertSetMoreStep<?> insertSetMoreStep = tx.dsl().insertInto(table).set(record);
    if (ignoreDuplicateKey) {
      SQLDialect dialect = tx.dsl().dialect();
      // H2 1.4.196 doesn't support jOOQ onDuplicateKeyIgnore (translated to MERGE INTO) or onConflictDoNothing
      // use savepoints in the transaction instead
      if (dialect == SQLDialect.H2) {
        int[] inserted = {0};
        tx.dsl().connection(conn -> {
          Savepoint savepoint = conn.setSavepoint();
          try (PreparedStatement ps = conn.prepareStatement(
              tx.dsl().renderInlined(insertSetMoreStep)))
          {
            ps.execute();
            conn.releaseSavepoint(savepoint);
            inserted[0] = 1;
          }
          catch (SQLException e) {
            conn.rollback(savepoint);
            if (!e.getSQLState().startsWith(C23_INTEGRITY_CONSTRAINT_VIOLATION.className())) {
              throw e;
            }
          }
        });
        return inserted[0];
      }
      return insertSetMoreStep.onDuplicateKeyIgnore().execute();
    }
    return insertSetMoreStep.execute();
  }

  public int insert(T entity, boolean ignoreDuplicateKey) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int inserted = insert(tx, entity, ignoreDuplicateKey);
      tx.commit();
      return inserted;
    }
  }

  public int insert(TransactionContext tx, T entity) {
    return insert(tx, entity, false);
  }

  public int insert(T entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int inserted = insert(tx, entity);
      tx.commit();
      return inserted;
    }
  }

  /**
   * Batch-inserts the entities.
   *
   * @param tx the transaction context
   * @param entities the entities to insert
   * @param ignoreDuplicateKey whether to ignore duplicate key errors during insert
   * @return the number of rows actually inserted. When {@code ignoreDuplicateKey} is {@code true}, rows whose duplicate
   *         already existed are skipped and not counted, so the result can be less than {@code entities.size()}.
   */
  public int insertBatch(TransactionContext tx, List<T> entities, boolean ignoreDuplicateKey) {
    if (CollectionUtils.isEmpty(entities)) {
      return 0;
    }
    Table<?> table = getJooqTable();
    SQLDialect dialect = tx.dsl().dialect();
    if (dialect == SQLDialect.H2) {
      int inserted = 0;
      for (T entity : entities) {
        inserted += insert(tx, entity, ignoreDuplicateKey);
      }
      return inserted;
    }
    var steps = entities.stream()
        .map(entity -> {
          UpdatableRecord<?> record = (UpdatableRecord<?>) tx.dsl().newRecord(table);
          // Set ID field if entity has one
          if (entity instanceof HasStringId hasStringId) {
            record.set(getIdField(table), hasStringId.getId());
          }
          fromEntity(record, entity);
          // Use insertInto().set() instead of record.insert() to avoid
          // jOOQ's RETURNING clause emulation which doesn't work well with H2
          InsertSetMoreStep<?> insertSetMoreStep = tx.dsl().insertInto(table).set(record);
          return ignoreDuplicateKey ? insertSetMoreStep.onDuplicateKeyIgnore() : insertSetMoreStep;
        })
        .toList();
    return sumBatchResult(tx.dsl().batch(steps).execute());
  }

  public int insertBatch(List<T> entities, boolean ignoreDuplicateKey) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int inserted = insertBatch(tx, entities, ignoreDuplicateKey);
      tx.commit();
      return inserted;
    }
  }

  public int insertBatch(TransactionContext tx, List<T> entities) {
    return insertBatch(tx, entities, false);
  }

  public int insertBatch(List<T> entities) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int inserted = insertBatch(tx, entities);
      tx.commit();
      return inserted;
    }
  }

  /**
   * Update an entity in the database using jOOQ.
   * <p>
   * This implementation fetches the existing record, populates it using {@link #fromEntity(UpdatableRecord, Object)},
   * and updates it.
   * </p>
   * <p>
   * Subclasses can override this method to add validation or business logic, then call {@code super.update(tx, entity)}
   * to perform the actual update.
   * </p>
   *
   * @param tx the transaction context
   * @param entity the entity to update
   * @return the number of rows updated (typically {@code 1})
   * @throws IllegalStateException if the entity is not found
   */
  @SuppressWarnings("unchecked")
  public int update(TransactionContext tx, T entity) {
    Table<?> table = getJooqTable();
    Field<String> idField = getIdField(table);

    String entityId = null;
    if (entity instanceof HasStringId hasStringId) {
      entityId = hasStringId.getId();
    }

    UpdatableRecord<?> record = (UpdatableRecord<?>) tx.dsl()
        .selectFrom(table)
        .where(idField.eq(entityId))
        .fetchOne();

    if (record == null) {
      throw new IllegalStateException("Entity not found: " + entityId);
    }

    fromEntity(record, entity);

    // Use explicit update instead of record.update() to avoid
    // jOOQ's RETURNING clause emulation which doesn't work well with H2
    return tx.dsl().update(table).set(record).where(idField.eq(entityId)).execute();
  }

  public int update(T entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int updated = update(tx, entity);
      tx.commit();
      return updated;
    }
  }

  /**
   * Batch update entities. Unlike {@link #update(TransactionContext, Object)}, the non-H2 path does not fetch
   * existing records first — it issues blind UPDATE statements that write ALL columns. Entities must be fully
   * populated with their intended DB values (not just the fields being changed), since no fetch-before-update occurs.
   * Rows not found in the database are silently skipped rather than throwing.
   * The H2 path falls back to individual {@link #update(TransactionContext, Object)} calls.
   */
  public int updateBatch(TransactionContext tx, List<T> entities) {
    if (CollectionUtils.isEmpty(entities)) {
      return 0;
    }
    Table<?> table = getJooqTable();
    Field<String> idField = getIdField(table);
    SQLDialect dialect = tx.dsl().dialect();
    if (dialect == SQLDialect.H2) {
      int updated = 0;
      for (T entity : entities) {
        updated += update(tx, entity);
      }
      return updated;
    }
    var steps = entities.stream()
        .map(entity -> {
          if (!(entity instanceof HasStringId hasStringId)) {
            throw new IllegalArgumentException("updateBatch requires HasStringId entities");
          }
          UpdatableRecord<?> record = (UpdatableRecord<?>) tx.dsl().newRecord(table);
          record.set(idField, hasStringId.getId());
          fromEntity(record, entity);
          return (Query) tx.dsl().update(table).set(record).where(idField.eq(record.get(idField)));
        })
        .toList();
    return sumBatchResult(tx.dsl().batch(steps).execute());
  }

  public int updateBatch(List<T> entities) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int updated = updateBatch(tx, entities);
      tx.commit();
      return updated;
    }
  }

  /**
   * Sums the per-statement affected-row counts returned by a jOOQ batch execute into a total.
   * <p>
   * Used by both {@code insertBatch} and {@code updateBatch}. Relies on the driver returning a real per-statement
   * count. If PostgreSQL's {@code reWriteBatchedInserts} were enabled the driver would coalesce statements and return
   * {@link Statement#SUCCESS_NO_INFO} ({@code -2}) per statement, which would silently under-report the total; the
   * batch methods here do not enable that option, so we log loudly if we ever see {@code SUCCESS_NO_INFO} rather than
   * let the count silently collapse. A statement that failed reports {@link Statement#EXECUTE_FAILED} ({@code -3}) —
   * jOOQ normally throws on batch failure rather than returning it, so this too is logged rather than silently dropped.
   */
  static int sumBatchResult(int[] batchResult) {
    int total = 0;
    int unknown = 0;
    int failed = 0;
    for (int count : batchResult) {
      if (count > 0) {
        total += count;
      }
      else if (count == Statement.SUCCESS_NO_INFO) {
        unknown++;
      }
      else if (count == Statement.EXECUTE_FAILED) {
        failed++;
      }
    }
    if (unknown > 0) {
      log.warn("Batch returned no affected-row count for {} of {} statements (SUCCESS_NO_INFO); total under-reported "
          + "as {}. This is only expected if PostgreSQL 'reWriteBatchedInserts' is enabled, which is not supported "
          + "here.", unknown, batchResult.length, total);
    }
    if (failed > 0) {
      log.warn("Batch reported {} of {} statements as EXECUTE_FAILED; total under-reported as {}. jOOQ normally throws "
          + "on batch failure, so this is unexpected.", failed, batchResult.length, total);
    }
    return total;
  }

  /**
   * Delete an entity from the database.
   *
   * @param tx the transaction context
   * @param entity the entity to delete
   */
  public void delete(TransactionContext tx, T entity) {
    if (entity == null) {
      return;
    }
    Table<?> table = getJooqTable();
    Field<String> idField = getIdField(table);
    String entityId = null;
    if (entity instanceof HasStringId hasStringId) {
      entityId = hasStringId.getId();
    }
    tx.dsl().deleteFrom(table).where(idField.eq(entityId)).execute();
  }

  public void delete(T entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      delete(tx, entity);
      tx.commit();
    }
  }
}
