/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.dataaccess;

import com.sonatype.insight.model.HasStringId;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

public abstract class AbstractDAO<T>
{
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
   */
  @SuppressWarnings("unchecked")
  public void insert(TransactionContext tx, T entity) {
    Table<?> table = getJooqTable();
    UpdatableRecord<?> record = (UpdatableRecord<?>) tx.dsl().newRecord(table);

    // Set ID field if entity has one
    if (entity instanceof HasStringId hasStringId) {
      record.set(getIdField(table), hasStringId.getId());
    }

    fromEntity(record, entity);

    // Use insertInto().set().execute() instead of record.insert() to avoid
    // jOOQ's RETURNING clause emulation which doesn't work well with H2
    tx.dsl().insertInto(table).set(record).execute();
  }

  public void insert(T entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      insert(tx, entity);
      tx.commit();
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
   * @throws IllegalStateException if the entity is not found
   */
  @SuppressWarnings("unchecked")
  public void update(TransactionContext tx, T entity) {
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
    tx.dsl().update(table).set(record).where(idField.eq(entityId)).execute();
  }

  public void update(T entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      update(tx, entity);
      tx.commit();
    }
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
