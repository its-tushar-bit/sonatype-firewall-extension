/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.fixture;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.db.rule.DatabaseRule;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.db.DatabaseConfig;

/**
 * Test fixtures for the database. Direct use is normally not required, use {@link DatabaseRule}.
 */
public interface DatabaseFixture
    extends AutoCloseable
{
  /**
   * Return an IQ {@link DatabaseConfig} object for this database fixture
   */
  DatabaseConfig getDatabaseConfig(final String databaseName);

  /**
   * Return a {@link DataSourceProvider} for this database fixture
   *
   * @return
   */
  DataSourceProvider getDataSourceProvider();

  default Map<String, Object> getDatabaseMetadata() {
    return new HashMap<>();
  }

  /**
   * Indicates if the DB fixture can be re-used by the next test or if it should be deleted and databases
   * re-initialized
   */
  boolean isFixtureReusable();

  /**
   * Load the given SQL file into the database
   */
  void loadSqlDump(Path sqlFile);

  /**
   * Dump and return the given schema as a string
   */
  String dumpSchema(String schema);
}
