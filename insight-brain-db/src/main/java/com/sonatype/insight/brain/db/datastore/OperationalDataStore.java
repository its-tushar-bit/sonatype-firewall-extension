/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import javax.sql.DataSource;

/**
 * Contract for the operational data store, aka ODS. This is the primary data store for IQ and includes some special
 * methods that the other data stores do not have.
 */
public interface OperationalDataStore
    extends DataStore
{
  String ID = "insight_brain_ods";

  // IQ version that the locking mechanism was introduced
  int LOCK_TABLE_DATABASE_VERSION = 181;

  @Override
  default String getID() {
    return OperationalDataStore.ID;
  }

  /**
   * Return the {@link DataSource} without the initialization. See {@link DatabaseMigrator#isMigrationEnabled()}
   */
  DataSource getDataSourceWithoutInit();

  /**
   * Is the database currently being used an in-memory database (tests only)
   */
  boolean isDatabaseInMemory();

  /**
   * Return the special {@link DataSource} that is used for the locking mechanism
   */
  DataSource getDataSourceForLocks();
}
