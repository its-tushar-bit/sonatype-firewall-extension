/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import com.sonatype.insight.brain.db.datastore.DataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiquibaseDataStoreMigrator
    implements DataStoreMigrator
{
  private static final Logger log = LoggerFactory.getLogger(LiquibaseDataStoreMigrator.class);

  private final DataStore dataStore;

  public LiquibaseDataStoreMigrator(final DataStore dataStore) {
    this.dataStore = dataStore;
  }

  @Override
  public void migrate() {
    log.debug("TODO - execute liquibase on " + dataStore.getID());
  }
}
