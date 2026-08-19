/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Objects;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.MigrationTracker.MIGRATION_TRACKER;

@Named
@Singleton
public class MigrationTrackerDAO
    extends AbstractOperationalSqlDAO<MigrationTracker>
{
  @Inject
  public MigrationTrackerDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public int insert(TransactionContext tx, MigrationTracker entity) {
    Objects.requireNonNull(entity.getId(), "MigrationTracker entity cannot be inserted without an id!");
    return super.insert(tx, entity);
  }

  @Override
  public void delete(TransactionContext tx, MigrationTracker entity) {
    if (entity == null) {
      return;
    }
    super.delete(tx, entity);
  }

  public boolean isTrackerPresent(String trackerId) {
    return getById(trackerId) != null;
  }

  public void insertTracker(TransactionContext tx, String trackerId) {
    this.insert(tx, new MigrationTracker(trackerId));
  }

  public void insertTracker(String trackerId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      insertTracker(tx, trackerId);
      tx.commit();
    }
  }

  public void deleteById(String migrationTrackerId) {
    super.delete(getById(migrationTrackerId));
  }

  @Override
  public Table<?> getJooqTable() {
    return MIGRATION_TRACKER;
  }

  @Override
  public Class<MigrationTracker> getEntityClass() {
    return MigrationTracker.class;
  }
}
