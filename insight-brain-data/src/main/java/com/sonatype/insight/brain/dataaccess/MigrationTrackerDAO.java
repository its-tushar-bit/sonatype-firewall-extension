/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.dataaccess.TransactionContext;

public class MigrationTrackerDAO
    extends AbstractOperationalSqlDAO<MigrationTracker>
{
  @Override
  public void insert(TransactionContext tx, MigrationTracker entity) {
    Objects.requireNonNull(entity.getId(), "MigrationTracker entity cannot be inserted without an id!");
    super.insert(tx, entity);
  }

  @Override
  public MigrationTracker getById(final String id) {
    return get("SELECT mt FROM MigrationTracker mt WHERE mt.id=?1", id);
  }

  public List<MigrationTracker> getAll() {
    String sQuery = "SELECT entity FROM MigrationTracker entity";
    return getList(sQuery);
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
}
